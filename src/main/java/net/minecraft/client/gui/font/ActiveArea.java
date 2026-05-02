package net.minecraft.client.gui.font;

import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public interface ActiveArea {
    Style style();

    float activeLeft();

    float activeTop();

    float activeRight();

    float activeBottom();
}
