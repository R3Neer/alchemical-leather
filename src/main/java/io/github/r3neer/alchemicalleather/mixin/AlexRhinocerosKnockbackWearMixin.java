package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.KnockbackWear;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve the exact random launch scale used by Alex 2.1.9 without consuming another RNG value. */
@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.entity.EntityRhinoceros",remap=false)
public abstract class AlexRhinocerosKnockbackWearMixin {
    @Unique private float alchemical$launchRandom=Float.NaN;

    @WrapOperation(method="launch",at=@At(value="INVOKE",target="Lnet/minecraft/util/RandomSource;nextFloat()F",remap=true),remap=false)
    private float alchemical$captureLaunchRandom(RandomSource random,Operation<Float> original,
                                                 Entity launch,float angle,float scale){
        float value=original.call(random);
        alchemical$launchRandom=value;
        return value;
    }

    @WrapOperation(method="launch",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D",remap=true),remap=false)
    private double alchemical$rhinoResistance(LivingEntity target,Holder<Attribute> attribute,Operation<Double> original,
                                               Entity launch,float angle,float scale){
        double withResistance=original.call(target,attribute);
        float draw=alchemical$launchRandom;
        alchemical$launchRandom=Float.NaN;
        if(attribute.equals(Attributes.KNOCKBACK_RESISTANCE)&&Float.isFinite(draw)){
            double base=1.0D+draw*0.5D*scale;
            KnockbackWear.emitMultiplicative(target,base,withResistance);
        }
        return withResistance;
    }
}
