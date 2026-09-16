package io.github.r3neer.alchemicalleather.effect;

import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Reserved holdout for Weakness' non-combat vanilla mechanic. */
public final class WearWeaknessCureTests {
    @GameTest public void successfulZombieVillagerCureBillsArmorWeakness(GameTestHelper h){
        var zombie=h.spawn(EntityTypes.ZOMBIE_VILLAGER,new BlockPos(3,2,3));
        var chest=new ItemStack(Items.LEATHER_CHESTPLATE);
        chest.set(Infusions.TYPE,new Infusion(Identifier.withDefaultNamespace("weakness"),0,"stable",0));
        zombie.setItemSlot(EquipmentSlot.CHEST,chest);
        EquipmentInfusions.sync(zombie);
        h.assertTrue(zombie.hasEffect(MobEffects.WEAKNESS),
            "The infused chestplate must be the live Weakness source before the cure starts");

        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.GOLDEN_APPLE));
        InteractionResult result=zombie.mobInteract(player,InteractionHand.MAIN_HAND);

        h.assertTrue(result instanceof InteractionResult.Success,
            "Vanilla must accept the golden apple while Weakness is active");
        h.assertTrue(zombie.isConverting(),
            "The successful interaction must actually start zombie-villager conversion");
        h.assertTrue(chest.getDamageValue()==1,
            "A cure enabled by armor Weakness costs exactly one durability point");
        h.succeed();
    }
}
