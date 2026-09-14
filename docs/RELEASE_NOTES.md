# Alchemical Leather 0.1.0-beta.1

First public beta for Minecraft 26.2 Fabric. This release closes the two remaining core infusion routes before promoting the project out of alpha.

## Highlights

- **Standalone tipped arrows:** plain arrows can now be dipped directly into Alchemical Leather potion cauldrons. BedrockIfy is not required.
- **Bedrock-style capacity:** one stored dose tips up to 16 arrows, two doses up to 32, and three doses up to 64. Partial stacks consume the corresponding whole Alchemical Leather doses.
- **Exact potion data:** tipped arrows preserve the stored `PotionContents`, including custom effects, tint and custom potion name data carried by the component.
- **Optional arrow interaction:** `config/alchemical-leather.json` contains `"cauldronTippedArrows": true` by default. Setting it to `false` disables only Alchemical Leather's own arrow-cauldron interaction and yields the gesture with `PASS`.
- **Crafting-table infusion:** one compatible armor item plus one normal, splash or lingering potion forms a shapeless special recipe. The result follows the same slot, BODY, duration and enchantment rules as cauldron infusion.
- **Bottle semantics stay meaningful:** normal and splash potions create timed non-instant infusions; lingering potions create stable non-instant infusions; instant effects remain instant.
- **Component-safe crafting:** the recipe copies the original armor, preserves unrelated item components, transfers the potion color, returns a glass bottle and creates exactly one infused armor item.
- **Atomic rejection:** enchanted armor, incompatible armor, effectless potions, malformed potion items, invalid humanoid slots, humanoid multi-effect potions and ambiguous/extra ingredients do not match the recipe.
- **BedrockIfy ownership preserved:** Alchemical Leather does not intercept arrows on `bedrockify:potion_cauldron`, does not patch BedrockIfy's dye-recipe policy and does not directly link BedrockIfy production classes.

## Validation

The TM implementation candidate passed:

- 88/88 required standalone server GameTests;
- GameTest registration consistency checks;
- the client GameTest under Xvfb / llvmpipe;
- 88/88 required GameTests with the real Clinging Reoriented + Scale Brews + BedrockIfy + Alex's Mobs compatibility fixture.

The final tagged beta is published only after the exact release candidate passes the same complete CI pipeline on `main`.
