package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.ReachWear;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class WearEntityReachMixin {
    @Inject(method="interactOn",at=@At("RETURN"))
    private void alchemical$entityReach(Entity target,InteractionHand hand,Vec3 location,CallbackInfoReturnable<InteractionResult> cir){
        if(!(cir.getReturnValue() instanceof InteractionResult.Success))return;
        var player=(Player)(Object)this;
        ReachWear.emit(player,Attributes.ENTITY_INTERACTION_RANGE,target.getBoundingBox().distanceToSqr(player.getEyePosition()));
    }
}
