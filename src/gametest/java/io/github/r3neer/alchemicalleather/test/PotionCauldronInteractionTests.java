package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.cauldron.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

public final class PotionCauldronInteractionTests {
    private InteractionResult use(GameTestHelper h,Player p,BlockPos pos,ItemStack stack){
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        return CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    private int dye(Item item){return new ItemStack(item).get(DataComponents.DYE).getTextureDiffuseColor()&0xffffff;}

    @GameTest public void splashAndLingeringPourWithNormalUse(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setShiftKeyDown(false);var pos=h.absolutePos(new BlockPos(1,1,1));
        for(var item:List.of(Items.SPLASH_POTION,Items.LINGERING_POTION)){
            h.getLevel().setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());var stack=PotionContents.createItemStack(item,Potions.SWIFTNESS);
            h.assertTrue(use(h,p,pos,stack)==InteractionResult.SUCCESS,"Splash/lingering pours without sneaking");
            var state=h.getLevel().getBlockState(pos);h.assertTrue(state.is(PotionCauldron.BLOCK)&&state.getValue(PotionCauldron.LEVEL)==1,"Normal use creates one potion dose");
            var be=(PotionCauldronEntity)h.getLevel().getBlockEntity(pos);h.assertTrue(be.bottle==item,"Potion cauldron keeps the original bottle type");
            h.assertTrue(p.getMainHandItem().is(Items.GLASS_BOTTLE),"Successful pour returns a glass bottle");
        }h.succeed();
    }

    @GameTest public void dyeingPotionCauldronPreservesPotionBottleAndDoses(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        var original=new PotionContents(Optional.of(Potions.SWIFTNESS),Optional.of(0x123456),List.of(new MobEffectInstance(MobEffects.JUMP_BOOST,200,1)),Optional.of("custom"));
        CauldronService.write(h.getLevel(),pos,original,Items.LINGERING_POTION,2);var red=new ItemStack(Items.DYE.red());int expected=DyeColors.blend(original.getColor(),dye(Items.DYE.red()));
        h.assertTrue(use(h,p,pos,red)==InteractionResult.SUCCESS,"Potion cauldron accepts dye");
        var state=h.getLevel().getBlockState(pos);var be=(PotionCauldronEntity)h.getLevel().getBlockEntity(pos);var recolored=be.contents;
        h.assertTrue(state.getValue(PotionCauldron.LEVEL)==2,"Dyeing changes no dose count");h.assertTrue(be.bottle==Items.LINGERING_POTION,"Dyeing preserves bottle type");
        h.assertTrue(recolored.potion().equals(original.potion())&&recolored.customEffects().equals(original.customEffects())&&recolored.customName().equals(original.customName()),"Dyeing preserves potion identity effects and name");
        h.assertTrue((recolored.getColor()&0xffffff)==expected,"Dye blends with the current potion color");h.assertTrue(red.isEmpty(),"Changed potion color consumes one dye");h.succeed();
    }

    @GameTest public void noOpPotionDyeDoesNotConsumeDye(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int redColor=dye(Items.DYE.red());
        var contents=new PotionContents(Optional.of(Potions.SWIFTNESS),Optional.of(redColor),List.of(),Optional.empty());CauldronService.write(h.getLevel(),pos,contents,Items.POTION,3);var red=new ItemStack(Items.DYE.red());
        h.assertTrue(use(h,p,pos,red)==InteractionResult.SUCCESS,"No-op potion dye interaction is accepted");var be=(PotionCauldronEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(red.getCount()==1,"No-op potion dye consumes nothing");h.assertTrue(be.contents.equals(contents)&&be.bottle==Items.POTION,"No-op potion dye mutates no stored data");h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==3,"No-op potion dye changes no doses");h.succeed();
    }
}
