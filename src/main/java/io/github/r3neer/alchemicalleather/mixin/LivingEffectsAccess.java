package io.github.r3neer.alchemicalleather.mixin;

import java.util.Collection;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEffectsAccess {
    @Invoker("onEffectAdded") void alchemical$added(MobEffectInstance effect, Entity source);
    @Invoker("onEffectUpdated") void alchemical$updated(MobEffectInstance effect, boolean attributes, Entity source);
    @Invoker("onEffectsRemoved") void alchemical$removed(Collection<MobEffectInstance> effects);
    @Invoker("tickEffects") void alchemical$tickEffects();
}
