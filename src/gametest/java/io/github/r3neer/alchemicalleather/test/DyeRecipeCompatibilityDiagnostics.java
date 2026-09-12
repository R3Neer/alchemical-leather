package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.data.Infusion;
import io.github.r3neer.alchemicalleather.data.Infusions;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.DyeRecipe;

public final class DyeRecipeCompatibilityDiagnostics {
    @GameTest public void comparePlainAndInfusedTargetMatching(GameTestHelper h){
        var manager=h.getLevel().getServer().getRecipeManager();
        var holder=manager.getRecipes().stream().filter(r->r.id().identifier().equals(Identifier.parse("alchemical_leather_test:iron_helmet_dyed"))).findFirst().orElseThrow();
        h.assertTrue(holder.value() instanceof DyeRecipe,"Synthetic dye recipe loaded");
        var recipe=(DyeRecipe)holder.value();
        var dye=new ItemStack(Items.DYE.red());
        var plain=CraftingInput.of(2,1,List.of(new ItemStack(Items.IRON_HELMET),dye.copy()));
        h.assertTrue(recipe.matches(plain,h.getLevel()),"Synthetic dye recipe accepts plain iron helmet");
        var infusedHelmet=new ItemStack(Items.IRON_HELMET);
        infusedHelmet.set(Infusions.TYPE,new Infusion(Identifier.parse("minecraft:night_vision"),0,"timed",200));
        var infused=CraftingInput.of(2,1,List.of(infusedHelmet,dye.copy()));
        h.assertTrue(recipe.matches(infused,h.getLevel()),"Synthetic dye recipe rejects only after Alchemical infusion component is present");
        h.succeed();
    }
}
