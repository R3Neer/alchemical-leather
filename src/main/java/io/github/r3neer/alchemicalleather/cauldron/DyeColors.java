package io.github.r3neer.alchemicalleather.cauldron;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public final class DyeColors {
    private DyeColors() {}

    public static int blend(int... colors) {
        if(colors.length==0)throw new IllegalArgumentException("At least one color is required");
        long peakSum=0,redSum=0,greenSum=0,blueSum=0;
        for(int raw:colors) {
            int color=raw&0xffffff;
            int red=color>>16&255,green=color>>8&255,blue=color&255;
            peakSum+=Math.max(red,Math.max(green,blue));
            redSum+=red;greenSum+=green;blueSum+=blue;
        }
        int red=(int)(redSum/colors.length),green=(int)(greenSum/colors.length),blue=(int)(blueSum/colors.length);
        int peak=Math.max(red,Math.max(green,blue));
        if(peak==0)return 0;
        double targetPeak=(double)peakSum/colors.length;
        red=(int)(red*targetPeak/peak);green=(int)(green*targetPeak/peak);blue=(int)(blue*targetPeak/peak);
        return (red<<16)|(green<<8)|blue;
    }

    public static int blend(ItemStack stack,int color) {
        DyedItemColor current=stack.get(DataComponents.DYED_COLOR);
        return current==null?(color&0xffffff):blend(color,current.rgb());
    }
}
