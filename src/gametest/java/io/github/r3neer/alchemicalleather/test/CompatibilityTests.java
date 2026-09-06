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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import java.util.*;
public class CompatibilityTests {
    @GameTest public void requestedModsActuallyLoaded(GameTestHelper h){
        if(Boolean.getBoolean("alchemical.compatRequired"))for(String id:List.of("bedrockify","scalebrews","alexsmobs","friendsandfoes","wilderwild","mr_deeper_dark","enchancement","functional_trims"))h.assertTrue(FabricLoader.getInstance().isModLoaded(id),"Required test mod "+id);
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
