package io.github.r3neer.alchemicalleather.effect;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Reserved holdouts for contexts where Slow Falling's fall-distance reset is not causal work. */
public final class SlowFallingContextTests {
    @GameTest public void resetIgnoresGroundPassengersFlightAndLevitation(GameTestHelper h){
        h.assertTrue(SlowFallingWear.resetContextEligible(false,false,false,false),
            "A genuinely falling free entity may owe work to Slow Falling's reset");
        h.assertFalse(SlowFallingWear.resetContextEligible(true,false,false,false),
            "Grounded residual fall distance is not future fall damage");
        h.assertFalse(SlowFallingWear.resetContextEligible(false,true,false,false),
            "Passenger fall state is transported by the vehicle, not Slow Falling");
        h.assertFalse(SlowFallingWear.resetContextEligible(false,false,true,false),
            "Independent player flight already owns fall-distance reset semantics");
        h.assertFalse(SlowFallingWear.resetContextEligible(false,false,false,true),
            "Levitation independently performs the same reset");
        h.succeed();
    }
}
