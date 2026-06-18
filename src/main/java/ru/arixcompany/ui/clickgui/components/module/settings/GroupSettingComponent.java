package ru.arixcompany.ui.clickgui.components.module.settings;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.GroupSetting;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.ui.clickgui.components.module.SettingComponentFactory;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.HashMap;
import java.util.Map;

public final class GroupSettingComponent implements IComponent {

    private static final float HEADER_H  = 12.0F;
    private static final float CHILD_GAP = 1.0F;
    private static final float INDENT    = 4.0F;

    @Getter
    private final GroupSetting setting;
    private final SettingComponentFactory factory;
    private final Map<Setting, IComponent> childCache = new HashMap<>();

    public GroupSettingComponent(GroupSetting setting, SettingComponentFactory factory) {
        this.setting = setting;
        this.factory = factory;
    }

    @Override
    public float getHeight() {
        float anim = (float) setting.getOpenAnim().getOutput();
        if (anim <= 0.001F) return HEADER_H;
        return HEADER_H + calcChildrenHeight() * anim;
    }

    private float calcChildrenHeight() {
        float total = 2.0F;
        for (Setting child : setting.getVisibleChildren()) {
            total += getOrCreate(child).getHeight() + CHILD_GAP;
        }
        return total;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {

        Gui.animRun(setting.getOpenAnim(), setting.isOpen() ? 1.0 : 0.0);
        float anim = (float) setting.getOpenAnim().getOutput();

        boolean hovered = hovered(mouseX, mouseY, x, y, width, HEADER_H);

        if (hovered) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        int headerBg = hovered ? ColorUtil.overCol(bgColor, accentColor, 0.12F) : bgColor;

        RenderUtils.fillRoundRect(guiGraphics, x, y, width, HEADER_H, 3.0F, headerBg);
        RenderUtils.drawRoundRectOutline(x, y, width, HEADER_H, 3.0F, 0.5F, outlineColor);

        String arrow = setting.isOpen() ? "▼" : "▶";
        float textY = y + HEADER_H / 2.0F - FontManager.get(10).getHeight() / 2.0F;
        FontManager.get(8).drawString(guiGraphics, arrow, x + 4.0F, textY, textInactive);

        FontManager.get(10).drawString(guiGraphics, setting.getName(), x + 14.0F, textY, textActive);

        if (anim <= 0.001F) return;

        float childAlpha = alpha * anim;
        float childX = x + INDENT;
        float childW = width - INDENT;
        float childY = y + HEADER_H + 2.0F;

        RenderUtils.fillRoundRect(guiGraphics,
                x + 1.5F, y + HEADER_H,
                1.0F, calcChildrenHeight() * anim,
                0.5F, ColorUtil.multAlpha(outlineColor, anim));

        for (Setting child : setting.getVisibleChildren()) {
            IComponent comp = getOrCreate(child);
            comp.render(guiGraphics, childX, childY, childW, mouseX, mouseY,
                    Colors.outline(childAlpha),
                    Colors.accent(childAlpha),
                    Colors.bgPrimary(childAlpha),
                    Colors.textInactive(childAlpha),
                    Colors.textActive(childAlpha),
                    childAlpha);
            childY += comp.getHeight() + CHILD_GAP;
        }
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        if (hovered(mouseX, mouseY, x, y, width, HEADER_H)) {
            setting.toggle();
            return true;
        }

        float anim = (float) setting.getOpenAnim().getOutput();
        if (anim <= 0.001F) return false;

        float childX = x + INDENT;
        float childW = width - INDENT;
        float childY = y + HEADER_H + 2.0F;

        for (Setting child : setting.getVisibleChildren()) {
            IComponent comp = getOrCreate(child);
            if (comp.handleClick(childX, childY, childW, mouseX, mouseY, button)) return true;
            childY += comp.getHeight() + CHILD_GAP;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        for (IComponent comp : childCache.values()) {
            if (comp.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return false;
    }

    @Override
    public void mouseReleased() {
        childCache.values().forEach(IComponent::mouseReleased);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (IComponent comp : childCache.values()) {
            if (comp.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (IComponent comp : childCache.values()) {
            if (comp.charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }

    private IComponent getOrCreate(Setting setting) {
        return childCache.computeIfAbsent(setting, factory::create);
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
