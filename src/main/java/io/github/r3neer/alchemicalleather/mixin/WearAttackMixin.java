package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.CombatWear;
import io.github.r3neer.alchemicalleather.effect.EffectAttributes;
import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import io.github.r3neer.alchemicalleather.effect.ReachWear;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Attributes accepted melee damage back to Strength/Weakness without charging misses or rejected hits. */
@Mixin(Player.class)
public abstract class WearAttackMixin {
    private static final Identifier ATTACK_DELTA=Identifier.fromNamespaceAndPath("alchemical_leather","attack_damage_delta");

    /**
     * Player#attack captures attackStrengthScale before onAttack() resets the attack ticker. The
     * hurtOrSimulate call happens after that reset, so reading getAttackStrengthScale() from the
     * wrapper would reconstruct the wrong damage scale. The critical bit is filled from vanilla's
     * own canCriticalAttack decision before the damage call.
     */
    @Unique private AttackContext alchemical$attackContext;
    @Unique private record AttackContext(float strengthScale,boolean attributeDriven,boolean critical){}

    @Inject(method="attack",at=@At("HEAD"))
    private void alchemical$captureAttackContext(Entity target,CallbackInfo ci){
        var self=(Player)(Object)this;
        alchemical$attackContext=new AttackContext(self.getAttackStrengthScale(0.5F),!self.isAutoSpinAttack(),false);
    }

    @WrapOperation(method="attack",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/player/Player;canCriticalAttack(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean alchemical$captureCriticalDecision(Player instance,Entity target,Operation<Boolean> original){
        boolean critical=original.call(instance,target);
        var context=alchemical$attackContext;
        if(context!=null)alchemical$attackContext=new AttackContext(context.strengthScale(),context.attributeDriven(),critical);
        return critical;
    }

    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean alchemical$attackDamage(Entity target,DamageSource source,float totalDamage,Operation<Boolean> original,Entity attacked){
        var self=(Player)(Object)this;
        // Snapshot before delegating: hurtServer mutates lastHurt/invulnerableTime, and damage
        // callbacks may also re-enter Player#attack and overwrite the per-player attack context.
        AttackContext context=alchemical$attackContext;
        LivingEntity livingTarget=target instanceof LivingEntity living?living:null;
        int invulnerableTime=livingTarget==null?0:livingTarget.invulnerableTime;
        float lastHurt=livingTarget==null?0.0F:((LivingHurtAccess)livingTarget).alchemical$getLastHurt();
        boolean bypassesCooldown=livingTarget==null||source.is(DamageTypeTags.BYPASSES_COOLDOWN);

        boolean accepted=original.call(target,source,totalDamage);
        if(!accepted||self.level().isClientSide())return accepted;

        if(context!=null&&context.attributeDriven()){
            float attackStrength=context.strengthScale();
            double scale=0.2D+attackStrength*attackStrength*0.8D;
            if(context.critical())scale*=1.5D;

            var strength=self.getEffect(MobEffects.STRENGTH);
            if(strength!=null){
                double contribution=Math.max(0.0D,EffectAttributes.contribution(self,Attributes.ATTACK_DAMAGE,strength))*scale;
                double work=livingTarget==null?Math.min(contribution,Math.max(0.0D,totalDamage)):
                    CombatWear.attackDeltaWork(totalDamage,contribution,invulnerableTime,lastHurt,bypassesCooldown);
                emit(self,strength,work);
            }
            var weakness=self.getEffect(MobEffects.WEAKNESS);
            if(weakness!=null){
                double suppression=Math.max(0.0D,-EffectAttributes.contribution(self,Attributes.ATTACK_DAMAGE,weakness))*scale;
                double work=livingTarget==null?suppression:
                    CombatWear.attackDeltaWork(totalDamage,-suppression,invulnerableTime,lastHurt,bypassesCooldown);
                emit(self,weakness,work);
            }
        }

        // Explicit 26.2 ATTACK_RANGE components replace ENTITY_INTERACTION_RANGE for attacks.
        // An active reach effect is therefore redundant for this exact action when such a component
        // is present, even if the target lies outside the ordinary interaction range.
        if(ReachWear.attackUsesInteractionRange(self.getMainHandItem()))
            ReachWear.emit(self,Attributes.ENTITY_INTERACTION_RANGE,target.getBoundingBox().distanceToSqr(self.getEyePosition()));
        return accepted;
    }

    @Inject(method="attack",at=@At("RETURN"))
    private void alchemical$clearAttackContext(Entity target,CallbackInfo ci){alchemical$attackContext=null;}

    private static void emit(Player player,MobEffectInstance effect,double work){
        if(effect!=null&&Double.isFinite(work)&&work>0)
            InfusionWear.emitBuiltin(player,effect.getEffect(),ATTACK_DELTA,work);
    }
}
