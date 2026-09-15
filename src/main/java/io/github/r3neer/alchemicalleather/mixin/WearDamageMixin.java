package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.EffectAttributes;
import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Generic causal accounting for vanilla damage prevention and knockback resistance. */
@Mixin(LivingEntity.class)
public abstract class WearDamageMixin {
    private static final Identifier DAMAGE_PREVENTED=Identifier.fromNamespaceAndPath("alchemical_leather","damage_prevented");
    private static final Identifier KNOCKBACK_REDUCED=Identifier.fromNamespaceAndPath("alchemical_leather","knockback_reduced");
    private static final Identifier ALEX_KNOCKBACK=Identifier.fromNamespaceAndPath("alexsmobs","knockback_resistance");

    @Inject(method="getDamageAfterMagicAbsorb",at=@At("HEAD"))
    private void alchemical$resistance(DamageSource source,float damage,CallbackInfoReturnable<Float> cir){
        var self=(LivingEntity)(Object)this;
        var resistance=self.getEffect(MobEffects.RESISTANCE);
        if(resistance==null||damage<=0||source.is(DamageTypeTags.BYPASSES_EFFECTS)||source.is(DamageTypeTags.BYPASSES_RESISTANCE))return;
        int absorbValue=(resistance.getAmplifier()+1)*5;
        float after=Math.max(damage*(25-absorbValue)/25.0F,0.0F);
        float prevented=damage-after;
        if(prevented>0)InfusionWear.emitBuiltin(self,resistance.getEffect(),DAMAGE_PREVENTED,prevented);
    }

    /**
     * Wrap the exact Fire Resistance query in hurtServer. Reaching this call means vanilla's
     * earlier general-invulnerability and dead-entity guards already passed and IS_FIRE was true;
     * a prior cancellation by another mixin therefore cannot manufacture alchemical work.
     */
    @WrapOperation(method="hurtServer",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z",ordinal=0))
    private boolean alchemical$fireResistanceDecision(LivingEntity instance,Holder<MobEffect> effect,
                                                      Operation<Boolean> original,ServerLevel level,
                                                      DamageSource source,float damage){
        boolean active=original.call(instance,effect);
        if(active&&effect.equals(MobEffects.FIRE_RESISTANCE)&&damage>0){
            var resistance=instance.getEffect(MobEffects.FIRE_RESISTANCE);
            if(resistance!=null)InfusionWear.emitBuiltin(instance,resistance.getEffect(),DAMAGE_PREVENTED,damage);
        }
        return active;
    }

    /* 26.2 funnels contextual knockback through this overload. The simple helper delegates here. */
    @Inject(method="knockback",at=@At("HEAD"))
    private void alchemical$knockback(double power,double xd,double zd,DamageSource source,float damage,boolean blocked,CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        var holder=BuiltInRegistries.MOB_EFFECT.get(ALEX_KNOCKBACK);
        if(holder.isEmpty()||power<=0)return;
        var effect=self.getEffect(holder.get());if(effect==null)return;

        double withResistance=self.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double withoutResistance=EffectAttributes.without(self,Attributes.KNOCKBACK_RESISTANCE,effect);
        double withMultiplier=Math.max(0.0D,1.0D-withResistance);
        double withoutMultiplier=Math.max(0.0D,1.0D-withoutResistance);
        double prevented=power*Math.max(0.0D,withoutMultiplier-withMultiplier);
        if(prevented>0)InfusionWear.emitBuiltin(self,effect.getEffect(),KNOCKBACK_REDUCED,prevented);
    }
}
