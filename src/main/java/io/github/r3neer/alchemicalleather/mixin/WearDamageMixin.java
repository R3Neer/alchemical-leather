package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.r3neer.alchemicalleather.effect.EffectAttributes;
import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import io.github.r3neer.alchemicalleather.effect.WearPredicates;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Generic causal accounting for vanilla damage prevention and knockback resistance. */
@Mixin(LivingEntity.class)
public abstract class WearDamageMixin {
    private static final Identifier DAMAGE_PREVENTED=Identifier.fromNamespaceAndPath("alchemical_leather","damage_prevented");
    private static final Identifier JUMP_FALL_PREVENTED=Identifier.fromNamespaceAndPath("alchemical_leather","jump_boost_fall_damage_prevented");
    private static final Identifier KNOCKBACK_REDUCED=Identifier.fromNamespaceAndPath("alchemical_leather","knockback_reduced");
    private static final Identifier ALEX_KNOCKBACK=Identifier.fromNamespaceAndPath("alexsmobs","knockback_resistance");

    @Shadow protected float lastHurt;
    @Unique private DamageSource alchemical$fallDamageSource;

    /**
     * Wrap the exact Resistance query in getDamageAfterMagicAbsorb. Reaching this call means the
     * BYPASSES_EFFECTS early return already did not apply; we still mirror the following
     * BYPASSES_RESISTANCE half of vanilla's condition before attributing any prevented damage.
     */
    @WrapOperation(method="getDamageAfterMagicAbsorb",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",ordinal=0))
    private boolean alchemical$resistanceDecision(LivingEntity instance,Holder<MobEffect> effect,
                                                  Operation<Boolean> original,DamageSource source,float damage){
        boolean active=original.call(instance,effect);
        if(active&&effect.equals(MobEffects.RESISTANCE)&&damage>0&&!source.is(DamageTypeTags.BYPASSES_RESISTANCE)){
            var resistance=instance.getEffect(MobEffects.RESISTANCE);
            if(resistance!=null){
                int absorbValue=(resistance.getAmplifier()+1)*5;
                float after=Math.max(damage*(25-absorbValue)/25.0F,0.0F);
                float prevented=damage-after;
                if(prevented>0)InfusionWear.emitBuiltin(instance,resistance.getEffect(),DAMAGE_PREVENTED,prevented);
            }
        }
        return active;
    }

    /**
     * Fire Resistance short-circuits hurtServer before its ordinary damage cooldown. Credit only
     * the portion that would really have crossed that gate without Fire Resistance; an i-frame
     * rejection is not alchemical work.
     */
    @WrapOperation(method="hurtServer",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",ordinal=0))
    private boolean alchemical$fireResistanceDecision(LivingEntity instance,Holder<MobEffect> effect,
                                                      Operation<Boolean> original,ServerLevel level,
                                                      DamageSource source,float damage){
        boolean active=original.call(instance,effect);
        if(active&&effect.equals(MobEffects.FIRE_RESISTANCE)&&damage>0){
            var resistance=instance.getEffect(MobEffects.FIRE_RESISTANCE);
            double prevented=WearPredicates.damagePassingCooldown(
                damage,instance.invulnerableTime,lastHurt,source.is(DamageTypeTags.BYPASSES_COOLDOWN));
            if(resistance!=null&&prevented>0.0)InfusionWear.emitBuiltin(instance,resistance.getEffect(),DAMAGE_PREVENTED,prevented);
        }
        return active;
    }

    /** Preserve the authoritative fall source only for the nested calculateFallDamage call. */
    @Inject(method="causeFallDamage",at=@At("HEAD"))
    private void alchemical$beginFallDamage(double fallDistance,float damageModifier,DamageSource source,CallbackInfoReturnable<Boolean> cir){
        var self=(LivingEntity)(Object)this;
        alchemical$fallDamageSource=self.level() instanceof ServerLevel?source:null;
    }

    /**
     * Jump Boost in 26.2 contributes +1 SAFE_FALL_DISTANCE per level. Meter only damage that its
     * contribution prevents after vanilla's flooring and hurt cooldown gate. This excludes falls
     * that the active i-frame window would have rejected even without Jump Boost.
     */
    @Inject(method="calculateFallDamage",at=@At("RETURN"))
    private void alchemical$jumpBoostFallProtection(double fallDistance,float damageModifier,CallbackInfoReturnable<Integer> cir){
        var self=(LivingEntity)(Object)this;
        DamageSource source=alchemical$fallDamageSource;
        if(source==null||!(self.level() instanceof ServerLevel level)||self.isDeadOrDying()||self.isInvulnerableTo(level,source))return;
        var effect=self.getEffect(MobEffects.JUMP_BOOST);
        if(effect==null)return;
        double safeWith=self.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        double safeWithout=EffectAttributes.without(self,Attributes.SAFE_FALL_DISTANCE,effect);
        double prevented=WearPredicates.jumpBoostFallDamagePrevented(
            fallDistance,damageModifier,self.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER),
            safeWith,safeWithout,cir.getReturnValue(),self.invulnerableTime,lastHurt,
            source.is(DamageTypeTags.BYPASSES_COOLDOWN));
        if(prevented>0.0)InfusionWear.emitBuiltin(self,effect.getEffect(),JUMP_FALL_PREVENTED,prevented);
    }

    @Inject(method="causeFallDamage",at=@At("RETURN"))
    private void alchemical$endFallDamage(double fallDistance,float damageModifier,DamageSource source,CallbackInfoReturnable<Boolean> cir){
        alchemical$fallDamageSource=null;
    }

    /**
     * Bind to the unique KNOCKBACK_RESISTANCE attribute read rather than a version-fragile method
     * descriptor. MixinExtras supplies the enclosing method's first double argument as the original
     * knockback power, regardless of the contextual overload Loom exposes in this mapping set.
     */
    @WrapOperation(method="knockback",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double alchemical$knockbackResistanceValue(LivingEntity instance,Holder<Attribute> attribute,
                                                       Operation<Double> original,
                                                       @Local(argsOnly=true,ordinal=0) double power){
        double withResistance=original.call(instance,attribute);
        if(power<=0.0||!attribute.equals(Attributes.KNOCKBACK_RESISTANCE))return withResistance;
        var holder=BuiltInRegistries.MOB_EFFECT.get(ALEX_KNOCKBACK);
        if(holder.isEmpty())return withResistance;
        var effect=instance.getEffect(holder.get());
        if(effect==null)return withResistance;

        double withoutResistance=EffectAttributes.without(instance,Attributes.KNOCKBACK_RESISTANCE,effect);
        double withMultiplier=Math.max(0.0D,1.0D-withResistance);
        double withoutMultiplier=Math.max(0.0D,1.0D-withoutResistance);
        double prevented=power*Math.max(0.0D,withoutMultiplier-withMultiplier);
        if(prevented>0.0)InfusionWear.emitBuiltin(instance,effect.getEffect(),KNOCKBACK_REDUCED,prevented);
        return withResistance;
    }
}
