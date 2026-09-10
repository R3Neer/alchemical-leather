package io.github.r3neer.alchemicalleather.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AnimalInfusion(List<Infusion> effects) {
    public AnimalInfusion { effects=List.copyOf(effects); }
    public static final Codec<AnimalInfusion> CODEC=Infusion.CODEC.listOf().xmap(AnimalInfusion::new,AnimalInfusion::effects)
        .validate(value->value.valid()?DataResult.success(value):DataResult.error(()->"Animal infusion must contain at least one valid effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AnimalInfusion> STREAM_CODEC=Infusion.STREAM_CODEC.apply(ByteBufCodecs.list())
        .map(AnimalInfusion::new,AnimalInfusion::effects);
    public boolean valid(){return !effects.isEmpty()&&effects.stream().allMatch(Infusion::valid);}
}
