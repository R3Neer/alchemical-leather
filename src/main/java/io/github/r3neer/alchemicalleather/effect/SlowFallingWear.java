package io.github.r3neer.alchemicalleather.effect;

/** Small causal helpers for Slow Falling mechanics that are separate from its gravity clamp. */
public final class SlowFallingWear {
    private static final float EPS=1.0E-6F;
    private SlowFallingWear(){}

    /**
     * In 26.2 Slow Falling and Levitation share the aiStep fall-distance reset branch. Credit Slow
     * Falling only when there was accumulated fall distance to erase, it was actually erased, and
     * Levitation would not have caused the same reset without it.
     */
    public static boolean fallDistanceResetNeeded(float before,float after,boolean levitation){
        return Float.isFinite(before)&&Float.isFinite(after)&&before>EPS&&after<=EPS&&!levitation;
    }
}
