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

    /**
     * Some 26.2 mechanics consume the literal signed factor (1-resistance), so values above one
     * reverse their impulse instead of clamping it to zero. In those paths wear represents only an
     * actual reduction in impulse magnitude; a reversal/increase is not "prevented knockback".
     */
    public static void emitSignedMultiplicative(LivingEntity target,double baseMagnitude,double withResistance){
        emit(target,baseMagnitude,withResistance,true);
    }

    private static void emit(LivingEntity target,double baseMagnitude,double withResistance,boolean signed){
        if(!Double.isFinite(baseMagnitude)||baseMagnitude<=0.0||!Double.isFinite(withResistance))return;
        var holder=BuiltInRegistries.MOB_EFFECT.get(ALEX_EFFECT);
        if(holder.isEmpty())return;
        var effect=target.getEffect(holder.get());
        if(effect==null)return;
        double withoutResistance=EffectAttributes.without(target,Attributes.KNOCKBACK_RESISTANCE,effect);
        double work=signed
            ?signedMultiplicativeReduction(baseMagnitude,withResistance,withoutResistance)
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

    /** For vanilla paths that clamp a non-positive (1-resistance) contribution away. */
    public static double multiplicativeReduction(double baseMagnitude,double withResistance,double withoutResistance){
        if(!finite(baseMagnitude,withResistance,withoutResistance)||baseMagnitude<=0.0)return 0.0;
        double with=Math.max(0.0,1.0-withResistance);
        double without=Math.max(0.0,1.0-withoutResistance);
        double work=baseMagnitude*Math.max(0.0,without-with);
        return Double.isFinite(work)&&work>0.0?work:0.0;
    }

    /**
     * For paths that consume the signed (1-resistance) factor directly. Compare absolute impulse
     * magnitude so an overshoot that reverses/increases knockback cannot masquerade as protection.
     */
    public static double signedMultiplicativeReduction(double baseMagnitude,double withResistance,double withoutResistance){
        if(!finite(baseMagnitude,withResistance,withoutResistance)||baseMagnitude<=0.0)return 0.0;
        double with=Math.abs(1.0-withResistance);
        double without=Math.abs(1.0-withoutResistance);
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
}
