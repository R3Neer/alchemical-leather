package io.github.r3neer.alchemicalleather.effect;

/** Small causal helpers for Slow Falling mechanics that are separate from its gravity clamp. */
public final class SlowFallingWear {
    private static final double EPS=1.0E-6D;
    private SlowFallingWear(){}

    /**
     * The fall-distance reset can only protect a genuinely falling entity. Grounded entities,
     * passengers and players in independent flight cannot owe future fall damage to this state;
     * Levitation independently performs the same reset.
     */
    public static boolean resetContextEligible(boolean grounded,boolean passenger,boolean independentFlight,boolean levitation){
        return !grounded&&!passenger&&!independentFlight&&!levitation;
    }

    /**
     * In 26.2 Slow Falling and Levitation share the aiStep fall-distance reset branch. Credit Slow
     * Falling only when there was accumulated fall distance to erase and it was actually erased.
     * Context-level independent causes are checked separately by resetContextEligible.
     */
    public static boolean fallDistanceResetNeeded(double before,double after,boolean levitation){
        return Double.isFinite(before)&&Double.isFinite(after)&&before>EPS&&after<=EPS&&!levitation;
    }
}
