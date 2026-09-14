package io.github.r3neer.alchemicalleather.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Multi-effect infusion bundle for one humanoid armor slot. */
public record HumanoidInfusion(List<Infusion> effects) {
    public HumanoidInfusion { effects=List.copyOf(effects); }
    public static final Codec<HumanoidInfusion> CODEC=Infusion.CODEC.listOf().xmap(HumanoidInfusion::new,HumanoidInfusion::effects)
        .validate(value->value.valid()?DataResult.success(value):DataResult.error(()->"Humanoid infusion must contain at least two valid effects"));
    public static final StreamCodec<RegistryFriendlyByteBuf,HumanoidInfusion> STREAM_CODEC=Infusion.STREAM_CODEC.apply(ByteBufCodecs.list())
        .map(HumanoidInfusion::new,HumanoidInfusion::effects);
    public boolean valid(){return effects.size()>=2&&effects.stream().allMatch(Infusion::valid);}
}
