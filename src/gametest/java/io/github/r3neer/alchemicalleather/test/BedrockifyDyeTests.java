package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.data.*;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

public final class BedrockifyDyeTests {
    private InteractionResult use(GameTestHelper h,net.minecraft.world.entity.player.Player p,BlockPos pos,ItemStack stack){p.setItemInHand(InteractionHand.MAIN_HAND,stack);return CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}

    @GameTest public void coloredWaterRecolorsGeneralizedDyeableArmorWithoutRemovingInfusion(GameTestHelper h) throws Exception {
        boolean loaded=FabricLoader.getInstance().isModLoaded("bedrockify");
        if(System.getProperty("alchemical.compatRequired","").equals("clinging"))h.assertTrue(loaded&&BedrockifyBridge.cauldronsActive(),"CI integration profile must load active BedrockIfy cauldrons");
        if(!BedrockifyBridge.cauldronsActive()){h.succeed();return;}
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("bedrockify:colored_water_cauldron"));var pos=h.absolutePos(new BlockPos(1,1,1));var p=h.makeMockPlayer(GameType.SURVIVAL);int color=0x345678;
        for(var item:List.of(Items.IRON_HELMET,Items.COPPER_HORSE_ARMOR)){
            var state=block.defaultBlockState();var property=BedrockifyBridge.property(state);state=state.setValue(property,3);h.getLevel().setBlockAndUpdate(pos,state);var be=h.getLevel().getBlockEntity(pos);be.getClass().getMethod("setDyeColor",int.class).invoke(be,color);
            var armor=new ItemStack(item);
            if(Infusions.slot(armor)==EquipmentSlot.BODY)armor.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(List.of(new Infusion(Identifier.parse("minecraft:luck"),0,"timed",100))));
            else armor.set(Infusions.TYPE,new Infusion(Identifier.parse("minecraft:night_vision"),0,"timed",100));
            int original=0x112233,expected=DyeColors.blend(color,original);armor.set(DataComponents.DYED_COLOR,new DyedItemColor(original));
            h.assertTrue(use(h,p,pos,armor)==InteractionResult.SUCCESS,"BedrockIfy colored water accepts generalized dyeable armor "+item);h.assertTrue(armor.get(DataComponents.DYED_COLOR).rgb()==expected,"BedrockIfy compatibility blends instead of replacing item dye");h.assertTrue(Infusions.blocked(armor),"BedrockIfy recolor preserves infusion on "+item);h.assertTrue(h.getLevel().getBlockState(pos).getValue(property)==2,"Exactly one BedrockIfy colored-water unit consumed");
        }h.succeed();
    }

    @GameTest public void disabledBedrockIfyCauldronSettingReturnsVanillaDyeOwnership(GameTestHelper h) throws Exception {
        if(!FabricLoader.getInstance().isModLoaded("bedrockify")){h.succeed();return;}
        var type=Class.forName("me.juancarloscp52.bedrockify.Bedrockify");var instance=type.getMethod("getInstance").invoke(null);var settings=type.getField("settings").get(instance);var field=settings.getClass().getField("bedrockCauldron");boolean old=field.getBoolean(settings);
        try {
            field.setBoolean(settings,false);h.assertFalse(BedrockifyBridge.cauldronsActive(),"Disabled BedrockIfy setting is not treated as active ownership");
            var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,2));var dye=new ItemStack(Items.DYE.red());
            h.assertTrue(use(h,p,pos,dye)==InteractionResult.SUCCESS,"Alchemical Leather takes over vanilla water dyeing when BedrockIfy feature is disabled");h.assertTrue(h.getLevel().getBlockState(pos).is(DyedWaterCauldron.BLOCK)&&h.getLevel().getBlockState(pos).getValue(DyedWaterCauldron.LEVEL)==4,"Fallback ownership creates native dyed water");h.assertTrue(dye.isEmpty(),"Exactly one dye consumed by fallback owner");
        } finally {field.setBoolean(settings,old);}
        h.assertTrue(!old||BedrockifyBridge.cauldronsActive(),"BedrockIfy setting restored after ownership test");h.succeed();
    }
}
