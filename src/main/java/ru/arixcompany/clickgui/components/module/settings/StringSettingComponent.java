package ru.arixcompany.clickgui.components.module.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import ru.arixcompany.clickgui.Gui;
import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.module.setting.implement.TextSetting;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

@RequiredArgsConstructor
public final class StringSettingComponent implements IComponent {

    @Getter private final TextSetting setting;

    private static final float FIELD_W       = 63.56F;
    private static final float FIELD_H       = 10.075F;
    private static final float FIELD_OFFSET  = 60.0F;
    private static final int   MAX_VISIBLE   = 16;

    @Override
    public float getHeight() {
        return 15.0F;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {
        float fieldX = x + FIELD_OFFSET;
        float fieldCenterY = y + (FIELD_H / 2.0F);
        float textY = fieldCenterY - (FontManager.get(10).getHeight() / 2.0F);
        float textX = fieldX + 5.0F;

        // Название настройки
        FontManager.get(10).drawString(guiGraphics, setting.getName(), x, y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F), textInactive);

        // Поле ввода
        RenderUtils.drawRoundRectOutline(fieldX, y, FIELD_W, FIELD_H, 3.0F, 0.1F, outlineColor);
        RenderUtils.fillRoundRect(fieldX, y, FIELD_W, FIELD_H, 3.0F, bgColor);

        String input = setting.getText();
        float cursorX = textX;

        if (input.isEmpty()) {
            FontManager.get(10).drawString(guiGraphics, "Enter text", textX, textY, ColorUtil.replAlpha(textInactive, (int)(ColorUtil.getAlpha(textInactive) * 0.5f)));
        } else {
            float curX = textX;
            float maxX = fieldX + FIELD_W - 5.0F;
            for (int i = 0; i < input.length(); i++) {
                String ch = String.valueOf(input.charAt(i));
                float charW = FontManager.get(10).getWidth(ch);
                if (curX + charW > maxX) {
                    cursorX = curX;
                    break;
                }

                int charColor = computeCharColor(input, i, textX, curX, maxX, textActive);
                FontManager.get(10).drawString(guiGraphics, ch, curX, textY, charColor);
                curX += charW;
                cursorX = curX;
            }
        }

        // Курсор
        if (Gui.activeStringSetting == setting && setting.active
                && System.currentTimeMillis() / 500L % 2L == 0L) {
            RenderUtils.fillRoundRect(cursorX, textY - 1.0F, 1.0F, FontManager.get(10).getHeight() + 2.0F, 0.5F, accentColor);
        }
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        float fieldX = x + FIELD_OFFSET;

        if (button == 0 && hovered(mouseX, mouseY, fieldX, y, FIELD_W, FIELD_H)) {
            if (Gui.activeStringSetting != setting) {
                if (Gui.activeStringSetting != null) Gui.activeStringSetting.active = false;
                Gui.activeStringSetting = setting;
                setting.active = true;
            }
            return true;
        }

        if (button == 0 && Gui.activeStringSetting == setting) {
            Gui.activeStringSetting.active = false;
            Gui.activeStringSetting = null;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Gui.activeStringSetting != setting) return false;

        if (keyCode == 256) { // ESC
            setting.active = false;
            Gui.activeStringSetting = null;
            return true;
        }
        if (keyCode == 259) { // Backspace
            if (!setting.getText().isEmpty()) {
                setting.setText(setting.getText().substring(0, setting.getText().length() - 1));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (Gui.activeStringSetting != setting) return false;

        if (codePoint == '\b') {
            if (!setting.getText().isEmpty()) {
                setting.setText(setting.getText().substring(0, setting.getText().length() - 1));
            }
            return true;
        }
        if (codePoint >= ' ' && codePoint != 127 && setting.getText().length() < 16) {
            setting.text += codePoint;
            return true;
        }
        return false;
    }

    private int computeCharColor(String input, int i,
                                 float textX, float curX, float maxX, int textInactive) {
        if (i < MAX_VISIBLE) return textInactive;
        float fadeStartX = textX + FontManager.get(10).getWidth(input.substring(0, MAX_VISIBLE));
        float gradientW  = Math.min(30.0F, maxX - fadeStartX);
        if (gradientW > 0.0F) {
            float fade = Mth.clamp((curX - fadeStartX) / gradientW, 0.0F, 1.0F);
            int a = (int) (((textInactive >> 24) & 0xFF) * (1.0F - fade));
            return ColorUtil.replAlpha(textInactive, a);
        }
        return ColorUtil.replAlpha(textInactive, 0);
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}