package io.github.r3neer.alchemicalleather.test;
import io.github.r3neer.alchemicalleather.data.*;
import io.github.r3neer.alchemicalleather.cauldron.*;
import io.github.r3neer.alchemicalleather.effect.*;
import io.github.r3neer.alchemicalleather.mixin.LivingEffectsAccess;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import com.mojang.serialization.JsonOps;
import java.util.*;
public class CoreTests {
    private Infusion timed(String effect,int duration,int amplifier){return new Infusion(Identifier.parse(effect),amplifier,"timed",duration);}
    private ItemStack leather(Item item,Infusion infusion){var stack=new ItemStack(item);stack.set(Infusions.TYPE,infusion);return stack;}
    private void ticks(LivingEntity p,int count){for(int i=0;i<count;i++)((LivingEffectsAccess)p).alchemical$tickEffects();}
    private InteractionResult use(GameTestHelper h,Player p,BlockPos pos,ItemStack stack){p.setItemInHand(InteractionHand.MAIN_HAND,stack);return CauldronService.interact(p,h.getLevel(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    @GameTest public void potionResolution(GameTestHelper h){
        var speed=Infusions.resolve(new PotionContents(Potions.STRONG_SWIFTNESS),Items.SPLASH_POTION);
        h.assertTrue(speed.ok()&&speed.infusion().amplifier()==1&&speed.infusion().remainingTicks()==1800,"Raw strong speed values");
        h.assertFalse(Infusions.resolve(new PotionContents(Potions.TURTLE_MASTER),Items.POTION).ok(),"Turtle Master rejected");
        h.assertFalse(Infusions.resolve(new PotionContents(Potions.WATER),Items.POTION).ok(),"No effect rejected");
        var duplicates=new PotionContents(Optional.empty(),Optional.empty(),List.of(new MobEffectInstance(MobEffects.SPEED,100),new MobEffectInstance(MobEffects.SPEED,100)),Optional.empty());
        h.assertFalse(Infusions.resolve(duplicates,Items.POTION).ok(),"Duplicates not merged");h.succeed();
    }
    @GameTest public void codecsPreserveUnknownEffect(GameTestHelper h){
        var missing=timed("missing:effect",123,2);var encoded=Infusion.CODEC.encodeStart(JsonOps.INSTANCE,missing).getOrThrow();
        h.assertTrue(Infusion.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow().equals(missing),"Unknown ID safely persists");
        h.assertTrue(missing.holder().isEmpty(),"Missing holder unresolved");
        var ops=h.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);var stack=leather(Items.LEATHER_LEGGINGS,timed("minecraft:speed",123,1));
        var copy=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,stack).getOrThrow()).getOrThrow();
        h.assertTrue(copy.get(Infusions.TYPE).equals(stack.get(Infusions.TYPE)),"Component item roundtrip");h.succeed();
    }
    @GameTest public void cauldronThreeBottles(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());
        for(int n=1;n<=3;n++){use(h,p,pos,PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS));h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==n,"Dose "+n);}
        var fourth=PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS);h.assertTrue(use(h,p,pos,fourth)==InteractionResult.FAIL&&fourth.getCount()==1,"Fourth dose unchanged");
        for(int n=3;n>0;n--){use(h,p,pos,new ItemStack(Items.GLASS_BOTTLE));h.assertTrue(p.getMainHandItem().is(Items.POTION),"Extract original bottle");}
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.CAULDRON),"Ordinary empty cauldron");h.succeed();
    }
    @GameTest public void noMixingOrLingeringConversion(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setShiftKeyDown(true);var pos=h.absolutePos(new BlockPos(1,1,1));
        CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.SWIFTNESS),Items.POTION,1);
        var other=PotionContents.createItemStack(Items.LINGERING_POTION,Potions.SWIFTNESS);
        h.assertTrue(use(h,p,pos,other)==InteractionResult.FAIL,"Lingering cannot convert normal dose");
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==1&&other.getCount()==1,"No consumption");
        h.assertTrue(use(h,p,pos,PotionContents.createItemStack(Items.POTION,Potions.LEAPING))==InteractionResult.FAIL,"Different potion rejected");h.succeed();
    }
    @GameTest public void replacementAndExactColor(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));
        var contents=new PotionContents(Optional.of(Potions.SWIFTNESS),Optional.of(0x123456),List.of(),Optional.empty());
        CauldronService.write(h.getLevel(),pos,contents,Items.POTION,2);
        var stack=leather(Items.LEATHER_LEGGINGS,timed("minecraft:jump_boost",55,2));stack.set(DataComponents.DYED_COLOR,new DyedItemColor(0xff0000));
        use(h,p,pos,stack);var result=p.getMainHandItem();
        h.assertTrue(result.get(Infusions.TYPE).equals(timed("minecraft:speed",3600,0)),"Infusion replaced entirely");
        h.assertTrue(result.get(DataComponents.DYED_COLOR).rgb()==0x123456,"Liquid RGB replaces prior dye");
        use(h,p,pos,result);h.assertTrue(p.getMainHandItem().get(Infusions.TYPE).remainingTicks()==3600,"Reinfusion not additive");h.succeed();
    }
    @GameTest public void wrongSlotAndEnchantmentAreAtomic(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));CauldronService.write(h.getLevel(),pos,new PotionContents(Potions.SWIFTNESS),Items.POTION,2);
        h.assertTrue(use(h,p,pos,new ItemStack(Items.LEATHER_BOOTS))==InteractionResult.FAIL,"Speed rejects boots");
        var leggings=new ItemStack(Items.LEATHER_LEGGINGS);leggings.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION),1);
        var before=leggings.copy();h.assertTrue(use(h,p,pos,leggings)==InteractionResult.FAIL,"Enchanted piece rejected");
        h.assertTrue(ItemStack.matches(before,leggings)&&h.getLevel().getBlockState(pos).getValue(PotionCauldron.LEVEL)==2,"No armor or liquid changes");h.succeed();
    }
    @GameTest public void washingConsumesOneWaterLevel(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var pos=h.absolutePos(new BlockPos(1,1,1));h.getLevel().setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));
        var stack=leather(Items.LEATHER_LEGGINGS,timed("minecraft:speed",200,0));stack.set(DataComponents.DYED_COLOR,new DyedItemColor(0x123456));use(h,p,pos,stack);
        h.assertFalse(stack.has(Infusions.TYPE)||stack.has(DataComponents.DYED_COLOR),"Both properties washed");
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(LayeredCauldronBlock.LEVEL)==2,"Exactly one level");h.succeed();
    }
    @GameTest public void pauseResumeAndExpiry(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var stack=leather(Items.LEATHER_LEGGINGS,timed("minecraft:speed",150,0));
        p.setItemSlot(EquipmentSlot.LEGS,stack);EquipmentInfusions.sync(p);ticks(p,60);
        h.assertTrue(stack.get(Infusions.TYPE).remainingTicks()==90,"60 equipped ticks consumed");
        p.setItemSlot(EquipmentSlot.LEGS,ItemStack.EMPTY);EquipmentInfusions.sync(p);ticks(p,100);
        h.assertTrue(stack.get(Infusions.TYPE).remainingTicks()==90&&!p.hasEffect(MobEffects.SPEED),"Time paused and effect removed");
        p.setItemSlot(EquipmentSlot.LEGS,stack);EquipmentInfusions.sync(p);ticks(p,90);
        h.assertFalse(stack.has(Infusions.TYPE)||p.hasEffect(MobEffects.SPEED),"Expired");h.assertTrue(stack.isEnchantable(),"Enchantable after expiry");h.succeed();
    }
    @GameTest public void stableAndInstant(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var stable=leather(Items.LEATHER_LEGGINGS,new Infusion(Identifier.parse("minecraft:speed"),2,"stable",0));
        p.setItemSlot(EquipmentSlot.LEGS,stable);EquipmentInfusions.sync(p);ticks(p,2000);
        h.assertTrue(p.getEffect(MobEffects.SPEED).getAmplifier()==2&&stable.get(Infusions.TYPE).remainingTicks()==0,"Stable level preserved without timer");
        p.setHealth(1);var instant=leather(Items.LEATHER_CHESTPLATE,new Infusion(Identifier.parse("minecraft:instant_health"),1,"instant",0));
        p.setItemSlot(EquipmentSlot.CHEST,instant);EquipmentInfusions.sync(p);float health=p.getHealth();
        h.assertTrue(health>1&&!instant.has(Infusions.TYPE),"Instant applied and consumed");EquipmentInfusions.sync(p);h.assertTrue(p.getHealth()==health,"No repeat");h.succeed();
    }
    @GameTest public void enchantingAndRepairGuards(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var stack=leather(Items.LEATHER_LEGGINGS,timed("minecraft:speed",100,0));stack.setDamageValue(40);
        var protection=h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        h.assertFalse(stack.isEnchantable()||stack.canBeEnchantedWith(protection,EnchantingContext.ACCEPTABLE),"Table and Fabric hook blocked");
        var menu=new AnvilMenu(0,p.getInventory());menu.getSlot(0).set(stack);menu.getSlot(1).set(new ItemStack(Items.LEATHER));menu.createResult();
        h.assertTrue(!menu.getSlot(2).getItem().isEmpty()&&menu.getSlot(2).getItem().get(Infusions.TYPE).equals(stack.get(Infusions.TYPE)),"Material repair preserves infusion");
        menu.getSlot(1).set(new ItemStack(Items.LEATHER_LEGGINGS));menu.createResult();h.assertTrue(menu.getSlot(2).getItem().isEmpty(),"Combining armor blocked");
        var input=CraftingInput.of(2,1,List.of(stack,new ItemStack(Items.LEATHER_LEGGINGS)));
        h.assertFalse(RepairItemRecipe.INSTANCE.matches(input,h.getLevel()),"Crafting repair blocked");h.succeed();
    }
    @GameTest public void cauldronHasVanillaIdentity(GameTestHelper h){
        h.assertTrue(PotionCauldron.BLOCK.asItem()==Items.CAULDRON,"Same item");h.assertTrue(PotionCauldron.BLOCK.getDescriptionId().equals(Blocks.CAULDRON.getDescriptionId()),"Same visible name");
        h.assertTrue(PotionCauldron.BLOCK.defaultBlockState().getShape(h.getLevel(),BlockPos.ZERO).bounds().equals(Blocks.CAULDRON.defaultBlockState().getShape(h.getLevel(),BlockPos.ZERO).bounds()),"Same shape bounds");h.succeed();
    }
}
