package io.github.r3neer.alchemicalleather.effect;
import io.github.r3neer.alchemicalleather.data.*;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
public final class EquipmentInfusions {
    public static void sync(LivingEntity entity) {
        if(!(entity.level() instanceof ServerLevel level)||!entity.isAlive()||entity instanceof ArmorStand)return;
        // Player NBT/equipment can be loaded before ServerPlayer.connection exists. Any projection at that point
        // reaches ServerPlayer.onEffectAdded/onEffectUpdated and attempts to send through a null connection.
        // Defer the entire operation, including instant consumption and clock creation, until JOIN reconciliation.
        if(entity instanceof ServerPlayer player&&player.connection==null)return;
        boolean any=false;
        for(var slot:Infusions.SLOTS){var stack=entity.getItemBySlot(slot);if(Infusions.blocked(stack)&&Infusions.slot(stack)==slot){any=true;break;}}
        var ledger=((LedgerHolder)entity).alchemical$existingLedger();
        if(!any){if(ledger!=null){ledger.retainArmor(Set.of());ledger.equipmentManaged=false;}return;}
        if(ledger==null)ledger=EffectLedger.of(entity);
        ledger.equipmentManaged=true;
        Set<Holder<MobEffect>> wanted=new HashSet<>();
        for(var slot:Infusions.HUMANOID_SLOTS){syncHumanoid(level,entity,slot,entity.getItemBySlot(slot),ledger,wanted);if(!entity.isAlive())return;}
        syncAnimal(level,entity,entity.getItemBySlot(EquipmentSlot.BODY),ledger,wanted);
        if(entity.isAlive())ledger.retainArmor(wanted);
    }
    private static void syncHumanoid(ServerLevel level,LivingEntity entity,EquipmentSlot slot,ItemStack stack,EffectLedger ledger,Set<Holder<MobEffect>> wanted) {
        if(Infusions.slot(stack)!=slot)return;
        var remaining=new ArrayList<>(Infusions.entries(stack));
        for(int i=0;i<remaining.size();) {
            var infusion=remaining.get(i);var holder=infusion.holder();
            if(holder.isPresent()&&Infusions.accepts(stack,slot,infusion.effect())&&holder.get().value().isInstantaneous()) {
                remaining.remove(i);storeHumanoid(stack,remaining);
                holder.get().value().applyInstantaneousEffect(level,entity,entity,entity,infusion.amplifier(),1.0);
                if(!entity.isAlive())return;
            } else i++;
        }
        Map<Holder<MobEffect>,Infusion> winners=new HashMap<>();
        for(var infusion:Infusions.entries(stack)) {
            if(!infusion.valid()||infusion.mode().equals("instant")||!Infusions.accepts(stack,slot,infusion.effect()))continue;
            var holder=infusion.holder();if(holder.isEmpty()||holder.get().value().isInstantaneous())continue;
            winners.merge(holder.get(),infusion,EquipmentInfusions::stronger);
        }
        winners.forEach((holder,infusion)->{wanted.add(holder);ledger.setArmor(infusion.instance());});
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
    private static void retainWear(ItemStack stack,List<Infusion> effects){
        var progress=stack.get(Infusions.WEAR_TYPE);if(progress==null)return;
        var ids=new HashSet<Identifier>();for(var effect:effects)ids.add(effect.effect());var next=progress.retain(ids);
        if(next.isEmpty())stack.remove(Infusions.WEAR_TYPE);else stack.set(Infusions.WEAR_TYPE,next);
    }
    private static void storeHumanoid(ItemStack stack,List<Infusion> effects) {
        stack.remove(Infusions.TYPE);stack.remove(Infusions.HUMANOID_TYPE);
        if(effects.size()==1)stack.set(Infusions.TYPE,effects.getFirst());
        else if(effects.size()>1)stack.set(Infusions.HUMANOID_TYPE,new HumanoidInfusion(effects));
        retainWear(stack,effects);
    }
    private static void storeAnimal(ItemStack stack,List<Infusion> effects) {
        if(effects.isEmpty())stack.remove(Infusions.ANIMAL_TYPE);else stack.set(Infusions.ANIMAL_TYPE,new AnimalInfusion(effects));
        retainWear(stack,effects);
    }
    public static void saveClocks(LivingEntity entity,EffectLedger ledger) {
        if(!ledger.equipmentManaged||!entity.isAlive())return;
        boolean resync=false;
        for(var slot:Infusions.HUMANOID_SLOTS) {
            var stack=entity.getItemBySlot(slot);if(Infusions.slot(stack)!=slot)continue;
            var current=Infusions.entries(stack);if(current.isEmpty())continue;
            boolean changed=false,expired=false;var next=new ArrayList<Infusion>(current.size());
            for(var infusion:current) {
                if(infusion.mode().equals("timed")&&infusion.holder().isPresent()&&Infusions.accepts(stack,slot,infusion.effect())) {
                    int remaining=ledger.remaining(infusion.holder().get());
                    if(remaining<=0){changed=true;expired=true;continue;}
                    if(remaining!=infusion.remainingTicks()){changed=true;next.add(infusion.remaining(remaining));}else next.add(infusion);
                } else next.add(infusion);
            }
            if(changed){storeHumanoid(stack,next);resync|=expired;}
        }
        var stack=entity.getItemBySlot(EquipmentSlot.BODY);var bundle=stack.get(Infusions.ANIMAL_TYPE);
        if(bundle!=null&&Infusions.animalArmor(stack)){
            boolean changed=false,expired=false;var next=new ArrayList<Infusion>(bundle.effects().size());
            for(var infusion:bundle.effects()) {
                if(infusion.mode().equals("timed")&&infusion.holder().isPresent()&&Infusions.accepts(stack,EquipmentSlot.BODY,infusion.effect())) {
                    changed=true;int remaining=infusion.remainingTicks()-1;
                    if(remaining>0)next.add(infusion.remaining(remaining));else expired=true;
                } else next.add(infusion);
            }
            if(changed){storeAnimal(stack,next);resync|=expired;}
        }
        if(resync&&entity.isAlive())sync(entity);
    }
}
