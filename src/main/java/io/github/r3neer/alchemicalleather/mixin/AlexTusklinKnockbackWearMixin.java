package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.KnockbackWear;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Exact 2.1.9 Tusklin fling: target resistance scales the signed live 0.4/0.9 launch strength. */
@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.entity.EntityTusklin",remap=false)
public abstract class AlexTusklinKnockbackWearMixin {
    @Shadow(remap=false)
    private float getLaunchStrength(){throw new AssertionError("mixin shadow");}

    @WrapOperation(method="tick",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D",remap=true),remap=false)
    private double alchemical$tusklinResistance(LivingEntity target,Holder<Attribute> attribute,Operation<Double> original){
        double withResistance=original.call(target,attribute);
        if(attribute.equals(Attributes.KNOCKBACK_RESISTANCE))
            KnockbackWear.emitSignedMultiplicative(target,getLaunchStrength(),withResistance);
        return withResistance;
    }
}
