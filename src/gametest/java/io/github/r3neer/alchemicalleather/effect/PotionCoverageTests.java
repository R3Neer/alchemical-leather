package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.EffectSlotRules;
import io.github.r3neer.alchemicalleather.data.WearRules;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

/** Registry-driven gate: loaded potion effects may not silently fall through compatibility policy. */
public final class PotionCoverageTests {
    @GameTest public void everyLoadedPotionEffectHasExplicitWearPolicy(GameTestHelper h){
        var missing=new TreeSet<String>();
        BuiltInRegistries.POTION.listElements().forEach(potion->{
            for(var effect:potion.value().getEffects()){
                Identifier id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
                if(id!=null&&!WearRules.classified(id))missing.add(id.toString());
            }
        });
        h.assertTrue(missing.isEmpty(),"Every loaded potion effect must declare wear or wear=none; missing: "+missing);
        h.succeed();
    }

    @GameTest public void everyAlexPotionEffectHasHumanoidSlot(GameTestHelper h){
        var missing=new TreeSet<String>();
        BuiltInRegistries.POTION.listElements().forEach(potion->{
            Identifier potionId=BuiltInRegistries.POTION.getKey(potion.value());
            if(potionId==null||!potionId.getNamespace().equals("alexsmobs"))return;
            for(var effect:potion.value().getEffects()){
                Identifier id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
                if(id!=null&&EffectSlotRules.slot(id)==null)missing.add(potionId+" -> "+id);
            }
        });
        h.assertTrue(missing.isEmpty(),"Every Alex's Mobs potion used with Alchemical Leather needs a humanoid slot; missing: "+missing);
        h.succeed();
    }

    @GameTest public void loadedVanillaPlusContributorsOwnExpectedCompatibilityPolicy(GameTestHelper h){
        assertPolicy(h,"scalebrews:growth",EquipmentSlot.CHEST,true);
        assertPolicy(h,"scalebrews:shrinking",EquipmentSlot.CHEST,true);
        assertPolicy(h,"clinging_reoriented:reorientation",EquipmentSlot.FEET,false);
        assertPolicy(h,"alexsmobs:clinging",EquipmentSlot.FEET,false);
        assertPolicy(h,"friendsandfoes:reach",EquipmentSlot.CHEST,false);
        assertPolicy(h,"wilderwild:reach_boost",EquipmentSlot.CHEST,false);
        assertPolicy(h,"wilderwild:scorching",EquipmentSlot.CHEST,false);
        if(FabricLoader.getInstance().isModLoaded("deeper_dark")){
            Identifier blindness=Identifier.parse("minecraft:blindness");
            h.assertTrue(EffectSlotRules.slot(blindness)==EquipmentSlot.HEAD,"Deeper Dark blindness potion must activate the helmet slot rule");
            var rule=WearRules.rule(blindness);
            h.assertTrue(rule!=null&&rule.none(),"Blindness must remain explicitly classified as wear=none");
        }
        h.succeed();
    }

    private static void assertPolicy(GameTestHelper h,String rawId,EquipmentSlot expectedSlot,boolean expectedNoWear){
        Identifier id=Identifier.parse(rawId);
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(id))return;
        h.assertTrue(EffectSlotRules.slot(id)==expectedSlot,rawId+" must resolve to "+expectedSlot+" from the active compatibility resources");
        var rule=WearRules.rule(id);
        h.assertTrue(rule!=null,rawId+" must have an explicit wear classification");
        h.assertTrue(rule.none()==expectedNoWear,rawId+" wear classification mismatch: expected wear=none "+expectedNoWear);
    }
}
