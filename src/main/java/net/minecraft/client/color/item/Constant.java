package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public record Constant(int value) implements ItemTintSource {
    public static final MapCodec<Constant> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_378430_ -> p_378430_.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("value").forGetter(Constant::value)).apply(p_378430_, Constant::new)
    );

    public Constant {
        value = ARGB.opaque(value);
    }

    @Override
    public int calculate(ItemStack p_376887_,  ClientLevel p_377958_,  LivingEntity p_376156_) {
        return this.value;
    }

    @Override
    public MapCodec<Constant> type() {
        return MAP_CODEC;
    }
}
