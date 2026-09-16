package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.ReachWear;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class WearBlockReachMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow protected ServerLevel level;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private BlockPos destroyPos;
    @Shadow private boolean hasDelayedDestroy;
    @Shadow private BlockPos delayedDestroyPos;

    @Unique private BlockPos alchemical$reachBreakPos;
    @Unique private double alchemical$reachBreakDistanceSqr=Double.NaN;

    @Inject(method="useItemOn",at=@At("RETURN"))
    private void alchemical$blockReach(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> cir){
        var result=cir.getReturnValue();if(result==null||!result.consumesAction())return;
        ReachWear.emit(player,Attributes.BLOCK_INTERACTION_RANGE,player.getEyePosition().distanceToSqr(hit.getLocation()));
    }

    /** Capture the same outline hit the normal client reach ray uses, but bill it only on success. */
    @Inject(method="handleBlockBreakAction",at=@At("HEAD"))
    private void alchemical$captureBlockBreakReach(BlockPos pos,ServerboundPlayerActionPacket.Action action,Direction direction,int maxY,int sequence,CallbackInfo ci){
        if(action==ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK){
            if(pos.equals(alchemical$reachBreakPos))alchemical$clearBreakReach();
            return;
        }
        if(action!=ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK)return;
        alchemical$clearBreakReach();
        double range=player.blockInteractionRange();
        if(!Double.isFinite(range)||range<=0.0)return;
        Vec3 from=player.getEyePosition();
        Vec3 to=from.add(player.calculateViewVector(player.getXRot(),player.getYRot()).scale(range));
        BlockHitResult hit=level.clip(new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        if(hit.getType()!=HitResult.Type.BLOCK||!hit.getBlockPos().equals(pos))return;
        alchemical$reachBreakPos=pos.immutable();
        alchemical$reachBreakDistanceSqr=from.distanceToSqr(hit.getLocation());
    }

    @Inject(method="handleBlockBreakAction",at=@At("RETURN"))
    private void alchemical$discardRejectedBlockBreak(BlockPos pos,ServerboundPlayerActionPacket.Action action,Direction direction,int maxY,int sequence,CallbackInfo ci){
        if(alchemical$reachBreakPos==null||!alchemical$reachBreakPos.equals(pos))return;
        if(action==ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK){alchemical$clearBreakReach();return;}
        if(action==ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK||action==ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK){
            if(!alchemical$breakStillActive(pos))alchemical$clearBreakReach();
        }
    }

    @Inject(method="tick",at=@At("RETURN"))
    private void alchemical$discardOrphanedBlockBreak(CallbackInfo ci){
        if(alchemical$reachBreakPos!=null&&!alchemical$breakStillActive(alchemical$reachBreakPos))alchemical$clearBreakReach();
    }

    @WrapOperation(method="destroyBlock",at=@At(value="INVOKE",
        target="Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean alchemical$successfulReachBreak(ServerLevel instance,BlockPos pos,boolean moving,Operation<Boolean> original){
        BlockPos candidate=alchemical$reachBreakPos;
        double distanceSqr=alchemical$reachBreakDistanceSqr;
        boolean changed=original.call(instance,pos,moving);
        if(changed&&candidate!=null&&candidate.equals(pos)&&Double.isFinite(distanceSqr)&&distanceSqr>=0.0){
            if(candidate.equals(alchemical$reachBreakPos))alchemical$clearBreakReach();
            ReachWear.emit(player,Attributes.BLOCK_INTERACTION_RANGE,distanceSqr);
        }
        return changed;
    }

    @Unique private boolean alchemical$breakStillActive(BlockPos pos){
        return isDestroyingBlock&&pos.equals(destroyPos)||hasDelayedDestroy&&pos.equals(delayedDestroyPos);
    }

    @Unique private void alchemical$clearBreakReach(){
        alchemical$reachBreakPos=null;
        alchemical$reachBreakDistanceSqr=Double.NaN;
    }
}
