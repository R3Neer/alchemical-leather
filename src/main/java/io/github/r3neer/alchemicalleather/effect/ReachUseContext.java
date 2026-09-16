package io.github.r3neer.alchemicalleather.effect;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Thread-confined bridge between successful Item#use calls and their actual vanilla block raycast. */
public final class ReachUseContext {
    private ReachUseContext(){}
    private static final ThreadLocal<Frame> CURRENT=new ThreadLocal<>();
    private static final class Frame {
        final Player player;
        BlockHitResult hit;
        Frame(Player player){this.player=player;}
    }

    public static void begin(Player player){
        if(player==null||player.level().isClientSide())return;
        CURRENT.set(new Frame(player));
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
        ReachWear.emit(player,Attributes.BLOCK_INTERACTION_RANGE,
            player.getEyePosition().distanceToSqr(frame.hit.getLocation()));
    }
}
