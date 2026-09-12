package io.github.r3neer.alchemicalleather.cauldron;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Environment-neutral bridge used by synchronized cauldron block entities to
 * request a terrain remesh after client-side render data changes.
 */
public final class CauldronRenderInvalidation {
    private static Consumer<BlockPos> clientInvalidator = pos -> {};

    private CauldronRenderInvalidation() {}

    public static void registerClientInvalidator(Consumer<BlockPos> invalidator) {
        clientInvalidator = Objects.requireNonNull(invalidator);
    }

    public static void afterClientDataLoad(Level level, BlockPos pos) {
        if(level != null && level.isClientSide()) clientInvalidator.accept(pos);
    }
}
