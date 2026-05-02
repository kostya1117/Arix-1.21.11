package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public class FireworkRocketRenderState extends EntityRenderState {
    public boolean isShotAtAngle;
    public final ItemStackRenderState item = new ItemStackRenderState();
}
