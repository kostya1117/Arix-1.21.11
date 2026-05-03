package ru.arixcompany.clickgui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public interface IComponent {

    public Minecraft mc = Minecraft.getInstance();

    default void render(GuiGraphics guiGraphics,
                        int mouseX, int mouseY, float alpha) {}

    default void render(GuiGraphics guiGraphics,
                        float x, float y, float width,
                        int mouseX, int mouseY,
                        int outlineColor, int accentColor, int bgColor,
                        int textInactive, int textActive,
                        float alpha) {}

    default float getHeight() { return 0.0F; }

    default boolean mouseClicked(int mouseX, int mouseY, int button) {
        return false;
    }

    default boolean handleClick(
                                float x, float y, float width,
                                int mouseX, int mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        return false;
    }

    default void mouseReleased() {}

    default boolean mouseScrolled(float mouseX, float mouseY, double scrollY) {
        return false;
    }

    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    default void tick() {}

    default void close() {}
}