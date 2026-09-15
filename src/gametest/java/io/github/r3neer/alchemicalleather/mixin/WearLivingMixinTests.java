package io.github.r3neer.alchemicalleather.mixin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Adversarial holdouts for causal environmental wear gates. */
public final class WearLivingMixinTests {
    @GameTest public void waterBreathingOnlyPaysWhenItIsTheButForBreathingCause(GameTestHelper h){
        h.assertTrue(WearLivingMixin.alchemical$waterBreathingNeeded(false,false,false,true,false),
            "Submerged ordinary wearer with no alternate breathing needs Water Breathing");
        h.assertFalse(WearLivingMixin.alchemical$waterBreathingNeeded(true,false,false,true,false),
            "Natural underwater breathing makes Water Breathing redundant");
        h.assertFalse(WearLivingMixin.alchemical$waterBreathingNeeded(false,true,false,true,false),
            "Creative invulnerability makes drowning prevention redundant");
        h.assertFalse(WearLivingMixin.alchemical$waterBreathingNeeded(false,false,true,true,false),
            "Conduit Power/Breath of the Nautilus makes Water Breathing non-causal");
        h.assertFalse(WearLivingMixin.alchemical$waterBreathingNeeded(false,false,false,false,false),
            "Dry eyes perform no Water Breathing work");
        h.assertFalse(WearLivingMixin.alchemical$waterBreathingNeeded(false,false,false,true,true),
            "Bubble-column breathing is not Water Breathing work");
        h.succeed();
    }
}
