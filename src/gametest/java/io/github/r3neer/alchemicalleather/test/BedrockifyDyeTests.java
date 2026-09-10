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
import net.minecraft.world.phys.*;

public final class BedrockifyDyeTests {
    @GameTest public void coloredWaterRecolorsGeneralizedDyeableArmorWithoutRemovingInfusion(GameTestHelper h) throws Exception {
        boolean loaded=FabricLoader.getInstance().isModLoaded("bedrockify");
        if(System.getProperty("alchemical.compatRequired","").equals("clinging"))h.assertTrue(loaded,"CI integration profile must load BedrockIfy");
        if(!loaded){h.succeed();return;}
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse("bedrockify:colored_water_cauldron"));var pos=h.absolutePos(new BlockPos(1,1,1));var p=h.makeMockPlayer(GameType.SURVIVAL);int color=0x345678;
        for(var item:List.of(Items.IRON_HELMET,Items.COPPER_HORSE_ARMOR)){
            var state=block.defaultBlockState();var property=BedrockifyBridge.property(state);state=state.setValue(property,3);h.getLevel().setBlockAndUpdate(pos,state);var be=h.getLevel().getBlockEntity(pos);be.getClass().getMethod("setDyeColor",int.class).invoke(be,color);
            var armor=new ItemStack(item);
            if(Infusions.slot(armor)==EquipmentSlot.BODY)armor.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(List.of(new Infusion(Identifier.parse("minecraft:luck"),0,"timed",100))));
            else armor.set(Infusions.TYPE,new Infusion(Identifier.parse("minecraft:night_vision"),0,"timed",100));
            armor.set(DataComponents.DYED_COLOR,new DyedItemColor(0x112233));p.setItemInHand(InteractionHand.MAIN_HAND,armor);
            var result=CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));h.assertTrue(result==InteractionResult.SUCCESS,"Colored water accepts generalized dyeable armor "+item);h.assertTrue(armor.get(DataComponents.DYED_COLOR).rgb()==color,"Colored water replaces dye on "+item);h.assertTrue(Infusions.blocked(armor),"Colored water preserves infusion on "+item);h.assertTrue(h.getLevel().getBlockState(pos).getValue(property)==2,"Exactly one colored-water level consumed");
        }h.succeed();
    }
}
