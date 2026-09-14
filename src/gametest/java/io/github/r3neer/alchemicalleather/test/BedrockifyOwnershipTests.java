package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
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
    private void fillBedrockPotion(GameTestHelper h,BlockPos pos,int level) throws Exception {fillBedrockPotion(h,pos,level,PotionContents.createItemStack(Items.LINGERING_POTION,Potions.SWIFTNESS));}
    private void fillBedrockPotion(GameTestHelper h,BlockPos pos,int level,ItemStack potion) throws Exception {
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("bedrockify:potion_cauldron"));var state=block.defaultBlockState();var property=BedrockifyBridge.property(state);
        state=state.setValue(property,level);h.getLevel().setBlockAndUpdate(pos,state);var be=h.getLevel().getBlockEntity(pos);
        be.getClass().getMethod("setPotion",ItemStack.class).invoke(be,potion);
    }

    @GameTest public void armorConsumesDoseWithoutReplacingBedrockifyPotionBlock(GameTestHelper h) throws Exception {
        if(!BedrockifyBridge.cauldronsActive()){h.succeed();return;}
        var pos=h.absolutePos(new BlockPos(1,1,1));fillBedrockPotion(h,pos,5);var p=h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(use(h,p,pos,new ItemStack(Items.LEATHER_LEGGINGS))==InteractionResult.SUCCESS,"Alchemical armor can consume from BedrockIfy potion cauldron");
        var state=h.getLevel().getBlockState(pos);h.assertTrue(BedrockifyBridge.potion(state),"Armor infusion preserves BedrockIfy's own potion-cauldron block");h.assertTrue(state.getValue(BedrockifyBridge.property(state))==2,"Exactly one canonical BedrockIfy potion dose is consumed");
        h.assertTrue(p.getMainHandItem().has(Infusions.TYPE),"Armor receives the imported infusion");h.succeed();
    }

    @GameTest public void effectlessImportedPotionDyesArmorWithoutCreatingInfusion(GameTestHelper h) throws Exception {
        if(!BedrockifyBridge.cauldronsActive()){h.succeed();return;}
        var pos=h.absolutePos(new BlockPos(1,1,1));var potion=PotionContents.createItemStack(Items.LINGERING_POTION,Potions.AWKWARD);fillBedrockPotion(h,pos,5,potion);var p=h.makeMockPlayer(GameType.SURVIVAL);
        var before=BedrockifyBridge.read(h.getLevel(),pos,h.getLevel().getBlockState(pos));int expected=before.contents().getColor()&0xffffff;
        h.assertTrue(use(h,p,pos,new ItemStack(Items.LEATHER_LEGGINGS))==InteractionResult.SUCCESS,"Imported effectless BedrockIfy potion acts as a dye bath");var armor=p.getMainHandItem();
        h.assertTrue(armor.has(DataComponents.DYED_COLOR)&&armor.get(DataComponents.DYED_COLOR).rgb()==expected,"BedrockIfy effectless potion transfers its visible color");
        h.assertFalse(Infusions.blocked(armor),"BedrockIfy effectless potion creates no infusion");var state=h.getLevel().getBlockState(pos);
        h.assertTrue(BedrockifyBridge.potion(state)&&state.getValue(BedrockifyBridge.property(state))==2,"Dye-only use preserves BedrockIfy block ownership and consumes one canonical dose");h.succeed();
    }

    @GameTest public void ordinaryPotionBottleIsLeftEntirelyToBedrockify(GameTestHelper h) throws Exception {
        if(!BedrockifyBridge.cauldronsActive()){h.succeed();return;}
        var pos=h.absolutePos(new BlockPos(1,1,1));fillBedrockPotion(h,pos,2);var beforeState=h.getLevel().getBlockState(pos);var p=h.makeMockPlayer(GameType.SURVIVAL);var potion=PotionContents.createItemStack(Items.LINGERING_POTION,Potions.SWIFTNESS);var beforeStack=potion.copy();
        h.assertTrue(use(h,p,pos,potion)==InteractionResult.PASS,"Alchemical Leather yields BedrockIfy bottle lifecycle to BedrockIfy without requiring sneak");
        h.assertTrue(h.getLevel().getBlockState(pos).equals(beforeState),"Yielding does not replace or mutate BedrockIfy's block");h.assertTrue(ItemStack.matches(p.getMainHandItem(),beforeStack),"Yielding does not consume or replace the potion stack");h.succeed();
    }

    @GameTest public void arrowTippingIsLeftEntirelyToBedrockify(GameTestHelper h) throws Exception {
        if(!BedrockifyBridge.cauldronsActive()){h.succeed();return;}
        var pos=h.absolutePos(new BlockPos(1,1,1));fillBedrockPotion(h,pos,5);var beforeState=h.getLevel().getBlockState(pos);var p=h.makeMockPlayer(GameType.SURVIVAL);var arrows=new ItemStack(Items.ARROW,32);var beforeStack=arrows.copy();
        h.assertTrue(use(h,p,pos,arrows)==InteractionResult.PASS,"Alchemical Leather yields arrow tipping on BedrockIfy's potion cauldron");
        h.assertTrue(h.getLevel().getBlockState(pos).equals(beforeState),"Yielding arrows does not mutate BedrockIfy's potion state");h.assertTrue(ItemStack.matches(p.getMainHandItem(),beforeStack),"Yielding arrows does not consume or replace BedrockIfy's input stack");h.succeed();
    }

    @GameTest public void ordinaryDyeOnBedrockifyPotionCauldronIsDelegated(GameTestHelper h) throws Exception {
        if(!BedrockifyBridge.cauldronsActive()){h.succeed();return;}
        var pos=h.absolutePos(new BlockPos(1,1,1));fillBedrockPotion(h,pos,5);var beforeState=h.getLevel().getBlockState(pos);var p=h.makeMockPlayer(GameType.SURVIVAL);var dye=new ItemStack(Items.DYE.blue());var beforeStack=dye.copy();
        h.assertTrue(use(h,p,pos,dye)==InteractionResult.PASS,"Alchemical Leather does not claim dye interactions on BedrockIfy's potion cauldron");
        h.assertTrue(h.getLevel().getBlockState(pos).equals(beforeState),"Delegated dye leaves BedrockIfy's state untouched");h.assertTrue(ItemStack.matches(p.getMainHandItem(),beforeStack),"Delegated dye is not consumed by Alchemical Leather");h.succeed();
    }
}
