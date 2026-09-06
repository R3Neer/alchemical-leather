package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import io.github.r3neer.alchemicalleather.effect.*;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class LivingEffectsMixin implements LedgerHolder {
    @Unique private EffectLedger alchemical$effects;
    @Override public EffectLedger alchemical$existingLedger() { return alchemical$effects; }
    @Override public EffectLedger alchemical$ledger() {
        if (alchemical$effects == null) alchemical$effects = new EffectLedger((LivingEntity)(Object)this);
        return alchemical$effects;
    }
    @WrapOperation(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At(value="INVOKE", target="Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object alchemical$capture(Map<?,?> map, Object key, Operation<Object> original, MobEffectInstance effect, Entity source) {
        if (alchemical$effects != null) alchemical$effects.externalAdd(effect, false);
        return original.call(map, key);
    }
    @WrapOperation(method="forceAddEffect", at=@At(value="INVOKE", target="Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object alchemical$force(Map<?,?> map, Object key, Object value, Operation<Object> original) {
        if (alchemical$effects != null) alchemical$effects.externalAdd((MobEffectInstance)value, true);
        return original.call(map, key, value);
    }
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at=@At("RETURN"))
    private void alchemical$afterAdd(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> ci) {
        if (alchemical$effects != null) alchemical$effects.reconcile();
    }
    @Inject(method="forceAddEffect", at=@At("RETURN"))
    private void alchemical$afterForce(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (alchemical$effects != null) alchemical$effects.reconcile();
    }
    @Inject(method="removeEffectNoUpdate", at=@At("RETURN"))
    private void alchemical$remove(Holder<MobEffect> id, CallbackInfoReturnable<MobEffectInstance> ci) {
        if (alchemical$effects != null && ci.getReturnValue() != null) alchemical$effects.externalRemoved(id);
    }
    @Inject(method="removeAllEffects", at=@At("RETURN"))
    private void alchemical$removeAll(CallbackInfoReturnable<Boolean> ci) {
        // Removed sources are captured by comparing against the remaining visible map.
        if (alchemical$effects != null) alchemical$effects.allRemoved();
    }
    @Inject(method="tickEffects", at=@At("HEAD"))
    private void alchemical$beforeTick(CallbackInfo ci) { if (alchemical$effects != null) { if(alchemical$effects.equipmentManaged) EquipmentInfusions.sync((LivingEntity)(Object)this); alchemical$effects.reconcile(); } }
    @Inject(method="tickEffects", at=@At("RETURN"))
    private void alchemical$afterTick(CallbackInfo ci) { if (alchemical$effects != null) { alchemical$effects.afterTick(); EquipmentInfusions.saveClocks((LivingEntity)(Object)this,alchemical$effects); } }
    @Inject(method="addAdditionalSaveData", at=@At("RETURN"))
    private void alchemical$save(ValueOutput output, CallbackInfo ci) {
        if (alchemical$effects != null) output.store("active_effects", MobEffectInstance.CODEC.listOf(), alchemical$effects.externalForSave());
    }
    @Inject(method="readAdditionalSaveData", at=@At("HEAD"))
    private void alchemical$load(CallbackInfo ci) { if(alchemical$effects!=null)alchemical$effects.clear(); }
    @Inject(method="triggerOnDeathMobEffects", at=@At("RETURN"))
    private void alchemical$death(CallbackInfo ci) { if (alchemical$effects != null) alchemical$effects.clear(); }
}
