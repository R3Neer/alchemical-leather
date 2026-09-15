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
}
