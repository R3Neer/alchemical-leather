package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.EffectSlotRules;
import io.github.r3neer.alchemicalleather.data.WearRules;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

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
}
