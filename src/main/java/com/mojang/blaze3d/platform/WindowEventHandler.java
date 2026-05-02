package com.mojang.blaze3d.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public interface WindowEventHandler {
    void setWindowActive(boolean p_85477_);

    void resizeDisplay();

    void cursorEntered();
}
