package io.github.r3neer.alchemicalleather.test;

import com.mojang.authlib.GameProfile;
import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import io.github.r3neer.alchemicalleather.effect.EquipmentInfusions;
import java.util.Arrays;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Regression coverage for equipment-effect projection during ServerPlayer construction/loading. */
public final class ConnectionLifecycleRegressionTests {
    @GameTest
    public void equipmentSyncBeforeServerPlayerConnectionIsReadyMustNotSendPacket(GameTestHelper h) {
        // A directly constructed ServerPlayer has a valid server level and inventory but no packet listener yet.
        // That is the relevant state while an integrated-server player is still being loaded/configured.
        var player = new ServerPlayer(
            h.getLevel().getServer(),
            h.getLevel(),
            new GameProfile(UUID.randomUUID(), "alch-preconnect"),
            ClientInformation.createDefault()
        );
        var leggings = new ItemStack(Items.LEATHER_LEGGINGS);
        leggings.set(Infusions.TYPE, new Infusion(Identifier.parse("minecraft:speed"), 0, "timed", 200));

        try {
            player.setItemSlot(EquipmentSlot.LEGS, leggings);
            EquipmentInfusions.sync(player);
        } catch (NullPointerException e) {
            boolean serverPlayerEffectPacket = Arrays.stream(e.getStackTrace()).anyMatch(frame ->
                frame.getClassName().equals("net.minecraft.server.level.ServerPlayer")
                    && frame.getMethodName().equals("onEffectAdded"));
            if (!serverPlayerEffectPacket) throw e;
            throw new AssertionError(
                "ALCHEMICAL_LEATHER_PRE_CONNECTION_PROJECTION_BUG: EquipmentInfusions.sync() projected an armor effect " +
                "while ServerPlayer.connection was null. Observed path: EquipmentInfusions.sync -> EffectLedger.setArmor -> " +
                "EffectLedger.project -> LivingEffectsAccess.alchemical$added -> ServerPlayer.onEffectAdded -> connection.send. " +
                "This is the lifecycle failure surfaced to players as 'Invalid player data' during player NBT load. " +
                "Make pre-connection equipment sync safe and ensure the effect is reconciled once the player is ready.",
                e
            );
        }

        h.succeed();
    }
}
