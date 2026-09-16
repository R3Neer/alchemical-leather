package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Alex's Mobs adapter for the exact successful Bug Pheromones target-veto boundary. */
@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents",remap=false)
public abstract class AlexBugPheromonesWearMixin {
    @Unique private static final Identifier BUG_PHEROMONES=Identifier.fromNamespaceAndPath("alexsmobs","bug_pheromones");
    @Unique private static final Identifier TARGET_REJECTION=Identifier.fromNamespaceAndPath("alchemical_leather","bug_pheromones_target_rejection");

    @Inject(method="fireChangeTarget",at=@At("RETURN"),remap=false)
    private static void alchemical$targetRejected(Mob mob,LivingEntity newTarget,CallbackInfoReturnable<Boolean> cir){
        if(!cir.getReturnValueZ()||mob==null||newTarget==null||!mob.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD))return;
        if(mob.getLastHurtByMob()==newTarget)return;
        Holder<MobEffect> holder=BuiltInRegistries.MOB_EFFECT.get(BUG_PHEROMONES).map(value->(Holder<MobEffect>)value).orElse(null);
        if(holder!=null&&newTarget.hasEffect(holder))InfusionWear.emitBuiltin(newTarget,holder,TARGET_REJECTION,1.0);
    }
}
