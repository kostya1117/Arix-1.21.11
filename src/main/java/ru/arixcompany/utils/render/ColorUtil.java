package ru.arixcompany.utils.render;

import net.minecraft.util.Mth;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.features.module.Theme;

import java.awt.*;

public class ColorUtil {
    public static float getAlpha(int color) {
        return (color >> 24 & 0xFF) / 255.0F;
    }

    public static Color setAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static int setAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }

    private static Theme getTheme() {
        return Gui.selectedTheme != null ? Gui.selectedTheme : Theme.PURPLE;
    }

    private static Theme getPreTheme() {
        return Gui.preSelectedTheme != null ? Gui.preSelectedTheme : Theme.PURPLE;
    }

    public static int getMainColor(int speed, int index) {
        Theme theme = getTheme();
        Theme preTheme = getPreTheme();
        return gradient2(
                interpolate(theme.getMain().getRGB(), preTheme.getMain().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                interpolate(theme.getMain().getRGB(), preTheme.getMain().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                speed,
                index);
    }

    public static int getTextColor(int speed, int index) {
        Theme theme = getTheme();
        Theme preTheme = getPreTheme();
        return gradient2(
                interpolate(theme.getText().getRGB(), preTheme.getText().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                interpolate(theme.getText().getRGB(), preTheme.getText().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                speed,
                index);
    }

    public Color interpolate(Color color1, Color color2, double amount) {
        amount = 1.0 - amount;
        amount = (float) Mth.clamp(amount, 0.0, 1.0);
        return new Color(
                (int) Mth.lerp(color1.getRed(), color2.getRed(), amount),
                (int) Mth.lerp(color1.getGreen(), color2.getGreen(), amount),
                (int) Mth.lerp(color1.getBlue(), color2.getBlue(), amount),
                (int) Mth.lerp(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static int gradient2(int color1, int color2, int speed, int index) {
        Color col1 = new Color(color1);
        Color col2 = new Color(color2);
        double angle = (System.currentTimeMillis() / speed + index) % 360L;
        double var13;
        float ratio = (float) ((var13 = angle % 360.0) / 360.0);
        int red = (int) (col1.getRed() * (1.0F - ratio) + col2.getRed() * ratio);
        int green = (int) (col1.getGreen() * (1.0F - ratio) + col2.getGreen() * ratio);
        int blue = (int) (col1.getBlue() * (1.0F - ratio) + col2.getBlue() * ratio);
        Color interpolatedColor = new Color(red, green, blue);
        return interpolatedColor.getRGB();
    }

    public static int interpolate(int color1, int color2, double amount) {
        amount = Math.max(0.0, Math.min(1.0, amount));

        int a1 = (color1 >> 24) & 0xFF, a2 = (color2 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF, r2 = (color2 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF,  g2 = (color2 >> 8) & 0xFF;
        int b1 = color1 & 0xFF,         b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * amount);
        int r = (int) (r1 + (r2 - r1) * amount);
        int g = (int) (g1 + (g2 - g1) * amount);
        int b = (int) (b1 + (b2 - b1) * amount);

        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
        float hue = angle / 360.0F;
        int color = Color.HSBtoRGB(hue, saturation, brightness);
        return getColor(red(color), green(color), blue(color), Math.max(0, Math.min(255, (int) (opacity * 255.0F))));
    }

    public static int gradient(int speed, int index, int... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int) (angle / 360.0F * colors.length);
        if (colorIndex == colors.length) {
            colorIndex--;
        }

        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return interpolateColor(color1, color2, angle / 360.0F * colors.length - colorIndex);
    }

    public static int interpolateColor(int color1, int color2, double offset) {
        float[] rgba1 = getRGBAf(color1);
        float[] rgba2 = getRGBAf(color2);
        double r = rgba1[0] + (rgba2[0] - rgba1[0]) * offset;
        double g = rgba1[1] + (rgba2[1] - rgba1[1]) * offset;
        double b = rgba1[2] + (rgba2[2] - rgba1[2]) * offset;
        double a = rgba1[3] + (rgba2[3] - rgba1[3]) * offset;
        return rgba((int) (r * 255.0), (int) (g * 255.0), (int) (b * 255.0), (int) (a * 255.0));
    }

    public static float[] getRGBAf(int c) {
        return new float[] { red(c) / 255.0F, green(c) / 255.0F, blue(c) / 255.0F, alpha(c) / 255.0F };
    }

    public static int applyOpacity(int n, float f) {
        return rgba2(getRedInt(n), getGreenInt(n), getBlueInt(n), (int) (getAlphaInt(n) * f / 255.0F));
    }

    public static int rgba2(int n, int n2, int n3, int n4) {
        return n4 << 24 | n << 16 | n2 << 8 | n3;
    }

    public static int getRedInt(int n) {
        return n >> 16 & 0xFF;
    }

    public static int getGreenInt(int n) {
        return n >> 8 & 0xFF;
    }

    public static int getBlueInt(int n) {
        return n & 0xFF;
    }

    public static int getAlphaInt(int n) {
        return n >> 24 & 0xFF;
    }

    public static Color getColor(int color) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int a = color >> 24 & 0xFF;
        return new Color(r, g, b, a);
    }

    public static int replAlpha(int c, int a) {
        return getColor(red(c), green(c), blue(c), a);
    }

    public static int red(int c) {
        return c >> 16 & 0xFF;
    }

    public static int green(int c) {
        return c >> 8 & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    public static int alpha(int c) {
        return c >> 24 & 0xFF;
    }

    public static int getColor(float r, float g, float b, float a) {
        int ri = Math.clamp(Math.round(r), 0, 255);
        int gi = Math.clamp(Math.round(g), 0, 255);
        int bi = Math.clamp(Math.round(b), 0, 255);
        int ai = Math.clamp(Math.round(a), 0, 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }

    public static int getColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= alpha << 24;
        color |= red << 16;
        color |= green << 8;
        return color | blue;
    }


    public static float[] rgb(int color) {
        return new float[] { (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                (color >> 24 & 0xFF) / 255.0F };
    }

    public static int rgba(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }


    public static float[] rgba(int color) {
        return new float[] { (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                (color >> 24 & 0xFF) / 255.0F };
    }

    public static int overCol(int color1, int color2, float percent01) {
        float percent = Mth.clamp(percent01, 0.0F, 1.0F);
        return getColor(
                Mth.lerp(percent, red(color1),   red(color2)),
                Mth.lerp(percent, green(color1), green(color2)),
                Mth.lerp(percent, blue(color1),  blue(color2)),
                Mth.lerp(percent, alpha(color1), alpha(color2))
        );
    }

    public static int multAlpha(int color, float percent01) {
        return getColor(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
    }

    public static int argb(int r, int g, int b, float alpha) {
        int a = (int)(Mth.clamp(alpha, 0f, 1f) * 255f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}