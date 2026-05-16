package ru.arixcompany.ui.clickgui.components.module.settings;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.utils.math.StringUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

@RequiredArgsConstructor
public final class BindSettingComponent implements IComponent {

    @Getter private final BindSetting setting;

    private static final float BIND_HEIGHT   = 10.075F;
    private static final float MIN_BTN_WIDTH = 16.055F;

    @Override
    public float getHeight() {
        return 13.0F;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {
        String displayName = (setting.getName() != null && !setting.getName().isEmpty()) ? setting.getName() : "KEY";
        String keyText     = setting.active ? "..." : StringUtil.getBindName(setting.getKey());
        float keyW         = FontManager.get(10).getWidth(keyText);
        float btnW         = Math.max(MIN_BTN_WIDTH, keyW + 8.0F);

        float bgX = calcBgX(x, width, btnW);
        float bgW = calcBgW(x, width, btnW);

        float textY = y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F);

        if (hovered(mouseX, mouseY, bgX, textY, bgW, BIND_HEIGHT)) {
            guiGraphics.requestCursor(CursorTypes.POINTING_HAND);
        }

        FontManager.get(10).drawString(guiGraphics, displayName, x, textY, textInactive);

        RenderUtils.drawRoundRectOutline(bgX, textY, bgW, BIND_HEIGHT, 3.0F,0.1F, outlineColor);
        RenderUtils.fillRoundRect(bgX, textY, bgW, BIND_HEIGHT, 3.0F, bgColor);

        FontManager.get(10).drawString(guiGraphics,keyText,  bgX + bgW / 2.0F - keyW / 2.0F,
                textY,setting.active ? accentColor : textInactive);
    }

    @Override
    public boolean handleClick( float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        String keyText = setting.active ? "..." : StringUtil.getBindName(setting.getKey());
        float keyW     = FontManager.get(10).getWidth(keyText);
        float btnW     = Math.max(MIN_BTN_WIDTH, keyW + 8.0F);

        float bgX = calcBgX(x, width, btnW);
        float bgW = calcBgW(x, width, btnW);

        if (!hovered(mouseX, mouseY, bgX, y, bgW, BIND_HEIGHT)) return false;

        if (button == 0) {
            if (Gui.activeBindSetting != setting) {
                if (Gui.activeBindSetting != null) Gui.activeBindSetting.active = false;
                Gui.activeBindSetting = setting;
                setting.active = true;
            }
            return true;
        }

        if (Gui.activeBindSetting == setting && button >= 0) {
            setting.setKey(-100 - button);
            setting.active = false;
            Gui.activeBindSetting = null;
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Gui.activeBindSetting != setting) return false;

        if (keyCode == 256) {
            setting.active = false;
            Gui.activeBindSetting = null;
            return true;
        }

        if (keyCode == 261) {
            setting.setKey(-1);
            setting.active = false;
            Gui.activeBindSetting = null;
            return true;
        }

        setting.setKey(keyCode);
        setting.active = false;
        Gui.activeBindSetting = null;
        return true;
    }

    private float calcBgX(float x, float width, float btnW) {
        float bindBtnX = x + width - btnW - 2.0F;
        if (bindBtnX < x) bindBtnX = x;
        return Math.max(bindBtnX - 6.0F, x);
    }

    private float calcBgW(float x, float width, float btnW) {
        float bindBtnX = x + width - btnW - 2.0F;
        if (bindBtnX < x) return width - 2.0F;
        float bgX = bindBtnX - 6.0F;
        float raw = btnW + 2.0F;
        if (bgX < x) return bgX + raw - x;
        return raw;
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}