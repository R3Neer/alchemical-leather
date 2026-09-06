package io.github.r3neer.alchemicalleather.test;
import io.github.r3neer.alchemicalleather.data.*;
import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.effect.*;
import io.github.r3neer.alchemicalleather.mixin.LivingEffectsAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;
import java.util.*;
public final class PersistenceTests {
    @GameTest public void playerSaveLoadKeepsSourcesSeparate(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.addEffect(new MobEffectInstance(MobEffects.SPEED,200,0));
        var armor=new ItemStack(Items.LEATHER_LEGGINGS);armor.set(Infusions.TYPE,new Infusion(Identifier.parse("minecraft:speed"),1,"timed",500));p.setItemSlot(EquipmentSlot.LEGS,armor);EquipmentInfusions.sync(p);
        for(int i=0;i<50;i++)((LivingEffectsAccess)p).alchemical$tickEffects();
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());p.saveWithoutId(output);
        var saved=output.buildResult();var input=TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved);
        var savedEffects=input.read("active_effects",MobEffectInstance.CODEC.listOf()).orElseThrow();
        h.assertTrue(savedEffects.size()==1&&savedEffects.getFirst().getAmplifier()==0&&savedEffects.getFirst().getDuration()==150,"Disk contains real external source only");
        var loaded=h.makeMockPlayer(GameType.SURVIVAL);loaded.load(input);EquipmentInfusions.sync(loaded);
        h.assertTrue(loaded.getItemBySlot(EquipmentSlot.LEGS).get(Infusions.TYPE).remainingTicks()==450,"Armor clock restored");
        loaded.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);EquipmentInfusions.sync(loaded);
        h.assertTrue(loaded.getEffect(MobEffects.SPEED).getAmplifier()==0&&loaded.getEffect(MobEffects.SPEED).getDuration()==150,"External remains after reload and unequip");h.succeed();
    }
    @GameTest public void blockEntityCustomContentsRoundTrip(GameTestHelper h){
        var contents=new PotionContents(Optional.empty(),Optional.of(0x112233),List.of(new MobEffectInstance(MobEffects.SPEED,77,2)),Optional.empty());
        var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.write(h.getLevel(),pos,contents,Items.SPLASH_POTION,3);
        var be=(PotionCauldronEntity)h.getLevel().getBlockEntity(pos);var saved=be.saveWithoutMetadata(h.getLevel().registryAccess());
        var loaded=new PotionCauldronEntity(pos,be.getBlockState());loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
        h.assertTrue(loaded.contents.equals(contents)&&loaded.bottle==Items.SPLASH_POTION,"Custom effects/color and splash provenance persist");h.succeed();
    }
    @GameTest public void periodicEffectMatchesVanilla(GameTestHelper h){
        var normal=h.makeMockPlayer(GameType.SURVIVAL);var armored=h.makeMockPlayer(GameType.SURVIVAL);normal.setHealth(1);armored.setHealth(1);
        normal.addEffect(new MobEffectInstance(MobEffects.REGENERATION,120,0));
        var armor=new ItemStack(Items.LEATHER_CHESTPLATE);armor.set(Infusions.TYPE,new Infusion(Identifier.parse("minecraft:regeneration"),0,"timed",120));armored.setItemSlot(EquipmentSlot.CHEST,armor);EquipmentInfusions.sync(armored);
        for(int i=0;i<120;i++){((LivingEffectsAccess)normal).alchemical$tickEffects();((LivingEffectsAccess)armored).alchemical$tickEffects();h.assertTrue(normal.getHealth()==armored.getHealth(),"Regen pulse same tick "+i);}
        h.succeed();
    }
    @GameTest public void instantaneousSemanticsOverrideStableFlag(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.getFoodData().setFoodLevel(1);
        var armor=new ItemStack(Items.LEATHER_CHESTPLATE);armor.set(Infusions.TYPE,new Infusion(Identifier.parse("minecraft:saturation"),1,"stable",0));p.setItemSlot(EquipmentSlot.CHEST,armor);EquipmentInfusions.sync(p);
        h.assertFalse(armor.has(Infusions.TYPE),"Any instantaneous effect is consumed, even stable from commands");h.assertTrue(p.getFoodData().getFoodLevel()>1,"Saturation ran generically");h.succeed();
    }
}
