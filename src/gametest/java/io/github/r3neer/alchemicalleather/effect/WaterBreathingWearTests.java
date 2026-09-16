package io.github.r3neer.alchemicalleather.effect;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Reserved counterfactual holdouts for Water Breathing's 26.2 air-refill branch. */
public final class WaterBreathingWearTests {
    @GameTest public void nautilusRefillNeedsWaterBreathingUnlessConduitAlsoWorks(GameTestHelper h){
        h.assertTrue(WaterBreathingWear.refillNeeded(true,false),
            "Breath of the Nautilus alone suppresses underwater refill, so Water Breathing is causal");
        h.assertFalse(WaterBreathingWear.refillNeeded(false,false),
            "Without Nautilus vanilla refills air independently, so Water Breathing gets no credit");
        h.assertFalse(WaterBreathingWear.refillNeeded(true,true),
            "Conduit Power independently restores refill and eclipses Water Breathing attribution");
        h.succeed();
    }
}
