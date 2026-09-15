package io.github.r3neer.alchemicalleather.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

/** Invokes the vanilla terminal damage path after the ordinary damageability gate. */
@Mixin(ItemStack.class)
public interface ItemStackDamageAccess {
    @Invoker("applyDamage")
    void alchemical$applyDamage(int newDamage, ServerPlayer player, Consumer<Item> onBreak);
}
