package io.github.r3neer.alchemicalleather.data;
import com.google.gson.*;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.*;
public final class EffectSlotRules implements SimpleSynchronousResourceReloadListener {
    private static volatile Map<Identifier,EquipmentSlot> rules=Map.of();
    public static EquipmentSlot slot(Identifier effect) { return rules.get(effect); }
    @Override public Identifier getFabricId() { return Identifier.fromNamespaceAndPath("alchemical_leather","effect_slots"); }
    @Override public void onResourceManagerReload(ResourceManager manager) {
        var next=new HashMap<Identifier,EquipmentSlot>();
        String prefix="alchemical_leather/effect_slots/";
        manager.listResources(prefix.substring(0,prefix.length()-1),p->p.getPath().endsWith(".json")).forEach((path,resource)->{
            try(var reader=resource.openAsReader()) {
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                if(json.has("requires_mod")&&!FabricLoader.getInstance().isModLoaded(json.get("requires_mod").getAsString()))return;
                if(json.has("requires_resource")&&manager.getResource(Identifier.parse(json.get("requires_resource").getAsString())).isEmpty())return;
                if(json.has("enabled")&&!json.get("enabled").getAsBoolean())return;
                var effect=Identifier.fromNamespaceAndPath(path.getNamespace(),path.getPath().substring(prefix.length(),path.getPath().length()-5));
                if(!BuiltInRegistries.MOB_EFFECT.containsKey(effect))throw new IllegalArgumentException("Unknown effect "+effect);
                EquipmentSlot slot=switch(json.get("slot").getAsString()) {
                    case "helmet"->EquipmentSlot.HEAD;case "chestplate"->EquipmentSlot.CHEST;
                    case "leggings"->EquipmentSlot.LEGS;case "boots"->EquipmentSlot.FEET;
                    default->throw new IllegalArgumentException("Unknown armor slot");
                };
                next.put(effect,slot);
            } catch(Exception e) { throw new IllegalArgumentException("Invalid infusion rule "+path,e); }
        });
        rules=Map.copyOf(next);
    }
}
