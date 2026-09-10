package io.github.r3neer.alchemicalleather.data;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;

/** Caches standard evidence that an item can actually be recolored in place. */
public final class DyeableArmorRules implements SimpleSynchronousResourceReloadListener {
    public static final Identifier ID=Identifier.fromNamespaceAndPath("alchemical_leather","dyeable_armor");
    public static final TagKey<Item> FALLBACK_TAG=TagKey.create(Registries.ITEM,ID);
    private static final Identifier DYE_RECIPE=Identifier.withDefaultNamespace("crafting_dye");
    private static volatile Set<Identifier> selfItems=Set.of();
    private static volatile Set<TaggedResult> taggedResults=Set.of();
    private final RegistryOps.RegistryInfoLookup registryInfo;

    public DyeableArmorRules(HolderLookup.Provider registries) {
        this.registryInfo=new RegistryOps.RegistryInfoLookup(){
            @Override @SuppressWarnings({"rawtypes","unchecked"})
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> key){
                return (Optional)registries.lookup(key).map(RegistryOps.RegistryInfo::fromRegistryLookup);
            }
        };
    }

    public static boolean dyeable(ItemStack stack) {
        if(stack.is(ItemTags.CAULDRON_CAN_REMOVE_DYE)||stack.is(FALLBACK_TAG))return true;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(selfItems.contains(id))return true;
        for(var candidate:taggedResults)if(candidate.result().equals(id)&&stack.is(candidate.targetTag()))return true;
        return false;
    }

    @Override public Identifier getFabricId(){return ID;}

    @Override public void onResourceManagerReload(ResourceManager manager) {
        var nextItems=new HashSet<Identifier>();var nextTagged=new HashSet<TaggedResult>();
        manager.listResources("recipe",path->path.getPath().endsWith(".json")).forEach((path,resource)->{
            try(var reader=resource.openAsReader()) {
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                if(!json.has("type")||!Identifier.parse(json.get("type").getAsString()).equals(DYE_RECIPE)||!conditionsAllow(json))return;
                if(!json.has("target")||!json.has("result"))throw new IllegalArgumentException("Missing target/result");
                var result=resultId(json.get("result"));
                collectSelfTarget(json.get("target"),result,nextItems,nextTagged);
            } catch(Exception e){throw new IllegalArgumentException("Invalid dye recipe "+path,e);}
        });
        selfItems=Set.copyOf(nextItems);taggedResults=Set.copyOf(nextTagged);
    }

    private boolean conditionsAllow(JsonObject json) {
        if(!json.has(ResourceConditions.CONDITIONS_KEY))return true;
        var parsed=ResourceCondition.CONDITION_CODEC.parse(JsonOps.INSTANCE,json.get(ResourceConditions.CONDITIONS_KEY));
        // Match Fabric 26.2's effective loader behavior: malformed condition payloads are logged by Fabric
        // but are not treated as a successful false condition by its current implementation.
        return parsed.isError()||parsed.getOrThrow().test(registryInfo);
    }

    private static Identifier resultId(JsonElement result) {
        if(result.isJsonPrimitive()&&result.getAsJsonPrimitive().isString())return Identifier.parse(result.getAsString());
        if(result.isJsonObject()&&result.getAsJsonObject().has("id")&&result.getAsJsonObject().get("id").isJsonPrimitive())
            return Identifier.parse(result.getAsJsonObject().get("id").getAsString());
        throw new IllegalArgumentException("Unsupported dye result encoding");
    }

    private static void collectSelfTarget(JsonElement target,Identifier result,Set<Identifier> itemIds,Set<TaggedResult> tagged) {
        if(target.isJsonPrimitive()&&target.getAsJsonPrimitive().isString()) {
            var raw=target.getAsString();
            if(raw.startsWith("#"))tagged.add(new TaggedResult(result,TagKey.create(Registries.ITEM,Identifier.parse(raw.substring(1)))));
            else if(Identifier.parse(raw).equals(result))itemIds.add(result);
            return;
        }
        if(target.isJsonArray()) {
            boolean containsResult=false;
            for(var entry:target.getAsJsonArray()) {
                if(!entry.isJsonPrimitive()||!entry.getAsJsonPrimitive().isString()||entry.getAsString().startsWith("#"))
                    throw new IllegalArgumentException("Dye target lists must contain item identifiers");
                if(Identifier.parse(entry.getAsString()).equals(result))containsResult=true;
            }
            if(containsResult)itemIds.add(result);
            return;
        }
        throw new IllegalArgumentException("Unsupported dye target encoding");
    }

    private record TaggedResult(Identifier result,TagKey<Item> targetTag){}
}
