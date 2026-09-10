package io.github.r3neer.alchemicalleather.cauldron;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class DyedWaterCauldron extends AbstractCauldronBlock implements EntityBlock {
    public static final Identifier ID=Identifier.fromNamespaceAndPath("alchemical_leather","dyed_water_cauldron");
    public static final IntegerProperty LEVEL=IntegerProperty.create("level",1,6);
    public static final MapCodec<DyedWaterCauldron> CODEC=simpleCodec(DyedWaterCauldron::new);
    public static final DyedWaterCauldron BLOCK=Registry.register(BuiltInRegistries.BLOCK,ID,new DyedWaterCauldron(
        BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).setId(ResourceKey.create(Registries.BLOCK,ID)).overrideDescription("block.minecraft.cauldron")));
    public static final BlockEntityType<DyedWaterCauldronEntity> ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,ID,
        new BlockEntityType<>(DyedWaterCauldronEntity::new,java.util.Set.of(BLOCK)));

    public DyedWaterCauldron(BlockBehaviour.Properties properties) {
        super(properties,CauldronInteractions.EMPTY);registerDefaultState(stateDefinition.any().setValue(LEVEL,2));
    }

    @Override protected MapCodec<? extends AbstractCauldronBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(LEVEL);}
    @Override public boolean isFull(BlockState state){return state.getValue(LEVEL)==6;}
    @Override protected double getContentHeight(BlockState state){return Mth.lerp(state.getValue(LEVEL)/6.0,0.375,0.9375);}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction direction){return Mth.ceil(state.getValue(LEVEL)/6.0F*3.0F);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new DyedWaterCauldronEntity(pos,state);}
    @Override public Item asItem(){return Items.CAULDRON;}
    @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean includeData){return new ItemStack(Items.CAULDRON);}
    public static void initialize(){}
}
