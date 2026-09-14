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
    private ReachWear(){}

    public static void emit(Player player,Holder<Attribute> attribute,double targetDistanceSqr){
        if(player.level().isClientSide()||!Double.isFinite(targetDistanceSqr)||targetDistanceSqr<0)return;
        var candidates=new ArrayList<MobEffectInstance>();var weights=new ArrayList<Double>();double totalWeight=0;
        for(var effect:player.getActiveEffects()){
            Identifier id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());var rule=WearRules.rule(id);
            if(rule==null||rule.none()||rule.work("builtin",DETECTOR,1.0)<=0)continue;
            double contribution=EffectAttributes.contribution(player,attribute,effect);
            if(contribution<=1.0E-9)continue;
            candidates.add(effect);weights.add(contribution);totalWeight+=contribution;
        }
        if(candidates.isEmpty()||totalWeight<=0)return;
        double baseline=EffectAttributes.without(player,attribute,candidates);
        if(targetDistanceSqr<=baseline*baseline+1.0E-6)return;
        for(int i=0;i<candidates.size();i++)InfusionWear.emitBuiltin(player,candidates.get(i).getEffect(),DETECTOR,weights.get(i)/totalWeight);
    }
}
