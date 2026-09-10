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

    @GameTest public void animalArmorClassificationIsPropertyDriven(GameTestHelper h){
        h.assertTrue(Infusions.animalArmor(new ItemStack(Items.LEATHER_HORSE_ARMOR)),"Vanilla leather horse armor detected from leather BODY semantics");
        h.assertTrue(Infusions.animalArmor(new ItemStack(Items.WOLF_ARMOR)),"Vanilla wolf armor detected through extension tag");
        h.assertFalse(Infusions.animalArmor(new ItemStack(Items.IRON_HORSE_ARMOR)),"Metal horse armor is not alchemical animal armor");
        var bodyOnly=new ItemStack(Items.STICK);bodyOnly.set(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.BODY).build());
        h.assertFalse(Infusions.animalArmor(bodyOnly),"BODY alone is insufficient");
        var assetLeather=bodyOnly.copy();assetLeather.set(DataComponents.EQUIPPABLE,Equippable.builder(EquipmentSlot.BODY).setAsset(EquipmentAssets.LEATHER).build());
        h.assertTrue(Infusions.animalArmor(assetLeather),"Mod-like BODY item inherits support from leather asset");
        var repairLeather=bodyOnly.copy();repairLeather.set(DataComponents.REPAIRABLE,new Repairable(HolderSet.direct(Items.LEATHER.builtInRegistryHolder())));
        h.assertTrue(Infusions.animalArmor(repairLeather),"Mod-like BODY item inherits support from leather repairability");h.succeed();
    }

    @GameTest public void unmappedPotionInfusesHorseAndWolfButNotHumanoid(GameTestHelper h){
        var luck=id(MobEffects.LUCK);h.assertTrue(EffectSlotRules.slot(luck)==null,"Luck deliberately has no humanoid slot rule");
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.LUCK),Items.POTION,3);
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);h.assertTrue(use(h,p,pos,leggings)==InteractionResult.FAIL,"Unmapped effect still rejected by humanoid leather");
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==3,"Humanoid rejection consumes no dose");
        h.assertTrue(use(h,p,pos,new ItemStack(Items.LEATHER_HORSE_ARMOR))==InteractionResult.SUCCESS,"Leather horse armor accepts unmapped potion");
        var horse=p.getMainHandItem();h.assertTrue(horse.get(Infusions.ANIMAL_TYPE).effects().getFirst().effect().equals(luck),"Horse armor stores Luck");
        h.assertTrue(use(h,p,pos,new ItemStack(Items.WOLF_ARMOR))==InteractionResult.SUCCESS,"Wolf armor accepts unmapped potion");
        var wolf=p.getMainHandItem();h.assertTrue(wolf.get(Infusions.ANIMAL_TYPE).effects().getFirst().effect().equals(luck),"Wolf armor stores Luck");h.succeed();
    }

    @GameTest public void multiEffectPotionRunsAndPausesOnHorse(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.TURTLE_MASTER),Items.POTION,1);
        h.assertTrue(use(h,p,pos,new ItemStack(Items.LEATHER_HORSE_ARMOR))==InteractionResult.SUCCESS,"Turtle Master infuses animal armor");
        var armor=p.getMainHandItem();var initial=armor.get(Infusions.ANIMAL_TYPE);h.assertTrue(initial.effects().size()==2,"Both Turtle Master effects are stored");
        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(3,2,3));horse.setNoAi(true);horse.setItemSlot(EquipmentSlot.BODY,armor);EquipmentInfusions.sync(horse);
        for(var infusion:initial.effects())h.assertTrue(horse.hasEffect(infusion.holder().orElseThrow()),"Every stored effect is projected");
        ticks(horse,20);var after=armor.get(Infusions.ANIMAL_TYPE);for(int i=0;i<after.effects().size();i++)h.assertTrue(after.effects().get(i).remainingTicks()==initial.effects().get(i).remainingTicks()-20,"Each animal clock advances independently");
        horse.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);EquipmentInfusions.sync(horse);var paused=after.effects().stream().map(Infusion::remainingTicks).toList();ticks(horse,20);
        h.assertTrue(after.effects().stream().map(Infusion::remainingTicks).toList().equals(paused),"Unequipped armor pauses all clocks");
        for(var infusion:initial.effects())h.assertFalse(horse.hasEffect(infusion.holder().orElseThrow()),"Unequip removes armor-owned effects");h.succeed();
    }

    @GameTest public void repeatedEffectSourcesRevealWithoutGap(GameTestHelper h){
        var contents=new PotionContents(Optional.empty(),Optional.empty(),List.of(new MobEffectInstance(MobEffects.SPEED,3,1),new MobEffectInstance(MobEffects.SPEED,7,0)),Optional.empty());
        var resolved=Infusions.resolveAll(contents,Items.POTION);h.assertTrue(resolved.ok()&&resolved.infusion().effects().size()==2,"Repeated custom effects remain independent");
        var armor=new ItemStack(Items.LEATHER_HORSE_ARMOR);armor.set(Infusions.ANIMAL_TYPE,resolved.infusion());
        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(3,2,3));horse.setNoAi(true);horse.setItemSlot(EquipmentSlot.BODY,armor);EquipmentInfusions.sync(horse);
        h.assertTrue(horse.getEffect(MobEffects.SPEED).getAmplifier()==1,"Stronger repeated source projects first");
        ticks(horse,3);var remaining=armor.get(Infusions.ANIMAL_TYPE);
        h.assertTrue(remaining.effects().size()==1&&remaining.effects().getFirst().amplifier()==0&&remaining.effects().getFirst().remainingTicks()==4,"Weaker source kept its own clock");
        h.assertTrue(horse.getEffect(MobEffects.SPEED)!=null&&horse.getEffect(MobEffects.SPEED).getAmplifier()==0,"Weaker source is revealed immediately without a blank tick");
        ticks(horse,4);h.assertFalse(armor.has(Infusions.ANIMAL_TYPE)||horse.hasEffect(MobEffects.SPEED),"Last repeated source expires cleanly");h.succeed();
    }

    @GameTest public void mixedInstantAndTimedConsumesOnlyInstant(GameTestHelper h){
        var contents=new PotionContents(Optional.empty(),Optional.empty(),List.of(new MobEffectInstance(MobEffects.INSTANT_HEALTH,1,0),new MobEffectInstance(MobEffects.SPEED,40,0)),Optional.empty());
        var bundle=Infusions.resolveAll(contents,Items.POTION).infusion();var armor=new ItemStack(Items.LEATHER_HORSE_ARMOR);armor.set(Infusions.ANIMAL_TYPE,bundle);
        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(3,2,3));horse.setNoAi(true);horse.setHealth(1);horse.setItemSlot(EquipmentSlot.BODY,armor);EquipmentInfusions.sync(horse);
        h.assertTrue(horse.getHealth()>1,"Instant Health fires on equip");var left=armor.get(Infusions.ANIMAL_TYPE);h.assertTrue(left!=null&&left.effects().size()==1&&left.effects().getFirst().effect().equals(id(MobEffects.SPEED)),"Timed sibling survives instant consumption");
        horse.setHealth(1);EquipmentInfusions.sync(horse);h.assertTrue(horse.getHealth()==1,"Consumed instant effect cannot replay");h.assertTrue(horse.hasEffect(MobEffects.SPEED),"Timed sibling remains active");h.succeed();
    }

    @GameTest public void bottleModesApplyToEveryAnimalEffect(GameTestHelper h){
        var contents=new PotionContents(Potions.TURTLE_MASTER);
        for(var item:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)){
            var resolved=Infusions.resolveAll(contents,item);h.assertTrue(resolved.ok()&&resolved.infusion().effects().size()==2,"All effects resolve for "+item);
            String expected=item==Items.LINGERING_POTION?"stable":"timed";for(var infusion:resolved.infusion().effects())h.assertTrue(infusion.mode().equals(expected),"Mode propagated to every effect");
        }h.succeed();
    }

    @GameTest public void reinfusionWashingAndEnchantmentStayAtomic(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.TURTLE_MASTER),Items.POTION,1);
        use(h,p,pos,new ItemStack(Items.LEATHER_HORSE_ARMOR));var armor=p.getMainHandItem();h.assertTrue(armor.get(Infusions.ANIMAL_TYPE).effects().size()==2,"Initial multi-effect infusion");
        CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.LUCK),Items.POTION,1);use(h,p,pos,armor);armor=p.getMainHandItem();
        h.assertTrue(armor.get(Infusions.ANIMAL_TYPE).effects().size()==1&&armor.get(Infusions.ANIMAL_TYPE).effects().getFirst().effect().equals(id(MobEffects.LUCK)),"Reinfusion replaces rather than accumulates");
        h.assertTrue(armor.has(DataComponents.DYED_COLOR),"Animal infusion carries potion color");h.getLevel().setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));use(h,p,pos,armor);
        h.assertFalse(armor.has(Infusions.ANIMAL_TYPE)||armor.has(Infusions.TYPE)||armor.has(DataComponents.DYED_COLOR),"Water removes animal infusion and color");
        CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.LUCK),Items.POTION,2);var enchanted=new ItemStack(Items.LEATHER_HORSE_ARMOR);
        enchanted.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),1);var before=enchanted.copy();
        h.assertTrue(use(h,p,pos,enchanted)==InteractionResult.FAIL,"Enchanted animal armor rejected");h.assertTrue(ItemStack.matches(before,enchanted)&&h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==2,"Rejected animal infusion is atomic");h.succeed();
    }

    @GameTest public void externalEffectClockSurvivesAnimalArmor(GameTestHelper h){
        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(3,2,3));horse.setNoAi(true);horse.addEffect(new MobEffectInstance(MobEffects.SPEED,200,0));
        var armor=animal(Items.LEATHER_HORSE_ARMOR,List.of(timed(MobEffects.SPEED,100,1)));horse.setItemSlot(EquipmentSlot.BODY,armor);EquipmentInfusions.sync(horse);ticks(horse,50);
        h.assertTrue(horse.getEffect(MobEffects.SPEED).getAmplifier()==1,"Stronger armor source projects over external effect");horse.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);EquipmentInfusions.sync(horse);
        h.assertTrue(horse.getEffect(MobEffects.SPEED).getAmplifier()==0&&horse.getEffect(MobEffects.SPEED).getDuration()==150,"External source kept its independent clock");h.succeed();
    }

    @GameTest public void animalInfusionComponentRoundTrips(GameTestHelper h){
        var original=animal(Items.LEATHER_HORSE_ARMOR,List.of(timed(MobEffects.SPEED,123,1),timed(MobEffects.SPEED,456,0),new Infusion(id(MobEffects.FIRE_RESISTANCE),0,"stable",0)));
        original.set(DataComponents.DYED_COLOR,new DyedItemColor(0x234567));var ops=h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var copy=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,original).getOrThrow()).getOrThrow();
        h.assertTrue(copy.get(Infusions.ANIMAL_TYPE).equals(original.get(Infusions.ANIMAL_TYPE)),"Multi/repeated animal bundle survives item serialization");
        h.assertTrue(copy.get(DataComponents.DYED_COLOR).equals(original.get(DataComponents.DYED_COLOR)),"Unrelated dye component survives serialization");h.succeed();
    }
}
