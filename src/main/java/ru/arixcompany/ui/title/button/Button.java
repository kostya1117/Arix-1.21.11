package ru.arixcompany.ui.title.button;

import net.minecraft.client.gui.GuiGraphics;

public interface Button {
    void render(GuiGraphics context, int mouseX, int mouseY, float delta);

    boolean mouseClicked(double mouseX, double mouseY, int button);
}
