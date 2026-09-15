# TM closeout: causal infusion wear and VanillaPlus compatibility

Status: **implementation and adversarial work complete; final documentation-complete CI gate pending**.

Frozen design source: [`TM_INFUSION_WEAR_SPEC.md`](TM_INFUSION_WEAR_SPEC.md). That specification is intentionally left unchanged as the pre-implementation contract; this document records what was actually built, attacked and converged.

## Objective

Complete the unfinished TM campaign for:

- causal durability wear for armor-owned potion effects;
- data-driven wear rules and optional semantic-event API;
- full VanillaPlus potion/effect classification coverage;
- first-party compatibility ownership in Clinging: Reoriented and Scale Brews;
- same-slot multi-effect humanoid infusion;
- an exhaustive standalone/client/real-fixture adversarial gate.

## S00 — specification and ownership migration

**Complete.**

- Frozen goals and causal attribution rules retained.
- Exact VanillaPlus potion/effect contributors identified from `R3Neer/VanillaPlus-26.2` rather than inferred from historical mod lists.
- Scale Brews owns Growth/Shrinking slot and wear resources.
- Clinging: Reoriented owns Reorientation slot and gravity semantic-event resources.
- Alchemical Leather removed duplicate first-party slot ownership while retaining selected data-only third-party compatibility.
- Alex's Mobs potion effects used by the pack are covered by slot-policy audit.

Merged companion commits:

- Scale Brews: `74066349eb33872d1f2b8584dfd05ffd96eae0f8`
- Clinging: Reoriented: `df1cff3a2fb9baf69d3bb8594159681b6966096d`

## S01 — wear model and public API

**Complete.**

Implemented:

- strict `WearRules` server-data loader;
- explicit `wear: none` and causal-source policies;
- `InfusionWearApi.emit(...)` semantic event API;
- slot-aware armor-source attribution through `EffectLedger`;
- `WearProgress` persistent fractional work per item/effect;
- ordinary Minecraft durability damage and normal item break behavior;
- Creative/infinite-material and Unbreakable invariants;
- reinfusion debt clearing;
- `infusionWear` configuration and parser coverage.

The emitting mod never chooses equipment or applies durability. Alchemical Leather remains authoritative for source effectiveness, owner slot, balance and accounting.

## S02 — builtin causal detectors

**Complete and adversarially hardened.**

Causal boundaries cover the current VanillaPlus-relevant generic mechanics, including self-propelled movement, jump contribution, Slow Falling gravity application, healing/damage/protection, combat contribution, breath prevention, reach, knockback resistance and selected effect proc origins.

The implementation deliberately avoids raw world-displacement charging where transport/impulse could be mistaken for self locomotion.

Reserved tests attack false attribution from external sources, redundant reach, wrong combat circumstances, persistence boundaries and malformed wear rules.

## S03 — companion adapters

**Complete and merged.**

### Scale Brews

Growth and Shrinking remain CHEST effects and explicitly use `wear: none`. The compatibility is data-only and Scale Brews continues to run standalone.

Merged commit `74066349eb33872d1f2b8584dfd05ffd96eae0f8` passed its post-merge build workflow (`35029211878`).

### Clinging: Reoriented

The historical compatibility branch had diverged heavily from current Clinging, so its semantic contract was reconstructed on a fresh branch from current `main` rather than force-rebased.

The optional adapter:

- reflectively resolves only the public Alchemical Leather event API when Alchemical Leather is installed;
- publishes a turn event only after the authoritative attempt path returns `SUCCESS`;
- derives the event owner from real active effects;
- reports controlled Reorientation flight only while Reorientation genuinely owns airborne self locomotion.

Adversarial review found one real production defect: continuous Reorientation work could be attributed while Elytra, fluid locomotion or independent player flight actually owned movement. The predicate was fixed and holdouts now cover those contexts plus passengers/support surfaces/Anatomy and lifecycle exclusions.

Documentation-complete PR head `8fbb3fcfd5ff97185c8018812923a1c897f35db6` passed full Clinging workflow `35031688007`; integration merged as `df1cff3a2fb9baf69d3bb8594159681b6966096d`.

## S04 — humanoid multi-effect support

**Complete.**

- Single-effect humanoid items retain the historical `alchemical_leather:infusion` representation.
- Same-slot bundles with at least two distinct effects use `alchemical_leather:humanoid_infusion`.
- BODY armor continues to use `animal_infusion` and may preserve repeated effect identities.
- Every humanoid bundle effect must map to the target's actual slot.
- Multi-effect timing and causal wear remain independent per effect.
- Reinfusion clears mutually exclusive infusion components and old wear debt atomically.
- Turtle Master-style Slowness/Resistance coverage is supported through the LEGS policy.

## S05 — exhaustive compatibility and adversarial gate

**Implementation complete; final exact documentation-head workflow is the remaining mechanical gate.**

The original reduced compatibility fixture was insufficient because it did not load all VanillaPlus potion/effect contributors. The final fixture now includes exact pack-pinned versions of:

- Alex's Mobs Continued;
- Friends & Foes;
- Wilder Wild;
- Deeper Dark;
- BedrockIfy;
- required runtime libraries;
- the exact merged Clinging and Scale commits above.

Third-party JARs are fetched from packwiz-pinned CDN URLs. First-party companion clones fail closed if `main` no longer equals the pinned converged SHA.

### Registry gate

The loaded potion registry must contain no unexplained wear gaps. Every loaded potion effect needs an explicit rule or explicit no-wear classification. Additional policy sentinels pin important first-party/pack slot and wear decisions.

### Cross-mod bridge gate

A dedicated holdout invokes the real Clinging `successfulTurn(ServerPlayer)` bridge and requires the real Alchemical Leather rule/accounting path to bill the FEET owner:

- 14 Reorientation turns → 28 fractional work, zero durability damage;
- 15th turn → one complete 30-work bucket, exactly one durability damage, zero residual work.

The test establishes explicit FEET ownership in the ledger because synthetic GameTest `ServerPlayer` instances intentionally lack a play connection and production equipment reconciliation correctly defers until JOIN.

## Red-run / failure classification log

The campaign kept failures visible and classified them before changing code:

1. **Infrastructure:** expanded fixture failed through brittle Modrinth Maven resolution. Replaced with exact packwiz CDN artifacts.
2. **Test-fixture drift:** inherited Reorientation tests still brewed with Shulker Shell after current Clinging moved to Gravity Charge. Updated fixture only.
3. **Test-fixture type:** initial reflection holdout used a player mock that did not satisfy the real `ServerPlayer` signature. Corrected fixture.
4. **Test-fixture ownership:** manually projected effect without an armor-owner slot; wear engine correctly refused ownerless billing. Corrected fixture.
5. **Test-fixture lifecycle:** production `EquipmentInfusions.sync` intentionally defers for pre-login `ServerPlayer` with null connection. Test now establishes slot-aware ledger state without weakening production safety.
6. **Fixture pinning:** direct shallow fetch by merge SHA was brittle. Final workflow clones `main` and asserts exact expected SHA.
7. **Production defect:** Clinging continuous Reorientation attribution included Elytra/fluid/player-flight contexts. Fixed in Clinging and converted to permanent holdouts.

No Alchemical Leather production invariant was weakened merely to turn a fixture green.

## Documentation closeout

Updated for the current development architecture:

- `README.md`
- `docs/GUIDE.md`
- `docs/architecture.md`
- `docs/validation.md`
- `docs/COMPATIBILITY.md`
- this closeout record.

The frozen pre-implementation specification remains unchanged.

## Final acceptance checklist

- [x] standalone wear engine/API implemented;
- [x] strict JSON/parser and fractional persistence coverage;
- [x] builtin causal detector campaign completed;
- [x] same-slot humanoid multi-effect support completed;
- [x] Scale Brews ownership converged and merged;
- [x] Clinging ownership/semantic bridge converged and merged;
- [x] real Clinging production attribution defect found, fixed and held out;
- [x] exact VanillaPlus potion-contributor fixture assembled;
- [x] dynamic potion-registry classification gate added;
- [x] real companion-bridge → owning-item durability holdout added;
- [x] player/architecture/compatibility/validation documentation updated;
- [ ] exact documentation-complete Alchemical Leather branch head passes standalone server, client and pinned VanillaPlus compatibility matrix;
- [ ] PR #11 merged to `main`;
- [ ] exact merged `main` commit passes the same CI pipeline.

The final three boxes are intentionally left open until their exact GitHub Actions evidence exists. TM is not improved by writing “PASS” slightly before reality catches up.
