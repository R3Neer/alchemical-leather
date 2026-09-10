package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.data.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

public final class NativeDyedWaterTests {
    private InteractionResult use(GameTestHelper h,net.minecraft.world.entity.player.Player p,BlockPos pos,ItemStack stack){
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        return CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    private int color(GameTestHelper h,BlockPos pos){return ((DyedWaterCauldronEntity)h.getLevel().getBlockEntity(pos)).color;}

    @GameTest public void vanillaWaterDyeConvertsEveryLevelWithoutBedrockOwner(GameTestHelper h){
        if(BedrockifyBridge.cauldronsActive()){h.succeed();return;}
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int red=Items.DYE.red().components().get(DataComponents.DYE).getTextureDiffuseColor()&0xffffff;
        for(int level=1;level<=3;level++){
            h.getLevel().setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,level));var dye=new ItemStack(Items.DYE.red(),2);
            h.assertTrue(use(h,p,pos,dye)==InteractionResult.SUCCESS,"Dye converts vanilla water level "+level);var state=h.getLevel().getBlockState(pos);
            h.assertTrue(state.is(DyedWaterCauldron.BLOCK)&&state.getValue(DyedWaterCauldron.LEVEL)==level*2,"Vanilla volume maps 1/2/3 to dyed 2/4/6");h.assertTrue(color(h,pos)==red,"Dyed water stores exact dye color");h.assertTrue(dye.getCount()==1,"Exactly one dye consumed");
        }h.succeed();
    }

    @GameTest public void repeatedAndMixedDyesHaveAtomicConsumption(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int red=Items.DYE.red().components().get(DataComponents.DYE).getTextureDiffuseColor()&0xffffff;int blue=Items.DYE.blue().components().get(DataComponents.DYE).getTextureDiffuseColor()&0xffffff;
        CauldronService.writeDyed(h.getLevel(),pos,red,4);var same=new ItemStack(Items.DYE.red(),2);h.assertTrue(use(h,p,pos,same)==InteractionResult.SUCCESS,"Same dye is handled");h.assertTrue(same.getCount()==2&&color(h,pos)==red,"No-op blend consumes no dye");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==4,"Dye never changes water amount");
        var mixed=new ItemStack(Items.DYE.blue(),2);int expected=DyeColors.blend(red,blue);h.assertTrue(use(h,p,pos,mixed)==InteractionResult.SUCCESS,"Different dye blends");h.assertTrue(mixed.getCount()==1&&color(h,pos)==expected,"Changed blend consumes exactly one dye");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==4,"Mixed dye preserves water amount");h.succeed();
    }

    @GameTest public void dyedWaterBlendsArmorAndDepletesBySingleUnits(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int water=0x345678,original=0x112233;CauldronService.writeDyed(h.getLevel(),pos,water,6);
        var armor=new ItemStack(Items.IRON_HELMET);var infusion=new Infusion(Identifier.parse("minecraft:night_vision"),0,"timed",200);armor.set(Infusions.TYPE,infusion);armor.set(DataComponents.DYED_COLOR,new DyedItemColor(original));
        h.assertTrue(use(h,p,pos,armor)==InteractionResult.SUCCESS,"Synthetic dyeable armor accepts native colored water");h.assertTrue(armor.get(DataComponents.DYED_COLOR).rgb()==DyeColors.blend(original,water),"Existing armor color blends with cauldron color");h.assertTrue(armor.get(Infusions.TYPE).equals(infusion),"Recolor preserves infusion");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==5,"Armor dye consumes one sixth-unit");
        for(int remaining=4;remaining>=1;remaining--){h.assertTrue(use(h,p,pos,armor)==InteractionResult.SUCCESS,"Repeated recolor consumes next sixth-unit");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==remaining,"Expected sixth-unit remaining");}
        h.assertTrue(use(h,p,pos,armor)==InteractionResult.SUCCESS,"Last sixth-unit can dye armor");h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON),"Last sixth-unit empties cauldron");h.succeed();
    }

    @GameTest public void bottleExtractionRequiresTwoUnitsAndReturnsWater(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.writeDyed(h.getLevel(),pos,0x123456,1);var bottle=new ItemStack(Items.GLASS_BOTTLE);
        h.assertTrue(use(h,p,pos,bottle)==InteractionResult.FAIL,"One sixth-unit is insufficient for a bottle");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==1&&bottle.is(Items.GLASS_BOTTLE),"Failed extraction is atomic");
        CauldronService.writeDyed(h.getLevel(),pos,0x123456,5);bottle=new ItemStack(Items.GLASS_BOTTLE);h.assertTrue(use(h,p,pos,bottle)==InteractionResult.SUCCESS,"Two or more units fill a bottle");var result=p.getMainHandItem();h.assertTrue(result.is(Items.POTION)&&result.get(DataComponents.POTION_CONTENTS).is(Potions.WATER),"Colored water yields ordinary water potion");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==3,"Bottle consumes exactly two sixth-units");h.succeed();
    }

    @GameTest public void bucketExtractionRequiresFullAndReturnsWaterBucket(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.writeDyed(h.getLevel(),pos,0x123456,5);var bucket=new ItemStack(Items.BUCKET);
        h.assertTrue(use(h,p,pos,bucket)==InteractionResult.PASS,"Non-full dyed water cannot fill bucket");h.assertTrue(bucket.is(Items.BUCKET)&&h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==5,"Failed bucket extraction is atomic");
        CauldronService.writeDyed(h.getLevel(),pos,0x123456,6);bucket=new ItemStack(Items.BUCKET);h.assertTrue(use(h,p,pos,bucket)==InteractionResult.SUCCESS,"Full dyed water fills bucket");h.assertTrue(p.getMainHandItem().is(Items.WATER_BUCKET)&&h.getLevel().getBlockState(pos).is(Blocks.CAULDRON),"Bucket strips tint and empties full cauldron");h.succeed();
    }

    @GameTest public void waterRefillStripsTintWithBedrockCompatibleVolumes(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        for(int amount:new int[]{1,2,3,4,5}){
            CauldronService.writeDyed(h.getLevel(),pos,0x654321,amount);var water=net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,Potions.WATER);h.assertTrue(use(h,p,pos,water)==InteractionResult.SUCCESS,"Water potion strips tint from amount "+amount);var state=h.getLevel().getBlockState(pos);int expected=Math.min(amount/2+1,3);h.assertTrue(state.is(Blocks.WATER_CAULDRON)&&state.getValue(LayeredCauldronBlock.LEVEL)==expected,"Fractional dyed amount floors before adding one vanilla bottle");h.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE),"Water potion is consumed");
        }
        CauldronService.writeDyed(h.getLevel(),pos,0x654321,3);var bucket=new ItemStack(Items.WATER_BUCKET);h.assertTrue(use(h,p,pos,bucket)==InteractionResult.SUCCESS,"Water bucket overwrites tinted partial volume");h.assertTrue(p.getMainHandItem().is(Items.BUCKET),"Water bucket is consumed");var full=h.getLevel().getBlockState(pos);h.assertTrue(full.is(Blocks.WATER_CAULDRON)&&full.getValue(LayeredCauldronBlock.LEVEL)==3,"Water bucket produces full vanilla water");h.succeed();
    }

    @GameTest public void irrelevantItemsCannotMutateDyedWater(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.writeDyed(h.getLevel(),pos,0xabcdef,4);var stick=new ItemStack(Items.STICK);h.assertTrue(use(h,p,pos,stick)==InteractionResult.PASS,"Non-dye item ignored");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==4&&color(h,pos)==0xabcdef,"Irrelevant item cannot mutate dyed water");
        var metal=new ItemStack(Items.IRON_HORSE_ARMOR);h.assertTrue(Infusions.slot(metal)==null,"Negative fixture is not dyeable armor");h.assertTrue(use(h,p,pos,metal)==InteractionResult.PASS,"Non-dyeable armor ignored");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==4,"Rejected armor consumes no water");h.succeed();
    }
}
