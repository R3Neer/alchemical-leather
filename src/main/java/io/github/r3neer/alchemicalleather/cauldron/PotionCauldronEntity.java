package io.github.r3neer.alchemicalleather.cauldron;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
public final class PotionCauldronEntity extends BlockEntity {
    public PotionContents contents=PotionContents.EMPTY;
    public Item bottle=Items.POTION;
    public PotionCauldronEntity(BlockPos pos,BlockState state){super(PotionCauldron.ENTITY,pos,state);}
    public void fill(PotionContents contents,Item bottle) {
        this.contents=contents;this.bottle=bottle;setChanged();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override protected void saveAdditional(ValueOutput output){super.saveAdditional(output);output.store("contents",PotionContents.CODEC,contents);output.store("bottle",Identifier.CODEC,BuiltInRegistries.ITEM.getKey(bottle));}
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);contents=input.read("contents",PotionContents.CODEC).orElse(PotionContents.EMPTY);
        bottle=input.read("bottle",Identifier.CODEC).map(BuiltInRegistries.ITEM::getValue).orElse(Items.POTION);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
}
