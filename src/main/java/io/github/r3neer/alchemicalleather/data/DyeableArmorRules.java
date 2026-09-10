package io.github.r3neer.alchemicalleather.data;

import com.google.gson.*;
import java.util.*;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;

/** Caches the targets of standard minecraft:crafting_dye recipes for O(1)/small-tag runtime classification. */
public final class DyeableArmorRules implements SimpleSynchronousResourceReloadListener {
    public static final TagKey<Item> FALLBACK_TAG=TagKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath("alchemical_leather","dyeable_armor"));
    private static volatile Set<Identifier> items=Set.of();
    private static volatile Set<TagKey<Item>> tags=Set.of();

    public static boolean dyeable(ItemStack stack) {
        if(stack.is(FALLBACK_TAG))return true;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(items.contains(id))return true;
        for(var tag:tags)if(stack.is(tag))return true;
        return false;
    }

    @Override public Identifier getFabricId(){return Identifier.fromNamespaceAndPath("alchemical_leather","dyeable_armor");}

    @Override public void onResourceManagerReload(ResourceManager manager) {
        var nextItems=new HashSet<Identifier>();var nextTags=new HashSet<TagKey<Item>>();
        manager.listResources("recipe",path->path.getPath().endsWith(".json")).forEach((path,resource)->{
            try(var reader=resource.openAsReader()) {
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                if(!json.has("type")||!json.get("type").getAsString().equals("minecraft:crafting_dye"))return;
                if(!json.has("target"))throw new IllegalArgumentException("Missing target");
                collect(json.get("target"),nextItems,nextTags);
            } catch(Exception e){throw new IllegalArgumentException("Invalid dye recipe "+path,e);}
        });
        items=Set.copyOf(nextItems);tags=Set.copyOf(nextTags);
    }

    private static void collect(JsonElement target,Set<Identifier> itemIds,Set<TagKey<Item>> itemTags) {
        if(target.isJsonPrimitive()&&target.getAsJsonPrimitive().isString()) {
            var raw=target.getAsString();
            if(raw.startsWith("#"))itemTags.add(TagKey.create(Registries.ITEM,Identifier.parse(raw.substring(1))));
            else itemIds.add(Identifier.parse(raw));
            return;
        }
        if(target.isJsonArray()) {
            for(var entry:target.getAsJsonArray()) {
                if(!entry.isJsonPrimitive()||!entry.getAsJsonPrimitive().isString()||entry.getAsString().startsWith("#"))
                    throw new IllegalArgumentException("Dye target lists must contain item identifiers");
                itemIds.add(Identifier.parse(entry.getAsString()));
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported dye target encoding");
    }
}
