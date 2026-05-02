package ru.arixcompany.utils.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class CustomFont implements AutoCloseable {

    // ============================================================
    // Константы
    // ============================================================

    private static final int ATLAS_SIZE = 2048;
    private static final int FIRST_CHAR = 32;
    private static final int CHAR_COUNT = 224; // ASCII 32..255
    private static final int OVERSAMPLE = 2;
    private static final float INV_OVERSAMPLE = 1.0f / OVERSAMPLE;
    private static final float INV_ATLAS = 1.0f / ATLAS_SIZE;

    // ============================================================
    // Minecraft color codes (§0–§f)
    // ============================================================

    private static final int[] MC_COLORS = {
            0xFF000000, // 0 — black
            0xFF0000AA, // 1 — dark blue
            0xFF00AA00, // 2 — dark green
            0xFF00AAAA, // 3 — dark aqua
            0xFFAA0000, // 4 — dark red
            0xFFAA00AA, // 5 — dark purple
            0xFFFFAA00, // 6 — gold
            0xFFAAAAAA, // 7 — gray
            0xFF555555, // 8 — dark gray
            0xFF5555FF, // 9 — blue
            0xFF55FF55, // a — green
            0xFF55FFFF, // b — aqua
            0xFFFF5555, // c — red
            0xFFFF55FF, // d — light purple
            0xFFFFFF55, // e — yellow
            0xFFFFFFFF  // f — white
    };

    private static int getColorForCode(char code) {
        if (code >= '0' && code <= '9') return MC_COLORS[code - '0'];
        if (code >= 'a' && code <= 'f') return MC_COLORS[code - 'a' + 10];
        return -1; // не цветовой код
    }

    // ============================================================
    // Поля
    // ============================================================

    private final STBTTFontinfo fontInfo;
    private final ByteBuffer ttfBuffer;

    @Getter
    private final float fontSize;

    /** Масштаб STB для данного fontSize */
    private final float stbScale;

    /** Масштабированные метрики шрифта (в пикселях экрана) */
    private final float scaledAscent;
    private final float scaledDescent;
    private final float scaledLineGap;

    /** Высота строки = ascent - descent + lineGap */
    @Getter
    private final float lineHeight;

    private final STBTTBakedChar.Buffer bakedChars;
    private DynamicTexture atlasTexture;
    private Identifier atlasLocation;

    /** Кэш ширин символов (уже делённых на OVERSAMPLE) */
    private final float[] charWidths = new float[CHAR_COUNT];

    // ============================================================
    // Конструктор
    // ============================================================

    public CustomFont(String resourcePath, float fontSize) throws IOException {
        this.fontSize = fontSize;

        // --- Загрузка TTF ---
        this.ttfBuffer = loadResource(resourcePath);

        this.fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, ttfBuffer)) {
            MemoryUtil.memFree(ttfBuffer);
            throw new IOException("Failed to init font: " + resourcePath);
        }

        // --- Метрики шрифта ---
        // Мы bake'им с размером fontSize * OVERSAMPLE, потом делим координаты на OVERSAMPLE.
        // Но метрики шрифта берём для реального fontSize.
        this.stbScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, fontSize);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pAsc = stack.mallocInt(1);
            IntBuffer pDesc = stack.mallocInt(1);
            IntBuffer pGap = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, pAsc, pDesc, pGap);

            // ascent > 0, descent < 0 (обычно)
            this.scaledAscent = pAsc.get(0) * stbScale;
            this.scaledDescent = pDesc.get(0) * stbScale;   // отрицательное
            this.scaledLineGap = pGap.get(0) * stbScale;
        }

        this.lineHeight = scaledAscent - scaledDescent + scaledLineGap;

        // --- Bake bitmap ---
        this.bakedChars = STBTTBakedChar.malloc(CHAR_COUNT);
        ByteBuffer bitmap = MemoryUtil.memAlloc(ATLAS_SIZE * ATLAS_SIZE);
        try {
            float bakePixelHeight = fontSize * OVERSAMPLE;
            int result = STBTruetype.stbtt_BakeFontBitmap(
                    ttfBuffer, bakePixelHeight,
                    bitmap, ATLAS_SIZE, ATLAS_SIZE,
                    FIRST_CHAR, bakedChars
            );
            if (result <= 0) {
                System.err.println("[CustomFont] Warning: stbtt_BakeFontBitmap returned " + result
                        + " — atlas may be too small for font size " + fontSize);
            }

            // --- Кэш ширин ---
            for (int i = 0; i < CHAR_COUNT; i++) {
                charWidths[i] = bakedChars.get(i).xadvance() * INV_OVERSAMPLE;
            }

            // --- Создание NativeImage → DynamicTexture ---
            NativeImage image = new NativeImage(
                    NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false
            );
            for (int py = 0; py < ATLAS_SIZE; py++) {
                int rowOffset = py * ATLAS_SIZE;
                for (int px = 0; px < ATLAS_SIZE; px++) {
                    int alpha = bitmap.get(rowOffset + px) & 0xFF;
                    // NativeImage.setPixelABGR ожидает ABGR: alpha в старших битах
                    image.setPixelABGR(px, py, (alpha << 24) | 0x00FFFFFF);
                }
            }

            String textureName = "customfont_" + resourcePath.hashCode() + "_" + (int) fontSize;
            this.atlasTexture = new DynamicTexture(() -> textureName, image);
            this.atlasTexture.setSampler(
                    RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR)
            );

            this.atlasLocation = Identifier.withDefaultNamespace("dynamic/" + textureName);
            Minecraft.getInstance().getTextureManager().register(atlasLocation, atlasTexture);

        } finally {
            MemoryUtil.memFree(bitmap);
        }
    }

    // ============================================================
    // Публичные методы рендера
    // ============================================================

    public void drawString(GuiGraphics g, String text, float x, float y, int color) {
        drawString(g, text, x, y, color, false);
    }

    public void drawString(GuiGraphics g, String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;

        if (shadow) {
            renderText(g, text, x + 1.0f, y + 1.0f, darken(color), true);
        }
        renderText(g, text, x, y, color, false);
    }

    public void drawCenteredString(GuiGraphics g, String text, float x, float y, int color) {
        drawCenteredString(g, text, x, y, color, false);
    }

    public void drawCenteredString(GuiGraphics g, String text, float x, float y,
                                   int color, boolean shadow) {
        float w = getWidth(text);
        drawString(g, text, x - w * 0.5f, y, color, shadow);
    }

    public void drawRightString(GuiGraphics g, String text, float x, float y, int color) {
        drawRightString(g, text, x, y, color, false);
    }

    public void drawRightString(GuiGraphics g, String text, float x, float y,
                                int color, boolean shadow) {
        float w = getWidth(text);
        drawString(g, text, x - w, y, color, shadow);
    }

    // ============================================================
    // Внутренний рендер (ядро)
    // ============================================================

    /**
     * Главная функция рендера. Использует STBTTAlignedQuad для получения
     * правильных экранных координат каждого глифа. Это гарантирует
     * единообразное вертикальное выравнивание всех символов.
     */
    private void renderText(GuiGraphics g, String text, float x, float y,
                            int originalColor, boolean isShadow) {
        if (text == null || text.isEmpty()) return;

        // baseline Y — отступаем ascent от верхнего края строки
        // STB quad'ы считают Y от baseline (yoff отрицателен для символов выше baseline)
        float baselineY = y + scaledAscent;
        float startX = x;

        int currentColor = originalColor;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        float lineStartX = startX;

        // Для stbtt_GetBakedQuad нам нужен float[] xpos
        // Но мы вручную считаем позиции, чтобы не терять float-точность

        float cursorX = startX;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // --- Обработка § кодов ---
            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                i++;

                int mappedColor = getColorForCode(code);
                if (mappedColor != -1) {
                    // Цветовой код: применяем цвет, сбрасываем форматирование
                    int alpha = ARGB.alpha(originalColor);
                    currentColor = ARGB.color(alpha,
                            ARGB.red(mappedColor),
                            ARGB.green(mappedColor),
                            ARGB.blue(mappedColor));
                    if (isShadow) currentColor = darken(currentColor);

                    bold = false;
                    italic = false;
                    underline = false;
                    strikethrough = false;
                    obfuscated = false;
                } else {
                    switch (code) {
                        case 'l' -> bold = true;
                        case 'o' -> italic = true;
                        case 'n' -> underline = true;
                        case 'm' -> strikethrough = true;
                        case 'k' -> obfuscated = true;
                        case 'r' -> {
                            currentColor = isShadow ? darken(originalColor) : originalColor;
                            bold = false;
                            italic = false;
                            underline = false;
                            strikethrough = false;
                            obfuscated = false;
                        }
                    }
                }
                continue;
            }

            // --- Перенос строки ---
            if (c == '\n') {
                if (underline) drawLineDecoration(g, lineStartX, cursorX, baselineY, 1.0f, currentColor);
                if (strikethrough) drawLineDecoration(g, lineStartX, cursorX, baselineY, -scaledAscent * 0.35f, currentColor);

                cursorX = startX;
                baselineY += lineHeight;
                lineStartX = startX;
                continue;
            }

            // --- Obfuscated ---
            char renderChar = c;
            if (obfuscated && c != ' ') {
                // Подбираем случайный символ похожей ширины
                renderChar = getRandomCharSimilarWidth(c);
            }

            // --- Проверка диапазона ---
            int charIndex = renderChar - FIRST_CHAR;
            if (charIndex < 0 || charIndex >= CHAR_COUNT) {
                // Символ вне baked диапазона — пропускаем с пробелом
                cursorX += getCharWidth(' ');
                continue;
            }

            // --- Получаем данные глифа ---
            STBTTBakedChar charData = bakedChars.get(charIndex);

            // UV координаты в атласе
            float u0 = charData.x0() * INV_ATLAS;
            float v0 = charData.y0() * INV_ATLAS;
            float u1 = charData.x1() * INV_ATLAS;
            float v1 = charData.y1() * INV_ATLAS;

            // Размер глифа на экране
            float glyphW = (charData.x1() - charData.x0()) * INV_OVERSAMPLE;
            float glyphH = (charData.y1() - charData.y0()) * INV_OVERSAMPLE;

            // Позиция глифа на экране (xoff/yoff уже в пикселях baked размера,
            // нужно поделить на OVERSAMPLE)
            float glyphX = cursorX + charData.xoff() * INV_OVERSAMPLE;
            float glyphY = baselineY + charData.yoff() * INV_OVERSAMPLE;

            // Italic: скос верхней части
            float italicShearTop = italic ? (glyphH * 0.2f) : 0;
            float italicShearBottom = 0;

            if (glyphW > 0 && glyphH > 0) {
                drawGlyph(g, glyphX, glyphY, glyphW, glyphH,
                        u0, v0, u1, v1,
                        italicShearTop, italicShearBottom,
                        currentColor);

                if (bold) {
                    drawGlyph(g, glyphX + 1.0f, glyphY, glyphW, glyphH,
                            u0, v0, u1, v1,
                            italicShearTop, italicShearBottom,
                            currentColor);
                }
            }

            float advance = charData.xadvance() * INV_OVERSAMPLE;
            if (bold) advance += 1.0f;
            cursorX += advance;
        }

        // Декорации для последней строки
        if (underline) {
            drawLineDecoration(g, lineStartX, cursorX, baselineY, 1.0f, currentColor);
        }
        if (strikethrough) {
            drawLineDecoration(g, lineStartX, cursorX, baselineY, -scaledAscent * 0.35f, currentColor);
        }
    }

    /**
     * Рендерит один глиф через blit. Координаты — float, но blit принимает int.
     * Чтобы избежать "прыганья" символов, мы округляем ТОЛЬКО финальные
     * позиции, а advance остаётся float.
     */
    private void drawGlyph(GuiGraphics g,
                           float x, float y, float w, float h,
                           float u0, float v0, float u1, float v1,
                           float italicShearTop, float italicShearBottom,
                           int color) {
        // Для стандартного blit (без italic) — простой вызов
        // Используем sub-pixel позиции через blit с float UV

        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.max(1, Math.round(w));
        int ih = Math.max(1, Math.round(h));

        // UV в пикселях атласа для blit
        float srcX = u0 * ATLAS_SIZE;
        float srcY = v0 * ATLAS_SIZE;
        int srcW = Math.round((u1 - u0) * ATLAS_SIZE);
        int srcH = Math.round((v1 - v0) * ATLAS_SIZE);

        if (srcW <= 0 || srcH <= 0) return;

        g.blit(
                RenderPipelines.GUI_TEXTURED,
                atlasLocation,
                ix, iy,
                srcX, srcY,
                iw, ih,
                srcW, srcH,
                ATLAS_SIZE, ATLAS_SIZE,
                color
        );
    }

    /**
     * Рисует горизонтальную линию (underline / strikethrough).
     * @param yOffset смещение от baseline (+ вниз, - вверх)
     */
    private void drawLineDecoration(GuiGraphics g, float fromX, float toX,
                                    float baselineY, float yOffset, int color) {
        int y = Math.round(baselineY + yOffset);
        int x1 = Math.round(fromX);
        int x2 = Math.round(toX);
        if (x2 > x1) {
            g.fill(x1, y, x2, y + 1, color);
        }
    }

    // ============================================================
    // Метрики
    // ============================================================

    /**
     * Ширина текста с учётом § кодов.
     */
    public float getWidth(String text) {
        if (text == null || text.isEmpty()) return 0;

        float width = 0;
        float maxWidth = 0;
        boolean bold = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                i++;

                if (code == 'l') {
                    bold = true;
                } else if (code == 'r' || getColorForCode(code) != -1) {
                    bold = false;
                }
                continue;
            }

            if (c == '\n') {
                maxWidth = Math.max(maxWidth, width);
                width = 0;
                continue;
            }

            float cw = getCharWidth(c);
            if (bold) cw += 1.0f;
            width += cw;
        }

        return Math.max(maxWidth, width);
    }

    public float getWidth(char c) {
        return getCharWidth(c);
    }

    private float getCharWidth(char c) {
        int idx = c - FIRST_CHAR;
        if (idx < 0 || idx >= CHAR_COUNT) {
            return fontSize * 0.3f; // fallback для неизвестных
        }
        return charWidths[idx];
    }

    /**
     * Высота текста (учитывает переносы строк).
     */
    public float getHeight(String text) {
        if (text == null || text.isEmpty()) return lineHeight;

        int lines = 1;
        boolean inCode = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++; // skip code char
                continue;
            }
            if (c == '\n') lines++;
        }
        return lines * lineHeight;
    }

    /**
     * Высота одной строки.
     */
    public float getHeight() {
        return lineHeight;
    }

    /**
     * Ascent — расстояние от верха строки до baseline.
     */
    public float getAscent() {
        return scaledAscent;
    }

    /**
     * Descent — расстояние от baseline до низа строки (положительное значение).
     */
    public float getDescent() {
        return -scaledDescent; // scaledDescent отрицателен
    }

    // ============================================================
    // Утилиты
    // ============================================================

    /**
     * Обрезает текст до заданной ширины.
     */
    public String trimToWidth(String text, float maxWidth) {
        if (text == null) return "";

        StringBuilder sb = new StringBuilder();
        float w = 0;
        boolean bold = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                sb.append(c).append(text.charAt(i + 1));

                if (code == 'l') bold = true;
                else if (code == 'r' || getColorForCode(code) != -1) bold = false;
                i++;
                continue;
            }

            float cw = getCharWidth(c);
            if (bold) cw += 1.0f;
            if (w + cw > maxWidth) break;

            sb.append(c);
            w += cw;
        }
        return sb.toString();
    }

    /**
     * Убирает все § коды из текста.
     */
    public static String stripColorCodes(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Затемняет цвет для тени (25% яркости).
     */
    private static int darken(int color) {
        int a = ARGB.alpha(color);
        int r = (int) (ARGB.red(color) * 0.25f);
        int gr = (int) (ARGB.green(color) * 0.25f);
        int b = (int) (ARGB.blue(color) * 0.25f);
        return ARGB.color(a, r, gr, b);
    }

    /**
     * Подбирает случайный символ с шириной, похожей на оригинальный (для §k).
     */
    private char getRandomCharSimilarWidth(char original) {
        float targetWidth = getCharWidth(original);
        // Пробуем несколько раз найти символ с похожей шириной
        for (int attempt = 0; attempt < 5; attempt++) {
            char candidate = (char) (FIRST_CHAR + 1 + (int) (Math.random() * (CHAR_COUNT - 1)));
            float candidateWidth = getCharWidth(candidate);
            if (Math.abs(candidateWidth - targetWidth) < targetWidth * 0.3f) {
                return candidate;
            }
        }
        // Fallback: любой символ
        return (char) (FIRST_CHAR + 1 + (int) (Math.random() * (CHAR_COUNT - 1)));
    }

    // ============================================================
    // Загрузка ресурсов
    // ============================================================

    private static ByteBuffer loadResource(String path) throws IOException {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;

        InputStream is = CustomFont.class.getClassLoader().getResourceAsStream(cleanPath);
        if (is == null) {
            is = CustomFont.class.getResourceAsStream(path);
        }
        if (is == null) {
            throw new IOException("Font resource not found: " + path);
        }

        try {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } finally {
            is.close();
        }
    }

    // ============================================================
    // Очистка
    // ============================================================

    @Override
    public void close() {
        if (bakedChars != null) {
            bakedChars.free();
        }

        if (atlasTexture != null) {
            if (atlasLocation != null) {
                try {
                    Minecraft.getInstance().getTextureManager().release(atlasLocation);
                } catch (Exception ignored) {
                    // Может быть вызвано после закрытия контекста
                }
            }
            atlasTexture = null;
            atlasLocation = null;
        }

        if (ttfBuffer != null) {
            MemoryUtil.memFree(ttfBuffer);
        }

        // fontInfo создан через STBTTFontinfo.create() — это managed struct,
        // он не владеет нативной памятью, но ссылается на ttfBuffer
    }
}