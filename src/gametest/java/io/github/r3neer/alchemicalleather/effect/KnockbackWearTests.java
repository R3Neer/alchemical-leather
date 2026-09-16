package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.hoglin.HoglinBase;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Reserved coverage for mechanics that consume KNOCKBACK_RESISTANCE outside LivingEntity#knockback. */
public final class KnockbackWearTests {
    private static final Identifier EFFECT=Identifier.parse("alexsmobs:knockback_resistance");
    private static final Identifier BISON=Identifier.parse("alexsmobs:bison");

    @GameTest public void directKnockbackFormulasUseTheActualMechanicContract(GameTestHelper h){
        h.assertTrue(Math.abs(KnockbackWear.multiplicativeReduction(2.0D,0.5D,0.0D)-1.0D)<1.0E-9,
            "A 0.5 resistance contribution removes half of a lower-clamped multiplicative two-unit impulse");
        h.assertTrue(KnockbackWear.multiplicativeReduction(2.0D,1.5D,1.0D)==0.0D,
            "An effect already eclipsed beyond a lower-clamped zero multiplier cannot manufacture work");
        h.assertTrue(Math.abs(KnockbackWear.multiplicativeReduction(2.0D,0.0D,-0.5D)-1.0D)<1.0E-9,
            "Lower-clamped mechanics preserve extra impulse from negative resistance in the counterfactual");

        h.assertTrue(Math.abs(KnockbackWear.unitClampedMultiplicativeReduction(2.0D,0.5D,0.0D)-1.0D)<1.0E-9,
            "Unit-clamped mechanics agree below their upper clamp");
        h.assertTrue(KnockbackWear.unitClampedMultiplicativeReduction(2.0D,0.0D,-0.5D)==0.0D,
            "Guster already caps negative-resistance amplification at one, so the potion cannot claim that fictional excess");
        h.assertTrue(Math.abs(KnockbackWear.unitClampedMultiplicativeReduction(2.0D,1.0D,0.0D)-2.0D)<1.0E-9,
            "A unit-clamped path credits a real transition from full impulse to zero");

        h.assertTrue(Math.abs(KnockbackWear.signedMultiplicativeReduction(2.0D,0.5D,0.0D)-1.0D)<1.0E-9,
            "Signed mechanics agree with the ordinary multiplier below one resistance");
        h.assertTrue(KnockbackWear.signedMultiplicativeReduction(2.0D,1.5D,0.5D)==0.0D,
            "Reversing an impulse with equal magnitude is not prevented knockback");
        h.assertTrue(KnockbackWear.signedMultiplicativeReduction(2.0D,1.8D,0.5D)==0.0D,
            "Increasing reversed impulse magnitude must never be billed as protection");
        h.assertTrue(Math.abs(KnockbackWear.signedMultiplicativeReduction(2.0D,1.2D,0.7D)-0.2D)<1.0E-9,
            "Crossing one may still reduce absolute impulse magnitude; only that reduction is work");

        h.assertTrue(Math.abs(KnockbackWear.subtractiveReduction(1.0D,0.75D,0.25D)-0.5D)<1.0E-9,
            "Hoglin-style subtraction bills the exact effective-power difference");
        h.assertTrue(Math.abs(KnockbackWear.subtractiveReduction(0.4D,0.75D,0.25D)-0.15D)<1.0E-9,
            "Subtractive resistance may fully suppress the live impulse while only partly suppressing the counterfactual");
        h.assertTrue(KnockbackWear.multiplicativeReduction(Double.NaN,0.5D,0.0D)==0.0D,
            "Non-finite input fails closed");
        h.succeed();
    }

    @GameTest public void ironGolemLiftBillsAlexResistanceAtItsDirectAttributeRead(GameTestHelper h){
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(EFFECT)){h.succeed();return;}
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var boots=equip(player);
        var golem=h.spawn(EntityTypes.IRON_GOLEM,new BlockPos(3,2,3));
        golem.setNoAi(true);
        player.setHealth(player.getMaxHealth());
        h.assertTrue(golem.doHurtTarget(h.getLevel(),player),"Iron Golem fixture must land a real accepted attack");
        var progress=boots.get(Infusions.WEAR_TYPE);
        h.assertTrue(progress!=null&&progress.work(EFFECT)>0.0D,
            "Iron Golem's direct 0.4*(1-resistance) lift must reach the alchemical knockback detector");
        golem.discard();
        h.succeed();
    }

    @GameTest public void hoglinThrowBillsItsSubtractiveResistancePath(GameTestHelper h){
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(EFFECT)){h.succeed();return;}
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var boots=equip(player);
        var hoglin=h.spawn(EntityTypes.HOGLIN,new BlockPos(3,2,3));
        hoglin.setNoAi(true);
        var attackKnockback=hoglin.getAttribute(Attributes.ATTACK_KNOCKBACK);
        h.assertTrue(attackKnockback!=null,"Hoglin exposes ATTACK_KNOCKBACK in the 26.2 fixture");
        attackKnockback.setBaseValue(2.0D);
        HoglinBase.throwTarget(hoglin,player);
        var progress=boots.get(Infusions.WEAR_TYPE);
        h.assertTrue(progress!=null&&progress.work(EFFECT)>0.0D,
            "Hoglin's ATTACK_KNOCKBACK-resistance subtraction must reach the alchemical detector");
        hoglin.discard();
        h.succeed();
    }

    @GameTest public void alexBisonLaunchBillsTheRealOptionalAdapter(GameTestHelper h){
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(EFFECT)||!BuiltInRegistries.ENTITY_TYPE.containsKey(BISON)){h.succeed();return;}
        var type=BuiltInRegistries.ENTITY_TYPE.get(BISON).orElseThrow().value();
        Entity bison=type.create(h.getLevel(),EntitySpawnReason.COMMAND);
        h.assertTrue(bison!=null,"Pinned Alex 2.1.9 fixture must create its bison entity");
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var boots=equip(player);
        try{
            Method launch=bison.getClass().getDeclaredMethod("launch",Entity.class,boolean.class);
            launch.setAccessible(true);
            launch.invoke(bison,player,false);
        }catch(ReflectiveOperationException e){
            throw new AssertionError("Pinned Alex 2.1.9 Bison launch boundary changed",e);
        }
        var progress=boots.get(Infusions.WEAR_TYPE);
        h.assertTrue(progress!=null&&progress.work(EFFECT)>0.0D,
            "Real Alex Bison target-resistance consumption must reach the optional alchemical adapter");
        bison.discard();
        h.succeed();
    }

    private static ItemStack equip(net.minecraft.world.entity.player.Player player){
        var boots=new ItemStack(Items.LEATHER_BOOTS);
        var infusion=new Infusion(EFFECT,0,"stable",0);
        boots.set(Infusions.TYPE,infusion);
        player.setItemSlot(EquipmentSlot.FEET,boots);
        var ledger=EffectLedger.of(player);
        ledger.equipmentManaged=true;
        ledger.setArmor(EquipmentSlot.FEET,infusion.instance());
        return boots;
    }
}
