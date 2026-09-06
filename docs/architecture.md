# Implementation notes

- `Infusion` uses a persistent identifier rather than a strict holder so removing an optional effect provider does not destroy item decoding. Runtime resolution is checked before activation. Timed, stable and instant modes have validated codec invariants.
- `EffectLedger` keeps an external vanilla effect chain separately from the armor clock. `new MobEffectInstance(copy)` does **not** copy hidden effects in 26.2; the ledger recursively copies those explicitly. Only the visible projection executes ticks. Hidden external chains advance duration without applying a second effect.
- Incoming accepted `addEffect` and `forceAddEffect` operations are captured after the vanilla/Fabric eligibility check. Actual additions retain vanilla behavior; projection reconciliation does not call addEffect every tick. Visible attributes update on amplifier changes; timing/visual flags synchronize when needed.
- Saves replace projected effect entries with the real external state. The armor component is saved on the item; loading clears session bookkeeping before reconstructing from equipment. Equipment checks are limited to four slots of known participants; no global entity/inventory scans.
- `UseBlockCallback` validates the transaction before mutation and prevents fallback to the throwable item or BedrockIfy evaporation behavior. Own potion cauldrons preserve full PotionContents and exact bottle type.
- The BedrockIfy adapter uses only its public getters through reflection, avoiding optional runtime class linkage. It imports only levels 2/5/8 and preserves the currently reported tint. No global conversion runs.
- Enchanting uses Fabric’s event and a vanilla table guard. Anvil/crafting/grindstone guards prevent ambiguous repair transfers. Survival tools that bypass these hooks need separate adapters; administrative component edits are intentionally outside balancing.
- Client tint uses Fabric 26.2 BlockTintsFactory. Block models reference vanilla water-cauldron geometry. No item or final raster assets are introduced.

Deliberate boundaries: no arrow tipping in converted cauldrons, no washing via milk, no guarantee against third-party direct mutation of activeEffects, no anti-duplication restrictions on Creative copies, and no crash-atomic disk transaction beyond Minecraft’s normal save semantics.
