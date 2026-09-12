# TM — live cauldron tint render bug

Temporary working document. Delete before merge.

## Analysis

Observed behavior: dyeing a potion cauldron changes the logical `PotionContents` color and that color is copied correctly to dyed armor, but the already-rendered liquid keeps its previous tint until a later render rebuild.

Current data path:

1. `CauldronService` blends the dye into `PotionContents.customColor`.
2. `PotionCauldronEntity.fill(...)` stores the new contents, calls `setChanged()`, then `sendBlockUpdated(pos, state, state, 3)`.
3. `PotionCauldronEntity.getRenderData()` exposes `contents.getColor()`.
4. The client block-color factory reads `FabricBlockGetter#getBlockEntityRenderData(pos)` while chunk meshes are built.
5. The same pattern is used by `DyedWaterCauldronEntity`.

The key mismatch is temporal: block-entity sync and terrain remeshing are separate concerns. A live BE data packet can update the client-side render data without necessarily dirtying the already-built section after that data has been applied. Initial-load tests do not cover this because the custom color exists before the first mesh is built.

### Alternatives considered

- Encode the color or a revision bit in block state: forces rebuilds but pollutes block state with render-only state and multiplies model variants.
- Replace/re-place the cauldron block on every tint: excessive lifecycle churn and risks losing BE data.
- Add a custom S2C packet solely for render invalidation: robust but redundant because the existing BE update packet already arrives at the correct client.
- Invalidate the client section when the synchronized BE data is loaded: minimal, directly coupled to the data dependency that drives tinting, and reusable for both potion and dyed-water cauldrons.

## Architectural plan

Introduce a tiny environment-neutral render-invalidation bridge in common code. Client initialization registers an implementation that marks the containing render section dirty through `Minecraft.levelRenderer`. Both cauldron block entities call the bridge after loading synchronized data while on the client. Server behavior remains unchanged.

The bridge must be a no-op until the client initializer registers it, so dedicated servers never load client classes.

## Implementation plan

- [ ] Add a common `CauldronRenderInvalidation` bridge with a client registration hook.
- [ ] Register the client implementation from `AlchemicalLeatherClient`, marking the containing section dirty.
- [ ] Invoke invalidation after `PotionCauldronEntity.loadAdditional(...)` applies new contents.
- [ ] Invoke invalidation after `DyedWaterCauldronEntity.loadAdditional(...)` applies new color.
- [ ] Extend the client GameTest so a cauldron is rendered first, then recolored while the world remains loaded, and verify the new synchronized render-data value after the live update.
- [ ] Exercise a second recolor and dyed-water live recolor to cover repeated invalidation and the shared path.
- [ ] Run standalone server tests, client GameTest and the real optional-mod fixture.
- [ ] Perform adversarial review for client/server classloading, duplicate updates, no-op dye, chunk-load behavior and regression risk.
- [ ] Delete this temporary document before merge.

## Review 1

No architectural changes required. The fix should invalidate only on client-side data application, not from the server mutation method, so remeshing occurs after the new render data is actually present. The client implementation should use the public `LevelRenderer#setSectionDirty(sectionX, sectionY, sectionZ)` path rather than relying on an old/new block-state comparison with identical states.

## Review 2

Stable relative to Review 1. Proceed with implementation.
