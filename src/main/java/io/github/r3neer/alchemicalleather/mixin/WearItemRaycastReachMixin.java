package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.ReachUseContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the exact block raycast result consumed by selected reach-sensitive Item#use paths. */
@Mixin(Item.class)
public abstract class WearItemRaycastReachMixin {
    @Inject(method="getPlayerPOVHitResult",at=@At("RETURN"))
    private static void alchemical$captureReachRaycast(Level level,Player player,ClipContext.Fluid fluid,CallbackInfoReturnable<BlockHitResult> cir){
        ReachUseContext.capture(player,cir.getReturnValue());
    }
}
