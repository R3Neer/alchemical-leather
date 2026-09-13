package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.data.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

public final class PotionCauldronInteractionTests {
    private InteractionResult use(GameTestHelper h,Player p,BlockPos pos,ItemStack stack){
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        return CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    private int dye(Item item){return new ItemStack(item).get(DataComponents.DYE).getTextureDiffuseColor()&0xffffff;}
    private Infusion timed(String effect,int duration,int amplifier){return new Infusion(Identifier.parse(effect),amplifier,"timed",duration);}

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

    @GameTest public void effectlessVanillaPotionsPourAndRoundTrip(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        for(var potion:List.of(Potions.AWKWARD,Potions.MUNDANE,Potions.THICK))for(var bottle:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)){
            h.getLevel().setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());var stack=PotionContents.createItemStack(bottle,potion);var original=stack.get(DataComponents.POTION_CONTENTS);
            h.assertTrue(use(h,p,pos,stack)==InteractionResult.SUCCESS,"Effectless potion pours: "+potion+" / "+bottle);
            var state=h.getLevel().getBlockState(pos);h.assertTrue(state.is(PotionCauldron.BLOCK)&&state.getValue(PotionCauldron.LEVEL)==1,"Effectless potion creates one stored dose");
            var be=(PotionCauldronEntity)h.getLevel().getBlockEntity(pos);h.assertTrue(be.bottle==bottle&&be.contents.equals(original),"Effectless contents and bottle type are preserved");
            h.assertTrue(use(h,p,pos,new ItemStack(Items.GLASS_BOTTLE))==InteractionResult.SUCCESS,"Effectless potion can be extracted again");var extracted=p.getMainHandItem();
            h.assertTrue(extracted.is(bottle)&&Objects.equals(extracted.get(DataComponents.POTION_CONTENTS),original),"Extraction round-trips effectless potion data and bottle type");
            h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON),"Extracting the only effectless dose leaves an ordinary empty cauldron");
        }h.succeed();
    }

    @GameTest public void effectlessPotionDyesEnchantedArmorWithoutCreatingInfusion(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int color=0x4169e1;
        var contents=new PotionContents(Optional.of(Potions.AWKWARD),Optional.of(color),List.of(),Optional.empty());CauldronService.write(h.getLevel(),pos,contents,Items.POTION,2);
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);leggings.set(DataComponents.CUSTOM_NAME,Component.literal("keep me"));leggings.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),1);var enchants=leggings.get(DataComponents.ENCHANTMENTS);
        h.assertTrue(use(h,p,pos,leggings)==InteractionResult.SUCCESS,"Effectless potion can dye enchanted compatible armor");var result=p.getMainHandItem();
        h.assertTrue(result.has(DataComponents.DYED_COLOR)&&result.get(DataComponents.DYED_COLOR).rgb()==color,"Effectless potion transfers its exact visible color");
        h.assertTrue(!Infusions.blocked(result)&&Objects.equals(result.get(DataComponents.ENCHANTMENTS),enchants),"Dye-only use creates no infusion and preserves enchantments");
        h.assertTrue(result.get(DataComponents.CUSTOM_NAME).getString().equals("keep me"),"Dye-only use preserves unrelated components");
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==1,"Dye-only use consumes exactly one potion dose");h.succeed();
    }

    @GameTest public void effectlessPotionPreservesExistingHumanoidInfusion(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int color=0x7b68ee;
        var contents=new PotionContents(Optional.of(Potions.MUNDANE),Optional.of(color),List.of(),Optional.empty());CauldronService.write(h.getLevel(),pos,contents,Items.SPLASH_POTION,1);
        var existing=timed("minecraft:speed",321,1);var leggings=new ItemStack(Items.LEATHER_LEGGINGS);leggings.set(Infusions.TYPE,existing);leggings.set(DataComponents.DYED_COLOR,new DyedItemColor(0x112233));
        h.assertTrue(use(h,p,pos,leggings)==InteractionResult.SUCCESS,"Effectless potion recolors already-infused humanoid armor");var result=p.getMainHandItem();
        h.assertTrue(existing.equals(result.get(Infusions.TYPE))&&!result.has(Infusions.ANIMAL_TYPE),"Existing humanoid infusion survives dye-only use unchanged");
        h.assertTrue(result.get(DataComponents.DYED_COLOR).rgb()==color,"Effectless potion replaces the visible armor color");h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON),"Dye-only use consumes the final dose");h.succeed();
    }

    @GameTest public void effectlessPotionDyesBodyArmorWithoutEmptyBundle(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int color=0x20b2aa;
        var contents=new PotionContents(Optional.of(Potions.THICK),Optional.of(color),List.of(),Optional.empty());CauldronService.write(h.getLevel(),pos,contents,Items.LINGERING_POTION,2);
        var plain=new ItemStack(Items.LEATHER_HORSE_ARMOR);h.assertTrue(use(h,p,pos,plain)==InteractionResult.SUCCESS,"Effectless potion dyes plain BODY armor");var dyed=p.getMainHandItem();
        h.assertTrue(dyed.get(DataComponents.DYED_COLOR).rgb()==color&&!dyed.has(Infusions.TYPE)&&!dyed.has(Infusions.ANIMAL_TYPE),"Plain BODY armor receives color without an empty infusion bundle");
        var existing=new AnimalInfusion(List.of(timed("minecraft:speed",222,0)));var infused=new ItemStack(Items.LEATHER_HORSE_ARMOR);infused.set(Infusions.ANIMAL_TYPE,existing);
        h.assertTrue(use(h,p,pos,infused)==InteractionResult.SUCCESS,"Effectless potion recolors already-infused BODY armor");var preserved=p.getMainHandItem();
        h.assertTrue(existing.equals(preserved.get(Infusions.ANIMAL_TYPE))&&preserved.get(DataComponents.DYED_COLOR).rgb()==color,"BODY infusion survives dye-only recoloring unchanged");
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON),"Two BODY dye operations consume two doses");h.succeed();
    }

    @GameTest public void tintedEffectlessPotionAcceptsMatchingRefill(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));var original=new PotionContents(Optional.of(Potions.AWKWARD),Optional.of(0x234567),List.of(),Optional.empty());
        CauldronService.write(h.getLevel(),pos,original,Items.POTION,1);var red=new ItemStack(Items.DYE.red());int expected=DyeColors.blend(original.getColor(),dye(Items.DYE.red()));
        h.assertTrue(use(h,p,pos,red)==InteractionResult.SUCCESS,"Effectless potion cauldron can itself be tinted");var refill=new ItemStack(Items.POTION);refill.set(DataComponents.POTION_CONTENTS,original);
        h.assertTrue(use(h,p,pos,refill)==InteractionResult.SUCCESS,"Matching effectless refill ignores the stored decorative tint");var be=(PotionCauldronEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==2&&(be.contents.getColor()&0xffffff)==expected,"Matching effectless refill preserves tint and adds one dose");h.succeed();
    }

    @GameTest public void waterPotionStillDelegatesToWaterCauldronSemantics(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());var water=PotionContents.createItemStack(Items.POTION,Potions.WATER);
        h.assertTrue(use(h,p,pos,water)==InteractionResult.PASS,"Water potion remains delegated to Minecraft water-cauldron handling");h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON)&&water.getCount()==1,"Alchemical Leather does not consume or convert delegated water potion");h.succeed();
    }

    @GameTest public void malformedPotionWithoutContentsIsRejectedAtomically(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());var malformed=new ItemStack(Items.POTION);malformed.remove(DataComponents.POTION_CONTENTS);
        h.assertTrue(use(h,p,pos,malformed)==InteractionResult.FAIL,"Potion item without potion contents is rejected");h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON)&&malformed.getCount()==1,"Malformed potion rejection mutates neither cauldron nor item");h.succeed();
    }

    @GameTest public void dyeingPotionCauldronPreservesPotionBottleAndDoses(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        var original=new PotionContents(Optional.of(Potions.SWIFTNESS),Optional.of(0x123456),List.of(new MobEffectInstance(MobEffects.JUMP_BOOST,200,1)),Optional.of("custom"));
        CauldronService.write(h.getLevel(),pos,original,Items.LINGERING_POTION,2);var red=new ItemStack(Items.DYE.red());int expected=DyeColors.blend(original.getColor(),dye(Items.DYE.red()));
        h.assertTrue(use(h,p,pos,red)==InteractionResult.SUCCESS,"Potion cauldron accepts dye");
        var state=h.getLevel().getBlockState(pos);var be=(PotionCauldronEntity)h.getLevel().getBlockEntity(pos);var recolored=be.contents;
        h.assertTrue(state.getValue(PotionCauldron.LEVEL)==2,"Dyeing changes no dose count");h.assertTrue(be.bottle==Items.LINGERING_POTION,"Dyeing preserves bottle type");
        h.assertTrue(recolored.potion().equals(original.potion())&&recolored.customEffects().equals(original.customEffects())&&recolored.customName().equals(original.customName()),"Dyeing preserves potion identity effects and name");
        h.assertTrue((recolored.getColor()&0xffffff)==expected,"Dye blends with the current potion color");h.assertTrue(red.isEmpty(),"Changed potion color consumes one dye");
        var refill=new ItemStack(Items.LINGERING_POTION);refill.set(DataComponents.POTION_CONTENTS,original);h.assertTrue(use(h,p,pos,refill)==InteractionResult.SUCCESS,"Tint does not make a matching potion count as different");
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==3&&((PotionCauldronEntity)h.getLevel().getBlockEntity(pos)).contents.getColor()==expected,"Matching refill preserves the cauldron tint");h.succeed();
    }

    @GameTest public void noOpPotionDyeDoesNotConsumeDye(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));int redColor=dye(Items.DYE.red());
        var contents=new PotionContents(Optional.of(Potions.SWIFTNESS),Optional.of(redColor),List.of(),Optional.empty());CauldronService.write(h.getLevel(),pos,contents,Items.POTION,3);var red=new ItemStack(Items.DYE.red());
        h.assertTrue(use(h,p,pos,red)==InteractionResult.SUCCESS,"No-op potion dye interaction is accepted");var be=(PotionCauldronEntity)h.getLevel().getBlockEntity(pos);
        h.assertTrue(red.getCount()==1,"No-op potion dye consumes nothing");h.assertTrue(be.contents.equals(contents)&&be.bottle==Items.POTION,"No-op potion dye mutates no stored data");h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==3,"No-op potion dye changes no doses");h.succeed();
    }
}
