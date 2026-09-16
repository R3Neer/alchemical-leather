package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.KnockbackWear;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Hoglin throw subtracts KNOCKBACK_RESISTANCE from ATTACK_KNOCKBACK before both random components. */
@Mixin(HoglinBase.class)
public abstract class WearHoglinKnockbackMixin {
    @WrapOperation(method="throwTarget",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D",ordinal=1))
    private static double alchemical$hoglinResistance(LivingEntity target,Holder<Attribute> attribute,
                                                       Operation<Double> original,LivingEntity body,
                                                       LivingEntity attacked){
        double withResistance=original.call(target,attribute);
        if(attribute.equals(Attributes.KNOCKBACK_RESISTANCE)){
            double base=body.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
            KnockbackWear.emitSubtractive(target,base,withResistance);
        }
        return withResistance;
    }
}
