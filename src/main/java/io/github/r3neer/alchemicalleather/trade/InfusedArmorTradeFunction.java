package io.github.r3neer.alchemicalleather.trade;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

/** Builds one validated infused-armor result while keeping one villager-trade candidate per category. */
public record InfusedArmorTradeFunction(LeatherworkerTrades.Tier tier) implements LootItemFunction {
    public static final MapCodec<InfusedArmorTradeFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        LeatherworkerTrades.Tier.CODEC.fieldOf("tier").forGetter(InfusedArmorTradeFunction::tier)
    ).apply(instance, InfusedArmorTradeFunction::new));

    @Override
    public ItemStack apply(ItemStack ignoredTemplate, LootContext context) {
        return LeatherworkerTrades.create(tier, context.getRandom());
    }

    @Override
    public MapCodec<? extends LootItemFunction> codec() {
        return MAP_CODEC;
    }
}
