# Validation

This document records the current **post-0.1.0-beta.1 development validation** for causal infusion wear, humanoid multi-effect potions and the optional compatibility protocol. It does not assign a new release version or tag.

The frozen design is in [`TM_INFUSION_WEAR_SPEC.md`](TM_INFUSION_WEAR_SPEC.md). The execution/closeout record is in [`TM_INFUSION_WEAR_CLOSEOUT.md`](TM_INFUSION_WEAR_CLOSEOUT.md).

## Scope under validation

The current branch adds or hardens:

- causal durability wear tied to attributable effect work rather than effect presence;
- per-item/per-effect fractional work accumulation and ordinary break semantics;
- `infusionWear` configuration;
- strict data-driven wear-rule parsing;
- a small public semantic-event API for optional mods;
- builtin causal detectors for movement, jump/fall, combat, healing/damage/protection, breath, reach and selected effect procs;
- slot-aware armor-source ownership and external-effect eclipse;
- same-slot multi-effect humanoid infusions, including Turtle Master-style bundles;
- first-party compatibility ownership migration to Clinging: Reoriented and Scale Brews;
- registry-driven VanillaPlus potion/effect coverage.

The existing beta.1 features remain part of the regression matrix: cauldron infusion, crafting infusion, BODY bundles, tipped arrows, dyed water, effectless dye baths, BedrockIfy ownership, trades, persistence and client cauldron rendering.

## TM phases

| Phase | Result |
|---|---|
| S00 — specification / ownership migration | **Complete**. Frozen design retained; first-party ownership moved to companion mods; exact VanillaPlus potion contributors identified. |
| S01 — wear data model / API | **Complete**. Wear resources, public semantic API, slot-aware source validation, fractional persistence and real durability path implemented. |
| S02 — builtin causal detectors | **Complete and adversarially expanded**. Movement, jump/fall, protection/damage, combat, breath/reach and selected mod-effect proc boundaries are covered. |
| S03 — companion adapters | **Complete**. Scale Brews and Clinging integrations converged independently and are merged to their `main` branches. |
| S04 — humanoid multi-effect | **Complete**. Same-slot distinct-effect bundles are atomic and keep independent timing/wear attribution. |
| S05 — exhaustive compatibility / adversarial gate | **Functionally green** on branch candidate `52cae148e287292b240d3ee5c57d4b45b1851c07`; final evidence-only head and post-merge main gate remain. |

## First-party companion evidence

### Scale Brews

Compatibility ownership merged to Scale Brews `main` as:

`74066349eb33872d1f2b8584dfd05ffd96eae0f8`

Growth/Shrinking own their chestplate slot declarations and explicit `wear: none` policy. Scale introduces no hard Alchemical Leather dependency. The exact merged `main` commit passed build workflow **35029211878**.

### Clinging: Reoriented

The original compatibility prototype had fallen more than a hundred commits behind current Clinging. It was deliberately replaced by a fresh current-main port rather than merged blindly.

The final integration merged to Clinging `main` as:

`df1cff3a2fb9baf69d3bb8594159681b6966096d`

The documentation-complete PR head `8fbb3fcfd5ff97185c8018812923a1c897f35db6` passed complete workflow **35031688007**, covering localization, build/unit tests, server GameTests, default client, First Person, Scale Brews server/client, Fresh Animations and semantic snapshots. The exact merged `main` commit then passed post-merge workflow **35032664835**.

### Production defect found by the adversary

The first current-main Clinging port classified continuous Reorientation work too broadly. Reorientation could still appear physics-owned while **Elytra**, a **non-empty fluid context** or **independent player flight** actually owned locomotion, allowing armor to be charged for work Reorientation did not perform.

The production predicate was corrected before convergence. Reserved holdouts now exclude Elytra, fluids, player flight, passengers, moving/support surfaces, Anatomy support and invalid player lifecycle states while preserving successful airborne self-controlled Reorientation work and successful discrete mounted turns.

This was the material production defect discovered during the final companion adversarial campaign.

## VanillaPlus potion-contributor fixture

The S05 fixture intentionally loads the exact potion/effect contributors from `R3Neer/VanillaPlus-26.2`, rather than every cosmetic/client mod in the pack:

- Alex's Mobs Continued 2.1.9;
- Clinging: Reoriented at exact merged commit `df1cff3a2fb9baf69d3bb8594159681b6966096d`;
- Scale Brews at exact merged commit `74066349eb33872d1f2b8584dfd05ffd96eae0f8`;
- Friends & Foes 4.0.27+mc26.2;
- Wilder Wild 4.2.11-mc26.2;
- Deeper Dark 4.4.1;
- BedrockIfy 1.11.8+mc26.2;
- the exact Gravity Changer, CodxLib, Cloth Config, ResourcefulLib and FrozenLib versions required by those pack entries.

Third-party JARs are fetched from the exact Modrinth CDN URLs stored by the pack's packwiz metadata. First-party companions are shallow-cloned from `main` and the workflow **fails unless their HEAD equals the expected converged SHA**.

## Registry and policy coverage

`PotionCoverageTests` walks the actual loaded potion registry. Every effect used by a loaded potion must have an explicit wear classification; an absent rule is not silently treated as no-wear.

Additional sentinels pin expected policy for:

- Scale Brews Growth / Shrinking — CHEST, `wear: none`;
- Clinging: Reoriented Reorientation — FEET, causal wear;
- Alex's Mobs Clinging — FEET, causal wear;
- Friends & Foes Reach — CHEST, causal wear;
- Wilder Wild Reach Boost / Scorching — CHEST, causal wear;
- Deeper Dark's Blindness potion path — HEAD, explicit `wear: none`.

Existing compatibility tests also audit the expected registered potion-family counts and exercise real brewing/content paths rather than only hard-coded effect identifiers.

## Cross-mod semantic bridge holdout

`CompanionWearBridgeTests` drives the **real Clinging adapter** into the **real Alchemical Leather API and wear engine** without compiling either production mod against the other.

The test first proves the Alchemical Leather half directly: the loaded Reorientation gravity-turn event maps one semantic unit to exactly 2 work. It then proves the Clinging bridge has resolved the public API and attributes the turn to the same active Reorientation effect before invoking the real bridge repeatedly.

The survival fixture requires:

- one turn-equivalent → exactly **2 work**;
- fourteen turn-equivalents → **28 work** debt and zero durability damage;
- the fifteenth → one 30-work bucket, exactly **1 ordinary durability damage**, zero residual debt.

The synthetic GameTest player has no network connection, so the fixture establishes slot-aware FEET ledger state directly rather than weakening production's pre-login connection-safety fence. It explicitly forces Survival, clears `instabuild` and asserts non-infinite-material semantics so Creative behavior cannot erase the debt being measured.

## S05 functional evidence

Branch head:

`52cae148e287292b240d3ee5c57d4b45b1851c07`

passed complete PR workflow **#684** / run **35034528304**.

| Gate | Result |
|---|---|
| Gradle build + standalone server GameTests | **PASS** |
| GameTest entrypoint consistency | **PASS** |
| Client GameTest under Xvfb / software GL | **PASS** |
| Exact pinned Clinging + Scale builds | **PASS** |
| Exact VanillaPlus potion-contributor fixture | **PASS** |
| Registry-driven potion/wear classification | **PASS** |
| Direct API + real Clinging bridge → owning boots | **PASS** |
| Full compatibility GameTest set | **PASS — 124/124 required tests** |
| CI artifact/log upload | **PASS** |

The workflow artifact explicitly records the expected companion commits and the compatibility log reports `All 124 required tests passed :)`.

## Failure classification during S05

The adversarial/fixture campaign deliberately kept red runs instead of rewriting history. They exposed the following distinct classes:

1. **Infrastructure — Modrinth Maven resolution.** The initial expanded fixture used Modrinth Maven coordinates and failed before GameTests. The fixture now downloads exact packwiz-pinned CDN artifacts instead.
2. **Test-fixture drift — Reorientation brewing ingredient.** An inherited test still used Shulker Shell, while current Clinging beta.3 uses `clinging_reoriented:gravity_charge`. The test was updated to the current recipe; production was unchanged.
3. **Test-fixture type — wrong mock player.** The first cross-mod reflection test used a mock that did not satisfy the real `ServerPlayer` method signature. The fixture was corrected.
4. **Test-fixture ownership — ownerless projection.** Manually projecting an armor effect without an equipment slot correctly caused the wear engine to refuse billing. The holdout was changed to establish explicit FEET ownership.
5. **Test-fixture lifecycle — pre-login ServerPlayer.** Attempting ordinary equipment reconciliation on a synthetic `ServerPlayer` with `connection == null` hit the intentional pre-login safety fence. The fixture now establishes slot-aware ledger state directly; the production fence remains intact.
6. **Fixture pinning — direct shallow SHA fetch.** Fetching an isolated merge SHA into a shallow repository proved brittle. The final workflow shallow-clones each companion's `main` and fails if its HEAD differs from the pinned converged SHA.
7. **Production — Clinging foreign-flight attribution.** The Elytra/fluid/player-flight bug described above was fixed in Clinging and converted into permanent holdouts.
8. **Test-fixture game mode — infinite materials.** `makeMockServerPlayerInLevel()` supplied infinite-material semantics, so the wear engine correctly discarded damage/debt. The holdout now forces Survival, sets `instabuild=false` and asserts `hasInfiniteMaterials()==false` before measuring wear.

No Alchemical Leather production behavior was weakened to make a fixture pass.

## Remaining final gate

This evidence update is intentionally documentation-only. Its exact head must pass the **same complete matrix** before PR #11 is merged. After merge, the exact `main` commit must pass the same workflow again. Those two gates are the remaining mechanical closeout; no new functional work is expected unless one of them finds a new defect.

## Manual QA boundary

Automated coverage is intentionally deep, but it is not a claim that every third-party mod combination or physical gameplay sequence has been manually played. Before a future public release, manual multiplayer/playability checks remain useful for long-duration durability feel, modpack interaction and whether the configured work thresholds are enjoyable rather than merely correct.

## Historical beta.1 baseline

The published 0.1.0-beta.1 release established the pre-wear baseline: native tipped arrows, crafting-table infusion, BODY potion bundles, dyed/effectless cauldron behavior, BedrockIfy ownership and the corrected GameTest registration guard. The causal-wear work preserves those systems and extends their regression matrix; it does not retroactively change what was published as beta.1.
