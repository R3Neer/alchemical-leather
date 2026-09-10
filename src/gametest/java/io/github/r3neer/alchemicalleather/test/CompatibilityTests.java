package io.github.r3neer.alchemicalleather.test;
import io.github.r3neer.alchemicalleather.data.*;
import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.effect.*;
import io.github.r3neer.alchemicalleather.mixin.LivingEffectsAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import java.util.*;
public class CompatibilityTests {
    @GameTest public void reorientationInfusesOnlyBoots(GameTestHelper h){
        var id=Identifier.parse("clinging_reoriented:reorientation");
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(id)){
            h.assertTrue(EffectSlotRules.slot(id)==null,"Optional rule inactive without a version supplying Reorientation");h.succeed();return;
        }
        h.assertTrue(EffectSlotRules.slot(id)==EquipmentSlot.FEET,"Reorientation belongs to boots");
        var brewing=h.getLevel().potionBrewing();
        var base=BuiltInRegistries.POTION.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();
        for(var type:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)){
            var bottle=brewing.mix(new ItemStack(Items.SHULKER_SHELL),PotionContents.createItemStack(type,base));
            var contents=bottle.get(DataComponents.POTION_CONTENTS);
            h.assertTrue(contents.potion().orElseThrow().is(id),"Actual shell recipe");
            var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
            CauldronService.write(h.getLevel(),pos,contents,type,1);
            for(var wrong:List.of(Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS)){
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrong));
                h.assertTrue(CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false))==InteractionResult.FAIL,"Other humanoid armor rejected");
            }
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LEATHER_BOOTS));
            CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
            var boots=p.getMainHandItem();var infusion=boots.get(Infusions.TYPE);
            h.assertTrue(infusion!=null && infusion.effect().equals(id),"Boot infusion retains Reorientation");
            h.assertTrue(infusion.mode().equals(type==Items.LINGERING_POTION?"stable":"timed"),"Bottle lifetime policy");
            p.setItemSlot(EquipmentSlot.FEET,boots);EquipmentInfusions.sync(p);
            h.assertTrue(p.hasEffect(infusion.holder().orElseThrow()),"Worn boots apply effect");
            p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);EquipmentInfusions.sync(p);
            h.assertFalse(p.hasEffect(infusion.holder().orElseThrow()),"Removing boots removes owned effect");
        }
        h.succeed();
    }
    @GameTest public void clingingAndReorientationInfuseAnimalArmor(GameTestHelper h){
        if(!FabricLoader.getInstance().isModLoaded("clinging_reoriented")){h.succeed();return;}
        var clingingId=Identifier.parse("alexsmobs:clinging");var reorientationId=Identifier.parse("clinging_reoriented:reorientation");
        var base=BuiltInRegistries.POTION.get(clingingId).orElseThrow();var brewing=h.getLevel().potionBrewing();
        var reorientation=brewing.mix(new ItemStack(Items.SHULKER_SHELL),PotionContents.createItemStack(Items.POTION,base)).get(DataComponents.POTION_CONTENTS);
        for(var contents:List.of(new PotionContents(base),reorientation)){
            var expected=contents.getAllEffects().iterator().next().getEffect();
            for(var animalItem:List.of(Items.LEATHER_HORSE_ARMOR,Items.WOLF_ARMOR)){
                var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.write(h.getLevel(),pos,contents,Items.POTION,1);
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(animalItem));
                h.assertTrue(CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false))==InteractionResult.SUCCESS,"Real gravity potion infuses animal armor");
                var armor=p.getMainHandItem();var bundle=armor.get(Infusions.ANIMAL_TYPE);h.assertTrue(bundle!=null&&bundle.effects().size()==1&&bundle.effects().getFirst().holder().orElseThrow().equals(expected),"Animal armor retains actual gravity effect");
                var entityType=animalItem==Items.LEATHER_HORSE_ARMOR?EntityTypes.HORSE:EntityTypes.WOLF;var animal=(Mob)h.spawn(entityType,new BlockPos(3,2,3));animal.setNoAi(true);animal.setItemSlot(EquipmentSlot.BODY,armor);EquipmentInfusions.sync(animal);
                h.assertTrue(animal.hasEffect(expected),"Equipped animal receives actual gravity effect");animal.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);EquipmentInfusions.sync(animal);h.assertFalse(animal.hasEffect(expected),"Unequip removes actual gravity effect");animal.discard();
            }
        }
        h.assertTrue(BuiltInRegistries.MOB_EFFECT.containsKey(clingingId)&&BuiltInRegistries.MOB_EFFECT.containsKey(reorientationId),"Both real gravity effects loaded");h.succeed();
    }
    @GameTest public void requestedModsActuallyLoaded(GameTestHelper h){
        var profile=System.getProperty("alchemical.compatRequired","");
        if(profile.equals("full"))for(String id:List.of("bedrockify","scalebrews","alexsmobs","friendsandfoes","wilderwild","mr_deeper_dark","enchancement","functional_trims"))h.assertTrue(FabricLoader.getInstance().isModLoaded(id),"Required full-fixture mod "+id);
        if(profile.equals("clinging"))for(String id:List.of("clinging_reoriented","alexsmobs"))h.assertTrue(FabricLoader.getInstance().isModLoaded(id),"Required Clinging fixture mod "+id);
        if(profile.equals("trade"))for(String id:List.of("clinging_reoriented","alexsmobs","scalebrews","bedrockify"))h.assertTrue(FabricLoader.getInstance().isModLoaded(id),"Required trade fixture mod "+id);
        h.succeed();
    }
    @GameTest public void allRegisteredFamiliesResolve(GameTestHelper h){
        for(String namespace:List.of("scalebrews","alexsmobs","friendsandfoes","wilderwild")){
            if(!FabricLoader.getInstance().isModLoaded(namespace))continue;
            int count=0;
            for(var holder:BuiltInRegistries.POTION.listElements().toList()){
                if(!holder.key().identifier().getNamespace().equals(namespace))continue;
                count++;
                for(Item type:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)){
                    var result=Infusions.resolve(new PotionContents(holder),type);
                    h.assertTrue(result.ok(),"Registered potion resolves: "+holder.key().identifier());
                    h.assertTrue(result.infusion().amplifier()==holder.value().getEffects().getFirst().getAmplifier(),"Amplifier unchanged");
                }
            }
            int expected=switch(namespace){case "scalebrews"->8;case "alexsmobs"->15;case "friendsandfoes"->3;default->4;};
            h.assertTrue(count==expected,"Audited potion count "+namespace+" got "+count);
        }
        h.succeed();
    }
    @GameTest public void actualBrewingAndLingeringTierThree(GameTestHelper h){
        if(!FabricLoader.getInstance().isModLoaded("scalebrews")){h.succeed();return;}
        var brewing=h.getLevel().potionBrewing();
        Item starter=BuiltInRegistries.ITEM.getOptional(Identifier.parse("alexsmobs:elastic_tendon")).orElse(Items.SLIME_BALL);
        var growth=brewing.mix(new ItemStack(starter),PotionContents.createItemStack(Items.POTION,Potions.AWKWARD));
        growth=brewing.mix(new ItemStack(Items.GLOWSTONE_DUST),growth);growth=brewing.mix(new ItemStack(Items.GLOWSTONE_DUST),growth);
        h.assertTrue(growth.get(DataComponents.POTION_CONTENTS).potion().orElseThrow().is(Identifier.parse("scalebrews:very_strong_growth")),"Real brewing gives Growth III");
        var shrinking=brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE),growth);
        for(var bottle:List.of(growth,shrinking)){
            var splash=brewing.mix(new ItemStack(Items.GUNPOWDER),bottle);
            var lingering=brewing.mix(new ItemStack(Items.DRAGON_BREATH),splash);
            h.assertTrue(lingering.is(Items.LINGERING_POTION),"Real lingering conversion");
            var infusion=Infusions.resolve(lingering.get(DataComponents.POTION_CONTENTS),lingering.getItem()).infusion();
            h.assertTrue(infusion.mode().equals("stable")&&infusion.amplifier()==2,"Stable tier III");
            var p=h.makeMockPlayer(GameType.SURVIVAL);var armor=new ItemStack(Items.LEATHER_CHESTPLATE);armor.set(Infusions.TYPE,infusion);p.setItemSlot(EquipmentSlot.CHEST,armor);EquipmentInfusions.sync(p);
            for(int i=0;i<200;i++)((LivingEffectsAccess)p).alchemical$tickEffects();
            h.assertTrue(p.getEffect(infusion.holder().orElseThrow()).getAmplifier()==2,"Worn level unchanged");
            p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);EquipmentInfusions.sync(p);h.assertFalse(p.hasEffect(infusion.holder().orElseThrow()),"Scale source removed");
        }
        h.succeed();
    }
    @GameTest public void bedrockifyImportAndFractions(GameTestHelper h) throws Exception {
        if(!FabricLoader.getInstance().isModLoaded("bedrockify")){h.succeed();return;}
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("bedrockify:potion_cauldron"));var pos=h.absolutePos(new BlockPos(1,1,1));var p=h.makeMockPlayer(GameType.SURVIVAL);
        for(int n:List.of(2,5,8)){
            var state=block.defaultBlockState();var property=BedrockifyBridge.property(state);state=state.setValue(property,n);h.getLevel().setBlockAndUpdate(pos,state);
            var be=h.getLevel().getBlockEntity(pos);be.getClass().getMethod("setPotion",ItemStack.class).invoke(be,PotionContents.createItemStack(Items.LINGERING_POTION,Potions.SWIFTNESS));
            var snapshot=BedrockifyBridge.read(h.getLevel(),pos,state);h.assertTrue(snapshot.doses()==(n+1)/3,"Canonical doses");
            var armor=new ItemStack(Items.LEATHER_LEGGINGS);p.setItemInHand(InteractionHand.MAIN_HAND,armor);
            CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
            h.assertTrue(p.getMainHandItem().get(Infusions.TYPE).mode().equals("stable"),"Lingering provenance imported");
        }
        var state=block.defaultBlockState();state=state.setValue(BedrockifyBridge.property(state),4);h.getLevel().setBlockAndUpdate(pos,state);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.LEATHER_LEGGINGS));
        h.assertTrue(CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false))==InteractionResult.FAIL,"Fractional import rejected");
        h.assertTrue(h.getLevel().getBlockState(pos).equals(state),"Fractional state unchanged");h.succeed();
    }
    @GameTest public void deeperDarkRealLootItem(GameTestHelper h){
        if(!FabricLoader.getInstance().isModLoaded("mr_deeper_dark")){h.succeed();return;}
        h.assertTrue(EffectSlotRules.slot(Identifier.parse("minecraft:blindness"))==EquipmentSlot.HEAD,"Datapack detection");
        var server=h.getLevel().getServer();
        var resource=server.getResourceManager().getResource(Identifier.parse("deeper_dark:loot_table/items/splash_potion_of_blindness.json")).orElseThrow();
        try(var reader=resource.openAsReader()){
            var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            var contents=json.getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("components").get("minecraft:potion_contents");
            var parsed=PotionContents.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),contents).getOrThrow();
            var result=Infusions.resolve(parsed,Items.SPLASH_POTION);h.assertTrue(result.ok()&&result.infusion().remainingTicks()==1200,"Real loot component supported");h.assertTrue(parsed.getColor()==1118481,"Real custom color");
        }catch(java.io.IOException e){throw new RuntimeException(e);}h.succeed();
    }
    @GameTest public void trimPreservesInfusion(GameTestHelper h){
        var stack=new ItemStack(Items.LEATHER_CHESTPLATE);var infusion=new Infusion(Identifier.parse("minecraft:strength"),1,"stable",0);stack.set(Infusions.TYPE,infusion);
        var pattern=h.getLevel().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(ResourceKey.create(Registries.TRIM_PATTERN,Identifier.parse("minecraft:sentry")));
        var result=SmithingTrimRecipe.applyTrim(stack,new ItemStack(Items.GOLD_INGOT),pattern);
        h.assertTrue(!result.isEmpty()&&result.has(DataComponents.TRIM)&&infusion.equals(result.get(Infusions.TYPE)),"Trim coexists with infusion");h.succeed();
    }
}
