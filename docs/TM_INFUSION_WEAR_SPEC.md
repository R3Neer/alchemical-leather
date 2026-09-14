# TM: causal infusion wear and optional compatibility API

Status: **temporary pre-implementation specification**. No production behavior is changed by this document.

Working branch: `chatgpt/infusion-wear-spec`.

This document freezes the current design discussion before implementation. The implementation must use the iterative TM workflow already established in this repository: IMPLEMENTER -> adversarial review -> holdouts -> repeat until a complete pass produces no changes.

## 1. Goal

Add optional durability wear to potion-infused armor when, and only when, the infusion performs attributable mechanical work.

The system must satisfy all of the following at once:

- lingering/stable infusion remains permanently bound to the item instead of expiring by time;
- stable infusion is not necessarily free to operate forever: effects that perform measurable work may slowly wear the carrier item;
- timed infusion keeps its existing time semantics and may also accumulate wear while it performs work;
- persistent state effects may explicitly declare zero wear;
- positive and negative effects follow the same causal rule;
- an item is never charged merely because its wearer happened to move, heal, take damage, deal damage or experience another relevant state change while the effect was present;
- external transport, external healing/damage and foreign effect sources must not be misattributed to the armor;
- third-party mods can integrate without Alchemical Leather attempting to understand every possible future effect;
- Alchemical Leather remains fully functional as a standalone mod with no hard runtime dependency on any VanillaPlus compatibility mod.

## 2. Non-goals

- Do not reimplement vanilla anvil repair. Compatible leather armor already uses its normal repair material behavior; no Alchemical Leather config toggle is required for this.
- Do not make JSON a scripting language.
- Do not infer arbitrary third-party semantics from an effect name, category, particle color or registry namespace.
- Do not charge durability merely because an effect is active.
- Do not make all effects wear at the same rate or use the same unit of work.
- Do not require VanillaPlus, Scale Brews, Clinging Reoriented, Alex's Mobs Continued, Friends&Foes, Wilder Wild, Deeper Dark or BedrockIfy for standalone operation.

## 3. Core invariant: causal attribution

An infused item may accumulate wear only when all of these are true:

1. infusion wear is globally enabled;
2. the item is currently equipped in the slot that owns the infusion;
3. the reported effect exists on that item;
4. Alchemical Leather's armor copy of that effect is the effective source at the moment of use, rather than a stronger/equal winning external source;
5. a configured wear rule exists for that effect and is not `none`;
6. the configured detector or API event reports work caused by that effect;
7. the reported work is finite, positive and server-authoritative.

Observation of a coincident outcome is not enough. Prefer instrumentation at the point where the effect changes the mechanic.

Examples:

- Regeneration: charge only health restored by Regeneration's own effect tick, never health restored by food, Instant Health, a beacon or another mod.
- Poison: charge only health removed by Poison's own effect tick, never damage from a zombie that happened on the same tick.
- Fire Resistance: charge only fire/lava damage actually prevented by this effect.
- Strength/Weakness: charge only successful attack outcome attributable to their attack-damage contribution; a missed, cancelled, fully blocked or invulnerable hit is not a use.
- Reach: charge only an interaction that succeeds outside the range the wearer would have had without the effect.
- Speed/Slowness: charge only locomotion that the affected entity itself generates and that the movement-speed effect actually modifies.

## 4. Movement attribution

World-space displacement is not a valid wear meter by itself.

A movement detector must distinguish self-propelled locomotion from transport and external impulses. At minimum, no movement wear may be charged for displacement produced only by:

- a ridden vehicle or mount;
- another entity carrying the wearer;
- a Living Platform / Scale Brews anatomical support;
- Clinging Reoriented moving-surface transport;
- a piston or flying machine carrying the wearer;
- knockback or another external impulse;
- water/current/conveyor-style external transport where the effect is not modifying the wearer's own locomotion;
- teleportation;
- Elytra fall-flying when the effect does not participate in Elytra physics.

If a support moves 200 blocks while the player walks 12 blocks relative to that support, a compatible movement detector may charge for the player's 12 blocks, not 212 blocks.

Movement attribution should be implemented from movement causes / travel paths where practical, not by subtracting positions after the fact. Existing Clinging Reoriented `MovingSurface` transport accounting and Scale Brews anatomy support APIs are useful compatibility signals but must not become required dependencies.

### 4.1 Speed and Slowness

Verified design fact for Minecraft Java 26.2: vanilla Speed/Slowness change movement speed but do not change Elytra fall-flying speed. Rocket propulsion while fall-flying therefore does not constitute Speed/Slowness work either.

Consequences:

- normal Elytra glide: zero Speed/Slowness wear;
- firework-boosted Elytra flight: zero Speed/Slowness wear;
- passenger movement: zero rider Speed/Slowness wear;
- moving-platform transport: zero transport wear;
- walking/running/sneaking/crawling or another movement mode only counts if 26.2's actual travel path is affected by the movement-speed modifier.

Swimming behavior must be proven against 26.2 in GameTests before the detector declares it chargeable; the detector must follow actual game behavior rather than a historical assumption.

### 4.2 Slow Falling

Verified design fact: Slow Falling does affect Elytra flight. Vanilla treats the interaction as intended behavior. Unboosted gliding becomes much slower and more efficient; firework boosts still work, while the Slow Falling physics continues to affect the resulting glide/momentum.

Therefore Slow Falling must not globally exclude `isFallFlying()`.

The preferred detector is not raw distance. It should report only ticks / gravity contribution for which Slow Falling's actual physics branch modified descent or fall-flying behavior. It should report zero when another state already makes that branch irrelevant.

## 5. Clinging versus Reorientation

These effects deliberately have different wear semantics.

### 5.1 Clinging

Clinging is a discrete capability: one successful voluntary airborne gravity decision before real support resets the charge.

Wear is charged only when a request successfully establishes a new gravity direction.

No wear for:

- failed or ambiguous requests;
- requesting the current direction;
- subsequent fall time or distance;
- remaining attached to the resulting surface;
- passive support/platform transport;
- unrelated external gravity changes.

Provisional balance: a successful Clinging turn contributes **4 work units**. With a 30-unit durability budget this is approximately 7.5 successful turns per durability point, about 480 successful turns over the 64 usable points of vanilla leather boots if ordinary damage is ignored.

### 5.2 Reorientation

Reorientation is sustained gravity-flight control and may therefore charge both sustained controlled flight and successful manoeuvres.

Provisional balance:

- durability budget: **30 work units per 1 durability**;
- controlled Reorientation flight: **1 work unit per second**;
- successful gravity turn: **+2 work units**.

A fresh vanilla leather boot item therefore provides approximately 32 minutes of straight controlled Reorientation flight before alchemical exhaustion if ordinary armor damage is ignored; manoeuvres reduce this gradually.

Continuous work is charged only while Reorientation actually owns relevant airborne gravity locomotion. It is not charged while the wearer is merely being transported.

Mounted Reorientation is special:

- normal mount travel: zero continuous rider wear;
- a successful rider-requested Reorientation gravity turn of a compatible mount: charge the manoeuvre event to the rider's infused item;
- subsequent movement of the mount: zero continuous rider wear.

## 6. Alchemical exhaustion

Provisional desired behavior:

- alchemical wear can reduce an item only to **1 durability remaining**;
- at 1 durability the infusion remains stored but is inactive until the item is repaired;
- ordinary armor damage remains ordinary and may still break the item;
- stable infusion resumes after repair and is never consumed merely by wear;
- timed infusion should not burn its timer while the infusion is disabled by alchemical exhaustion, because no effect is being delivered during that period;
- partial wear progress must survive unequip/re-equip and save/load, otherwise players can reset it trivially.

Implementation should use a small persistent item data component for fractional/partial wear progress. A single normalized per-item progress value is preferable to one counter per effect: each rule converts natural work to durability fraction before contributing to the item accumulator. This also handles BODY armor with several active effects cleanly.

Open implementation detail for TM review: decide whether a successful material repair clears fractional wear progress. The intuitive default is yes, because repair refreshes the physical carrier, but this must not change vanilla repair amounts/costs.

## 7. Global configuration

`config/alchemical-leather.json` should contain one global opt-out for the mechanic:

```json
{
  "cauldronTippedArrows": true,
  "infusionWear": true
}
```

This is a server-authoritative behavior toggle. It does not define individual effect semantics.

Per-effect semantics belong to reloadable server-data JSON resources.

## 8. Per-effect wear JSON

Create a new resource family separate from `effect_slots`:

`data/<effect_namespace>/alchemical_leather/wear_rules/<effect_path>.json`

Wear rules are intentionally separate from slot rules because BODY/animal armor may contain effects that have no humanoid slot mapping, and a mod may want to define wear semantics without making an effect valid on a new humanoid slot.

The effect ID is derived from the resource namespace/path, matching the existing slot-rule convention.

Common optional gates should mirror `EffectSlotRules` where useful:

```json
{
  "enabled": true,
  "requires_mod": "example_mod",
  "requires_effect": "example_mod:example_effect",
  "requires_resource": "example_mod:some/resource.json",
  "work_per_damage": 30.0,
  "sources": []
}
```

Unknown detector/source IDs or malformed numeric values must fail the data reload clearly rather than silently creating free or destructive wear.

### 8.1 Explicit no-wear rule

Every supported effect should be able to make zero wear an explicit policy:

```json
{
  "wear": "none"
}
```

This is different from having no rule. `none` means the integration consciously decided that the effect is a persistent state or otherwise should not wear the item. Missing rule means unsupported/unclassified for the wear system.

Automated compatibility audits may therefore distinguish deliberate zero wear from forgotten integration.

### 8.2 Built-in detector rule

Example shape for a simple generic effect:

```json
{
  "work_per_damage": 512.0,
  "sources": [
    {
      "type": "builtin",
      "detector": "alchemical_leather:self_propelled_movement_speed"
    }
  ]
}
```

Built-in detectors own their causal semantics. JSON may tune balance but must not contain arbitrary boolean expressions such as `if_flying && !vehicle && ...`.

This avoids turning data packs into an untestable scripting engine.

### 8.3 API-event rule

Complex effects can publish semantic events and let JSON own balance:

```json
{
  "requires_mod": "clinging_reoriented",
  "work_per_damage": 30.0,
  "sources": [
    {
      "type": "event",
      "event": "clinging_reoriented:controlled_flight_tick",
      "work": 0.05
    },
    {
      "type": "event",
      "event": "clinging_reoriented:gravity_turn",
      "work": 2.0
    }
  ]
}
```

At 20 ticks/s, `0.05` work per controlled-flight tick produces 1 work unit/s. The same gravity-turn event can have a different weight in the Clinging rule.

The API producer reports semantic occurrences; the wear JSON controls balance. This lets pack authors tune durability without recompiling the source mod.

## 9. Built-in detector policy

Alchemical Leather may provide a deliberately small set of generic causal detectors. Initial candidates:

- `self_propelled_movement_speed`: movement attributable to the wearer and actually affected by movement-speed semantics; excludes transport, fall-flying and external impulses;
- `jump_boost`: successful wearer jump for which Jump Boost contributes additional jump impulse;
- `effect_tick_healing`: HP actually restored inside the armor-owned effect's tick;
- `effect_tick_damage`: HP actually removed inside the armor-owned effect's tick;
- `attack_damage_added`: successful final attack contribution from a positive attack-damage modifier;
- `attack_damage_removed`: successful final attack contribution removed by a negative attack-damage modifier;
- `damage_prevented`: damage specifically prevented by the configured defensive effect;
- `oxygen_preserved`: air/oxygen loss actually prevented by the effect;
- `slow_falling_physics`: ticks / gravity contribution where Slow Falling modifies normal fall or Elytra physics;
- `reach_extension_used`: successful interaction outside the no-effect range but inside the effect range;
- `knockback_prevented`: impulse actually removed by knockback resistance.

A detector must have one stable natural work unit (blocks, HP, seconds/ticks, impulse, occurrences, etc.). `work_per_damage` converts that natural work into one durability point.

The actual initial detector set should remain minimal. If a detector cannot be defined without effect-specific assumptions, use an API event instead.

## 10. Public compatibility API

The core API should be small and should never let a caller directly damage an armor stack.

Conceptual surface:

```java
public final class InfusionWearApi {
    public static boolean emit(
        LivingEntity wearer,
        Holder<MobEffect> effect,
        Identifier event,
        double amount
    );
}
```

Semantics:

- `wearer` is the entity whose equipped infusion is being used, even if another entity is the object being moved/affected (for example a rider turns a mount with Reorientation);
- `effect` identifies the semantic capability used;
- `event` is namespaced and owned by the producer mod;
- `amount` is a natural event quantity and must be finite and positive;
- the call is only a candidate usage report;
- Alchemical Leather rechecks equipped item ownership, effect winner/source, rule presence, exhaustion and global config before accepting work;
- the API returns whether any armor-owned work was accepted, useful for tests/debugging but not for gameplay branching.

The producer should not know the armor slot, item durability, lingering/timed mode, current accumulator or balance threshold.

### 10.1 Optional dependency pattern

Third-party mods should be able to support Alchemical Leather without requiring it.

Recommended pattern:

- compile against the small API as an optional/compile-only dependency;
- isolate direct API references in a dedicated compatibility class;
- initialize that class only when `alchemical_leather` is present;
- keep the producer's normal gameplay path identical when Alchemical Leather is absent;
- optionally ship `wear_rules` JSON in the producer mod itself; the files are inert when Alchemical Leather is absent.

If direct optional linkage proves fragile under Fabric class loading, an Alchemical Leather-specific entrypoint/bridge can be added during implementation. The important contract is that the source mod remains independently usable.

### 10.2 API limitations by design

The API does not attempt to:

- discover whether a custom action was meaningful;
- infer which event a third-party effect should publish;
- run arbitrary predicates supplied by JSON;
- resolve another mod's internal ownership rules;
- make client-only events authoritative;
- make a reported event count if the armor infusion is not the effective source.

The mod that owns the mechanic is the best authority on whether its custom action succeeded.

## 11. VanillaPlus default integration policy

Alchemical Leather should remain general-purpose, but the VanillaPlus ecosystem is a useful first-party compatibility target because the repository already ships optional effect-slot resources for several of these mods.

Recommended ownership order:

1. **Vanilla effects:** Alchemical Leather owns and tests default wear rules.
2. **Data-only third-party effects:** preferably ship optional JSON either in the source mod or, as a convenience fallback, bundled in Alchemical Leather behind `requires_mod` / `requires_effect` guards.
3. **Complex custom mechanics:** preferably the owning mod emits semantic API events. Alchemical Leather may bundle the matching JSON balance rule.
4. **Uncooperative external mods:** an isolated optional Alchemical Leather adapter is acceptable for VanillaPlus convenience if there is no safe generic detector and no upstream integration. It must never become a hard dependency and must be covered by a real compatibility fixture.

Bundled optional integration is valuable when it gives users correct behavior simply by installing Alchemical Leather, but logic should live as close as possible to the mechanic that can authoritatively say "this action succeeded".

## 12. Provisional VanillaPlus wear matrix

This table covers effects currently admitted by Alchemical Leather's VanillaPlus-oriented resources plus the known Turtle Master gap. Balance values remain provisional until playtesting; the attribution semantics are the important frozen part.

| Effect | Causal use | Must not charge for | Preferred integration |
|---|---|---|---|
| Speed | self-propelled movement actually modified by Speed | Elytra, rockets, passengers, supports/platforms, impulses, teleport | builtin movement detector |
| Slowness | same as Speed, but negative contribution | same exclusions as Speed | builtin movement detector |
| Jump Boost | own successful jump with additional jump impulse | mount jump, external launch, swimming input | builtin jump detector |
| Strength | successful damage contribution from Strength | miss, cancelled hit, invulnerability, fully blocked result | builtin attack contribution |
| Weakness | successful damage removed by Weakness | same non-results as Strength | builtin attack contribution |
| Regeneration | HP actually healed by Regen tick | food, Instant Health, beacon, foreign heal | builtin effect-tick healing |
| Poison | HP actually removed by Poison tick | other damage; no charge when Poison cannot reduce health | builtin effect-tick damage |
| Fire Resistance | fire/lava damage actually prevented by this effect | pre-existing immunity / foreign cancellation | builtin damage prevention |
| Water Breathing | oxygen loss actually prevented | breathable context, aquatic immunity, foreign winning source | builtin oxygen preservation |
| Night Vision | persistent perception state | n/a | explicit `none` |
| Blindness (Deeper Dark potion) | persistent perception state | n/a | explicit `none` |
| Invisibility | persistent stealth state | n/a | explicit `none` initially; future AI-event integration may refine |
| Slow Falling | ticks where Slow Falling actually modifies fall/Elytra physics | transport/downward platform movement, irrelevant states | builtin slow-falling detector |
| Instant Health | infusion consumes itself when applied | n/a | no additional wear |
| Instant Damage | infusion consumes itself when applied | n/a | no additional wear |
| Infested | successful infestation proc | failed/no-spawn outcome | vanilla-specific detector/event |
| Oozing | successful slime-spawn death proc | failed/no-spawn outcome | vanilla-specific detector/event |
| Wind Charged | successful wind-burst death proc | failed/cancelled outcome | vanilla-specific detector/event |
| Weaving | actual cobweb-related benefit and/or successful death web proc, depending final vanilla semantics | passive unrelated movement / failed placement | vanilla-specific detector(s) |
| Resistance (Turtle Master) | damage actually prevented by Resistance | damage already cancelled by another cause | builtin damage prevention |
| Turtle Master Slowness component | self-propelled movement actually slowed | all Speed/Slowness transport exclusions | builtin movement detector |
| Clinging | successful new gravity direction | fall time/distance, support transport, failed request | API gravity-turn event |
| Reorientation | owned controlled-flight time + successful turn | passive transport; ordinary mount travel; Elytra ownership | API controlled-flight + turn events |
| Growth (Scale Brews) | persistent body state | n/a | explicit `none` |
| Shrinking (Scale Brews) | persistent body state | n/a | explicit `none` |
| Reach (Friends&Foes) | successful action that requires extra reach | action already inside no-effect reach | builtin reach detector if compatible; otherwise adapter/API |
| Reach Boost (Wilder Wild) | successful action that requires extra reach | action already inside no-effect reach | builtin reach detector if compatible; otherwise adapter/API |
| Scorching (Wilder Wild) | successful effect-caused ignition / added burn work | failed proc, immunity, no effective burn change | adapter/API unless generic hook is exact |
| Knockback Resistance (Alex's Mobs Continued) | knockback impulse actually removed by effect | no incoming knockback / foreign cancellation | builtin generic knockback detector if exact |
| Poison Resistance (Alex's Mobs Continued) | poison application actually rejected because of this effect | poison already invalid/immune for another reason | adapter/API unless generic hook can identify cause |
| Soulsteal (Alex's Mobs Continued) | HP actually restored by the life-steal result | full-health/no-heal outcome, cancelled/blocked hit | adapter/API |
| Bug Pheromones (Alex's Mobs Continued) | persistent AI relationship state | n/a | explicit `none` initially |
| Lava Vision (Alex's Mobs Continued) | persistent perception state | n/a | explicit `none` |

### 12.1 Turtle Master prerequisite

Current humanoid infusion resolution rejects multi-effect potions, while BODY armor can store a bundle. Turtle Master therefore remains a pre-existing coverage gap for humanoid armor.

The wear engine must be capable of accounting for both Slowness and Resistance on one item, but the project must not claim complete VanillaPlus potion coverage until humanoid Turtle Master infusion semantics are intentionally resolved.

## 13. Effect ownership and external sources

The existing `EffectLedger` already separates armor and external copies of a MobEffect and projects a winner. Wear attribution should build on that distinction.

Rules:

- if an external stronger effect wins, the armor does not wear for that effect;
- if the armor effect is the effective source, candidate work may count;
- when two sources are mechanically indistinguishable but only one is projected, only the projected armor source may be charged;
- removing/replacing/reinfusing the item must not leave a stale effect -> item ownership pointer;
- BODY multi-effect items must attribute all accepted work to that one equipped item without double-damaging the item for one normalized durability threshold crossing.

## 14. Data/API audit requirement

The validation profile should enumerate every loaded potion/effect that Alchemical Leather can infuse in the full VanillaPlus fixture.

For each admitted effect it must find exactly one explicit wear classification:

- a valid wear rule with one or more sources; or
- explicit `wear: none`; or
- instantaneous self-consumption semantics.

Missing classification is a validation failure, not implicit free wear.

This prevents a newly added VanillaPlus potion from silently bypassing the system.

The standalone profile must run the same core engine with no optional mods installed.

## 15. Adversarial cases reserved for implementation

The implementation/review cycle must attack at least these failure modes:

- Speed boots wearing while riding a horse, minecart, boat or Elytra;
- Speed/Slowness wearing from firework-boosted Elytra flight;
- movement wear while standing still on a moving Living Platform or flying machine;
- relative walking on a moving support charging total world displacement instead of own movement;
- Slow Falling failing to wear during Elytra flight even though it changes Elytra physics;
- Slow Falling wearing merely because a descending platform carries the player downward;
- Regeneration charging for food/beacon/heal effects;
- Poison charging for unrelated incoming damage;
- Strength/Weakness charging for misses, cancelled hits or fully ineffective attacks;
- Fire Resistance charging while already immune for another reason;
- Water Breathing charging an entity that can already breathe underwater;
- Reach charging ordinary in-range interactions;
- Clinging charging failed turns or time after a turn;
- Reorientation charging ordinary mount travel;
- Reorientation failing to charge a successful rider-requested gravity turn of a mount;
- external stronger potion source winning while the armor still wears;
- wear progress resetting on unequip/relog;
- alchemical wear destroying the item instead of stopping at one durability;
- exhausted stable infusion being deleted instead of merely disabled;
- duplicate counting when both a bundled fallback adapter and an upstream API integration are present;
- optional compatibility classes linking/crashing when the target mod is absent.

## 16. Open balance values

The following are design placeholders, not final acceptance constants unless later frozen by playtesting:

- Speed/Slowness: approximately 512 blocks of eligible self-propelled movement per durability at base level;
- Jump Boost: approximately 16 eligible jumps per durability at level I, preferably scaling from actual additional impulse rather than a crude amplifier multiplier;
- Regeneration/Poison: approximately 8 HP of attributable health delta per durability;
- defensive damage prevention: tune in HP prevented rather than time active;
- Slow Falling: prefer effective-physics time/gravity work over raw descent distance so Elytra interaction is represented correctly;
- Clinging: 4 work per successful turn, 30 work/durability;
- Reorientation: 1 work/s controlled flight +2/turn, 30 work/durability.

Where possible, stronger effects should naturally produce more work because the detector measures the actual contribution (extra damage, extra impulse, health delta, etc.), rather than multiplying wear blindly by amplifier.

## 17. Release discipline

This document is temporary and should be removed or folded into permanent architecture/guide/validation documentation once implementation converges.

No production implementation, version bump or release should occur from this branch until the user explicitly moves the task beyond analysis/specification.