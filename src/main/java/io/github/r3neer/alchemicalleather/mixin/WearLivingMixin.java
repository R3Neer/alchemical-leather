package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.EffectAttributes;
import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import io.github.r3neer.alchemicalleather.effect.SlowFallingWear;
import io.github.r3neer.alchemicalleather.effect.WaterBreathingWear;
import io.github.r3neer.alchemicalleather.effect.WearPredicates;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Generic causal wear detectors that can be expressed without effect-specific mod knowledge. */
@Mixin(LivingEntity.class)
public abstract class WearLivingMixin {
    private static final Identifier MOVEMENT=Identifier.fromNamespaceAndPath("alchemical_leather","self_propelled_movement_speed");
    private static final Identifier JUMP=Identifier.fromNamespaceAndPath("alchemical_leather","jump_boost_jump");
    private static final Identifier WATER_BREATHING=Identifier.fromNamespaceAndPath("alchemical_leather","water_breathing_tick");
    private static final Identifier SLOW_FALLING=Identifier.fromNamespaceAndPath("alchemical_leather","slow_falling_tick");

    @Shadow protected abstract float getWaterSlowDown();
    @Shadow protected abstract float getJumpPower();

    @Unique private Vec3 alchemical$travelOrigin;
    @Unique private Vec3 alchemical$preRelativeVelocity;
    @Unique private boolean alchemical$meterTravel;
    @Unique private double alchemical$movementDistanceLimit;
    @Unique private Vec3 alchemical$waterTravelOrigin;
    @Unique private Vec3 alchemical$waterPreRelativeVelocity;
    @Unique private boolean alchemical$meterWaterTravel;
    @Unique private double alchemical$waterDistanceLimit;
    @Unique private double alchemical$waterDrag;
    @Unique private double alchemical$jumpPreY=Double.NaN;
    @Unique private double alchemical$slowFallingPreAiStepFallDistance=Double.NaN;
    @Unique private long alchemical$slowFallingWearTick=Long.MIN_VALUE;

    @Inject(method="travel",at=@At("HEAD"))
    private void alchemical$beforeSelfMovement(Vec3 input,CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        alchemical$preRelativeVelocity=null;
        alchemical$movementDistanceLimit=0.0;
        double inputSqr=input==null?0.0:input.horizontalDistanceSqr();
        alchemical$meterTravel=!self.level().isClientSide()&&WearPredicates.movementSpeedGroundEligible(
            self.isPassenger(),self.isFallFlying(),self.isInWater(),self.isInLava(),self.onGround(),inputSqr);
        alchemical$travelOrigin=alchemical$meterTravel?self.position():null;
    }

    @Inject(method="handleRelativeFrictionAndCalculateMovement",at=@At("HEAD"))
    private void alchemical$beforeGroundAcceleration(Vec3 input,float friction,CallbackInfoReturnable<Vec3> cir){
        if(!alchemical$meterTravel)return;
        alchemical$preRelativeVelocity=((LivingEntity)(Object)this).getDeltaMovement();
    }

    @Inject(method="handleRelativeFrictionAndCalculateMovement",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V",shift=At.Shift.AFTER))
    private void alchemical$afterGroundAcceleration(Vec3 input,float friction,CallbackInfoReturnable<Vec3> cir){
        if(!alchemical$meterTravel||alchemical$preRelativeVelocity==null)return;
        var self=(LivingEntity)(Object)this;
        Vec3 after=self.getDeltaMovement();
        alchemical$movementDistanceLimit=WearPredicates.groundImpulseDistanceLimit(
            alchemical$preRelativeVelocity.x,alchemical$preRelativeVelocity.z,after.x,after.z,
            friction,self.getAttributeValue(Attributes.AIR_DRAG_MODIFIER));
    }

    @Inject(method="travel",at=@At("RETURN"))
    private void alchemical$selfMovement(Vec3 input,CallbackInfo ci){
        Vec3 origin=alchemical$travelOrigin;
        double limit=alchemical$movementDistanceLimit;
        boolean meter=alchemical$meterTravel;
        alchemical$meterTravel=false;
        alchemical$travelOrigin=null;
        alchemical$preRelativeVelocity=null;
        alchemical$movementDistanceLimit=0.0;
        if(!meter||origin==null)return;
        emitMovement((LivingEntity)(Object)this,origin,limit);
    }

    @Inject(method="travelInWater",at=@At("HEAD"))
    private void alchemical$beforeWaterMovement(Vec3 input,double baseGravity,boolean isFalling,double oldY,CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        alchemical$waterPreRelativeVelocity=null;
        alchemical$waterDistanceLimit=0.0;
        double efficiency=self.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
        double inputSqr=input==null?0.0:input.horizontalDistanceSqr();
        alchemical$meterWaterTravel=!self.level().isClientSide()&&WearPredicates.movementSpeedWaterEligible(
            self.isPassenger(),self.isFallFlying(),self.isInWater(),efficiency,inputSqr);
        if(!alchemical$meterWaterTravel){
            alchemical$waterTravelOrigin=null;
            return;
        }
        alchemical$waterTravelOrigin=self.position();
        alchemical$waterPreRelativeVelocity=self.getDeltaMovement();
        alchemical$waterDrag=WearPredicates.waterMovementDrag(
            self.isSprinting(),getWaterSlowDown(),efficiency,self.onGround(),self.hasEffect(MobEffects.DOLPHINS_GRACE));
    }

    @Inject(method="travelInWater",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;moveRelative(FLnet/minecraft/world/phys/Vec3;)V",shift=At.Shift.AFTER))
    private void alchemical$afterWaterAcceleration(Vec3 input,double baseGravity,boolean isFalling,double oldY,CallbackInfo ci){
        if(!alchemical$meterWaterTravel||alchemical$waterPreRelativeVelocity==null)return;
        Vec3 after=((LivingEntity)(Object)this).getDeltaMovement();
        alchemical$waterDistanceLimit=WearPredicates.waterImpulseDistanceLimit(
            alchemical$waterPreRelativeVelocity.x,alchemical$waterPreRelativeVelocity.z,after.x,after.z,alchemical$waterDrag);
    }

    @Inject(method="travelInWater",at=@At("RETURN"))
    private void alchemical$waterMovement(Vec3 input,double baseGravity,boolean isFalling,double oldY,CallbackInfo ci){
        Vec3 origin=alchemical$waterTravelOrigin;
        double limit=alchemical$waterDistanceLimit;
        boolean meter=alchemical$meterWaterTravel;
        alchemical$meterWaterTravel=false;
        alchemical$waterTravelOrigin=null;
        alchemical$waterPreRelativeVelocity=null;
        alchemical$waterDistanceLimit=0.0;
        alchemical$waterDrag=0.0;
        if(!meter||origin==null)return;
        emitMovement((LivingEntity)(Object)this,origin,limit);
    }

    @Inject(method="jumpFromGround",at=@At("HEAD"))
    private void alchemical$beforeJump(CallbackInfo ci){
        alchemical$jumpPreY=((LivingEntity)(Object)this).getDeltaMovement().y;
    }

    @Inject(method="jumpFromGround",at=@At("RETURN"))
    private void alchemical$jump(CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        double preY=alchemical$jumpPreY;
        alchemical$jumpPreY=Double.NaN;
        if(self.level().isClientSide()||self.isPassenger()||!Double.isFinite(preY))return;
        var effect=self.getEffect(MobEffects.JUMP_BOOST);
        if(effect==null)return;
        float boost=self.getJumpBoostPower();
        float boostedJumpPower=getJumpPower();
        double work=WearPredicates.jumpBoostWork(preY,boostedJumpPower,boost,self.getDeltaMovement().y);
        if(work>0.0)InfusionWear.emitBuiltin(self,effect.getEffect(),JUMP,work);
    }

    /** Capture fall distance before aiStep reaches Slow Falling/Levitation's shared reset branch. */
    @Inject(method="aiStep",at=@At("HEAD"))
    private void alchemical$beforeAiStep(CallbackInfo ci){
        alchemical$slowFallingPreAiStepFallDistance=((LivingEntity)(Object)this).fallDistance;
    }

    /**
     * The reset happens immediately before travel. Confirm that accumulated fall distance was
     * actually erased, exclude Levitation's independent cause, and share the same once-per-tick
     * meter as the gravity-clamp hooks so ordinary Slow Falling does not pay twice.
     */
    @Inject(method="aiStep",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;travel(Lnet/minecraft/world/phys/Vec3;)V"))
    private void alchemical$slowFallingFallDistanceReset(CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        double before=alchemical$slowFallingPreAiStepFallDistance;
        alchemical$slowFallingPreAiStepFallDistance=Double.NaN;
        if(self.level().isClientSide()||self.hasEffect(MobEffects.LEVITATION))return;
        var effect=self.getEffect(MobEffects.SLOW_FALLING);
        if(effect!=null&&SlowFallingWear.fallDistanceResetNeeded(before,self.fallDistance,false))
            alchemical$emitSlowFallingWork(self,effect);
    }

    /**
     * Wrap the exact water-breathing query inside LivingEntity#baseTick. Reaching this call means
     * the eyes are submerged, the eye block is not a bubble column, and natural underwater
     * breathing has already failed. We still exclude creative invulnerability and independent
     * Conduit/Nautilus breathing because those make Water Breathing non-causal for drowning.
     */
    @WrapOperation(method="baseTick",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/effect/MobEffectUtil;hasWaterBreathing(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean alchemical$waterBreathingDecision(LivingEntity entity,Operation<Boolean> original){
        boolean breathes=original.call(entity);
        if(!breathes||entity.level().isClientSide())return breathes;
        var effect=entity.getEffect(MobEffects.WATER_BREATHING);
        if(effect==null)return breathes;
        boolean creative=entity instanceof Player player&&player.getAbilities().invulnerable;
        boolean alternate=entity.hasEffect(MobEffects.CONDUIT_POWER)||entity.hasEffect(MobEffects.BREATH_OF_THE_NAUTILUS);
        if(!creative&&!alternate)InfusionWear.emitBuiltin(entity,effect.getEffect(),WATER_BREATHING,1.0);
        return breathes;
    }

    /**
     * Breath of the Nautilus independently prevents drowning but suppresses underwater air refill.
     * At this exact helper call the air supply is already known to be below max. If vanilla says
     * refill is allowed, Water Breathing is the but-for cause only when Nautilus is present and
     * Conduit Power is not independently enabling the same refill.
     */
    @WrapOperation(method="baseTick",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/effect/MobEffectUtil;shouldEffectsRefillAirsupply(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean alchemical$waterBreathingRefillDecision(LivingEntity entity,Operation<Boolean> original){
        boolean refill=original.call(entity);
        if(!refill||entity.level().isClientSide())return refill;
        var effect=entity.getEffect(MobEffects.WATER_BREATHING);
        if(effect==null)return refill;
        if(WaterBreathingWear.refillNeeded(entity.hasEffect(MobEffects.BREATH_OF_THE_NAUTILUS),entity.hasEffect(MobEffects.CONDUIT_POWER)))
            InfusionWear.emitBuiltin(entity,effect.getEffect(),WATER_BREATHING,1.0);
        return refill;
    }

    @WrapOperation(method="travelInAir",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getEffectiveGravity()D"))
    private double alchemical$slowFallingAirGravity(LivingEntity entity,Operation<Double> original,Vec3 input){
        double gravity=original.call(entity);
        alchemical$emitSlowFallingGravity(entity,gravity,true);
        return gravity;
    }

    @WrapOperation(method="updateFallFlyingMovement",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getEffectiveGravity()D"))
    private double alchemical$slowFallingElytraGravity(LivingEntity entity,Operation<Double> original,Vec3 movement){
        double gravity=original.call(entity);
        alchemical$emitSlowFallingGravity(entity,gravity,true);
        return gravity;
    }

    @WrapOperation(method="travelInFluid",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getEffectiveGravity()D"))
    private double alchemical$slowFallingFluidGravity(LivingEntity entity,Operation<Double> original,Vec3 input){
        double gravity=original.call(entity);
        // Water sprinting bypasses getFluidFallingAdjustedMovement's gravity subtraction. Lava
        // always consumes baseGravity in its final vertical adjustment.
        boolean consumed=entity.isInLava()||(entity.isInWater()&&!entity.isSprinting());
        alchemical$emitSlowFallingGravity(entity,gravity,consumed);
        return gravity;
    }

    @Unique
    private void alchemical$emitSlowFallingGravity(LivingEntity entity,double effectiveGravity,boolean consumed){
        if(!consumed||entity.level().isClientSide()||entity.onGround()||entity.isPassenger())return;
        if(entity instanceof Player player&&player.getAbilities().flying)return;
        var effect=entity.getEffect(MobEffects.SLOW_FALLING);
        if(effect==null)return;
        if(WearPredicates.slowFallingGravityApplied(entity.getGravity(),entity.getDeltaMovement().y,effectiveGravity))
            alchemical$emitSlowFallingWork(entity,effect);
    }

    @Unique
    private void alchemical$emitSlowFallingWork(LivingEntity entity,MobEffectInstance effect){
        long tick=entity.level().getGameTime();
        if(alchemical$slowFallingWearTick==tick)return;
        alchemical$slowFallingWearTick=tick;
        InfusionWear.emitBuiltin(entity,effect.getEffect(),SLOW_FALLING,1.0);
    }

    private static void emitMovement(LivingEntity self,Vec3 origin,double limit){
        Vec3 delta=self.position().subtract(origin);
        double actual=Math.hypot(delta.x,delta.z);
        double distance=WearPredicates.attributableMovementDistance(actual,limit);
        if(distance<=0.0)return;
        emitMovementEffect(self,self.getEffect(MobEffects.SPEED),distance,true);
        emitMovementEffect(self,self.getEffect(MobEffects.SLOWNESS),distance,false);
    }

    private static void emitMovementEffect(LivingEntity self,MobEffectInstance effect,double distance,boolean beneficial){
        if(effect==null)return;
        double contribution=EffectAttributes.contribution(self,Attributes.MOVEMENT_SPEED,effect);
        double work=WearPredicates.movementSpeedWork(distance,contribution,effect.getAmplifier(),beneficial);
        if(work>0.0)InfusionWear.emitBuiltin(self,effect.getEffect(),MOVEMENT,work);
    }
}
