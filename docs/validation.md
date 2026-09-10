# Validation of 0.1.0-alpha.3

Validation date: **11 September 2026**. Target: Minecraft 26.2, Java 25, Fabric Loader 0.19.5, Fabric API 0.159.0+26.2 and Loom 1.17.20.

This is alpha validation: automated coverage is broad and the client path is exercised in an integrated world, but it is not a claim that every third-party mod combination, datapack extension or physical gameplay path has been manually tested.

## Release-candidate CI evidence

The alpha.3 feature candidate is exercised by the normal GitHub Actions pipeline. Candidate run `34540349307` completed successfully after the final implementation correction.

| Run | Result |
|---|---|
| Standalone build + server GameTests | **PASS — 41/41 required GameTests** |
| Client GameTest under Xvfb / llvmpipe | **PASS** |
| Real Clinging Reoriented + Scale Brews + BedrockIfy + Alex's Mobs fixture | **PASS — 41/41 required GameTests** |

The compatibility fixture uses the real **Clinging Reoriented 0.1.0-alpha.6**, **Scale Brews 0.1.0-beta.5**, **BedrockIfy 1.11.8+mc26.2** and **Alex's Mobs Continued 2.1.9** releases together with Gravity Changer, CodxLib and Cloth Config dependencies required by Clinging. Third-party JARs are resolved/downloaded for CI and are not bundled in Alchemical Leather.

## Leatherworker economy coverage added in alpha.3

Nine new server GameTests cover the villager-trade feature and its policy boundaries:

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

The client suite creates real custom cauldrons in an integrated world, verifies synchronized block-entity color and checks the immutable Fabric render-data snapshots used by the multithread-safe tint path.

### Native dyed water and BedrockIfy ownership

Tests cover the six-unit fluid model, color mixing, no-op dye application, blending with existing item color, infusion preservation, bottle/bucket extraction, water-potion tint removal, bucket delegation and atomic failures.

With real BedrockIfy loaded, tests also verify vanilla-water+dye ownership, disabled-setting fallback, colored-water armor recoloring, canonical potion dose consumption, ordinary bottle handoff and rejection of fractional/noncanonical imported potion levels.

### Real Clinging Reoriented integration

The real fixture still verifies manual Clinging and Reorientation infusion into leather horse armor and wolf armor plus the humanoid boots mapping. Alpha.3 additionally verifies the intentional economy distinction: Clinging can participate in Master trades; Reorientation cannot.

## Historical alpha.1 compatibility evidence

Alpha.1 validation previously exercised Friends&Foes 4.0.27, Wilder Wild 4.2.11, Deeper Dark 4.4.1, Additional Additions 10.0.12, Enchancement 26.2-r4, Functional Armor Trims 2.2.1 and Grind Enchantments 4.2.1+26.1.2 in larger local compatibility fixtures. Those results remain useful historical evidence, but are not presented as if every provider were rerun in alpha.3.

## Remaining human/runtime checks

- Physical right-click / crouch behavior in both hands, especially under latency or third-party claim protection.
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

The GitHub Actions workflow runs both and also prepares the pinned real compatibility fixture before invoking `runGameTest` with Clinging Reoriented, Scale Brews, BedrockIfy, Alex's Mobs and their required dependencies.

Logs are written under `build/run/*/logs`; client captures are written under `build/run/clientGameTest/screenshots`. The production JAR excludes the gametest source set, synthetic test resources and all third-party fixture binaries.
