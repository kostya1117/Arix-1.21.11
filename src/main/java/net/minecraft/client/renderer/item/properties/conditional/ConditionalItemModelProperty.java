package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public interface ConditionalItemModelProperty extends ItemModelPropertyTest {
    MapCodec<? extends ConditionalItemModelProperty> type();
}
