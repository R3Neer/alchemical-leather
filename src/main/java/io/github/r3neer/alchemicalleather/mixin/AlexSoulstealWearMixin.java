package io.github.r3neer.alchemicalleather.mixin;

import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import java.util.ArrayDeque;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Alex's Mobs adapter. The target class need not exist in standalone Alchemical Leather. */
@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents",remap=false)
public abstract class AlexSoulstealWearMixin {
    @Unique private static final Identifier SOULSTEAL=Identifier.fromNamespaceAndPath("alexsmobs","soulsteal");
    @Unique private static final Identifier HEALING=Identifier.fromNamespaceAndPath("alchemical_leather","soulsteal_healing");
    @Unique private static final ThreadLocal<ArrayDeque<Snapshot>> alchemical$stack=ThreadLocal.withInitial(ArrayDeque::new);
    @Unique private record Snapshot(LivingEntity attacker,float health,Holder<MobEffect> effect){}

    @Inject(method="fireLivingDamage",at=@At("HEAD"),remap=false)
    private static void alchemical$beforeDamage(LivingEntity victim,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        LivingEntity attacker=source.getEntity() instanceof LivingEntity living?living:null;
        Holder<MobEffect> holder=BuiltInRegistries.MOB_EFFECT.get(SOULSTEAL).map(value->(Holder<MobEffect>)value).orElse(null);
        if(attacker==null||holder==null||!attacker.hasEffect(holder))alchemical$stack.get().push(new Snapshot(null,0,null));
        else alchemical$stack.get().push(new Snapshot(attacker,attacker.getHealth(),holder));
    }

    @Inject(method="fireLivingDamage",at=@At("RETURN"),remap=false)
    private static void alchemical$afterDamage(LivingEntity victim,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        var stack=alchemical$stack.get();if(stack.isEmpty())return;
        var snapshot=stack.pop();if(stack.isEmpty())alchemical$stack.remove();
        if(snapshot.attacker()==null||snapshot.effect()==null)return;
        float healed=snapshot.attacker().getHealth()-snapshot.health();
        if(healed>0)InfusionWear.emitBuiltin(snapshot.attacker(),snapshot.effect(),HEALING,healed);
    }
}
