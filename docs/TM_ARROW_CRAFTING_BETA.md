# TM: arrow tipping, crafting infusion and beta gate

Status: **release candidate complete; publication is gated by successful `main` CI**.

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

- Version becomes `0.1.0-beta.1` only after the complete TM functional gate is green.
- README/player guide/architecture/validation are updated.
- Required automated validation passes standalone and with the real compatibility fixture containing BedrockIfy.
- The release is published as a prerelease named `Alchemical Leather 0.1.0-beta.1` only from a successful `main` CI run.

## 2. Explicit exclusions

- No change to vanilla `minecraft:crafting_imbue` tipped-arrow crafting.
- No attempt to replace or modify BedrockIfy's arrow algorithm on BedrockIfy blocks.
- No config toggle for armor crafting in this sprint.
- Effectless potions remain valid cauldron dye baths, but they do **not** create a crafting-table infusion recipe result.
- No new UI/config screen dependency.

## 3. Implementation plan — IMPLEMENTADOR

- [x] I1 Add resilient JSON config with `cauldronTippedArrows=true` default.
- [x] I2 Extract/reuse a single armor-infusion transformation kernel so cauldron and crafting cannot drift semantically.
- [x] I3 Add own-cauldron arrow tipping with 16/32/64 capacity semantics and exact `PotionContents` propagation.
- [x] I4 Ensure arrows on BedrockIfy cauldrons always remain outside Alchemical Leather ownership.
- [x] I5 Register a special shapeless armor-infusion recipe serializer.
- [x] I6 Add the recipe data resource and glass-bottle remainder behavior.
- [x] I7 Add focused GameTests for arrows, config-independent behavior and crafting normal/splash/lingering modes.
- [x] I8 Add rejection/atomicity/BedrockIfy ownership regressions.
- [x] I9 Update release version, README/release notes, player guide, architecture, validation and publication automation after implementation review converged.

## 4. Adversarial model — ADVERSARIO

The pre-implementation model attacked these likely comfortable-but-wrong implementations:

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
- losing custom name/durability/other data components;
- changing or restoring BedrockIfy's intentionally revoked `DyeRecipe` behavior.

### Holdouts revealed after implementation

The reserved holdouts were revealed only after production code existed and added coverage for:

- partial dose retention as well as the 16/17 and 32/33 capacity boundaries;
- complete custom `PotionContents` propagation to tipped arrows;
- malformed potion items and non-dyeable armor in the crafting path;
- atomic reinfusion of BODY armor;
- explicit BedrockIfy arrow ownership with unchanged foreign block and input stack;
- synthetic stackable armor, which exposed a real bug: copying a modded stack with count greater than one could produce multiple infused armor items from a single potion. Production now forces crafting output count to exactly one.

## 5. Failure classification and iteration log

### F1 — creative GameTest fixture

The first implementation run compiled and passed 86 of 87 server GameTests. The only failure was the creative arrow test: `makeMockPlayer(GameType.CREATIVE)` did not guarantee the `abilities.instabuild` flag used by Minecraft/production code. This was classified as a **test-fixture defect**, not a production defect. The fixture now explicitly sets `instabuild=true`.

### F2 — stackable modded armor

Independent adversarial review noticed that `ItemStack.copy()` preserved an input stack count greater than one. Although vanilla armor is unstackable, a modded compatible armor item could therefore receive multiple infused outputs from one potion. This was classified as a **production defect**. The special recipe now forces the assembled output count to one and a holdout proves the invariant.

After F2, production was frozen again and the complete matrix was rerun.

## 6. Acceptance matrix and evidence

Functional implementation candidate: PR `#10`, head `8b8223c3ec309dee4b0271d777df9adf9fac8ca7`, pull-request CI run `34864583775`.

Documentation-complete beta candidate: head `26e280cbc99ea08ee944f0740ebb3d93c90d7921`, push CI run `34865578006`.

Both complete runs passed the same matrix:

| Gate | Result |
|---|---|
| Standalone build + server GameTests | **PASS — 88/88 required GameTests** |
| GameTest registration consistency | **PASS** |
| Client GameTest under Xvfb / llvmpipe | **PASS** |
| Real Clinging Reoriented + Scale Brews + BedrockIfy + Alex's Mobs fixture | **PASS — 88/88 required GameTests** |
| CI artifact upload | **PASS** |

The matrix includes:

- arrow boundaries and partial state: 1, 16, 17, 32, 33 and 64 arrows plus residual-dose cases;
- exact vanilla and custom-effect/custom-color/custom-name `PotionContents` propagation;
- configuration default, explicit disable value and malformed-field fallback at the pure parser level;
- normal, splash and lingering crafting bottle modes;
- humanoid valid/invalid slots, BODY multi-effect infusion, enchanted armor, already-infused armor, incompatible armor and malformed potion inputs;
- one-output invariant for synthetic stackable armor;
- standalone ownership and real BedrockIfy ownership boundaries.

## 7. Release-candidate closeout

The implementation, adversarial review and documentation-complete PR gate have converged. `0.1.0-beta.1` is the candidate version. A final no-behavior-change evidence commit is run through the same pipeline before merge. After merge, `main` must pass again; `.github/workflows/release.yml` then publishes only if that successful `main` build's `v0.1.0-beta.1` tag does not already exist, attaching the exact validated JAR and appending its target commit and SHA-256 to the release notes.

Actual execution evidence is also recorded in `docs/validation.md`. The GitHub release is the durable record of the final tagged commit and binary digest.
