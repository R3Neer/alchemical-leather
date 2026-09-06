package io.github.r3neer.alchemicalleather.effect;
import io.github.r3neer.alchemicalleather.data.*;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.server.level.ServerLevel;
public final class EquipmentInfusions {
    public static void sync(LivingEntity entity) {
        if(!(entity.level() instanceof ServerLevel level)||!entity.isAlive()||entity instanceof ArmorStand)return;
        boolean any=false;
        for(var slot:Infusions.SLOTS)if(entity.getItemBySlot(slot).has(Infusions.TYPE)){any=true;break;}
        var ledger=((LedgerHolder)entity).alchemical$existingLedger();
        if(!any){if(ledger!=null){ledger.retainArmor(Set.of());ledger.equipmentManaged=false;}return;}
        if(ledger==null)ledger=EffectLedger.of(entity);
        ledger.equipmentManaged=true;
        Set<Holder<MobEffect>> wanted=new HashSet<>();
        for(var slot:Infusions.SLOTS) {
            var stack=entity.getItemBySlot(slot); var infusion=stack.get(Infusions.TYPE);
            if(infusion==null||!infusion.valid()||Infusions.slot(stack)!=slot||EffectSlotRules.slot(infusion.effect())!=slot)continue;
            var holder=infusion.holder();if(holder.isEmpty())continue;
            if(holder.get().value().isInstantaneous()) {
                stack.remove(Infusions.TYPE);
                holder.get().value().applyInstantaneousEffect(level,entity,entity,entity,infusion.amplifier(),1.0);
                if(!entity.isAlive())return;
            } else if(!infusion.mode().equals("instant")) {
                wanted.add(holder.get());ledger.setArmor(infusion.instance());
            }
        }
        ledger.retainArmor(wanted);
    }
    public static void saveClocks(LivingEntity entity,EffectLedger ledger) {
        if(!ledger.equipmentManaged||!entity.isAlive())return;
        for(var slot:Infusions.SLOTS) {
            var stack=entity.getItemBySlot(slot);var infusion=stack.get(Infusions.TYPE);
            if(infusion==null||!infusion.mode().equals("timed")||Infusions.slot(stack)!=slot||EffectSlotRules.slot(infusion.effect())!=slot)continue;
            var holder=infusion.holder();if(holder.isEmpty())continue;
            int remaining=ledger.remaining(holder.get());
            if(remaining<0)continue;
            if(remaining==0)stack.remove(Infusions.TYPE);
            else if(remaining!=infusion.remainingTicks())stack.set(Infusions.TYPE,infusion.remaining(remaining));
        }
    }
}
