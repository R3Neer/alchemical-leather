package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.KnockbackWear;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sonic Boom consumes KNOCKBACK_RESISTANCE twice inside a compiler-generated lambda. Rather than
 * bind compatibility to an unstable lambda$... method name, wrap the second synchronous
 * Optional#ifPresent in tick: it is the filtered attack-target action that contains hurt + push.
 */
@Mixin(SonicBoom.class)
public abstract class WearSonicBoomKnockbackMixin {
    @SuppressWarnings({"rawtypes","unchecked"})
    @WrapOperation(method="tick",at=@At(value="INVOKE",
        target="Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V",ordinal=1))
    private void alchemical$sonicBoomKnockback(Optional optional,Consumer consumer,Operation<Void> original,
                                               ServerLevel level,Warden body,long timestamp){
        Object selected=optional.orElse(null);
        LivingEntity target=selected instanceof LivingEntity living?living:null;
        if(target==null){original.call(optional,consumer);return;}

        DamageSource beforeSource=target.getLastDamageSource();
        Vec3 source=body.position().add(body.getAttachments().get(EntityAttachment.WARDEN_CHEST,0,body.getYRot()));
        Vec3 delta=target.getEyePosition().subtract(source);
        Vec3 direction=delta.lengthSqr()>1.0E-12D?delta.normalize():Vec3.ZERO;
        double horizontal=direction.horizontalDistance();
        double baseMagnitude=Math.hypot(2.5D*horizontal,0.5D*Math.abs(direction.y));

        original.call(optional,consumer);

        // The lambda reads resistance and pushes only when hurtServer returned true. sonicBoom()
        // creates a fresh DamageSource, so a changed matching source is the stable success witness.
        DamageSource afterSource=target.getLastDamageSource();
        if(afterSource==beforeSource||afterSource==null||!afterSource.is(DamageTypes.SONIC_BOOM)||afterSource.getEntity()!=body)return;
        if(baseMagnitude<=0.0D||!Double.isFinite(baseMagnitude))return;
        KnockbackWear.emitMultiplicative(target,baseMagnitude,target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }
}
