# Compatibility and infusion-wear protocol

This document describes the optional potion-effect compatibility and causal infusion-wear protocol shipped with **Alchemical Leather 0.1.0-beta.2**.

## Ownership model

Alchemical Leather owns the generic protocol:

- infusion storage and projection;
- arbitration between armor-owned and external effect sources;
- effect-slot and wear-rule resource loading;
- builtin causal detectors for generic Minecraft mechanics;
- attribution of semantic events to the actually equipped infused item;
- fractional work accumulation and ordinary item durability damage.

A companion mod should own the semantics of effects that only that mod understands. The companion reports a successful semantic action; it does **not** choose an armor item, mutate durability or copy Alchemical Leather's balance values.

First-party ownership is deliberately split:

- **Clinging: Reoriented** owns Reorientation's boots slot and publishes successful gravity-turn / controlled-flight facts. It also publishes the successful-turn fact used by Alex's Mobs Clinging without redefining Alex's Mobs registry ownership.
- **Scale Brews** owns Growth/Shrinking slot declarations and explicitly classifies both as `wear: none`.
- **Alchemical Leather** bundles data-only compatibility and generic causal detectors for selected third-party effects used by VanillaPlus, including Alex's Mobs Continued, Friends & Foes, Wilder Wild and Deeper Dark where appropriate.

Optional mods remain optional. Alchemical Leather works standalone, and first-party companions work without Alchemical Leather.

## Effect-slot resources

Humanoid effect slots are declared as:

`data/<effect-namespace>/alchemical_leather/effect_slots/<effect-path>.json`

Example:

```json
{
  "slot": "boots",
  "requires_effect": "clinging_reoriented:reorientation"
}
```

Supported humanoid slots are `helmet`, `chestplate`, `leggings` and `boots`. BODY/animal armor does not use this humanoid mapping.

Resources may use the existing optional guards such as `requires_mod`, `requires_effect`, `requires_resource` and `enabled`. A missing or inactive optional resource does not create a hard dependency.

## Wear-rule resources

Wear rules are independent from slot rules:

`data/<effect-namespace>/alchemical_leather/wear_rules/<effect-path>.json`

An absent wear rule means **unclassified**, not zero wear. The VanillaPlus gate fails if a loaded potion effect silently falls through this classification.

### Persistent state with no operating wear

```json
{
  "wear": "none",
  "requires_effect": "scalebrews:growth"
}
```

`wear: none` is explicit policy. It is appropriate when an effect represents a persistent body/visual state rather than measurable mechanical work. Growth and Shrinking use this policy; simply remaining large or small does not turn armor into a disguised timer.

### Builtin causal detector

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

JSON selects a known detector and its economy. It is intentionally **not** a scripting language: arbitrary Java calls, entity predicates or free-form boolean expressions do not belong in data files.

### Semantic event

```json
{
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
  ],
  "requires_effect": "clinging_reoriented:reorientation"
}
```

## Public semantic API

Companion mods that know when their own effect performed real work may call:

```java
InfusionWearApi.emit(
    LivingEntity wearer,
    Holder<MobEffect> effect,
    Identifier event,
    double amount
);
```

The call reports a fact. Alchemical Leather then verifies that:

1. infusion wear is enabled;
2. the reported effect is actually present on an equipped infusion;
3. that exact equipped slot owns the projected armor source;
4. the armor source is effective rather than eclipsed by a stronger/equal external source;
5. the rule explicitly accepts the reported event;
6. the work amount is finite and positive.

Only after those checks is work accumulated on the owning `ItemStack`.

## Durability semantics

Fractional work is stored on the infused item per effect. When an effect's accumulated work reaches its `work_per_damage` threshold, Alchemical Leather applies ordinary Minecraft item damage to that armor stack.

- alchemical wear and ordinary armor damage share the same durability pool;
- armor can break normally at zero durability;
- there is no one-durability floor or dormant infused state;
- Creative/infinite-material players neither take alchemical durability damage nor bank hidden debt for later;
- `UNBREAKABLE` items are not damaged;
- reinfusion replaces the infusion atomically and clears obsolete wear debt;
- vanilla repair mechanics remain vanilla.

Stable/lingering means the infusion itself does not expire with time. It does **not** imply free operation: a stable effect with a causal wear rule still pays when it performs work.

## First-party companion semantics

### Clinging

Clinging is discrete. Only a successful voluntary gravity turn counts. Failed, blocked, unchanged, grounded-mount and passive-transport states do not. Its current rule uses 4 work per successful turn and 30 work per durability point.

### Reorientation

Reorientation combines discrete and sustained work:

- 2 work per successful voluntary gravity turn;
- 0.05 work per controlled-flight tick;
- 30 work per durability point.

Continuous work is suppressed when locomotion belongs to a passenger/vehicle, moving support, Anatomy support, non-empty fluid context, Elytra or independent player flight. A successful Reorientation-requested airborne mount turn still publishes the discrete turn event.

The companion integration is linkage-safe and adds no hard Alchemical Leather dependency.

### Scale Brews

Growth and Shrinking are chestplate effects and explicitly use `wear: none`. Scale Brews owns those resources in its own JAR.

## Generic third-party mechanics

Some third-party effects use generic Minecraft mechanics that Alchemical Leather can observe causally without asking the owning mod to publish an event.

**Knockback Resistance** is the important beta.2 example. The final audit inventories the direct attribute consumers that can reduce knockback on the infused wearer rather than assuming every mechanic funnels through `LivingEntity#knockback`.

For Minecraft 26.2 the validated vanilla target routes include ordinary living-entity knockback plus Hoglin, Warden Sonic Boom, Mace, Iron Golem and Arrow knockback. For Alex's Mobs Continued 2.1.9 the validated target routes include Guster, Bison, Tusklin and Rhinoceros. Optional Alex adapters are linkage-safe and do not make Alex's Mobs a runtime dependency.

The detector reports only the impulse magnitude actually suppressed by the effective armor-owned effect. Routes that read Knockback Resistance from the **attacking mob itself** are intentionally outside this target-armor accounting.

## VanillaPlus compatibility gate

CI builds a focused compatibility profile using the exact potion/effect contributors from `R3Neer/VanillaPlus-26.2` plus BedrockIfy for cauldron ownership:

- Alex's Mobs Continued 2.1.9;
- Clinging: Reoriented at commit `df1cff3a2fb9baf69d3bb8594159681b6966096d`;
- Scale Brews at commit `74066349eb33872d1f2b8584dfd05ffd96eae0f8`;
- Friends & Foes 4.0.27+mc26.2;
- Wilder Wild 4.2.11-mc26.2;
- Deeper Dark 4.4.1;
- BedrockIfy 1.11.8+mc26.2;
- the runtime libraries required by those exact pack entries.

The first-party companions are fetched by exact commit SHA and built as part of CI. The third-party JARs are fetched from the exact packwiz-pinned Modrinth CDN URLs.

The registry-driven gate asserts that every loaded potion effect has an explicit wear classification. Additional sentinels pin the intended slot/wear policy for Growth, Shrinking, Reorientation, Clinging, Friends & Foes Reach, Wilder Wild Reach Boost/Scorching and Deeper Dark's Blindness potion path.

## Boundaries

Alchemical Leather does not promise arbitrary compatibility with every mod or datapack. The protocol is designed so new effects can declare a slot and wear policy without patching core code, but causal semantics still need a trustworthy detector or a semantic event from the owning mod.

See [validation.md](validation.md) for the exact tested matrix and [architecture.md](architecture.md) for implementation details.
