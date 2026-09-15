package io.github.r3neer.alchemicalleather.data;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;

/** Pure armor transformation shared by cauldron and crafting paths. */
public final class ArmorInfusionService {
    public record Result(ItemStack stack,String error){public boolean ok(){return stack!=null;}}
    private ArmorInfusionService(){}

    public static Result apply(ItemStack input,PotionContents contents,Item bottle,boolean allowEffectlessDye){
        var target=Infusions.slot(input);
        if(target==null)return new Result(null,"slot");
        if(contents==null)return new Result(null,"invalid");
        boolean hasEffects=contents.getAllEffects().iterator().hasNext();
        var result=input.copy();
        if(!hasEffects){
            if(!allowEffectlessDye)return new Result(null,"no_effect");
            result.set(DataComponents.DYED_COLOR,new DyedItemColor(contents.getColor()&0xffffff));
            return new Result(result,null);
        }
        if(Infusions.enchanted(input))return new Result(null,"enchanted");
        if(target==EquipmentSlot.BODY){
            var resolved=Infusions.resolveAll(contents,bottle);
            if(!resolved.ok())return new Result(null,resolved.error());
            Infusions.clearInfusionComponents(result);
            result.set(Infusions.ANIMAL_TYPE,resolved.infusion());
        }else{
            var resolved=Infusions.resolveHumanoid(contents,bottle,target);
            if(!resolved.ok())return new Result(null,resolved.error());
            Infusions.clearInfusionComponents(result);
            var effects=resolved.infusion().effects();
            if(effects.size()==1)result.set(Infusions.TYPE,effects.getFirst());
            else result.set(Infusions.HUMANOID_TYPE,new HumanoidInfusion(effects));
        }
        result.set(DataComponents.DYED_COLOR,new DyedItemColor(contents.getColor()&0xffffff));
        return new Result(result,null);
    }
}
