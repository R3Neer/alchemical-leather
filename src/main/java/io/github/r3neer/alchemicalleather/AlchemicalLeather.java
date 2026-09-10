package io.github.r3neer.alchemicalleather;
import io.github.r3neer.alchemicalleather.data.*;
import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.effect.EquipmentInfusions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.item.v1.EnchantmentEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.LivingEntity;
public final class AlchemicalLeather implements ModInitializer {
    @Override public void onInitialize() {
        Infusions.initialize();PotionCauldron.initialize();DyedWaterCauldron.initialize();
        var data=ResourceManagerHelper.get(PackType.SERVER_DATA);
        data.registerReloadListener(new DyeableArmorRules());
        data.registerReloadListener(new EffectSlotRules());
        UseBlockCallback.EVENT.register(CauldronService::interact);
        EnchantmentEvents.ALLOW_ENCHANTING.register((enchantment,stack,context)->Infusions.blocked(stack)?TriState.FALSE:TriState.DEFAULT);
        ServerEntityEvents.EQUIPMENT_CHANGE.register((entity,slot,previous,next)->{if(Infusions.SLOTS.contains(slot))EquipmentInfusions.sync(entity);});
        ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{if(entity instanceof LivingEntity living)for(var slot:Infusions.SLOTS)if(Infusions.blocked(living.getItemBySlot(slot))){EquipmentInfusions.sync(living);break;}});
    }
}
