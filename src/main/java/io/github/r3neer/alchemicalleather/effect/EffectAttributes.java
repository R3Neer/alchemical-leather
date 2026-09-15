package io.github.r3neer.alchemicalleather.effect;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;

/** Pure counterfactual attribute evaluation without mutating the live entity attribute map. */
public final class EffectAttributes {
    private EffectAttributes(){}

    public static double without(LivingEntity entity,Holder<Attribute> attribute,Collection<MobEffectInstance> excludedEffects){
        var instance=entity.getAttribute(attribute);if(instance==null)return entity.getAttributeValue(attribute);
        var excluded=new HashSet<net.minecraft.resources.Identifier>();
        for(var effect:excludedEffects)effect.getEffect().value().createModifiers(effect.getAmplifier(),(candidate,modifier)->{
            if(candidate.equals(attribute))excluded.add(modifier.id());
        });
        return calculate(instance,excluded);
    }

    public static double without(LivingEntity entity,Holder<Attribute> attribute,MobEffectInstance excludedEffect){
        return without(entity,attribute,List.of(excludedEffect));
    }

    public static double contribution(LivingEntity entity,Holder<Attribute> attribute,MobEffectInstance effect){
        return entity.getAttributeValue(attribute)-without(entity,attribute,effect);
    }

    private static double calculate(AttributeInstance instance,Set<net.minecraft.resources.Identifier> excluded){
        double base=instance.getBaseValue();
        for(var modifier:instance.getModifiers())if(!excluded.contains(modifier.id())&&modifier.operation()==AttributeModifier.Operation.ADD_VALUE)base+=modifier.amount();
        double result=base;
        for(var modifier:instance.getModifiers())if(!excluded.contains(modifier.id())&&modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)result+=base*modifier.amount();
        for(var modifier:instance.getModifiers())if(!excluded.contains(modifier.id())&&modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)result*=1.0+modifier.amount();
        return instance.getAttribute().value().sanitizeValue(result);
    }
}
