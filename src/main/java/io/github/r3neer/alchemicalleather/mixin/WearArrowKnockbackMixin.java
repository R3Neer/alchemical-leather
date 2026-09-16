package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.KnockbackWear;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Arrow weapon knockback is scaled horizontally by 0.6 * (1-KNOCKBACK_RESISTANCE). */
@Mixin(AbstractArrow.class)
public abstract class WearArrowKnockbackMixin {
    @Unique private double alchemical$arrowKnockback;

    @WrapOperation(method="doKnockback",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyKnockback(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float alchemical$captureArrowKnockback(ServerLevel level,ItemStack weapon,net.minecraft.world.entity.Entity target,
                                                    DamageSource source,float base,Operation<Float> original){
        float result=original.call(level,weapon,target,source,base);
        alchemical$arrowKnockback=result;
        return result;
    }

    @WrapOperation(method="doKnockback",at=@At(value="INVOKE",
        target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double alchemical$arrowResistance(LivingEntity target,Holder<Attribute> attribute,Operation<Double> original,
                                              LivingEntity mob,DamageSource source){
        double withResistance=original.call(target,attribute);
        if(attribute.equals(Attributes.KNOCKBACK_RESISTANCE)&&alchemical$arrowKnockback>0.0)
            KnockbackWear.emitMultiplicative(target,alchemical$arrowKnockback*0.6D,withResistance);
        alchemical$arrowKnockback=0.0D;
        return withResistance;
    }
}
