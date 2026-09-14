# TM: arrow tipping, crafting infusion and beta gate

Status: **scope frozen / implementation in progress**.

Active roles for this work: **IMPLEMENTADOR** for production changes, then **ADVERSARIO** for independent review and holdout tests. The cycle follows the iterative TM workflow used by Scale Brews: every phase is re-reviewed until a complete pass produces no changes.

## 1. Frozen requirements

### R1 — Alchemical Leather arrow tipping is standalone

- `minecraft:arrow` used on an **Alchemical Leather potion cauldron** creates `minecraft:tipped_arrow` carrying the cauldron's exact `POTION_CONTENTS`.
- The feature works with **no BedrockIfy installed**.
- Default economy mirrors Bedrock Edition / BedrockIfy semantics at Alchemical Leather's three canonical doses:
  - 1 dose: up to 16 arrows;
  - 2 doses: up to 32 arrows;
  - 3 doses: up to 64 arrows.
- Partial stacks are supported and consume only the corresponding potion capacity. One dose is treated as 16-arrow capacity, so 1–16 arrows consume 1 dose, 17–32 consume 2 doses and 33–64 consume 3 doses.
- Creative mode does not consume the source arrows or cauldron dose and does not duplicate an identical tipped-arrow stack repeatedly into the inventory.
- The result preserves the cauldron's complete `PotionContents`, including custom color/effects/name where representable by the component.

### R2 — Arrow tipping is configurable

- A config file at `config/alchemical-leather.json` is created on first run if absent.
- Key: `"cauldronTippedArrows": true`.
- `false` disables **only Alchemical Leather's own arrow-on-own-potion-cauldron interaction**.
- Disabled behavior returns `PASS`; it must not eat the click, consume arrows or consume potion.
- Malformed/missing config fields fall back to documented defaults without crashing startup.

### R3 — BedrockIfy ownership is never stolen

- If BedrockIfy is absent, R1 still works.
- If BedrockIfy is installed and its cauldrons are enabled, BedrockIfy remains the sole owner of arrow tipping on `bedrockify:potion_cauldron`.
- Alchemical Leather must not intercept, duplicate, pre-consume or post-consume the BedrockIfy arrow gesture.
- Existing BedrockIfy ownership boundaries for bottles, colored water and crafting dye policy remain unchanged.

### R4 — Crafting-table armor infusion

- A shapeless special recipe accepts **exactly one compatible Alchemical Leather armor item plus exactly one potion item**.
- Accepted potion item types: normal potion, splash potion and lingering potion.
- The result is a copy of the input armor with the same infusion semantics as the cauldron:
  - normal and splash -> `timed` for non-instant effects;
  - lingering -> `stable` for non-instant effects;
  - instantaneous effects remain `instant`;
  - humanoid armor still accepts exactly one effect and must match its configured equipment slot;
  - BODY/animal armor accepts the potion's complete effect bundle.
- The potion's visual color is applied through `DYED_COLOR`, matching cauldron infusion.
- Unrelated armor components are preserved by copying the original stack.
- The potion leaves a glass bottle as crafting remainder.

### R5 — Crafting rejection/atomicity

The recipe must not match, and must produce no transformed armor, when:

- there is more than one armor item or more than one potion;
- any unrelated non-empty ingredient is present;
- the armor is not Alchemical Leather-compatible/dyeable;
- the potion has no effects;
- the armor is enchanted;
- humanoid slot rules reject the effect;
- a humanoid potion contains multiple effects;
- potion contents are missing/malformed.

Crafting an already-infused compatible armor item is allowed only when the new potion is otherwise valid; the new infusion replaces the previous Alchemical Leather infusion atomically, matching reinfusion through the cauldron.

### R6 — BedrockIfy crafting policy remains untouched

- The new infusion recipe is **not** a `minecraft:crafting_dye` recipe and does not patch `DyeRecipe`.
- BedrockIfy's intentional disabling of ordinary crafting-table dye recipes while its cauldron feature is active remains respected.
- No BedrockIfy mixin is targeted and no BedrockIfy production class is linked directly.

### R7 — Beta gate

- Version becomes `0.1.0-beta.1` only after the complete TM gate is green.
- README/player guide/architecture/validation are updated.
- Required automated validation passes standalone and with the real compatibility fixture containing BedrockIfy.
- The release is published as a prerelease named `Alchemical Leather 0.1.0-beta.1`.

## 2. Explicit exclusions

- No change to vanilla `minecraft:crafting_imbue` tipped-arrow crafting.
- No attempt to replace or modify BedrockIfy's arrow algorithm on BedrockIfy blocks.
- No config toggle for armor crafting in this sprint.
- Effectless potions remain valid cauldron dye baths, but they do **not** create a crafting-table infusion recipe result.
- No new UI/config screen dependency.

## 3. Implementation plan — IMPLEMENTADOR

- [ ] I1 Add resilient JSON config with `cauldronTippedArrows=true` default.
- [ ] I2 Extract/reuse a single armor-infusion transformation kernel so cauldron and crafting cannot drift semantically.
- [ ] I3 Add own-cauldron arrow tipping with 16/32/64 capacity semantics and exact `PotionContents` propagation.
- [ ] I4 Ensure arrows on BedrockIfy cauldrons always remain outside Alchemical Leather ownership.
- [ ] I5 Register a special shapeless armor-infusion recipe serializer.
- [ ] I6 Add the recipe data resource and glass-bottle remainder behavior.
- [ ] I7 Add focused GameTests for arrows, config-independent kernel behavior and crafting normal/splash/lingering modes.
- [ ] I8 Add rejection/atomicity/BedrockIfy ownership regressions.
- [ ] I9 Update docs and version only after implementation review converges.

## 4. Adversarial model — ADVERSARIO (pre-implementation)

Likely comfortable-but-wrong implementations to attack:

- treating every arrow stack as one full dose, allowing 64 arrows from one dose;
- rounding consumption down instead of up at 16/17 and 32/33 boundaries;
- reconstructing only a registry potion and silently losing custom effects/color/name;
- applying Alchemical Leather arrow logic to any potion-looking cauldron, stealing BedrockIfy ownership;
- disabling arrows in config but returning `FAIL`/`SUCCESS`, blocking a foreign or future handler;
- allowing any item with `POTION_CONTENTS` rather than only the three potion bottle item types in armor crafting;
- matching the recipe client-side on broad armor eligibility but rejecting server-side after output appears;
- mutating the original armor stack instead of returning a copy;
- leaving both humanoid and animal infusion components after replacing an infusion;
- allowing enchanted armor through crafting even though the cauldron rejects it;
- implementing lingering as timed, losing bottle-mode semantics;
- accepting effectless potions because they carry `POTION_CONTENTS`;
- losing custom name/durability/trim/other data components;
- changing or restoring BedrockIfy's intentionally revoked `DyeRecipe` behavior.

### Reserved holdouts

Concrete holdout inputs are intentionally not enumerated here. They will cover at least one dose-count boundary, one component-preservation case, one replacement/reinfusion case and one BedrockIfy ownership case after reading the implementation.

## 5. Acceptance matrix

- Arrow boundaries: 1, 16, 17, 32, 33, 64 arrows.
- Potion data: vanilla potion plus custom-effect/custom-color contents.
- Config: enabled, disabled, absent/malformed fallback at kernel/config level.
- Crafting bottles: potion, splash, lingering.
- Armor: humanoid valid slot, humanoid wrong slot, BODY multi-effect, enchanted, already infused, incompatible armor.
- Ownership: standalone; BedrockIfy installed/enabled on its own potion cauldron; existing dye ownership regressions.
- Build gates: `check`, registered GameTests, server GameTests, client check, full compatibility fixture.

A checkbox above means implemented, not demonstrated. Evidence belongs in `docs/validation.md` after it has actually run.
