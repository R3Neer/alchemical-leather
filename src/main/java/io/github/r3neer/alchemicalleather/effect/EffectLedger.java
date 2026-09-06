package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.mixin.EffectAccess;
import io.github.r3neer.alchemicalleather.mixin.LivingEffectsAccess;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;

/** Separate clocks for armor and vanilla's external chain. Only the visible winner executes. */
public final class EffectLedger {
    private final LivingEntity entity;
    private final Map<Holder<MobEffect>, Entry> entries = new HashMap<>();
    private boolean internal;
    public boolean equipmentManaged;
    public int remaining(Holder<MobEffect> id) { var e=entries.get(id);return e==null?0:e.armor==null?0:e.armor.getDuration(); }
    public void retainArmor(Set<Holder<MobEffect>> wanted) { for(var id:List.copyOf(entries.keySet()))if(!wanted.contains(id))removeArmor(id); }
    private static final class Entry {
        MobEffectInstance external;
        MobEffectInstance armor;
        boolean eligible=true;
        Entry(MobEffectInstance external, MobEffectInstance armor) { this.external = external; this.armor = armor; }
    }
    public EffectLedger(LivingEntity entity) { this.entity = entity; }
    public static EffectLedger of(LivingEntity entity) { return ((LedgerHolder) entity).alchemical$ledger(); }
    public boolean managed(Holder<MobEffect> effect) { return !internal && entries.containsKey(effect); }
    public static MobEffectInstance copy(MobEffectInstance effect) {
        if (effect == null) return null;
        var copy = new MobEffectInstance(effect);
        ((EffectAccess) copy).alchemical$setHidden(copy(((EffectAccess) effect).alchemical$getHidden()));
        return copy;
    }
    public void setArmor(MobEffectInstance armor) {
        var entry = entries.get(armor.getEffect());
        if (entry == null) {
            entry = new Entry(copy(entity.getEffect(armor.getEffect())), copy(armor));
            entry.eligible=entity.canBeAffected(armor);
            entries.put(armor.getEffect(), entry);
        } else entry.armor = copy(armor);
        project(armor.getEffect(), entry);
    }
    public void removeArmor(Holder<MobEffect> effect) {
        var entry = entries.remove(effect);
        if (entry != null) { entry.armor = null; project(effect, entry); }
    }
    public void externalAdd(MobEffectInstance effect, boolean force) {
        var entry = entries.get(effect.getEffect());
        if (internal || entry == null) return;
        if (force || entry.external == null) entry.external = copy(effect);
        else entry.external.update(copy(effect));
    }
    public void externalRemoved(Holder<MobEffect> effect) {
        if (!internal && entries.containsKey(effect)) entries.get(effect).external = null;
    }
    public void allRemoved() {
        entries.forEach((key,e) -> { if (!entity.hasEffect(key)) e.external = null; });
    }
    public void reconcile() { entries.forEach(this::project); }
    public void afterTick() {
        for (var iterator = entries.entrySet().iterator(); iterator.hasNext();) {
            var pair = iterator.next(); var e = pair.getValue();
            e.external = advance(e.external); e.armor = advance(e.armor);
            project(pair.getKey(), e);
            if (e.armor == null) iterator.remove();
        }
    }
    private static MobEffectInstance advance(MobEffectInstance effect) {
        if (effect == null) return null;
        ((EffectAccess) effect).alchemical$tickDuration();
        ((EffectAccess) effect).alchemical$downgrade();
        return effect.getDuration() == 0 ? null : effect;
    }
    private static MobEffectInstance winner(Entry e) {
        if (e.armor == null || !e.eligible) return e.external;
        if (e.external == null) return e.armor;
        if (e.armor.getAmplifier() != e.external.getAmplifier())
            return e.armor.getAmplifier() > e.external.getAmplifier() ? e.armor : e.external;
        if (e.external.isInfiniteDuration()) return e.external;
        return e.armor.isInfiniteDuration() || e.armor.getDuration() > e.external.getDuration() ? e.armor : e.external;
    }
    private void project(Holder<MobEffect> id, Entry entry) {
        if (entity.level().isClientSide()) return;
        var desired = winner(entry); var current = entity.getEffect(id);
        var access = (LivingEffectsAccess) entity;
        internal = true;
        try {
            if (desired == null) {
                if (current != null) { entity.getActiveEffectsMap().remove(id); access.alchemical$removed(List.of(current)); }
            } else if (current == null || current.getAmplifier() != desired.getAmplifier()) {
                var next = entry.armor == null ? copy(desired) : new MobEffectInstance(desired); // Deliberately no hidden chain in the projection.
                if (current != null) next.copyBlendState(current);
                entity.getActiveEffectsMap().put(id, next);
                if (current == null) {
                    access.alchemical$added(next, null); next.onEffectAdded(entity); next.onEffectStarted(entity);
                } else access.alchemical$updated(next, true, null);
            } else {
                ((EffectAccess) current).alchemical$setHidden(entry.armor == null ? copy(((EffectAccess)desired).alchemical$getHidden()) : null);
                if (current.getDuration() != desired.getDuration() || current.isVisible()!=desired.isVisible() || current.isAmbient()!=desired.isAmbient() || current.showIcon()!=desired.showIcon()) {
                    ((EffectAccess) current).alchemical$copyDetails(desired);
                    access.alchemical$updated(current, false, null);
                }
            }
        } finally { internal = false; }
    }
    public List<MobEffectInstance> externalForSave() {
        var saved = new HashMap<>(entity.getActiveEffectsMap());
        entries.forEach((key,e) -> { saved.remove(key); if (e.external != null) saved.put(key, e.external); });
        return List.copyOf(saved.values());
    }
    public void clear() { entries.clear(); equipmentManaged=false; }
}
