package io.github.r3neer.alchemicalleather.trade;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public class TradeEnchantmentGuardTest {
    @GameTest
    public void enchantedArmorCannotEnterVillagerInfusionPipeline(GameTestHelper h) {
        var boots = new ItemStack(Items.LEATHER_BOOTS);
        var protection = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        boots.enchant(protection, 1);
        h.assertTrue(LeatherworkerTrades.validSlot(LeatherworkerTrades.Tier.EXPERT, boots) == null, "Default-enchanted trade armor is rejected before infusion");
        h.succeed();
    }
}
