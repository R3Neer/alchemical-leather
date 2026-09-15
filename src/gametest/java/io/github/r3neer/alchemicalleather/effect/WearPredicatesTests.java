package io.github.r3neer.alchemicalleather.effect;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Adversarial holdouts for causal environmental wear gates. */
public final class WearPredicatesTests {
    private static final double EPS=1.0E-9;

    @GameTest public void waterBreathingOnlyPaysWhenItIsTheButForBreathingCause(GameTestHelper h){
        h.assertTrue(WearPredicates.waterBreathingNeeded(false,false,false,true,false),
            "Submerged ordinary wearer with no alternate breathing needs Water Breathing");
        h.assertFalse(WearPredicates.waterBreathingNeeded(true,false,false,true,false),
            "Natural underwater breathing makes Water Breathing redundant");
        h.assertFalse(WearPredicates.waterBreathingNeeded(false,true,false,true,false),
            "Creative invulnerability makes drowning prevention redundant");
        h.assertFalse(WearPredicates.waterBreathingNeeded(false,false,true,true,false),
            "Conduit Power/Breath of the Nautilus makes Water Breathing non-causal");
        h.assertFalse(WearPredicates.waterBreathingNeeded(false,false,false,false,false),
            "Dry eyes perform no Water Breathing work");
        h.assertFalse(WearPredicates.waterBreathingNeeded(false,false,false,true,true),
            "Bubble-column breathing is not Water Breathing work");
        h.succeed();
    }

    @GameTest public void slowFallingOnlyPaysWhenItsClampChangesGravity(GameTestHelper h){
        h.assertTrue(WearPredicates.slowFallingChangesGravity(0.08,-0.2),
            "Default descending gravity is reduced by Slow Falling");
        h.assertTrue(WearPredicates.slowFallingChangesGravity(0.08,0.0),
            "Vanilla treats zero vertical velocity as falling for the gravity clamp");
        h.assertFalse(WearPredicates.slowFallingChangesGravity(0.01,-0.2),
            "A gravity source already at the Slow Falling clamp makes it redundant");
        h.assertFalse(WearPredicates.slowFallingChangesGravity(0.005,-0.2),
            "Lower foreign gravity must not be charged to Slow Falling");
        h.assertFalse(WearPredicates.slowFallingChangesGravity(0.08,0.1),
            "Ascending motion does not use Slow Falling's gravity branch");
        h.assertFalse(WearPredicates.slowFallingChangesGravity(Double.NaN,-0.2),
            "Malformed gravity cannot create wear");
        h.succeed();
    }

    @GameTest public void movementSpeedOnlyMetersGroundedSelfPropulsion(GameTestHelper h){
        h.assertTrue(WearPredicates.movementSpeedGroundEligible(false,false,false,false,true,1.0),
            "Grounded voluntary input is eligible for Speed/Slowness wear");
        h.assertFalse(WearPredicates.movementSpeedGroundEligible(false,false,false,false,false,1.0),
            "Airborne input must not pay because ordinary air travel does not use MOVEMENT_SPEED");
        h.assertFalse(WearPredicates.movementSpeedGroundEligible(true,false,false,false,true,1.0),
            "Riding transport is not wearer locomotion");
        h.assertFalse(WearPredicates.movementSpeedGroundEligible(false,true,false,false,true,1.0),
            "Elytra travel is not movement-speed work");
        h.assertFalse(WearPredicates.movementSpeedGroundEligible(false,false,true,false,true,1.0),
            "Water travel is handled only by its causal Depth Strider bridge");
        h.assertFalse(WearPredicates.movementSpeedGroundEligible(false,false,false,true,true,1.0),
            "Lava travel uses a separate movement path and is not charged by the ground detector");
        h.assertFalse(WearPredicates.movementSpeedGroundEligible(false,false,false,false,true,0.0),
            "Passive displacement without input must not pay");
        h.succeed();
    }

    @GameTest public void waterMovementOnlyPaysWhenMovementSpeedActuallyFeedsSwimming(GameTestHelper h){
        h.assertFalse(WearPredicates.movementSpeedWaterEligible(false,false,true,0.0,1.0),
            "Swimming without Water Movement Efficiency does not use MOVEMENT_SPEED");
        h.assertTrue(WearPredicates.movementSpeedWaterEligible(false,false,true,1.0/3.0,1.0),
            "Depth Strider bridges MOVEMENT_SPEED into swimming and makes Speed/Slowness causal");
        h.assertFalse(WearPredicates.movementSpeedWaterEligible(true,false,true,1.0,1.0),
            "Passenger transport must not become swimming wear");
        h.assertFalse(WearPredicates.movementSpeedWaterEligible(false,true,true,1.0,1.0),
            "Fall-flying is never charged through the water detector");
        h.assertFalse(WearPredicates.movementSpeedWaterEligible(false,false,true,1.0,0.0),
            "Water currents without voluntary horizontal input do not count");

        double drag=WearPredicates.waterMovementDrag(false,0.8,1.0,false,false);
        double expectedDrag=0.8+(0.54600006-0.8)*0.5;
        h.assertTrue(Math.abs(drag-expectedDrag)<EPS,
            "Airborne Depth Strider efficiency must be halved exactly like vanilla water travel");
        h.assertTrue(Math.abs(WearPredicates.waterMovementDrag(false,0.8,1.0,false,true)-0.96)<EPS,
            "Dolphin's Grace overrides horizontal water drag after Depth Strider blending");

        double limit=WearPredicates.waterImpulseDistanceLimit(4.0,0.0,4.02,0.0,0.8);
        h.assertTrue(Math.abs(limit-0.1)<EPS,
            "Existing current/knockback momentum must cancel from the water self-propulsion bound");
        h.assertTrue(Math.abs(WearPredicates.attributableMovementDistance(4.0,limit)-0.1)<EPS,
            "Large water displacement cannot be charged beyond the player's own swim impulse");
        h.succeed();
    }

    @GameTest public void externalMomentumCannotInflateMovementWear(GameTestHelper h){
        // Existing 5 blocks/tick of external momentum cancels: only the +0.1 moveRelative impulse matters.
        double limit=WearPredicates.groundImpulseDistanceLimit(5.0,0.0,5.1,0.0,0.6,1.0);
        double expected=0.1/(1.0-0.546);
        h.assertTrue(Math.abs(limit-expected)<EPS,
            "Impulse distance must depend on the self acceleration, not pre-existing knockback velocity");
        double hugeActual=WearPredicates.attributableMovementDistance(5.0,limit);
        h.assertTrue(Math.abs(hugeActual-limit)<EPS,
            "Large external displacement must be capped by the self-propelled impulse contribution");
        double blocked=WearPredicates.attributableMovementDistance(0.0,limit);
        h.assertTrue(blocked==0.0,"Walking into a wall must not create movement wear");
        double shortMove=WearPredicates.attributableMovementDistance(0.05,limit);
        h.assertTrue(Math.abs(shortMove-0.05)<EPS,
            "Real movement shorter than the impulse bound must cap attributable work");
        h.assertTrue(WearPredicates.groundImpulseDistanceLimit(Double.NaN,0.0,0.1,0.0,0.6,1.0)==0.0,
            "Malformed velocity cannot create wear");
        h.succeed();
    }
}
