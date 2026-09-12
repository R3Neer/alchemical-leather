package io.github.r3neer.alchemicalleather.client;
import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.data.*;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
public final class AlchemicalLeatherClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        CauldronRenderInvalidation.registerClientInvalidator(pos->{
            var client=net.minecraft.client.Minecraft.getInstance();
            if(client.level!=null){
                int sectionX=pos.getX()>>4,sectionY=pos.getY()>>4,sectionZ=pos.getZ()>>4;
                client.level.setSectionRangeDirty(sectionX,sectionY,sectionZ,sectionX,sectionY,sectionZ);
            }
        });
        BlockColorRegistry.register((state,level,pos,colors)->{
            int color=0xffffffff;
            if(level instanceof FabricBlockGetter getter&&getter.getBlockEntityRenderData(pos) instanceof Integer rgb)color=rgb|0xff000000;
            colors.add(color);
        },PotionCauldron.BLOCK,DyedWaterCauldron.BLOCK);
        ItemTooltipCallback.EVENT.register((stack,context,flag,lines)->{
            var single=stack.get(Infusions.TYPE);var animal=stack.get(Infusions.ANIMAL_TYPE);
            var display=stack.get(DataComponents.TOOLTIP_DISPLAY);
            boolean showSingle=single!=null&&(display==null||display.shows(Infusions.TYPE));
            boolean showAnimal=animal!=null&&(display==null||display.shows(Infusions.ANIMAL_TYPE));
            if(!showSingle&&!showAnimal)return;
            lines.add(Component.translatable("tooltip.alchemical_leather.title").withStyle(ChatFormatting.DARK_AQUA));
            if(showSingle)addInfusion(lines,single);
            if(showAnimal)for(var infusion:animal.effects())addInfusion(lines,infusion);
        });
    }
    private static void addInfusion(List<Component> lines,Infusion infusion) {
        var holder=infusion.holder();
        if(holder.isEmpty()){lines.add(Component.translatable("message.alchemical_leather.invalid").withStyle(ChatFormatting.GRAY));return;}
        var name=Component.translatable(holder.get().value().getDescriptionId());
        if(infusion.amplifier()>0)name=Component.translatable("potion.withAmplifier",name,Component.translatable("potion.potency."+infusion.amplifier()));
        lines.add(name.withStyle(ChatFormatting.GRAY));
        if(infusion.mode().equals("timed")) {
            int seconds=(infusion.remainingTicks()+19)/20;
            lines.add(Component.translatable("tooltip.alchemical_leather.remaining",String.format(java.util.Locale.ROOT,"%d:%02d",seconds/60,seconds%60)).withStyle(ChatFormatting.GRAY));
        } else lines.add(Component.translatable("tooltip.alchemical_leather."+infusion.mode()).withStyle(ChatFormatting.GRAY));
    }
}
