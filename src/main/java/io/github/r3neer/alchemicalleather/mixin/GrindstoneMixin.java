package io.github.r3neer.alchemicalleather.mixin;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMixin {
    @Inject(method="computeResult",at=@At("HEAD"),cancellable=true)
    private void alchemical$deny(ItemStack first,ItemStack second,CallbackInfoReturnable<ItemStack> ci){if(Infusions.blocked(first)||Infusions.blocked(second))ci.setReturnValue(ItemStack.EMPTY);}
}
