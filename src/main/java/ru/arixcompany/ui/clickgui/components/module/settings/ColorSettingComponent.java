package ru.arixcompany.ui.clickgui.components.module.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.features.module.setting.implement.ColorSetting;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.awt.*;

@RequiredArgsConstructor
public final class ColorSettingComponent implements IComponent {

    @Getter private final ColorSetting setting;
    @Setter private ColorPickerPositionProvider positionProvider;

    @FunctionalInterface
    public interface ColorPickerPositionProvider {
        float[] findPosition(ColorSetting colorSetting);
    }

    private static final float BTN_W = 51F;
    private static final float BTN_H = 10.075F;


    @Override
    public float getHeight() {
        return 15.0F;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {
        float colorW = 40.0F;
        float colorX = x + width - colorW - 2.0F;

        FontManager.get(10).drawString(guiGraphics,setting.getName(), x, y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F),textInactive);

        Color hueColor = setting.getColor();
        RenderUtils.drawRoundRectOutline(colorX - 15.0F,  y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F), BTN_W, BTN_H, 3.0F,0.1F, outlineColor);
        RenderUtils.fillRoundRect(guiGraphics, colorX - 15.0F,  y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F), BTN_W, BTN_H, 3.0F, bgColor);
        RenderUtils.fillRoundRect(
                colorX + 32.0F - 10.0F, y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F), 13.285F, 8.315F,
                0.0F, 3.0F, 3.0F, 0.0F,
                ColorUtil.replAlpha(hueColor.getRGB(), (int) (255.0F * alpha))
        );

        String hex = String.format("#%02X%02X%02X",
                hueColor.getRed(), hueColor.getGreen(), hueColor.getBlue());
        float hexW = FontManager.get(10).getWidth(hex);
        FontManager.get(10).drawString(guiGraphics,hex,  colorX + colorW / 2.0F - hexW / 2.0F - 16.0F,
                y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F),textInactive);
    }

    public void renderColorPicker(float mainAlpha) {
        if (Gui.activeColorPicker != setting) return;
        if (Gui.colorPickerX == 0 && Gui.colorPickerY == 0) return;

        float alpha   = mainAlpha * Gui.animation15.getOutput();
        int outline   = Colors.outline(alpha);
        int bg        = Colors.bgSecondary(alpha);
        float a15     = (float) Gui.animation15.getOutput();
        float offX    = 30.0F - 30.0F * a15;

        float pickerX = Gui.colorPickerX;
        float pickerY = Gui.colorPickerY;

        RenderUtils.drawRoundRectOutline(pickerX + offX, pickerY, 73.64F, 64.68F, 5.5F,0.1F, outline);
        RenderUtils.fillRoundRect(pickerX + offX, pickerY, 73.64F, 64.68F, 5.5F, bg);

        float palW = 63.92F, palH = 47.02F;
        float palX = pickerX + 5.0F + offX;
        float palY = pickerY + 5.0F;
        float hue  = setting.getCurrent() / 106.0F;

        Color base = Color.getHSBColor(hue, 1.0F, 1.0F);
        RenderUtils.horizontalGradient(palX, palY, palW, palH, Color.WHITE.getRGB(), base.getRGB());
        RenderUtils.verticalGradient(palX, palY, palW, palH,
                new Color(0, 0, 0, 0).getRGB(), new Color(0, 0, 0, 255).getRGB());

        RenderUtils.fillRoundRect(
                palX + setting.getSaturation() * palW - 2.5F,
                palY + (1.0F - setting.getBrightness()) * palH - 2.5F,
                5.0F, 5.0F, 3.0F, Color.WHITE.getRGB()
        );

        float hueSliderX = pickerX + 5.0F + offX;
        float hueSliderY = palY + palH + 5.0F;
        float hueSliderW = 64.0F, hueSliderH = 2.59F;

        RenderUtils.drawRoundRectOutline(hueSliderX, hueSliderY, hueSliderW, hueSliderH, 2.0F,0.5F, outline);
        renderHueSlider(hueSliderX, hueSliderY, hueSliderW, hueSliderH, a15);

        RenderUtils.fillRoundRect(
                hueSliderX + setting.getCurrent() / 106.0F * hueSliderW - 3.0F,
                hueSliderY, 4.7F, 4.7F, 2.0F, Color.WHITE.getRGB()
        );
    }

    private void renderHueSlider(float x, float y, float w, float h, float a15) {
        float segW = w / 6.0F;
        Color[] colors = {
                Color.getHSBColor(0.0F,    1, 1), Color.getHSBColor(0.1667F, 1, 1),
                Color.getHSBColor(0.3333F, 1, 1), Color.getHSBColor(0.5F,    1, 1),
                Color.getHSBColor(0.6667F, 1, 1), Color.getHSBColor(0.8333F, 1, 1),
                Color.getHSBColor(1.0F,    1, 1)
        };
        for (int i = 0; i < 6; i++) {
            RenderUtils.horizontalGradient(
                    x + i * segW, y + 1.0F, segW, h,
                    ColorUtil.multAlpha(colors[i].getRGB(), a15),
                    ColorUtil.multAlpha(colors[i + 1].getRGB(), a15)
            );
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (Gui.activeColorPicker != setting) return false;
        if (Gui.colorPickerX == 0 && Gui.colorPickerY == 0) return false;

        float palW = 63.92F, palH = 47.02F;
        float palX = Gui.colorPickerX + 5.0F;
        float palY = Gui.colorPickerY + 5.0F;

        if (button == 0 && hovered(mouseX, mouseY, palX, palY, palW, palH)) {
            Gui.pickingSaturationBrightness = true;
            setting.setSaturation(clamp01((mouseX - palX) / palW));
            setting.setBrightness(1.0F - clamp01((mouseY - palY) / palH));
            return true;
        }

        float hueX = Gui.colorPickerX + 5.0F;
        float hueY = palY + palH + 5.0F;
        if (button == 0 && hovered(mouseX, mouseY, hueX, hueY, 64.0F, 3.59F)) {
            Gui.pickingHue = true;
            setting.setCurrent(clamp01((mouseX - hueX) / 64.0F) * 106.0F);
            return true;
        }

        return hovered(mouseX, mouseY, Gui.colorPickerX, Gui.colorPickerY, 73.64F, 64.68F);
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        float colorX = x + width - 40.0F - 2.0F;
        float btnX   = colorX - 10.0F;

        if (button == 0 && hovered(mouseX, mouseY, btnX, y, BTN_W, BTN_H)) {
            if (Gui.activeColorPicker == setting) {
                Gui.animation15.setDirection(Direction.BACKWARDS);
                Gui.activeColorPicker = null;
                Gui.colorPickerX = 0;
                Gui.colorPickerY = 0;
            } else {
                Gui.activeColorPicker = setting;
                Gui.animation15.setDirection(Direction.FORWARDS);
                if (positionProvider != null) {
                    float[] pos = positionProvider.findPosition(setting);
                    if (pos != null) {
                        Gui.colorPickerX = pos[0];
                        Gui.colorPickerY = pos[1];
                    }
                }
            }
            return true;
        }

        if (Gui.activeColorPicker == setting
                && (Gui.colorPickerX != 0 || Gui.colorPickerY != 0)) {

            float pX = Gui.colorPickerX, pY = Gui.colorPickerY;
            float paletteW = 63.92F, paletteH = 47.02F;
            float paletteX = pX + 5.0F, paletteY = pY + 5.0F;

            if (hovered(mouseX, mouseY, paletteX, paletteY, paletteW, paletteH)) {
                Gui.pickingSaturationBrightness = true;
                setting.setSaturation(clamp01((mouseX - paletteX) / paletteW));
                setting.setBrightness(1.0F - clamp01((mouseY - paletteY) / paletteH));
                return true;
            }

            float hueSliderX = pX + 5.0F, hueSliderY = paletteY + paletteH + 5.0F;
            if (hovered(mouseX, mouseY, hueSliderX, hueSliderY, 64.0F, 2.59F)) {
                Gui.pickingHue = true;
                setting.setCurrent(clamp01((mouseX - hueSliderX) / 64.0F) * 106.0F);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        if (Gui.activeColorPicker != setting) return false;
        if (Gui.colorPickerX == 0 && Gui.colorPickerY == 0) return false;

        float pX = Gui.colorPickerX, pY = Gui.colorPickerY;
        float paletteW = 63.92F, paletteH = 47.02F;
        float paletteX = pX + 5.0F, paletteY = pY + 5.0F;

        if (Gui.pickingSaturationBrightness) {
            setting.setSaturation(clamp01((mouseX - paletteX) / paletteW));
            setting.setBrightness(1.0F - clamp01((mouseY - paletteY) / paletteH));
            return true;
        }

        if (Gui.pickingHue) {
            setting.setCurrent(clamp01((mouseX - (pX + 5.0F)) / 64.0F) * 106.0F);
            return true;
        }

        return false;
    }


    @Override
    public void mouseReleased() {
        if (Gui.activeColorPicker == setting) {
            Gui.pickingSaturationBrightness = false;
            Gui.pickingHue = false;
        }
    }

    private float clamp01(float v) { return Math.max(0.0F, Math.min(1.0F, v)); }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}