# Alchemical Leather

> Leather armor can absorb potion effects from potion-filled cauldrons.

Alchemical Leather is a Fabric mod for Minecraft 26.2. It lets the four vanilla leather armor pieces hold one potion effect each, with the effect assigned to an appropriate equipment slot.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DDBD3B)](https://fabricmc.net/)

## The familiar cauldron

There is no new block, item, recipe, screen, or crafting station. You use the ordinary Minecraft cauldron: it keeps its vanilla item, name, recipe, body, outline, and placement behavior. Only its liquid changes when it contains a potion. Breaking it or using pick-block returns an ordinary cauldron.

![Three ordinary cauldrons: potion liquid, empty vanilla cauldron, and a second potion cauldron](docs/images/vanilla-cauldron.png)

## How it plays

1. Fill a cauldron with a potion. Use **sneak + right-click** for splash and lingering potions.
2. Add up to three identical bottles. Different bottle types or potion contents never mix.
3. Right-click the liquid with unenchanted vanilla leather armor. One dose gives one infusion, if the effect belongs to that armor slot.
4. Equip the piece to activate the effect.
5. Wash it in a water cauldron to remove the infusion and leather dye, consuming one water level.

![Infused vanilla leather leggings grant Speed II while equipped](docs/images/infused-leather.png)

An infusion replaces the previous effect, amplifier, duration, and leather color. It never combines effects, raises a level, or extends a duration. Potions carrying several effects, such as Turtle Master, are rejected without consuming a dose.

## Infusion rules

| Potion source | Result while equipped |
|---|---|
| Normal potion | The original duration runs only while worn. It pauses in inventory. |
| Splash potion | Same as a normal potion, poured with sneak + right-click. |
| Lingering potion | A non-instant effect remains active permanently at its original amplifier. |
| Instant effect | Activates once on equipping, then the infusion is consumed. |

The armor remains vanilla leather. Its trim, custom name, damage, and unrelated components are preserved. Infused armor cannot receive legitimate survival enchantments; existing enchanted armor cannot be infused. Rename it or repair it with leather in an anvil, and wash it before combining armor pieces or using a grindstone.

## Effect slots

Effect-to-slot assignments are datapack data, not a hard-coded potion list.

| Equipment slot | Included effects |
|---|---|
| Helmet | Night Vision, Invisibility, Water Breathing, Deeper Dark Blindness, Alex's Mobs Lava Vision |
| Chestplate | Strength, Weakness, Regeneration, Fire Resistance, Poison, Instant Health/Damage, Wind Charged, Oozing, Infested, Scale Brews Growth/Shrinking, Alex's Mobs body effects, Friends&Foes Reaching, Wilder Wild Reach Boost/Scorching |
| Leggings | Speed, Slowness, Jump Boost, Weaving |
| Boots | Slow Falling, Alex's Mobs Knockback Resistance and Clinging |

Growth and Shrinking compete for the chestplate. Speed and Jump Boost compete for leggings. Lingering Growth III and Shrinking III deliberately stay level III for as long as the chestplate is worn.

## Installation

Install on **both client and server**:

1. Install Java 25, Minecraft 26.2, Fabric Loader 0.19.5 or later, and Fabric API 0.159.0+26.2 or later.
2. Download the release JAR and place it in the instance or server's `mods` directory.
3. Start the game. No configuration is required for vanilla use.

The current first release is `0.1.0-alpha.1`.

## Optional mod compatibility

No optional mod is required. Alchemical Leather works standalone and recognizes these audited providers when they are present:

- BedrockIfy 1.11.8: compatible potion-cauldron import at complete doses 1, 2, and 3. Partial arrow doses are left unchanged. Its dyed water can recolor infused leather.
- Scale Brews beta.3 and beta.4: Growth and Shrinking, including uncapped lingering level III infusions.
- Alex's Mobs Continued 2.1.9, Friends&Foes 4.0.27, Wilder Wild 4.2.11, Deeper Dark 4.4.1, Additional Additions 10.0.12, Enchancement 26.2-r4, Functional Armor Trims 2.2.1, and Grind Enchantments 4.2.1+26.1.2.

The technical full-cauldron block is internal and has no item or creative-menu entry. BedrockIfy uses a different storage model, so custom potion data already lost by BedrockIfy cannot be recovered. Converted cauldrons use Alchemical Leather's three-dose rules and no longer tip arrows.

## Datapacks

Rules live at:

```text
data/<effect_namespace>/alchemical_leather/effect_slots/<effect_path>.json
```

For example:

```json
{"slot":"chestplate"}
```

Supported slots are `helmet`, `chestplate`, `leggings`, and `boots`. A pack can disable a rule with `{"enabled":false}`. It can also use `requires_mod` and `requires_resource` for optional providers. Changing a rule suspends incompatible equipped armor without deleting its stored infusion, so it can still be washed.

## Build and validate

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTest
.\gradlew.bat runClientGameTest
.\gradlew.bat runGameTest '-PcompatMods=C:/path/to/fixture/mods'
```

The final standalone build passed 28 required server GameTests. Compatibility fixtures passed 63 required server GameTests with both Scale Brews beta.3 and beta.4. The client GameTest verified cauldron color synchronization, equipping, effect synchronization, removal on unequipping, and produced the screenshots above.

Read [the validation report](docs/validation.md) for coverage, fixture versions, and the remaining real-game checks. Automated checks do not replace full-modpack client QA for ItemSwapper, dispensers, resource packs, camera/collision effects, death/respawn, reconnects, and Enchancement's alternate overhaul mode.

## License

Alchemical Leather is licensed under the [GNU General Public License v3.0 or later](LICENSE). It does not bundle optional mod binaries or implementation classes. Vanilla geometry and textures are referenced by their Minecraft resource identifiers.
