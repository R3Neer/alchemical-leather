package io.github.r3neer.alchemicalleather.trade;

import com.mojang.datafixers.util.Pair;
import io.github.r3neer.alchemicalleather.data.Infusions;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeSets;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.item.trading.VillagerTrades;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class LeatherworkerTradeTests {
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("alchemical_leather", path); }
    private static ResourceKey<VillagerTrade> trade(String path) { return ResourceKey.create(Registries.VILLAGER_TRADE, id(path)); }
    private static TagKey<Potion> potionTag(String path) { return TagKey.create(Registries.POTION, id(path)); }
    private static net.minecraft.core.Holder.Reference<Potion> potion(String id) { return BuiltInRegistries.POTION.get(Identifier.parse(id)).orElseThrow(); }

    private LootContext context(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var params = new LootParams.Builder(h.getLevel())
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1))))
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
            .create(LootContextParamSets.VILLAGER_TRADE);
        return new LootContext.Builder(params).create(Optional.empty());
    }

    private MerchantOffer offer(GameTestHelper h, String path) {
        var trade = h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).getOrThrow(trade(path));
        var offer = trade.value().getOffer(context(h));
        h.assertTrue(offer != null, "Alchemical Leather trade generates a valid offer: " + path);
        return offer;
    }

    @GameTest
    public void actualTradeSetsPreserveVanillaAndAddThreeCandidates(GameTestHelper h) {
        var trades = h.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE);
        var expert = trades.getOrThrow(trade("leatherworker/expert_infusion"));
        var timed = trades.getOrThrow(trade("leatherworker/master_timed_infusion"));
        var persistent = trades.getOrThrow(trade("leatherworker/master_persistent_infusion"));
        var level4 = trades.getOrThrow(VillagerTradeTags.LEATHERWORKER_LEVEL_4);
        var level5 = trades.getOrThrow(VillagerTradeTags.LEATHERWORKER_LEVEL_5);

        h.assertTrue(level4.contains(expert), "Expert infusion trade is in Leatherworker IV tag");
        h.assertTrue(level5.contains(timed) && level5.contains(persistent), "Both Master infusion trades are in Leatherworker V tag");
        h.assertTrue(level4.contains(trades.getOrThrow(VillagerTrades.LEATHERWORKER_4_TURTLE_SCUTE_EMERALD)), "Expert vanilla scute trade preserved");
        h.assertTrue(level4.contains(trades.getOrThrow(VillagerTrades.LEATHERWORKER_4_EMERALD_DYED_LEATHER_HORSE_ARMOR)), "Expert vanilla horse armor trade preserved");
        h.assertTrue(level5.contains(trades.getOrThrow(VillagerTrades.LEATHERWORKER_5_EMERALD_SADDLE)), "Master vanilla saddle trade preserved");
        h.assertTrue(level5.contains(trades.getOrThrow(VillagerTrades.LEATHERWORKER_5_EMERALD_DYED_LEATHER_HELMET)), "Master vanilla helmet trade preserved");

        for (var tag : List.of(VillagerTradeTags.LEATHERWORKER_LEVEL_1, VillagerTradeTags.LEATHERWORKER_LEVEL_2, VillagerTradeTags.LEATHERWORKER_LEVEL_3)) {
            for (var holder : trades.getOrThrow(tag)) {
                h.assertFalse(holder.unwrapKey().orElseThrow().identifier().getNamespace().equals("alchemical_leather"), "No infusion trade before Expert");
            }
        }

        var sets = h.getLevel().registryAccess().lookupOrThrow(Registries.TRADE_SET);
        var expertSet = sets.getOrThrow(TradeSets.LEATHERWORKER_LEVEL_4).value();
        var masterSet = sets.getOrThrow(TradeSets.LEATHERWORKER_LEVEL_5).value();
        h.assertTrue(expertSet.getTrades().contains(expert), "Expert TradeSet sees appended tag entry");
        h.assertTrue(masterSet.getTrades().contains(timed) && masterSet.getTrades().contains(persistent), "Master TradeSet sees both appended entries");
        h.assertTrue(expertSet.calculateNumberOfTrades(context(h)) == 2 && masterSet.calculateNumberOfTrades(context(h)) == 2, "Vanilla two-offer TradeSet cardinality preserved");
        h.succeed();
    }

    @GameTest
    public void economicArmorTagsEnforceProgression(GameTestHelper h) {
        var expert = BuiltInRegistries.ITEM.getOrThrow(LeatherworkerTrades.EXPERT_ARMOR);
        var master = BuiltInRegistries.ITEM.getOrThrow(LeatherworkerTrades.MASTER_ARMOR);
        h.assertTrue(expert.contains(Items.LEATHER_LEGGINGS.builtInRegistryHolder()) && expert.contains(Items.LEATHER_BOOTS.builtInRegistryHolder()), "Expert pool contains legs and feet");
        for (Item forbidden : List.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_HORSE_ARMOR, Items.WOLF_ARMOR)) {
            h.assertFalse(expert.contains(forbidden.builtInRegistryHolder()), "Expert pool excludes head, chest and animal armor");
        }
        for (Item expected : List.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, Items.LEATHER_HORSE_ARMOR, Items.WOLF_ARMOR)) {
            h.assertTrue(master.contains(expected.builtInRegistryHolder()), "Master pool contains intended vanilla armor: " + BuiltInRegistries.ITEM.getKey(expected));
        }
        h.assertFalse(master.contains(Items.IRON_HELMET.builtInRegistryHolder()), "Mechanical dyeability never auto-enrolls arbitrary armor into villager economy");
        h.succeed();
    }

    @GameTest
    public void expertOfferIsTimedLevelOneLegOrFootArmor(GameTestHelper h) {
        var offer = offer(h, "leatherworker/expert_infusion");
        var result = offer.getResult();
        var slot = Infusions.slot(result);
        var entries = Infusions.entries(result);
        h.assertTrue(slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET, "Expert sells only legs or feet");
        h.assertTrue(entries.size() == 1 && entries.getFirst().mode().equals("timed") && entries.getFirst().amplifier() == 0, "Expert infusion is timed level I");
        h.assertTrue(offer.getCostA().getCount() >= 10 && offer.getCostA().getCount() <= 14, "Expert price stays in 10-14 emerald range");
        h.assertTrue(offer.getMaxUses() == 3, "Expert offer has three uses");
        h.assertFalse(result.has(DataComponents.ADDITIONAL_TRADE_COST), "Internal price premium is stripped from sold item");
        h.succeed();
    }

    @GameTest
    public void masterTimedOfferSupportsFullAndAnimalArmorAtLevelTwoMax(GameTestHelper h) {
        var offer = offer(h, "leatherworker/master_timed_infusion");
        var result = offer.getResult();
        var slot = Infusions.slot(result);
        var entries = Infusions.entries(result);
        h.assertTrue(slot != null && Infusions.SLOTS.contains(slot), "Master timed result is compatible armor including BODY");
        h.assertTrue(entries.size() == 1 && entries.getFirst().mode().equals("timed") && entries.getFirst().amplifier() <= 1, "Master timed infusion is at most level II");
        h.assertTrue((slot == EquipmentSlot.BODY) == result.has(Infusions.ANIMAL_TYPE), "BODY uses animal infusion component only");
        h.assertTrue(offer.getCostA().getCount() >= 16 && offer.getCostA().getCount() <= 28, "Master timed price stays in 16-28 emerald range");
        h.assertTrue(offer.getMaxUses() == 2, "Master timed offer has two uses");
        h.succeed();
    }

    @GameTest
    public void masterPersistentOfferIsEndGameStableLevelOne(GameTestHelper h) {
        var offer = offer(h, "leatherworker/master_persistent_infusion");
        var result = offer.getResult();
        var entries = Infusions.entries(result);
        h.assertTrue(entries.size() == 1 && entries.getFirst().mode().equals("stable") && entries.getFirst().amplifier() == 0, "Persistent trade is stable level I");
        h.assertTrue(offer.getCostA().getCount() >= 28 && offer.getCostA().getCount() <= 40, "Persistent emerald price stays in 28-40 range");
        h.assertTrue(offer.getCostB().is(Items.DRAGON_BREATH) && offer.getCostB().getCount() == 1, "Persistent trade always requires one Dragon's Breath as second cost");
        h.assertTrue(offer.getMaxUses() == 1, "Persistent trade has one use per restock");
        h.assertFalse(result.has(DataComponents.ADDITIONAL_TRADE_COST), "Price metadata never leaks onto result");
        h.succeed();
    }

    @GameTest
    public void deterministicPolicyRejectsWrongTierSlotsAndPower(GameTestHelper h) {
        h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.EXPERT, Items.LEATHER_CHESTPLATE, Potions.FIRE_RESISTANCE), "Expert cannot sell chestplates");
        h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.EXPERT, Items.LEATHER_HELMET, Potions.NIGHT_VISION), "Expert cannot sell helmets");
        h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.EXPERT, Items.LEATHER_HORSE_ARMOR, Potions.SWIFTNESS), "Expert cannot sell animal armor");
        h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.EXPERT, Items.LEATHER_LEGGINGS, Potions.STRONG_SWIFTNESS), "Expert cannot sell level II");
        h.assertTrue(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_TIMED, Items.LEATHER_CHESTPLATE, Potions.STRONG_STRENGTH), "Master timed may sell chest level II");
        h.assertTrue(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_TIMED, Items.WOLF_ARMOR, Potions.STRONG_REGENERATION), "Master timed may sell animal armor level II");
        h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_PERSISTENT, Items.LEATHER_CHESTPLATE, Potions.STRONG_STRENGTH), "Persistent trade cannot exceed level I");
        h.assertTrue(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_PERSISTENT, Items.LEATHER_CHESTPLATE, Potions.STRENGTH), "Persistent chest level I allowed");
        h.assertTrue(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_PERSISTENT, Items.LEATHER_HORSE_ARMOR, Potions.REGENERATION), "Persistent animal armor level I allowed");
        h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_TIMED, Items.LEATHER_CHESTPLATE, Potions.TURTLE_MASTER), "Multi-effect Turtle Master is never sold");
        h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_TIMED, Items.LEATHER_LEGGINGS, Potions.STRENGTH), "Humanoid effect-slot mapping remains authoritative");
        h.succeed();
    }

    @GameTest
    public void defaultPersistentPoolReservesSpecialEffects(GameTestHelper h) {
        var body = BuiltInRegistries.POTION.getOrThrow(potionTag("leatherworker/master_persistent/body"));
        h.assertFalse(body.contains(Potions.INVISIBILITY), "Persistent Invisibility remains outside default villager pool");
        h.assertFalse(body.contains(Potions.TURTLE_MASTER) || body.contains(Potions.STRONG_TURTLE_MASTER), "Turtle Master remains outside default persistent pool");
        h.assertFalse(body.contains(Potions.WIND_CHARGED) || body.contains(Potions.OOZING) || body.contains(Potions.INFESTED) || body.contains(Potions.WEAVING), "Reactive special potions remain exploration/brewing content");
        h.assertTrue(body.contains(Potions.NIGHT_VISION) && body.contains(Potions.FIRE_RESISTANCE) && body.contains(Potions.REGENERATION), "Useful sustained level-I effects remain available");
        h.succeed();
    }

    @GameTest
    public void realOptionalPotionFamiliesRespectTradeCaps(GameTestHelper h) {
        if (FabricLoader.getInstance().isModLoaded("scalebrews")) {
            var growth = potion("scalebrews:growth");
            var growth2 = potion("scalebrews:strong_growth");
            var growth3 = potion("scalebrews:very_strong_growth");
            h.assertTrue(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_TIMED, Items.LEATHER_CHESTPLATE, growth2), "Scale Brews Growth II may be Master timed");
            h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_TIMED, Items.LEATHER_CHESTPLATE, growth3), "Scale Brews Growth III is never sold");
            h.assertTrue(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_PERSISTENT, Items.LEATHER_CHESTPLATE, growth), "Scale Brews Growth I may be persistent");
            h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_PERSISTENT, Items.LEATHER_CHESTPLATE, growth2), "Scale Brews Growth II is never persistent");
            h.assertFalse(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_PERSISTENT, Items.WOLF_ARMOR, growth2), "BODY cannot bypass Scale Brews persistent cap");
        }
        if (FabricLoader.getInstance().isModLoaded("alexsmobs")) {
            var clinging = potion("alexsmobs:clinging");
            var longClinging = potion("alexsmobs:long_clinging");
            h.assertTrue(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_TIMED, Items.LEATHER_BOOTS, longClinging), "Clinging is a valid Master boots trade");
            h.assertTrue(LeatherworkerTrades.validForTrade(LeatherworkerTrades.Tier.MASTER_PERSISTENT, Items.WOLF_ARMOR, clinging), "Clinging may be persistent on Master animal armor");
        }
        if (FabricLoader.getInstance().isModLoaded("clinging_reoriented")) {
            var reorientation = potion("clinging_reoriented:reorientation");
            for (var pair : List.of(
                Pair.of(LeatherworkerTrades.Tier.EXPERT, Items.LEATHER_BOOTS),
                Pair.of(LeatherworkerTrades.Tier.MASTER_TIMED, Items.LEATHER_BOOTS),
                Pair.of(LeatherworkerTrades.Tier.MASTER_PERSISTENT, Items.LEATHER_BOOTS),
                Pair.of(LeatherworkerTrades.Tier.MASTER_PERSISTENT, Items.WOLF_ARMOR))) {
                h.assertFalse(LeatherworkerTrades.validForTrade(pair.getFirst(), pair.getSecond(), reorientation), "Reorientation is hard-banned from every villager tier and BODY");
            }
        }
        h.succeed();
    }
}
