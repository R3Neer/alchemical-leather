package io.github.r3neer.alchemicalleather.cauldron;
import io.github.r3neer.alchemicalleather.data.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
public final class CauldronService {
    private static InteractionResult error(Player player,String error){if(player instanceof net.minecraft.server.level.ServerPlayer server)server.sendSystemMessage(Component.translatable("message.alchemical_leather."+error),true);return InteractionResult.FAIL;}
    private static boolean bottle(ItemStack s){return s.is(Items.POTION)||s.is(Items.SPLASH_POTION)||s.is(Items.LINGERING_POTION);}
    private static boolean allowed(Player player,Level level,BlockPos pos){return !player.isSpectator()&&player.getAbilities().mayBuild&&(!(level instanceof ServerLevel server)||server.mayInteract(player,pos));}
    private static boolean armorForSide(ItemStack stack,Level level){return level.isClientSide()?Infusions.armorCandidate(stack):Infusions.slot(stack)!=null;}

    public static InteractionResult interact(Player player,Level level,InteractionHand hand,BlockHitResult hit) {
        BlockPos pos=hit.getBlockPos();var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof AbstractCauldronBlock))return InteractionResult.PASS;
        var stack=player.getItemInHand(hand);var incoming=stack.get(DataComponents.POTION_CONTENTS);
        boolean ownPotion=state.is(PotionCauldron.BLOCK),ownDyed=state.is(DyedWaterCauldron.BLOCK);
        boolean bedActive=BedrockifyBridge.cauldronsActive();boolean bedPotion=bedActive&&BedrockifyBridge.potion(state),bedDyed=bedActive&&BedrockifyBridge.dyed(state);
        boolean armor=armorForSide(stack,level),dye=stack.has(DataComponents.DYE);

        // Alchemical Leather owns vanilla water + dye unless BedrockIfy's corresponding feature is positively active.
        if(dye&&state.is(Blocks.WATER_CAULDRON)) {
            if(bedActive)return InteractionResult.PASS;
            if(!allowed(player,level,pos))return InteractionResult.FAIL;
            if(level.isClientSide())return InteractionResult.SUCCESS;
            int waterLevel=state.getValue(LayeredCauldronBlock.LEVEL);var dyeColor=stack.get(DataComponents.DYE);Item used=stack.getItem();
            writeDyed(level,pos,dyeColor.getTextureDiffuseColor(),waterLevel*2);consumeDye(stack,player,used);dyeFeedback(level,pos);return InteractionResult.SUCCESS;
        }

        if(ownDyed) {
            if(dye) {
                if(!allowed(player,level,pos))return InteractionResult.FAIL;
                if(level.isClientSide())return InteractionResult.SUCCESS;
                var be=dyedEntity(level,pos);int current=be.color;int next=DyeColors.blend(current,stack.get(DataComponents.DYE).getTextureDiffuseColor());
                if(next!=current){Item used=stack.getItem();be.setColor(next);consumeDye(stack,player,used);dyeFeedback(level,pos);}return InteractionResult.SUCCESS;
            }
            if(armor) {
                if(!allowed(player,level,pos))return InteractionResult.FAIL;
                if(level.isClientSide())return InteractionResult.SUCCESS;
                if(Infusions.slot(stack)==null)return InteractionResult.PASS;
                int color=dyedEntity(level,pos).color;stack.set(DataComponents.DYED_COLOR,new DyedItemColor(DyeColors.blend(stack,color)));
                lowerDyed(level,pos,state,1);player.awardStat(Stats.USE_CAULDRON);splashFeedback(level,pos);return InteractionResult.SUCCESS;
            }
            if(stack.is(Items.GLASS_BOTTLE)) {
                if(state.getValue(DyedWaterCauldron.LEVEL)<2)return InteractionResult.FAIL;
                if(!allowed(player,level,pos))return InteractionResult.FAIL;
                if(level.isClientSide())return InteractionResult.SUCCESS;
                Item used=stack.getItem();player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,PotionContents.createItemStack(Items.POTION,Potions.WATER)));
                player.awardStat(Stats.USE_CAULDRON);player.awardStat(Stats.ITEM_USED.get(used));lowerDyed(level,pos,state,2);fluidFeedback(level,pos,SoundEvents.BOTTLE_FILL,GameEvent.FLUID_PICKUP);return InteractionResult.SUCCESS;
            }
            if(stack.is(Items.BUCKET)) {
                if(state.getValue(DyedWaterCauldron.LEVEL)!=6)return InteractionResult.PASS;
                if(!allowed(player,level,pos))return InteractionResult.FAIL;
                if(level.isClientSide())return InteractionResult.SUCCESS;
                Item used=stack.getItem();player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.WATER_BUCKET)));
                player.awardStat(Stats.USE_CAULDRON);player.awardStat(Stats.ITEM_USED.get(used));level.setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());fluidFeedback(level,pos,SoundEvents.BUCKET_FILL,GameEvent.FLUID_PICKUP);return InteractionResult.SUCCESS;
            }
            if(stack.is(Items.POTION)&&incoming!=null&&incoming.is(Potions.WATER)) {
                if(!allowed(player,level,pos))return InteractionResult.FAIL;
                if(level.isClientSide())return InteractionResult.SUCCESS;
                int dyedLevel=state.getValue(DyedWaterCauldron.LEVEL);if(dyedLevel==6)return InteractionResult.SUCCESS;
                int waterLevel=Math.min(dyedLevel/2+1,3);Item used=stack.getItem();player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.GLASS_BOTTLE)));
                player.awardStat(Stats.USE_CAULDRON);player.awardStat(Stats.ITEM_USED.get(used));level.setBlockAndUpdate(pos,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,waterLevel));fluidFeedback(level,pos,SoundEvents.BOTTLE_EMPTY,GameEvent.FLUID_PLACE);return InteractionResult.SUCCESS;
            }
            // WATER_BUCKET and the other ordinary bucket interactions deliberately fall through to the block's vanilla EMPTY dispatcher.
        }

        if(armor&&state.is(Blocks.WATER_CAULDRON)) {
            if(!allowed(player,level,pos))return InteractionResult.FAIL;
            if(level.isClientSide())return InteractionResult.SUCCESS;
            if(Infusions.slot(stack)==null)return InteractionResult.PASS;
            if(!Infusions.blocked(stack)&&!stack.has(DataComponents.DYED_COLOR))return InteractionResult.PASS;
            stack.remove(Infusions.TYPE);stack.remove(Infusions.ANIMAL_TYPE);stack.remove(DataComponents.DYED_COLOR);player.awardStat(Stats.CLEAN_ARMOR);
            LayeredCauldronBlock.lowerFillLevel(state,level,pos);splashFeedback(level,pos);return InteractionResult.SUCCESS;
        }

        if(armor&&bedDyed) {
            if(!allowed(player,level,pos))return InteractionResult.FAIL;
            if(level.isClientSide())return InteractionResult.SUCCESS;
            if(Infusions.slot(stack)==null)return InteractionResult.PASS;
            try {
                int color=BedrockifyBridge.color(level,pos);var property=BedrockifyBridge.property(state);int remaining=state.getValue(property)-1;
                stack.set(DataComponents.DYED_COLOR,new DyedItemColor(DyeColors.blend(stack,color)));
                level.setBlockAndUpdate(pos,remaining==0?Blocks.CAULDRON.defaultBlockState():state.setValue(property,remaining));player.awardStat(Stats.USE_CAULDRON);splashFeedback(level,pos);return InteractionResult.SUCCESS;
            } catch(ReflectiveOperationException|RuntimeException e){return error(player,"invalid");}
        }

        boolean potionRelevant=bottle(stack)||armor&&(ownPotion||bedPotion)||ownPotion;
        if(!potionRelevant)return InteractionResult.PASS;
        if(!allowed(player,level,pos))return InteractionResult.FAIL;
        if(bottle(stack)&&!stack.is(Items.POTION)&&!player.isShiftKeyDown())return error(player,"sneak");
        if(bottle(stack)&&incoming!=null&&incoming.is(Potions.WATER)&&!ownPotion&&!bedPotion)return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;

        PotionContents contents=null;Item type=null;int doses=0;
        if(ownPotion) {
            if(!(level.getBlockEntity(pos) instanceof PotionCauldronEntity be))return error(player,"invalid");
            contents=be.contents;type=be.bottle;doses=state.getValue(PotionCauldron.LEVEL);
        } else if(bedPotion) {
            if(!bottle(stack)&&!armor)return InteractionResult.PASS;
            try {var snapshot=BedrockifyBridge.read(level,pos,state);contents=snapshot.contents();type=snapshot.bottle();doses=snapshot.doses();}
            catch(IllegalArgumentException e){return error(player,"fractional");}
            catch(ReflectiveOperationException|RuntimeException e){return error(player,"invalid");}
        }
        if(bottle(stack)) {
            var resolved=Infusions.resolveAll(incoming,stack.getItem());if(!resolved.ok())return error(player,resolved.error());
            if(!state.is(Blocks.CAULDRON)&&!ownPotion&&!bedPotion)return error(player,"different");
            if(doses>=3)return error(player,"full");
            if(doses>0&&(!contents.equals(incoming)||type!=stack.getItem()))return error(player,"different");
            Item bottleType=stack.getItem();write(level,pos,incoming,bottleType,doses+1);
            player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.GLASS_BOTTLE)));feedback(level,pos);return InteractionResult.SUCCESS;
        }
        if(armor&&(ownPotion||bedPotion)) {
            if(Infusions.slot(stack)==null)return InteractionResult.PASS;
            if(Infusions.enchanted(stack))return error(player,"enchanted");
            var target=Infusions.slot(stack);var result=stack.copy();
            if(target==EquipmentSlot.BODY) {
                var resolved=Infusions.resolveAll(contents,type);if(!resolved.ok())return error(player,resolved.error());
                result.remove(Infusions.TYPE);result.set(Infusions.ANIMAL_TYPE,resolved.infusion());
            } else {
                var resolved=Infusions.resolve(contents,type);if(!resolved.ok())return error(player,resolved.error());
                if(!Infusions.accepts(stack,target,resolved.infusion().effect()))return error(player,"slot");
                result.remove(Infusions.ANIMAL_TYPE);result.set(Infusions.TYPE,resolved.infusion());
            }
            result.set(DataComponents.DYED_COLOR,new DyedItemColor(contents.getColor()&0xffffff));
            write(level,pos,contents,type,doses-1);player.setItemInHand(hand,result);feedback(level,pos);return InteractionResult.SUCCESS;
        }
        if(ownPotion&&stack.is(Items.GLASS_BOTTLE)) {
            if(type!=Items.POTION&&type!=Items.SPLASH_POTION&&type!=Items.LINGERING_POTION)return error(player,"invalid");
            var result=new ItemStack(type);result.set(DataComponents.POTION_CONTENTS,contents);
            write(level,pos,contents,type,doses-1);player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,result));feedback(level,pos);return InteractionResult.SUCCESS;
        }
        return ownPotion?InteractionResult.FAIL:InteractionResult.PASS;
    }

    public static void write(Level level,BlockPos pos,PotionContents contents,Item bottle,int doses) {
        if(doses==0){level.setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());return;}
        if(doses<1||doses>3)throw new IllegalArgumentException("Invalid doses");
        level.setBlockAndUpdate(pos,PotionCauldron.BLOCK.defaultBlockState().setValue(PotionCauldron.LEVEL,doses));
        if(!(level.getBlockEntity(pos) instanceof PotionCauldronEntity be))throw new IllegalStateException("Missing potion cauldron entity");
        be.fill(contents,bottle);level.updateNeighbourForOutputSignal(pos,PotionCauldron.BLOCK);
    }
    public static void writeDyed(Level level,BlockPos pos,int color,int amount) {
        if(amount==0){level.setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());return;}
        if(amount<1||amount>6)throw new IllegalArgumentException("Invalid dyed-water amount");
        level.setBlockAndUpdate(pos,DyedWaterCauldron.BLOCK.defaultBlockState().setValue(DyedWaterCauldron.LEVEL,amount));dyedEntity(level,pos).setColor(color);level.updateNeighbourForOutputSignal(pos,DyedWaterCauldron.BLOCK);
    }
    private static DyedWaterCauldronEntity dyedEntity(Level level,BlockPos pos){if(level.getBlockEntity(pos) instanceof DyedWaterCauldronEntity be)return be;throw new IllegalStateException("Missing dyed-water cauldron entity");}
    private static void lowerDyed(Level level,BlockPos pos,BlockState state,int amount){int next=state.getValue(DyedWaterCauldron.LEVEL)-amount;if(next<0)throw new IllegalArgumentException("Not enough dyed water");if(next==0)level.setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());else level.setBlockAndUpdate(pos,state.setValue(DyedWaterCauldron.LEVEL,next));}
    private static void consumeDye(ItemStack stack,Player player,Item used){if(!player.getAbilities().instabuild)stack.shrink(1);player.awardStat(Stats.ITEM_USED.get(used));}
    private static void feedback(Level level,BlockPos pos){level.playSound(null,pos,SoundEvents.BOTTLE_EMPTY,SoundSource.BLOCKS,1,1);level.gameEvent(null,GameEvent.BLOCK_CHANGE,pos);}
    private static void dyeFeedback(Level level,BlockPos pos){level.playSound(null,pos,SoundEvents.DYE_USE,SoundSource.BLOCKS,1,1);level.gameEvent(null,GameEvent.BLOCK_CHANGE,pos);}
    private static void splashFeedback(Level level,BlockPos pos){level.playSound(null,pos,SoundEvents.GENERIC_SPLASH,SoundSource.BLOCKS,.15F,1.25F);level.gameEvent(null,GameEvent.BLOCK_CHANGE,pos);}
    private static void fluidFeedback(Level level,BlockPos pos,SoundEvent sound,GameEvent event){level.playSound(null,pos,sound,SoundSource.BLOCKS,1,1);level.gameEvent(null,event,pos);}
}
