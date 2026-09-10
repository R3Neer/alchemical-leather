package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;

public final class BedrockifyOwnershipTests {
    private InteractionResult use(GameTestHelper h,net.minecraft.world.entity.player.Player p,BlockPos pos,ItemStack stack){
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        return CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    private Object fillBedrockPotion(GameTestHelper h,BlockPos pos,int level) throws Exception {
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("bedrockify:potion_cauldron"));var state=block.defaultBlockState();var property=BedrockifyBridge.property(state);
        state=state.setValue(property,level);h.getLevel().setBlockAndUpdate(pos,state);var be=h.getLevel().getBlockEntity(pos);
        be.getClass().getMethod("setPotion",ItemStack.class).invoke(be,PotionContents.createItemStack(Items.LINGERING_POTION,Potions.SWIFTNESS));return be;
    }

    @GameTest public void armorConsumesDoseWithoutReplacingBedrockifyPotionBlock(GameTestHelper h) throws Exception {
        if(!BedrockifyBridge.cauldronsActive()){h.succeed();return;}
        var pos=h.absolutePos(new BlockPos(1,1,1));fillBedrockPotion(h,pos,5);var p=h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(use(h,p,pos,new ItemStack(Items.LEATHER_LEGGINGS))==InteractionResult.SUCCESS,"Alchemical armor can consume from BedrockIfy potion cauldron");
        var state=h.getLevel().getBlockState(pos);h.assertTrue(BedrockifyBridge.potion(state),"Armor infusion preserves BedrockIfy's own potion-cauldron block");h.assertTrue(state.getValue(BedrockifyBridge.property(state))==2,"Exactly one canonical BedrockIfy potion dose is consumed");
        h.assertTrue(p.getMainHandItem().has(Infusions.TYPE),"Armor receives the imported infusion");h.succeed();
    }

    @GameTest public void ordinaryPotionBottleIsLeftEntirelyToBedrockify(GameTestHelper h) throws Exception {
        if(!BedrockifyBridge.cauldronsActive()){h.succeed();return;}
        var pos=h.absolutePos(new BlockPos(1,1,1));fillBedrockPotion(h,pos,2);var before=h.getLevel().getBlockState(pos);var p=h.makeMockPlayer(GameType.SURVIVAL);var potion=PotionContents.createItemStack(Items.LINGERING_POTION,Potions.SWIFTNESS);p.setShiftKeyDown(true);
        h.assertTrue(use(h,p,pos,potion)==InteractionResult.PASS,"Alchemical Leather yields BedrockIfy bottle lifecycle to BedrockIfy");
        h.assertTrue(h.getLevel().getBlockState(pos).equals(before),"Yielding does not replace or mutate BedrockIfy's block");h.assertTrue(ItemStack.matches(p.getMainHandItem(),potion),"Yielding does not consume or replace the potion stack");h.succeed();
    }
}
