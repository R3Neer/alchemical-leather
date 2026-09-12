package io.github.r3neer.alchemicalleather.test;
import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.data.Infusions;
import java.util.Optional;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.alchemy.PotionContents;
public final class ClientChecks implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            var potionPos=new BlockPos(2,-60,4);var dyedPos=new BlockPos(-2,-60,4);var lowPotionPos=new BlockPos(0,-60,4);
            world.getServer().runCommand("fill -4 -61 -2 4 -61 7 minecraft:stone");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("setblock -2 -60 4 alchemical_leather:dyed_water_cauldron[level=6]{color:6636321}");
            world.getServer().runCommand("setblock 0 -60 4 alchemical_leather:potion_cauldron[level=1]{contents:{potion:\"minecraft:swiftness\",custom_color:1193046},bottle:\"minecraft:potion\"}");
            world.getServer().runCommand("setblock 2 -60 4 alchemical_leather:potion_cauldron[level=3]{contents:{potion:\"minecraft:strength\",custom_color:16733440},bottle:\"minecraft:lingering_potion\"}");
            world.getServer().runCommand("tp @a 0 -60 0 0 20");
            context.waitTicks(30);world.getConnection().waitForChunksRender();
            assertCauldronColors(context,potionPos,16733440,dyedPos,6636321,"initial visible cauldrons");
            context.runOnClient(client->{
                var be=client.level.getBlockEntity(lowPotionPos);
                if(!(be instanceof PotionCauldronEntity cauldron)||cauldron.contents.getColor()!=1193046)throw new AssertionError("Low potion cauldron initial color not synchronized");
            });
            context.takeScreenshot("alchemical-leather-cauldron-colors-before-live-tint");

            recolorOnServer(world,potionPos,0x22cc88,dyedPos,0xcc3344);
            context.waitTicks(10);world.getConnection().waitForChunksRender();
            assertCauldronColors(context,potionPos,0x22cc88,dyedPos,0xcc3344,"first live recolor");
            context.takeScreenshot("alchemical-leather-cauldron-colors-after-live-tint-1");

            recolorOnServer(world,potionPos,0x6633cc,dyedPos,0x33aaff);
            context.waitTicks(10);world.getConnection().waitForChunksRender();
            assertCauldronColors(context,potionPos,0x6633cc,dyedPos,0x33aaff,"second live recolor");
            context.takeScreenshot("alchemical-leather-cauldron-colors-after-live-tint-2");

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

    private static void recolorOnServer(net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world,BlockPos potionPos,int potionColor,BlockPos dyedPos,int dyedColor) {
        world.getServer().runOnServer(server->{
            var level=server.overworld();
            var potionBe=level.getBlockEntity(potionPos);
            if(!(potionBe instanceof PotionCauldronEntity potion))throw new AssertionError("Missing potion cauldron on server");
            var contents=potion.contents;
            potion.fill(new PotionContents(contents.potion(),Optional.of(potionColor),contents.customEffects(),contents.customName()),potion.bottle);
            var dyedBe=level.getBlockEntity(dyedPos);
            if(!(dyedBe instanceof DyedWaterCauldronEntity dyed))throw new AssertionError("Missing dyed-water cauldron on server");
            dyed.setColor(dyedColor);
        });
    }

    private static void assertCauldronColors(ClientGameTestContext context,BlockPos potionPos,int expectedPotion,BlockPos dyedPos,int expectedDyed,String phase) {
        context.runOnClient(client->{
            var potionBe=client.level.getBlockEntity(potionPos);
            if(!(potionBe instanceof PotionCauldronEntity cauldron)||cauldron.contents.getColor()!=expectedPotion)throw new AssertionError("Potion block entity color not synchronized during "+phase);
            var dyedBe=client.level.getBlockEntity(dyedPos);
            if(!(dyedBe instanceof DyedWaterCauldronEntity dyed)||dyed.color!=expectedDyed)throw new AssertionError("Dyed-water block entity color not synchronized during "+phase);
            var getter=(FabricBlockGetter)client.level;
            if(!(getter.getBlockEntityRenderData(potionPos) instanceof Integer potionColor)||potionColor!=expectedPotion)throw new AssertionError("Potion cauldron render-data color not synchronized during "+phase);
            if(!(getter.getBlockEntityRenderData(dyedPos) instanceof Integer dyedColor)||dyedColor!=expectedDyed)throw new AssertionError("Dyed-water render-data color not synchronized during "+phase);
        });
    }
}
