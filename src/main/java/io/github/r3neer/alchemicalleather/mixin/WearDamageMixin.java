package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
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

    @Inject(method="hurtServer",at=@At("HEAD"))
    private void alchemical$fireResistance(ServerLevel level,DamageSource source,float damage,CallbackInfoReturnable<Boolean> cir){
        var self=(LivingEntity)(Object)this;
        var resistance=self.getEffect(MobEffects.FIRE_RESISTANCE);
        if(resistance==null||damage<=0||!source.is(DamageTypeTags.IS_FIRE)||self.isDeadOrDying())return;
        // hurtServer rejects general/base invulnerability before it reaches the Fire Resistance branch.
        // If that earlier guard already applies, Fire Resistance is not the causal reason damage disappears.
        if(self.isInvulnerableTo(level,source))return;
        InfusionWear.emitBuiltin(self,resistance.getEffect(),DAMAGE_PREVENTED,damage);
    }

    @Inject(method="knockback(DDD)V",at=@At("HEAD"))
    private void alchemical$knockback(double power,double xd,double zd,CallbackInfo ci){
        var self=(LivingEntity)(Object)this;
        var holder=BuiltInRegistries.MOB_EFFECT.get(ALEX_KNOCKBACK);
        if(holder.isEmpty()||power<=0)return;
        var effect=self.getEffect(holder.get());if(effect==null)return;
        double total=self.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double contribution=0.5D*(effect.getAmplifier()+1.0D);
        double without=Math.max(0.0D,1.0D-Math.max(0.0D,total-contribution));
        double with=Math.max(0.0D,1.0D-total);
        double prevented=power*Math.max(0.0D,without-with);
        if(prevented>0)InfusionWear.emitBuiltin(self,effect.getEffect(),KNOCKBACK_REDUCED,prevented);
    }
}
