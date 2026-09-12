# Alchemical Leather — player guide

[← Back to README](../README.md)

This is the detailed reference. The README deliberately leaves some mechanics for discovery; this page does not.

## Contents

- [How it works](#how-it-works)
- [Which armor is compatible?](#which-armor-is-compatible)
- [Humanoid armor](#humanoid-armor)
- [Animal / BODY armor](#animal--body-armor)
- [Potion types and timing](#potion-types-and-timing)
- [Leatherworker trades](#leatherworker-trades)
- [Dyed water and washing](#dyed-water-and-washing)
- [Installation](#installation)
- [Mod compatibility](#mod-compatibility)
- [Datapack support](#datapack-support)
- [Building](#building)
- [License](#license)

---

## How it works

1. Pour an effectful potion into a normal cauldron. An Alchemical Leather potion cauldron holds up to three identical bottles.
2. Use an **unenchanted compatible dyeable armor item** on it. One dose is consumed, the armor stores the infusion and takes on the potion color.
3. Equip the armor to receive its effect or effects.
4. Unequip it to pause its own timed infusion clocks.
5. Wash it in ordinary water when you want to remove both Alchemical Leather infusion data and dye color.

Normal, splash and lingering potions are all poured with ordinary **right-click / Use**. When a splash or lingering potion is aimed at an Alchemical Leather cauldron, the cauldron interaction consumes it instead of throwing it.

Reinfusing an item replaces its previous Alchemical Leather infusion. Infused armor cannot be enchanted, and enchanted armor cannot be infused. Custom names, durability, trims and unrelated item components are preserved by Alchemical Leather's cauldron transactions.

## Which armor is compatible?

Compatibility is based on **actual dyeability**, not on the word “leather” or on a hard-coded list of item IDs.

An item must first be equippable in an armor slot. Alchemical Leather then recognizes dyeability from standard Minecraft/mod conventions:

1. membership in `#minecraft:cauldron_can_remove_dye`;
2. a loaded `minecraft:crafting_dye` recipe that recolors the result item **in place**; or
3. the fallback tag `#alchemical_leather:dyeable_armor` for custom dye systems that cannot be inferred automatically.

This covers vanilla leather player armor, leather horse armor and wolf armor, and lets standard modded dyeable armor work without per-item integration code. A recipe that converts item A into a different dyed item B does **not** make A compatible merely because B is dyed.

The server is authoritative for this classification. Multiplayer clients use a broader armor-only prediction check so datapack or server-side dyeability rules do not have to be duplicated on the client.

## Humanoid armor

HEAD, CHEST, LEGS and FEET keep Alchemical Leather's original body-part rules. A humanoid piece stores **one effect**, and a multi-effect potion is rejected for it.

| Armor slot | Built-in / tested effect mappings |
|---|---|
| Helmet | Night Vision, Invisibility, Water Breathing, Blindness (Deeper Dark), Lava Vision (Alex's Mobs) |
| Chestplate | Strength, Weakness, Regeneration, Fire Resistance, Poison, Instant Health, Instant Damage, Wind Charged, Oozing, Infested, Growth, Shrinking, Poison Resistance, Bug Pheromones, Soulsteal, Reaching, Reach Boost, Scorching |
| Leggings | Speed, Slowness, Jump Boost, Weaving |
| Boots | Slow Falling, Knockback Resistance, Clinging, Reorientation (Clinging Reoriented) |

The table is slot-based, not item-based: a compatible modded dyeable helmet follows the same helmet rules as a leather cap.

## Animal / BODY armor

Dyeable armor equipped in Minecraft's **BODY / animal-armor slot** follows a deliberately different rule:

- it may accept any effectful potion; there is no humanoid body-part mapping;
- it stores **one potion at a time**;
- if that one potion contains several effects, all of those effects stay together in the infusion;
- repeated entries of the same effect keep independent timers, while only the strongest currently applicable source is projected;
- reinfusion replaces the previous potion bundle rather than accumulating another potion.

Vanilla leather horse armor and wolf armor are supported. A multi-effect potion such as Turtle Master is valid on compatible BODY armor even though it is rejected on humanoid armor.

Instant effects are consumed before firing so they cannot replay. If a potion contains both an instant effect and timed siblings, only the instant entry is removed; the remaining effects stay on the armor.

## Potion types and timing

| Potion | Infusion behavior |
|---|---|
| Normal | Keeps each effect's original level and duration. Time passes only while the armor is equipped. |
| Splash | Uses the same timed armor behavior as a normal potion after being poured. |
| Lingering | Each non-instant effect remains stable indefinitely while the armor is equipped. |
| Instant effect | Activates once when the armor is equipped, then that instant entry is consumed. |

Armor-owned effects and ordinary external Minecraft effects keep separate clocks. If both provide the same effect, Alchemical Leather projects the currently appropriate visible winner without deleting the external source. Removing the armor reveals any surviving external effect again.

## Leatherworker trades

High-level Leatherworkers can sell armor that is already infused. This is a deliberately narrower economy than the general infusion system: **being compatible with Alchemical Leather does not automatically make an armor item or potion eligible for villager trading**.

| Leatherworker level | Infused trade | Default armor | Infusion ceiling | Default emerald price | Uses before restock |
|---|---|---|---|---:|---:|
| I–III | None | — | — | — | — |
| IV — Expert | Timed | Leather leggings or boots | Level I | 12–14 | 3 |
| V — Master | Advanced timed | Leather helmet, chestplate, leggings, boots, leather horse armor or wolf armor | Level II | 17–28 | 2 |
| V — Master | Persistent | Same Master armor pool | Level I, stable | 29–39 + 1 Dragon's Breath | 1 |

Normal villager reputation, demand and discounts can change the emerald payment. The **Dragon's Breath remains a separate second cost**, so reaching the End is still required for the default persistent-villager route.

The default potion pools are intentionally curated:

| Tier / slot | Default choices |
|---|---|
| Expert leggings | Speed I, Jump Boost I |
| Expert boots | Slow Falling I |
| Master timed helmet | extended Night Vision I, Water Breathing I, Invisibility I |
| Master timed chestplate | Strength II, Regeneration II, extended Fire Resistance I; Growth II / Shrinking II when Scale Brews is installed |
| Master timed leggings | Speed II, Jump Boost II |
| Master timed boots | extended Slow Falling I; extended Clinging I when Alex's Mobs is installed |
| Master timed BODY | union of the curated Master timed effects above |
| Master persistent helmet | Night Vision I, Water Breathing I |
| Master persistent chestplate | Fire Resistance I, Strength I, Regeneration I; Growth I / Shrinking I when Scale Brews is installed |
| Master persistent leggings | Speed I, Jump Boost I |
| Master persistent boots | Slow Falling I; Clinging I when Alex's Mobs is installed |
| Master persistent BODY | union of the curated persistent effects above |

Several limits are enforced by code rather than relying only on tags:

- Expert never sells infused helmets, chestplates or animal/BODY armor.
- Villager trades never sell a level-III-or-higher infusion.
- Persistent villager infusions are always level I; therefore Scale Brews Growth/Shrinking can never exceed level I when persistent.
- **Reorientation is never a Leatherworker trade**, even though players can still infuse it manually where normal Alchemical Leather rules allow it.
- Multi-effect and instantaneous potions are not sold as infused armor.
- The default persistent pool also reserves Invisibility, Turtle Master, Wind Charged, Oozing, Infested and Weaving rather than turning the villager into a replacement for brewing or exploration.

In an otherwise vanilla Leatherworker trade pool, Minecraft still chooses two offers per high-level trade set. Adding one Alchemical candidate to Expert makes that category appear in **2/3** of Expert selections. Master has two vanilla candidates plus the two Alchemical categories, so each Alchemical category appears in **1/2** of Master selections, at least one appears in **5/6**, and both appear together in **1/6**. Datapacks or other mods that extend the same villager-trade tags can naturally change those probabilities.

Trade-generated armor uses the potion's color and the same Alchemical Leather components as manually infused equipment, so its runtime behavior, washing rules and enchantment exclusion are identical after purchase.

## Dyed water and washing

Alchemical Leather includes its own colored-water cauldron mechanic; **BedrockIfy is not required**.

Use any item carrying Minecraft's standard `DYE` component on a water cauldron. The water becomes dyed, and adding another dye blends colors using Minecraft-style brightness-preserving color mixing. Reapplying a dye that would not change the color consumes nothing.

The same `DYE` items can tint an Alchemical Leather potion cauldron. The tint blends with the potion's current visible color while preserving potion identity, effects, custom name, bottle type and dose count. A no-op tint consumes no dye.

Internally the colored cauldron uses six fluid units so armor recoloring can be finer-grained than ordinary three-level water:

- vanilla water levels 1 / 2 / 3 become dyed-water levels 2 / 4 / 6;
- recoloring one compatible armor item consumes 1 unit;
- a glass bottle requires and consumes 2 units and returns an ordinary water potion;
- an empty bucket can extract water only from level 6;
- adding a water potion below level 6 removes the tint and converts the remaining amount back to ordinary water; a full level-6 dyed cauldron accepts the gesture without consuming the potion;
- pouring a water bucket in clears the tint and produces a full ordinary water cauldron through Minecraft's normal bucket interaction.

Colored water blends with an armor item's existing `DYED_COLOR`; it does not remove its infusion. Ordinary water does the opposite job: washing compatible armor removes `DYED_COLOR`, `alchemical_leather:infusion` and `alchemical_leather:animal_infusion`, consuming one vanilla water level. Merely dyed compatible armor can be washed too.

## Installation

Alchemical Leather must be installed on both client and server.

- Minecraft 26.2
- Fabric Loader 0.19.5 or newer
- Fabric API 0.159.0+26.2 or newer
- Java 25

Download the regular JAR from [releases](https://github.com/R3Neer/alchemical-leather/releases) and place it in the instance's `mods` folder.

## Mod compatibility

Alchemical Leather works without optional content mods. Standard modded dyeable armor can be discovered automatically through the rules above; that is a compatibility mechanism, not a promise that every mod combination has been playtested.

The alpha.3 CI integration fixture specifically exercises:

- **Clinging Reoriented 0.1.0-alpha.6**;
- **Scale Brews 0.1.0-beta.5**;
- **BedrockIfy 1.11.8+mc26.2**;
- **Alex's Mobs Continued 2.1.9**;
- the Gravity Changer, CodxLib and Cloth Config versions required by the Clinging fixture.

With Alex's Mobs and Clinging Reoriented, Clinging and Reorientation can both still be manually infused into compatible BODY armor and follow their existing humanoid rules. The villager economy is deliberately different: Clinging can enter Master trade pools, while **Reorientation is hard-banned from every Leatherworker trade**.

With Scale Brews, Growth/Shrinking II may appear in Master timed armor. Persistent villager equipment is restricted to Growth/Shrinking I, and level III is never sold; brewing therefore remains necessary for the strongest scale effects.

BedrockIfy remains optional. When its cauldron feature is active, BedrockIfy owns its own potion/colored-water blocks and the vanilla-water-plus-dye entry point. Alchemical Leather only intercepts its own armor-specific actions there, preserving BedrockIfy's block and consuming exactly one compatible dose/unit. If BedrockIfy's cauldron feature is absent, disabled or cannot be positively verified, Alchemical Leather's native dyed-water path remains available.

Earlier alpha validation also exercised Friends&Foes, Wilder Wild, Deeper Dark, Additional Additions, Enchancement, Functional Armor Trims and Grind Enchantments. See [validation.md](validation.md) for current and historical fixture boundaries.

## Datapack support

### Effect-to-slot rules

Humanoid effect placement is controlled by datapack files at:

```text
data/<effect_namespace>/alchemical_leather/effect_slots/<effect_path>.json
```

Example:

```json
{
  "slot": "chestplate"
}
```

Valid values are `helmet`, `chestplate`, `leggings` and `boots`. Rules may also use `enabled`, `requires_mod`, `requires_resource` and `requires_effect` for optional integrations. BODY armor does not use this table.

### Custom dyeable armor fallback

If a mod implements a custom dye system that neither uses `#minecraft:cauldron_can_remove_dye` nor a self-recoloring `minecraft:crafting_dye` recipe, add its actual armor item to:

```text
#alchemical_leather:dyeable_armor
```

The item must still have an `EQUIPPABLE` component whose slot is an armor slot. The tag does not turn arbitrary equipment into armor.

### Leatherworker economy pools

Villager eligibility is opt-in and separate from general dyeability. Armor can be added to:

```text
#alchemical_leather:leatherworker/expert_armor
#alchemical_leather:leatherworker/master_armor
```

Potion pools are split by tier and slot:

```text
#alchemical_leather:leatherworker/expert/<slot>
#alchemical_leather:leatherworker/master_timed/<slot>
#alchemical_leather:leatherworker/master_persistent/<slot>
```

where `<slot>` is `head`, `chest`, `legs`, `feet` or `body` as applicable. Expert itself only accepts `legs` and `feet`.

Adding an ID to one of these tags does **not** bypass runtime policy. The result must still be genuinely compatible dyeable armor, must not start enchanted, humanoid effects must match their slot, Expert remains level-I legs/feet only, no villager tier can sell level III+, persistent trades remain level I, and Reorientation remains forbidden.

Optional mod entries can use ordinary optional tag entries (`required: false`), so no Java dependency is necessary merely to extend a trade pool.

## Building

Use Java 25 and the included Gradle wrapper:

```powershell
.\gradlew.bat build
```

Additional test task:

```powershell
.\gradlew.bat runClientGameTest
```

`build` runs the required server GameTests. Alpha.3 CI also runs the client GameTest under Xvfb and a real Clinging Reoriented + Scale Brews + BedrockIfy + Alex's Mobs compatibility fixture. The alpha.3 candidate passes **41 required server GameTests** in both the standalone and real optional-mod runs; the client suite also passes synchronization, render-data and equip/unequip assertions. See [validation.md](validation.md) for coverage and remaining manual checks.

## License

Alchemical Leather is available under the [GNU General Public License v3.0 or later](../LICENSE).
