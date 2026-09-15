package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Generic causal wear detectors that can be expressed without effect-specific mod knowledge. */
@Mixin(LivingEntity.class)
public abstract class WearLivingMixin {
    private static final Identifier MOVEMENT=Identifier.fromNamespaceAndPath("alchemical_leather","self_propelled_movement_speed");
    private static final Identifier JUMP=Identifier.fromNamespaceAndPath("alchemical_leather","jump_boost_jump");
    private static final Identifier WATER_BREATHING=Identifier.fromNamespaceAndPath("alchemical_leather","water_breathing_tick");
    private static final Identifier SLOW_FALLING=Identifier.fromNamespaceAndPath("alchemical_leather","slow_falling_tick");
    @Unique private Vec3 alchemical$travelOrigin;
    @Unique private boolean alchemical$meterTravel;

    @Inject(method="travel",at=@At("HEAD"))
    private void alchemical$beforeSelfMovement(Vec3 input,CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        alchemical$meterTravel=!self.level().isClientSide()&&!self.isPassenger()&&!self.isFallFlying()&&!self.isInWater()&&!self.isInLava()
            &&input!=null&&input.horizontalDistanceSqr()>1.0E-8;
        alchemical$travelOrigin=alchemical$meterTravel?self.position():null;
    }

    @Inject(method="travel",at=@At("RETURN"))
    private void alchemical$selfMovement(Vec3 input,CallbackInfo ci){
        if(!alchemical$meterTravel||alchemical$travelOrigin==null)return;
        var self=(LivingEntity)(Object)this;
        Vec3 delta=self.position().subtract(alchemical$travelOrigin);double distance=Math.sqrt(delta.x*delta.x+delta.z*delta.z);
        alchemical$meterTravel=false;alchemical$travelOrigin=null;
        if(!Double.isFinite(distance)||distance<=1.0E-8)return;
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
            &&!self.hasEffect(MobEffects.LEVITATION)&&self.getDeltaMovement().y<0.0)
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
        return alchemical$waterBreathingNeeded(natural,creative,alternate,submerged,bubble);
    }

    /** Pure holdout seam for the vanilla drowning gate; kept package-visible for GameTests. */
    static boolean alchemical$waterBreathingNeeded(boolean natural,boolean creative,boolean alternate,boolean submerged,boolean bubble){
        return !natural&&!creative&&!alternate&&submerged&&!bubble;
    }
}
