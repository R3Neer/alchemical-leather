package io.github.r3neer.alchemicalleather.effect;

/** Pure causal gates shared by runtime detectors and adversarial tests. */
public final class WearPredicates {
    private static final double MOVEMENT_EPSILON=1.0E-8;
    private static final double BASE_AIR_DRAG=0.91;
    private static final double DEPTH_STRIDER_TARGET_DRAG=0.54600006;
    private static final double WEB_NORMAL_XZ=0.25;
    private static final double WEB_NORMAL_Y=0.05;
    private static final double WEB_WEAVING_XZ=0.5;
    private static final double WEB_WEAVING_Y=0.25;

    private WearPredicates(){}

    /**
     * True only when Water Breathing is the but-for reason vanilla will not advance drowning.
     * alternateBreathing covers independent effects such as Conduit Power and Breath of the Nautilus.
     */
    public static boolean waterBreathingNeeded(boolean naturalBreathing,boolean creativeInvulnerable,
                                               boolean alternateBreathing,boolean submergedEyes,boolean bubbleColumn){
        return !naturalBreathing&&!creativeInvulnerable&&!alternateBreathing&&submergedEyes&&!bubbleColumn;
    }

    /** Vanilla's Slow Falling clamp can only change gravity while descending from >0.01 gravity. */
    public static boolean slowFallingChangesGravity(double normalGravity,double verticalVelocity){
        return Double.isFinite(normalGravity)&&Double.isFinite(verticalVelocity)&&verticalVelocity<=0.0&&normalGravity>0.01;
    }

    /**
     * Confirms that a completed getEffectiveGravity call actually returned Slow Falling's 0.01
     * clamp rather than merely observing an active effect. The caller separately decides whether
     * that gravity value is consumed by its movement path.
     */
    public static boolean slowFallingGravityApplied(double normalGravity,double verticalVelocity,double effectiveGravity){
        if(!finite(normalGravity,verticalVelocity,effectiveGravity)||!slowFallingChangesGravity(normalGravity,verticalVelocity))return false;
        return Math.abs(effectiveGravity-Math.min(normalGravity,0.01))<=MOVEMENT_EPSILON
            &&effectiveGravity<normalGravity-MOVEMENT_EPSILON;
    }

    /** Speed/Slowness use MOVEMENT_SPEED for ordinary grounded travel, not airborne travel. */
    public static boolean movementSpeedGroundEligible(boolean passenger,boolean fallFlying,boolean inWater,
                                                      boolean inLava,boolean onGround,double inputHorizontalSqr){
        return !passenger&&!fallFlying&&!inWater&&!inLava&&onGround&&hasInput(inputHorizontalSqr);
    }

    /**
     * Vanilla water travel only blends getSpeed() into moveRelative when WATER_MOVEMENT_EFFICIENCY
     * is positive (Depth Strider in vanilla). Without that bridge, Speed/Slowness do no water work.
     */
    public static boolean movementSpeedWaterEligible(boolean passenger,boolean fallFlying,boolean inWater,
                                                     double waterMovementEfficiency,double inputHorizontalSqr){
        return !passenger&&!fallFlying&&inWater&&Double.isFinite(waterMovementEfficiency)
            &&waterMovementEfficiency>0.0&&hasInput(inputHorizontalSqr);
    }

    /**
     * Converts causal movement distance to configured work only when the effect changes the live
     * MOVEMENT_SPEED attribute in the expected direction. Amplifier weighting is intentional balance:
     * stronger infusions spend more durability per block of otherwise identical causal locomotion.
     */
    public static double movementSpeedWork(double attributableDistance,double attributeContribution,
                                           int amplifier,boolean beneficial){
        if(!finite(attributableDistance,attributeContribution)||attributableDistance<=MOVEMENT_EPSILON)return 0.0;
        if(beneficial?attributeContribution<=MOVEMENT_EPSILON:attributeContribution>=-MOVEMENT_EPSILON)return 0.0;
        int level=Math.max(1,amplifier+1);
        double work=attributableDistance*level;
        return Double.isFinite(work)&&work>0.0?work:0.0;
    }

    /** Mirrors the horizontal drag chosen by LivingEntity#travelInWater in 26.2. */
    public static double waterMovementDrag(boolean sprinting,double baseSlowDown,double waterMovementEfficiency,
                                           boolean onGround,boolean dolphinsGrace){
        if(!finite(baseSlowDown,waterMovementEfficiency))return 0.0;
        double drag=sprinting?0.9:baseSlowDown;
        double efficiency=clamp(waterMovementEfficiency,0.0,1.0);
        if(!onGround)efficiency*=0.5;
        if(efficiency>0.0)drag+=(DEPTH_STRIDER_TARGET_DRAG-drag)*efficiency;
        if(dolphinsGrace)drag=0.96;
        return clamp(drag,0.0,1.0);
    }

    /**
     * True only for the exact stuck multiplier assigned by WebBlock when Weaving changes its
     * mechanics. In vanilla 26.2 this signature is unique to that WebBlock branch.
     */
    public static boolean isWeavingWebMultiplier(double x,double y,double z){
        return finite(x,y,z)&&Math.abs(x-WEB_WEAVING_XZ)<=MOVEMENT_EPSILON
            &&Math.abs(y-WEB_WEAVING_Y)<=MOVEMENT_EPSILON&&Math.abs(z-WEB_WEAVING_XZ)<=MOVEMENT_EPSILON;
    }

    /**
     * Measures only the part of a completed Entity.move that Weaving can have recovered from a
     * cobweb. Requested movement bounds the possible counterfactual benefit; actual displacement
     * removes work blocked by collisions. Per-axis caps also prevent step-up Y motion from being
     * mistaken for vertical Weaving work when the requested Y delta was zero.
     */
    public static double weavingRealizedBenefit(double requestedX,double requestedY,double requestedZ,
                                                double actualX,double actualY,double actualZ){
        if(!finite(requestedX,requestedY,requestedZ,actualX,actualY,actualZ))return 0.0;
        double bx=realizedAxisBenefit(requestedX,actualX,WEB_NORMAL_XZ,WEB_WEAVING_XZ);
        double by=realizedAxisBenefit(requestedY,actualY,WEB_NORMAL_Y,WEB_WEAVING_Y);
        double bz=realizedAxisBenefit(requestedZ,actualZ,WEB_NORMAL_XZ,WEB_WEAVING_XZ);
        double benefit=Math.sqrt(bx*bx+by*by+bz*bz);
        return Double.isFinite(benefit)&&benefit>MOVEMENT_EPSILON?benefit:0.0;
    }

    /**
     * Converts only the horizontal velocity added by moveRelative into an upper bound on the
     * distance attributable to that self-propelled impulse. Existing velocity cancels out, so
     * knockback/platform momentum cannot inflate the bound. The geometric tail mirrors vanilla's
     * post-move ground drag. Degenerate no-decay states conservatively charge only this tick's
     * impulse instead of manufacturing an infinite distance.
     */
    public static double groundImpulseDistanceLimit(double beforeX,double beforeZ,double afterX,double afterZ,
                                                    double blockFriction,double airDragModifier){
        if(!finite(beforeX,beforeZ,afterX,afterZ,blockFriction,airDragModifier))return 0.0;
        double airDrag=clamp(1.0-(1.0-BASE_AIR_DRAG)*airDragModifier,0.0,1.0);
        return impulseDistanceLimit(beforeX,beforeZ,afterX,afterZ,clamp(blockFriction*airDrag,0.0,1.0));
    }

    /** Same causal bound for water travel, using the exact horizontal water drag chosen by vanilla. */
    public static double waterImpulseDistanceLimit(double beforeX,double beforeZ,double afterX,double afterZ,double waterDrag){
        if(!finite(beforeX,beforeZ,afterX,afterZ,waterDrag))return 0.0;
        return impulseDistanceLimit(beforeX,beforeZ,afterX,afterZ,clamp(waterDrag,0.0,1.0));
    }

    /** The actual travel distance prevents blocked input from creating wear. */
    public static double attributableMovementDistance(double actualHorizontalDistance,double impulseDistanceLimit){
        if(!Double.isFinite(actualHorizontalDistance)||!Double.isFinite(impulseDistanceLimit)
            ||actualHorizontalDistance<=MOVEMENT_EPSILON||impulseDistanceLimit<=MOVEMENT_EPSILON)return 0.0;
        return Math.min(actualHorizontalDistance,impulseDistanceLimit);
    }

    private static double realizedAxisBenefit(double requested,double actual,double normalMultiplier,double weavingMultiplier){
        double possible=Math.abs(requested)*(weavingMultiplier-normalMultiplier);
        if(possible<=MOVEMENT_EPSILON)return 0.0;
        double realizedFraction=(weavingMultiplier-normalMultiplier)/weavingMultiplier;
        return Math.min(possible,Math.abs(actual)*realizedFraction);
    }

    private static double impulseDistanceLimit(double beforeX,double beforeZ,double afterX,double afterZ,double drag){
        double impulse=Math.hypot(afterX-beforeX,afterZ-beforeZ);
        if(impulse<=MOVEMENT_EPSILON)return 0.0;
        if(drag>=0.999999)return impulse;
        double limit=impulse/(1.0-drag);
        return Double.isFinite(limit)&&limit>0.0?limit:0.0;
    }

    private static boolean hasInput(double inputHorizontalSqr){
        return Double.isFinite(inputHorizontalSqr)&&inputHorizontalSqr>MOVEMENT_EPSILON;
    }

    private static boolean finite(double... values){
        for(double value:values)if(!Double.isFinite(value))return false;
        return true;
    }

    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
}
