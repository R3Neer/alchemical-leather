package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.config.AlchemicalConfig;
import io.github.r3neer.alchemicalleather.data.*;
import io.github.r3neer.alchemicalleather.mixin.ItemStackDamageAccess;
import net.minecraft.core.Holder;
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
        var owner=findOwner(wearer,effectId);if(owner==null)return;
        apply(owner.stack(),wearer,owner.slot(),effectId,rule,work);
    }

    private record Owner(ItemStack stack,EquipmentSlot slot){}
    private static Owner findOwner(LivingEntity wearer,Identifier effect){
        for(var slot:Infusions.SLOTS){
            var stack=wearer.getItemBySlot(slot);
            if(stack.isEmpty()||Infusions.slot(stack)!=slot||!Infusions.hasEffect(stack,effect)||!Infusions.accepts(stack,slot,effect))continue;
            return new Owner(stack,slot);
        }
        return null;
    }

    static void apply(ItemStack stack,LivingEntity wearer,EquipmentSlot slot,Identifier effect,WearRules.Rule rule,double addedWork){
        // MAX_DAMAGE, not isDamageableItem(), is the durable-storage invariant. Enchancement may
        // deliberately report isDamageableItem=false while its global durability switch is off.
        if(stack.isEmpty()||stack.getMaxDamage()<=0)return;
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

    /**
     * Prefer Minecraft's complete durability pipeline whenever the current mod stack exposes it.
     * Only when a compatibility mod masks a MAX_DAMAGE item as non-damageable do we bypass that
     * gate and rejoin vanilla at its terminal applyDamage method. This keeps Unbreaking/other
     * legitimate durability hooks intact in ordinary environments while preserving the explicit
     * alchemical operating cost under Enchancement's global durability-off policy.
     */
    static void damageArmor(ItemStack stack,LivingEntity wearer,EquipmentSlot slot,int amount){
        if(amount<=0||stack.isEmpty()||stack.getMaxDamage()<=0)return;
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
        if(!stack.isEmpty()&&stack.getMaxDamage()>0&&stack.getDamageValue()>=stack.getMaxDamage()){
            stack.shrink(1);
            wearer.onEquippedItemBroken(brokenItem,slot);
        }
    }
}
