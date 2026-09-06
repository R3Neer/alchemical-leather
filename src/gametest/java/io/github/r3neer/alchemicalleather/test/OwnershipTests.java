package io.github.r3neer.alchemicalleather.test;
import io.github.r3neer.alchemicalleather.effect.EffectLedger;
import io.github.r3neer.alchemicalleather.mixin.LivingEffectsAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.LivingEntity;
public class OwnershipTests {
    private void ticks(LivingEntity p,int count) { for(int i=0;i<count;i++) ((LivingEffectsAccess)p).alchemical$tickEffects(); }
    @GameTest public void strongerExternalSurvives(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        p.addEffect(new MobEffectInstance(MobEffects.SPEED,200,1));
        var ledger=EffectLedger.of(p); ledger.setArmor(new MobEffectInstance(MobEffects.SPEED,3600,0));
        ticks(p,60); ledger.removeArmor(MobEffects.SPEED);
        h.assertTrue(p.getEffect(MobEffects.SPEED).getDuration()==140,"External clock must remain 140"); h.succeed();
    }
    @GameTest public void externalAddedUnderStrongerArmor(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL); var ledger=EffectLedger.of(p);
        ledger.setArmor(new MobEffectInstance(MobEffects.SPEED,-1,2));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED,200,0)); ticks(p,60);
        ledger.removeArmor(MobEffects.SPEED);
        h.assertTrue(p.getEffect(MobEffects.SPEED).getAmplifier()==0 && p.getEffect(MobEffects.SPEED).getDuration()==140,"Hidden external survives"); h.succeed();
    }
    @GameTest public void equalLevelsDontExtendExternal(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL); var ledger=EffectLedger.of(p);
        ledger.setArmor(new MobEffectInstance(MobEffects.SPEED,3600,0));
        p.addEffect(new MobEffectInstance(MobEffects.SPEED,200,0)); ticks(p,60); ledger.removeArmor(MobEffects.SPEED);
        h.assertTrue(p.getEffect(MobEffects.SPEED).getDuration()==140,"Equal external not extended"); h.succeed();
    }
    @GameTest public void milkDoesNotResurrectExternal(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL); var ledger=EffectLedger.of(p);
        p.addEffect(new MobEffectInstance(MobEffects.SPEED,200,0)); ledger.setArmor(new MobEffectInstance(MobEffects.SPEED,-1,1));
        p.removeAllEffects(); ticks(p,1); ledger.removeArmor(MobEffects.SPEED);
        h.assertFalse(p.hasEffect(MobEffects.SPEED),"Milk removed external permanently");h.succeed();
    }
    @GameTest public void hiddenChainSurvives(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL); var ledger=EffectLedger.of(p);
        p.addEffect(new MobEffectInstance(MobEffects.SPEED,300,0)); p.addEffect(new MobEffectInstance(MobEffects.SPEED,40,1));
        ledger.setArmor(new MobEffectInstance(MobEffects.SPEED,-1,2)); ticks(p,10); ledger.removeArmor(MobEffects.SPEED); ticks(p,40);
        h.assertTrue(p.getEffect(MobEffects.SPEED).getAmplifier()==0 && p.getEffect(MobEffects.SPEED).getDuration()==250,"External hidden chain and countdown preserved"); h.succeed();
    }
    @GameTest public void savedEffectsExcludeArmor(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL); var ledger=EffectLedger.of(p);
        ledger.setArmor(new MobEffectInstance(MobEffects.SPEED,-1,2));
        h.assertTrue(ledger.externalForSave().isEmpty(),"Armor never saved as external");
        p.addEffect(new MobEffectInstance(MobEffects.SPEED,200,0));ticks(p,50);
        h.assertTrue(ledger.externalForSave().getFirst().getDuration()==150,"Saved external clock");h.succeed();
    }
}