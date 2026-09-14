package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.ReachWear;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class WearBlockReachMixin {
    @Inject(method="useItemOn",at=@At("RETURN"))
    private void alchemical$blockReach(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> cir){
        var result=cir.getReturnValue();if(result==null||!result.consumesAction())return;
        ReachWear.emit(player,Attributes.BLOCK_INTERACTION_RANGE,player.getEyePosition().distanceToSqr(hit.getLocation()));
    }
}
