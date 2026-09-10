package io.github.r3neer.alchemicalleather.test;

import io.github.r3neer.alchemicalleather.data.Infusions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DyeabilityConditionTests {
    private boolean loaded(GameTestHelper h,String id) {
        var recipeId=Identifier.parse(id);
        return h.getLevel().getServer().getRecipeManager().getRecipes().stream()
            .anyMatch(recipe->recipe.id().identifier().equals(recipeId));
    }

    @GameTest public void disabledSelfDyeRecipeDoesNotCreateDyeability(GameTestHelper h) {
        h.assertFalse(loaded(h,"alchemical_leather_test:disabled_iron_boots_dyed"),
            "fabric:false crafting_dye recipe must not be loaded by Minecraft");
        h.assertTrue(Infusions.slot(new ItemStack(Items.IRON_BOOTS))==null,
            "A resource rejected by Fabric load conditions cannot make armor dyeable for Alchemical Leather");
        h.succeed();
    }

    @GameTest public void invalidSelfDyeRecipeDoesNotCreateDyeability(GameTestHelper h) {
        h.assertFalse(loaded(h,"alchemical_leather_test:invalid_chainmail_boots_dyed"),
            "Invalid crafting_dye recipe must be rejected by Minecraft's recipe codec");
        h.assertTrue(Infusions.slot(new ItemStack(Items.CHAINMAIL_BOOTS))==null,
            "A recipe rejected by Minecraft's codec cannot make armor dyeable for Alchemical Leather");
        h.succeed();
    }
}
