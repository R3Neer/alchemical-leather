package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import io.github.r3neer.alchemicalleather.effect.WearPredicates;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Charges only cobweb movement that Weaving actually recovers from vanilla's stronger slowdown. */
@Mixin(Entity.class)
public abstract class WearWebMixin {
    @Unique private static final Identifier MOVEMENT=Identifier.fromNamespaceAndPath("alchemical_leather","weaving_movement");

    @Shadow protected Vec3 stuckSpeedMultiplier;

    @Unique private Vec3 alchemical$weavingMoveOrigin;
    @Unique private Vec3 alchemical$weavingRequestedMove;
    @Unique private boolean alchemical$meterWeavingMove;

    @Inject(method="move",at=@At("HEAD"))
    private void alchemical$beforeWebMove(MoverType moverType,Vec3 delta,CallbackInfo ci){
        alchemical$meterWeavingMove=false;
        alchemical$weavingMoveOrigin=null;
        alchemical$weavingRequestedMove=null;
        var self=(Entity)(Object)this;
        if(self.level().isClientSide()||moverType==MoverType.PISTON||!(self instanceof LivingEntity living))return;
        var effect=living.getEffect(MobEffects.WEAVING);if(effect==null)return;
        Vec3 multiplier=stuckSpeedMultiplier;
        if(multiplier==null||!WearPredicates.isWeavingWebMultiplier(multiplier.x,multiplier.y,multiplier.z))return;
        alchemical$meterWeavingMove=true;
        alchemical$weavingMoveOrigin=self.position();
        alchemical$weavingRequestedMove=delta;
    }

    @Inject(method="move",at=@At("RETURN"))
    private void alchemical$afterWebMove(MoverType moverType,Vec3 delta,CallbackInfo ci){
        boolean meter=alchemical$meterWeavingMove;
        Vec3 origin=alchemical$weavingMoveOrigin;
        Vec3 requested=alchemical$weavingRequestedMove;
        alchemical$meterWeavingMove=false;
        alchemical$weavingMoveOrigin=null;
        alchemical$weavingRequestedMove=null;
        if(!meter||origin==null||requested==null)return;
        var self=(Entity)(Object)this;
        if(!(self instanceof LivingEntity living))return;
        Vec3 actual=self.position().subtract(origin);
        double benefit=WearPredicates.weavingRealizedBenefit(
            requested.x,requested.y,requested.z,actual.x,actual.y,actual.z);
        if(benefit<=0.0)return;
        var effect=living.getEffect(MobEffects.WEAVING);
        if(effect!=null)InfusionWear.emitBuiltin(living,effect.getEffect(),MOVEMENT,benefit);
    }
}
