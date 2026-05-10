package ru.arixcompany.ui.draggable;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.SettingAdder;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

import java.util.List;

@Getter
public abstract class DraggableComponent extends SettingAdder implements IDraggable, IMinecraft {

    protected static final float ALPHA_EPSILON = 0.005F;
    private static final float SCREEN_MARGIN = 2.0F;

    protected final String name;

    protected float x;
    protected float y;
    protected float width;
    protected float height;

    protected float renderX;
    protected float renderY;

    protected boolean dragging;
    protected float dragOffsetX;
    protected float dragOffsetY;

    @Setter protected boolean visible = true;
    @Setter protected boolean pinned = false;
    @Setter protected boolean snapToEdge = false;
    @Setter protected float snapThreshold = 8.0F;

    protected final Animation alphaAnimation;

    private boolean settingsOpen = false;
    private final Animation settingsAnimation;

    protected DraggableComponent(String name, float x, float y, float width, float height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.renderX = x;
        this.renderY = y;
        this.width = width;
        this.height = height;

        this.alphaAnimation = new EaseInOutQuad(250, 1.0);
        this.alphaAnimation.setDirection(Direction.FORWARDS);

        this.settingsAnimation = new EaseInOutQuad(200, 1.0);
        this.settingsAnimation.setDirection(Direction.BACKWARDS);
        this.settingsAnimation.timerUtil.setTime(System.currentTimeMillis() - 9999);
    }

    @Override
    public void update() {
        updateVisibility();
        alphaAnimation.setDirection(visible ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    protected void updateVisibility() {}

    @Override
    public final void renderComponent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (dragging) {
            updateDragPosition(mouseX, mouseY);
        }

        renderX = x;
        renderY = y;

        float alpha = getAlpha();
        if (alpha <= ALPHA_EPSILON) return;

        renderDraggable(graphics, mouseX, mouseY, delta, renderX, renderY, width, height, alpha);

        float settingsAlpha = (float) settingsAnimation.getOutput();
        if (settingsAlpha > ALPHA_EPSILON && hasSettings()) {
            DraggableSettingsRenderer.render(graphics, this, mouseX, mouseY, alpha * settingsAlpha);
        }
    }

    protected abstract void renderDraggable(
            GuiGraphics graphics,
            int mouseX, int mouseY, float delta,
            float x, float y, float width, float height,
            float alpha
    );


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!canInteract()) return false;

        if (settingsOpen && hasSettings()) {
            if (DraggableSettingsRenderer.mouseClicked(this, mouseX, mouseY, button)) {
                return true;
            }

            if (!DraggableSettingsRenderer.isOver(this, mouseX, mouseY)) {
                closeSettings();
                if (!isMouseOver(mouseX, mouseY)) {
                    return false;
                }
            }
        }

        if (button == 1 && isDragHandle(mouseX, mouseY) && hasSettings()) {
            toggleSettings();
            return true;
        }

        if (button == 0 && isDragHandle(mouseX, mouseY) && !pinned) {
            startDragging(mouseX, mouseY);
            return true;
        }

        return onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            stopDragging();
            return true;
        }

        return onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            updateDragPosition(mouseX, mouseY);
            return true;
        }

        return onMouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!canInteract() || !isMouseOver(mouseX, mouseY)) return false;
        return onMouseScrolled(mouseX, mouseY, scrollY);
    }

    protected boolean onMouseClicked(double mouseX, double mouseY, int button) { return false; }
    protected boolean onMouseReleased(double mouseX, double mouseY, int button) { return false; }
    protected boolean onMouseDragged(double mouseX, double mouseY, int button) { return false; }
    protected boolean onMouseScrolled(double mouseX, double mouseY, double scrollY) { return false; }

    protected boolean isDragHandle(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY);
    }

    public boolean hasSettings() {
        return !settings().isEmpty();
    }

    public List<Setting> getVisibleSettings() {
        return getSettingsForGUI();
    }

    public void toggleSettings() {
        settingsOpen = !settingsOpen;
        settingsAnimation.setDirection(settingsOpen ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    public void closeSettings() {
        if (settingsOpen) {
            settingsOpen = false;
            settingsAnimation.setDirection(Direction.BACKWARDS);
        }
    }

    @Override
    public boolean shouldRender() {
        return visible || getAlpha() > ALPHA_EPSILON;
    }

    @Override
    public boolean canInteract() {
        return visible && getAlpha() > ALPHA_EPSILON;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= renderX && mouseX <= renderX + width
                && mouseY >= renderY && mouseY <= renderY + height;
    }

    @Override
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        this.renderX = x;
        this.renderY = y;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void stopDragging() {
        this.dragging = false;
    }

    public float getAlpha() {
        return (float) alphaAnimation.getOutput();
    }

    protected void startDragging(double mouseX, double mouseY) {
        this.dragging = true;
        this.dragOffsetX = (float) mouseX - x;
        this.dragOffsetY = (float) mouseY - y;
    }

    protected void updateDragPosition(double mouseX, double mouseY) {
        this.x = (float) mouseX - dragOffsetX;
        this.y = (float) mouseY - dragOffsetY;
        if (snapToEdge) applySnapToEdges();
    }

    private void applySnapToEdges() {
        if (mc.getWindow() == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (x < snapThreshold) x = SCREEN_MARGIN;
        if (y < snapThreshold) y = SCREEN_MARGIN;
        if (x + width > sw - snapThreshold) x = sw - width - SCREEN_MARGIN;
        if (y + height > sh - snapThreshold) y = sh - height - SCREEN_MARGIN;
    }
}