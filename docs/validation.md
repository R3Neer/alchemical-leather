# Validation of 0.1.0-alpha.2

Validation date: **10 September 2026**. Target: Minecraft 26.2, Java 25, Fabric Loader 0.19.5, Fabric API 0.159.0+26.2 and Loom 1.17.20.

This is alpha validation: automated coverage is broad and the client path is exercised in an integrated world, but it is not a claim that every third-party mod combination or physical gameplay path has been manually tested.

## Release-candidate CI evidence

The feature-freeze implementation was tested through the pull-request merge result against the then-current `main` branch.

| Run | Result |
|---|---|
| Standalone build + server GameTests | **PASS — 32/32 required GameTests** |
| Client GameTest under Xvfb / llvmpipe | **PASS** |
| Real Clinging Reoriented + BedrockIfy fixture | **PASS — 32/32 required GameTests** |

The final implementation round also added a thread-safety correction for dynamic cauldron tinting. The same three CI stages passed again after that change, including client assertions for Fabric block-entity render-data snapshots.

The compatibility fixture uses the real **Clinging Reoriented 0.1.0-alpha.6** release together with **BedrockIfy 1.11.8+mc26.2**, **Alex's Mobs Continued 2.1.9** and the Gravity Changer / CodxLib / Cloth Config dependencies required by Clinging. Third-party JARs are resolved/downloaded for CI and are not bundled in Alchemical Leather.

## What alpha.2 adds to automated coverage

### Dyeable-armor classification

Tests cover:

- all six vanilla standard dyeable armor cases: four humanoid leather pieces, leather horse armor and wolf armor;
- direct-item, item-list and tag-target `minecraft:crafting_dye` recipes;
- `#minecraft:cauldron_can_remove_dye` as a dyeability signal;
- `#alchemical_leather:dyeable_armor` as the custom fallback;
- rejection of a transmuting dye-recipe input when only the different result item is recolored;
- rejection of a self-dye recipe disabled by `fabric:false`;
- rejection of a codec-invalid `crafting_dye` resource that Minecraft itself omits.

The last two fixtures intentionally exercise failure paths. The invalid-recipe fixture therefore causes Minecraft to log the expected recipe parse error during GameTest startup; the test verifies that this local resource failure does not become phantom dyeability or break the test world.

### Humanoid and BODY policy

Tests prove that generalized humanoid armor keeps the original effect-to-slot restrictions and rejects multi-effect potions. BODY armor is separately tested with horse and wolf armor, including otherwise unmapped effects.

BODY coverage includes:

- one-potion replacement semantics;
- Turtle Master / multi-effect bundles;
- repeated entries of the same effect with independent clocks;
- immediate fallback to a surviving weaker repeated source;
- normal, splash and lingering timing modes;
- mixed instant + timed effects, with instant entries consumed before firing;
- equip/unequip pause and resume;
- conflicts with external vanilla effects;
- serialization of the animal-infusion component;
- enchanted-target rejection without consuming a dose.

### Lifecycle and persistence

The current-main pre-login regression test constructs a `ServerPlayer` whose `connection` is still null. It verifies that equipment synchronization cannot project an armor effect through that missing network channel. The production fix defers the complete synchronization and reconciles it at `ServerPlayConnectionEvents.JOIN`.

Persistence tests also cover separation of external and armor-owned effects across player save/load, custom potion-cauldron contents and dyed-water RGB block-entity data.

### Native dyed water

Tests cover the six-unit fluid model, color mixing, no-op dye application, blending with existing item color, preservation of infusion components, bottle and bucket extraction, water-potion tint removal, water-bucket delegation and insufficient-volume atomic failures.

The client suite creates real custom cauldrons in an integrated world and verifies synchronized block-entity color. It additionally checks that both cauldron types expose the expected immutable Fabric render-data RGB used by the multithread-safe tint path.

### BedrockIfy ownership

With real BedrockIfy loaded, tests verify:

- active BedrockIfy owns vanilla water + dye without Alchemical Leather side effects;
- disabling the BedrockIfy cauldron setting returns that entry point to Alchemical Leather;
- BedrockIfy colored water blends compatible armor color and loses exactly one fluid unit;
- Alchemical Leather armor can consume a canonical BedrockIfy potion dose without replacing the foreign potion-cauldron block prematurely;
- ordinary potion-bottle interactions on a BedrockIfy potion cauldron are left entirely to BedrockIfy;
- fractional/noncanonical imported potion levels fail without mutation.

### Real Clinging Reoriented integration

The alpha.6 fixture verifies the actual Clinging and Reorientation effects, not synthetic identifiers. Compatible leather horse armor and wolf armor can both receive and project these effects under the BODY policy. The existing humanoid boots mapping remains separately covered.

## Historical alpha.1 compatibility evidence

Alpha.1 validation previously exercised Scale Brews beta.3/beta.4, Friends&Foes 4.0.27, Wilder Wild 4.2.11, Deeper Dark 4.4.1, Additional Additions 10.0.12, Enchancement 26.2-r4, Functional Armor Trims 2.2.1 and Grind Enchantments 4.2.1+26.1.2 in larger local compatibility fixtures. Those results remain useful historical evidence, but they are **not** presented as if every provider were rerun in the smaller alpha.2 CI fixture.

The earlier Scale Brews tests executed real Growth III / Shrinking III brewing and splash/lingering conversion. Deeper Dark coverage decoded its real Blindness loot component. Other optional families were enumerated and resolved. See the alpha.1 prerelease history for the original run context.

## Remaining human/runtime checks

- Physical right-click / crouch behavior in both hands, especially under latency or third-party claim protection.
- Real gameplay feel of animal armor infusion on mounted/tamed entities rather than test-controlled entities.
- Full installed resource-pack rendering, translated tooltips and combinations with armor-trim visual mods.
- Growth/Shrinking camera/collision/health transitions, Night Vision transitions, gravity-surface behavior and death-triggered effects in a real modpack session.
- Dispenser, ItemSwapper, death/respawn, dimension travel and disconnect/reconnect combinations beyond the covered storage/lifecycle foundations.
- Alternate Enchancement overhaul configurations and other mods that replace crafting, enchanting or cauldron interaction semantics.
- Quantitative profiling with many simultaneous wearers. Source review confirms event-driven equipment synchronization and no global inventory/entity scan, but no throughput benchmark is claimed.

## Reproduction

With Java 25:

```bash
./gradlew build
./gradlew runClientGameTest
```

The GitHub Actions workflow runs both and also prepares the pinned Clinging Reoriented compatibility fixture before invoking `runGameTest` with the fixture directory.

Logs are written under `build/run/*/logs`; client captures are written under `build/run/clientGameTest/screenshots`. The production JAR excludes the gametest source set, its synthetic recipes/tags and all third-party fixture binaries.
