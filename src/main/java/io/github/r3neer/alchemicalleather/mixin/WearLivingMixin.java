package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import io.github.r3neer.alchemicalleather.effect.WearPredicates;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
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
    @Unique private Vec3 alchemical$travelOrigin;
    @Unique private Vec3 alchemical$preRelativeVelocity;
    @Unique private boolean alchemical$meterTravel;
    @Unique private double alchemical$movementDistanceLimit;

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
        var self=(LivingEntity)(Object)this;
        Vec3 delta=self.position().subtract(origin);
        double actual=Math.hypot(delta.x,delta.z);
        double distance=WearPredicates.attributableMovementDistance(actual,limit);
        if(distance<=0.0)return;
        emit(self,self.getEffect(MobEffects.SPEED),MOVEMENT,distance);
        emit(self,self.getEffect(MobEffects.SLOWNESS),MOVEMENT,distance);
    }

    @Inject(method="jumpFromGround",at=@At("RETURN"))
    private void alchemical$jump(CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        if(self.level().isClientSide()||self.isPassenger())return;
        var effect=self.getEffect(MobEffects.JUMP_BOOST);
        if(effect!=null)InfusionWear.emitBuiltin(self,effect.getEffect(),JUMP,effect.getAmplifier()+1.0);
    }

    @Inject(method="tick",at=@At("RETURN"))
    private void alchemical$environmentalWear(CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        if(self.level().isClientSide()||!self.isAlive())return;

        var breathing=self.getEffect(MobEffects.WATER_BREATHING);
        if(breathing!=null&&wouldNeedWaterBreathing(self))InfusionWear.emitBuiltin(self,breathing.getEffect(),WATER_BREATHING,1.0);

        var falling=self.getEffect(MobEffects.SLOW_FALLING);
        if(falling!=null&&!self.onGround()&&!self.isPassenger()&&!self.isInWater()&&!self.isInLava()
            &&WearPredicates.slowFallingChangesGravity(self.getGravity(),self.getDeltaMovement().y))
            InfusionWear.emitBuiltin(self,falling.getEffect(),SLOW_FALLING,1.0);
    }

    private static void emit(LivingEntity self,MobEffectInstance effect,Identifier detector,double amount){
        if(effect!=null)InfusionWear.emitBuiltin(self,effect.getEffect(),detector,amount*(effect.getAmplifier()+1.0));
    }

    private static boolean wouldNeedWaterBreathing(LivingEntity self){
        boolean natural=self.canBreatheUnderwater();
        boolean creative=self instanceof Player player&&player.getAbilities().invulnerable;
        // In 26.2 Conduit Power and Breath of the Nautilus independently satisfy vanilla's
        // water-breathing gate. If either is present, Water Breathing is not the but-for cause.
        boolean alternate=self.hasEffect(MobEffects.CONDUIT_POWER)||self.hasEffect(MobEffects.BREATH_OF_THE_NAUTILUS);
        boolean submerged=self.isEyeInFluid(FluidTags.WATER);
        boolean bubble=false;
        if(submerged){
            BlockPos eye=BlockPos.containing(self.getX(),self.getEyeY(),self.getZ());
            bubble=self.level().getBlockState(eye).is(Blocks.BUBBLE_COLUMN);
        }
        return WearPredicates.waterBreathingNeeded(natural,creative,alternate,submerged,bubble);
    }
}
