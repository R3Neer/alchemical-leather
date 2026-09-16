package io.github.r3neer.alchemicalleather.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Exact counterfactual accounting for mechanics that consume KNOCKBACK_RESISTANCE directly. */
public final class KnockbackWear {
    public static final Identifier DETECTOR=Identifier.fromNamespaceAndPath("alchemical_leather","knockback_reduced");
    private static final Identifier ALEX_EFFECT=Identifier.fromNamespaceAndPath("alexsmobs","knockback_resistance");
    private KnockbackWear(){}

    public static void emitMultiplicative(LivingEntity target,double baseMagnitude,double withResistance){
        emit(target,baseMagnitude,withResistance,false);
    }

    /** Guster clamps the literal (1-resistance) factor into [0,1], including negative resistance. */
    public static void emitUnitClampedMultiplicative(LivingEntity target,double baseMagnitude,double withResistance){
        emit(target,baseMagnitude,withResistance,true);
    }

    private static void emit(LivingEntity target,double baseMagnitude,double withResistance,boolean unitClamped){
        if(!Double.isFinite(baseMagnitude)||baseMagnitude<=0.0||!Double.isFinite(withResistance))return;
        var holder=BuiltInRegistries.MOB_EFFECT.get(ALEX_EFFECT);
        if(holder.isEmpty())return;
        var effect=target.getEffect(holder.get());
        if(effect==null)return;
        double withoutResistance=EffectAttributes.without(target,Attributes.KNOCKBACK_RESISTANCE,effect);
        double work=unitClamped
            ?unitClampedMultiplicativeReduction(baseMagnitude,withResistance,withoutResistance)
            :multiplicativeReduction(baseMagnitude,withResistance,withoutResistance);
        if(work>0.0)InfusionWear.emitBuiltin(target,effect.getEffect(),DETECTOR,work);
    }

    public static void emitSubtractive(LivingEntity target,double baseMagnitude,double withResistance){
        if(!Double.isFinite(baseMagnitude)||baseMagnitude<=0.0||!Double.isFinite(withResistance))return;
        var holder=BuiltInRegistries.MOB_EFFECT.get(ALEX_EFFECT);
        if(holder.isEmpty())return;
        var effect=target.getEffect(holder.get());
        if(effect==null)return;
        double withoutResistance=EffectAttributes.without(target,Attributes.KNOCKBACK_RESISTANCE,effect);
        double work=subtractiveReduction(baseMagnitude,withResistance,withoutResistance);
        if(work>0.0)InfusionWear.emitBuiltin(target,effect.getEffect(),DETECTOR,work);
    }

    /**
     * 26.2 KNOCKBACK_RESISTANCE is sanitized to [-2,1]. Most consumers use max(0,1-r) or an
     * equivalent raw (1-r), which is therefore non-negative at runtime. Negative resistance may
     * still amplify knockback and remains part of the counterfactual.
     */
    public static double multiplicativeReduction(double baseMagnitude,double withResistance,double withoutResistance){
        if(!finite(baseMagnitude,withResistance,withoutResistance)||baseMagnitude<=0.0)return 0.0;
        double with=Math.max(0.0,1.0-withResistance);
        double without=Math.max(0.0,1.0-withoutResistance);
        double work=baseMagnitude*Math.max(0.0,without-with);
        return Double.isFinite(work)&&work>0.0?work:0.0;
    }

    /** For Guster-style paths that additionally cap negative-resistance amplification at one. */
    public static double unitClampedMultiplicativeReduction(double baseMagnitude,double withResistance,double withoutResistance){
        if(!finite(baseMagnitude,withResistance,withoutResistance)||baseMagnitude<=0.0)return 0.0;
        double with=clamp(1.0-withResistance,0.0,1.0);
        double without=clamp(1.0-withoutResistance,0.0,1.0);
        double work=baseMagnitude*Math.max(0.0,without-with);
        return Double.isFinite(work)&&work>0.0?work:0.0;
    }

    public static double subtractiveReduction(double baseMagnitude,double withResistance,double withoutResistance){
        if(!finite(baseMagnitude,withResistance,withoutResistance)||baseMagnitude<=0.0)return 0.0;
        double with=Math.max(0.0,baseMagnitude-withResistance);
        double without=Math.max(0.0,baseMagnitude-withoutResistance);
        double work=Math.max(0.0,without-with);
        return Double.isFinite(work)&&work>0.0?work:0.0;
    }

    private static boolean finite(double... values){
        for(double value:values)if(!Double.isFinite(value))return false;
        return true;
    }

    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
}
