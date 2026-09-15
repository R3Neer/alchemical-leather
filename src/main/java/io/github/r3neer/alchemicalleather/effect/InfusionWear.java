package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.config.AlchemicalConfig;
import io.github.r3neer.alchemicalleather.data.*;
import io.github.r3neer.alchemicalleather.mixin.ItemStackDamageAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;

/** Server-authoritative conversion of attributable effect work into ordinary armor durability damage. */
public final class InfusionWear {
    private InfusionWear(){}

    public static void emitEvent(LivingEntity wearer,Holder<MobEffect> effect,Identifier event,double amount){
        emit(wearer,effect,"event",event,amount);
    }

    public static void emitBuiltin(LivingEntity wearer,Holder<MobEffect> effect,Identifier detector,double amount){
        emit(wearer,effect,"builtin",detector,amount);
    }

    private static void emit(LivingEntity wearer,Holder<MobEffect> effect,String type,Identifier source,double amount){
        if(!AlchemicalConfig.infusionWear()||wearer==null||effect==null||source==null||!Double.isFinite(amount)||amount<=0)return;
        if(!(wearer.level() instanceof ServerLevel))return;
        Identifier effectId=BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        if(effectId==null)return;
        var rule=WearRules.rule(effectId);if(rule==null||rule.none())return;
        double work=rule.work(type,source,amount);if(!Double.isFinite(work)||work<=0)return;
        var ledger=((LedgerHolder)wearer).alchemical$existingLedger();
        if(ledger==null||!ledger.armorEffective(effect))return;
        var owner=findOwner(wearer,ledger,effect,effectId);if(owner==null)return;
        apply(owner.stack(),wearer,owner.slot(),effectId,rule,work);
    }

    private record Owner(ItemStack stack,EquipmentSlot slot){}
    private static Owner findOwner(LivingEntity wearer,EffectLedger ledger,Holder<MobEffect> effect,Identifier effectId){
        EquipmentSlot slot=ledger.armorOwner(effect);
        if(slot==null)return null;
        var stack=wearer.getItemBySlot(slot);
        if(stack.isEmpty()||Infusions.slot(stack)!=slot||!Infusions.hasEffect(stack,effectId)||!Infusions.accepts(stack,slot,effectId))return null;
        return new Owner(stack,slot);
    }

    static void apply(ItemStack stack,LivingEntity wearer,EquipmentSlot slot,Identifier effect,WearRules.Rule rule,double addedWork){
        // MAX_DAMAGE/DAMAGE establish a real durability bar, but vanilla's explicit UNBREAKABLE
        // component remains authoritative. A compatibility mod may still mask isDamageableItem().
        if(!hasVanillaDamageBar(stack))return;
        var progress=stack.getOrDefault(Infusions.WEAR_TYPE,WearProgress.EMPTY);
        double total=progress.work(effect)+addedWork;
        int damage=(int)Math.floor(total/rule.workPerDamage());
        double remaining=total-damage*rule.workPerDamage();
        if(damage<=0){
            var next=progress.with(effect,total);if(next.isEmpty())stack.remove(Infusions.WEAR_TYPE);else stack.set(Infusions.WEAR_TYPE,next);return;
        }
        damageArmor(stack,wearer,slot,damage);
        if(stack.isEmpty()){
            EquipmentInfusions.sync(wearer);
            return;
        }
        var next=progress.with(effect,remaining);
        if(next.isEmpty())stack.remove(Infusions.WEAR_TYPE);else stack.set(Infusions.WEAR_TYPE,next);
    }

    /** Vanilla's structural damageability predicate before any mixin can override the method result. */
    static boolean hasVanillaDamageBar(ItemStack stack){
        return !stack.isEmpty()&&stack.has(DataComponents.MAX_DAMAGE)&&stack.has(DataComponents.DAMAGE)
            &&!stack.has(DataComponents.UNBREAKABLE)&&stack.getMaxDamage()>0;
    }

    /**
     * Prefer Minecraft's complete durability pipeline whenever the current mod stack exposes it.
     * Only when a compatibility mod masks an otherwise vanilla-damageable item do we bypass that
     * gate and rejoin vanilla at its terminal applyDamage method. This keeps Unbreaking/other
     * legitimate durability hooks intact in ordinary environments while preserving the explicit
     * alchemical operating cost under Enchancement's global durability-off policy.
     */
    static void damageArmor(ItemStack stack,LivingEntity wearer,EquipmentSlot slot,int amount){
        if(amount<=0||!hasVanillaDamageBar(stack))return;
        if(wearer instanceof ServerPlayer player&&player.hasInfiniteMaterials())return;
        if(stack.isDamageableItem()){
            stack.hurtAndBreak(amount,wearer,slot);
            return;
        }

        long rawDamage=(long)stack.getDamageValue()+amount;
        int newDamage=(int)Math.min(Integer.MAX_VALUE,rawDamage);
        ServerPlayer player=wearer instanceof ServerPlayer sp?sp:null;
        var brokenItem=stack.getItem();
        ((ItemStackDamageAccess)(Object)stack).alchemical$applyDamage(
            newDamage,player,broken->wearer.onEquippedItemBroken(broken,slot));

        // A durability-disabling mod can also make ItemStack#isBroken false inside applyDamage.
        // Complete exactly that suppressed break. In normal environments the branch above already
        // used hurtAndBreak, so this fallback cannot duplicate a vanilla break callback.
        if(!stack.isEmpty()&&stack.getDamageValue()>=stack.getMaxDamage()){
            stack.shrink(1);
            wearer.onEquippedItemBroken(brokenItem,slot);
        }
    }
}
