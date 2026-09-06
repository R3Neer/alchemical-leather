package io.github.r3neer.alchemicalleather.data;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.world.effect.*;
import java.util.Optional;
public record Infusion(Identifier effect, int amplifier, String mode, int remainingTicks) {
    public static final Codec<Infusion> CODEC = RecordCodecBuilder.<Infusion>create(i -> i.group(
        Identifier.CODEC.fieldOf("effect").forGetter(Infusion::effect),
        Codec.intRange(0,255).fieldOf("amplifier").forGetter(Infusion::amplifier),
        Codec.STRING.fieldOf("mode").forGetter(Infusion::mode),
        Codec.INT.optionalFieldOf("remaining_ticks",0).forGetter(Infusion::remainingTicks)
    ).apply(i,Infusion::new)).validate(i -> i.valid() ? DataResult.success(i) : DataResult.error(() -> "Invalid infusion mode/duration"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Infusion> STREAM_CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,Infusion::effect,ByteBufCodecs.VAR_INT,Infusion::amplifier,
        ByteBufCodecs.STRING_UTF8,Infusion::mode,ByteBufCodecs.VAR_INT,Infusion::remainingTicks,Infusion::new);
    public boolean valid() { return amplifier>=0 && amplifier<=255 && (mode.equals("timed") ? remainingTicks>0 : (mode.equals("stable") || mode.equals("instant")) && remainingTicks==0); }
    public Optional<Holder.Reference<MobEffect>> holder() { return BuiltInRegistries.MOB_EFFECT.get(effect); }
    public Infusion remaining(int value) { return new Infusion(effect,amplifier,mode,value); }
    public MobEffectInstance instance() { return new MobEffectInstance(holder().orElseThrow(),mode.equals("stable") ? -1 : remainingTicks,amplifier,false,false,true); }
}
