package io.github.r3neer.alchemicalleather.data;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import java.util.*;
public final class Infusions {
    public static final DataComponentType<Infusion> TYPE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
        Identifier.fromNamespaceAndPath("alchemical_leather","infusion"),DataComponentType.<Infusion>builder().persistent(Infusion.CODEC).networkSynchronized(Infusion.STREAM_CODEC).build());
    public static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    public static EquipmentSlot slot(ItemStack stack) {
        if(stack.is(Items.LEATHER_HELMET)) return EquipmentSlot.HEAD;
        if(stack.is(Items.LEATHER_CHESTPLATE)) return EquipmentSlot.CHEST;
        if(stack.is(Items.LEATHER_LEGGINGS)) return EquipmentSlot.LEGS;
        if(stack.is(Items.LEATHER_BOOTS)) return EquipmentSlot.FEET;
        return null;
    }
    public static boolean blocked(ItemStack stack) { return stack.has(TYPE); }
    public static boolean enchanted(ItemStack stack) {
        var normal=stack.get(DataComponents.ENCHANTMENTS); var stored=stack.get(DataComponents.STORED_ENCHANTMENTS);
        return normal!=null&&!normal.isEmpty() || stored!=null&&!stored.isEmpty();
    }
    public record Resolution(Infusion infusion,String error) { public boolean ok(){return infusion!=null;} }
    public static Resolution resolve(PotionContents contents,Item bottle) {
        if(contents==null) return new Resolution(null,"no_effect");
        var effects=new ArrayList<net.minecraft.world.effect.MobEffectInstance>(); contents.getAllEffects().forEach(effects::add);
        if(effects.size()>1) return new Resolution(null,"multiple_effects");
        if(effects.isEmpty()) return new Resolution(null,"no_effect");
        var effect=effects.getFirst(); var id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        if(EffectSlotRules.slot(id)==null) return new Resolution(null,"unsupported");
        String mode=effect.getEffect().value().isInstantaneous()?"instant":bottle==Items.LINGERING_POTION?"stable":"timed";
        if(mode.equals("timed") && effect.getDuration()<=0) return new Resolution(null,"invalid");
        return new Resolution(new Infusion(id,effect.getAmplifier(),mode,mode.equals("timed")?effect.getDuration():0),null);
    }
    public static void initialize() { }
}
