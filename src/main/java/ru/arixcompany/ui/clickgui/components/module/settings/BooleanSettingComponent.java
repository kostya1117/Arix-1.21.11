package ru.arixcompany.ui.clickgui.components.module.settings;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.utils.math.StringUtil;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;
import org.lwjgl.glfw.GLFW;

@RequiredArgsConstructor
public final class BooleanSettingComponent implements IComponent {

    @Getter private final BooleanSetting setting;

    private static final float TOGGLE_W = 20.0F;
    private static final float TOGGLE_H = 10.0F;
    private static final float BIND_HEIGHT = 10.075F;
    private static final float MIN_BTN_WIDTH = 16.055F;

    @Override
    public float getHeight() {
        return 15.0F;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {

        Gui.animRun(setting.anim, setting.isValue() ? 1.0 : 0.0);
        float anim = setting.anim.getOutput();
        float thumb_size = 6.0F;

        float toggleX = x + width - TOGGLE_W - 3.0F;
        float toggleY = y + (getHeight() / 2.0F) - (TOGGLE_H / 2.0F);

        // Render toggle
        if (hovered(mouseX, mouseY, toggleX, toggleY, TOGGLE_W, TOGGLE_H)) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        RenderUtils.fillRoundRect(guiGraphics, toggleX, toggleY, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2.0F, bgColor);

        float thumbX = toggleX + 2.0F + (TOGGLE_W - thumb_size - 4.0F) * anim;
        float thumbY = toggleY + (TOGGLE_H / 2.0F) - (thumb_size / 2.0F);
        int thumbColor = ColorUtil.overCol(textInactive, accentColor, anim);
        RenderUtils.fillRoundRect(guiGraphics, thumbX, thumbY, thumb_size, thumb_size, thumb_size / 2.0F, thumbColor);

        // Render bind button only if active (binding mode)
        if (setting.isActive()) {
            String keyText = "...";
            float keyW = FontManager.get(10).getWidth(keyText);
            float btnW = Math.max(MIN_BTN_WIDTH, keyW + 8.0F);

            float bgX = calcBgX(x, toggleX, btnW);
            float bgW = calcBgW(x, toggleX, btnW);

            float textY = y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F);

            if (hovered(mouseX, mouseY, bgX, textY, bgW, BIND_HEIGHT)) {
                guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
            }

            RenderUtils.drawRoundRectOutline(bgX, textY, bgW, BIND_HEIGHT, 3.0F, 0.1F, outlineColor);
            RenderUtils.fillRoundRect(guiGraphics, bgX, textY, bgW, BIND_HEIGHT, 3.0F, bgColor);

            FontManager.get(10).drawString(guiGraphics, keyText, bgX + bgW / 2.0F - keyW / 2.0F,
                    textY, accentColor);
        }

        // Render label
        float textY = y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F);
        FontManager.get(10).drawString(guiGraphics, setting.getName(), x, textY, textInactive);
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        float toggleX = x + width - TOGGLE_W - 3.0F;
        float toggleY = y + (getHeight() / 2.0F) - (TOGGLE_H / 2.0F);

        // Left click - toggle value
        if (button == 0) {
            if (hovered(mouseX, mouseY, toggleX, toggleY, TOGGLE_W, TOGGLE_H)) {
                setting.setValue(!setting.isValue());
                return true;
            }
            // clicking anywhere else on the row while in bind mode — cancel
            if (setting.isActive() && hovered(mouseX, mouseY, x, y, width, getHeight())) {
                setting.setActive(false);
                Gui.activeBooleanBindSetting = null;
                return true;
            }
        }

        // Right click while in bind mode — bind RMB
        if (button == 1 && setting.isActive()) {
            if (hovered(mouseX, mouseY, x, y, width, getHeight())) {
                setting.setKey(-100 - 1);
                setting.setActive(false);
                Gui.activeBooleanBindSetting = null;
                return true;
            }
        }

        // Middle click (wheel) - start bind mode, only if hovering this row
        if (button == 2) {
            if (!hovered(mouseX, mouseY, x, y, width, getHeight())) return false;

            if (Gui.activeBooleanBindSetting != setting) {
                if (Gui.activeBooleanBindSetting != null) Gui.activeBooleanBindSetting.setActive(false);
                Gui.activeBooleanBindSetting = setting;
                setting.setActive(true);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Gui.activeBooleanBindSetting != setting) return false;

        if (keyCode == 256) {
            setting.setActive(false);
            Gui.activeBooleanBindSetting = null;
            return true;
        }

        if (keyCode == 261) {
            setting.setKey(GLFW.GLFW_KEY_UNKNOWN);
            setting.setActive(false);
            Gui.activeBooleanBindSetting = null;
            return true;
        }

        setting.setKey(keyCode);
        setting.setActive(false);
        Gui.activeBooleanBindSetting = null;
        return true;
    }

    private float calcBgX(float x, float toggleX, float btnW) {
        float bindBtnX = toggleX - btnW - 8.0F;
        if (bindBtnX < x) bindBtnX = x;
        return Math.max(bindBtnX - 6.0F, x);
    }

    private float calcBgW(float x, float toggleX, float btnW) {
        float bindBtnX = toggleX - btnW - 8.0F;
        if (bindBtnX < x) return toggleX - x - 8.0F;
        float bgX = bindBtnX - 6.0F;
        float raw = btnW + 2.0F;
        if (bgX < x) return bgX + raw - x;
        return raw;
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}