package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.AnimalInfusion;
import io.github.r3neer.alchemicalleather.data.HumanoidInfusion;
import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.GameType;

/** Holdouts for armor-source ownership and multi-source infusion clocks. */
public final class WearOwnershipTests {
    private static final Identifier JUMP=Identifier.fromNamespaceAndPath("alchemical_leather","jump_boost_jump");
    private static final Identifier JUMP_EFFECT=Identifier.withDefaultNamespace("jump_boost");

    @GameTest public void strongestArmorSourceOwnsItsOwnWear(GameTestHelper h){
        var wearer=h.makeMockPlayer(GameType.SURVIVAL);
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);
        leggings.set(Infusions.TYPE,new Infusion(JUMP_EFFECT,0,"stable",0));
        var body=new ItemStack(Items.WOLF_ARMOR);
        body.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(List.of(new Infusion(JUMP_EFFECT,1,"stable",0))));
        wearer.setItemSlot(EquipmentSlot.LEGS,leggings);
        wearer.setItemSlot(EquipmentSlot.BODY,body);

        h.assertTrue(InfusionWear.hasVanillaDamageBar(body),"Wolf armor supplies a real durability bar for the BODY ownership holdout");
        var ledger=EffectLedger.of(wearer);ledger.equipmentManaged=true;
        ledger.setArmor(EquipmentSlot.LEGS,new MobEffectInstance(MobEffects.JUMP_BOOST,-1,0));
        ledger.setArmor(EquipmentSlot.BODY,new MobEffectInstance(MobEffects.JUMP_BOOST,-1,1));
        h.assertTrue(ledger.armorOwner(MobEffects.JUMP_BOOST)==EquipmentSlot.BODY,
            "The stronger BODY infusion, not sync order, owns the projected effect");

        for(int i=0;i<16;i++)InfusionWear.emitBuiltin(wearer,MobEffects.JUMP_BOOST,JUMP,1.0);
        h.assertTrue(body.getDamageValue()==1,"Causal wear is billed to the BODY source that actually won arbitration");
        h.assertTrue(leggings.getDamageValue()==0,"A weaker duplicate armor source must not pay another item's work");
        h.succeed();
    }

    @GameTest public void duplicateArmorSourcesKeepIndependentClocks(GameTestHelper h){
        var wearer=h.makeMockPlayer(GameType.SURVIVAL);
        var ledger=EffectLedger.of(wearer);ledger.equipmentManaged=true;
        ledger.setArmor(EquipmentSlot.LEGS,new MobEffectInstance(MobEffects.JUMP_BOOST,100,0));
        ledger.setArmor(EquipmentSlot.BODY,new MobEffectInstance(MobEffects.JUMP_BOOST,200,1));
        ledger.afterTick();
        h.assertTrue(ledger.remaining(MobEffects.JUMP_BOOST,EquipmentSlot.LEGS)==99,
            "The hidden leggings source advances its own clock while equipped");
        h.assertTrue(ledger.remaining(MobEffects.JUMP_BOOST,EquipmentSlot.BODY)==199,
            "The projected BODY source advances independently of the hidden source");
        h.assertTrue(ledger.armorOwner(MobEffects.JUMP_BOOST)==EquipmentSlot.BODY,
            "Clock advancement does not change the stronger source owner");

        ledger.retainArmorSlots(Map.of(MobEffects.JUMP_BOOST,Set.of(EquipmentSlot.LEGS)));
        h.assertTrue(ledger.armorOwner(MobEffects.JUMP_BOOST)==EquipmentSlot.LEGS,
            "Removing the winning slot promotes the surviving source instead of retaining stale ownership");
        h.assertTrue(ledger.remaining(MobEffects.JUMP_BOOST,EquipmentSlot.BODY)==0,
            "Removed equipment no longer retains a hidden armor clock");
        h.succeed();
    }

    @GameTest public void externalWinnerEclipsesAllArmorWithoutPausingTheirClocks(GameTestHelper h){
        var wearer=h.makeMockPlayer(GameType.SURVIVAL);
        var ledger=EffectLedger.of(wearer);ledger.equipmentManaged=true;
        ledger.setArmor(EquipmentSlot.LEGS,new MobEffectInstance(MobEffects.JUMP_BOOST,100,0));
        ledger.setArmor(EquipmentSlot.BODY,new MobEffectInstance(MobEffects.JUMP_BOOST,200,1));
        ledger.externalAdd(new MobEffectInstance(MobEffects.JUMP_BOOST,2,2),true);
        ledger.reconcile();
        h.assertFalse(ledger.armorEffective(MobEffects.JUMP_BOOST),"Stronger external source eclipses every equipped armor source");
        h.assertTrue(ledger.armorOwner(MobEffects.JUMP_BOOST)==null,"No armor item owns wear while the external source wins");

        ledger.afterTick();
        h.assertTrue(ledger.remaining(MobEffects.JUMP_BOOST,EquipmentSlot.LEGS)==99,"Hidden LEGS clock keeps advancing under external eclipse");
        h.assertTrue(ledger.remaining(MobEffects.JUMP_BOOST,EquipmentSlot.BODY)==199,"Hidden BODY clock keeps advancing under external eclipse");
        h.assertFalse(ledger.armorEffective(MobEffects.JUMP_BOOST),"External source remains winner until its own clock expires");

        ledger.afterTick();
        h.assertTrue(ledger.remaining(MobEffects.JUMP_BOOST,EquipmentSlot.LEGS)==98,"LEGS clock remains independent after external expiry");
        h.assertTrue(ledger.remaining(MobEffects.JUMP_BOOST,EquipmentSlot.BODY)==198,"BODY clock remains independent after external expiry");
        h.assertTrue(ledger.armorEffective(MobEffects.JUMP_BOOST),"Best armor source resurfaces immediately when the external winner expires");
        h.assertTrue(ledger.armorOwner(MobEffects.JUMP_BOOST)==EquipmentSlot.BODY,"The stronger surviving BODY source regains ownership");
        h.assertTrue(wearer.getEffect(MobEffects.JUMP_BOOST)!=null&&wearer.getEffect(MobEffects.JUMP_BOOST).getAmplifier()==1,
            "Visible projection returns to the strongest equipped armor source");
        h.succeed();
    }

    @GameTest public void repeatedEffectEntriesRemainBodyOnlySemantics(GameTestHelper h){
        var contents=new PotionContents(Optional.empty(),Optional.empty(),List.of(
            new MobEffectInstance(MobEffects.JUMP_BOOST,200,0),
            new MobEffectInstance(MobEffects.JUMP_BOOST,400,1)),Optional.empty());
        var body=Infusions.resolveAll(contents,Items.POTION);
        h.assertTrue(body.ok()&&body.infusion().effects().size()==2,
            "BODY keeps repeated effect entries as independent potion clocks");
        var humanoid=Infusions.resolveHumanoid(contents,Items.POTION,EquipmentSlot.LEGS);
        h.assertFalse(humanoid.ok(),"Humanoid bundles reject repeated identities they cannot clock independently");
        h.assertTrue("duplicate_effect".equals(humanoid.error()),"Duplicate humanoid rejection is explicit and atomic");
        h.assertFalse(new HumanoidInfusion(List.of(
            new Infusion(JUMP_EFFECT,0,"timed",200),new Infusion(JUMP_EFFECT,1,"timed",400))).valid(),
            "Persisted humanoid bundles also reject duplicate effect identities");
        h.succeed();
    }
}
