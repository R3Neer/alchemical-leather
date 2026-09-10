# Alchemical Leather — player guide

[← Back to README](../README.md)

This is the detailed reference. The README deliberately leaves some mechanics for discovery; this page does not.

## Contents

- [How it works](#how-it-works)
- [Which armor is compatible?](#which-armor-is-compatible)
- [Humanoid armor](#humanoid-armor)
- [Animal / BODY armor](#animal--body-armor)
- [Potion types and timing](#potion-types-and-timing)
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

Splash and lingering potions are poured with **sneak + right-click**, which prevents them from being thrown accidentally.

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

## Dyed water and washing

Alchemical Leather includes its own colored-water cauldron mechanic; **BedrockIfy is not required**.

Use any item carrying Minecraft's standard `DYE` component on a water cauldron. The water becomes dyed, and adding another dye blends colors using Minecraft-style brightness-preserving color mixing. Reapplying a dye that would not change the color consumes nothing.

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

The alpha.2 CI integration fixture specifically exercises:

- **Clinging Reoriented 0.1.0-alpha.6**;
- **BedrockIfy 1.11.8+mc26.2**;
- **Alex's Mobs Continued 2.1.9**;
- the Gravity Changer, CodxLib and Cloth Config versions required by that Clinging fixture.

With Clinging Reoriented, Clinging and Reorientation potions can be infused into compatible BODY armor as well as following their humanoid boots mapping. The CI fixture tests the real registered effects on both leather horse armor and wolf armor.

BedrockIfy remains optional. When its cauldron feature is active, BedrockIfy owns its own potion/colored-water blocks and the vanilla-water-plus-dye entry point. Alchemical Leather only intercepts its own armor-specific actions there, preserving BedrockIfy's block and consuming exactly one compatible dose/unit. If BedrockIfy's cauldron feature is absent, disabled or cannot be positively verified, Alchemical Leather's native dyed-water path remains available.

Earlier alpha validation also exercised Scale Brews, Friends&Foes, Wilder Wild, Deeper Dark, Additional Additions, Enchancement, Functional Armor Trims and Grind Enchantments. See [validation.md](validation.md) for the distinction between current alpha.2 CI evidence and historical alpha.1 fixture coverage.

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

## Building

Use Java 25 and the included Gradle wrapper:

```powershell
.\gradlew.bat build
```

Additional test task:

```powershell
.\gradlew.bat runClientGameTest
```

`build` runs the required server GameTests. CI also runs the client GameTest under Xvfb and a real Clinging Reoriented + BedrockIfy compatibility fixture. At the alpha.2 feature freeze, both server runs passed all **32 required GameTests**, and the client test passed synchronization, render-data and equip/unequip assertions. See [validation.md](validation.md) for coverage and remaining manual checks.

## License

Alchemical Leather is available under the [GNU General Public License v3.0 or later](../LICENSE).
