package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Optional Alex's Mobs adapter. The target class need not exist in standalone Alchemical Leather. */
@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.event.ServerEvents",remap=false)
public abstract class AlexSoulstealWearMixin {
    @Unique private static final Identifier SOULSTEAL=Identifier.fromNamespaceAndPath("alexsmobs","soulsteal");
    @Unique private static final Identifier HEALING=Identifier.fromNamespaceAndPath("alchemical_leather","soulsteal_healing");

    /**
     * Wrap only Soulsteal's own heal call. The same Alex handler can later damage the attacker via
     * Spiked Turtle Shell retaliation; a HEAD/RETURN health delta would incorrectly subtract that
     * unrelated retaliation from the amount of healing Soulsteal actually performed.
     */
    @WrapOperation(
        method="onLivingDamageEvent",
        at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;heal(F)V"),
        remap=false
    )
    private void alchemical$soulstealHeal(LivingEntity attacker,float requested,Operation<Void> original){
        float before=attacker.getHealth();
        original.call(attacker,requested);
        float healed=attacker.getHealth()-before;
        if(healed<=0)return;
        Holder<MobEffect> holder=BuiltInRegistries.MOB_EFFECT.get(SOULSTEAL).map(value->(Holder<MobEffect>)value).orElse(null);
        if(holder!=null&&attacker.hasEffect(holder))InfusionWear.emitBuiltin(attacker,holder,HEALING,healed);
    }
}
