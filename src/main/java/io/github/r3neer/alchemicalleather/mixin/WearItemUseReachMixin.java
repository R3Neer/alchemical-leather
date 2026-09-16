package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.ReachWear;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
    @Unique private static final ThreadLocal<BlockHitResult> alchemical$reachHit=new ThreadLocal<>();

    @Inject(method="use",at=@At("HEAD"))
    private void alchemical$resetReachHit(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){
        if(!level.isClientSide())alchemical$reachHit.remove();
    }

    @WrapOperation(method="use",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/item/Item;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult alchemical$captureReachHit(Level level,Player player,ClipContext.Fluid fluid,Operation<BlockHitResult> original){
        BlockHitResult hit=original.call(level,player,fluid);
        if(!level.isClientSide())alchemical$reachHit.set(hit);
        return hit;
    }

    @Inject(method="use",at=@At("RETURN"))
    private void alchemical$billSuccessfulReachUse(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){
        if(level.isClientSide())return;
        BlockHitResult hit=alchemical$reachHit.get();
        alchemical$reachHit.remove();
        InteractionResult result=cir.getReturnValue();
        if(result==null||!result.consumesAction()||hit==null||hit.getType()!=HitResult.Type.BLOCK)return;
        ReachWear.emit(player,Attributes.BLOCK_INTERACTION_RANGE,player.getEyePosition().distanceToSqr(hit.getLocation()));
    }
}
