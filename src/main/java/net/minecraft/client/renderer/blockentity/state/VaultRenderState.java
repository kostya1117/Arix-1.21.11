package net.minecraft.client.renderer.blockentity.state;

import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public class VaultRenderState extends BlockEntityRenderState {
    public  ItemClusterRenderState displayItem;
    public float spin;
}
