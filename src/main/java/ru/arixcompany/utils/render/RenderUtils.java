package ru.arixcompany.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.optifine.render.BufferUploader;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;

public class RenderUtils {

    private static final ArrayDeque<float[]> transformStack = new ArrayDeque<>();

    public static void pushScale(float scale) {
        pushScale(scale, scale, 0.0f, 0.0f);
    }

    public static void pushScale(float sx, float sy) {
        pushScale(sx, sy, 0.0f, 0.0f);
    }

    public static void pushScale(float scale, float originX, float originY) {
        pushScale(scale, scale, originX, originY);
    }

    public static void pushScale(float sx, float sy, float originX, float originY) {
        float[] current = transformStack.isEmpty() ? identity() : transformStack.peek();
        float[] scale = new float[]{
                sx, 0,  originX * (1.0f - sx),
                0,  sy, originY * (1.0f - sy)
        };
        transformStack.push(multiply(current, scale));
    }

    public static void pushScaleCentered(float scale) {
        pushScaleCentered(scale, scale);
    }

    public static void pushScaleCentered(float sx, float sy) {
        Minecraft mc = Minecraft.getInstance();
        float cx = mc.getWindow().getGuiScaledWidth() * 0.5f;
        float cy = mc.getWindow().getGuiScaledHeight() * 0.5f;
        pushScale(sx, sy, cx, cy);
    }

    public static void pushTranslation(float tx, float ty) {
        float[] current = transformStack.isEmpty() ? identity() : transformStack.peek();
        float[] translation = new float[]{
                1, 0, tx,
                0, 1, ty
        };
        transformStack.push(multiply(current, translation));
    }

    public static void pushRotation(float degrees) {
        float[] current = transformStack.isEmpty() ? identity() : transformStack.peek();
        float rad = (float) Math.toRadians(degrees);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float[] rotation = new float[]{
                cos, -sin, 0,
                sin,  cos, 0
        };
        transformStack.push(multiply(current, rotation));
    }

    public static void popTransform() {
        if (!transformStack.isEmpty()) {
            transformStack.pop();
        }
    }

    public static void popScale() {
        popTransform();
    }

    public static void popRotation() {
        popTransform();
    }

    public static float[] currentTransform() {
        return transformStack.isEmpty() ? identity() : transformStack.peek();
    }

    public static float transformX(float x, float y) {
        float[] m = currentTransform();
        return m[0] * x + m[1] * y + m[2];
    }

    public static float transformY(float x, float y) {
        float[] m = currentTransform();
        return m[3] * x + m[4] * y + m[5];
    }

    private static final ArrayDeque<ClipRect> clipStack = new ArrayDeque<>();

    private record ClipRect(int x, int y, int w, int h) {
        static ClipRect intersect(ClipRect a, ClipRect b) {
            int nx = Math.max(a.x, b.x);
            int ny = Math.max(a.y, b.y);
            int nr = Math.min(a.x + a.w, b.x + b.w);
            int nb = Math.min(a.y + a.h, b.y + b.h);
            int nw = Math.max(0, nr - nx);
            int nh = Math.max(0, nb - ny);
            return new ClipRect(nx, ny, nw, nh);
        }
    }

    public static void pushRoundedClipRect(float x, float y, float w, float h, float radius) {
        pushRoundedClipRect(x, y, w, h, radius, radius, radius, radius);
    }

    public static void pushRoundedClipRect(float x, float y, float w, float h,
                                           float tl, float tr, float br, float bl) {
        Minecraft mc = Minecraft.getInstance();
        float scale = (float) mc.getWindow().getGuiScale();

        int scissorX = (int) (x * scale);
        int scissorY = (int) ((mc.getWindow().getGuiScaledHeight() - y - h) * scale);
        int scissorW = (int) (w * scale);
        int scissorH = (int) (h * scale);

        ClipRect incoming = new ClipRect(scissorX, scissorY, scissorW, scissorH);
        ClipRect applied = clipStack.isEmpty() ? incoming : ClipRect.intersect(clipStack.peek(), incoming);
        clipStack.push(applied);

        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilMask(0xFF);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glColorMask(false, false, false, false);
        GL11.glDepthMask(false);

        fillRoundRect(x, y, w, h, tl, tr, br, bl, 0xFFFFFFFF);

        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(true);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilMask(0x00);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(applied.x, applied.y, applied.w, applied.h);
    }

    public static void popClipRect() {
        if (!clipStack.isEmpty()) {
            clipStack.pop();
        }

        if (clipStack.isEmpty()) {
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glStencilMask(0xFF);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
        } else {
            ClipRect prev = clipStack.peek();
            GL11.glScissor(prev.x, prev.y, prev.w, prev.h);
        }
    }

    public static void pushClipRect(float x, float y, float w, float h) {
        Minecraft mc = Minecraft.getInstance();
        float scale = (float) mc.getWindow().getGuiScale();

        int scissorX = (int) (x * scale);
        int scissorY = (int) ((mc.getWindow().getGuiScaledHeight() - y - h) * scale);
        int scissorW = (int) (w * scale);
        int scissorH = (int) (h * scale);

        ClipRect incoming = new ClipRect(scissorX, scissorY, scissorW, scissorH);
        ClipRect applied = clipStack.isEmpty() ? incoming : ClipRect.intersect(clipStack.peek(), incoming);
        clipStack.push(applied);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(applied.x, applied.y, applied.w, applied.h);
    }

    public static void fillRoundRect(float x, float y, float w, float h, float radius, int color) {
        RoundRectShader.drawRoundRect(x, y, w, h, radius, color);
    }

    public static void fillRoundRect(GuiGraphics ctx, float x, float y, float w, float h, float radius, int color) {
        RoundRectShader.drawRoundRect(ctx, x, y, w, h, radius, color);
    }

    public static void fillRoundRect(float x, float y, float w, float h,
                                     float tl, float tr, float br, float bl, int color) {
        RoundRectShader.drawRoundRect(x, y, w, h, tl, tr, br, bl, color);
    }

    public static void drawRoundRectOutline(float x, float y, float w, float h,
                                            float radius, float thickness, int color) {
        RoundRectShader.drawRoundRectOutline(x, y, w, h, radius, thickness, color);
    }

    public static void drawShadow(float x, float y, float w, float h,
                                  float radius, int layers, int shadowColor) {
        RoundRectShader.drawShadow(x, y, w, h, radius, layers, shadowColor);
    }

    public static void fillRoundRectGradient(float x, float y, float w, float h,
                                             float radius, int topColor, int bottomColor) {
        RoundRectShader.drawRoundRectGradient(x, y, w, h, radius, topColor, bottomColor);
    }

    public static void fillRoundRectGradient(GuiGraphics ctx, float x, float y, float w, float h,
                                             float radius, int topColor, int bottomColor) {
        RoundRectShader.drawRoundRectGradient(ctx, x, y, w, h, radius, topColor, bottomColor);
    }

    public static void fillRoundRectGradient(float x, float y, float w, float h,
                                             float tl, float tr, float br, float bl,
                                             int topColor, int bottomColor) {
        RoundRectShader.drawRoundRectGradient(x, y, w, h, tl, tr, br, bl, topColor, bottomColor);
    }

    public static void horizontalGradient(GuiGraphics ctx, float x, float y, float w, float h,
                                          int leftColor, int rightColor) {
        RoundRectShader.drawHorizontalGradient(ctx, x, y, w, h, 0, leftColor, rightColor);
    }

    public static void horizontalGradient(float x, float y, float w, float h,
                                          int leftColor, int rightColor) {
        RoundRectShader.drawHorizontalGradient(x, y, w, h, 0, leftColor, rightColor);
    }

    public static void horizontalGradient(float x, float y, float w, float h,
                                          float radius, int leftColor, int rightColor) {
        RoundRectShader.drawHorizontalGradient(x, y, w, h, radius, leftColor, rightColor);
    }

    public static void horizontalGradient(float x, float y, float w, float h,
                                          float tl, float tr, float br, float bl,
                                          int leftColor, int rightColor) {
        RoundRectShader.drawHorizontalGradient(x, y, w, h, tl, tr, br, bl, leftColor, rightColor);
    }

    public static void verticalGradient(float x, float y, float w, float h,
                                        int topColor, int bottomColor) {
        RoundRectShader.drawRoundRectGradient(x, y, w, h, 0, topColor, bottomColor);
    }

    public static void verticalGradient(float x, float y, float w, float h,
                                        float radius, int topColor, int bottomColor) {
        RoundRectShader.drawRoundRectGradient(x, y, w, h, radius, topColor, bottomColor);
    }

    public static void gradient(float x, float y, float w, float h,
                                int c00, int c10, int c11, int c01) {
        RoundRectShader.drawGradient4(x, y, w, h, 0, c00, c10, c11, c01);
    }

    public static void gradient(float x, float y, float w, float h,
                                float radius,
                                int c00, int c10, int c11, int c01) {
        RoundRectShader.drawGradient4(x, y, w, h, radius, c00, c10, c11, c01);
    }

    public static void gradient(float x, float y, float w, float h,
                                float tl, float tr, float br, float bl,
                                int c00, int c10, int c11, int c01) {
        RoundRectShader.drawGradient4(x, y, w, h, tl, tr, br, bl, c00, c10, c11, c01);
    }

    public static void drawCircle(float cx, float cy, float startDeg, float endDeg,
                                  float radius, float thickness, int color) {
        if (radius <= 0.0f || thickness <= 0.0f) return;

        float diff = endDeg - startDeg;
        if (Math.abs(diff) < 0.05f) return;

        // Длина дуги в пикселях
        float arcLength = (float) (2.0 * Math.PI * radius * (Math.abs(diff) / 360.0f));

        // Чем меньше шаг, тем глаже круг
        float stepPx = Math.max(0.18f, thickness * 0.12f);

        // Для маленьких радиусов принудительно много сегментов
        int steps = Math.max(80, (int) Math.ceil(arcLength / stepPx));

        // Чуть больше толщины, чтобы сегменты красиво перекрывались
        float dotSize = thickness + 0.9f;
        float dotRadius = dotSize * 0.5f;

        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float angle = startDeg + diff * t - 90.0f;

            double rad = Math.toRadians(angle);
            float x = cx + (float) Math.cos(rad) * radius;
            float y = cy + (float) Math.sin(rad) * radius;

            fillRoundRect(
                    x - dotRadius,
                    y - dotRadius,
                    dotSize,
                    dotSize,
                    dotRadius,
                    color
            );
        }
    }

    private static float[] identity() {
        return new float[]{
                1, 0, 0,
                0, 1, 0
        };
    }

    private static float[] multiply(float[] a, float[] b) {
        return new float[]{
                a[0] * b[0] + a[1] * b[3],
                a[0] * b[1] + a[1] * b[4],
                a[0] * b[2] + a[1] * b[5] + a[2],

                a[3] * b[0] + a[4] * b[3],
                a[3] * b[1] + a[4] * b[4],
                a[3] * b[2] + a[4] * b[5] + a[5]
        };
    }
}