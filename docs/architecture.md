# Architecture and implementation notes

This document describes the architecture of Alchemical Leather 0.1.0-alpha.3. Player-facing mechanics live in [GUIDE.md](GUIDE.md).

## Armor classification

Alchemical Leather does not classify armor by material or item ID. `Infusions.slot(stack)` requires an `EQUIPPABLE` component whose slot is an armor slot and exact dyeability from `DyeableArmorRules`.

Dyeability has three sources:

1. `#minecraft:cauldron_can_remove_dye`;
2. a loaded, self-recoloring `minecraft:crafting_dye` recipe whose result item is also accepted by its target ingredient;
3. `#alchemical_leather:dyeable_armor` as an explicit compatibility fallback.

The recipe index is built on server-data reload. It receives registry context, applies Fabric `fabric:load_conditions`, validates candidates with Minecraft 26.2's registry-aware `DyeRecipe.MAP_CODEC`, and then extracts only the target/result relationship needed to determine self-recoloring. Invalid or conditionally disabled resources provide no evidence. A transmuting A→B recipe cannot classify A merely because B receives `DYED_COLOR`.

Exact classification is server-authoritative. Client interaction prediction intentionally uses the broader `armorCandidate` check (`EQUIPPABLE` + armor slot) so remote clients do not need a second copy of the server recipe/tag index.

## Infusion data model

The original persistent `alchemical_leather:infusion` component remains unchanged for humanoid armor, preserving alpha.1 item decoding.

BODY / animal armor uses `alchemical_leather:animal_infusion`. `AnimalInfusion` is a non-empty immutable list of `Infusion` entries representing **one potion**, not a collection of separately accumulated potions. Reinfusion replaces the entire bundle.

`resolveAll` converts every effect in `PotionContents`, including repeated effect IDs. Normal and splash bottles produce timed entries, lingering bottles produce stable entries, and instantaneous effects remain instant. The legacy `resolve` path requires exactly one effect and an effect-slot mapping for humanoid armor.

## Slot policy

`Infusions.SLOTS` contains HEAD, CHEST, LEGS, FEET and BODY.

- HUMANOID_ARMOR requires `EffectSlotRules.slot(effect) == actualSlot` and keeps the single-effect component.
- ANIMAL_ARMOR accepts any valid potion effect and stores the one-potion bundle.

This means modded dyeable humanoid armor inherits the same body-part rules as vanilla leather, while compatible BODY armor is independent of the humanoid effect-slot datapack.

## Leatherworker trade economy

Minecraft 26.2 exposes villager trades and trade sets as data-driven registries. Alchemical Leather adds exactly three `villager_trade` resources and appends their keys to the vanilla Leatherworker level tags without replacing the underlying vanilla `TradeSet` or removing any vanilla trade:

- one Expert timed category at level IV;
- one Master timed category at level V;
- one Master persistent category at level V.

Each category stays a single candidate regardless of how many armor/potion combinations it can generate. `InfusedArmorTradeFunction` is a registered loot-item function used by those trade resources. It delegates to `LeatherworkerTrades`, which first chooses among valid armor items and only then among valid potions for the selected slot. This two-stage selection prevents BODY armor from becoming more likely merely because its permitted potion union is larger.

Trade economy eligibility is intentionally separate from mechanical infusion compatibility. The item tags `#alchemical_leather:leatherworker/expert_armor` and `#alchemical_leather:leatherworker/master_armor` are explicit economic allowlists. Slot-specific potion tags define curated tier pools. Optional Scale Brews and Alex's Mobs entries use non-required tag elements and introduce no compile-time dependency.

Tag membership is only a candidate source, not authority. Runtime policy revalidates the generated stack and effect:

- armor must still be actually dyeable/equippable and must not start enchanted;
- Expert accepts only LEGS/FEET and amplifier 0;
- Master timed accepts HEAD/CHEST/LEGS/FEET/BODY and amplifier at most 1;
- every villager tier rejects amplifier 2 or greater;
- persistent trades force stable mode and amplifier 0;
- `clinging_reoriented:reorientation` is hard-rejected regardless of datapack tags;
- humanoid effects must still match `EffectSlotRules`; BODY keeps its independent slot policy;
- multi-effect and instantaneous trade potions are rejected.

The output stack receives the same `infusion` or `animal_infusion` component used by manual infusion plus the potion-derived `DYED_COLOR`. Variant pricing is attached transiently through Minecraft's `ADDITIONAL_TRADE_COST`; `VillagerTrade` folds that into the first emerald cost and removes the component from the sold item. The persistent trade keeps one Dragon's Breath as `additional_wants`, so reputation/demand changes to the primary emerald cost cannot erase the End-resource gate.

If datapacks leave a category with no valid armor/potion combination, the custom loot function returns an empty stack. Vanilla `VillagerTrade.getOffer` then omits that offer rather than exposing invalid equipment.

## Runtime effect ownership

`EffectLedger` keeps the armor-owned source separate from Minecraft's external effect chain. Only the visible winner is projected into the entity's live effect map; external effects are not destroyed merely because armor temporarily outranks them.

Timed armor clocks are written back to the item and advance only while equipped. BODY entries with the same effect ID keep independent clocks; `EquipmentInfusions` chooses the strongest current entry for projection, and a surviving weaker source is exposed immediately when the stronger one expires.

Instant entries are removed from the item before firing so they cannot replay after death, unload or a repeated synchronization. For a BODY potion containing instant and non-instant siblings, only the instant entries are consumed.

Saves replace projected entries with the actual external state. Armor state remains on the item component. No global entity or inventory scan is introduced; synchronization is driven by equipment changes, entity load and player join.

## ServerPlayer connection lifecycle

A `ServerPlayer` can have NBT/equipment loaded before its play connection exists. Projecting an armor effect in that state reaches `ServerPlayer.onEffectAdded` and attempts to send through a null connection.

`EquipmentInfusions.sync` therefore returns before ledger creation, instant consumption or projection whenever `entity instanceof ServerPlayer` and `connection == null`. `ServerPlayConnectionEvents.JOIN` performs the deferred reconciliation once packet delivery is safe. This preserves the item state during pre-login loading rather than merely suppressing the packet side effect.

## Cauldron transactions

`UseBlockCallback` is the common transaction boundary. Eligibility, potion policy, enchantment state and source contents are validated before item/fluid mutation.

Potion cauldrons retain full `PotionContents` plus the original bottle type. Armor infusion copies the target stack, changes only Alchemical Leather components and `DYED_COLOR`, and consumes the fluid dose only after validation. Normal, splash and lingering bottles all use the same ordinary block-use path; a successful cauldron interaction consumes the gesture before splash/lingering item use can throw the bottle.

Dye items can tint an Alchemical Leather potion cauldron. Tinting rebuilds `PotionContents` with only its custom color changed; potion holder, custom effects, custom name, bottle type and dose count stay intact. Potion identity comparisons deliberately ignore custom color, so a matching refill remains compatible with a tinted cauldron and preserves the existing tint. A no-op blend consumes no dye.

Vanilla water washing removes `DYED_COLOR`, `infusion` and `animal_infusion` from exact qualifying armor and lowers the vanilla water level once.

## Native dyed-water subsystem

`alchemical_leather:dyed_water_cauldron` uses a block state with six logical levels and `DyedWaterCauldronEntity` for RGB. Its model reuses vanilla water-cauldron geometry, mapping levels 1-2 / 3-4 / 5-6 to vanilla visual fill levels 1 / 2 / 3.

Dyes are discovered via `DataComponents.DYE`. `DyeColors` implements the same brightness-preserving averaging used by Minecraft dye mixing. The subsystem supports color mixing, one-unit armor recoloring, two-unit bottle extraction, full six-unit bucket extraction, water-potion tint removal and vanilla water-bucket refill delegation.

The block entity persists RGB independently of the six-level block state and synchronizes it using the standard update-packet path.

## Client render-data boundary

Fabric 26.2's `BlockTintsFactory` can execute from chunk-meshing threads. It must not inspect mutable block entities directly.

Both custom cauldron block entities implement Fabric's `RenderDataBlockEntity` contract and expose their current RGB as an immutable `Integer`. The tint factory reads only `FabricBlockGetter.getBlockEntityRenderData(pos)`, falling back to white if no valid snapshot exists. `setChanged()` plus `sendBlockUpdated(..., 3)` and the block entity update packet synchronize changed RGB to the client.

A tint-only update does not change the cauldron `BlockState`, so packet synchronization alone is insufficient to guarantee that an already-built terrain mesh is rebuilt with the new RGB. After a client-side block entity load changes the exposed color, `CauldronRenderInvalidation` forwards the exact originating `Level` and block position to the client initializer. The client marks only that containing section dirty through Minecraft 26.2's `ClientLevel#setSectionRangeDirty(...)` path. The common bridge contains no client-only class reference and is a no-op on dedicated servers.

## BedrockIfy ownership

BedrockIfy is optional and is never linked at compile time. `BedrockifyBridge` uses reflection for the minimal state it needs.

Ownership is deliberately per interaction:

- Alchemical Leather always owns its native dyed-water block.
- With active BedrockIfy cauldrons, BedrockIfy owns vanilla water + dye and all ordinary interactions on its own potion/colored-water blocks.
- Alchemical Leather intercepts only compatible armor actions on BedrockIfy blocks.
- BedrockIfy potion import accepts only canonical complete-bottle levels 2/5/8; one armor infusion consumes one canonical dose without replacing the foreign block unless it becomes empty.
- BedrockIfy colored water recolors compatible armor and consumes exactly one of its six units.
- BedrockIfy's active cauldron feature deliberately revokes ordinary `DyeRecipe.matches(...)`; Alchemical Leather treats that as foreign recipe policy rather than attempting to restore crafting-table dyeing.
- If the BedrockIfy setting cannot be positively verified because it is absent, disabled, reflectively incompatible or otherwise fails at runtime, Alchemical Leather treats it as inactive and keeps its native vanilla-water dye entry point.

There is no registry replacement, handler clearing, mixin-priority contest or production mutation of BedrockIfy settings.

## Enchanting and item transforms

`Infusions.blocked` covers both humanoid and BODY components. Fabric enchanting hooks plus the existing vanilla/anvil/crafting/grindstone guards maintain the mutual exclusion between enchantments and Alchemical Leather infusions. Standard component-preserving transforms such as armor trims and self-recoloring dye recipes retain infusion data when those transforms remain enabled by the active mod set.

Administrative component editing and third-party code that directly mutates internal effect maps remain outside the balancing contract.

## Validation boundaries

Automated tests cover classification false positives, disabled/invalid recipe resources, humanoid and BODY policy, repeated/multi/instant effects, external ownership, persistence, pre-connection player loading, six-level dyed water, potion-cauldron tinting and bottle interaction modes, BedrockIfy ownership, real Clinging Reoriented/Scale Brews integration and the data-driven Leatherworker economy. Trade tests inspect actual registries/tags, TradeSet cardinality, generated offers, prices/use limits, Dragon's Breath gating, tier/slot boundaries, hard bans and optional-mod caps. The client suite covers synchronized cauldron state, render-data snapshots, repeated live recoloring of already-meshed potion/dyed-water cauldrons and equipment effect synchronization.

The Gradle `verifyGameTestEntrypoints` task scans server GameTest source classes and compares them with the Fabric `fabric-gametest` entrypoint list. `check` fails on either an unregistered GameTest class or a stale descriptor entry, preventing silent test-discovery drift from producing misleadingly green CI.

See [validation.md](validation.md) for the exact release-candidate evidence, the post-release registration audit and remaining human/runtime checks.
