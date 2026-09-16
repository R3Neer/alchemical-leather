package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.ReachWear;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Bills only archaeology progress actually advanced by a block-reach-assisted brush stroke. */
@Mixin(BrushItem.class)
public abstract class WearBrushReachMixin {
    @Unique private static final ThreadLocal<BlockHitResult> alchemical$brushReachHit=new ThreadLocal<>();

    @WrapOperation(method="onUseTick",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/item/BrushItem;calculateHitResult(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult alchemical$captureBrushHit(BrushItem instance,Player player,Operation<HitResult> original){
        HitResult hit=original.call(instance,player);
        if(!player.level().isClientSide()&&hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK)
            alchemical$brushReachHit.set(block);
        else alchemical$brushReachHit.remove();
        return hit;
    }

    @WrapOperation(method="onUseTick",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/level/block/entity/BrushableBlockEntity;brush(JLnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean alchemical$billSuccessfulBrush(BrushableBlockEntity brushable,long gameTime,ServerLevel level,
                                                    Player player,Direction direction,ItemStack stack,Operation<Boolean> original){
        boolean changed=original.call(brushable,gameTime,level,player,direction,stack);
        BlockHitResult hit=alchemical$brushReachHit.get();
        alchemical$brushReachHit.remove();
        if(changed&&hit!=null)
            ReachWear.emit(player,Attributes.BLOCK_INTERACTION_RANGE,player.getEyePosition().distanceToSqr(hit.getLocation()));
        return changed;
    }
}
