# Validation — 0.1.0-beta.2

This document records the validation evidence for **Alchemical Leather 0.1.0-beta.2** on Minecraft 26.2 Fabric. The beta.2 candidate includes the causal infusion-wear system, humanoid same-slot multi-effect support, optional-mod wear protocol and the final post-merge causal audit performed before release preparation.

## Validated scope

Beta.2 adds or hardens:

- causal durability wear tied to attributable effect work rather than effect presence;
- per-item/per-effect fractional work accumulation and ordinary break semantics;
- `infusionWear` configuration;
- strict data-driven wear-rule parsing;
- a small public semantic-event API for optional mods;
- builtin causal detectors for movement, jump/fall, combat, healing/damage/protection, breath, reach, knockback and selected effect procs;
- slot-aware armor-source ownership and external-effect eclipse;
- same-slot multi-effect humanoid infusions, including Turtle Master-style bundles;
- first-party compatibility ownership in Clinging: Reoriented and Scale Brews;
- registry-driven VanillaPlus potion/effect coverage;
- a final consumer-site audit that verifies mechanics such as Knockback Resistance do not bypass the generic detector simply because vanilla or a supported mod reads the attribute directly.

The beta.1 systems remain in the regression matrix: cauldron infusion, crafting infusion, BODY bundles, tipped arrows, dyed water, effectless dye baths, BedrockIfy ownership, trades, persistence and client cauldron rendering.

## Implementation campaign summary

The causal-wear work was developed in five implementation phases plus an adversarial compatibility gate:

| Phase | Result |
|---|---|
| S00 — specification / ownership migration | **Complete.** First-party ownership moved to companion mods and the exact VanillaPlus potion contributors were identified. |
| S01 — wear data model / API | **Complete.** Wear resources, public semantic API, slot-aware source validation, fractional persistence and real durability path implemented. |
| S02 — builtin causal detectors | **Complete.** Movement, jump/fall, protection/damage, combat, breath/reach and selected mod-effect proc boundaries implemented and attacked. |
| S03 — companion adapters | **Complete.** Scale Brews and Clinging integrations converged independently and were merged to their own `main` branches. |
| S04 — humanoid multi-effect | **Complete.** Same-slot distinct-effect bundles are atomic and keep independent timing/wear attribution. |
| S05 — exhaustive compatibility / adversarial gate | **Complete.** Registry coverage, exact companion fixture, cross-mod semantic bridge and client/server matrix converged before the later beta.2 durability audit. |

The temporary design/closeout documents used to run those campaigns were removed during beta.2 release preparation after their durable conclusions were folded into this file, [architecture.md](architecture.md) and [COMPATIBILITY.md](COMPATIBILITY.md).

## First-party companion pins

### Scale Brews

Compatibility ownership is validated at:

`74066349eb33872d1f2b8584dfd05ffd96eae0f8`

Growth/Shrinking own their chestplate slot declarations and explicit `wear: none` policy. Scale introduces no hard Alchemical Leather dependency.

### Clinging: Reoriented

Compatibility ownership is validated at:

`df1cff3a2fb9baf69d3bb8594159681b6966096d`

Clinging owns Reorientation's FEET slot and publishes semantic successful-turn / controlled-flight facts through an optional linkage-safe bridge. Continuous Reorientation work excludes Elytra, fluids, independent player flight, passengers, moving/support surfaces, Anatomy support and invalid player lifecycle states while preserving genuine controlled airborne work and successful discrete turns.

## VanillaPlus potion-contributor fixture

The beta.2 fixture intentionally loads the exact potion/effect contributors from `R3Neer/VanillaPlus-26.2`, rather than every cosmetic/client mod in the pack:

- Alex's Mobs Continued 2.1.9;
- Clinging: Reoriented at `df1cff3a2fb9baf69d3bb8594159681b6966096d`;
- Scale Brews at `74066349eb33872d1f2b8584dfd05ffd96eae0f8`;
- Friends & Foes 4.0.27+mc26.2;
- Wilder Wild 4.2.11-mc26.2;
- Deeper Dark 4.4.1;
- BedrockIfy 1.11.8+mc26.2;
- Gravity Changer 1.5.2-beta.5-mc26.2;
- CodxLib 1.5.1, Cloth Config 26.2.155, ResourcefulLib 5.0.3 and FrozenLib 2.5.3-mc26.2.

Third-party JARs are fetched from the exact Modrinth CDN URLs stored by the pack's packwiz metadata. First-party companions are fetched by exact commit SHA and the workflow verifies that exact checkout before building them.

## Registry and policy coverage

`PotionCoverageTests` walks the actual loaded potion registry. Every effect used by a loaded potion must have an explicit wear classification; an absent rule is not silently treated as no-wear.

Additional sentinels pin expected policy for:

- Scale Brews Growth / Shrinking — CHEST, `wear: none`;
- Clinging: Reoriented Reorientation — FEET, causal wear;
- Alex's Mobs Clinging — FEET, causal wear;
- Friends & Foes Reach — CHEST, causal wear;
- Wilder Wild Reach Boost / Scorching — CHEST, causal wear;
- Deeper Dark's Blindness potion path — HEAD, explicit `wear: none`.

Existing compatibility tests also audit expected registered potion-family counts and exercise real brewing/content paths rather than only hard-coded effect identifiers.

## Cross-mod semantic bridge holdout

`CompanionWearBridgeTests` drives the **real Clinging adapter** into the **real Alchemical Leather API and wear engine** without compiling either production mod against the other.

The fixture proves that one Reorientation turn-equivalent maps to 2 work, fourteen turn-equivalents leave 28 work debt with zero durability damage, and the fifteenth crosses the 30-work threshold for exactly one ordinary durability point with zero residual debt.

The synthetic GameTest player has no network connection, so the fixture establishes slot-aware FEET ledger state directly rather than weakening production's pre-login connection-safety fence. Survival/non-infinite-material semantics are asserted before wear is measured.

## Final beta.2 causal audit

After the original S00–S05 implementation had already merged to `main`, a separate source-level adversarial pass audited the **actual Minecraft 26.2 and pack-version consumer sites** instead of trusting that a detector's existence implied complete attribution.

That audit produced permanent fixes and holdouts for several classes of missed or over-broad causal work:

1. **Damage cooldowns / i-frames.** Strength, Weakness, Fire Resistance and Jump Boost defensive wear now follow the damage that vanilla actually accepts rather than work that would have mattered only in a hypothetical uncooldowned hit.
2. **Weakness cure semantics.** A successful zombie-villager cure can bill the Weakness infusion at the authoritative conversion boundary, before vanilla removes the effect.
3. **Water Breathing.** Wear covers both prevented drowning progression and underwater air recovery when Water Breathing is the actual enabling cause.
4. **Reach.** The contrafactual reach calculation is exercised across entity/block interaction, item-use/raycast and brushing contexts, with baseline/alternate reach kept separate.
5. **Slow Falling.** Work is metered where gravity/fall state is actually consumed instead of merely because Slow Falling is present.
6. **Selected third-party procs.** Alex/Wilder effect adapters were rechecked for success-only attribution and duplicate billing.
7. **Knockback Resistance.** The audit found that many vanilla and Alex mechanics consume `KNOCKBACK_RESISTANCE` directly and do not pass through one universal knockback method. Those target-side routes now feed the same detector and data-owned balance.

### Knockback Resistance coverage

For vanilla 26.2, target-side coverage includes ordinary `LivingEntity#knockback`, Hoglin, Warden Sonic Boom, Mace, Iron Golem and Arrow knockback. A Sulfur Cube read is intentionally excluded because it reads the attacker's own resistance rather than the infused target's.

For Alex's Mobs Continued 2.1.9, target-side coverage includes Guster, Bison, Tusklin and Rhinoceros. Other audited reads belonging to the attacking mob are intentionally excluded from target-armor billing.

The final implementation mirrors the actual consumer arithmetic and emits only reduced impulse magnitude. The Minecraft attribute is sanitized to `[-2,1]`; Guster additionally clamps its multiplier to `[0,1]`. Route-specific Java code does not own durability prices: every path publishes to the existing `alchemical_leather:knockback_reduced` detector and the JSON rule remains the single balance source.

A real Alex Bison route is exercised in the compatibility fixture in addition to algebraic holdouts and vanilla Golem/Hoglin paths.

## Final pre-release functional evidence

The final production candidate before documentation/release preparation is:

`b58a370ffb82c334a984a607ed676720fc45014f`

GitHub Actions run **35087671151** completed successfully on that exact commit.

| Gate | Result |
|---|---|
| Gradle build + standalone server GameTests | **PASS — 140/140** |
| GameTest entrypoint consistency | **PASS** |
| Client GameTest under Xvfb / llvmpipe | **PASS** |
| Clinging exact-pin build/test suite | **PASS — 126/126 GameTests** |
| Scale Brews exact-pin build/test suite | **PASS — 154/154 GameTests** |
| Exact VanillaPlus potion-contributor fixture | **PASS — 140/140 integrated GameTests** |
| Registry-driven potion/wear classification | **PASS** |
| Direct API + real Clinging bridge → owning boots | **PASS** |
| Real Alex target-knockback linkage holdout | **PASS** |
| CI artifact/log upload | **PASS** |

After `b58a370…`, the final source-level adversarial pass produced **no further production changes**. The beta.2 release-preparation commit changes documentation/version metadata only and is required to pass the same complete CI pipeline before `main` is eligible for automatic publication.

## Release gate

`.github/workflows/release.yml` publishes only after the `Build and test` workflow completes successfully on `main`. It reads `version=` from `gradle.properties`, refuses to recreate an existing version tag, downloads the JAR produced by that exact validated workflow and marks alpha/beta/rc versions as prereleases.

Therefore the beta.2 GitHub release can only target a `main` commit that has already passed the complete server/client/companion/VanillaPlus matrix.

## Historical beta.1 baseline

The published 0.1.0-beta.1 release established native tipped arrows, crafting-table infusion, BODY potion bundles, dyed/effectless cauldron behavior, BedrockIfy ownership and the corrected GameTest registration guard. Beta.2 preserves those systems while adding the causal-wear architecture, multi-effect humanoid support and the hardened compatibility matrix above.

## Manual QA boundary

Automated coverage is intentionally deep, but it is not a claim that every third-party mod combination or physical gameplay sequence has been manually played. Manual multiplayer/playability checks remain useful for long-duration durability feel, modpack interaction and whether configured work thresholds are enjoyable rather than merely correct.
