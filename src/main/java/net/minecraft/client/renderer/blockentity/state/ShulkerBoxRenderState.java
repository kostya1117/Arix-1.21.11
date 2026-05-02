package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public class ShulkerBoxRenderState extends BlockEntityRenderState {
    public Direction direction = Direction.NORTH;
    public  DyeColor color;
    public float progress;
}
