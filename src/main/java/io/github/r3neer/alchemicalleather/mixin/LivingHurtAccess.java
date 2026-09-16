package io.github.r3neer.alchemicalleather.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only access to the hurt cooldown's comparison damage for exact causal attribution. */
@Mixin(LivingEntity.class)
public interface LivingHurtAccess {
    @Accessor("lastHurt") float alchemical$getLastHurt();
}
