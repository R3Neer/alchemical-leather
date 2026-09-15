package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.api.InfusionWearApi;
import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import io.github.r3neer.alchemicalleather.data.WearProgress;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
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
        Identifier turnEvent=Identifier.parse("clinging_reoriented:gravity_turn");
        Holder<MobEffect> effect=BuiltInRegistries.MOB_EFFECT.get(effectId).orElseThrow();
        var player=h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().instabuild=false;
        h.assertFalse(player.hasInfiniteMaterials(),"Wear holdout requires survival/non-infinite material semantics");

        var boots=new ItemStack(Items.LEATHER_BOOTS);
        boots.set(Infusions.TYPE,new Infusion(effectId,0,"stable",0));
        player.setItemSlot(EquipmentSlot.FEET,boots);
        var equippedBoots=player.getItemBySlot(EquipmentSlot.FEET);

        // GameTest ServerPlayers intentionally have no play connection, so production equipment
        // reconciliation defers until JOIN. Establish the same slot-aware ledger ownership directly
        // rather than weakening that connection-safety invariant for a synthetic player.
        var ledger=EffectLedger.of(player);
        ledger.equipmentManaged=true;
        ledger.setArmor(EquipmentSlot.FEET,new MobEffectInstance(effect,-1,0));
        h.assertTrue(player.hasEffect(effect),"Slot-aware ledger projection exposes the armor-owned Reorientation effect");
        h.assertTrue(ledger.armorEffective(effect),"Reorientation boots are the effective source before the companion event");
        h.assertTrue(ledger.armorOwner(effect)==EquipmentSlot.FEET,"The projected Reorientation source is explicitly owned by the equipped boots");

        var rule=io.github.r3neer.alchemicalleather.data.WearRules.rule(effectId);
        h.assertTrue(rule!=null&&!rule.none(),"Loaded Reorientation rule is causal wear");
        h.assertTrue(Math.abs(rule.work("event",turnEvent,1.0)-2.0)<1.0e-9,"Loaded Reorientation gravity-turn rule contributes exactly two work");

        // First prove the Alchemical Leather half independently: one semantic unit for the
        // Reorientation turn rule is exactly two work on the owning boots.
        InfusionWearApi.emit(player,effect,turnEvent,1.0);
        var progress=equippedBoots.getOrDefault(Infusions.WEAR_TYPE,WearProgress.EMPTY);
        h.assertTrue(Math.abs(progress.work(effectId)-2.0)<1.0e-9,"Direct semantic API event contributes exactly the configured two work");

        // Then prove the companion half itself was initialized and resolves the same active effect.
        Class<?> bridge=Class.forName("io.github.r3neer.clingingreoriented.compat.AlchemicalLeatherCompat");
        Field emitter=bridge.getDeclaredField("emit");
        emitter.setAccessible(true);
        h.assertTrue(emitter.get(null)!=null,"Clinging optional bridge resolved the Alchemical Leather public API");
        Method turnEffect=bridge.getDeclaredMethod("turnEffect",ServerPlayer.class);
        turnEffect.setAccessible(true);
        h.assertTrue(effect.equals(turnEffect.invoke(null,player)),"Clinging bridge attributes the successful turn to the active Reorientation effect");
        Method successfulTurn=bridge.getMethod("successfulTurn",ServerPlayer.class);

        successfulTurn.invoke(null,player);
        progress=equippedBoots.getOrDefault(Infusions.WEAR_TYPE,WearProgress.EMPTY);
        h.assertTrue(Math.abs(progress.work(effectId)-4.0)<1.0e-9,"The real Clinging bridge reaches the same semantic event path");

        // One direct API event + thirteen companion events = fourteen turn-equivalents = 28 work.
        for(int i=0;i<12;i++)successfulTurn.invoke(null,player);
        progress=equippedBoots.getOrDefault(Infusions.WEAR_TYPE,WearProgress.EMPTY);
        h.assertTrue(equippedBoots.getDamageValue()==0,"Fourteen turn-equivalents remain below the durability threshold");
        h.assertTrue(Math.abs(progress.work(effectId)-28.0)<1.0e-9,"Fourteen turn-equivalents accumulate exactly 28 work on the equipped boots");

        successfulTurn.invoke(null,player);
        progress=equippedBoots.getOrDefault(Infusions.WEAR_TYPE,WearProgress.EMPTY);
        h.assertTrue(equippedBoots.getDamageValue()==1,"The fifteenth turn-equivalent reaches 30 work and damages the equipped boots exactly once");
        h.assertTrue(Math.abs(progress.work(effectId))<1.0e-9,"The spent 30-work bucket leaves no hidden fractional debt");
        h.succeed();
    }
}
