package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.GameType;

/** Reserved holdouts added by the final causal-wear audit. */
public final class WearFinalAuditTests {
    private static final Identifier BUG_PHEROMONES=Identifier.parse("alexsmobs:bug_pheromones");
    private static final Identifier SOULSTEAL=Identifier.parse("alexsmobs:soulsteal");
    private static final Identifier SPIKED_SHELL=Identifier.parse("alexsmobs:spiked_turtle_shell");

    @GameTest public void explicitItemAttackRangeMakesPotionReachRedundantForAttacks(GameTestHelper h){
        var stack=new ItemStack(Items.STICK);
        h.assertTrue(ReachWear.attackUsesInteractionRange(stack),
            "Ordinary attacks inherit ENTITY_INTERACTION_RANGE and may therefore consume reach-effect work");
        stack.set(DataComponents.ATTACK_RANGE,new AttackRange(0.0F,6.0F,0.0F,6.0F,0.0F,1.0F));
        h.assertFalse(ReachWear.attackUsesInteractionRange(stack),
            "An explicit ATTACK_RANGE component replaces ENTITY_INTERACTION_RANGE for 26.2 attacks");
        h.succeed();
    }

    @GameTest public void alexBugPheromonesBillsOnlyARealArthropodTargetVeto(GameTestHelper h){
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(BUG_PHEROMONES)){h.succeed();return;}
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var chest=equip(player,BUG_PHEROMONES,0);
        var spider=h.spawn(EntityTypes.SPIDER,new BlockPos(3,2,3));
        spider.setNoAi(true);
        try{
            Class<?> events=Class.forName("com.github.alexthe666.alexsmobs.fabric.event.FabricServerEvents");
            Method fire=events.getMethod("fireChangeTarget",Mob.class,LivingEntity.class);
            boolean vetoed=(boolean)fire.invoke(null,spider,player);
            h.assertTrue(vetoed,"Bug Pheromones must actually veto an arthropod target acquisition in the pinned Alex fixture");
            var progress=chest.get(Infusions.WEAR_TYPE);
            h.assertTrue(progress!=null&&Math.abs(progress.work(BUG_PHEROMONES)-1.0)<1.0E-9,
                "Exactly one successful Bug Pheromones target veto contributes one work unit");
        }catch(ReflectiveOperationException e){
            throw new AssertionError("Pinned Alex's Mobs target-veto boundary changed",e);
        }finally{
            spider.discard();
        }
        h.succeed();
    }

    @GameTest public void alexSoulstealBillsItsHealBeforeSpikedShellRetaliation(GameTestHelper h){
        if(!BuiltInRegistries.MOB_EFFECT.containsKey(SOULSTEAL)||!BuiltInRegistries.ITEM.containsKey(SPIKED_SHELL)){h.succeed();return;}
        var attacker=h.makeMockPlayer(GameType.SURVIVAL);
        var victim=h.makeMockPlayer(GameType.SURVIVAL);
        var chest=equip(attacker,SOULSTEAL,2); // level 3 => Soulsteal proc chance is 100% and the 8-point heal hits its cap.
        attacker.setHealth(10.0F);
        attacker.absSnapTo(3.0,2.0,3.0,0.0F,0.0F);
        victim.absSnapTo(4.0,2.0,3.0,0.0F,0.0F);
        var shell=BuiltInRegistries.ITEM.get(SPIKED_SHELL).orElseThrow().value();
        victim.setItemSlot(EquipmentSlot.HEAD,new ItemStack(shell));
        DamageSource source=h.getLevel().damageSources().playerAttack(attacker);
        try{
            Class<?> eventClass=Class.forName("com.github.alexthe666.alexsmobs.fabric.forge.event.entity.living.LivingDamageEvent");
            Object event=eventClass.getConstructor(LivingEntity.class,DamageSource.class,float.class).newInstance(victim,source,8.0F);
            Class<?> events=Class.forName("com.github.alexthe666.alexsmobs.event.ServerEvents");
            Object handler=events.getConstructor().newInstance();
            events.getMethod("onLivingDamageEvent",eventClass).invoke(handler,event);
            h.assertTrue(attacker.getHealth()<18.0F,
                "Spiked Turtle Shell retaliation must run after the guaranteed Soulsteal heal in this holdout");
            h.assertTrue(!chest.has(Infusions.WEAR_TYPE),
                "Soulsteal must bill the full 8 HP heal immediately; retaliation must not leave a sub-threshold net-health wear debt");
        }catch(ReflectiveOperationException e){
            throw new AssertionError("Pinned Alex's Mobs Soulsteal boundary changed",e);
        }
        h.succeed();
    }

    private static ItemStack equip(net.minecraft.server.level.ServerPlayer player,Identifier effect,int amplifier){
        var chest=new ItemStack(Items.LEATHER_CHESTPLATE);
        var infusion=new Infusion(effect,amplifier,"stable",0);
        chest.set(Infusions.TYPE,infusion);
        player.setItemSlot(EquipmentSlot.CHEST,chest);
        var ledger=EffectLedger.of(player);
        ledger.equipmentManaged=true;
        ledger.setArmor(EquipmentSlot.CHEST,infusion.instance());
        return chest;
    }
}
