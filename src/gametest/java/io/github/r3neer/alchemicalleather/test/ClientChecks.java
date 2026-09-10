package io.github.r3neer.alchemicalleather.test;
import io.github.r3neer.alchemicalleather.cauldron.PotionCauldronEntity;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
public final class ClientChecks implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            world.getServer().runCommand("fill -4 -61 -2 4 -61 7 minecraft:stone");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("setblock -2 -60 4 minecraft:cauldron");
            world.getServer().runCommand("setblock 0 -60 4 alchemical_leather:potion_cauldron[level=1]{contents:{potion:\"minecraft:swiftness\",custom_color:1193046},bottle:\"minecraft:potion\"}");
            world.getServer().runCommand("setblock 2 -60 4 alchemical_leather:potion_cauldron[level=3]{contents:{potion:\"minecraft:strength\",custom_color:16733440},bottle:\"minecraft:lingering_potion\"}");
            world.getServer().runCommand("tp @a 0 -60 0 0 20");
            context.waitTicks(30);world.getConnection().waitForChunksRender();
            context.runOnClient(client->{
                var be=client.level.getBlockEntity(new BlockPos(0,-60,4));
                if(!(be instanceof PotionCauldronEntity cauldron)||cauldron.contents.getColor()!=1193046)throw new AssertionError("Potion block entity and color not synchronized");
            });
            context.takeScreenshot("alchemical-leather-vanilla-cauldron");
            world.getServer().runCommand("item replace entity @a armor.legs with minecraft:leather_leggings[alchemical_leather:infusion={effect:\"minecraft:speed\",amplifier:1,mode:\"stable\"},minecraft:dyed_color=1193046]");
            context.waitTicks(30);
            context.runOnClient(client->{
                var stack=client.player.getItemBySlot(EquipmentSlot.LEGS);
                if(!stack.has(Infusions.TYPE)||!client.player.hasEffect(MobEffects.SPEED)||client.player.getEffect(MobEffects.SPEED).getAmplifier()!=1)throw new AssertionError("Vanilla leather infusion/effect not synchronized");
            });
            context.setScreen(()->new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
            context.waitTicks(5);context.takeScreenshot("alchemical-leather-inventory");context.setScreen(()->null);
            world.getServer().runCommand("item replace entity @a armor.legs with minecraft:air");context.waitTicks(10);
            context.runOnClient(client->{if(client.player.hasEffect(MobEffects.SPEED))throw new AssertionError("Effect remains on client after vanilla leather unequip");});
            world.getServer().runCommand("item replace entity @a armor.head with minecraft:iron_helmet[alchemical_leather:infusion={effect:\"minecraft:night_vision\",amplifier:0,mode:\"stable\"},minecraft:dyed_color=65280]");
            context.waitTicks(20);
            context.runOnClient(client->{
                var stack=client.player.getItemBySlot(EquipmentSlot.HEAD);
                if(!stack.has(Infusions.TYPE)||!stack.has(net.minecraft.core.component.DataComponents.DYED_COLOR)||!client.player.hasEffect(MobEffects.NIGHT_VISION))throw new AssertionError("Synthetic dyeable humanoid infusion/effect not synchronized");
            });
            world.getServer().runCommand("item replace entity @a armor.head with minecraft:air");context.waitTicks(10);
            context.runOnClient(client->{if(client.player.hasEffect(MobEffects.NIGHT_VISION))throw new AssertionError("Effect remains on client after generalized dyeable armor unequip");});
        }
    }
}
