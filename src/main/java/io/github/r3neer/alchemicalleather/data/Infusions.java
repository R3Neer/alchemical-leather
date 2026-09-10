package io.github.r3neer.alchemicalleather.data;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import java.util.*;
public final class Infusions {
    public static final DataComponentType<Infusion> TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alchemical_leather","infusion"),DataComponentType.<Infusion>builder().persistent(Infusion.CODEC).networkSynchronized(Infusion.STREAM_CODEC).build());
    public static final DataComponentType<AnimalInfusion> ANIMAL_TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alchemical_leather","animal_infusion"),DataComponentType.<AnimalInfusion>builder().persistent(AnimalInfusion.CODEC).networkSynchronized(AnimalInfusion.STREAM_CODEC).build());
    public static final List<EquipmentSlot> HUMANOID_SLOTS=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    public static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.BODY);
    public static EquipmentSlot slot(ItemStack stack) {
        var equippable=stack.get(DataComponents.EQUIPPABLE);
        if(equippable==null||!equippable.slot().isArmor()||!DyeableArmorRules.dyeable(stack))return null;
        return equippable.slot();
    }
    public static boolean animalArmor(ItemStack stack){return slot(stack)==EquipmentSlot.BODY;}
    public static boolean accepts(ItemStack stack,EquipmentSlot actualSlot,Identifier effect) {
        var target=slot(stack);if(target!=actualSlot)return false;
        return actualSlot.getType()==EquipmentSlot.Type.ANIMAL_ARMOR||EffectSlotRules.slot(effect)==actualSlot;
    }
    public static List<Infusion> entries(ItemStack stack) {
        var animal=stack.get(ANIMAL_TYPE);if(animal!=null)return animal.effects();
        var single=stack.get(TYPE);return single==null?List.of():List.of(single);
    }
    public static boolean blocked(ItemStack stack) { return stack.has(TYPE)||stack.has(ANIMAL_TYPE); }
    public static boolean enchanted(ItemStack stack) {
        var normal=stack.get(DataComponents.ENCHANTMENTS); var stored=stack.get(DataComponents.STORED_ENCHANTMENTS);
        return normal!=null&&!normal.isEmpty() || stored!=null&&!stored.isEmpty();
    }
    public record Resolution(Infusion infusion,String error) { public boolean ok(){return infusion!=null;} }
    public record AnimalResolution(AnimalInfusion infusion,String error) { public boolean ok(){return infusion!=null;} }
    public static AnimalResolution resolveAll(PotionContents contents,Item bottle) {
        if(contents==null)return new AnimalResolution(null,"no_effect");
        var effects=new ArrayList<net.minecraft.world.effect.MobEffectInstance>();contents.getAllEffects().forEach(effects::add);
        if(effects.isEmpty())return new AnimalResolution(null,"no_effect");
        var resolved=new ArrayList<Infusion>(effects.size());
        for(var effect:effects) {
            var id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            String mode=effect.getEffect().value().isInstantaneous()?"instant":bottle==Items.LINGERING_POTION?"stable":"timed";
            if(mode.equals("timed")&&effect.getDuration()<=0)return new AnimalResolution(null,"invalid");
            resolved.add(new Infusion(id,effect.getAmplifier(),mode,mode.equals("timed")?effect.getDuration():0));
        }
        return new AnimalResolution(new AnimalInfusion(resolved),null);
    }
    public static Resolution resolve(PotionContents contents,Item bottle) {
        var all=resolveAll(contents,bottle);if(!all.ok())return new Resolution(null,all.error());
        if(all.infusion().effects().size()>1)return new Resolution(null,"multiple_effects");
        var infusion=all.infusion().effects().getFirst();
        if(EffectSlotRules.slot(infusion.effect())==null)return new Resolution(null,"unsupported");
        return new Resolution(infusion,null);
    }
    public static void initialize() { }
}
