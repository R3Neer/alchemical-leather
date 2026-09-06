# Alchemical Leather

Leather armor can absorb potion effects from potion-filled cauldrons.

Fabric **Minecraft 26.2**, Java **25**, Fabric Loader **0.19.5+**, Fabric API **0.159.0+26.2**. Install on both client and server. The first local build is **0.1.0-alpha.1**; see `docs/validation.md` for the exact test coverage and remaining playtesting.

## Playing

Use the ordinary Minecraft cauldron. There is no new item, recipe, station or screen. Its appearance and name stay vanilla; the liquid changes when filled with a potion. Breaking or pick-block returns the ordinary cauldron.

1. Right-click a cauldron with a potion. **Sneak + right-click** to pour splash or lingering potions. Without sneaking, a thrown potion aimed at a cauldron is rejected with feedback.
2. Add at most three identical bottles. Different potion contents or bottle types never mix.
3. Right-click the liquid with an unenchanted leather armor piece in your hand. One dose infuses one piece. The effect must match its armor slot.
4. Equip it to activate the effect. Ordinary/splash infusions consume their original duration only while worn. Lingering infusions are permanent while worn, at the original level.
5. Instantaneous effects activate once on equipping and consume the infusion, even from lingering potions.
6. Clean water in a cauldron removes the infusion and dye, consuming one water level.

One effect per piece. Reinfusing replaces its effect, level, remaining time and dye; nothing stacks. A multi-effect potion such as Turtle Master is rejected without consuming anything. The liquid color replaces the previous leather color exactly. Once a temporal/instant infusion is consumed, the color remains and the piece becomes enchantable again.

Infused armor cannot receive survival enchantments. Existing enchanted pieces, including curses, cannot be infused. Renaming and anvil repairs using leather preserve an infusion. Wash before combining two armor pieces or using a grindstone. Trims are preserved and can be applied normally.

## Armor slots

| Slot | Initial effects |
|---|---|
| Helmet | Night Vision, Invisibility, Water Breathing; Deeper Dark Blindness; Alex’s Mobs Lava Vision |
| Chestplate | Strength, Weakness, Regeneration, Fire Resistance, Poison, Instant Health/Damage, Wind Charged, Oozing, Infested; Scale Brews Growth/Shrinking; Alex’s Mobs Poison Resistance, Bug Pheromones, Soulsteal; Friends&Foes Reaching; Wilder Wild Reach Boost/Scorching |
| Leggings | Speed (including Alex’s Mobs Speed III), Slowness, Jump Boost, Weaving |
| Boots | Slow Falling; Alex’s Mobs Knockback Resistance and Clinging |

Growth III and Shrinking III lingering remain level III permanently while the chestplate is equipped. They compete with Strength, Regeneration and Fire Resistance. Speed and Jump Boost compete for leggings. Clinging is boots-only.

## Optional compatibility

No optional mod is required. Tested fixtures use BedrockIfy 1.11.8, Scale Brews beta.4, Alex’s Mobs Continued 2.1.9, Friends&Foes 4.0.27, Wilder Wild 4.2.11, Deeper Dark 4.4.1, Additional Additions 10.0.12, Enchancement 26.2-r4 and Functional Armor Trims 2.2.1. Exact results are recorded separately.

Existing BedrockIfy potion cauldrons can be infused/imported at its canonical internal levels 2/5/8 (one/two/three bottles). A partial dose left by tipped arrows is rejected unchanged. Converted cauldrons follow this mod’s homogeneous three-dose rules; they no longer tip arrows. Other BedrockIfy cauldrons remain under its control. Its dyed water recolors leather while preserving an infusion. BedrockIfy 1.11.8 does not support dyes in potion liquid, so this mod does not add that interaction.

BedrockIfy cannot preserve arbitrary custom effects in its own storage. This mod’s newly filled cauldrons preserve the complete potion contents, including Deeper Dark’s custom Blindness splash. Previously lost data cannot be recovered.

External effects keep independent duration from the armor contribution, including equal levels and hidden stronger/weaker effects. Milk clears the external source; an equipped armor source can reappear on the next tick. Mods writing directly to the internal effect map bypass normal hooks and require separate assessment.

## Datapacks

One rule per effect, independent of potion ID, amplifier, duration or bottle type:

`data/<effect_namespace>/alchemical_leather/effect_slots/<effect_path>.json`

```json
{"slot":"chestplate"}
```

Slots: `helmet`, `chestplate`, `leggings`, `boots`. Override with `{"enabled":false}` to disable a rule. Optional `requires_mod` and `requires_resource` condition a rule on an actual mod or resource. The Blindness rule checks Deeper Dark’s loot-table resource, allowing its datapack form too. Malformed active rules fail reload without publishing a partial classification map. Missing effect IDs on items persist safely, stay inactive and can be washed away.

Changing a rule suspends incompatible equipped infusions without deleting their data. Multi-effect storage and level caps are not configuration options. Adding a rule does not create a new potion or brewing recipe.

## Build and test

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTest
.\gradlew.bat runClientGameTest
.\gradlew.bat runGameTest '-PcompatMods=C:/path/to/fixture/mods'
```

The compatibility fixture must contain the audited optional mods and their dependencies. It is not bundled. The compatibility tests verify actual loading rather than silently passing an empty fixture. `build` uses the Fabric test integration; CI runs server and client checks separately.

No production JAR contains test classes, test-only Saturation rules or optional mod binaries. The component lives on the original vanilla leather items. The technical filled-block ID is `alchemical_leather:potion_cauldron`; it has no item or creative entry.

## License

GPL-3.0-or-later. Optional mods retain their own licenses; none of their implementation classes or assets are bundled. Vanilla geometry/textures are referenced by resource ID.
