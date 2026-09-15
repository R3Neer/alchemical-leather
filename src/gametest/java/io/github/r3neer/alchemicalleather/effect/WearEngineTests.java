package io.github.r3neer.alchemicalleather.effect;

import com.google.gson.JsonParser;
import com.mojang.util.Unit;
import io.github.r3neer.alchemicalleather.config.AlchemicalConfig;
import io.github.r3neer.alchemicalleather.data.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.GameType;

public final class WearEngineTests {
    private static final Identifier JUMP=Identifier.fromNamespaceAndPath("alchemical_leather","jump_boost_jump");

    @GameTest public void configParserDefaultsAndWearToggle(GameTestHelper h){
        var defaults=AlchemicalConfig.parse(JsonParser.parseString("{}"));
        h.assertTrue(defaults.infusionWear(),"Infusion wear defaults enabled");
        h.assertFalse(AlchemicalConfig.parse(JsonParser.parseString("{\"infusionWear\":false}")).infusionWear(),"Explicit false disables infusion wear");
        h.assertTrue(AlchemicalConfig.parse(JsonParser.parseString("{\"infusionWear\":\"bad\"}")).infusionWear(),"Malformed infusionWear falls back safely");
        h.succeed();
    }

    @GameTest public void wearRuleParserDistinguishesNoneBuiltinAndEvent(GameTestHelper h){
        var none=WearRules.parse(JsonParser.parseString("{\"wear\":\"none\"}").getAsJsonObject());
        h.assertTrue(none.none(),"Explicit wear=none is classified without work");
        var builtin=WearRules.parse(JsonParser.parseString("{\"work_per_damage\":16,\"sources\":[{\"type\":\"builtin\",\"detector\":\"alchemical_leather:jump_boost_jump\"}]}").getAsJsonObject());
        h.assertTrue(!builtin.none()&&builtin.workPerDamage()==16.0&&builtin.work("builtin",JUMP,2.0)==2.0,"Builtin detector parses with deterministic work");
        var event=WearRules.parse(JsonParser.parseString("{\"work_per_damage\":30,\"sources\":[{\"type\":\"event\",\"event\":\"clinging_reoriented:gravity_turn\",\"work\":2}]}").getAsJsonObject());
        h.assertTrue(event.work("event",Identifier.parse("clinging_reoriented:gravity_turn"),1.0)==2.0,"Semantic event weight parses independently");
        h.succeed();
    }

    @GameTest public void alchemicalWearBreaksArmorNormally(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);
        var effectId=Identifier.withDefaultNamespace("jump_boost");
        leggings.set(Infusions.TYPE,new Infusion(effectId,0,"stable",0));
        leggings.setDamageValue(leggings.getMaxDamage()-1);
        p.setItemSlot(EquipmentSlot.LEGS,leggings);
        var ledger=EffectLedger.of(p);ledger.equipmentManaged=true;ledger.setArmor(new MobEffectInstance(MobEffects.JUMP_BOOST,-1));
        for(int i=0;i<16;i++)InfusionWear.emitBuiltin(p,MobEffects.JUMP_BOOST,JUMP,1.0);
        h.assertTrue(p.getItemBySlot(EquipmentSlot.LEGS).isEmpty(),"Alchemical wear reaches zero and breaks the armor instead of stopping at one durability");
        h.succeed();
    }

    @GameTest public void explicitUnbreakableArmorIsNotBypassed(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);
        var effectId=Identifier.withDefaultNamespace("jump_boost");
        leggings.set(Infusions.TYPE,new Infusion(effectId,0,"stable",0));
        leggings.set(DataComponents.UNBREAKABLE,Unit.INSTANCE);
        p.setItemSlot(EquipmentSlot.LEGS,leggings);
        var ledger=EffectLedger.of(p);ledger.equipmentManaged=true;ledger.setArmor(new MobEffectInstance(MobEffects.JUMP_BOOST,-1));
        h.assertFalse(InfusionWear.hasVanillaDamageBar(leggings),"Vanilla UNBREAKABLE remains authoritative over the compatibility bypass");
        for(int i=0;i<64;i++)InfusionWear.emitBuiltin(p,MobEffects.JUMP_BOOST,JUMP,1.0);
        h.assertTrue(leggings.getDamageValue()==0&&!leggings.has(Infusions.WEAR_TYPE),"Unbreakable infused armor accumulates neither damage nor hidden alchemical debt");
        h.succeed();
    }

    @GameTest public void strongerExternalEffectPreventsArmorWear(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);
        var effectId=Identifier.withDefaultNamespace("jump_boost");
        leggings.set(Infusions.TYPE,new Infusion(effectId,0,"stable",0));
        p.setItemSlot(EquipmentSlot.LEGS,leggings);
        var ledger=EffectLedger.of(p);ledger.equipmentManaged=true;ledger.setArmor(new MobEffectInstance(MobEffects.JUMP_BOOST,-1,0));
        ledger.externalAdd(new MobEffectInstance(MobEffects.JUMP_BOOST,1200,1),true);ledger.reconcile();
        h.assertFalse(ledger.armorEffective(MobEffects.JUMP_BOOST),"Stronger external Jump Boost wins projection");
        for(int i=0;i<32;i++)InfusionWear.emitBuiltin(p,MobEffects.JUMP_BOOST,JUMP,1.0);
        h.assertTrue(leggings.getDamageValue()==0,"Eclipsed armor source pays no durability");
        h.succeed();
    }

    @GameTest public void equalExternalEffectWinsTiesAndPreventsArmorWear(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);
        var effectId=Identifier.withDefaultNamespace("jump_boost");
        leggings.set(Infusions.TYPE,new Infusion(effectId,0,"timed",600));
        p.setItemSlot(EquipmentSlot.LEGS,leggings);
        var ledger=EffectLedger.of(p);ledger.equipmentManaged=true;
        ledger.setArmor(new MobEffectInstance(MobEffects.JUMP_BOOST,600,0));
        ledger.externalAdd(new MobEffectInstance(MobEffects.JUMP_BOOST,600,0),true);ledger.reconcile();
        h.assertFalse(ledger.armorEffective(MobEffects.JUMP_BOOST),"Equal amplifier and equal duration resolve to the external source");
        for(int i=0;i<32;i++)InfusionWear.emitBuiltin(p,MobEffects.JUMP_BOOST,JUMP,1.0);
        h.assertTrue(leggings.getDamageValue()==0,"Equal winning external source prevents duplicate armor wear");
        h.succeed();
    }

    @GameTest public void longerArmorSourceMayStillBeEffectiveAtEqualAmplifier(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);
        var effectId=Identifier.withDefaultNamespace("jump_boost");
        leggings.set(Infusions.TYPE,new Infusion(effectId,0,"timed",1200));
        p.setItemSlot(EquipmentSlot.LEGS,leggings);
        var ledger=EffectLedger.of(p);ledger.equipmentManaged=true;
        ledger.setArmor(new MobEffectInstance(MobEffects.JUMP_BOOST,1200,0));
        ledger.externalAdd(new MobEffectInstance(MobEffects.JUMP_BOOST,600,0),true);ledger.reconcile();
        h.assertTrue(ledger.armorEffective(MobEffects.JUMP_BOOST),"At equal amplifier the longer armor source remains the projected source");
        for(int i=0;i<16;i++)InfusionWear.emitBuiltin(p,MobEffects.JUMP_BOOST,JUMP,1.0);
        h.assertTrue(leggings.getDamageValue()==1,"Effective longer armor source pays its own causal wear");
        h.succeed();
    }

    @GameTest public void weaknessWorkUsesClampedCounterfactualAttackDamage(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,0));
        var weakness=p.getEffect(MobEffects.WEAKNESS);
        h.assertTrue(weakness!=null,"Weakness is active for the counterfactual test");
        double live=p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double without=EffectAttributes.without(p,Attributes.ATTACK_DAMAGE,weakness);
        double suppression=Math.max(0.0,-EffectAttributes.contribution(p,Attributes.ATTACK_DAMAGE,weakness));
        h.assertTrue(Math.abs(suppression-(without-live))<1.0E-9,"Weakness work equals the actual clamped attribute delta");
        h.assertTrue(suppression<=without+1.0E-9,"Weakness cannot suppress more attack damage than existed without it");
        h.assertTrue(suppression<4.0,"Bare-hand Weakness I is clamped below its nominal -4 modifier");
        h.succeed();
    }

    @GameTest public void turtleMasterBecomesAtomicHumanoidBundle(GameTestHelper h){
        var contents=new PotionContents(Optional.empty(),Optional.empty(),List.of(
            new MobEffectInstance(MobEffects.SLOWNESS,400,3),new MobEffectInstance(MobEffects.RESISTANCE,400,2)),Optional.empty());
        var result=ArmorInfusionService.apply(new ItemStack(Items.LEATHER_LEGGINGS),contents,Items.LINGERING_POTION,false);
        h.assertTrue(result.ok(),"A multi-effect potion whose effects share one humanoid slot is accepted atomically");
        var bundle=result.stack().get(Infusions.HUMANOID_TYPE);
        h.assertTrue(bundle!=null&&bundle.effects().size()==2&&!result.stack().has(Infusions.TYPE)&&!result.stack().has(Infusions.ANIMAL_TYPE),"Humanoid bundle is the sole infusion representation");
        for(var infusion:bundle.effects())h.assertTrue(infusion.mode().equals("stable"),"Lingering multi-effect potion keeps stable semantics per effect");
        h.succeed();
    }
}
