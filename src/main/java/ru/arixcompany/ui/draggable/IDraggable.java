package ru.arixcompany.ui.draggable;

import net.minecraft.client.gui.GuiGraphics;

public interface IDraggable {

    String getName();

    float getX();
    float getY();
    float getWidth();
    float getHeight();

    boolean isVisible();
    void setVisible(boolean visible);

    boolean isDragging();
    void stopDragging();

    boolean shouldRender();

    boolean canInteract();

    boolean isMouseOver(double mouseX, double mouseY);

    void setPosition(float x, float y);
    void update();
    void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta);

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return false;
    }
}