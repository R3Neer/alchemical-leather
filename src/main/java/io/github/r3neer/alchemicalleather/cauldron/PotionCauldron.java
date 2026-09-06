package io.github.r3neer.alchemicalleather.cauldron;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
public final class PotionCauldron extends AbstractCauldronBlock implements EntityBlock {
    public static final Identifier ID=Identifier.fromNamespaceAndPath("alchemical_leather","potion_cauldron");
    public static final IntegerProperty LEVEL=LayeredCauldronBlock.LEVEL;
    public static final MapCodec<PotionCauldron> CODEC=simpleCodec(PotionCauldron::new);
    public static final PotionCauldron BLOCK=Registry.register(BuiltInRegistries.BLOCK,ID,new PotionCauldron(
        BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).setId(ResourceKey.create(Registries.BLOCK,ID)).overrideDescription("block.minecraft.cauldron")));
    public static final BlockEntityType<PotionCauldronEntity> ENTITY=Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,ID,
        new BlockEntityType<>(PotionCauldronEntity::new,java.util.Set.of(BLOCK)));
    public PotionCauldron(BlockBehaviour.Properties properties) {
        super(properties,CauldronInteractions.EMPTY);registerDefaultState(stateDefinition.any().setValue(LEVEL,1));
    }
    @Override protected MapCodec<? extends AbstractCauldronBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(LEVEL);}
    @Override public boolean isFull(BlockState state){return state.getValue(LEVEL)==3;}
    @Override protected double getContentHeight(BlockState state){return (6+3*state.getValue(LEVEL))/16.0;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction direction){return state.getValue(LEVEL);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PotionCauldronEntity(pos,state);}
    @Override public Item asItem(){return Items.CAULDRON;}
    @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean includeData){return new ItemStack(Items.CAULDRON);}
    public static void initialize(){}
}
