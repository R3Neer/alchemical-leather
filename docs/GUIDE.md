# Alchemical Leather — player guide

[← Back to README](../README.md)

This guide describes **Alchemical Leather 0.1.0-beta.2** for Minecraft 26.2 Fabric, including causal infusion wear, same-slot humanoid multi-effect potions and the optional compatibility protocol.

## Contents

- [How it works](#how-it-works)
- [Which armor is compatible?](#which-armor-is-compatible)
- [Humanoid armor](#humanoid-armor)
- [Animal / BODY armor](#animal--body-armor)
- [Potion types and timing](#potion-types-and-timing)
- [Causal infusion wear](#causal-infusion-wear)
- [Tipped arrows](#tipped-arrows)
- [Crafting-table infusion](#crafting-table-infusion)
- [Leatherworker trades](#leatherworker-trades)
- [Dyed water and washing](#dyed-water-and-washing)
- [Mod compatibility](#mod-compatibility)
- [Configuration](#configuration)
- [Datapack support](#datapack-support)
- [Building](#building)
- [License](#license)

---

## How it works

1. Pour a non-water potion into a normal cauldron. An Alchemical Leather potion cauldron holds up to three identical bottles, even when the potion has no effects.
2. With an effectful potion, use an **unenchanted compatible dyeable armor item** on it. One dose is consumed, the armor stores the infusion and takes on the potion color. You can instead combine the armor with one normal, splash or lingering potion in a crafting grid.
3. With an effectless potion such as Awkward, Mundane or Thick, use any compatible dyeable armor item on the cauldron. One dose is consumed and the armor takes on the liquid color, but no infusion is created or replaced. Effectless potions do not match the crafting infusion recipe.
4. Plain arrows can be dipped into an Alchemical Leather potion cauldron to create tipped arrows directly; BedrockIfy is not required.
5. Equip infused armor to receive its effect or effects. Unequip it to pause its own timed infusion clocks.
6. While equipped, effects with a causal wear policy consume armor durability only when they perform attributable mechanical work.
7. Wash compatible armor in ordinary water when you want to remove both Alchemical Leather infusion data and dye color.

Normal, splash and lingering potions are all poured with ordinary **Use**. A splash or lingering potion aimed at an Alchemical Leather cauldron is consumed by the cauldron interaction instead of being thrown. Valid potion contents can be stored even when a particular armor piece could not accept them; target eligibility is checked only when effectful liquid is applied to armor.

Effectless potion cauldrons are deliberately **dye-only**. They can recolor compatible armor that is already enchanted or infused while preserving enchantments, infusions, names, durability, trims and unrelated components. They never create an empty infusion. Water potions keep ordinary water-cauldron semantics.

Reinfusing an item with an effectful potion replaces its previous Alchemical Leather infusion **atomically** and clears wear debt that belonged to the previous infusion. Infused armor cannot be enchanted, and enchanted armor cannot receive an effectful infusion.

## Which armor is compatible?

Compatibility is based on **actual dyeability**, not the word “leather” or a hard-coded list of item IDs.

An item must first be equippable in an armor slot. Alchemical Leather then recognizes dyeability from standard Minecraft/mod conventions:

1. membership in `#minecraft:cauldron_can_remove_dye`;
2. a loaded `minecraft:crafting_dye` recipe that recolors the result item **in place**; or
3. `#alchemical_leather:dyeable_armor` as an explicit fallback for custom dye systems that cannot be inferred automatically.

This covers vanilla leather player armor, leather horse armor and wolf armor, and allows conventional modded dyeable armor to work without per-item Java integration. A recipe that transforms item A into different item B does not make A dyeable merely because B receives `DYED_COLOR`.

The server is authoritative. Multiplayer clients use a broader armor-only prediction check so datapack/server dyeability rules do not need a second client-side index.

## Humanoid armor

HEAD, CHEST, LEGS and FEET use body-part effect mappings. The mapping is slot-based, not item-based: compatible modded dyeable leggings obey the same potion-slot rule as vanilla leather leggings.

| Armor slot | Built-in / tested mappings |
|---|---|
| Helmet | Night Vision, Invisibility, Water Breathing, Blindness (Deeper Dark), Lava Vision (Alex's Mobs) |
| Chestplate | Strength, Weakness, Regeneration, Fire Resistance, Poison, Instant Health, Instant Damage, Wind Charged, Oozing, Infested, Growth, Shrinking, Poison Resistance, Bug Pheromones, Soulsteal, Reaching, Reach Boost, Scorching |
| Leggings | Speed, Slowness, Jump Boost, Resistance, Weaving |
| Boots | Slow Falling, Knockback Resistance, Clinging, Reorientation (Clinging: Reoriented) |

### Multi-effect humanoid potions

Humanoid armor is no longer limited to exactly one effect. A potion may carry several **distinct** effects when every effect maps to the **same actual armor slot**.

For example, Turtle Master can live on leggings because its Slowness and Resistance components are both mapped to LEGS. The bundle is one atomic infusion: reinfusion replaces it as a unit, but each constituent effect keeps independent timing, projection and causal wear accounting.

A multi-effect potion is rejected when:

- its effects map to different humanoid slots;
- any required effect lacks a valid slot mapping;
- the bundle contains duplicate entries of the same effect where the humanoid representation requires distinct effects;
- the target's actual equipment slot does not match the common mapped slot.

Effectless dye-only liquid has no effect to map and may recolor any compatible humanoid armor without creating or replacing an infusion.

## Animal / BODY armor

Dyeable armor equipped in Minecraft's **BODY / animal-armor slot** follows a deliberately different rule:

- it may accept any effectful potion; there is no humanoid body-part mapping;
- it stores **one potion at a time**;
- if that potion contains several effects, all remain together in the infusion;
- repeated entries of the same effect can keep independent timers while only the strongest currently applicable source is projected;
- reinfusion replaces the previous potion bundle instead of accumulating another potion.

Vanilla leather horse armor and wolf armor are supported. Multi-effect potions are naturally valid on BODY armor. An effectless potion only recolors BODY armor and never creates an empty `animal_infusion`; an existing BODY infusion is preserved.

Instant entries are consumed before firing so they cannot replay. If a potion contains instant and non-instant siblings, only the instant entries are consumed.

## Potion types and timing

| Potion | Infusion behavior |
|---|---|
| Normal | Keeps each effect's original level and duration. Its armor-owned clock advances only while equipped. |
| Splash | Uses the same timed armor behavior as a normal potion once infused. |
| Lingering | Non-instant entries are stable/non-expiring while equipped. Stable does **not** mean zero durability wear. |
| Instant effect | Activates once when equipped, then that instant entry is consumed. |
| No effects | Valid cauldron dye bath only; no crafting-table infusion. |

Armor-owned and ordinary external effects keep separate source state. Alchemical Leather projects the appropriate visible winner without deleting the external source. If an external equal/stronger source eclipses the armor source, the surviving armor clock/state stays on the item and becomes visible again when appropriate.

That source arbitration also matters to durability: an eclipsed armor effect does not pay for work it did not provide.

## Causal infusion wear

Infusion wear is designed around a simple invariant:

> **Armor pays only when its infusion performs attributable mechanical work.**

Merely having an effect icon is not work.

The system first verifies that wear is enabled, the relevant infusion is currently equipped, the reported effect belongs to that exact item/slot, the armor is the effective source, and the effect has an explicit wear rule. Only then can a builtin causal detector or a semantic event contribute work.

### Examples

- **Speed / Slowness:** self-propelled locomotion actually modified by movement speed. Vehicle travel, passive moving platforms, teleports, knockback and unrelated external impulses do not count.
- **Jump Boost:** a real boosted jump, plus fall damage actually prevented by Jump Boost. Vehicle jumps and external launches do not count.
- **Slow Falling:** physics ticks where Slow Falling actually changes gravity/fall-flying behavior; it is not charged for a rocket impulse merely because an Elytra flight exists.
- **Regeneration / Poison:** HP actually restored/removed by the effect's own tick.
- **Fire Resistance / Resistance:** damage genuinely prevented by that effect rather than damage already cancelled by another immunity or invulnerability gate.
- **Strength / Weakness:** the attributable contribution/suppression on a successful damage result. Weakness also pays once when a successful zombie-villager cure actually requires and consumes the effect.
- **Water Breathing:** drowning/breath loss actually prevented, plus underwater air recovery when Water Breathing is what enables it.
- **Reach / Reach Boost:** successful entity, block, item-use, raycast or brushing work that really required the extra range.
- **Knockback Resistance:** impulse magnitude genuinely suppressed where the mechanic consumes the attribute. Beta.2 covers the direct vanilla consumers and the supported Alex's Mobs Continued 2.1.9 target-knockback routes exercised by the compatibility fixture.
- **Soulsteal:** HP actually restored by the successful Soulsteal proc.
- **Scorching:** attributable fire/ignition work at the real mechanic's origin.
- **Clinging:** successful voluntary gravity turns only.
- **Reorientation:** successful turns plus controlled airborne self-locomotion. Elytra, fluid movement, independent player flight, riding and support transport do not count as continuous Reorientation work.
- **Growth / Shrinking:** explicit `wear: none` because maintaining body size is a persistent state rather than operating work.

Other passive/visual states may also explicitly use `wear: none` where no robust causal detector is justified.

### Fractional work and real durability

Each wear rule converts work into durability with its own `work_per_damage` threshold. Work can be fractional and is stored **on the infused item, per effect**. Multi-effect armor can therefore owe different fractions for different effects without double-charging unrelated work.

When a bucket reaches its threshold, Alchemical Leather applies ordinary Minecraft item damage:

- normal armor damage and alchemical wear use the same durability pool;
- armor **can break normally** at zero durability;
- there is no one-durability floor, dormant infusion or special reactivation mechanic;
- ordinary vanilla repair behavior remains ordinary repair behavior;
- `UNBREAKABLE` items are not damaged;
- Creative/infinite-material players neither receive alchemical damage nor bank hidden wear debt to be paid later;
- reinfusion removes obsolete fractional debt together with the old infusion.

### Disabling wear

`config/alchemical-leather.json` contains:

```json
{
  "cauldronTippedArrows": true,
  "infusionWear": true
}
```

Set `infusionWear` to `false` to disable the causal durability system while retaining infusion effects/timing. Missing or non-boolean fields fall back to defaults; malformed whole files are ignored for that launch with defaults and a warning.

## Tipped arrows

Alchemical Leather potion cauldrons can create tipped arrows **without BedrockIfy**. Use plain arrows on the cauldron; the result receives the stored complete `POTION_CONTENTS`, including custom effects, tint and custom potion-name data carried by that component.

| Stored doses | Maximum arrows tipped in one interaction |
|---:|---:|
| 1 | 16 |
| 2 | 32 |
| 3 | 64 |

1–16 arrows consume one dose, 17–32 consume two, and 33–64 consume three. If more arrows are supplied than capacity, only the supported amount is tipped. Creative consumes neither source arrows nor potion doses and avoids repeatedly inserting an identical result stack.

`cauldronTippedArrows=false` disables only Alchemical Leather's native arrow interaction and yields the gesture with `PASS`.

When BedrockIfy's cauldron feature is active, **BedrockIfy remains sole owner of arrows on `bedrockify:potion_cauldron`**.

## Crafting-table infusion

The special shapeless recipe requires exactly:

- one compatible armor item; and
- one normal, splash or lingering potion with effects.

It uses the same transformation kernel as cauldron infusion. Humanoid single- and valid same-slot multi-effect bundles follow the rules above; BODY armor keeps the complete potion bundle. Normal/splash create timed non-instant entries and lingering creates stable entries.

The recipe copies the armor, preserves unrelated components such as name/durability/trims, transfers visible potion color, returns a glass bottle and always outputs exactly one armor item. A valid reinfusion atomically replaces the old infusion and its wear progress.

It rejects enchanted armor, incompatible armor, effectless/malformed potion input, wrong-slot humanoid effects, cross-slot humanoid bundles, ambiguous multiple candidates and unrelated extra ingredients.

This is not a `minecraft:crafting_dye` recipe and does not bypass BedrockIfy's deliberate ordinary dye-recipe policy.

## Leatherworker trades

High-level Leatherworkers can sell already-infused armor. This economy is intentionally narrower than general infusion compatibility.

| Level | Infused category | Default armor | Ceiling |
|---|---|---|---|
| I–III | none | — | — |
| IV Expert | timed | leggings / boots | Level I |
| V Master | advanced timed | humanoid + BODY pools | Level II |
| V Master | persistent | same Master pool | Level I stable + Dragon's Breath second cost |

Runtime policy still validates dyeability, slot mapping, enchantment state and effect level. Reorientation is explicitly excluded from Leatherworker trades. Multi-effect and instantaneous potions are not sold as infused armor. Persistent Scale Brews villager equipment is limited to Growth/Shrinking I.

Trade-generated armor uses the same components, color, runtime projection and causal wear system as manually infused equipment.

## Dyed water and washing

Alchemical Leather includes native colored water; BedrockIfy is not required.

Use any item carrying Minecraft's standard `DYE` component on a water cauldron. Colors blend using Minecraft-style brightness-preserving mixing. The same dye items can tint an Alchemical Leather potion cauldron without changing potion identity, effects, custom name, bottle type or dose count.

The dyed-water cauldron uses six logical units:

- vanilla water levels 1 / 2 / 3 become dyed-water levels 2 / 4 / 6;
- recoloring one compatible armor item consumes 1 unit;
- a glass bottle consumes 2 units and returns a water potion;
- a bucket extracts only at level 6;
- a water potion below level 6 removes tint and converts remaining amount back to ordinary water;
- a water bucket clears tint and delegates to vanilla refill behavior.

Colored water changes dye but preserves infusion. Effectless potion dye baths set their visible RGB while preserving any existing infusion/enchantments. Ordinary water washing removes dye plus humanoid/BODY Alchemical infusion components and consumes one vanilla water level.

## Mod compatibility

Alchemical Leather works without optional content mods.

The beta.2 compatibility gate exercises the actual potion/effect contributors used by `R3Neer/VanillaPlus-26.2`:

- Alex's Mobs Continued 2.1.9;
- **Clinging: Reoriented** at validated commit `df1cff3a2fb9baf69d3bb8594159681b6966096d`;
- **Scale Brews** at validated commit `74066349eb33872d1f2b8584dfd05ffd96eae0f8`;
- Friends & Foes 4.0.27+mc26.2;
- Wilder Wild 4.2.11-mc26.2;
- Deeper Dark 4.4.1;
- BedrockIfy 1.11.8+mc26.2;
- their exact required runtime libraries from the VanillaPlus pack metadata.

The first-party repositories own their own compatibility resources. Alchemical Leather's CI fetches the exact validated companion commits rather than following moving development branches.

The loaded potion registry is audited dynamically: every effect appearing in a loaded potion must be classified by a concrete wear rule, explicit `wear: none`, or its intentional instantaneous semantics. Separate sentinels pin important slot/wear expectations so a resource cannot silently migrate to a semantically wrong slot while still being “classified”.

The cross-mod holdout invokes the real Clinging semantic bridge and verifies that repeated Reorientation turns accumulate fractional work on the **owning equipped boots** and eventually produce ordinary durability damage at the configured threshold. The final beta.2 audit also exercises a real Alex's Mobs Bison knockback route so optional target-knockback linkage is not validated merely by successful class loading.

See [COMPATIBILITY.md](COMPATIBILITY.md) for the JSON/API contract and ownership boundaries.

## Configuration

`config/alchemical-leather.json` is created with defaults when absent.

```json
{
  "cauldronTippedArrows": true,
  "infusionWear": true
}
```

- `cauldronTippedArrows`: controls only native arrow-on-Alchemical-potion-cauldron behavior.
- `infusionWear`: controls only causal durability wear. Disabling it does not remove infusion effects or change their duration model.

Unknown/malformed individual field types fall back to defaults. A malformed whole file does not crash startup.

## Datapack support

### Dyeable armor fallback

Use `#alchemical_leather:dyeable_armor` when a custom dye system cannot be inferred from standard tags/self-recoloring recipes.

### Humanoid effect slots

Path:

`data/<effect-namespace>/alchemical_leather/effect_slots/<effect-path>.json`

Example:

```json
{
  "slot": "boots",
  "requires_effect": "example_mod:example_effect"
}
```

Supported slots are `helmet`, `chestplate`, `leggings`, `boots`. Optional guards include `requires_mod`, `requires_effect`, `requires_resource` and `enabled`.

### Wear rules

Path:

`data/<effect-namespace>/alchemical_leather/wear_rules/<effect-path>.json`

Explicit no-wear:

```json
{
  "wear": "none"
}
```

Builtin causal detector:

```json
{
  "work_per_damage": 512.0,
  "sources": [
    {
      "type": "builtin",
      "detector": "alchemical_leather:self_propelled_movement_speed"
    }
  ]
}
```

Semantic event:

```json
{
  "work_per_damage": 30.0,
  "sources": [
    {
      "type": "event",
      "event": "example_mod:successful_action",
      "work": 2.0
    }
  ],
  "requires_effect": "example_mod:example_effect"
}
```

Wear JSON selects known detectors/events and configures balance; it is not an arbitrary scripting language.

### Public semantic API

When only the owning mod can know that its mechanic succeeded:

```java
InfusionWearApi.emit(wearer, effect, event, amount);
```

The emitting mod reports facts only. Alchemical Leather validates ownership/effectiveness and applies configured work/durability.

### Leatherworker economy

Leatherworker armor/potion pools remain data-driven allowlists separate from mechanical compatibility. Extending an infusion mapping does not automatically inject that effect into villager economy.

## Building

Use Java 25 and the included Gradle wrapper.

Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTest
.\gradlew.bat runClientGameTest
```

Unix-like systems:

```bash
./gradlew build
./gradlew runGameTest
./gradlew runClientGameTest
```

`check` also runs `verifyGameTestEntrypoints`, which fails if a server `@GameTest` class is compiled but missing from the Fabric test descriptor, or if the descriptor contains a stale test class. This guard exists because a historically green CI once managed the impressive feat of not actually running every compiled GameTest.

The main CI additionally assembles the exact pinned VanillaPlus potion-contributor fixture, builds the two first-party companions and runs the registry/compatibility matrix. See [validation.md](validation.md).

## License

Alchemical Leather is licensed under [GPL-3.0-or-later](../LICENSE). Optional dependencies retain their own licenses and are test/runtime inputs, not bundled production content.

Not an official Minecraft product; not approved by or associated with Mojang or Microsoft.
