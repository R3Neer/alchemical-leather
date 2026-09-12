package io.github.r3neer.alchemicalleather.test;

import com.mojang.serialization.JsonOps;
import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.data.*;
import io.github.r3neer.alchemicalleather.effect.*;
import io.github.r3neer.alchemicalleather.mixin.LivingEffectsAccess;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

public final class AnimalArmorTests {
    private InteractionResult use(GameTestHelper h,net.minecraft.world.entity.player.Player p,BlockPos pos,ItemStack stack){p.setItemInHand(InteractionHand.MAIN_HAND,stack);return CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    private void ticks(LivingEntity entity,int count){for(int i=0;i<count;i++)((LivingEffectsAccess)entity).alchemical$tickEffects();}
    private Identifier id(Holder<MobEffect> effect){return BuiltInRegistries.MOB_EFFECT.getKey(effect.value());}
    private Infusion timed(Holder<MobEffect> effect,int duration,int amplifier){return new Infusion(id(effect),amplifier,"timed",duration);}
    private ItemStack animal(Item item,List<Infusion> effects){var stack=new ItemStack(item);stack.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(effects));return stack;}

    @GameTest public void dyeableArmorDiscoveryCoversVanillaAndModLikeData(GameTestHelper h){
        var vanilla=Map.of(
            Items.LEATHER_HELMET,EquipmentSlot.HEAD,
            Items.LEATHER_CHESTPLATE,EquipmentSlot.CHEST,
            Items.LEATHER_LEGGINGS,EquipmentSlot.LEGS,
            Items.LEATHER_BOOTS,EquipmentSlot.FEET,
            Items.LEATHER_HORSE_ARMOR,EquipmentSlot.BODY,
            Items.WOLF_ARMOR,EquipmentSlot.BODY);
        vanilla.forEach((item,slot)->h.assertTrue(Infusions.slot(new ItemStack(item))==slot,"Vanilla standard dye/wash armor discovered: "+item));
        h.assertTrue(Infusions.slot(new ItemStack(Items.IRON_HELMET))==EquipmentSlot.HEAD,"Direct self-recoloring crafting_dye recipe gives mod-like humanoid support");
        h.assertTrue(Infusions.slot(new ItemStack(Items.COPPER_HORSE_ARMOR))==EquipmentSlot.BODY,"Direct self-recoloring crafting_dye recipe gives mod-like BODY support");
        h.assertTrue(Infusions.slot(new ItemStack(Items.GOLDEN_HORSE_ARMOR))==EquipmentSlot.BODY,"Tagged self-recoloring target is discovered");
        h.assertTrue(Infusions.slot(new ItemStack(Items.DIAMOND_HORSE_ARMOR))==EquipmentSlot.BODY,"List target admits its actual result item");
        h.assertTrue(Infusions.slot(new ItemStack(Items.NETHERITE_HORSE_ARMOR))==null,"A transmuting crafting_dye input is not mistaken for an in-place dyeable item");
        h.assertTrue(Infusions.slot(new ItemStack(Items.IRON_CHESTPLATE))==EquipmentSlot.CHEST,"Standard cauldron_can_remove_dye tag gives mod-like support without a dye recipe special case");
        h.assertTrue(Infusions.slot(new ItemStack(Items.GOLDEN_HELMET))==EquipmentSlot.HEAD,"Fallback tag covers custom dye systems");
        h.assertTrue(Infusions.slot(new ItemStack(Items.IRON_HORSE_ARMOR))==null,"Armor slot without dyeability evidence is rejected");
        var wrongSlot=new ItemStack(Items.IRON_HELMET);wrongSlot.set(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.SADDLE).build());
        h.assertTrue(Infusions.slot(wrongSlot)==null,"Dyeability alone cannot turn non-armor equipment into Alchemical Leather");h.succeed();
    }

    @GameTest public void syntheticHumanoidArmorKeepsSlotAndSingleEffectRules(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.NIGHT_VISION),Items.POTION,1);
        h.assertTrue(use(h,p,pos,new ItemStack(Items.IRON_HELMET))==InteractionResult.SUCCESS,"Synthetic dyeable helmet can be infused");
        var helmet=p.getMainHandItem();var infusion=helmet.get(Infusions.TYPE);h.assertTrue(infusion!=null&&infusion.effect().equals(id(MobEffects.NIGHT_VISION))&&!helmet.has(Infusions.ANIMAL_TYPE),"Humanoid armor retains legacy single component");
        p.setItemSlot(EquipmentSlot.HEAD,helmet);EquipmentInfusions.sync(p);h.assertTrue(p.hasEffect(MobEffects.NIGHT_VISION),"Generalized humanoid armor activates by actual slot");p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);EquipmentInfusions.sync(p);h.assertFalse(p.hasEffect(MobEffects.NIGHT_VISION),"Generalized humanoid armor unequips cleanly");
        CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.SWIFTNESS),Items.POTION,1);var wrong=new ItemStack(Items.IRON_HELMET);h.assertTrue(use(h,p,pos,wrong)==InteractionResult.FAIL,"Speed still rejects a dyeable helmet");h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==1,"Wrong humanoid slot consumes no dose");
        CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.TURTLE_MASTER),Items.POTION,1);var multi=new ItemStack(Items.IRON_HELMET);h.assertTrue(use(h,p,pos,multi)==InteractionResult.FAIL,"Humanoid dyeable armor still rejects multi-effect potions");h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==1,"Multi-effect humanoid rejection is atomic");h.succeed();
    }

    @GameTest public void unmappedPotionInfusesHorseAndWolfButNotHumanoid(GameTestHelper h){
        var luck=id(MobEffects.LUCK);h.assertTrue(EffectSlotRules.slot(luck)==null,"Luck deliberately has no humanoid slot rule");
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.LUCK),Items.POTION,3);
        h.assertTrue(use(h,p,pos,new ItemStack(Items.LEATHER_LEGGINGS))==InteractionResult.FAIL,"Unmapped effect still rejected by humanoid armor");h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==3,"Humanoid rejection consumes no dose");
        h.assertTrue(use(h,p,pos,new ItemStack(Items.LEATHER_HORSE_ARMOR))==InteractionResult.SUCCESS,"Leather horse armor accepts unmapped potion");var horse=p.getMainHandItem();h.assertTrue(horse.get(Infusions.ANIMAL_TYPE).effects().getFirst().effect().equals(luck),"Horse armor stores Luck");
        h.assertTrue(use(h,p,pos,new ItemStack(Items.WOLF_ARMOR))==InteractionResult.SUCCESS,"Wolf armor accepts unmapped potion");var wolf=p.getMainHandItem();h.assertTrue(wolf.get(Infusions.ANIMAL_TYPE).effects().getFirst().effect().equals(luck),"Wolf armor stores Luck");h.succeed();
    }

    @GameTest public void multiEffectPotionRunsAndPausesOnHorse(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.TURTLE_MASTER),Items.POTION,1);
        h.assertTrue(use(h,p,pos,new ItemStack(Items.LEATHER_HORSE_ARMOR))==InteractionResult.SUCCESS,"Turtle Master infuses animal armor");var armor=p.getMainHandItem();var initial=armor.get(Infusions.ANIMAL_TYPE);h.assertTrue(initial.effects().size()==2,"One potion retains both Turtle Master effects");
        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(3,2,3));horse.setNoAi(true);horse.setItemSlot(EquipmentSlot.BODY,armor);EquipmentInfusions.sync(horse);for(var entry:initial.effects())h.assertTrue(horse.hasEffect(entry.holder().orElseThrow()),"Every effect from the one potion is projected");
        ticks(horse,20);var after=armor.get(Infusions.ANIMAL_TYPE);for(int i=0;i<after.effects().size();i++)h.assertTrue(after.effects().get(i).remainingTicks()==initial.effects().get(i).remainingTicks()-20,"Each animal clock advances independently");
        horse.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);EquipmentInfusions.sync(horse);var paused=armor.get(Infusions.ANIMAL_TYPE).effects().stream().map(Infusion::remainingTicks).toList();ticks(horse,20);var afterPause=armor.get(Infusions.ANIMAL_TYPE).effects().stream().map(Infusion::remainingTicks).toList();
        h.assertTrue(afterPause.equals(paused),"Unequipped armor pauses all live item clocks");for(var entry:initial.effects())h.assertFalse(horse.hasEffect(entry.holder().orElseThrow()),"Unequip removes armor-owned effects");h.succeed();
    }

    @GameTest public void repeatedEffectSourcesRevealWithoutGap(GameTestHelper h){
        var contents=new PotionContents(Optional.empty(),Optional.empty(),List.of(new MobEffectInstance(MobEffects.SPEED,3,1),new MobEffectInstance(MobEffects.SPEED,7,0)),Optional.empty());var resolved=Infusions.resolveAll(contents,Items.POTION);h.assertTrue(resolved.ok()&&resolved.infusion().effects().size()==2,"Repeated custom effects remain part of the one potion bundle");
        var armor=new ItemStack(Items.LEATHER_HORSE_ARMOR);armor.set(Infusions.ANIMAL_TYPE,resolved.infusion());var horse=h.spawn(EntityTypes.HORSE,new BlockPos(3,2,3));horse.setNoAi(true);horse.setItemSlot(EquipmentSlot.BODY,armor);EquipmentInfusions.sync(horse);h.assertTrue(horse.getEffect(MobEffects.SPEED).getAmplifier()==1,"Stronger repeated source projects first");
        ticks(horse,3);var remaining=armor.get(Infusions.ANIMAL_TYPE);h.assertTrue(remaining.effects().size()==1&&remaining.effects().getFirst().amplifier()==0&&remaining.effects().getFirst().remainingTicks()==4,"Weaker source kept its own clock");h.assertTrue(horse.getEffect(MobEffects.SPEED)!=null&&horse.getEffect(MobEffects.SPEED).getAmplifier()==0,"Weaker source is revealed immediately without a blank tick");
        ticks(horse,4);h.assertFalse(armor.has(Infusions.ANIMAL_TYPE)||horse.hasEffect(MobEffects.SPEED),"Last repeated source expires cleanly");h.succeed();
    }

    @GameTest public void mixedInstantAndTimedConsumesOnlyInstant(GameTestHelper h){
        var contents=new PotionContents(Optional.empty(),Optional.empty(),List.of(new MobEffectInstance(MobEffects.INSTANT_HEALTH,1,0),new MobEffectInstance(MobEffects.SPEED,40,0)),Optional.empty());var bundle=Infusions.resolveAll(contents,Items.POTION).infusion();var armor=new ItemStack(Items.LEATHER_HORSE_ARMOR);armor.set(Infusions.ANIMAL_TYPE,bundle);
        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(3,2,3));horse.setNoAi(true);horse.setHealth(1);horse.setItemSlot(EquipmentSlot.BODY,armor);EquipmentInfusions.sync(horse);h.assertTrue(horse.getHealth()>1,"Instant Health fires on equip");var left=armor.get(Infusions.ANIMAL_TYPE);h.assertTrue(left!=null&&left.effects().size()==1&&left.effects().getFirst().effect().equals(id(MobEffects.SPEED)),"Timed sibling from same potion survives instant consumption");
        horse.setHealth(1);EquipmentInfusions.sync(horse);h.assertTrue(horse.getHealth()==1,"Consumed instant effect cannot replay");h.assertTrue(horse.hasEffect(MobEffects.SPEED),"Timed sibling remains active");h.succeed();
    }

    @GameTest public void bottleModesApplyToEveryAnimalEffect(GameTestHelper h){
        var contents=new PotionContents(Potions.TURTLE_MASTER);for(var item:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)){var resolved=Infusions.resolveAll(contents,item);h.assertTrue(resolved.ok()&&resolved.infusion().effects().size()==2,"All effects resolve for "+item);String expected=item==Items.LINGERING_POTION?"stable":"timed";for(var entry:resolved.infusion().effects())h.assertTrue(entry.mode().equals(expected),"Mode propagated to every effect");}h.succeed();
    }

    @GameTest public void cauldronColorAndWashGeneralizeWithDyeability(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        for(var item:List.of(Items.IRON_HELMET,Items.COPPER_HORSE_ARMOR)){
            var contents=new PotionContents(item==Items.IRON_HELMET?Potions.NIGHT_VISION:Potions.LUCK);CauldronService.write(h.getLevel(),pos,contents,Items.POTION,1);var input=new ItemStack(item);input.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("keep me"));
            h.assertTrue(use(h,p,pos,input)==InteractionResult.SUCCESS,"Potion infusion works on synthetic dyeable armor "+item);var infused=p.getMainHandItem();h.assertTrue(infused.get(DataComponents.DYED_COLOR).rgb()==(contents.getColor()&0xffffff),"Potion color applied to "+item);var customName=infused.get(DataComponents.CUSTOM_NAME);h.assertTrue(customName!=null&&customName.getString().equals("keep me"),"Unrelated components preserved on "+item);
            h.getLevel().setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));use(h,p,pos,infused);h.assertFalse(Infusions.blocked(infused)||infused.has(DataComponents.DYED_COLOR),"Water removes infusion and dye from "+item);h.assertTrue(h.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)==2,"Infused wash consumes exactly one water level");
            var merelyDyed=new ItemStack(item);merelyDyed.set(DataComponents.DYED_COLOR,new DyedItemColor(0x123456));h.getLevel().setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));use(h,p,pos,merelyDyed);h.assertFalse(merelyDyed.has(DataComponents.DYED_COLOR),"Water also cleans merely dyed qualifying armor "+item);h.assertTrue(h.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)==2,"Plain dye wash consumes one level");
        }h.succeed();
    }

    @GameTest public void craftingDyePreservesInfusionOnGeneralizedArmor(GameTestHelper h){
        var helmet=new ItemStack(Items.IRON_HELMET);var infusion=timed(MobEffects.NIGHT_VISION,200,0);helmet.set(Infusions.TYPE,infusion);var manager=h.getLevel().getServer().getRecipeManager();
        var holder=manager.getRecipes().stream().filter(r->r.id().identifier().equals(Identifier.parse("alchemical_leather_test:iron_helmet_dyed"))).findFirst().orElseThrow();h.assertTrue(holder.value() instanceof DyeRecipe,"Synthetic standard dye recipe loaded");var recipe=(DyeRecipe)holder.value();var input=CraftingInput.of(2,1,List.of(helmet,new ItemStack(Items.DYE.red())));
        if(BedrockifyBridge.cauldronsActive()){
            h.assertFalse(recipe.matches(input,h.getLevel()),"Active BedrockIfy deliberately revokes crafting dye recipes in favor of cauldron dyeing");h.succeed();return;
        }
        h.assertTrue(recipe.matches(input,h.getLevel()),"Infused generalized armor remains dyeable through ordinary recipe");var dyed=recipe.assemble(input);h.assertTrue(dyed.get(Infusions.TYPE).equals(infusion)&&dyed.has(DataComponents.DYED_COLOR),"Ordinary dyeing preserves Alchemical Leather infusion");h.succeed();
    }

    @GameTest public void reinfusionAndEnchantmentStayAtomic(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.TURTLE_MASTER),Items.POTION,1);use(h,p,pos,new ItemStack(Items.COPPER_HORSE_ARMOR));var armor=p.getMainHandItem();h.assertTrue(armor.get(Infusions.ANIMAL_TYPE).effects().size()==2,"Initial BODY infusion contains one multi-effect potion");
        CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.LUCK),Items.POTION,1);use(h,p,pos,armor);armor=p.getMainHandItem();h.assertTrue(armor.get(Infusions.ANIMAL_TYPE).effects().size()==1&&armor.get(Infusions.ANIMAL_TYPE).effects().getFirst().effect().equals(id(MobEffects.LUCK)),"Reinfusion replaces previous potion instead of accumulating another");
        CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.LUCK),Items.POTION,2);var enchanted=new ItemStack(Items.COPPER_HORSE_ARMOR);enchanted.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),1);var before=enchanted.copy();h.assertTrue(use(h,p,pos,enchanted)==InteractionResult.FAIL,"Enchanted dyeable BODY armor rejected");h.assertTrue(ItemStack.matches(before,enchanted)&&h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==2,"Rejected BODY infusion is atomic");h.succeed();
    }

    @GameTest public void externalEffectClockSurvivesAnimalArmor(GameTestHelper h){
        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(3,2,3));horse.setNoAi(true);horse.addEffect(new MobEffectInstance(MobEffects.SPEED,200,0));var armor=animal(Items.LEATHER_HORSE_ARMOR,List.of(timed(MobEffects.SPEED,100,1)));horse.setItemSlot(EquipmentSlot.BODY,armor);EquipmentInfusions.sync(horse);ticks(horse,50);h.assertTrue(horse.getEffect(MobEffects.SPEED).getAmplifier()==1,"Stronger armor source projects over external effect");horse.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);EquipmentInfusions.sync(horse);h.assertTrue(horse.getEffect(MobEffects.SPEED).getAmplifier()==0&&horse.getEffect(MobEffects.SPEED).getDuration()==150,"External source kept its independent clock");h.succeed();
    }

    @GameTest public void animalInfusionComponentRoundTrips(GameTestHelper h){
        var original=animal(Items.LEATHER_HORSE_ARMOR,List.of(timed(MobEffects.SPEED,123,1),timed(MobEffects.SPEED,456,0),new Infusion(id(MobEffects.FIRE_RESISTANCE),0,"stable",0)));original.set(DataComponents.DYED_COLOR,new DyedItemColor(0x234567));var ops=h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);var copy=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,original).getOrThrow()).getOrThrow();h.assertTrue(copy.get(Infusions.ANIMAL_TYPE).equals(original.get(Infusions.ANIMAL_TYPE)),"Multi/repeated effects from one animal potion survive item serialization");h.assertTrue(copy.get(DataComponents.DYED_COLOR).equals(original.get(DataComponents.DYED_COLOR)),"Unrelated dye component survives serialization");h.succeed();
    }
}
