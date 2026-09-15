package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** End-to-end combat holdouts for causal Strength/Weakness attribution. */
public final class WearCombatTests {
    private static final Identifier STRENGTH=Identifier.withDefaultNamespace("strength");

    @GameTest public void criticalStrengthUsesVanillasPreHitCriticalDecision(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var chest=new ItemStack(Items.LEATHER_CHESTPLATE);
        chest.set(Infusions.TYPE,new Infusion(STRENGTH,0,"stable",0));
        player.setItemSlot(EquipmentSlot.CHEST,chest);
        var ledger=EffectLedger.of(player);ledger.equipmentManaged=true;
        ledger.setArmor(EquipmentSlot.CHEST,new MobEffectInstance(MobEffects.STRENGTH,-1,0));
        for(int i=0;i<20;i++)player.tick();
        float charge=player.getAttackStrengthScale(0.5F);
        h.assertTrue(charge>0.99F,"Critical holdout starts from a fully charged attack");
        var effect=player.getEffect(MobEffects.STRENGTH);
        h.assertTrue(effect!=null,"Strength projection exists before the critical hit");
        double contribution=EffectAttributes.contribution(player,Attributes.ATTACK_DAMAGE,effect);

        player.setSprinting(false);
        player.setOnGround(false);
        player.fallDistance=1.0F;
        var target=h.spawn(EntityTypes.ZOMBIE,new BlockPos(3,2,3));target.setNoAi(true);
        player.attack(target);

        var progress=chest.get(Infusions.WEAR_TYPE);
        double expected=contribution*(0.2D+charge*charge*0.8D)*1.5D;
        h.assertTrue(progress!=null,"Accepted critical hit records Strength work");
        h.assertTrue(Math.abs(progress.work(STRENGTH)-expected)<1.0E-6,
            "Strength work uses the exact vanilla critical decision made before hurtOrSimulate");
        target.discard();
        h.succeed();
    }

    @GameTest public void autoSpinDamageDoesNotBillAttackDamageEffects(GameTestHelper h){
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var chest=new ItemStack(Items.LEATHER_CHESTPLATE);
        chest.set(Infusions.TYPE,new Infusion(STRENGTH,0,"stable",0));
        player.setItemSlot(EquipmentSlot.CHEST,chest);
        var ledger=EffectLedger.of(player);ledger.equipmentManaged=true;
        ledger.setArmor(EquipmentSlot.CHEST,new MobEffectInstance(MobEffects.STRENGTH,-1,0));
        player.startAutoSpinAttack(10,7.0F,new ItemStack(Items.TRIDENT));
        h.assertTrue(player.isAutoSpinAttack(),"Fixture is in the vanilla auto-spin attack path");

        var target=h.spawn(EntityTypes.ZOMBIE,new BlockPos(3,2,3));target.setNoAi(true);
        player.attack(target);
        h.assertTrue(chest.getDamageValue()==0&&!chest.has(Infusions.WEAR_TYPE),
            "Auto-spin uses its explicit spin damage, so active Strength performs no attributable attack work");
        target.discard();
        h.succeed();
    }
}
