package io.github.r3neer.alchemicalleather.cauldron;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class DyedWaterCauldronEntity extends BlockEntity implements RenderDataBlockEntity {
    public int color=0xffffff;

    public DyedWaterCauldronEntity(BlockPos pos,BlockState state){super(DyedWaterCauldron.ENTITY,pos,state);}

    public void setColor(int color) {
        this.color=color&0xffffff;setChanged();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }

    @Override protected void saveAdditional(ValueOutput output){super.saveAdditional(output);output.putInt("color",color);}
    @Override protected void loadAdditional(ValueInput input){
        int previousColor=color;
        super.loadAdditional(input);color=input.getIntOr("color",0xffffff)&0xffffff;
        if(color!=previousColor)CauldronRenderInvalidation.afterClientDataLoad(level,worldPosition);
    }
    @Override public Object getRenderData(){return color;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
}
