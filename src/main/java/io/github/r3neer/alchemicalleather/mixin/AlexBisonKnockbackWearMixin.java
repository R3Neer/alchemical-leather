package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.KnockbackWear;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Exact 2.1.9 Bison launch: target resistance scales the signed 0.6/4.0 horizontal contribution. */
@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.entity.EntityBison",remap=false)
public abstract class AlexBisonKnockbackWearMixin {
    @WrapOperation(method="launch",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D",remap=true),remap=false)
    private double alchemical$bisonResistance(LivingEntity target,Holder<Attribute> attribute,Operation<Double> original,
                                               Entity launch,boolean huge){
        double withResistance=original.call(target,attribute);
        if(attribute.equals(Attributes.KNOCKBACK_RESISTANCE))
            KnockbackWear.emitSignedMultiplicative(target,huge?4.0D:0.6D,withResistance);
        return withResistance;
    }
}
