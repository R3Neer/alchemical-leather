package io.github.r3neer.alchemicalleather.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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

/** Measures effect procs from changes created synchronously by the effect hook itself. */
@Mixin(MobEffectInstance.class)
public abstract class WearEffectProcMixin {
    private static final Identifier PROC=Identifier.fromNamespaceAndPath("alchemical_leather","effect_proc");
    private static final Identifier SCORCHING_IGNITION=Identifier.fromNamespaceAndPath("alchemical_leather","scorching_ignition");
    private static final Identifier SCORCHING_FIRE=Identifier.fromNamespaceAndPath("alchemical_leather","scorching_fire_placement");
    private static final Identifier WILDER_SCORCHING=Identifier.fromNamespaceAndPath("wilderwild","scorching");

    @WrapOperation(method="onMobHurt",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffect;onMobHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;ILnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void alchemical$hurtProc(MobEffect effect,ServerLevel level,LivingEntity mob,int amplifier,DamageSource source,float damage,Operation<Void> original){
        var instance=(MobEffectInstance)(Object)this;var holder=instance.getEffect();
        Identifier id=BuiltInRegistries.MOB_EFFECT.getKey(holder.value());
        int beforeSilverfish=holder.equals(MobEffects.INFESTED)?countSilverfish(level,mob):0;
        Entity direct=WILDER_SCORCHING.equals(id)?source.getDirectEntity():null;
        int beforeFire=direct==null?0:direct.getRemainingFireTicks();
        original.call(effect,level,mob,amplifier,source,damage);
        if(holder.equals(MobEffects.INFESTED)){
            int spawned=countSilverfish(level,mob)-beforeSilverfish;
            if(spawned>0)InfusionWear.emitBuiltin(mob,holder,PROC,spawned);
        }else if(WILDER_SCORCHING.equals(id)&&direct!=null){
            int added=Math.max(0,direct.getRemainingFireTicks()-beforeFire);
            if(added>0)InfusionWear.emitBuiltin(mob,holder,SCORCHING_IGNITION,added/20.0);
        }
    }

    @WrapOperation(method="onMobRemoved",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffect;onMobRemoved(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;ILnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void alchemical$deathProc(MobEffect effect,ServerLevel level,LivingEntity mob,int amplifier,Entity.RemovalReason reason,Operation<Void> original){
        var instance=(MobEffectInstance)(Object)this;var holder=instance.getEffect();
        Identifier id=BuiltInRegistries.MOB_EFFECT.getKey(holder.value());
        int beforeSlimes=holder.equals(MobEffects.OOZING)?countSlimes(level,mob):0;
        int beforeWebs=holder.equals(MobEffects.WEAVING)?countWebs(level,mob.blockPosition()):0;
        BlockPos scorchingOrigin=WILDER_SCORCHING.equals(id)?mob.getOnPos():null;
        int beforeFire=scorchingOrigin==null?0:countFire(level,scorchingOrigin);
        original.call(effect,level,mob,amplifier,reason);
        if(reason!=Entity.RemovalReason.KILLED)return;
        if(holder.equals(MobEffects.OOZING)){
            int spawned=countSlimes(level,mob)-beforeSlimes;if(spawned>0)InfusionWear.emitBuiltin(mob,holder,PROC,spawned);
        }else if(holder.equals(MobEffects.WEAVING)){
            int placed=countWebs(level,mob.blockPosition())-beforeWebs;if(placed>0)InfusionWear.emitBuiltin(mob,holder,PROC,placed);
        }else if(holder.equals(MobEffects.WIND_CHARGED)){
            InfusionWear.emitBuiltin(mob,holder,PROC,1.0);
        }else if(WILDER_SCORCHING.equals(id)&&scorchingOrigin!=null){
            int placed=countFire(level,scorchingOrigin)-beforeFire;if(placed>0)InfusionWear.emitBuiltin(mob,holder,SCORCHING_FIRE,placed);
        }
    }

    private static int countSilverfish(ServerLevel level,LivingEntity mob){return level.getEntitiesOfClass(Silverfish.class,mob.getBoundingBox().inflate(2.0)).size();}
    private static int countSlimes(ServerLevel level,LivingEntity mob){return level.getEntitiesOfClass(Slime.class,mob.getBoundingBox().inflate(2.0)).size();}
    private static int countWebs(ServerLevel level,BlockPos center){
        int count=0;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(level.getBlockState(center.offset(x,y,z)).is(Blocks.COBWEB))count++;
        return count;
    }
    private static int countFire(ServerLevel level,BlockPos center){
        int count=0;
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){
            var state=level.getBlockState(center.offset(x,y,z));if(state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE))count++;
        }
        return count;
    }
}
