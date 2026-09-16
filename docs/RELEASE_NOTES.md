# Alchemical Leather 0.1.0-beta.2

Second public beta for Minecraft 26.2 Fabric. Beta.2 turns potion-infused armor durability into a causal system: armor pays for mechanical work its own infusion actually performed, rather than simply losing durability because an effect icon happened to be active.

## Highlights

- **Causal infusion wear:** effects accumulate fractional work on the exact equipped infused item and convert complete work buckets into ordinary Minecraft durability damage. Armor can break normally; there is no hidden one-durability floor.
- **Source-aware billing:** an infusion does not pay while an equal/stronger external effect owns the mechanic. Creative/infinite-material players do not accumulate hidden wear debt.
- **Same-slot humanoid multi-effect potions:** distinct effects may share one humanoid infusion when every effect maps to the same armor slot. Turtle Master-style bundles keep independent timing and wear attribution.
- **Data-driven wear policy:** each loaded potion effect must be explicitly classified by a wear rule, `wear: none`, or intentional instantaneous semantics. Unknown/malformed detector data is rejected rather than silently accepted.
- **Optional compatibility API:** companion mods can publish successful semantic work through `InfusionWearApi` without selecting armor or applying durability themselves.
- **First-party ownership:** Clinging: Reoriented owns Reorientation/Clinging semantic gravity work; Scale Brews owns Growth/Shrinking slot resources and explicit `wear: none` policy. Neither companion gains a hard Alchemical Leather dependency.
- **Broader causal coverage:** movement, jump/fall, healing/damage/protection, combat, breath, reach and selected third-party procs are billed at their real decision/consumption sites.
- **Final Knockback Resistance audit:** direct target-side consumers in Minecraft 26.2 and Alex's Mobs Continued 2.1.9 are covered instead of assuming every impulse funnels through one knockback method. Vanilla coverage includes ordinary living knockback, Hoglin, Sonic Boom, Mace, Iron Golem and Arrow paths; Alex coverage includes Guster, Bison, Tusklin and Rhinoceros.
- **Cooldown-safe combat/protection:** Strength, Weakness, Fire Resistance and Jump Boost defensive work no longer bills work that vanilla ultimately rejects through damage cooldowns.
- **Extra semantic fixes:** successful Weakness-assisted zombie-villager curing, Water Breathing air recovery, broader reach contexts and Slow Falling consumption sites have permanent holdouts.
- **Beta.1 features retained:** native tipped arrows, crafting infusion, effectless dye baths, BODY bundles, dyed water, BedrockIfy ownership, trades and rendering remain in the regression matrix.

## Validation

The final pre-release production candidate passed:

- **140/140** required standalone Alchemical Leather server GameTests;
- GameTest registration consistency checks;
- the client GameTest under Xvfb / llvmpipe;
- pinned Clinging: Reoriented build/tests at `df1cff3a2fb9baf69d3bb8594159681b6966096d` (**126/126** GameTests);
- pinned Scale Brews build/tests at `74066349eb33872d1f2b8584dfd05ffd96eae0f8` (**154/154** GameTests);
- **140/140** required GameTests with the exact VanillaPlus potion-contributor fixture, including Alex's Mobs Continued 2.1.9, Friends & Foes, Wilder Wild, Deeper Dark and BedrockIfy;
- registry-driven explicit wear classification and real cross-mod semantic/linkage holdouts.

The release workflow publishes this prerelease only after the exact beta.2 commit on `main` passes the same complete pipeline.

This is beta software; back up worlds before updating.
