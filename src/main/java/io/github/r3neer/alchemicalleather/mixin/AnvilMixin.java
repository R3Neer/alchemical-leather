package io.github.r3neer.alchemicalleather.mixin;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AnvilMenu.class)
public abstract class AnvilMixin {
    @Shadow @Final private DataSlot cost;
    @Inject(method="createResult",at=@At("HEAD"),cancellable=true)
    private void alchemical$deny(CallbackInfo ci) {
        var menu=(AnvilMenu)(Object)this;var first=menu.getSlot(0).getItem();var second=menu.getSlot(1).getItem();
        if(Infusions.blocked(second)||Infusions.blocked(first)&&!second.isEmpty()&&(Infusions.enchanted(second)||second.is(first.getItem()))) {
            menu.getSlot(2).set(ItemStack.EMPTY);cost.set(0);ci.cancel();
        }
    }
}
