package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.data.Infusions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DyeabilityConditionTests {
    @GameTest public void disabledSelfDyeRecipeDoesNotCreateDyeability(GameTestHelper h) {
        var recipeId=Identifier.parse("alchemical_leather_test:disabled_iron_boots_dyed");
        boolean loaded=h.getLevel().getServer().getRecipeManager().getRecipes().stream()
            .anyMatch(recipe->recipe.id().identifier().equals(recipeId));
        h.assertFalse(loaded,"fabric:false crafting_dye recipe must not be loaded by Minecraft");
        h.assertTrue(Infusions.slot(new ItemStack(Items.IRON_BOOTS))==null,
            "A resource rejected by Fabric load conditions cannot make armor dyeable for Alchemical Leather");
        h.succeed();
    }
}
