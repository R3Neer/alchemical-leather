package io.github.r3neer.alchemicalleather.cauldron;
import io.github.r3neer.alchemicalleather.data.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
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
    public static InteractionResult interact(Player player,Level level,InteractionHand hand,BlockHitResult hit) {
        BlockPos pos=hit.getBlockPos();var state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof AbstractCauldronBlock))return InteractionResult.PASS;
        var stack=player.getItemInHand(hand);boolean own=state.is(PotionCauldron.BLOCK),bed=BedrockifyBridge.potion(state);
        boolean armor=Infusions.slot(stack)!=null;
        boolean relevant=bottle(stack)||armor&&(own||bed||state.is(Blocks.WATER_CAULDRON)||BedrockifyBridge.dyed(state))||own;
        if(!relevant)return InteractionResult.PASS;
        if(player.isSpectator()||!player.getAbilities().mayBuild||level instanceof ServerLevel server&&!server.mayInteract(player,pos))return InteractionResult.FAIL;
        if(bottle(stack)&&!stack.is(Items.POTION)&&!player.isShiftKeyDown())return error(player,"sneak");
        var incoming=stack.get(DataComponents.POTION_CONTENTS);
        if(bottle(stack)&&incoming!=null&&incoming.is(Potions.WATER)&&!own&&!bed)return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(armor&&state.is(Blocks.WATER_CAULDRON)) {
            if(!stack.has(Infusions.TYPE)&&!stack.has(DataComponents.DYED_COLOR))return InteractionResult.PASS;
            stack.remove(Infusions.TYPE);stack.remove(DataComponents.DYED_COLOR);
            LayeredCauldronBlock.lowerFillLevel(state,level,pos);feedback(level,pos);return InteractionResult.SUCCESS;
        }
        if(armor&&BedrockifyBridge.dyed(state)) {
            try {int color=BedrockifyBridge.color(level,pos);var property=BedrockifyBridge.property(state);int remaining=state.getValue(property)-1;
                stack.set(DataComponents.DYED_COLOR,new DyedItemColor(color&0xffffff));
                level.setBlockAndUpdate(pos,remaining==0?Blocks.CAULDRON.defaultBlockState():state.setValue(property,remaining));feedback(level,pos);return InteractionResult.SUCCESS;
            } catch(ReflectiveOperationException|RuntimeException e){return error(player,"invalid");}
        }
        PotionContents contents=null;Item type=null;int doses=0;
        if(own) {
            if(!(level.getBlockEntity(pos) instanceof PotionCauldronEntity be))return error(player,"invalid");
            contents=be.contents;type=be.bottle;doses=state.getValue(PotionCauldron.LEVEL);
        } else if(bed) {
            if(!bottle(stack)&&!armor)return InteractionResult.PASS;
            try {var snapshot=BedrockifyBridge.read(level,pos,state);contents=snapshot.contents();type=snapshot.bottle();doses=snapshot.doses();}
            catch(IllegalArgumentException e){return error(player,"fractional");}
            catch(ReflectiveOperationException|RuntimeException e){return error(player,"invalid");}
        }
        if(bottle(stack)) {
            var resolved=Infusions.resolve(incoming,stack.getItem());if(!resolved.ok())return error(player,resolved.error());
            if(!state.is(Blocks.CAULDRON)&&!own&&!bed)return error(player,"different");
            if(doses>=3)return error(player,"full");
            if(doses>0&&(!contents.equals(incoming)||type!=stack.getItem()))return error(player,"different");
            Item bottleType=stack.getItem();
            write(level,pos,incoming,bottleType,doses+1);
            player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.GLASS_BOTTLE)));
            feedback(level,pos);return InteractionResult.SUCCESS;
        }
        if(armor&&(own||bed)) {
            var resolved=Infusions.resolve(contents,type);if(!resolved.ok())return error(player,resolved.error());
            if(Infusions.enchanted(stack))return error(player,"enchanted");
            if(EffectSlotRules.slot(resolved.infusion().effect())!=Infusions.slot(stack))return error(player,"slot");
            var result=stack.copy();result.set(Infusions.TYPE,resolved.infusion());result.set(DataComponents.DYED_COLOR,new DyedItemColor(contents.getColor()&0xffffff));
            write(level,pos,contents,type,doses-1);player.setItemInHand(hand,result);feedback(level,pos);return InteractionResult.SUCCESS;
        }
        if(own&&stack.is(Items.GLASS_BOTTLE)) {
            if(type!=Items.POTION&&type!=Items.SPLASH_POTION&&type!=Items.LINGERING_POTION)return error(player,"invalid");
            var result=new ItemStack(type);result.set(DataComponents.POTION_CONTENTS,contents);
            write(level,pos,contents,type,doses-1);player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,result));feedback(level,pos);return InteractionResult.SUCCESS;
        }
        return own?InteractionResult.FAIL:InteractionResult.PASS;
    }
    public static void write(Level level,BlockPos pos,PotionContents contents,Item bottle,int doses) {
        if(doses==0){level.setBlockAndUpdate(pos,Blocks.CAULDRON.defaultBlockState());return;}
        if(doses<1||doses>3)throw new IllegalArgumentException("Invalid doses");
        level.setBlockAndUpdate(pos,PotionCauldron.BLOCK.defaultBlockState().setValue(PotionCauldron.LEVEL,doses));
        if(!(level.getBlockEntity(pos) instanceof PotionCauldronEntity be))throw new IllegalStateException("Missing potion cauldron entity");
        be.fill(contents,bottle);level.updateNeighbourForOutputSignal(pos,PotionCauldron.BLOCK);
    }
    private static void feedback(Level level,BlockPos pos){level.playSound(null,pos,SoundEvents.BOTTLE_EMPTY,SoundSource.BLOCKS,1,1);level.gameEvent(null,GameEvent.BLOCK_CHANGE,pos);}
}
