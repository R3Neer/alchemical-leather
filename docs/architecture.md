# Architecture and implementation notes

This document describes the architecture shipped with **Alchemical Leather 0.1.0-beta.2**. Player-facing mechanics live in [GUIDE.md](GUIDE.md). Optional-mod contracts live in [COMPATIBILITY.md](COMPATIBILITY.md).

## Armor classification

Alchemical Leather does not classify armor by material or item ID. `Infusions.slot(stack)` requires an `EQUIPPABLE` component whose slot is an armor slot and exact dyeability from `DyeableArmorRules`.

Dyeability has three sources:

1. `#minecraft:cauldron_can_remove_dye`;
2. a loaded self-recoloring `minecraft:crafting_dye` recipe whose result item is also accepted by its target ingredient;
3. `#alchemical_leather:dyeable_armor` as an explicit compatibility fallback.

The recipe index is built on server-data reload, honors Fabric load conditions and accepts only genuine self-recoloring evidence. Exact classification is server-authoritative. Client interaction prediction deliberately uses the broader `armorCandidate` check so a remote client does not need a duplicate copy of the server's recipe/tag index.

## Infusion data model

The item is the durable source of truth. Alchemical Leather has four persistent item components relevant to infusion/wear:

- `alchemical_leather:infusion` — the original single-effect humanoid representation, retained for backwards-compatible decoding and still used for one-effect humanoid infusions;
- `alchemical_leather:humanoid_infusion` — a non-empty bundle of at least two **distinct** humanoid effects that all belong to the same equipment slot;
- `alchemical_leather:animal_infusion` — the complete non-empty effect bundle from one BODY/animal potion, where repeated effect identities remain legal;
- `alchemical_leather:wear_progress` — fractional causal work, keyed per effect, accumulated on that exact item.

`Infusions.entries(stack)` normalizes those representations for runtime projection. `clearInfusionComponents` clears all infusion forms plus wear progress, which makes reinfusion atomic and prevents stale wear debt from surviving a different potion.

### Resolution rules

`resolveAll` converts every potion effect into an `Infusion`, preserving amplifier and bottle semantics:

- normal/splash non-instant effects become `timed` with their original duration;
- lingering non-instant effects become `stable`;
- instantaneous effects remain `instant`.

BODY armor accepts the resulting complete potion bundle.

Humanoid armor uses `resolveHumanoid(contents, bottle, targetSlot)`. Every effect must map through `EffectSlotRules` to the same actual target slot, and effect identities must be distinct. One resolved effect is stored in the legacy `infusion` component; two or more become `HumanoidInfusion`. This allows potions such as Turtle Master when their constituent effects share one humanoid slot while still rejecting cross-slot bundles.

Zero-effect contents remain invalid **as an infusion**. Cauldron dye-only behavior handles them one layer higher rather than weakening the non-empty infusion invariant.

## Shared armor transformation

`ArmorInfusionService` is the transformation kernel shared by cauldron and crafting paths. It receives the target stack, complete `PotionContents`, bottle type and whether effectless dye-only application is allowed.

The service:

1. verifies exact armor eligibility;
2. copies the input before mutation;
3. handles effectless dye-only policy separately;
4. rejects enchanted targets for effectful infusion;
5. resolves BODY or same-slot humanoid contents;
6. clears all previous Alchemical infusion/wear components atomically;
7. writes the new single/bundle component;
8. transfers visible potion RGB through `DYED_COLOR`.

This keeps cauldron and crafting semantics from drifting independently and ensures reinfusion does not leave stale components or fractional wear behind.

## Effect-slot policy

`Infusions.SLOTS` contains HEAD, CHEST, LEGS, FEET and BODY.

Humanoid policy is data-driven through `EffectSlotRules`. BODY armor bypasses humanoid effect-slot mapping and stores a whole potion bundle. Optional first-party mods own their own effect-slot resources where appropriate; Alchemical Leather no longer duplicates Scale Brews or Reorientation ownership.

Resource loading is server-data-driven and guarded by optional `requires_mod`, `requires_effect`, `requires_resource` and `enabled` conditions.

## Runtime effect ownership

`EffectLedger` separates armor-owned sources from Minecraft's external effect chain. Only the appropriate visible winner is projected into the entity's live effect map; external effects are not destroyed simply because armor temporarily outranks them.

For every armor-managed effect the ledger also records the **owning equipment slot**. That owner is essential for causal wear: reporting that an effect performed work is not enough. The wear engine bills only the exact equipped item selected by the ledger for that effect.

`EquipmentInfusions` reconciles each equipment slot independently, projects the strongest currently relevant armor entry, writes timed clocks back to the item, consumes instant entries before they can replay and updates slot ownership in the ledger. Multi-effect humanoid entries keep independent timing and wear attribution even though they live in one atomic infusion bundle.

An equal/stronger external effect may eclipse the armor source. While the armor is not the effective source, its item is not charged for causal work. When the external source disappears, the surviving armor source can become visible again without having destroyed the external clock or its own item state.

## ServerPlayer connection lifecycle

A `ServerPlayer` can have NBT/equipment loaded before its play connection exists. Projecting an armor effect in that state would eventually try to send an effect packet through a null connection.

`EquipmentInfusions.sync` therefore defers reconciliation when a `ServerPlayer` has no connection. `ServerPlayConnectionEvents.JOIN` performs the real synchronization once packet delivery is safe. Tests preserve this invariant rather than weakening it for synthetic pre-login players.

## Causal infusion-wear engine

Causal wear is split into **classification**, **causal detection**, **source ownership** and **durability accounting**.

### Wear-rule loading

`WearRules` reloads JSON resources from:

`data/<effect-namespace>/alchemical_leather/wear_rules/<effect-path>.json`

A rule is either explicit `wear: none`, or supplies a positive finite `work_per_damage` threshold and one or more recognized sources. Sources are either:

- `builtin` — a known Alchemical Leather causal detector;
- `event` — a semantic event whose success can only be known by another mod.

Unknown detector types/names, malformed values and coercible-but-wrong JSON types are rejected rather than silently becoming permissive policy. An absent rule is **unclassified**, not implicit no-wear.

### Builtin causal detectors

Mixin hooks are placed at causal decision/consumption sites instead of inferring work from coincident world state whenever Minecraft exposes a suitable boundary.

Examples include:

- self-propelled movement contribution rather than raw displacement;
- actual jump contribution and fall damage prevented by Jump Boost;
- Slow Falling at gravity application;
- effect-owned healing/damage ticks;
- damage genuinely prevented by Resistance/Fire Resistance after ordinary damage gates;
- attack contribution from Strength/Weakness only when damage is actually accepted;
- Weakness consumed by a successful zombie-villager cure;
- Water Breathing at both drowning prevention and underwater air-recovery decisions;
- extended reach only when baseline reach was insufficient across entity, block, item-use, raycast and brushing paths;
- knockback-resistance consumption at every audited target path rather than assuming all callers funnel through `LivingEntity#knockback`;
- successful proc sites such as Infested/Oozing/Wind Charged/Scorching/Soulsteal where appropriate.

Movement predicates explicitly exclude vehicle/passenger travel, passive support displacement, teleportation, knockback/external impulses and locomotion modes the effect does not actually modify.

### Knockback Resistance audit

Minecraft 26.2 reads `KNOCKBACK_RESISTANCE` directly in several mechanics. Beta.2 therefore routes ordinary living knockback, Hoglin, Sonic Boom, Mace, Iron Golem and Arrow reductions through the same `alchemical_leather:knockback_reduced` detector.

Alex's Mobs Continued 2.1.9 also has target-side direct consumers. Linkage-safe `@Pseudo` adapters cover Guster, Bison, Tusklin and Rhinoceros. Reads that belong to the attacking mob itself are not billed to the target's infused armor.

All routes report suppressed impulse magnitude into the same JSON-owned economy. Java does not contain route-specific durability prices. Reconstruction follows the arithmetic of the real consumer, including Guster's `[0,1]` multiplier clamp; the other audited routes follow their actual lower-clamped resistance behavior. The effective Minecraft attribute itself is sanitized to `[-2,1]` before these consumers see it.

### Public semantic-event API

`InfusionWearApi.emit(wearer, effect, event, amount)` is deliberately tiny. A companion reports only that one semantic unit occurred. It does not select armor, inspect infusion mode or apply durability.

`InfusionWear.emitEvent` then verifies:

- global wear is enabled;
- the event amount is finite/positive;
- the effect has a rule accepting that event;
- the ledger reports an armor-owned source for the effect;
- that armor source is currently effective;
- the owning slot still contains an infusion with that effect.

Only then is configured event work accumulated on that exact stack.

### Fractional work and durability

`WearProgress` stores fractional work per effect on the item. `InfusionWear` adds causal work, converts complete `work_per_damage` buckets into integer item damage and keeps only the residual fraction.

Terminal damage goes through Minecraft's ordinary item-damage path. Consequences are intentional:

- alchemical wear and ordinary armor damage share one durability pool;
- the item may break normally at zero durability;
- there is no one-durability floor or dormant infusion mode;
- Unbreakable items remain unbreakable;
- vanilla repair remains vanilla repair;
- Creative/infinite-material players receive no alchemical damage and do not accumulate hidden debt;
- reinfusion removes the old wear component together with the old infusion.

Stable/lingering affects **time expiry**, not causal durability. A stable effect with a real work rule still pays when it performs work.

## First-party companion ownership

### Scale Brews

Scale Brews owns Growth and Shrinking effect-slot/wear resources in its own JAR. Both map to CHEST and explicitly declare `wear: none`. There is no Alchemical Leather Java dependency in Scale Brews.

### Clinging: Reoriented

Clinging: Reoriented owns Reorientation's FEET slot and the semantic gravity events used by Clinging/Reorientation wear. Its optional bridge resolves `InfusionWearApi.emit` reflectively only when Alchemical Leather is present.

It publishes a discrete gravity-turn event only after its authoritative gravity attempt returns success. Continuous Reorientation work is published only for controlled airborne self-locomotion and excludes passenger/support transport, Anatomy support, fluids, Elytra and independent player flight.

Alchemical Leather still performs source/equipment validation. A companion event cannot force an arbitrary item to take damage.

## Leatherworker economy

Villager trade eligibility is intentionally separate from mechanical infusion compatibility. Alchemical Leather contributes Expert timed, Master timed and Master persistent categories through data-driven villager-trade resources/tags.

Runtime policy revalidates armor, effect slot, enchantment state and amplifier ceilings. Reorientation is hard-rejected from trades. Multi-effect and instantaneous trade potions remain excluded even though multi-effect humanoid potions can now be manually infused when they share a slot.

Trade-generated armor uses the same infusion components and runtime/wear engine as manually created armor.

## Cauldron transactions

`UseBlockCallback` remains the common transaction boundary. Validation happens before item/fluid mutation.

Potion cauldrons retain complete `PotionContents` and source bottle type. Storage validity and infusion eligibility remain separate:

- effectful armor application delegates to `ArmorInfusionService` and consumes a dose only after the transformation is valid;
- effectless contents may act as dye-only baths when the caller permits it and preserve existing infusion/enchantment state;
- normal, splash and lingering bottles share ordinary block-use semantics;
- water potions remain on the water-cauldron path.

Tinting rebuilds only the visible custom color while preserving potion identity/effects/name/bottle/dose data.

## Tipped arrows

Native Alchemical Leather potion cauldrons tip arrows without BedrockIfy. Capacity maps the three whole stored doses to 16/32/64 arrows. Complete `PotionContents` is copied to the tipped arrows.

`cauldronTippedArrows` gates only this native interaction. Creative consumes neither source arrows nor potion doses and avoids repeated identical-result duplication.

## Crafting infusion

`ArmorInfusionRecipe` is a dedicated shapeless `CustomRecipe`, not a `DyeRecipe`. It accepts exactly one compatible armor item plus one normal/splash/lingering effectful potion and delegates to `ArmorInfusionService`.

The output is copied, component-safe and forced to count one. It returns a glass bottle. Same-slot humanoid multi-effect bundles are valid; cross-slot/duplicate-invalid bundles, malformed/effectless contents, enchantments, incompatible armor and ambiguous inputs are rejected atomically.

## Dyed-water subsystem and client rendering

`alchemical_leather:dyed_water_cauldron` uses six logical levels and a block entity for RGB. Dyes use Minecraft-style brightness-preserving mixing. Armor recoloring, bottles, buckets and water-tint removal consume the documented unit amounts.

Both custom cauldron block entities expose immutable render-data snapshots. The client tint path never reads mutable block entities from chunk-meshing threads. Tint-only block-entity updates explicitly dirty only the containing client section so existing terrain geometry is remeshed immediately.

## BedrockIfy ownership

BedrockIfy is optional and linked reflectively only at the narrow boundary needed for foreign cauldron state.

Ownership remains per interaction:

- Alchemical Leather owns its native potion/dyed-water blocks;
- BedrockIfy owns ordinary interactions and arrow tipping on its own potion cauldron;
- Alchemical Leather may apply compatible armor semantics to canonical foreign potion doses without replacing BedrockIfy's block ownership;
- non-canonical fractional imported potion levels are rejected;
- BedrockIfy's deliberate ordinary `DyeRecipe` revocation is treated as foreign policy, not patched around.

There is no handler clearing, registry replacement or mixin-priority contest.

## Configuration

`AlchemicalConfig` persists JSON defaults and currently exposes:

- `cauldronTippedArrows`, default `true`;
- `infusionWear`, default `true`.

Missing/non-boolean fields fall back to defaults. A malformed/unreadable whole file logs a warning and uses defaults for that launch.

## Enchanting and item transforms

All three infusion representations are mutually exclusive with enchantments. Effectless dye-only application does not create an infusion and may therefore recolor enchanted compatible armor without violating that invariant.

Standard component-preserving transforms such as trims can retain infusion and wear data. Administrative component editing and third-party direct mutation of internal live-effect maps remain outside the balancing contract.

## VanillaPlus validation architecture

The beta.2 compatibility gate loads the exact pack-pinned **potion/effect contributors** plus BedrockIfy:

- Alex's Mobs Continued 2.1.9;
- Clinging: Reoriented at `df1cff3a2fb9baf69d3bb8594159681b6966096d`;
- Scale Brews at `74066349eb33872d1f2b8584dfd05ffd96eae0f8`;
- Friends & Foes 4.0.27+mc26.2;
- Wilder Wild 4.2.11-mc26.2;
- Deeper Dark 4.4.1;
- BedrockIfy 1.11.8+mc26.2 and required runtime libraries.

Clinging and Scale are fetched by exact commit SHA and built inside CI. Third-party JARs come from the exact CDN URLs recorded by `R3Neer/VanillaPlus-26.2` packwiz metadata.

`PotionCoverageTests` walks the **loaded potion registry** and fails if any effect lacks an explicit wear classification. Additional policy sentinels pin important slots and no-wear/wear decisions. `CompanionWearBridgeTests` exercises the real Clinging semantic bridge through the public API into slot-aware Alchemical Leather accounting, while the beta.2 final audit adds direct-route holdouts including real vanilla knockback consumers and a real Alex's Mobs Bison path.

The standalone server suite, client GameTest and compatibility fixture are all required. `verifyGameTestEntrypoints` additionally prevents compiled server GameTests from silently disappearing because somebody forgot to register them, an historical failure mode sufficiently embarrassing to deserve permanent automation.

See [validation.md](validation.md) for execution evidence.
