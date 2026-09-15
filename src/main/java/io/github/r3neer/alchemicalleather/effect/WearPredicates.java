package io.github.r3neer.alchemicalleather.effect;

/** Pure causal gates shared by runtime detectors and adversarial tests. */
public final class WearPredicates {
    private WearPredicates(){}

    /**
     * True only when Water Breathing is the but-for reason vanilla will not advance drowning.
     * alternateBreathing covers independent effects such as Conduit Power and Breath of the Nautilus.
     */
    public static boolean waterBreathingNeeded(boolean naturalBreathing,boolean creativeInvulnerable,
                                               boolean alternateBreathing,boolean submergedEyes,boolean bubbleColumn){
        return !naturalBreathing&&!creativeInvulnerable&&!alternateBreathing&&submergedEyes&&!bubbleColumn;
    }

    /**
     * Vanilla 26.2 only changes gravity for Slow Falling while descending and only when the
     * entity's normal gravity is above the 0.01 clamp. Other movement exclusions are supplied by
     * the runtime detector because they describe where this mod intentionally meters the physics.
     */
    public static boolean slowFallingChangesGravity(double normalGravity,double verticalVelocity){
        return Double.isFinite(normalGravity)&&Double.isFinite(verticalVelocity)&&verticalVelocity<=0.0&&normalGravity>0.01;
    }
}
