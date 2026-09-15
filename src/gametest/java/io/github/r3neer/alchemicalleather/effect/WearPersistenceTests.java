package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.AnimalInfusion;
import io.github.r3neer.alchemicalleather.data.ArmorInfusionService;
import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import io.github.r3neer.alchemicalleather.data.WearProgress;
import io.github.r3neer.alchemicalleather.mixin.LivingEffectsAccess;
import java.util.List;
import java.util.Optional;
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

/** Persistence and cleanup holdouts for fractional alchemical wear stored on armor items. */
public final class WearPersistenceTests {
    private static final Identifier SPEED=Identifier.withDefaultNamespace("speed");
    private static final Identifier SLOWNESS=Identifier.withDefaultNamespace("slowness");
    private static final Identifier JUMP=Identifier.withDefaultNamespace("jump_boost");
    private static final Identifier INSTANT_HEALTH=Identifier.withDefaultNamespace("instant_health");
    private static final Identifier JUMP_DETECTOR=Identifier.fromNamespaceAndPath("alchemical_leather","jump_boost_jump");

    @GameTest public void reinfusionClearsPriorEffectWearDebtAtomically(GameTestHelper h){
        var original=new ItemStack(Items.LEATHER_LEGGINGS);
        original.set(Infusions.TYPE,new Infusion(SPEED,0,"stable",0));
        original.set(Infusions.WEAR_TYPE,new WearProgress(List.of(new WearProgress.Entry(SPEED,123.5))));
        var contents=new PotionContents(Optional.empty(),Optional.empty(),
            List.of(new MobEffectInstance(MobEffects.JUMP_BOOST,200,0)),Optional.empty());

        var result=ArmorInfusionService.apply(original,contents,Items.POTION,false);
        h.assertTrue(result.ok(),"Compatible reinfusion succeeds");
        h.assertTrue(result.stack().get(Infusions.TYPE)!=null&&result.stack().get(Infusions.TYPE).effect().equals(JUMP),
            "Result contains only the replacement infusion");
        h.assertFalse(result.stack().has(Infusions.WEAR_TYPE),"Replacement infusion starts without debt from the previous effect");
        h.assertTrue(original.has(Infusions.WEAR_TYPE)&&original.get(Infusions.TYPE).effect().equals(SPEED),
            "Pure transformation leaves the original stack untouched until the caller commits it");
        h.succeed();
    }

    @GameTest public void creativeWearDoesNotBankFractionalDebt(GameTestHelper h){
        var wearer=h.makeMockPlayer(GameType.CREATIVE);
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);
        leggings.set(Infusions.TYPE,new Infusion(JUMP,0,"stable",0));
        wearer.setItemSlot(EquipmentSlot.LEGS,leggings);
        var ledger=EffectLedger.of(wearer);ledger.equipmentManaged=true;
        ledger.setArmor(EquipmentSlot.LEGS,new MobEffectInstance(MobEffects.JUMP_BOOST,-1,0));

        // Jump Boost needs 16 work for one durability. Eight units used to leave hidden debt even
        // though creative correctly suppressed the eventual durability hit.
        for(int i=0;i<8;i++)InfusionWear.emitBuiltin(wearer,MobEffects.JUMP_BOOST,JUMP_DETECTOR,1.0);
        h.assertTrue(leggings.getDamageValue()==0,"Creative armor takes no alchemical durability damage");
        h.assertFalse(leggings.has(Infusions.WEAR_TYPE),"Creative use cannot bank debt for a later survival-mode bill");
        h.succeed();
    }

    @GameTest public void consumingInstantBodyEntryRetainsSiblingWear(GameTestHelper h){
        var wearer=h.makeMockPlayer(GameType.SURVIVAL);wearer.setHealth(10.0F);
        var body=new ItemStack(Items.WOLF_ARMOR);
        body.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(List.of(
            new Infusion(INSTANT_HEALTH,0,"instant",0),new Infusion(SPEED,0,"timed",200))));
        body.set(Infusions.WEAR_TYPE,new WearProgress(List.of(
            new WearProgress.Entry(INSTANT_HEALTH,3.0),new WearProgress.Entry(SPEED,17.0))));
        wearer.setItemSlot(EquipmentSlot.BODY,body);

        EquipmentInfusions.sync(wearer);
        var remaining=body.get(Infusions.ANIMAL_TYPE);
        var wear=body.get(Infusions.WEAR_TYPE);
        h.assertTrue(remaining!=null&&remaining.effects().size()==1&&remaining.effects().getFirst().effect().equals(SPEED),
            "Instant BODY entry is consumed without deleting its timed sibling");
        h.assertTrue(wear!=null&&Math.abs(wear.work(SPEED)-17.0)<1.0E-9&&wear.work(INSTANT_HEALTH)==0.0,
            "Wear cleanup removes only the consumed instant effect debt");
        h.succeed();
    }

    @GameTest public void expiringBodyEntryRetainsOtherEffectWear(GameTestHelper h){
        var wearer=h.makeMockPlayer(GameType.SURVIVAL);
        var body=new ItemStack(Items.WOLF_ARMOR);
        body.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(List.of(
            new Infusion(SPEED,0,"timed",1),new Infusion(SLOWNESS,0,"timed",5))));
        body.set(Infusions.WEAR_TYPE,new WearProgress(List.of(
            new WearProgress.Entry(SPEED,11.0),new WearProgress.Entry(SLOWNESS,13.0))));
        wearer.setItemSlot(EquipmentSlot.BODY,body);
        EquipmentInfusions.sync(wearer);

        ((LivingEffectsAccess)wearer).alchemical$tickEffects();
        var remaining=body.get(Infusions.ANIMAL_TYPE);
        var wear=body.get(Infusions.WEAR_TYPE);
        h.assertTrue(remaining!=null&&remaining.effects().size()==1&&remaining.effects().getFirst().effect().equals(SLOWNESS),
            "Only the one-tick BODY effect expires");
        h.assertTrue(remaining.effects().getFirst().remainingTicks()==4,"Sibling BODY clock advances exactly once");
        h.assertTrue(wear!=null&&wear.work(SPEED)==0.0&&Math.abs(wear.work(SLOWNESS)-13.0)<1.0E-9,
            "Expiration prunes only the expired effect's fractional wear debt");
        h.succeed();
    }
}
