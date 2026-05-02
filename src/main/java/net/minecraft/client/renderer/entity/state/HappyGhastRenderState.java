package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public class HappyGhastRenderState extends LivingEntityRenderState {
    public ItemStack bodyItem = ItemStack.EMPTY;
    public boolean isRidden;
    public boolean isLeashHolder;
}
