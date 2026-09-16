package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bills Weakness only when vanilla actually consumes it to start a zombie-villager cure. */
@Mixin(ZombieVillager.class)
public abstract class WearWeaknessCureMixin {
    private static final Identifier WEAKNESS_CURE=Identifier.fromNamespaceAndPath("alchemical_leather","weakness_zombie_cure");

    @Inject(method="mobInteract",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/monster/zombie/ZombieVillager;startConverting(Ljava/util/UUID;I)V"))
    private void alchemical$successfulWeaknessCure(Player player,InteractionHand hand,
                                                   CallbackInfoReturnable<InteractionResult> cir){
        var self=(ZombieVillager)(Object)this;
        var effect=self.getEffect(MobEffects.WEAKNESS);
        if(effect!=null)InfusionWear.emitBuiltin(self,effect.getEffect(),WEAKNESS_CURE,1.0);
    }
}
