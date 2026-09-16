package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.ReachWear;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    /**
     * Vanilla reaches this call only inside `if (brushingUpdatedState)`. Hooking the brush's own
     * durability payment therefore gives us the exact successful archaeology-work boundary without
     * depending on the private BrushableBlockEntity#brush descriptor.
     */
    @Inject(method="onUseTick",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void alchemical$billSuccessfulBrush(Level level,LivingEntity user,ItemStack stack,int ticksRemaining,CallbackInfo ci){
        BlockHitResult hit=alchemical$brushReachHit.get();
        alchemical$brushReachHit.remove();
        if(level.isClientSide()||!(user instanceof Player player)||hit==null)return;
        ReachWear.emit(player,Attributes.BLOCK_INTERACTION_RANGE,player.getEyePosition().distanceToSqr(hit.getLocation()));
    }

    @Inject(method="onUseTick",at=@At("RETURN"))
    private void alchemical$clearBrushHit(Level level,LivingEntity user,ItemStack stack,int ticksRemaining,CallbackInfo ci){
        alchemical$brushReachHit.remove();
    }
}
