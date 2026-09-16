package io.github.r3neer.alchemicalleather.effect;

/** Counterfactual gates for Water Breathing's less obvious 26.2 air-supply behavior. */
public final class WaterBreathingWear {
    private WaterBreathingWear(){}

    /**
     * Breath of the Nautilus prevents drowning but, by itself, suppresses underwater air refill.
     * Water Breathing is therefore the but-for cause of refill only when Nautilus is present and
     * Conduit Power is not independently enabling the same refill.
     */
    public static boolean refillNeeded(boolean breathOfTheNautilus,boolean conduitPower){
        return breathOfTheNautilus&&!conduitPower;
    }
}
