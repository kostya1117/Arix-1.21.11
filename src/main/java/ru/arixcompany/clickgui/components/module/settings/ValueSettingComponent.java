package ru.arixcompany.clickgui.components.module.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.clickgui.Gui;
import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.module.setting.implement.SliderSetting;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

@RequiredArgsConstructor
public final class ValueSettingComponent implements IComponent {

    @Getter private final SliderSetting setting;

    @Override
    public float getHeight() {
        return 19.0F;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {
        float sliderY = y + 10.0F;
        float sliderW = width - 2.5F;
        float progress = clamp01((setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin()));
        float progressW = sliderW * progress;

        RenderUtils.drawRoundRectOutline(x, sliderY + 2.0F, sliderW, 4.0F, 2.0F,0.3F, outlineColor);
        RenderUtils.fillRoundRect(x, sliderY + 2.0F, sliderW, 4.0F, 2.0F, bgColor);

        if (progressW > 2.0F) {
            RenderUtils.fillRoundRect(x + 1.0F, sliderY + 2.5F, progressW - 2.0F, 3.0F, 2.0F, accentColor);
        }

        float thumbX = x + 1.0F + progressW - 5.0F + (progressW < 1.0F ? 5 : 2);
        RenderUtils.fillRoundRect(thumbX, sliderY + 2.2F, 5.0F, 3.88F, 2.0F, textActive);

        String valueText = formatValue();
        float valueW    = FontManager.get(13).getWidth(valueText);

        FontManager.get(13).drawString(guiGraphics,setting.getName(),x, y + 1.0F + 7.0F,textInactive);
        FontManager.get(13).drawString(guiGraphics,valueText,x + sliderW - valueW - 2.0F, y + 7.0F,accentColor);
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        float sliderW       = width - 2.5F;
        float sliderActualY = y + 12.0F;

        if (button == 0 && hovered(mouseX, mouseY, x, sliderActualY, sliderW, 4.0F)) {
            Gui.activeValueSetting = setting;
            Gui.sliderX     = x;
            Gui.sliderY     = sliderActualY;
            Gui.sliderWidth = sliderW;

            float progress = clamp01((mouseX - x) / sliderW);
            setting.setValue(snap(setting.getMin() + (setting.getMax() - setting.getMin()) * progress));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        if (Gui.activeValueSetting != setting) return false;

        float progress = clamp01((mouseX - Gui.sliderX) / Gui.sliderWidth);
        setting.setValue(snap(setting.getMin() + (setting.getMax() - setting.getMin()) * progress));
        return true;
    }

    @Override
    public void mouseReleased() {
        if (Gui.activeValueSetting == setting) {
            Gui.activeValueSetting = null;
            Gui.sliderX = Gui.sliderY = Gui.sliderWidth = 0;
        }
    }

    private String formatValue() {
        if (setting.getStep() >= 1.0F)  return String.valueOf((int) setting.getValue());
        if (setting.getStep() >= 0.1F)  return String.format("%.1f", setting.getValue());
        if (setting.getStep() >= 0.01F) return String.format("%.2f", setting.getValue());
        return String.format("%.3f", setting.getValue());
    }

    private float snap(float value) {
        float c = Math.max(setting.getMin(), Math.min(setting.getMax(), value));
        if (setting.getStep() <= 0) return c;
        float s = setting.getMin() + Math.round((c - setting.getMin()) / setting.getStep()) * setting.getStep();
        return Math.max(setting.getMin(), Math.min(setting.getMax(), Math.round(s * 10000.0F) / 10000.0F));
    }

    private float clamp01(float v) { return Math.max(0, Math.min(1, v)); }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}