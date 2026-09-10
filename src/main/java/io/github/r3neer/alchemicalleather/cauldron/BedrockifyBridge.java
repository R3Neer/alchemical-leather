package io.github.r3neer.alchemicalleather.cauldron;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
/** Optional reflective adapter; never links BedrockIfy classes into standalone runtime. */
public final class BedrockifyBridge {
    private static final Identifier POTION=Identifier.parse("bedrockify:potion_cauldron");
    private static final Identifier DYED=Identifier.parse("bedrockify:colored_water_cauldron");
    public static boolean potion(BlockState s){return BuiltInRegistries.BLOCK.getKey(s.getBlock()).equals(POTION);}
    public static boolean dyed(BlockState s){return BuiltInRegistries.BLOCK.getKey(s.getBlock()).equals(DYED);}
    public static boolean cauldronsActive(){
        if(!BuiltInRegistries.BLOCK.containsKey(DYED))return false;
        try {
            var type=Class.forName("me.juancarloscp52.bedrockify.Bedrockify");
            var instance=type.getMethod("getInstance").invoke(null);if(instance==null)return false;
            var settings=type.getField("settings").get(instance);if(settings==null)return false;
            return settings.getClass().getField("bedrockCauldron").getBoolean(settings);
        } catch(ReflectiveOperationException|LinkageError e){return false;}
    }
    public static IntegerProperty property(BlockState state){return (IntegerProperty)state.getBlock().getStateDefinition().getProperty("c_level");}
    public record Snapshot(PotionContents contents,Item bottle,int doses){}
    public static int color(Level level,BlockPos pos) throws ReflectiveOperationException {
        var be=level.getBlockEntity(pos);if(be==null)throw new IllegalStateException("Missing BedrockIfy block entity");
        return (Integer)be.getClass().getMethod("getTintColor").invoke(be);
    }
    public static Snapshot read(Level level,BlockPos pos,BlockState state) throws ReflectiveOperationException {
        int value=state.getValue(property(state));
        if(value!=2&&value!=5&&value!=8)throw new IllegalArgumentException("fractional");
        var be=level.getBlockEntity(pos);if(be==null)throw new IllegalStateException("Missing BedrockIfy block entity");
        var id=(Identifier)be.getClass().getMethod("getFluidId").invoke(be);
        var holder=BuiltInRegistries.POTION.get(id).orElseThrow();
        var contents=new PotionContents(holder);
        if((contents.getColor()&0xffffff)!=(color(level,pos)&0xffffff))contents=new PotionContents(java.util.Optional.of(holder),java.util.Optional.of(color(level,pos)),java.util.List.of(),java.util.Optional.empty());
        return new Snapshot(contents,(Item)be.getClass().getMethod("getPotionType").invoke(be),(value+1)/3);
    }
}
