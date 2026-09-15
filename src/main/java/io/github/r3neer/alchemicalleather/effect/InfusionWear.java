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
        // Do not use isDamageableItem() here. Enchancement deliberately makes that return false
        // when its global durability switch is disabled, but alchemical wear is an explicit cost
        // that must still consume the item's ordinary DAMAGE/MAX_DAMAGE bar and be able to break it.
        if(stack.isEmpty()||stack.getMaxDamage()<=0)return;
        var progress=stack.getOrDefault(Infusions.WEAR_TYPE,WearProgress.EMPTY);
        double total=progress.work(effect)+addedWork;
        int damage=(int)Math.floor(total/rule.workPerDamage());
        double remaining=total-damage*rule.workPerDamage();
        if(damage<=0){
            var next=progress.with(effect,total);if(next.isEmpty())stack.remove(Infusions.WEAR_TYPE);else stack.set(Infusions.WEAR_TYPE,next);return;
        }
        damageArmorDirectly(stack,wearer,slot,damage);
        if(stack.isEmpty()){
            EquipmentInfusions.sync(wearer);
            return;
        }
        var next=progress.with(effect,remaining);
        if(next.isEmpty())stack.remove(Infusions.WEAR_TYPE);else stack.set(Infusions.WEAR_TYPE,next);
    }

    /**
     * Applies the explicit alchemical cost past ItemStack#isDamageableItem, then rejoins vanilla at
     * its terminal applyDamage path. This preserves vanilla's durability criterion, clamping,
     * shrink-on-break and equipment-break callback without re-enabling ordinary durability sources.
     */
    static void damageArmorDirectly(ItemStack stack,LivingEntity wearer,EquipmentSlot slot,int amount){
        if(amount<=0||stack.isEmpty()||stack.getMaxDamage()<=0)return;
        if(wearer instanceof ServerPlayer player&&player.hasInfiniteMaterials())return;
        long rawDamage=(long)stack.getDamageValue()+amount;
        int newDamage=(int)Math.min(Integer.MAX_VALUE,rawDamage);
        ServerPlayer player=wearer instanceof ServerPlayer sp?sp:null;
        ((ItemStackDamageAccess)(Object)stack).alchemical$applyDamage(
            newDamage,player,broken->wearer.onEquippedItemBroken(broken,slot));
    }
}
