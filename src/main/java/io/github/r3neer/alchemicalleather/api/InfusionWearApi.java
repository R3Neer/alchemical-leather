package io.github.r3neer.alchemicalleather.api;

import io.github.r3neer.alchemicalleather.effect.InfusionWear;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

/**
 * Optional compatibility API for mods that know when one of their potion effects performed semantic work.
 * Emitters report facts; Alchemical Leather remains responsible for armor ownership, balance and durability.
 */
public final class InfusionWearApi {
    private InfusionWearApi(){}

    public static void emit(LivingEntity wearer,Holder<MobEffect> effect,Identifier event,double amount){
        InfusionWear.emitEvent(wearer,effect,event,amount);
    }
}
