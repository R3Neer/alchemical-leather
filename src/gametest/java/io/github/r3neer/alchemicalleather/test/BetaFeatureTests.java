package io.github.r3neer.alchemicalleather.test;

import com.google.gson.JsonParser;
import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.config.AlchemicalConfig;
import io.github.r3neer.alchemicalleather.data.*;
import io.github.r3neer.alchemicalleather.recipe.ArmorInfusionRecipe;
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
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

public final class BetaFeatureTests {
    private InteractionResult use(GameTestHelper h,Player p,BlockPos pos,ItemStack stack){
        p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        return CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    private ItemStack find(Player p,Item item){
        for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);if(stack.is(item))return stack;}
        return ItemStack.EMPTY;
    }
    private ArmorInfusionRecipe recipe(GameTestHelper h){
        var holder=h.getLevel().getServer().getRecipeManager().getRecipes().stream()
            .filter(r->r.id().identifier().equals(ArmorInfusionRecipe.ID)).findFirst().orElseThrow();
        return (ArmorInfusionRecipe)holder.value();
    }

    @GameTest public void configParserDefaultsAndArrowToggle(GameTestHelper h){
        h.assertTrue(AlchemicalConfig.parse(JsonParser.parseString("{}")).cauldronTippedArrows(),"Missing option keeps arrow tipping enabled by default");
        h.assertFalse(AlchemicalConfig.parse(JsonParser.parseString("{\"cauldronTippedArrows\":false}")).cauldronTippedArrows(),"Explicit false disables standalone arrow tipping");
        h.assertTrue(AlchemicalConfig.parse(JsonParser.parseString("{\"cauldronTippedArrows\":\"bad\"}")).cauldronTippedArrows(),"Malformed field falls back to the documented default");h.succeed();
    }

    @GameTest public void arrowTippingStandaloneCoversDoseBoundaries(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(1,1,1));var contents=new PotionContents(Potions.SWIFTNESS);
        record Case(int doses,int arrows,int tipped,int plain,int remaining){}
        var cases=List.of(
            new Case(1,1,1,0,0),new Case(1,16,16,0,0),new Case(1,17,16,1,0),
            new Case(2,17,17,0,0),new Case(2,32,32,0,0),new Case(2,33,32,1,0),
            new Case(3,33,33,0,0),new Case(3,64,64,0,0));
        for(var c:cases){
            var p=h.makeMockPlayer(GameType.SURVIVAL);CauldronService.write(h.getLevel(),pos,contents,Items.POTION,c.doses());
            h.assertTrue(use(h,p,pos,new ItemStack(Items.ARROW,c.arrows()))==InteractionResult.SUCCESS,"Arrow tipping succeeds at "+c);
            h.assertTrue(p.getInventory().countItem(Items.TIPPED_ARROW)==c.tipped(),"Expected tipped arrow count at "+c);
            h.assertTrue(p.getInventory().countItem(Items.ARROW)==c.plain(),"Expected remaining plain arrows at "+c);
            var tipped=find(p,Items.TIPPED_ARROW);h.assertTrue(!tipped.isEmpty()&&Objects.equals(tipped.get(DataComponents.POTION_CONTENTS),contents),"Tipped arrows carry exact stored potion contents at "+c);
            var state=h.getLevel().getBlockState(pos);
            if(c.remaining()==0)h.assertTrue(state.is(Blocks.CAULDRON),"Consumed capacity empties cauldron at "+c);
            else h.assertTrue(state.is(PotionCauldron.BLOCK)&&state.getValue(PotionCauldron.LEVEL)==c.remaining(),"Expected remaining doses at "+c);
        }h.succeed();
    }

    @GameTest public void arrowTippingPreservesCustomPotionContents(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        var contents=new PotionContents(Optional.of(Potions.SWIFTNESS),Optional.of(0x345678),List.of(new MobEffectInstance(MobEffects.JUMP_BOOST,123,1)),Optional.of("custom-arrow-bath"));
        CauldronService.write(h.getLevel(),pos,contents,Items.SPLASH_POTION,1);h.assertTrue(use(h,p,pos,new ItemStack(Items.ARROW,8))==InteractionResult.SUCCESS,"Custom potion bath tips arrows");
        var result=find(p,Items.TIPPED_ARROW);h.assertTrue(Objects.equals(result.get(DataComponents.POTION_CONTENTS),contents),"Custom effects color and name round-trip to tipped arrows exactly");h.succeed();
    }

    @GameTest public void creativeArrowTippingConsumesNothingAndDoesNotRepeatIdenticalStack(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.CREATIVE);var pos=h.absolutePos(new BlockPos(1,1,1));var contents=new PotionContents(Potions.SWIFTNESS);CauldronService.write(h.getLevel(),pos,contents,Items.POTION,3);var arrows=new ItemStack(Items.ARROW,64);
        h.assertTrue(use(h,p,pos,arrows)==InteractionResult.SUCCESS,"Creative arrow tipping succeeds");h.assertTrue(arrows.getCount()==64,"Creative keeps source arrows");
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==3,"Creative keeps all cauldron doses");int first=p.getInventory().countItem(Items.TIPPED_ARROW);h.assertTrue(first==64,"Creative receives one tipped stack");
        h.assertTrue(use(h,p,pos,arrows)==InteractionResult.SUCCESS,"Repeated creative tipping remains a valid gesture");h.assertTrue(p.getInventory().countItem(Items.TIPPED_ARROW)==first,"Repeated identical creative tipping does not duplicate the stack");h.succeed();
    }

    @GameTest public void armorCraftingAcceptsAllBottleModesAndPreservesComponents(GameTestHelper h){
        var recipe=recipe(h);
        for(var bottle:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)){
            var armor=new ItemStack(Items.LEATHER_LEGGINGS);armor.set(DataComponents.CUSTOM_NAME,Component.literal("keep-components"));armor.setDamageValue(7);
            var potion=PotionContents.createItemStack(bottle,Potions.SWIFTNESS);var input=CraftingInput.of(2,1,List.of(armor,potion));
            h.assertTrue(recipe.matches(input,h.getLevel()),"Armor infusion recipe accepts "+bottle);var result=recipe.assemble(input);var infusion=result.get(Infusions.TYPE);
            h.assertTrue(infusion!=null,"Humanoid crafting creates the single infusion component");String expected=bottle==Items.LINGERING_POTION?"stable":"timed";h.assertTrue(infusion.mode().equals(expected),"Bottle mode matches cauldron semantics for "+bottle);
            h.assertTrue(result.getDamageValue()==7&&result.get(DataComponents.CUSTOM_NAME).getString().equals("keep-components"),"Crafting preserves damage/name for "+bottle);
            h.assertTrue(result.has(DataComponents.DYED_COLOR)&&result.get(DataComponents.DYED_COLOR).rgb()==(potion.get(DataComponents.POTION_CONTENTS).getColor()&0xffffff),"Crafting transfers potion color for "+bottle);
            var remaining=recipe.getRemainingItems(input);h.assertTrue(remaining.get(0).isEmpty()&&remaining.get(1).is(Items.GLASS_BOTTLE),"Potion bottle remains as glass after crafting");
        }h.succeed();
    }

    @GameTest public void armorCraftingSupportsBodyBundlesAndAtomicReinfusion(GameTestHelper h){
        var recipe=recipe(h);var body=new ItemStack(Items.LEATHER_HORSE_ARMOR);body.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(List.of(new Infusion(Identifier.withDefaultNamespace("speed"),0,"timed",40))));
        var turtle=PotionContents.createItemStack(Items.LINGERING_POTION,Potions.TURTLE_MASTER);var input=CraftingInput.of(2,1,List.of(body,turtle));h.assertTrue(recipe.matches(input,h.getLevel()),"BODY armor accepts multi-effect potion in crafting");var result=recipe.assemble(input);var bundle=result.get(Infusions.ANIMAL_TYPE);
        h.assertTrue(bundle!=null&&bundle.effects().size()==2&&!result.has(Infusions.TYPE),"BODY crafting atomically replaces old infusion with complete potion bundle");for(var effect:bundle.effects())h.assertTrue(effect.mode().equals("stable"),"Lingering BODY effects are stable");h.succeed();
    }

    @GameTest public void armorCraftingRejectsInvalidInputsAtomically(GameTestHelper h){
        var recipe=recipe(h);var speed=PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS);
        h.assertFalse(recipe.matches(CraftingInput.of(2,1,List.of(new ItemStack(Items.LEATHER_HELMET),speed)),h.getLevel()),"Wrong humanoid effect slot is rejected");
        h.assertFalse(recipe.matches(CraftingInput.of(2,1,List.of(new ItemStack(Items.LEATHER_LEGGINGS),PotionContents.createItemStack(Items.POTION,Potions.AWKWARD))),h.getLevel()),"Effectless potion cannot create crafting infusion");
        var enchanted=new ItemStack(Items.LEATHER_LEGGINGS);enchanted.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),1);
        h.assertFalse(recipe.matches(CraftingInput.of(2,1,List.of(enchanted,speed)),h.getLevel()),"Enchanted armor is rejected like the cauldron path");
        h.assertFalse(recipe.matches(CraftingInput.of(3,1,List.of(new ItemStack(Items.LEATHER_LEGGINGS),speed,new ItemStack(Items.DIRT))),h.getLevel()),"Unrelated extra ingredient rejects recipe");
        h.assertFalse(recipe.matches(CraftingInput.of(3,1,List.of(new ItemStack(Items.LEATHER_LEGGINGS),speed,PotionContents.createItemStack(Items.SPLASH_POTION,Potions.SWIFTNESS))),h.getLevel()),"Two potion bottles reject recipe");h.succeed();
    }
}
