package io.github.r3neer.alchemicalleather.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.github.r3neer.alchemicalleather.data.AnimalInfusion;
import io.github.r3neer.alchemicalleather.data.EffectSlotRules;
import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;

/** Economy policy for Leatherworker infusion trades. Mechanical infusion compatibility remains separate. */
public final class LeatherworkerTrades {
    public static final Identifier FUNCTION_ID = id("infused_armor_trade");
    public static final TagKey<Item> EXPERT_ARMOR = itemTag("leatherworker/expert_armor");
    public static final TagKey<Item> MASTER_ARMOR = itemTag("leatherworker/master_armor");
    public static final Identifier REORIENTATION = Identifier.fromNamespaceAndPath("clinging_reoriented", "reorientation");
    private static boolean initialized;

    private LeatherworkerTrades() {}

    public enum Tier {
        EXPERT("expert", false, 0),
        MASTER_TIMED("master_timed", false, 1),
        MASTER_PERSISTENT("master_persistent", true, 0);

        public static final Codec<Tier> CODEC = Codec.STRING.comapFlatMap(
            name -> Arrays.stream(values()).filter(value -> value.id.equals(name)).findFirst()
                .<DataResult<Tier>>map(DataResult::success)
                .orElseGet(() -> DataResult.error(() -> "Unknown Alchemical Leather trade tier: " + name)),
            Tier::id
        );

        private final String id;
        private final boolean persistent;
        private final int maxAmplifier;

        Tier(String id, boolean persistent, int maxAmplifier) {
            this.id = id;
            this.persistent = persistent;
            this.maxAmplifier = maxAmplifier;
        }

        public String id() { return id; }
        public boolean persistent() { return persistent; }
        public int maxAmplifier() { return maxAmplifier; }
    }

    public static void initialize() {
        if (initialized) return;
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, FUNCTION_ID, InfusedArmorTradeFunction.MAP_CODEC);
        initialized = true;
    }

    /** Select armor first, then potion, so BODY's larger potion pool does not make BODY armor more likely. */
    static ItemStack create(Tier tier, RandomSource random) {
        var validItems = new ArrayList<Item>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(armorTag(tier))) {
            Item item = holder.value();
            EquipmentSlot slot = validSlot(tier, new ItemStack(item));
            if (slot != null && !validPotions(tier, slot).isEmpty()) validItems.add(item);
        }
        if (validItems.isEmpty()) return ItemStack.EMPTY;

        Item item = validItems.get(random.nextInt(validItems.size()));
        EquipmentSlot slot = validSlot(tier, new ItemStack(item));
        var potions = validPotions(tier, slot);
        if (potions.isEmpty()) return ItemStack.EMPTY;
        return buildResult(tier, item, potions.get(random.nextInt(potions.size())));
    }

    /** Package-visible deterministic seam used by GameTests and by candidate filtering. */
    static ItemStack buildResult(Tier tier, Item item, Holder<Potion> potion) {
        ItemStack result = new ItemStack(item);
        EquipmentSlot slot = validSlot(tier, result);
        if (slot == null) return ItemStack.EMPTY;

        Resolved resolved = resolve(tier, slot, potion);
        if (resolved == null) return ItemStack.EMPTY;

        if (slot == EquipmentSlot.BODY) {
            result.remove(Infusions.TYPE);
            result.set(Infusions.ANIMAL_TYPE, new AnimalInfusion(List.of(resolved.infusion)));
        } else {
            result.remove(Infusions.ANIMAL_TYPE);
            result.set(Infusions.TYPE, resolved.infusion);
        }
        result.set(DataComponents.DYED_COLOR, new DyedItemColor(resolved.contents.getColor() & 0xffffff));
        int premium = pricePremium(tier, slot, resolved.effectId);
        if (premium > 0) result.set(DataComponents.ADDITIONAL_TRADE_COST, premium);
        return result;
    }

    static boolean validForTrade(Tier tier, Item item, Holder<Potion> potion) {
        return !buildResult(tier, item, potion).isEmpty();
    }

    private static List<Holder<Potion>> validPotions(Tier tier, EquipmentSlot slot) {
        var result = new ArrayList<Holder<Potion>>();
        for (Holder<Potion> potion : BuiltInRegistries.POTION.getTagOrEmpty(potionTag(tier, slot))) {
            if (resolve(tier, slot, potion) != null) result.add(potion);
        }
        return result;
    }

    static EquipmentSlot validSlot(Tier tier, ItemStack stack) {
        // Trade generation must obey the same mutual exclusion as cauldron infusion, including
        // custom armor whose default component patch already contains an enchantment.
        if (Infusions.enchanted(stack)) return null;
        EquipmentSlot slot = Infusions.slot(stack);
        if (slot == null) return null;
        if (tier == Tier.EXPERT && slot != EquipmentSlot.LEGS && slot != EquipmentSlot.FEET) return null;
        return Infusions.SLOTS.contains(slot) ? slot : null;
    }

    private static Resolved resolve(Tier tier, EquipmentSlot slot, Holder<Potion> potion) {
        PotionContents contents = new PotionContents(potion);
        var effects = new ArrayList<net.minecraft.world.effect.MobEffectInstance>();
        contents.getAllEffects().forEach(effects::add);
        if (effects.size() != 1) return null;

        var effect = effects.getFirst();
        var effectValue = effect.getEffect().value();
        Identifier effectId = BuiltInRegistries.MOB_EFFECT.getKey(effectValue);
        if (effectId == null || effectId.equals(REORIENTATION) || effectValue.isInstantaneous()) return null;
        if (effect.getDuration() <= 0 || effect.getAmplifier() < 0 || effect.getAmplifier() > tier.maxAmplifier()) return null;
        // Absolute ceiling: villager trades never sell level III+ even if a future tier is added carelessly.
        if (effect.getAmplifier() > 1) return null;
        if (tier.persistent() && effect.getAmplifier() != 0) return null;
        // Keep the Scale Brews invariant explicit even if the generic persistent cap changes later.
        if (effectId.getNamespace().equals("scalebrews") && tier.persistent() && effect.getAmplifier() != 0) return null;
        if (slot != EquipmentSlot.BODY && EffectSlotRules.slot(effectId) != slot) return null;

        String mode = tier.persistent() ? "stable" : "timed";
        int remaining = tier.persistent() ? 0 : effect.getDuration();
        return new Resolved(contents, effectId, new Infusion(effectId, effect.getAmplifier(), mode, remaining));
    }

    private static int pricePremium(Tier tier, EquipmentSlot slot, Identifier effect) {
        int piece = switch (tier) {
            case EXPERT -> slot == EquipmentSlot.LEGS ? 2 : 0;
            case MASTER_TIMED -> switch (slot) {
                case CHEST -> 6;
                case BODY -> 5;
                case HEAD, LEGS -> 2;
                default -> 0;
            };
            case MASTER_PERSISTENT -> switch (slot) {
                case CHEST, BODY -> 5;
                case HEAD, LEGS -> 2;
                default -> 0;
            };
        };
        String path = effect.getPath();
        int effectPremium;
        if (effect.getNamespace().equals("scalebrews")) effectPremium = 6;
        else if (path.equals("regeneration")) effectPremium = 5;
        else if (path.equals("strength")) effectPremium = tier.persistent() ? 4 : 3;
        else if (path.equals("clinging")) effectPremium = tier.persistent() ? 4 : 3;
        else if (path.equals("speed") || path.equals("jump_boost")) effectPremium = 2;
        else if (path.equals("fire_resistance")) effectPremium = 2;
        else effectPremium = tier == Tier.EXPERT ? 2 : 1;
        int maximum = switch (tier) {
            case EXPERT -> 4;
            case MASTER_TIMED, MASTER_PERSISTENT -> 12;
        };
        return Math.min(piece + effectPremium, maximum);
    }

    private static TagKey<Item> armorTag(Tier tier) {
        return tier == Tier.EXPERT ? EXPERT_ARMOR : MASTER_ARMOR;
    }

    private static TagKey<Potion> potionTag(Tier tier, EquipmentSlot slot) {
        return TagKey.create(Registries.POTION, id("leatherworker/" + tier.id() + "/" + slotName(slot)));
    }

    private static String slotName(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "head";
            case CHEST -> "chest";
            case LEGS -> "legs";
            case FEET -> "feet";
            case BODY -> "body";
            default -> throw new IllegalArgumentException("Unsupported armor slot: " + slot);
        };
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, id(path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("alchemical_leather", path);
    }

    private record Resolved(PotionContents contents, Identifier effectId, Infusion infusion) {}
}
