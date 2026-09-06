package io.github.r3neer.alchemicalleather.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MobEffectInstance.class)
public interface EffectAccess {
    @Accessor("hiddenEffect") MobEffectInstance alchemical$getHidden();
    @Accessor("hiddenEffect") void alchemical$setHidden(MobEffectInstance effect);
    @Accessor("duration") void alchemical$setDuration(int duration);
    @Invoker("setDetailsFrom") void alchemical$copyDetails(MobEffectInstance effect);
    @Invoker("tickDownDuration") void alchemical$tickDuration();
    @Invoker("downgradeToHiddenEffect") boolean alchemical$downgrade();
}
