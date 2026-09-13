# Validation of 0.1.0-alpha.5

Release validation date: **13 September 2026**. Target: Minecraft 26.2, Java 25, Fabric Loader 0.19.5, Fabric API 0.159.0+26.2 and Loom 1.17.20.

This is alpha validation: automated coverage is broad and the client path is exercised in an integrated world, but it is not a claim that every third-party mod combination, datapack extension or physical gameplay path has been manually tested.

## Alpha.5 release scope

Alpha.5 packages the effectless-potion storage and dye-bath correction on top of the alpha.4 cauldron/rendering fixes:

- valid non-water potion contents are stored independently of whether they can create an infusion;
- Awkward, Mundane and Thick potions round-trip through Alchemical Leather cauldrons with normal, splash and lingering bottle types;
- effectless potion contents act as dye-only baths for compatible dyeable armor, consuming one dose without creating or replacing an infusion;
- dye-only application preserves enchantments, existing humanoid/BODY infusions and unrelated item components, and BODY armor never receives an empty `AnimalInfusion`;
- tinted effectless cauldrons accept matching refills while preserving their decorative tint;
- water potions remain delegated to Minecraft's water-cauldron path, malformed contents and non-dyeable targets fail atomically, and effectful target eligibility is checked only when armor is actually applied;
- BedrockIfy retains ownership of its potion block/fluid lifecycle while canonical imported effectless doses use the same dye-only armor semantics;
- alpha.4's ordinary-use splash/lingering pouring, live tint remeshing and GameTest-entrypoint consistency guard remain part of the release.

## Alpha.5 release-candidate CI evidence

The functional change merged through PR `#7` as main commit `ef89645fd394911eafb6553f82debf1c1e677190`; post-merge run `34750022598` completed successfully. The alpha.5 release-preparation head `71e00b9eed616978fd440bd1d703987449c376e4` then passed both push run `34750487121` and independent pull-request run `34750496727`. The final tag is intentionally withheld until a documentation-complete commit on `main` passes this same pipeline; that exact target and run are recorded below before publication.

| Alpha.5 release matrix | Result |
|---|---|
| Standalone build + server GameTests | **PASS — 79/79 required GameTests** |
| GameTest registration consistency check | **PASS** |
| Client GameTest under Xvfb / llvmpipe | **PASS** |
| Real Clinging Reoriented + Scale Brews + BedrockIfy + Alex's Mobs fixture | **PASS — 79/79 required GameTests** |

The compatibility fixture uses the real **Clinging Reoriented 0.1.0-alpha.6**, **Scale Brews 0.1.0-beta.5**, **BedrockIfy 1.11.8+mc26.2** and **Alex's Mobs Continued 2.1.9** releases together with Gravity Changer, CodxLib and Cloth Config dependencies required by Clinging. Third-party JARs are resolved/downloaded for CI and are not bundled in Alchemical Leather.

## Alpha.5 effectless-potion development evidence

A post-release gameplay report exposed a conceptual coupling in `CauldronService`: bottle insertion called `Infusions.resolveAll(...)` before storage, so valid potion contents with zero effects were rejected as `no_effect`. That resolution function was correct for creating an infusion, but storage had accidentally inherited the stricter infusion requirement.

The correction separates those responsibilities. Valid non-water potion contents can be stored independently of whether they are infusible. When compatible armor is later used on the cauldron, effectful contents follow the existing infusion rules; effectless contents take the dye-only path and update only `DYED_COLOR`, preserving enchantments and any existing humanoid/BODY infusion.

The tests-first baseline run `34749420765` compiled successfully and ran **76** server GameTests, with exactly five new required regressions failing for the intended old behavior: effectless vanilla potion pouring, enchanted dye-only armor, preservation of an existing humanoid infusion, BODY dye-only behavior and tint-preserving effectless refill. Water delegation and malformed-potion atomic rejection already passed on that baseline.

After implementation and adversarial expansion, run `34749615391` on commit `43c79c047d768cdddc1f964eae6ef2d82f12caf8` completed successfully:

| Effectless-potion regression matrix | Result |
|---|---|
| Standalone build + server GameTests | **PASS — 79/79 required GameTests** |
| GameTest registration consistency check | **PASS** |
| Client GameTest under Xvfb / llvmpipe | **PASS** |
| Real Clinging Reoriented + Scale Brews + BedrockIfy + Alex's Mobs fixture | **PASS — 79/79 required GameTests** |

The ten additional server regressions cover:

- Awkward, Mundane and Thick potion contents across normal, splash and lingering bottle storage plus glass-bottle round trips;
- effectless potion color transfer to compatible humanoid armor without creating an infusion;
- recoloring already-enchanted compatible armor while preserving its enchantments and unrelated components;
- recoloring already-infused humanoid armor without replacing its infusion;
- BODY armor dye-only behavior without constructing an empty `AnimalInfusion`, including preservation of an existing BODY infusion;
- tinting an effectless potion cauldron and refilling it with matching untinted contents while preserving the decorative tint;
- water-potion delegation to Minecraft's water-cauldron path;
- atomic rejection of malformed potion items with no `POTION_CONTENTS`;
- storage of valid effectful potion contents before a particular armor target's infusion eligibility is checked, while failed armor application still consumes no dose;
- atomic rejection of non-dyeable armor from the dye-only path;
- imported canonical BedrockIfy effectless potion contents acting as a dye bath while preserving BedrockIfy's block ownership and consuming exactly one foreign dose.

This intentionally leaves `Infusions.resolve` and `resolveAll` strict: zero effects are still invalid **as an infusion**. No empty infusion representation was added to either humanoid or BODY data.

## Historical alpha.4 release scope

Alpha.4 consolidates the post-alpha.3 cauldron fixes and the corrected test harness:

- normal, splash and lingering potions all pour into Alchemical Leather cauldrons with ordinary Use;
- potion cauldrons can be tinted with standard dye items without changing potion identity, effects, custom name, bottle type or dose count;
- matching potion refills remain compatible after tinting and preserve the existing tint;
- live potion/dyed-water RGB changes now remesh the already-rendered client section immediately instead of waiting for a later chunk rebuild;
- every current server `@GameTest` class is registered, and `verifyGameTestEntrypoints` prevents silent descriptor drift from recurring;
- BedrockIfy's intentional crafting-dye revocation while its cauldron feature is active is treated as foreign ownership policy rather than patched around.

## Historical alpha.4 release-candidate CI evidence

The alpha.4 release-preparation PR merged as commit `dd3d26318b1810e59d261923bbcf1c661a718b3d` on `main`. Main run `34723466522` completed successfully after release-prep push run `34723206044` and independent pull-request run `34723320180` had already passed the same pipeline.

| Alpha.4 release matrix | Result |
|---|---|
| Standalone build + server GameTests | **PASS — 69/69 required GameTests** |
| GameTest registration consistency check | **PASS** |
| Client GameTest under Xvfb / llvmpipe | **PASS** |
| Real Clinging Reoriented + Scale Brews + BedrockIfy + Alex's Mobs fixture | **PASS — 69/69 required GameTests** |

The compatibility fixture uses the real **Clinging Reoriented 0.1.0-alpha.6**, **Scale Brews 0.1.0-beta.5**, **BedrockIfy 1.11.8+mc26.2** and **Alex's Mobs Continued 2.1.9** releases together with Gravity Changer, CodxLib and Cloth Config dependencies required by Clinging. Third-party JARs are resolved/downloaded for CI and are not bundled in Alchemical Leather.

## Historical alpha.4 client rendering regression coverage

The client suite starts with visible potion and dyed-water cauldrons after their terrain section has already been meshed. It then changes both RGB values twice while the world remains loaded and verifies the synchronized block-entity state and Fabric render-data snapshots after each update.

CI screenshots for the final implementation were manually reviewed: the visible liquids change from their initial orange/brown appearance to green/red and then purple/blue without reloading the world. This specifically covers the live-render failure that ordinary state assertions missed.

## Historical alpha.3 release evidence and correction

The final alpha.3 release candidate was merge commit `224aadc7c2cf06198e4afac496d2536711da4518` on `main`. Main run `34541183384` completed successfully after the pull-request candidate and its independent PR run had already passed the same pipeline.

A 12 September audit found that the original alpha.3 validation document had incorrectly reported **41/41** server GameTests for those runs. The original `alchemical_leather-test` descriptor registered only five older server test classes, so newer GameTest classes compiled but were silently not discovered. The actual alpha.3 run logs show **32/32** required server GameTests in both server invocations.

| Alpha.3 release run | Result actually executed |
|---|---|
| Standalone build + server GameTests | **PASS — 32/32 required GameTests** |
| Client GameTest under Xvfb / llvmpipe | **PASS** |
| Real Clinging Reoriented + Scale Brews + BedrockIfy + Alex's Mobs fixture | **PASS — 32/32 required GameTests** |

The earlier implementation-candidate run `34540349307` and pull-request run `34540989217` used the same incomplete GameTest registration. They remain useful build/client evidence, but they must not be cited as proof that the later unregistered server suites ran.

## Post-alpha.3 GameTest registration audit — 12 September 2026

PR `#4` repaired the test harness by registering every current server GameTest class and adding `verifyGameTestEntrypoints` to Gradle. `check` now fails whenever a Java class containing `@GameTest` is absent from the `fabric-gametest` entrypoint list or the descriptor contains a stale server GameTest entry.

Audit run `34693184518` on commit `fa3519896b28c84c693f63a6c8b395f8deb1d7ea` completed successfully with the corrected registration:

| Corrected audit run | Result |
|---|---|
| Standalone build + server GameTests | **PASS — 69/69 required GameTests** |
| GameTest registration consistency check | **PASS** |
| Client GameTest under Xvfb / llvmpipe | **PASS** |
| Real Clinging Reoriented + Scale Brews + BedrockIfy + Alex's Mobs fixture | **PASS — 69/69 required GameTests** |

The first full-fixture audit exposed one previously hidden expectation mismatch: BedrockIfy deliberately forces every `DyeRecipe.matches(...)` result to false while its `bedrockCauldron` feature is active, replacing crafting-table armor dyeing with its cauldron path. The Alchemical Leather test now checks that intentional revocation under active BedrockIfy while the dedicated BedrockIfy colored-water tests continue to verify recoloring and infusion preservation. No production compatibility workaround was added for behavior BedrockIfy intentionally owns.

The corrected suite also means the potion-cauldron interaction regressions added after alpha.3 are genuinely executed by CI: splash/lingering normal-use pouring, potion-cauldron tinting, no-op dye handling, tint-preserving matching refills and BedrockIfy ownership boundaries.

## Leatherworker economy coverage retained

Nine server GameTests cover the villager-trade feature and its policy boundaries:

- the actual `VILLAGER_TRADE` registry contains the three Alchemical Leather trade resources;
- the vanilla Leatherworker level-IV and level-V tags retain their vanilla entries and gain exactly the intended Alchemical categories;
- levels I–III remain free of Alchemical Leather trades;
- the existing vanilla TradeSets still select two offers rather than being replaced or widened;
- Expert economic armor is restricted to leggings/boots, while Master includes helmet, chestplate, leggings, boots, leather horse armor and wolf armor;
- actual generated Expert offers are timed level I, cost within the intended range and have three uses;
- actual generated Master timed offers remain at level II or lower and have two uses;
- actual persistent offers are stable level I, retain Dragon's Breath as the second cost and have one use;
- transient `ADDITIONAL_TRADE_COST` pricing data is removed from the sold item by vanilla trade generation;
- deterministic policy checks reject Expert head/chest/BODY, wrong humanoid effect slots, Turtle Master, level III and persistent level II;
- the default persistent BODY pool excludes Invisibility, Turtle Master, Wind Charged, Oozing, Infested and Weaving;
- with the real Scale Brews fixture, Growth/Shrinking II may be timed Master trades, level III is rejected, and persistent Scale Brews is capped at level I on humanoid and BODY armor;
- with the real Alex's Mobs / Clinging Reoriented fixture, Clinging is permitted at Master while Reorientation is hard-rejected at every villager tier including BODY;
- pre-enchanted candidate stacks are rejected before infusion, preserving the enchantment/infusion mutual exclusion.

The tests invoke real `VillagerTrade.getOffer` paths with Minecraft's villager-trade loot context instead of validating only helper methods, so malformed registry/tag/resource wiring is visible to CI.

## Existing alpha.2 coverage retained

### Dyeable-armor classification

Tests cover all six vanilla standard dyeable armor cases, direct/list/tag-target self-dye recipes, the vanilla cauldron-removal signal, the Alchemical Leather fallback tag, transmuting false-positive rejection, `fabric:false` resources and codec-invalid dye resources.

The invalid-recipe fixture intentionally causes Minecraft to log the expected recipe parse error during GameTest startup; the test verifies that this local resource failure does not become phantom dyeability or break the test world.

### Humanoid and BODY policy

Generalized humanoid armor keeps effect-to-slot restrictions and rejects multi-effect potions. BODY armor is separately tested with horse and wolf armor, including one-potion replacement semantics, Turtle Master/multi-effect bundles, repeated same-effect clocks, normal/splash/lingering modes, instant+timed siblings, pause/resume, external-effect conflicts, serialization and enchanted-target atomicity.

### Lifecycle, persistence and rendering

The pre-login regression constructs a `ServerPlayer` whose `connection` is still null and verifies that equipment synchronization cannot project through that missing network channel. Persistence tests cover separation of external and armor-owned effects plus custom cauldron and dyed-water block-entity data.

The client suite creates real custom cauldrons in an integrated world, verifies synchronized block-entity color, checks the immutable Fabric render-data snapshots used by the multithread-safe tint path, and covers repeated live remeshing after synchronized RGB changes.

### Native dyed water and BedrockIfy ownership

Tests cover the six-unit fluid model, color mixing, no-op dye application, blending with existing item color, infusion preservation, bottle/bucket extraction, water-potion tint removal, bucket delegation and atomic failures.

With real BedrockIfy loaded, tests also verify vanilla-water+dye ownership, disabled-setting fallback, colored-water armor recoloring, canonical potion dose consumption, ordinary bottle handoff, dye delegation, effectless imported dye-only ownership, crafting-dye revocation while BedrockIfy's cauldron feature is active and rejection of fractional/noncanonical imported potion levels.

### Real Clinging Reoriented integration

The real fixture verifies manual Clinging and Reorientation infusion into leather horse armor and wolf armor plus the humanoid boots mapping. The economy tests additionally verify the intentional distinction: Clinging can participate in Master trades; Reorientation cannot.

## Historical alpha.1 compatibility evidence

Alpha.1 validation previously exercised Friends&Foes 4.0.27, Wilder Wild 4.2.11, Deeper Dark 4.4.1, Additional Additions 10.0.12, Enchancement 26.2-r4, Functional Armor Trims 2.2.1 and Grind Enchantments 4.2.1+26.1.2 in larger local compatibility fixtures. Those results remain useful historical evidence, but are not presented as if every provider were rerun in alpha.5.

## Remaining human/runtime checks

- Physical ordinary right-click / Use behavior in both hands for normal, splash and lingering potion pouring, including effectless dye baths, especially under latency or third-party claim protection.
- Real villager UI/restock behavior across repeated day cycles, reputation changes, demand changes, curing discounts and third-party villager-economy mods.
- Gameplay feel and economy of the 2/3 Expert and default Master selection probabilities across naturally generated villagers rather than test-controlled registries.
- Real gameplay feel of animal armor infusion on mounted/tamed entities rather than test-controlled entities.
- Full installed resource-pack rendering, translated tooltips and combinations with armor-trim visual mods.
- Growth/Shrinking camera/collision/health transitions, Night Vision transitions, gravity-surface behavior and death-triggered effects in a real modpack session.
- Dispenser, ItemSwapper, death/respawn, dimension travel and disconnect/reconnect combinations beyond the covered storage/lifecycle foundations.
- Alternate Enchancement overhaul configurations and other mods that replace crafting, enchanting, cauldron or villager-trade semantics.
- Quantitative profiling with many simultaneous wearers. Source review confirms event-driven equipment synchronization and no global inventory/entity scan, but no throughput benchmark is claimed.

## Reproduction

With Java 25:

```bash
./gradlew build
./gradlew runClientGameTest
```

The GitHub Actions workflow runs both and also prepares the pinned real compatibility fixture before invoking `runGameTest` with Clinging Reoriented, Scale Brews, BedrockIfy, Alex's Mobs and their required dependencies. `build` also runs `verifyGameTestEntrypoints`, preventing server GameTest registration drift from silently reducing CI coverage again.

Logs are written under `build/run/*/logs`; client captures are written under `build/run/clientGameTest/screenshots`. The production JAR excludes the gametest source set, synthetic test resources and all third-party fixture binaries.
