package io.github.r3neer.alchemicalleather.effect;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Thread-confined bridge between successful Item#use calls and their actual vanilla block raycast. */
public final class ReachUseContext {
    private ReachUseContext(){}
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    private static final class Frame {
        final Player player;
        final InteractionHand hand;
        final boolean requireConsumption;
        final Item initialItem;
        final int initialCount;
        BlockHitResult hit;
        Frame(Player player,InteractionHand hand,boolean requireConsumption){
            this.player=player;
            this.hand=hand;
            this.requireConsumption=requireConsumption;
            var stack=player.getItemInHand(hand);
            this.initialItem=stack.getItem();
            this.initialCount=stack.getCount();
        }
    }

    public static void begin(Player player,InteractionHand hand,boolean requireConsumption){
        if(player==null||hand==null||player.level().isClientSide())return;
        CURRENT.set(new Frame(player,hand,requireConsumption));
    }

    public static void capture(Player player,BlockHitResult hit){
        var frame=CURRENT.get();
        if(frame==null||frame.player!=player||hit==null||hit.getType()!=HitResult.Type.BLOCK)return;
        frame.hit=hit;
    }

    public static void finish(Player player,InteractionResult result){
        var frame=CURRENT.get();
        CURRENT.remove();
        if(frame==null||frame.player!=player||result==null||!result.consumesAction()||frame.hit==null)return;
        if(frame.requireConsumption){
            var after=player.getItemInHand(frame.hand);
            boolean consumed=after.getItem()!=frame.initialItem||after.getCount()<frame.initialCount;
            if(!consumed)return;
        }
        ReachWear.emit(player,Attributes.BLOCK_INTERACTION_RANGE,
            player.getEyePosition().distanceToSqr(frame.hit.getLocation()));
    }
}
