package ru.arixcompany.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class RenderUtils {

    public static void fillRoundRect(float x, float y, float w, float h, float radius, int color) {
        RoundRectShader.drawRoundRect(x, y, w, h, radius, color);
    }

    public static void fillRoundRect(float x, float y, float w, float h,
                                     float tl, float tr, float br, float bl, int color) {
        RoundRectShader.drawRoundRect(x, y, w, h, tl, tr, br, bl, color);
    }

    public static void drawRoundRectOutline(float x, float y, float w, float h,
                                            float radius, float thickness, int color) {
        RoundRectShader.drawRoundRectOutline(x, y, w, h, radius, thickness, color);
    }

    public static void fillRoundRectGradient(float x, float y, float w, float h,
                                             float radius, int topColor, int bottomColor) {
        RoundRectShader.drawRoundRectGradient(x, y, w, h, radius, topColor, bottomColor);
    }

    public static void drawShadow(float x, float y, float w, float h,
                                  float radius, int layers, int shadowColor) {
        RoundRectShader.drawShadow(x, y, w, h, radius, layers, shadowColor);
    }
}