package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Measures synchronous work done by an effect's own server tick, rather than coincident health changes. */
@Mixin(MobEffectInstance.class)
public abstract class WearEffectTickMixin {
    private static final Identifier HEALTH=Identifier.fromNamespaceAndPath("alchemical_leather","effect_health_delta");
    private static final Identifier POISON_REMOVED=Identifier.fromNamespaceAndPath("alchemical_leather","poison_removed");
    private static final Identifier ALEX_POISON_RESISTANCE=Identifier.fromNamespaceAndPath("alexsmobs","poison_resistance");

    @WrapOperation(method="tickServer",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean alchemical$causalTick(MobEffect effect,ServerLevel level,LivingEntity entity,int amplifier,Operation<Boolean> original){
        var instance=(MobEffectInstance)(Object)this;
        float before=entity.getHealth();
        boolean hadPoison=entity.hasEffect(MobEffects.POISON);
        boolean result=original.call(effect,level,entity,amplifier);
        float after=entity.getHealth();
        var holder=instance.getEffect();
        if(holder.equals(MobEffects.REGENERATION)&&after>before)InfusionWear.emitBuiltin(entity,holder,HEALTH,after-before);
        else if(holder.equals(MobEffects.POISON)&&after<before)InfusionWear.emitBuiltin(entity,holder,HEALTH,before-after);
        Identifier id=BuiltInRegistries.MOB_EFFECT.getKey(holder.value());
        if(ALEX_POISON_RESISTANCE.equals(id)&&hadPoison&&!entity.hasEffect(MobEffects.POISON))InfusionWear.emitBuiltin(entity,holder,POISON_REMOVED,1.0);
        return result;
    }
}
