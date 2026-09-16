package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.KnockbackWear;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Exact 2.1.9 Guster lift: mirror its float trajectory and clamp(1-resistance,0,1). */
@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.entity.EntityGuster",remap=false)
public abstract class AlexGusterKnockbackWearMixin {
    @Shadow private int liftingTime;

    @WrapOperation(method="aiStep",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D",remap=true),remap=false)
    private double alchemical$gusterResistance(LivingEntity lifted,Holder<Attribute> attribute,Operation<Double> original){
        double withResistance=original.call(lifted,attribute);
        if(!attribute.equals(Attributes.KNOCKBACK_RESISTANCE))return withResistance;
        Entity self=(Entity)(Object)this;
        float radius=1.0F+liftingTime*0.05F;
        float angle=liftingTime*-0.25F;
        double extraX=self.getX()+radius*Mth.sin(Mth.PI+angle);
        double extraZ=self.getZ()+radius*Mth.cos(angle);
        double dx=extraX-lifted.getX();
        double dz=extraZ-lifted.getZ();
        double base=Math.sqrt(dx*dx+0.01D+dz*dz);
        KnockbackWear.emitUnitClampedMultiplicative(lifted,base,withResistance);
        return withResistance;
    }
}
