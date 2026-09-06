# Validation of 0.1.0-alpha.1

## Reorientation update — 6 September 2026

The updated prerelease adds optional `clinging_reoriented:reorientation` support
for leather boots. A `requires_effect` guard keeps the rule inactive when the effect
is absent, including older Clinging Reoriented versions. Clinging's existing slot
is unchanged. No new mandatory dependency is introduced.

- Final standalone build: 29/29 required tests passed at 23:33:54 local time
  (28 project tests and Fabric's environment check).
- Full compatibility fixture including Clinging Reoriented alpha.4 and Gravity
  Changer 1.5.2-beta.5: 64/64 passed at 23:35:11 (28 project, 35 upstream, one runner).
- A separate real-server run with Clinging Reoriented alpha.3 passed at 23:36:29;
  the new effect is absent and its optional boots rule stays inactive.
- The new test actually brews Reorientation with a shulker shell, pours its normal,
  splash and lingering contents into a cauldron, rejects helmet/tunic/pants, infuses
  boots, checks the lifetime mode, and verifies equip/unequip effect ownership.
- The Clinging Reoriented project's client suite separately passed physical Shift,
  repeated Reorientation, failure/success sounds and First Person checks at 23:30:15.
  This does not replace human playtesting of the combined armor and gravity behavior.

The earlier test counts and fixtures below describe the original alpha build.

Local validation on 6 September 2026, Windows, Java 25.0.3, Minecraft 26.2, Fabric Loader 0.19.5, Fabric API 0.159.0+26.2, Loom 1.17.20 and Gradle 9.5.1. This is an alpha with automated coverage and a reviewed client capture, not a claim of completed human playtesting of the full modpack.

## Automated coverage

| Run | Result |
|---|---|
| Final standalone `build --offline` | PASS, 28/28 required server tests |
| Full fixture, installed Scale Brews beta.4 | PASS, 63/63 required server tests |
| Full fixture, requested Scale Brews beta.3 | PASS, 63/63 required server tests |
| Standalone client GameTest | PASS, synchronization/equip/unequip assertions and two reviewed captures |

The server totals include Fabric's environment check; fixture totals also include tests from other mods. They are not 63 distinct Alchemical Leather tests. `build` runs the server suite; the client task is invoked separately.

There are 27 project server GameTests: 6 effect-ownership tests, 11 core tests, 6 optional-compatibility tests and 4 persistence/behavior tests. Optional-provider branches run only when their providers are present. Passing without providers does not establish optional compatibility. With `-PcompatMods`, the suite first requires the audited providers to be loaded.

Coverage includes:

- Separate external and equipped effect clocks, stronger/weaker/equal sources, adding an external effect during an infusion, milk and hidden vanilla chains.
- Complete PotionContents cardinality, real amplifier/duration, missing effect identifiers and component serialization.
- Three doses, exact bottle extraction, rejecting mixed contents/types, slot/enchantment failures without consumption, replacement color and one-level washing.
- Pausing, resuming, expiry, stable effects, one-shot instant effects and enchanting/repair guards.
- Real player save/load with equipment time and external effects; block-entity save/load of custom potion contents.
- Regeneration pulse comparison against vanilla; generic consumption of the real instantaneous Saturation effect, with a test-only slot rule.
- Loaded optional potion families and slot resolution; execution of Scale Brews brewing through Growth III and Shrinking III, conversion to splash/lingering, and uncapped stable level III equipment effects.
- BedrockIfy canonical levels and fractional-dose rejection; decoding Deeper Dark's real Blindness loot item; smithing trim preservation.

Client automation creates an isolated integrated world, verifies block-entity contents/color synchronization, captures the ordinary cauldron body with colored liquid, equips infused vanilla leather, checks the synchronized effect, opens the inventory for a capture, and checks removal after unequipping. Both screenshots were visually inspected. This check uses commands for setup and does not replace real mouse interaction or full-modpack visual QA.

## Compatibility fixtures

The isolated fixture contains BedrockIfy 1.11.8, Alex's Mobs Continued 2.1.9, Friends&Foes 4.0.27, Wilder Wild 4.2.11, Deeper Dark 4.4.1, Additional Additions 10.0.12, Enchancement 26.2-r4, Functional Armor Trims 2.2.1 and Grind Enchantments 4.2.1+26.1.2, plus their dependencies. Scale Brews beta.4 matches the installed profile; beta.3 is checked in a separate fixture. No fixture binaries are bundled.

The test world's Enchancement configuration is a copy of the installed profile, including `overhaulEnchanting: DISABLED`, `disableDurability: NONE` and the disabled rebalance options. The user's profile was read, not modified. An earlier fixture with Enchancement defaults failed Additional Additions' own wrench durability test because those defaults disable durability globally; Alchemical Leather's tests passed in that run. The installed configuration resolves that external-test conflict. This does not establish compatibility of every Enchancement overhaul configuration.

The full fixture runs 63 required server tests: the 27 project tests, 35 tests supplied by other mods, and Fabric's internal environment check. The standalone suite runs 28 including Fabric's check. Provider-family inspection distinguishes registered potions from recipes: Scale Brews recipes were executed, while the Deeper Dark loot item was decoded and the other families were enumerated and resolved. Not every optional brewing recipe was crafted.

## Remaining human/runtime checks

- Real right-click/sneak behavior in both hands and under latency or third-party claim protection.
- Full installed resource-pack rendering, translated tooltips, Functional Armor Trims visuals and functional combinations.
- Growth/Shrinking camera, collision and health transitions; Night Vision transitions; Clinging surfaces and death-triggered effects.
- Actual ItemSwapper and dispenser interactions, death/respawn, dimension travel and disconnect/reconnect. Serialization tests cover their storage foundation, not every live lifecycle path.
- Enchancement's alternate overhaul menu and late confirmation changes.
- Profiling many simultaneous wearers and equipment synchronization traffic. Source inspection confirms no global entity or full-inventory scans; no quantitative load benchmark is claimed.

The repository and alpha prerelease are published on GitHub. Subsequent pushes run
the provided GitHub Actions workflow; consult the run attached to the current commit
for its outcome. The original delivery's local-only status is historical.

## Reproduction

Run `./gradlew build` with Java 25 for the standalone build/tests. Run `./gradlew runClientGameTest` in a graphical environment (CI uses Xvfb). For each optional fixture, run `./gradlew runGameTest -PcompatMods=/absolute/path/to/mods`, placing the intended Enchancement configuration in `build/run/gameTest/config/enchancement.json` first. Run directories persist to retain that configuration; use separate checkouts/run directories when testing different configurations concurrently.

Logs are written under `build/run/gameTest/logs` and `build/run/clientGameTest/logs`; client captures under `build/run/clientGameTest/screenshots`. The production JAR must exclude the gametest source set, its Saturation rule, optional binaries and decompiled audit material.
