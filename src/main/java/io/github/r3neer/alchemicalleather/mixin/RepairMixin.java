package io.github.r3neer.alchemicalleather.mixin;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RepairItemRecipe.class)
public abstract class RepairMixin {
    @Inject(method="canCombine",at=@At("HEAD"),cancellable=true)
    private static void alchemical$deny(ItemStack first,ItemStack second,CallbackInfoReturnable<Boolean> ci){if(Infusions.blocked(first)||Infusions.blocked(second))ci.setReturnValue(false);}
}
