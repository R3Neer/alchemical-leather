package io.github.r3neer.alchemicalleather.effect;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Reserved holdouts for Strength/Weakness work across LivingEntity's damage cooldown. */
public final class CombatCooldownWearTests {
    @GameTest public void strengthAndWeaknessBillOnlyDamageTheyChangePastIFrames(GameTestHelper h){
        h.assertTrue(Math.abs(CombatWear.attackDeltaWork(6.0D,3.0D,20,5.0F,false)-1.0D)<1.0E-9,
            "Strength gets credit only for the one damage point that crosses lastHurt, not its full raw +3 contribution");
        h.assertTrue(Math.abs(CombatWear.attackDeltaWork(6.0D,3.0D,0,5.0F,false)-3.0D)<1.0E-9,
            "Outside the i-frame window the full Strength contribution remains causal");
        h.assertTrue(Math.abs(CombatWear.attackDeltaWork(6.0D,-4.0D,20,5.0F,false)-4.0D)<1.0E-9,
            "Weakness bills the four damage points it suppresses after the same cooldown comparison");
        h.assertTrue(CombatWear.attackDeltaWork(4.0D,3.0D,20,5.0F,false)==0.0D,
            "If both live and counterfactual hits are rejected by i-frames, Strength performs no work");
        h.assertTrue(Math.abs(CombatWear.attackDeltaWork(6.0D,3.0D,20,5.0F,true)-3.0D)<1.0E-9,
            "A source that bypasses cooldown preserves the full raw Strength contribution");
        h.succeed();
    }
}
