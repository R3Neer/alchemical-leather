package io.github.r3neer.alchemicalleather.recipe;

import com.mojang.serialization.MapCodec;
import io.github.r3neer.alchemicalleather.data.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Shapeless special recipe: one compatible armor item plus one potion bottle. */
public final class ArmorInfusionRecipe extends CustomRecipe {
    public static final Identifier ID=Identifier.fromNamespaceAndPath("alchemical_leather","armor_infusion");
    public static final ArmorInfusionRecipe INSTANCE=new ArmorInfusionRecipe();
    public static final MapCodec<ArmorInfusionRecipe> CODEC=MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf,ArmorInfusionRecipe> STREAM_CODEC=StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<ArmorInfusionRecipe> SERIALIZER=new RecipeSerializer<>(CODEC,STREAM_CODEC);

    private record Inputs(ItemStack armor,ItemStack potion,ArmorInfusionService.Result result){}

    private ArmorInfusionRecipe(){}

    private static boolean bottle(ItemStack stack){return stack.is(Items.POTION)||stack.is(Items.SPLASH_POTION)||stack.is(Items.LINGERING_POTION);}

    private static Inputs resolve(CraftingInput input){
        if(input.ingredientCount()!=2)return null;
        ItemStack armor=ItemStack.EMPTY,potion=ItemStack.EMPTY;
        for(var stack:input.items()){
            if(stack.isEmpty())continue;
            if(bottle(stack)){
                if(!potion.isEmpty())return null;
                potion=stack;
            }else{
                if(!armor.isEmpty())return null;
                armor=stack;
            }
        }
        if(armor.isEmpty()||potion.isEmpty()||Infusions.slot(armor)==null)return null;
        PotionContents contents=potion.get(DataComponents.POTION_CONTENTS);
        if(contents==null)return null;
        var result=ArmorInfusionService.apply(armor,contents,potion.getItem(),false);
        return result.ok()?new Inputs(armor,potion,result):null;
    }

    @Override public boolean matches(CraftingInput input,Level level){return resolve(input)!=null;}

    @Override public ItemStack assemble(CraftingInput input){
        var resolved=resolve(input);return resolved==null?ItemStack.EMPTY:resolved.result().stack();
    }

    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){
        var remaining=NonNullList.withSize(input.size(),ItemStack.EMPTY);
        for(int i=0;i<input.size();i++)if(bottle(input.getItem(i)))remaining.set(i,new ItemStack(Items.GLASS_BOTTLE));
        return remaining;
    }

    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer(){return SERIALIZER;}

    public static void initialize(){Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,ID,SERIALIZER);}
}
