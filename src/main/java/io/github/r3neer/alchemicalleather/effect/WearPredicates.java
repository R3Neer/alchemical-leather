package io.github.r3neer.alchemicalleather.effect;

/** Pure causal gates shared by runtime detectors and adversarial tests. */
public final class WearPredicates {
    private static final double MOVEMENT_EPSILON=1.0E-8;
    private static final double BASE_AIR_DRAG=0.91;

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

    /** Speed/Slowness use MOVEMENT_SPEED for ordinary grounded travel, not airborne travel. */
    public static boolean movementSpeedGroundEligible(boolean passenger,boolean fallFlying,boolean inWater,
                                                      boolean inLava,boolean onGround,double inputHorizontalSqr){
        return !passenger&&!fallFlying&&!inWater&&!inLava&&onGround&&Double.isFinite(inputHorizontalSqr)
            &&inputHorizontalSqr>MOVEMENT_EPSILON;
    }

    /**
     * Converts only the horizontal velocity added by moveRelative into an upper bound on the
     * distance attributable to that self-propelled impulse. Existing velocity cancels out, so
     * knockback/platform momentum cannot inflate the bound. The geometric tail mirrors vanilla's
     * post-move ground drag. Degenerate zero-drag states conservatively charge only this tick's
     * impulse instead of manufacturing an infinite distance.
     */
    public static double groundImpulseDistanceLimit(double beforeX,double beforeZ,double afterX,double afterZ,
                                                    double blockFriction,double airDragModifier){
        if(!finite(beforeX,beforeZ,afterX,afterZ,blockFriction,airDragModifier))return 0.0;
        double impulse=Math.hypot(afterX-beforeX,afterZ-beforeZ);
        if(impulse<=MOVEMENT_EPSILON)return 0.0;
        double airDrag=clamp(1.0-(1.0-BASE_AIR_DRAG)*airDragModifier,0.0,1.0);
        double drag=clamp(blockFriction*airDrag,0.0,1.0);
        if(drag>=0.999999)return impulse;
        double limit=impulse/(1.0-drag);
        return Double.isFinite(limit)&&limit>0.0?limit:0.0;
    }

    /** The actual travel distance prevents blocked input from creating wear. */
    public static double attributableMovementDistance(double actualHorizontalDistance,double impulseDistanceLimit){
        if(!Double.isFinite(actualHorizontalDistance)||!Double.isFinite(impulseDistanceLimit)
            ||actualHorizontalDistance<=MOVEMENT_EPSILON||impulseDistanceLimit<=MOVEMENT_EPSILON)return 0.0;
        return Math.min(actualHorizontalDistance,impulseDistanceLimit);
    }

    private static boolean finite(double... values){
        for(double value:values)if(!Double.isFinite(value))return false;
        return true;
    }

    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
}
