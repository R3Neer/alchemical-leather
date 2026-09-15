package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.EffectSlotRules;
import io.github.r3neer.alchemicalleather.mixin.EffectAccess;
import io.github.r3neer.alchemicalleather.mixin.LivingEffectsAccess;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/** Separate clocks for external effects and every equipped armor source. Only the visible winner executes. */
public final class EffectLedger {
    private final LivingEntity entity;
    private final Map<Holder<MobEffect>, Entry> entries = new HashMap<>();
    private boolean internal;
    public boolean equipmentManaged;

    private record ArmorSource(MobEffectInstance effect,boolean eligible){}
    private record Winner(MobEffectInstance effect,EquipmentSlot armorSlot){}
    private static final class Entry {
        MobEffectInstance external;
        final EnumMap<EquipmentSlot,ArmorSource> armor=new EnumMap<>(EquipmentSlot.class);
        Entry(MobEffectInstance external){this.external=external;}
    }

    public EffectLedger(LivingEntity entity){this.entity=entity;}
    public static EffectLedger of(LivingEntity entity){return ((LedgerHolder)entity).alchemical$ledger();}
    public boolean managed(Holder<MobEffect> effect){return !internal&&entries.containsKey(effect);}

    public int remaining(Holder<MobEffect> id){
        var entry=entries.get(id);var best=entry==null?null:bestArmor(entry);
        return best==null?0:best.effect().getDuration();
    }
    public int remaining(Holder<MobEffect> id,EquipmentSlot slot){
        var entry=entries.get(id);var source=entry==null?null:entry.armor.get(slot);
        return source==null?0:source.effect().getDuration();
    }

    /** True only when an armor copy, rather than an external source, is the currently projected winner. */
    public boolean armorEffective(Holder<MobEffect> effect){
        var entry=entries.get(effect);var winner=entry==null?null:winner(entry);
        return !internal&&winner!=null&&winner.armorSlot()!=null;
    }

    /** Slot whose infused item owns the currently projected armor source, or null when armor does not win. */
    public EquipmentSlot armorOwner(Holder<MobEffect> effect){
        if(internal)return null;
        var entry=entries.get(effect);var winner=entry==null?null:winner(entry);
        return winner==null?null:winner.armorSlot();
    }

    public static MobEffectInstance copy(MobEffectInstance effect){
        if(effect==null)return null;
        var copy=new MobEffectInstance(effect);
        ((EffectAccess)copy).alchemical$setHidden(copy(((EffectAccess)effect).alchemical$getHidden()));
        return copy;
    }

    /** Legacy/test helper: production callers should always identify the owning equipment slot. */
    public void setArmor(MobEffectInstance armor){
        var id=BuiltInRegistries.MOB_EFFECT.getKey(armor.getEffect().value());
        var slot=id==null?null:EffectSlotRules.slot(id);
        setArmor(slot==null?EquipmentSlot.CHEST:slot,armor);
    }

    public void setArmor(EquipmentSlot slot,MobEffectInstance armor){
        var entry=entries.get(armor.getEffect());
        if(entry==null){entry=new Entry(copy(entity.getEffect(armor.getEffect())));entries.put(armor.getEffect(),entry);}
        entry.armor.put(slot,new ArmorSource(copy(armor),entity.canBeAffected(armor)));
        project(armor.getEffect(),entry);
    }

    public void removeArmor(Holder<MobEffect> effect){
        var entry=entries.remove(effect);
        if(entry!=null){entry.armor.clear();project(effect,entry);}
    }

    /**
     * Removes stale per-slot armor sources after an equipment reconciliation. This is intentionally
     * slot-aware: the same effect may legally exist in BODY and a humanoid slot at once.
     */
    public void retainArmorSlots(Map<Holder<MobEffect>,? extends Set<EquipmentSlot>> wanted){
        for(var iterator=entries.entrySet().iterator();iterator.hasNext();){
            var pair=iterator.next();var entry=pair.getValue();
            var allowed=wanted.get(pair.getKey());
            if(allowed==null)entry.armor.clear();
            else entry.armor.keySet().removeIf(slot->!allowed.contains(slot));
            project(pair.getKey(),entry);
            if(entry.armor.isEmpty())iterator.remove();
        }
    }

    /** Backwards-compatible effect-only retention used by older tests/callers. */
    public void retainArmor(Set<Holder<MobEffect>> wanted){
        var slots=new HashMap<Holder<MobEffect>,Set<EquipmentSlot>>();
        for(var effect:wanted){var entry=entries.get(effect);if(entry!=null)slots.put(effect,Set.copyOf(entry.armor.keySet()));}
        retainArmorSlots(slots);
    }

    public void externalAdd(MobEffectInstance effect,boolean force){
        var entry=entries.get(effect.getEffect());
        if(internal||entry==null)return;
        if(force||entry.external==null)entry.external=copy(effect);
        else entry.external.update(copy(effect));
    }
    public void externalRemoved(Holder<MobEffect> effect){if(!internal&&entries.containsKey(effect))entries.get(effect).external=null;}
    public void allRemoved(){entries.forEach((key,e)->{if(!entity.hasEffect(key))e.external=null;});}
    public void reconcile(){entries.forEach(this::project);}

    public void afterTick(){
        for(var iterator=entries.entrySet().iterator();iterator.hasNext();){
            var pair=iterator.next();var entry=pair.getValue();
            entry.external=advance(entry.external);
            for(var sourceIterator=entry.armor.entrySet().iterator();sourceIterator.hasNext();){
                var sourcePair=sourceIterator.next();var source=sourcePair.getValue();
                var advanced=advance(source.effect());
                if(advanced==null)sourceIterator.remove();
                else sourcePair.setValue(new ArmorSource(advanced,source.eligible()));
            }
            project(pair.getKey(),entry);
            if(entry.armor.isEmpty())iterator.remove();
        }
    }

    private static MobEffectInstance advance(MobEffectInstance effect){
        if(effect==null)return null;
        ((EffectAccess)effect).alchemical$tickDuration();
        ((EffectAccess)effect).alchemical$downgrade();
        return effect.getDuration()==0?null:effect;
    }

    private static ArmorSource bestArmor(Entry entry){
        ArmorSource best=null;
        for(var slot:EquipmentSlot.VALUES){
            var candidate=entry.armor.get(slot);
            if(candidate==null||!candidate.eligible())continue;
            if(best==null||stronger(candidate.effect(),best.effect()))best=candidate;
        }
        return best;
    }

    private static EquipmentSlot bestArmorSlot(Entry entry,ArmorSource best){
        if(best==null)return null;
        for(var slot:EquipmentSlot.VALUES)if(entry.armor.get(slot)==best)return slot;
        return null;
    }

    private static boolean stronger(MobEffectInstance candidate,MobEffectInstance current){
        if(candidate.getAmplifier()!=current.getAmplifier())return candidate.getAmplifier()>current.getAmplifier();
        if(candidate.isInfiniteDuration()!=current.isInfiniteDuration())return candidate.isInfiniteDuration();
        return candidate.getDuration()>current.getDuration();
    }

    private static Winner winner(Entry entry){
        var armor=bestArmor(entry);
        if(armor==null)return entry.external==null?null:new Winner(entry.external,null);
        var slot=bestArmorSlot(entry,armor);
        if(entry.external==null)return new Winner(armor.effect(),slot);
        var external=entry.external;
        if(armor.effect().getAmplifier()!=external.getAmplifier())
            return armor.effect().getAmplifier()>external.getAmplifier()?new Winner(armor.effect(),slot):new Winner(external,null);
        if(external.isInfiniteDuration())return new Winner(external,null);
        return armor.effect().isInfiniteDuration()||armor.effect().getDuration()>external.getDuration()
            ?new Winner(armor.effect(),slot):new Winner(external,null);
    }

    private void project(Holder<MobEffect> id,Entry entry){
        if(entity.level().isClientSide())return;
        var selected=winner(entry);var desired=selected==null?null:selected.effect();var current=entity.getEffect(id);
        var access=(LivingEffectsAccess)entity;
        boolean hasArmor=!entry.armor.isEmpty();
        internal=true;
        try{
            if(desired==null){
                if(current!=null){entity.getActiveEffectsMap().remove(id);access.alchemical$removed(List.of(current));}
            }else if(current==null||current.getAmplifier()!=desired.getAmplifier()){
                var next=hasArmor?new MobEffectInstance(desired):copy(desired); // No vanilla hidden chain while armor arbitration is active.
                if(current!=null)next.copyBlendState(current);
                entity.getActiveEffectsMap().put(id,next);
                if(current==null){access.alchemical$added(next,null);next.onEffectAdded(entity);next.onEffectStarted(entity);}
                else access.alchemical$updated(next,true,null);
            }else{
                ((EffectAccess)current).alchemical$setHidden(hasArmor?null:copy(((EffectAccess)desired).alchemical$getHidden()));
                if(current.getDuration()!=desired.getDuration()||current.isVisible()!=desired.isVisible()||current.isAmbient()!=desired.isAmbient()||current.showIcon()!=desired.showIcon()){
                    ((EffectAccess)current).alchemical$copyDetails(desired);access.alchemical$updated(current,false,null);
                }
            }
        }finally{internal=false;}
    }

    public List<MobEffectInstance> externalForSave(){
        var saved=new HashMap<>(entity.getActiveEffectsMap());
        entries.forEach((key,e)->{saved.remove(key);if(e.external!=null)saved.put(key,e.external);});
        return List.copyOf(saved.values());
    }
    public void clear(){entries.clear();equipmentManaged=false;}
}
