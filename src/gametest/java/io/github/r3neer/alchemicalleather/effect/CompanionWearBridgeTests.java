package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import io.github.r3neer.alchemicalleather.data.WearProgress;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Cross-mod holdout: the real companion adapter must reach the real wear engine. */
public final class CompanionWearBridgeTests {
    @GameTest public void clingingBridgeChargesTheOwningReorientationBoots(GameTestHelper h) throws Exception {
        if(!FabricLoader.getInstance().isModLoaded("clinging_reoriented")){h.succeed();return;}

        Identifier effectId=Identifier.parse("clinging_reoriented:reorientation");
        var effect=BuiltInRegistries.MOB_EFFECT.get(effectId).orElseThrow();
        var player=h.makeMockServerPlayerInLevel();
        var inputBoots=new ItemStack(Items.LEATHER_BOOTS);
        inputBoots.set(Infusions.TYPE,new Infusion(effectId,0,"stable",0));
        player.setItemSlot(EquipmentSlot.FEET,inputBoots);

        // Use the real equipment reconciliation path so the live effect and its owning slot are
        // established exactly as they are in production. Manually projecting the effect would be
        // an invalid fixture because wear intentionally refuses to bill an ownerless source.
        EquipmentInfusions.sync(player);
        var ledger=EffectLedger.of(player);
        var equippedBoots=player.getItemBySlot(EquipmentSlot.FEET);
        h.assertTrue(player.hasEffect(effect),"Real equipment sync projects the armor-owned Reorientation effect");
        h.assertTrue(ledger.armorEffective(effect),"Reorientation boots are the effective source before the companion event");
        h.assertTrue(ledger.armorOwner(effect)==EquipmentSlot.FEET,"Real equipment sync attributes Reorientation to the equipped boots");

        Class<?> bridge=Class.forName("io.github.r3neer.clingingreoriented.compat.AlchemicalLeatherCompat");
        var successfulTurn=bridge.getMethod("successfulTurn",ServerPlayer.class);
        for(int i=0;i<14;i++)successfulTurn.invoke(null,player);
        var progress=equippedBoots.getOrDefault(Infusions.WEAR_TYPE,WearProgress.EMPTY);
        h.assertTrue(equippedBoots.getDamageValue()==0,"Fourteen successful Reorientation turns remain below the durability threshold");
        h.assertTrue(Math.abs(progress.work(effectId)-28.0)<1.0e-9,"Fourteen real companion turn events accumulate exactly 28 work on the equipped boots");

        successfulTurn.invoke(null,player);
        progress=equippedBoots.getOrDefault(Infusions.WEAR_TYPE,WearProgress.EMPTY);
        h.assertTrue(equippedBoots.getDamageValue()==1,"The fifteenth real companion turn event reaches 30 work and damages the equipped boots exactly once");
        h.assertTrue(Math.abs(progress.work(effectId))<1.0e-9,"The spent 30-work bucket leaves no hidden fractional debt");
        h.succeed();
    }
}
