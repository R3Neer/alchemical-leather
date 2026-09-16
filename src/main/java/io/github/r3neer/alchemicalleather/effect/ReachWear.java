package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.WearRules;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Causal attribution for successful interactions that only extended reach made possible. */
public final class ReachWear {
    public static final Identifier DETECTOR=Identifier.fromNamespaceAndPath("alchemical_leather","extra_reach_use");
    private static final double EPS=1.0E-6;
    private ReachWear(){}

    private record Candidate(MobEffectInstance effect,double contribution){}

    public static void emit(Player player,Holder<Attribute> attribute,double targetDistanceSqr){
        emit(player,attribute,targetDistanceSqr,0.0D);
    }

    /**
     * baselineBuffer is a vanilla acceptance margin that remains available in the counterfactual
     * where the reach effect is removed. It is normally zero. The only current non-zero caller is
     * 26.2 entity interaction after an explicit ATTACK_RANGE item supplied the client-side target:
     * the server then accepts that packet within ENTITY_INTERACTION_RANGE + 3.
     */
    public static void emit(Player player,Holder<Attribute> attribute,double targetDistanceSqr,double baselineBuffer){
        if(player.level().isClientSide()||!Double.isFinite(targetDistanceSqr)||targetDistanceSqr<0
            ||!Double.isFinite(baselineBuffer)||baselineBuffer<0)return;
        var necessary=new ArrayList<Candidate>();
        double totalWeight=0.0;
        for(var effect:player.getActiveEffects()){
            Identifier id=BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            var rule=WearRules.rule(id);
            if(rule==null||rule.none()||rule.work("builtin",DETECTOR,1.0)<=0)continue;
            double contribution=EffectAttributes.contribution(player,attribute,effect);
            if(contribution<=1.0E-9)continue;

            // But-for test per effect. If every other active source plus any vanilla acceptance
            // margin still reaches the target after removing this one, this effect did no necessary
            // work and must not share the bill.
            double without=EffectAttributes.without(player,attribute,effect);
            if(!needed(targetDistanceSqr,without,baselineBuffer))continue;
            necessary.add(new Candidate(effect,contribution));
            totalWeight+=contribution;
        }
        if(necessary.isEmpty()||totalWeight<=0.0)return;
        for(var candidate:necessary){
            double amount=share(candidate.contribution(),totalWeight);
            if(amount>0.0)InfusionWear.emitBuiltin(player,candidate.effect().getEffect(),DETECTOR,amount);
        }
    }

    /**
     * 26.2 attacks use an explicit ATTACK_RANGE component in preference to
     * ENTITY_INTERACTION_RANGE. In that case a reach potion cannot be the but-for cause of the
     * attack being in range, even though it may still be active on the player.
     */
    public static boolean attackUsesInteractionRange(ItemStack mainHand){
        return mainHand==null||!mainHand.has(DataComponents.ATTACK_RANGE);
    }

    static boolean needed(double targetDistanceSqr,double rangeWithoutEffect){
        return needed(targetDistanceSqr,rangeWithoutEffect,0.0D);
    }

    static boolean needed(double targetDistanceSqr,double rangeWithoutEffect,double baselineBuffer){
        if(!Double.isFinite(targetDistanceSqr)||!Double.isFinite(rangeWithoutEffect)||!Double.isFinite(baselineBuffer)
            ||targetDistanceSqr<0.0||rangeWithoutEffect<0.0||baselineBuffer<0.0)return false;
        double counterfactual=rangeWithoutEffect+baselineBuffer;
        return Double.isFinite(counterfactual)&&targetDistanceSqr>counterfactual*counterfactual+EPS;
    }

    static double share(double contribution,double totalNecessaryContribution){
        if(!Double.isFinite(contribution)||!Double.isFinite(totalNecessaryContribution)
            ||contribution<=0.0||totalNecessaryContribution<=0.0)return 0.0;
        return contribution/totalNecessaryContribution;
    }
}
