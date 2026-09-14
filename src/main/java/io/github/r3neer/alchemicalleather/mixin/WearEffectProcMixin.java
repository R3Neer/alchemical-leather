package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Measures vanilla effect procs from changes created synchronously by the effect hook itself. */
@Mixin(MobEffectInstance.class)
public abstract class WearEffectProcMixin {
    private static final Identifier PROC=Identifier.fromNamespaceAndPath("alchemical_leather","effect_proc");

    @WrapOperation(method="onMobHurt",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffect;onMobHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;ILnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void alchemical$hurtProc(MobEffect effect,ServerLevel level,LivingEntity mob,int amplifier,DamageSource source,float damage,Operation<Void> original){
        var instance=(MobEffectInstance)(Object)this;
        int before=instance.getEffect().equals(MobEffects.INFESTED)?countSilverfish(level,mob):0;
        original.call(effect,level,mob,amplifier,source,damage);
        if(instance.getEffect().equals(MobEffects.INFESTED)){
            int spawned=countSilverfish(level,mob)-before;
            if(spawned>0)InfusionWear.emitBuiltin(mob,instance.getEffect(),PROC,spawned);
        }
    }

    @WrapOperation(method="onMobRemoved",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffect;onMobRemoved(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;ILnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void alchemical$deathProc(MobEffect effect,ServerLevel level,LivingEntity mob,int amplifier,Entity.RemovalReason reason,Operation<Void> original){
        var instance=(MobEffectInstance)(Object)this;var holder=instance.getEffect();
        int beforeSlimes=holder.equals(MobEffects.OOZING)?countSlimes(level,mob):0;
        int beforeWebs=holder.equals(MobEffects.WEAVING)?countWebs(level,mob.blockPosition()):0;
        original.call(effect,level,mob,amplifier,reason);
        if(reason!=Entity.RemovalReason.KILLED)return;
        if(holder.equals(MobEffects.OOZING)){
            int spawned=countSlimes(level,mob)-beforeSlimes;if(spawned>0)InfusionWear.emitBuiltin(mob,holder,PROC,spawned);
        }else if(holder.equals(MobEffects.WEAVING)){
            int placed=countWebs(level,mob.blockPosition())-beforeWebs;if(placed>0)InfusionWear.emitBuiltin(mob,holder,PROC,placed);
        }else if(holder.equals(MobEffects.WIND_CHARGED)){
            InfusionWear.emitBuiltin(mob,holder,PROC,1.0);
        }
    }

    private static int countSilverfish(ServerLevel level,LivingEntity mob){return level.getEntitiesOfClass(Silverfish.class,mob.getBoundingBox().inflate(2.0)).size();}
    private static int countSlimes(ServerLevel level,LivingEntity mob){return level.getEntitiesOfClass(Slime.class,mob.getBoundingBox().inflate(2.0)).size();}
    private static int countWebs(ServerLevel level,BlockPos center){
        int count=0;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(level.getBlockState(center.offset(x,y,z)).is(Blocks.COBWEB))count++;
        return count;
    }
}
