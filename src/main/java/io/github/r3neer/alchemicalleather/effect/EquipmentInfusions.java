package io.github.r3neer.alchemicalleather.effect;
import io.github.r3neer.alchemicalleather.data.*;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
public final class EquipmentInfusions {
    public static void sync(LivingEntity entity) {
        if(!(entity.level() instanceof ServerLevel level)||!entity.isAlive()||entity instanceof ArmorStand)return;
        boolean any=false;
        for(var slot:Infusions.SLOTS){var stack=entity.getItemBySlot(slot);if(Infusions.blocked(stack)&&Infusions.slot(stack)==slot){any=true;break;}}
        var ledger=((LedgerHolder)entity).alchemical$existingLedger();
        if(!any){if(ledger!=null){ledger.retainArmor(Set.of());ledger.equipmentManaged=false;}return;}
        if(ledger==null)ledger=EffectLedger.of(entity);
        ledger.equipmentManaged=true;
        Set<Holder<MobEffect>> wanted=new HashSet<>();
        for(var slot:Infusions.HUMANOID_SLOTS)syncSingle(level,entity,slot,entity.getItemBySlot(slot),ledger,wanted);
        syncAnimal(level,entity,entity.getItemBySlot(EquipmentSlot.BODY),ledger,wanted);
        if(entity.isAlive())ledger.retainArmor(wanted);
    }
    private static void syncSingle(ServerLevel level,LivingEntity entity,EquipmentSlot slot,ItemStack stack,EffectLedger ledger,Set<Holder<MobEffect>> wanted) {
        var infusion=stack.get(Infusions.TYPE);
        if(infusion==null||!infusion.valid()||!Infusions.accepts(stack,slot,infusion.effect()))return;
        var holder=infusion.holder();if(holder.isEmpty())return;
        if(holder.get().value().isInstantaneous()) {
            stack.remove(Infusions.TYPE);
            holder.get().value().applyInstantaneousEffect(level,entity,entity,entity,infusion.amplifier(),1.0);
        } else if(!infusion.mode().equals("instant")) {
            wanted.add(holder.get());ledger.setArmor(infusion.instance());
        }
    }
    private static void syncAnimal(ServerLevel level,LivingEntity entity,ItemStack stack,EffectLedger ledger,Set<Holder<MobEffect>> wanted) {
        var bundle=stack.get(Infusions.ANIMAL_TYPE);
        if(bundle==null||!bundle.valid()||!Infusions.animalArmor(stack))return;
        var remaining=new ArrayList<>(bundle.effects());
        for(int i=0;i<remaining.size();) {
            var infusion=remaining.get(i);var holder=infusion.holder();
            if(holder.isPresent()&&Infusions.accepts(stack,EquipmentSlot.BODY,infusion.effect())&&holder.get().value().isInstantaneous()) {
                remaining.remove(i);storeAnimal(stack,remaining);
                holder.get().value().applyInstantaneousEffect(level,entity,entity,entity,infusion.amplifier(),1.0);
                if(!entity.isAlive())return;
            } else i++;
        }
        bundle=stack.get(Infusions.ANIMAL_TYPE);if(bundle==null)return;
        Map<Holder<MobEffect>,Infusion> winners=new HashMap<>();
        for(var infusion:bundle.effects()) {
            if(!infusion.valid()||infusion.mode().equals("instant")||!Infusions.accepts(stack,EquipmentSlot.BODY,infusion.effect()))continue;
            var holder=infusion.holder();if(holder.isEmpty()||holder.get().value().isInstantaneous())continue;
            winners.merge(holder.get(),infusion,EquipmentInfusions::stronger);
        }
        winners.forEach((holder,infusion)->{wanted.add(holder);ledger.setArmor(infusion.instance());});
    }
    private static Infusion stronger(Infusion first,Infusion second) {
        if(first.amplifier()!=second.amplifier())return first.amplifier()>second.amplifier()?first:second;
        boolean firstStable=first.mode().equals("stable"),secondStable=second.mode().equals("stable");
        if(firstStable!=secondStable)return firstStable?first:second;
        return first.remainingTicks()>=second.remainingTicks()?first:second;
    }
    private static void storeAnimal(ItemStack stack,List<Infusion> effects) {
        if(effects.isEmpty())stack.remove(Infusions.ANIMAL_TYPE);else stack.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(effects));
    }
    public static void saveClocks(LivingEntity entity,EffectLedger ledger) {
        if(!ledger.equipmentManaged||!entity.isAlive())return;
        for(var slot:Infusions.HUMANOID_SLOTS) {
            var stack=entity.getItemBySlot(slot);var infusion=stack.get(Infusions.TYPE);
            if(infusion==null||!infusion.mode().equals("timed")||!Infusions.accepts(stack,slot,infusion.effect()))continue;
            var holder=infusion.holder();if(holder.isEmpty())continue;
            int remaining=ledger.remaining(holder.get());
            if(remaining<0)continue;
            if(remaining==0)stack.remove(Infusions.TYPE);
            else if(remaining!=infusion.remainingTicks())stack.set(Infusions.TYPE,infusion.remaining(remaining));
        }
        var stack=entity.getItemBySlot(EquipmentSlot.BODY);var bundle=stack.get(Infusions.ANIMAL_TYPE);
        if(bundle==null||!Infusions.animalArmor(stack))return;
        boolean changed=false;var next=new ArrayList<Infusion>(bundle.effects().size());
        for(var infusion:bundle.effects()) {
            if(infusion.mode().equals("timed")&&infusion.holder().isPresent()&&Infusions.accepts(stack,EquipmentSlot.BODY,infusion.effect())) {
                changed=true;int remaining=infusion.remainingTicks()-1;if(remaining>0)next.add(infusion.remaining(remaining));
            } else next.add(infusion);
        }
        if(changed)storeAnimal(stack,next);
    }
}
