package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.EffectAttributes;
import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import io.github.r3neer.alchemicalleather.effect.ReachWear;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Attributes accepted melee damage back to Strength/Weakness without charging misses or rejected hits. */
@Mixin(Player.class)
public abstract class WearAttackMixin {
    private static final Identifier ATTACK_DELTA=Identifier.fromNamespaceAndPath("alchemical_leather","attack_damage_delta");

    @WrapOperation(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean alchemical$attackDamage(Entity target,DamageSource source,float totalDamage,Operation<Boolean> original,Entity attacked){
        var self=(Player)(Object)this;
        boolean accepted=original.call(target,source,totalDamage);
        if(!accepted||self.level().isClientSide())return accepted;

        float attackStrength=self.getAttackStrengthScale(0.5F);
        double scale=0.2D+attackStrength*attackStrength*0.8D;
        boolean critical=attackStrength>0.9F&&self.fallDistance>0.0F&&!self.onGround()&&!self.onClimbable()
            &&!self.isInWater()&&!self.isMobilityRestricted()&&!self.isPassenger()&&target instanceof LivingEntity&&!self.isSprinting();
        if(critical)scale*=1.5D;

        var strength=self.getEffect(MobEffects.STRENGTH);
        if(strength!=null){
            double contribution=Math.max(0.0D,EffectAttributes.contribution(self,Attributes.ATTACK_DAMAGE,strength));
            emit(self,strength,contribution*scale,totalDamage);
        }
        var weakness=self.getEffect(MobEffects.WEAKNESS);
        if(weakness!=null){
            double suppression=Math.max(0.0D,-EffectAttributes.contribution(self,Attributes.ATTACK_DAMAGE,weakness));
            emit(self,weakness,suppression*scale,Double.POSITIVE_INFINITY);
        }
        ReachWear.emit(self,Attributes.ENTITY_INTERACTION_RANGE,target.getBoundingBox().distanceToSqr(self.getEyePosition()));
        return accepted;
    }

    private static void emit(Player player,MobEffectInstance effect,double work,double cap){
        if(effect==null||!Double.isFinite(work)||work<=0)return;
        if(Double.isFinite(cap))work=Math.min(work,Math.max(0.0D,cap));
        if(work>0)InfusionWear.emitBuiltin(player,effect.getEffect(),ATTACK_DELTA,work);
    }
}
