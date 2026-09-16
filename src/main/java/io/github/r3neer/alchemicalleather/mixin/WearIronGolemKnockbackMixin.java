package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.KnockbackWear;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Iron Golem lift is a fixed 0.4F vertical impulse scaled by 1-KNOCKBACK_RESISTANCE. */
@Mixin(IronGolem.class)
public abstract class WearIronGolemKnockbackMixin {
    @WrapOperation(method="doHurtTarget",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double alchemical$golemResistance(LivingEntity target,Holder<Attribute> attribute,
                                              Operation<Double> original,ServerLevel level,Entity attacked){
        double withResistance=original.call(target,attribute);
        if(attribute.equals(Attributes.KNOCKBACK_RESISTANCE))
            KnockbackWear.emitMultiplicative(target,0.4F,withResistance);
        return withResistance;
    }
}
