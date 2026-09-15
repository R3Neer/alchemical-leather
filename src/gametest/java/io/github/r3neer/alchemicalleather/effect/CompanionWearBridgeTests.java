package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Cross-mod holdout: the real companion adapter must reach the real wear engine. */
public final class CompanionWearBridgeTests {
    @GameTest public void clingingBridgeChargesTheOwningReorientationBoots(GameTestHelper h) throws Exception {
        if(!FabricLoader.getInstance().isModLoaded("clinging_reoriented")){h.succeed();return;}

        Identifier effectId=Identifier.parse("clinging_reoriented:reorientation");
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectId).orElseThrow();
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var boots=new ItemStack(Items.LEATHER_BOOTS);
        boots.set(Infusions.TYPE,new Infusion(effectId,0,"stable",0));
        player.setItemSlot(EquipmentSlot.FEET,boots);

        var ledger=EffectLedger.of(player);
        ledger.equipmentManaged=true;
        ledger.setArmor(new MobEffectInstance(effect,-1,0));
        h.assertTrue(player.hasEffect(effect),"Fixture projects the armor-owned Reorientation effect");
        h.assertTrue(ledger.armorEffective(effect),"Reorientation boots are the effective source before the companion event");

        Class<?> bridge=Class.forName("io.github.r3neer.clingingreoriented.compat.AlchemicalLeatherCompat");
        var successfulTurn=bridge.getMethod("successfulTurn",ServerPlayer.class);
        for(int i=0;i<14;i++)successfulTurn.invoke(null,player);
        h.assertTrue(boots.getDamageValue()==0,"Fourteen successful Reorientation turns remain below the 30-work durability threshold");
        successfulTurn.invoke(null,player);
        h.assertTrue(boots.getDamageValue()==1,"The fifteenth real companion turn event reaches 30 work and damages the owning boots exactly once");
        h.succeed();
    }
}
