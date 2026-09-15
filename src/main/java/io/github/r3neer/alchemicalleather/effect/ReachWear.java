package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.WearRules;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Causal attribution for successful interactions that only extended reach made possible. */
public final class ReachWear {
    public static final Identifier DETECTOR=Identifier.fromNamespaceAndPath("alchemical_leather","extra_reach_use");
    private static final double EPS=1.0E-6;
    private ReachWear(){}

    private record Candidate(MobEffectInstance effect,double contribution){}

    public static void emit(Player player,Holder<Attribute> attribute,double targetDistanceSqr){
        if(player.level().isClientSide()||!Double.isFinite(targetDistanceSqr)||targetDistanceSqr<0)return;
        var necessary=new ArrayList<Candidate>();
        double totalWeight=0.0;
        for(var effect:player.getActiveEffects()){
            Identifier id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            var rule=WearRules.rule(id);
            if(rule==null||rule.none()||rule.work("builtin",DETECTOR,1.0)<=0)continue;
            double contribution=EffectAttributes.contribution(player,attribute,effect);
            if(contribution<=1.0E-9)continue;

            // But-for test per effect. If every other active source still reaches the target after
            // removing this one, this effect did no necessary work and must not share the bill.
            double without=EffectAttributes.without(player,attribute,effect);
            if(!needed(targetDistanceSqr,without))continue;
            necessary.add(new Candidate(effect,contribution));
            totalWeight+=contribution;
        }
        if(necessary.isEmpty()||totalWeight<=0.0)return;
        for(var candidate:necessary){
            double amount=share(candidate.contribution(),totalWeight);
            if(amount>0.0)InfusionWear.emitBuiltin(player,candidate.effect().getEffect(),DETECTOR,amount);
        }
    }

    static boolean needed(double targetDistanceSqr,double rangeWithoutEffect){
        return Double.isFinite(targetDistanceSqr)&&Double.isFinite(rangeWithoutEffect)
            &&targetDistanceSqr>=0.0&&rangeWithoutEffect>=0.0
            &&targetDistanceSqr>rangeWithoutEffect*rangeWithoutEffect+EPS;
    }

    static double share(double contribution,double totalNecessaryContribution){
        if(!Double.isFinite(contribution)||!Double.isFinite(totalNecessaryContribution)
            ||contribution<=0.0||totalNecessaryContribution<=0.0)return 0.0;
        return contribution/totalNecessaryContribution;
    }
}
