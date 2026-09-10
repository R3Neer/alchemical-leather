package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.data.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

public final class DyedWaterTests {
    private InteractionResult use(GameTestHelper h,net.minecraft.world.entity.player.Player p,BlockPos pos,ItemStack stack){p.setItemInHand(InteractionHand.MAIN_HAND,stack);return CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    private int dye(Item item){return new ItemStack(item).get(DataComponents.DYE).getTextureDiffuseColor()&0xffffff;}
    private int color(GameTestHelper h,BlockPos pos){return ((DyedWaterCauldronEntity)h.getLevel().getBlockEntity(pos)).color;}

    @GameTest public void vanillaWaterDyeOwnershipAndLevels(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        for(int level=1;level<=3;level++){
            h.getLevel().setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,level));var red=new ItemStack(Items.DYE.red());
            var result=use(h,p,pos,red);
            if(BedrockifyBridge.cauldronsActive()){
                h.assertTrue(result==InteractionResult.PASS,"Active BedrockIfy owns vanilla water + dye");h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.WATER_CAULDRON)&&red.getCount()==1,"Yielding is side-effect free");
            }else{
                h.assertTrue(result==InteractionResult.SUCCESS,"Native dye-water entry succeeds without BedrockIfy");var state=h.getLevel().getBlockState(pos);h.assertTrue(state.is(DyedWaterCauldron.BLOCK)&&state.getValue(DyedWaterCauldron.LEVEL)==level*2,"Vanilla levels map 1/2/3 to 2/4/6");h.assertTrue(color(h,pos)==dye(Items.DYE.red()),"First dye determines water color");h.assertTrue(red.isEmpty(),"One dye consumed in survival");
            }
        }h.succeed();
    }

    @GameTest public void nativeColorMixingAndNoOpDoesNotConsumeDye(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int red=dye(Items.DYE.red()),blue=dye(Items.DYE.blue());CauldronService.writeDyed(h.getLevel(),pos,red,4);
        var blueStack=new ItemStack(Items.DYE.blue());h.assertTrue(use(h,p,pos,blueStack)==InteractionResult.SUCCESS,"Second dye mixes into native colored water");h.assertTrue(color(h,pos)==DyeColors.blend(red,blue),"Native water uses brightness-preserving dye blend");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==4,"Mixing dye changes no fluid amount");h.assertTrue(blueStack.isEmpty(),"Changed mix consumes one dye");
        int current=color(h,pos);CauldronService.writeDyed(h.getLevel(),pos,current,4);var same=new ItemStack(Items.DYE.blue());int before=same.getCount();
        // Force a mathematically identical blend by using the exact current color as both operands through a pre-dyed red case.
        CauldronService.writeDyed(h.getLevel(),pos,red,4);var redStack=new ItemStack(Items.DYE.red());h.assertTrue(use(h,p,pos,redStack)==InteractionResult.SUCCESS,"Identical dye interaction is accepted");h.assertTrue(color(h,pos)==red&&redStack.getCount()==1,"No-op color mix consumes no dye");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==4,"No-op mix changes no fluid");
        h.assertTrue(before==1,"Fixture sanity");h.succeed();
    }

    @GameTest public void nativeArmorDyeingBlendsPreservesAndUsesSixUnits(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int water=0x22aa66;CauldronService.writeDyed(h.getLevel(),pos,water,6);
        for(int i=0;i<6;i++){
            ItemStack armor=i%2==0?new ItemStack(Items.IRON_HELMET):new ItemStack(Items.COPPER_HORSE_ARMOR);
            if(i==0)armor.set(DataComponents.DYED_COLOR,new DyedItemColor(0xaa2244));
            if(Infusions.slot(armor)==net.minecraft.world.entity.EquipmentSlot.BODY)armor.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(List.of(new Infusion(Identifier.parse("minecraft:luck"),0,"timed",100))));
            else armor.set(Infusions.TYPE,new Infusion(Identifier.parse("minecraft:night_vision"),0,"timed",100));
            int expected=i==0?DyeColors.blend(water,0xaa2244):water;h.assertTrue(use(h,p,pos,armor)==InteractionResult.SUCCESS,"Dyeable armor consumes one native color unit");h.assertTrue(armor.get(DataComponents.DYED_COLOR).rgb()==expected,"Armor color applies/blends correctly");h.assertTrue(Infusions.blocked(armor),"Coloring preserves infusion data");
            if(i<5)h.assertTrue(h.getLevel().getBlockState(pos).is(DyedWaterCauldron.BLOCK)&&h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==5-i,"Exactly one of six units consumed");
            else h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON),"Sixth armor use empties cauldron");
        }h.succeed();
    }

    @GameTest public void nativeBottleBucketAndWaterPotionAccounting(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        CauldronService.writeDyed(h.getLevel(),pos,0x123456,1);var bottle=new ItemStack(Items.GLASS_BOTTLE);h.assertTrue(use(h,p,pos,bottle)==InteractionResult.FAIL,"Half-bottle unit cannot create a full bottle");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==1&&bottle.is(Items.GLASS_BOTTLE),"Insufficient bottle extraction is atomic");
        CauldronService.writeDyed(h.getLevel(),pos,0x123456,2);bottle=new ItemStack(Items.GLASS_BOTTLE);h.assertTrue(use(h,p,pos,bottle)==InteractionResult.SUCCESS,"Two units fill one water bottle");var waterBottle=p.getMainHandItem();h.assertTrue(waterBottle.is(Items.POTION)&&waterBottle.get(DataComponents.POTION_CONTENTS).is(Potions.WATER),"Colored water extracts as ordinary water potion");h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON),"Two-unit bottle extraction empties cauldron");
        CauldronService.writeDyed(h.getLevel(),pos,0x123456,5);var bucket=new ItemStack(Items.BUCKET);h.assertTrue(use(h,p,pos,bucket)==InteractionResult.PASS,"Non-full dyed water cannot fill bucket");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==5&&bucket.is(Items.BUCKET),"Non-full bucket extraction is atomic");
        CauldronService.writeDyed(h.getLevel(),pos,0x123456,6);bucket=new ItemStack(Items.BUCKET);h.assertTrue(use(h,p,pos,bucket)==InteractionResult.SUCCESS,"Full dyed water fills bucket");h.assertTrue(p.getMainHandItem().is(Items.WATER_BUCKET)&&h.getLevel().getBlockState(pos).is(Blocks.CAULDRON),"Full bucket extraction returns water and empties cauldron");
        for(int dyed=1;dyed<=6;dyed++){
            CauldronService.writeDyed(h.getLevel(),pos,0x123456,dyed);var water=PotionContents.createItemStack(Items.POTION,Potions.WATER);var result=use(h,p,pos,water);h.assertTrue(result==InteractionResult.SUCCESS,"Water potion refill interaction accepted at level "+dyed);
            if(dyed==6){h.assertTrue(h.getLevel().getBlockState(pos).is(DyedWaterCauldron.BLOCK)&&h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==6,"Full dyed cauldron stays full");h.assertTrue(p.getMainHandItem().is(Items.POTION),"Full cauldron does not consume water potion");}
            else {int expected=Math.min(dyed/2+1,3);h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.WATER_CAULDRON)&&h.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)==expected,"Dyed volume converts back to vanilla water conservatively");h.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE),"Accepted refill consumes water potion");}
        }h.succeed();
    }

    @GameTest public void nativeDyedWaterRejectsUnqualifiedTargets(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.writeDyed(h.getLevel(),pos,0xabcdef,4);
        var metal=new ItemStack(Items.IRON_HORSE_ARMOR);var before=metal.copy();h.assertTrue(use(h,p,pos,metal)==InteractionResult.PASS,"Non-dyeable armor is not colored by native dyed water");h.assertTrue(ItemStack.matches(before,metal)&&h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==4,"Rejected armor consumes no color unit");
        var stick=new ItemStack(Items.STICK);h.assertTrue(use(h,p,pos,stick)==InteractionResult.PASS,"Arbitrary non-dye item is ignored");h.assertTrue(h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==4,"Ignored item leaves dyed water untouched");
        h.assertTrue(Infusions.armorCandidate(new ItemStack(Items.IRON_HORSE_ARMOR))&&Infusions.slot(new ItemStack(Items.IRON_HORSE_ARMOR))==null,"Client prediction candidate is intentionally broader than server dyeability authority");h.succeed();
    }
}
