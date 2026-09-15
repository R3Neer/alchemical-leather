# Alchemical Leather

What if **dyeable armor** could carry your potions, and pay for their useful work with the same durability that keeps ordinary armor honest?

A **Fabric mod for Minecraft 26.2** that turns familiar armor into alchemical equipment. Pour a potion into a cauldron, infuse a compatible piece, equip it, and take the effect with you.

Current published prerelease: **0.1.0-beta.1**. The current development line adds causal infusion wear, multi-effect humanoid potions and the optional compatibility protocol described below; no new release tag is implied by this documentation.

![Potion-filled cauldrons in game](docs/images/vanilla-cauldron.png)

## Start experimenting

Try a **Speed potion, an empty cauldron and unenchanted leather leggings**. Pour the potion, use the leggings on the cauldron, then equip them.

Once that works:

- Walk under your own power and watch an effect with a causal wear rule slowly consume the same durability pool as normal armor damage.
- Ride, get knocked back or let an external effect outrank the armor and compare what does **not** count as alchemical work.
- Try a multi-effect potion whose effects belong to the same humanoid slot, such as Turtle Master on leggings.
- Put the armor away for a while, then return to it.
- Try normal, splash and lingering bottles. Lingering/stable stops duration expiry; it does not make causal work free.
- Put plain arrows into an Alchemical Leather potion cauldron and make tipped arrows without needing BedrockIfy.
- Combine compatible armor with a potion in a crafting grid as an alternative infusion route.
- Try an **Awkward, Mundane or Thick potion** and use its cauldron as a pure color bath.
- Try armor that is dyeable but is not ordinary player leather armor.
- Add dye to a water cauldron and see what compatible armor does with it.
- See what an experienced **Leatherworker** is willing to sell.

Humanoid armor cares about which effects belong on which body part. A single-effect potion works as before; a multi-effect humanoid potion is accepted only when every distinct effect maps to the **same actual armor slot**, and those effects retain independent timing and wear accounting. Animal/BODY armor keeps the complete effect bundle from one potion and follows its own slot policy.

**Infusions and enchantments are mutually exclusive.** Normal, splash and lingering potions can infuse through a cauldron or the shapeless crafting recipe. Effectless potions can be stored as cauldron dye baths but do not create crafting-table infusions. Alchemical Leather potion cauldrons can tip plain arrows directly, using up to 16 arrows per stored dose and preserving the complete `PotionContents`. Water cauldrons wash compatible armor clean.

## Causal infusion wear

Infusion wear is enabled by default with `"infusionWear": true` in `config/alchemical-leather.json`.

The rule is deliberately stricter than “effect active = armor ticking down”. An infused item pays only when its effect performs **attributable mechanical work** and that armor source is actually effective.

Examples:

- Speed/Slowness charge self-propelled locomotion they actually modify, not a horse, platform, teleport, knockback or unrelated transport.
- Regeneration and Poison charge HP really changed by their own ticks.
- Fire Resistance and Resistance charge damage actually prevented by those effects.
- Reach charges only interactions that needed the extra reach.
- Slow Falling charges where its gravity branch actually changes the fall/fall-flying physics.
- Clinging charges successful voluntary gravity turns.
- Reorientation charges successful turns plus controlled self-flight, but not Elytra, fluid locomotion, passenger/support transport or independent player flight.
- Growth and Shrinking explicitly use `wear: none`; simply remaining large or small is not a durability timer.

Fractional work is stored on the actual infused item. Reaching a rule's threshold applies **ordinary item durability damage**. The armor may break normally at zero durability; there is no one-durability floor or dormant alchemical state. Creative/infinite-material players do not take alchemical damage and do not accumulate hidden debt.

A stronger or equal external source can eclipse the armor source. While eclipsed, the armor does not pay for work it did not provide.

Set `"infusionWear": false` to disable only this causal durability system while keeping infusion effects and their existing duration semantics.

## Optional-mod ownership

Alchemical Leather owns the generic protocol, not every other mod's internal mechanics.

- **Clinging: Reoriented** owns Reorientation's boots slot and publishes semantic events only after authoritative successful gravity work. The integration is optional and linkage-safe.
- **Scale Brews** owns Growth/Shrinking slot and wear resources; both are chestplate effects with explicit `wear: none`.
- Alchemical Leather ships selected data-only rules/detectors for third-party potion effects used by VanillaPlus where no hard runtime link is required.

Third-party mods can define slot/wear JSON and, when only they know that an action succeeded, report a semantic event through the small public `InfusionWearApi`. Alchemical Leather still decides which equipped item owns the effect, whether the armor is the effective source, how fractional work is accumulated and when durability changes.

See **[compatibility and API](docs/COMPATIBILITY.md)** for the resource formats and integration contract.

## Arrow tipping and BedrockIfy

Arrow tipping is enabled by default and is independent of BedrockIfy. To disable only Alchemical Leather's own arrow-on-cauldron interaction, set `"cauldronTippedArrows": false` in `config/alchemical-leather.json`.

When BedrockIfy is installed and its cauldrons are active, BedrockIfy remains the sole owner of arrows and ordinary interactions on its own potion-cauldron blocks. Alchemical Leather does not win compatibility by starting a mixin knife fight in the parking lot.

## Validation

The wear branch is validated standalone and against a focused **VanillaPlus potion-contributor fixture** using the exact pack-pinned versions of Alex's Mobs Continued, Friends & Foes, Wilder Wild, Deeper Dark and BedrockIfy, plus the TM-converged `main` commits of Clinging: Reoriented and Scale Brews.

The registry audit fails when any loaded potion effect lacks an explicit wear classification. A cross-mod holdout drives a real Clinging Reorientation turn event through the public API into the owning infused boots and checks fractional work plus terminal durability damage.

See [validation](docs/validation.md) and the [TM closeout](docs/TM_INFUSION_WEAR_CLOSEOUT.md) for the evidence and failure classifications.

## Install

Requires **Minecraft 26.2, Java 25, Fabric Loader 0.19.5+ and Fabric API 0.159.0+26.2 or newer for 26.2**.

Download a regular JAR from [releases](https://github.com/R3Neer/alchemical-leather/releases) and put it in `mods`. Install the mod and Fabric API on **both client and server**. No optional content mod is required.

This is beta software. Back up worlds before updating; tested integrations are evidence for those exact fixtures, not a blanket guarantee for every modpack assembled by a sufficiently adventurous primate.

## Go further

- [Player guide / wiki](docs/GUIDE.md) — complete mechanics, with spoilers.
- [Compatibility and infusion-wear API](docs/COMPATIBILITY.md) — slot/wear resources, semantic events and ownership boundaries.
- [Frozen TM design specification](docs/TM_INFUSION_WEAR_SPEC.md) · [TM implementation closeout](docs/TM_INFUSION_WEAR_CLOSEOUT.md)
- [Build and tests](docs/GUIDE.md#building) · [Architecture](docs/architecture.md) · [Validation](docs/validation.md) · [Issues](https://github.com/R3Neer/alchemical-leather/issues)

[GPL-3.0-or-later](LICENSE). Not an official Minecraft product; not approved by or associated with Mojang or Microsoft.
