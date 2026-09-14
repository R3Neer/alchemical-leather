# TM: causal infusion wear, optional effect ownership and compatibility API

Status: **temporary pre-implementation specification**. Production implementation follows this document on branch `chatgpt/infusion-wear-spec`.

Method: iterative TM. Every implementation phase is followed by an adversarial review with reserved holdouts. Production changes are revised until a complete review produces no further changes.

## 1. Frozen goals

Alchemical Leather must remain a standalone Fabric mod while providing a generic protocol that lets armor infusions wear their carrier item only when the infusion performs attributable mechanical work.

Required behavior:

- normal/splash potion infusions retain their current timed semantics;
- lingering potion infusions retain stable/non-expiring semantics;
- positive and negative effects follow the same causal wear rules;
- wear is not inferred merely from an effect being active or from coincident entity state changes;
- wear damages the actual infused armor item and **may break it normally at zero durability**;
- there is no one-durability floor, dormant-infused state or special reactivation behavior;
- ordinary vanilla repair behavior is not replaced or gated by Alchemical Leather;
- third-party mods may define slot ownership and wear behavior without Alchemical Leather knowing their internals;
- optional built-in compatibility may be shipped for common third-party effects when that is materially more convenient for users and requires no hard runtime dependency;
- Alchemical Leather's own vanilla and selected external defaults remain data-driven and optional where possible;
- the full VanillaPlus compatibility fixture must have explicit coverage for every potion effect that can be infused.

## 2. Ownership model

### 2.1 Alchemical Leather owns the protocol

Alchemical Leather owns:

- infusion storage/components;
- effect/source arbitration through the existing armor/external ledger;
- resource loading for effect-slot rules and wear rules;
- builtin causal detectors for generic Minecraft mechanics;
- a small public compatibility API for semantic events only the owning mod can know;
- attribution of API events back to the equipped infused item;
- fractional work accumulation and armor durability damage;
- validation, malformed-resource fallback/failure policy and test coverage.

### 2.2 A mod should own semantics of its own effects

For first-party companion mods maintained alongside Alchemical Leather:

- **Clinging Reoriented** owns the armor slot declarations for `clinging_reoriented:*` effects and publishes semantic wear events such as successful gravity turns / controlled Reorientation flight;
- **Scale Brews** owns the armor slot declarations for `scalebrews:*` effects and explicitly declares its stable body-state effects' wear policy;
- Alchemical Leather removes duplicate bundled slot declarations for those first-party effects after migration.

The companion mods must still work normally when Alchemical Leather is absent. Their compatibility resources/API adapter must therefore be optional and have no hard runtime dependency.

### 2.3 Optional bundled compatibility for third-party mods

Alchemical Leather may bundle optional data-only compatibility for third-party mods when doing so substantially improves the end-user experience and does not link their production classes. Current intended examples include the Alex's Mobs Continued effects used by VanillaPlus, plus existing optional data rules for other pack effects where appropriate.

## 3. Core invariant: causal attribution

An infused item may accumulate wear only when all of these are true:

1. global infusion wear is enabled;
2. the item is currently equipped in the slot that owns the infusion;
3. the reported effect exists on that specific item;
4. the armor infusion is the effective source at the moment of use, rather than a stronger/equal winning external source;
5. the effect has an explicit wear policy and that policy is not `none`;
6. the configured builtin detector or semantic API event reports work caused by the effect;
7. the reported work is finite, positive and server-authoritative.

Observation of a coincident outcome is never sufficient when a causal hook exists.

Examples:

- Regeneration charges only health restored by Regeneration's own tick, never food, Instant Health, beacon healing or another mod.
- Poison charges only health removed by Poison's own tick, never unrelated damage in the same tick and never attempted poison damage that was prevented.
- Strength/Weakness charge only the portion of a successful attack result attributable to their attack-damage modification.
- Fire Resistance/Resistance charge only damage actually prevented by that effect.
- Soulsteal charges only health actually restored by a successful Soulsteal proc.
- Reach charges only a successful interaction/attack that was outside the range available without the effect.

## 4. Movement attribution

World-space displacement alone is not a valid wear meter.

Movement wear must distinguish **self-propelled locomotion modified by the effect** from transport or external impulses. No movement wear is charged for displacement caused only by:

- mounts or ridden vehicles;
- another entity carrying the wearer;
- Living Platforms / Scale Brews anatomical support;
- Clinging Reoriented moving-surface transport;
- pistons or flying machines carrying the wearer;
- knockback or external impulses;
- currents/conveyors where the movement-speed effect does not modify the wearer's own locomotion;
- teleportation;
- Elytra fall-flying when that effect does not participate in Elytra physics.

If a moving support travels 200 blocks while the wearer walks 12 blocks relative to it, only the attributable 12 blocks may count.

Movement detectors should hook causes/travel mechanics where practical rather than infer by subtracting absolute positions after the fact.

### 4.1 Speed and Slowness

Speed/Slowness wear applies only to locomotion modes whose movement-speed mechanics they actually modify. In particular:

- riding/vehicle transport does not count;
- passive platform/flying-machine transport does not count;
- Elytra fall-flying does not count;
- firework propulsion while fall-flying does not count;
- external knockback/impulses do not count.

Positive and negative movement-speed effects use the same attribution model.

### 4.2 Jump Boost

Only an actual jump performed by the affected wearer, where Jump Boost contributes additional jump power, counts. Vehicle/mount jumps, swimming ascent, piston launch, knockback and other external launch sources do not count.

### 4.3 Slow Falling

Slow Falling may affect fall-flying/Elytra physics and therefore cannot simply exclude Elytra. It should charge only while its own physics branch is materially modifying the entity's fall/fall-flying behavior. Firework propulsion itself is not Slow Falling work, but Slow Falling may continue contributing while a rocket-boosted Elytra flight is in progress.

## 5. Clinging and Reorientation economy

### 5.1 Clinging

Clinging is a discrete one-decision aerial ability.

- only a successful voluntary gravity-direction change counts;
- same-direction attempts, blocked attempts, no-space, ambiguous input and foreign gravity do not count;
- no wear is charged merely for remaining in the resulting gravity, falling afterwards, travelling distance, landing or being carried by a support;
- provisional work: **4 units per successful gravity link/turn**.

### 5.2 Reorientation

Reorientation is sustained gravity locomotion.

- **30 work units = 1 durability**;
- **+1 work unit per second** of Reorientation-controlled self locomotion;
- **+2 work units per successful voluntary gravity turn**;
- passive vehicle/platform/flying-machine transport contributes no continuous work;
- ordinary riding contributes no continuous work;
- a successful rider-requested Reorientation turn of a compatible airborne mount does count as the discrete gravity-turn event;
- failed/blocked/same-direction requests do not count.

With leather boots this is intentionally a slow maintenance cost, not a short fuel tank. The piece still breaks normally if wear reaches zero.

## 6. Stable/Lingering semantics

Lingering/stable means the **infusion itself does not expire with time**. It does not imply zero operating wear.

Effects that are persistent states rather than measurable work may explicitly declare `wear: none`. Initial intended examples:

- Scale Brews Growth;
- Scale Brews Shrinking;
- Night Vision;
- Blindness;
- Lava Vision;
- likely Invisibility and Bug Pheromones unless a robust causal detector is later justified.

This keeps lingering meaningful without turning every permanent state into a disguised durability timer.

## 7. Resource formats

### 7.1 Effect-slot ownership

Existing slot resources remain conceptually:

`data/<effect-namespace>/alchemical_leather/effect_slots/<effect-path>.json`

Supported fields remain at least:

```json
{
  "slot": "boots",
  "requires_mod": "example_mod",
  "requires_effect": "example_mod:example_effect",
  "requires_resource": "example_mod:some/resource.json",
  "enabled": true
}
```

Slots: `helmet`, `chestplate`, `leggings`, `boots`.

Companion first-party mods should ship their own namespaced slot resources. Alchemical Leather may bundle optional resources for third-party mods when practical.

### 7.2 Wear rules

Wear rules are independent of effect-slot rules:

`data/<effect-namespace>/alchemical_leather/wear_rules/<effect-path>.json`

Absence of a rule means **unclassified/unsupported for wear**, not implicitly `none`.

Explicit no-wear example:

```json
{
  "wear": "none"
}
```

Builtin detector example:

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

Semantic event example:

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

The final parser may normalize details, but the semantic separation is frozen: JSON selects a known builtin detector or event and configures work amounts; it does not implement arbitrary logic.

### 7.3 JSON limitations

JSON must **not** become a scripting language. It may not express arbitrary entity predicates, Java calls, effect-specific state machines or free-form boolean expressions such as `moving && !elytra && !horse`.

Use a builtin detector when Minecraft exposes a stable generic causal hook. Use the API when only the owning mod knows whether its semantic action succeeded.

## 8. Public compatibility API

The public API should be minimal and dependency-light. Provisional semantic shape:

```java
InfusionWearApi.emit(
    LivingEntity wearer,
    Holder<MobEffect> effect,
    Identifier event,
    double amount
);
```

The emitting mod reports a semantic event only. It does **not** choose an armor item, mutate durability, inspect lingering/timed mode or know Alchemical Leather's partial accumulator.

Alchemical Leather validates the event, source ownership and configured rule, then converts event amount to work and work to durability damage.

Companion mods should isolate optional calls behind a compatibility adapter loaded only when `alchemical_leather` is present (or use an equivalent linkage-safe strategy).

## 9. Durability application

- fractional work is accumulated per infused item/effect as needed;
- once accumulated work reaches `work_per_damage`, ordinary item durability damage is applied;
- durability damage caused by infusion wear follows ordinary break semantics and **can destroy the armor item**;
- no one-durability floor exists;
- no separate dormant state exists;
- normal armor damage and alchemical wear share the same durability pool;
- vanilla repair mechanics remain vanilla.

## 10. Multi-effect humanoid armor / Turtle Master

The current humanoid resolver rejects multi-effect potions while BODY armor can store bundles. Full potion support requires a deliberate humanoid multi-effect design.

For a multi-effect potion such as Turtle Master:

- compatible effects on the same armor item must retain independent causal wear accounting;
- Slowness contributes only when self-propelled locomotion is actually slowed;
- Resistance contributes only damage actually prevented;
- either/both may contribute in the same interval;
- no charge occurs merely because the effects are active.

The representation must remain atomic under infusion/reinfusion and must not leave both incompatible single/bundle components behind.

## 11. VanillaPlus coverage and optional integrations

The compatibility fixture must enumerate the actual loaded potion registry and verify that every infusion-capable effect is explicitly classified as:

- a concrete wear rule;
- `wear: none`; or
- instantaneous/self-consuming semantics where additional durability wear is intentionally unnecessary.

### 11.1 Alex's Mobs Continued

Alchemical Leather will provide optional built-in support for every brewable Alex's Mobs Continued potion used by the VanillaPlus version, with an explicit humanoid armor slot for each effect.

Known potion families to verify against the exact loaded 26.2 build include:

- Knockback Resistance;
- Lava Vision;
- Speed III (vanilla Speed effect, therefore reuses vanilla policy/slot);
- Poison Resistance;
- Bug Pheromones;
- Soulsteal;
- Clinging.

Do not infer support from the historical upstream list alone; validation uses the exact compatibility fixture.

### 11.2 Scale Brews

Scale Brews owns its Alchemical Leather compatibility resources after migration:

- Growth slot declaration;
- Shrinking slot declaration;
- explicit wear classification (`none` initially for both unless later evidence changes it);
- docs explaining optional Alchemical Leather interoperability.

### 11.3 Clinging Reoriented

Clinging Reoriented owns its Alchemical Leather compatibility resources after migration:

- Reorientation slot declaration;
- Clinging semantic event publication where appropriate without stealing Alex's Mobs' effect ownership;
- Reorientation semantic event publication;
- optional dependency/linkage-safe adapter;
- docs explaining wear semantics and optional integration.

## 12. Initial causal detector policy

The implementation/review must explicitly classify at least these current VanillaPlus-relevant effects:

| Effect | Initial causal work policy |
|---|---|
| Speed / Slowness | self-propelled locomotion actually modified by movement speed; no Elytra/rocket/transport |
| Jump Boost | successful wearer jump whose jump power was increased |
| Strength / Weakness | effective successful attack damage contribution/suppression |
| Regeneration / Poison | HP actually healed/damaged by that effect's own tick |
| Fire Resistance | fire/lava damage actually prevented |
| Resistance | damage actually prevented |
| Water Breathing | breath loss/drowning progression actually prevented |
| Slow Falling | ticks/physics where Slow Falling changes fall/fall-flying behavior, including applicable Elytra cases |
| Night Vision / Blindness | `none` initially |
| Invisibility | `none` initially |
| Instant Health / Instant Damage | self-consuming instant infusion; no additional wear |
| Infested | successful silverfish proc |
| Oozing | successful slime death proc |
| Wind Charged | successful wind burst death proc |
| Weaving | attributable cobweb movement benefit and/or successful death web proc, to be verified against 26.2 mechanics |
| Clinging | successful gravity link only; provisional 4 units |
| Reorientation | controlled self-flight time + successful turns; 30 units/damage, 1 unit/s + 2/turn |
| Growth / Shrinking | `none` |
| Reach / Reach Boost | successful interaction/attack possible only because of extra reach |
| Knockback Resistance | knockback impulse actually prevented/reduced |
| Poison Resistance | poison actually removed/rejected because of the effect |
| Soulsteal | HP actually restored by successful Soulsteal proc |
| Bug Pheromones | `none` initially |
| Lava Vision | `none` |
| Scorching | successful attributable ignition/fire contribution |

Exact balance constants other than the frozen Clinging/Reorientation provisional values remain subject to TM playability review.

## 13. Adversarial model and reserved holdouts

Implementation must be attacked for at least:

- charging Speed/Slowness while riding, fall-flying, using rockets, on moving Living Platforms, flying machines or under knockback;
- charging support displacement rather than only relative self locomotion;
- charging Jump Boost for vehicle jumps or external launches;
- charging Regeneration for coincident food/beacon/foreign healing;
- charging Poison for unrelated simultaneous damage;
- charging Fire Resistance/Resistance where another immunity/cancellation already prevented the damage;
- charging Reach for interactions already inside baseline range;
- charging Clinging/Reorientation for failed, blocked or unchanged requests;
- charging continuous Reorientation for passive mount/platform transport;
- failing to charge a successful Reorientation mount turn;
- Slow Falling incorrectly excluding Elytra or charging rocket impulse itself;
- external stronger/equal effects eclipsing the armor while the armor still pays;
- malformed/unknown JSON rules causing crashes or silent unsafe behavior;
- duplicate slot/wear ownership after first-party compatibility migration;
- optional first-party mods failing to launch without Alchemical Leather;
- Alchemical Leather failing standalone without compatibility mods;
- armor wear stopping at one durability instead of breaking normally;
- multi-effect reinfusion leaving stale components or double-charging incorrectly;
- VanillaPlus fixture discovering an unclassified potion effect.

## 14. TM implementation phases

### S00 — specification / ownership migration

- freeze this document;
- audit exact potion/effect registries used by VanillaPlus;
- create coordinated first-party working branches;
- migrate first-party slot ownership resources to their owning mods;
- complete Alex's Mobs slot coverage in Alchemical Leather.

### S01 — wear data model / API

- wear-rule resource loader;
- public semantic event API;
- source/equipped-item validation;
- fractional accumulation and ordinary durability break behavior;
- config flag and parser tests.

### S02 — builtin causal detectors

- movement attribution;
- jump/fall/Slow Falling;
- healing/damage/protection;
- combat attribute effects;
- breath/reach/other generic mechanics.

### S03 — companion adapters

- Clinging/Reorientation semantic events and resources;
- Scale Brews resources;
- optional compatibility linkage validation.

### S04 — multi-effect humanoid support

- atomic multi-effect storage/projection for compatible humanoid armor;
- Turtle Master and equivalent coverage;
- independent wear attribution per effect.

### S05 — exhaustive compatibility / adversarial gate

- standalone Alchemical Leather matrix;
- real VanillaPlus compatibility profile;
- automatic potion/effect classification audit;
- reserved causal holdouts;
- client/server regression matrix;
- documentation update only after behavior converges.

## 15. Acceptance condition

This sprint is not complete until:

- every required standalone test passes;
- compatibility fixtures pass with and without optional mods;
- the exact VanillaPlus potion registry has no unexplained infusion/wear gaps;
- first-party mods own their own slot/wear semantics without hard Alchemical Leather dependency;
- Alex's Mobs potion coverage is complete for the pack version;
- adversarial review completes a full pass without finding a production change;
- README/GUIDE/architecture/validation/compatibility docs in every touched repository reflect the actual final behavior.
