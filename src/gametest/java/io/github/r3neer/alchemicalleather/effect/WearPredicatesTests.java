package io.github.r3neer.alchemicalleather.effect;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Adversarial holdouts for causal environmental wear gates. */
public final class WearPredicatesTests {
    @GameTest public void waterBreathingOnlyPaysWhenItIsTheButForBreathingCause(GameTestHelper h){
        h.assertTrue(WearPredicates.waterBreathingNeeded(false,false,false,true,false),
            "Submerged ordinary wearer with no alternate breathing needs Water Breathing");
        h.assertFalse(WearPredicates.waterBreathingNeeded(true,false,false,true,false),
            "Natural underwater breathing makes Water Breathing redundant");
        h.assertFalse(WearPredicates.waterBreathingNeeded(false,true,false,true,false),
            "Creative invulnerability makes drowning prevention redundant");
        h.assertFalse(WearPredicates.waterBreathingNeeded(false,false,true,true,false),
            "Conduit Power/Breath of the Nautilus makes Water Breathing non-causal");
        h.assertFalse(WearPredicates.waterBreathingNeeded(false,false,false,false,false),
            "Dry eyes perform no Water Breathing work");
        h.assertFalse(WearPredicates.waterBreathingNeeded(false,false,false,true,true),
            "Bubble-column breathing is not Water Breathing work");
        h.succeed();
    }

    @GameTest public void slowFallingOnlyPaysWhenItsClampChangesGravity(GameTestHelper h){
        h.assertTrue(WearPredicates.slowFallingChangesGravity(0.08,-0.2),
            "Default descending gravity is reduced by Slow Falling");
        h.assertTrue(WearPredicates.slowFallingChangesGravity(0.08,0.0),
            "Vanilla treats zero vertical velocity as falling for the gravity clamp");
        h.assertFalse(WearPredicates.slowFallingChangesGravity(0.01,-0.2),
            "A gravity source already at the Slow Falling clamp makes it redundant");
        h.assertFalse(WearPredicates.slowFallingChangesGravity(0.005,-0.2),
            "Lower foreign gravity must not be charged to Slow Falling");
        h.assertFalse(WearPredicates.slowFallingChangesGravity(0.08,0.1),
            "Ascending motion does not use Slow Falling's gravity branch");
        h.assertFalse(WearPredicates.slowFallingChangesGravity(Double.NaN,-0.2),
            "Malformed gravity cannot create wear");
        h.succeed();
    }
}
