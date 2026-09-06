package io.github.r3neer.alchemicalleather.mixin;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class EnchantableStackMixin {
    @Inject(method="isEnchantable",at=@At("HEAD"),cancellable=true)
    private void alchemical$deny(CallbackInfoReturnable<Boolean> ci){if(Infusions.blocked((ItemStack)(Object)this))ci.setReturnValue(false);}
}
