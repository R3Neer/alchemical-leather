package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Weaving only pays for a tick in which the wearer is actively trying to move through a web. */
@Mixin(WebBlock.class)
public abstract class WearWebMixin {
    @Unique private static final Identifier MOVEMENT=Identifier.fromNamespaceAndPath("alchemical_leather","weaving_movement");
    @Unique private static final Map<LivingEntity,Integer> alchemical$lastTick=new WeakHashMap<>();

    @Inject(method="entityInside",at=@At("RETURN"))
    private void alchemical$weavingMovement(BlockState state,Level level,BlockPos pos,Entity entity,InsideBlockEffectApplier effects,boolean precise,CallbackInfo ci){
        if(level.isClientSide()||!(entity instanceof LivingEntity living)||living.isPassenger())return;
        var effect=living.getEffect(MobEffects.WEAVING);if(effect==null)return;
        if(Math.abs(living.xxa)+Math.abs(living.yya)+Math.abs(living.zza)<=1.0E-6F)return;
        Integer previous=alchemical$lastTick.put(living,living.tickCount);
        if(previous!=null&&previous==living.tickCount)return;
        InfusionWear.emitBuiltin(living,effect.getEffect(),MOVEMENT,1.0);
    }
}
