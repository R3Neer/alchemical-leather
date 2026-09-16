package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.ReachUseContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Covers vanilla Item#use paths whose successful result is causally selected by
 * Item#getPlayerPOVHitResult and therefore by BLOCK_INTERACTION_RANGE.
 *
 * EnderEyeItem is intentionally absent: its raycast merely selects a branch before throwing the
 * eye, so a successful throw is not caused by the block-reach query.
 */
@Mixin({BucketItem.class,BottleItem.class,BoatItem.class,SpawnEggItem.class,PlaceOnWaterBlockItem.class})
public abstract class WearItemUseReachMixin {
    @Inject(method="use",at=@At("HEAD"))
    private void alchemical$beginReachUse(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){
        // SpawnEggItem reports SUCCESS even when EntityType#spawn returned null; for that one path
        // require the server-side stack to have been consumed/changed before billing reach.
        ReachUseContext.begin(player,hand,(Object)this instanceof SpawnEggItem);
    }

    @Inject(method="use",at=@At("RETURN"))
    private void alchemical$finishReachUse(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){
        ReachUseContext.finish(player,cir.getReturnValue());
    }
}
