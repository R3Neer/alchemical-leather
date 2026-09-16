package io.github.r3neer.alchemicalleather.effect;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Reserved holdouts for the shared hurtServer cooldown gate used by protective wear detectors. */
public final class DamageCooldownWearTests {
    @GameTest public void fireResistanceOnlyOwnsDamageThatWouldPassIFrames(GameTestHelper h){
        h.assertTrue(WearPredicates.damagePassingCooldown(1.0D,20,2.0F,false)==0.0D,
            "A smaller fire hit fully rejected by active i-frames is not Fire Resistance work");
        h.assertTrue(WearPredicates.damagePassingCooldown(3.0D,20,2.0F,false)==1.0D,
            "Only the increment above lastHurt would reach actuallyHurt during active i-frames");
        h.assertTrue(WearPredicates.damagePassingCooldown(1.0D,20,2.0F,true)==1.0D,
            "A cooldown-bypassing source keeps its full causal damage amount");
        h.assertTrue(WearPredicates.damagePassingCooldown(3.0D,0,2.0F,false)==3.0D,
            "Outside the i-frame window the full incoming fire damage remains causal");
        h.succeed();
    }
}
