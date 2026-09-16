package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.KnockbackWear;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Mace smash uses KNOCKBACK_RESISTANCE only on its distance-scaled horizontal impulse. */
@Mixin(MaceItem.class)
public abstract class WearMaceKnockbackMixin {
    @WrapOperation(method="getKnockbackPower",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private static double alchemical$maceResistance(LivingEntity nearby,Holder<Attribute> attribute,
                                                     Operation<Double> original,Entity attacker,
                                                     LivingEntity target,Vec3 direction){
        double withResistance=original.call(nearby,attribute);
        if(attribute.equals(Attributes.KNOCKBACK_RESISTANCE)){
            double base=(3.5D-direction.length())*0.7D*(attacker.fallDistance>5.0D?2.0D:1.0D);
            KnockbackWear.emitMultiplicative(nearby,base,withResistance);
        }
        return withResistance;
    }
}
