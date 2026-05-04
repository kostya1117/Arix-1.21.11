package ru.arixcompany.utils.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class CustomFont implements AutoCloseable {

    private static final int ATLAS_SIZE = 2048;

    private static final int OVERSAMPLE = 2;
    private static final float INV_OVERSAMPLE = 1.0f / OVERSAMPLE;
    private static final float INV_ATLAS = 1.0f / ATLAS_SIZE;
    private static final int GLYPH_PADDING = 2;

    private static final int SCAN_START = 0x0020;
    private static final int SCAN_END = 0xFFFF;

    private static final int[] MC_COLORS = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
            0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
            0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
    };

    private static int getColorForCode(char code) {
        if (code >= '0' && code <= '9') return MC_COLORS[code - '0'];
        if (code >= 'a' && code <= 'f') return MC_COLORS[code - 'a' + 10];
        return -1;
    }

    private final STBTTFontinfo fontInfo;
    private final ByteBuffer ttfBuffer;

    @Getter
    private final float fontSize;

    private final float stbScale;

    private final float scaledAscent;
    private final float scaledDescent;
    private final float scaledLineGap;

    @Getter
    private final float lineHeight;

    // Список только реально запечённых символов (для obfuscated)
    private final List<Integer> availableCodepoints = new ArrayList<>();
    // Мапа только реально запечённых символов -> индекс в bakedChars
    private final Map<Integer, Integer> codepointToIndex = new HashMap<>();

    // bakedChars теперь храним буфером, но заполняем только bakedCount
    private final STBTTBakedChar.Buffer bakedChars;

    private DynamicTexture atlasTexture;
    private Identifier atlasLocation;

    // Ширины храним для всех найденных глифов (даже если не влезли в атлас)
    private final Map<Integer, Float> charWidths = new HashMap<>();

    public CustomFont(String resourcePath, float fontSize) throws IOException {
        this.fontSize = fontSize;

        this.ttfBuffer = loadResource(resourcePath);

        this.fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, ttfBuffer)) {
            MemoryUtil.memFree(ttfBuffer);
            throw new IOException("Failed to init font: " + resourcePath);
        }

        this.stbScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, fontSize);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pAsc = stack.mallocInt(1);
            IntBuffer pDesc = stack.mallocInt(1);
            IntBuffer pGap = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, pAsc, pDesc, pGap);

            this.scaledAscent = pAsc.get(0) * stbScale;
            this.scaledDescent = pDesc.get(0) * stbScale;
            this.scaledLineGap = pGap.get(0) * stbScale;
        }

        this.lineHeight = scaledAscent - scaledDescent + scaledLineGap;

        // --- Сканируем все кодпоинты, но НЕ строим codepointToIndex заранее ---
        List<Integer> scanned = new ArrayList<>();
        for (int codepoint = SCAN_START; codepoint <= SCAN_END; codepoint++) {
            int glyphIndex = STBTruetype.stbtt_FindGlyphIndex(fontInfo, codepoint);
            if (glyphIndex != 0) {
                scanned.add(codepoint);

                // Сразу считаем advance (в экранных пикселях, БЕЗ oversample)
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer advanceWidth = stack.mallocInt(1);
                    IntBuffer lsb = stack.mallocInt(1);
                    STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codepoint, advanceWidth, lsb);
                    float advPx = advanceWidth.get(0) * stbScale;
                    charWidths.put(codepoint, advPx);
                }
            }
        }

        if (scanned.isEmpty()) {
            MemoryUtil.memFree(ttfBuffer);
            throw new IOException("No renderable glyphs found in font: " + resourcePath);
        }

        // Буфер bakedChars под максимум, но реально заполним только bakedCount
        this.bakedChars = STBTTBakedChar.malloc(scanned.size());

        // FIX #1: обязательно calloc (или memSet 0), иначе в атласе мусор
        ByteBuffer bitmap = MemoryUtil.memCalloc(ATLAS_SIZE * ATLAS_SIZE);

        try {
            int atlasX = 0;
            int atlasY = 0;
            int rowHeight = 0;

            int bakedCount = 0;

            for (int codepoint : scanned) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer advanceWidth = stack.mallocInt(1);
                    IntBuffer leftSideBearing = stack.mallocInt(1);
                    STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codepoint, advanceWidth, leftSideBearing);

                    IntBuffer x0 = stack.mallocInt(1);
                    IntBuffer y0 = stack.mallocInt(1);
                    IntBuffer x1 = stack.mallocInt(1);
                    IntBuffer y1 = stack.mallocInt(1);
                    STBTruetype.stbtt_GetCodepointBitmapBox(
                            fontInfo,
                            codepoint,
                            stbScale * OVERSAMPLE,
                            stbScale * OVERSAMPLE,
                            x0, y0, x1, y1
                    );

                    int glyphW = x1.get(0) - x0.get(0);
                    int glyphH = y1.get(0) - y0.get(0);

                    // Пробел/невидимые: bitmap может быть 0x0, но advance нужен
                    boolean hasBitmap = glyphW > 0 && glyphH > 0;

                    int placeX = 0;
                    int placeY = 0;

                    if (hasBitmap) {
                        // FIX #2: учитываем padding между глифами
                        if (atlasX + glyphW + GLYPH_PADDING >= ATLAS_SIZE && atlasX > 0) {
                            atlasX = 0;
                            atlasY += rowHeight;
                            rowHeight = 0;
                        }

                        if (atlasY + glyphH + GLYPH_PADDING >= ATLAS_SIZE) {
                            // Атлас переполнен: просто не печём этот глиф (но advance уже в charWidths есть)
                            // ВАЖНО: НЕ добавляем его в codepointToIndex, чтобы не рисовать мусор
                            continue;
                        }

                        placeX = atlasX;
                        placeY = atlasY;

                        IntBuffer pWidth = stack.mallocInt(1);
                        IntBuffer pHeight = stack.mallocInt(1);
                        IntBuffer pXOff = stack.mallocInt(1);
                        IntBuffer pYOff = stack.mallocInt(1);

                        ByteBuffer glyphBitmap = STBTruetype.stbtt_GetCodepointBitmap(
                                fontInfo,
                                stbScale * OVERSAMPLE,
                                stbScale * OVERSAMPLE,
                                codepoint,
                                pWidth, pHeight, pXOff, pYOff
                        );

                        if (glyphBitmap != null) {
                            int actualWidth = pWidth.get(0);
                            int actualHeight = pHeight.get(0);

                            // Копируем в atlas с защитой от выхода
                            for (int gy = 0; gy < actualHeight; gy++) {
                                int dstRowOffset = (placeY + gy) * ATLAS_SIZE;
                                int srcRowOffset = gy * actualWidth;
                                for (int gx = 0; gx < actualWidth; gx++) {
                                    int dstIdx = dstRowOffset + placeX + gx;
                                    bitmap.put(dstIdx, glyphBitmap.get(srcRowOffset + gx));
                                }
                            }
                            STBTruetype.stbtt_FreeBitmap(glyphBitmap);
                        }

                        atlasX += glyphW + GLYPH_PADDING;
                        rowHeight = Math.max(rowHeight, glyphH + GLYPH_PADDING);
                    }

                    // Записываем bakedChar в позицию bakedCount
                    STBTTBakedChar bc = bakedChars.get(bakedCount);
                    long bcAddress = bc.address();

                    if (hasBitmap) {
                        MemoryUtil.memPutShort(bcAddress + STBTTBakedChar.X0, (short) placeX);
                        MemoryUtil.memPutShort(bcAddress + STBTTBakedChar.Y0, (short) placeY);
                        MemoryUtil.memPutShort(bcAddress + STBTTBakedChar.X1, (short) (placeX + glyphW));
                        MemoryUtil.memPutShort(bcAddress + STBTTBakedChar.Y1, (short) (placeY + glyphH));
                    } else {
                        // невидимый глиф (пробел и т.п.)
                        MemoryUtil.memPutShort(bcAddress + STBTTBakedChar.X0, (short) 0);
                        MemoryUtil.memPutShort(bcAddress + STBTTBakedChar.Y0, (short) 0);
                        MemoryUtil.memPutShort(bcAddress + STBTTBakedChar.X1, (short) 0);
                        MemoryUtil.memPutShort(bcAddress + STBTTBakedChar.Y1, (short) 0);
                    }

                    MemoryUtil.memPutFloat(bcAddress + STBTTBakedChar.XOFF, (float) x0.get(0));
                    MemoryUtil.memPutFloat(bcAddress + STBTTBakedChar.YOFF, (float) y0.get(0));
                    MemoryUtil.memPutFloat(
                            bcAddress + STBTTBakedChar.XADVANCE,
                            (float) advanceWidth.get(0) * stbScale * OVERSAMPLE
                    );

                    // FIX #3: маппим только реально запечённые (или хотя бы корректно описанные) bakedCount
                    codepointToIndex.put(codepoint, bakedCount);
                    availableCodepoints.add(codepoint);

                    bakedCount++;
                }
            }

            // --- NativeImage ---
            NativeImage image = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);

            for (int py = 0; py < ATLAS_SIZE; py++) {
                int rowOffset = py * ATLAS_SIZE;
                for (int px = 0; px < ATLAS_SIZE; px++) {
                    int alpha = bitmap.get(rowOffset + px) & 0xFF;
                    int argb = (alpha << 24) | 0x00FFFFFF;
                    image.setPixelABGR(px, py, argb);
                }
            }

            String textureName = "customfont_" + resourcePath.hashCode() + "_" + (int) fontSize;
            this.atlasTexture = new DynamicTexture(() -> textureName, image);
            this.atlasTexture.setSampler(
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
            );

            this.atlasLocation = Identifier.withDefaultNamespace("dynamic/" + textureName);
            Minecraft.getInstance().getTextureManager().register(atlasLocation, atlasTexture);

        } finally {
            MemoryUtil.memFree(bitmap);
        }
    }

    // ------------------- Render API -------------------

    public void drawString(GuiGraphics g, String text, float x, float y, int color) {
        drawString(g, text, x, y, color, false);
    }

    public void drawString(GuiGraphics g, String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty()) return;
        if (shadow) renderText(g, text, x + 1.0f, y + 1.0f, darken(color), true);
        renderText(g, text, x, y, color, false);
    }

    public void drawCenteredString(GuiGraphics g, String text, float x, float y, int color) {
        drawCenteredString(g, text, x, y, color, false);
    }

    public void drawCenteredString(GuiGraphics g, String text, float x, float y, int color, boolean shadow) {
        float w = getWidth(text);
        drawString(g, text, x - w * 0.5f, y, color, shadow);
    }

    public void drawRightString(GuiGraphics g, String text, float x, float y, int color) {
        drawRightString(g, text, x, y, color, false);
    }

    public void drawRightString(GuiGraphics g, String text, float x, float y, int color, boolean shadow) {
        float w = getWidth(text);
        drawString(g, text, x - w, y, color, shadow);
    }

    // ------------------- Core render -------------------

    private void renderText(GuiGraphics g, String text, float x, float y, int originalColor, boolean isShadow) {
        float baselineY = y + scaledAscent;
        float startX = x;

        int currentColor = originalColor;

        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean obfuscated = false;

        float lineStartX = startX;
        float cursorX = startX;

        int prevCpForKerning = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                i++;

                int mappedColor = getColorForCode(code);
                if (mappedColor != -1) {
                    int alpha = ARGB.alpha(originalColor);
                    currentColor = ARGB.color(alpha,
                            ARGB.red(mappedColor),
                            ARGB.green(mappedColor),
                            ARGB.blue(mappedColor));
                    if (isShadow) currentColor = darken(currentColor);

                    bold = italic = underline = strikethrough = obfuscated = false;
                } else {
                    switch (code) {
                        case 'l' -> bold = true;
                        case 'o' -> italic = true;
                        case 'n' -> underline = true;
                        case 'm' -> strikethrough = true;
                        case 'k' -> obfuscated = true;
                        case 'r' -> {
                            currentColor = isShadow ? darken(originalColor) : originalColor;
                            bold = italic = underline = strikethrough = obfuscated = false;
                        }
                    }
                }

                prevCpForKerning = -1;
                continue;
            }

            if (c == '\n') {
                if (underline) drawLineDecoration(g, lineStartX, cursorX, baselineY, 1.0f, currentColor);
                if (strikethrough) drawLineDecoration(g, lineStartX, cursorX, baselineY, -scaledAscent * 0.35f, currentColor);

                cursorX = startX;
                baselineY += lineHeight;
                lineStartX = startX;
                prevCpForKerning = -1;
                continue;
            }

            char renderChar = c;
            if (obfuscated && c != ' ') {
                renderChar = getRandomCharSimilarWidth(c);
            }

            int cp = (int) renderChar;

            // (опционально) kerning — делает текст заметно “правильнее” по spacing
            if (prevCpForKerning != -1) {
                cursorX += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prevCpForKerning, cp) * stbScale;
            }

            Integer charIndex = codepointToIndex.get(cp);
            if (charIndex == null) {
                // нет в атласе -> просто продвигаем курсор по метрикам
                float adv = getCharWidth(renderChar);
                if (bold) adv += 1.0f;
                cursorX += adv;
                prevCpForKerning = cp;
                continue;
            }

            STBTTBakedChar charData = bakedChars.get(charIndex);

            int srcX = charData.x0();
            int srcY = charData.y0();
            int srcW = charData.x1() - charData.x0();
            int srcH = charData.y1() - charData.y0();

            float glyphX = cursorX + charData.xoff() * INV_OVERSAMPLE;
            float glyphY = baselineY + charData.yoff() * INV_OVERSAMPLE;

            if (srcW > 0 && srcH > 0) {
                drawGlyph(g, glyphX, glyphY, srcX, srcY, srcW, srcH, italic, currentColor);

                if (bold) {
                    drawGlyph(g, glyphX + 1.0f, glyphY, srcX, srcY, srcW, srcH, italic, currentColor);
                }
            }

            float advance = charData.xadvance() * INV_OVERSAMPLE;
            if (bold) advance += 1.0f;
            cursorX += advance;

            prevCpForKerning = cp;
        }

        if (underline) drawLineDecoration(g, lineStartX, cursorX, baselineY, 1.0f, currentColor);
        if (strikethrough) drawLineDecoration(g, lineStartX, cursorX, baselineY, -scaledAscent * 0.35f, currentColor);
    }

    private void drawGlyph(GuiGraphics g,
                           float x, float y,
                           int srcX, int srcY, int srcW, int srcH,
                           boolean italic,
                           int color) {
        if (srcW <= 0 || srcH <= 0) return;

        g.pose().pushMatrix();

        // translateLocal = “нормальный” сдвиг в экранных координатах
        g.pose().translateLocal(x, y);

        // Рисуем oversampled-битмап и масштабируем до нормального размера 1/OVERSAMPLE
        g.pose().scale(INV_OVERSAMPLE, INV_OVERSAMPLE);

        // Простейшая “italic” через shear (если тебе не надо — можно выкинуть блок)
        if (italic) {
            float shear = 0.25f;
            float dstH = srcH * INV_OVERSAMPLE;
            // сдвиг, чтобы верх был правее низа
            g.pose().translateLocal(shear * dstH, 0.0f);
            g.pose().shearX(-shear);
        }

        g.blit(
                RenderPipelines.GUI_TEXTURED,
                atlasLocation,
                0, 0,
                (float) srcX, (float) srcY,
                srcW, srcH,
                srcW, srcH,
                ATLAS_SIZE, ATLAS_SIZE,
                color
        );

        g.pose().popMatrix();
    }

    private void drawLineDecoration(GuiGraphics g, float fromX, float toX, float baselineY, float yOffset, int color) {
        int y = Math.round(baselineY + yOffset);
        int x1 = Math.round(fromX);
        int x2 = Math.round(toX);
        if (x2 > x1) g.fill(x1, y, x2, y + 1, color);
    }

    // ------------------- Metrics -------------------

    public float getWidth(String text) {
        if (text == null || text.isEmpty()) return 0;

        float width = 0;
        float maxWidth = 0;
        boolean bold = false;

        int prevCpForKerning = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                i++;
                if (code == 'l') bold = true;
                else if (code == 'r' || getColorForCode(code) != -1) bold = false;

                prevCpForKerning = -1;
                continue;
            }

            if (c == '\n') {
                maxWidth = Math.max(maxWidth, width);
                width = 0;
                prevCpForKerning = -1;
                continue;
            }

            int cp = (int) c;
            if (prevCpForKerning != -1) {
                width += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prevCpForKerning, cp) * stbScale;
            }

            float cw = getCharWidth(c);
            if (bold) cw += 1.0f;
            width += cw;

            prevCpForKerning = cp;
        }

        return Math.max(maxWidth, width);
    }

    public float getWidth(char c) {
        return getCharWidth(c);
    }

    private float getCharWidth(char c) {
        Float w = charWidths.get((int) c);
        if (w != null) return w;
        return fontSize * 0.3f;
    }

    public float getHeight(String text) {
        if (text == null || text.isEmpty()) return lineHeight;
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) { i++; continue; }
            if (c == '\n') lines++;
        }
        return lines * lineHeight;
    }

    public float getHeight() {
        return lineHeight;
    }

    public float getAscent() {
        return scaledAscent;
    }

    public float getDescent() {
        return -scaledDescent;
    }

    // ------------------- Utils -------------------

    public String trimToWidth(String text, float maxWidth) {
        if (text == null) return "";

        StringBuilder sb = new StringBuilder();
        float w = 0;
        boolean bold = false;

        int prevCpForKerning = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                sb.append(c).append(text.charAt(i + 1));

                if (code == 'l') bold = true;
                else if (code == 'r' || getColorForCode(code) != -1) bold = false;

                i++;
                prevCpForKerning = -1;
                continue;
            }

            int cp = (int) c;
            float kern = 0;
            if (prevCpForKerning != -1) {
                kern = STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prevCpForKerning, cp) * stbScale;
            }

            float cw = getCharWidth(c) + kern;
            if (bold) cw += 1.0f;

            if (w + cw > maxWidth) break;

            sb.append(c);
            w += cw;

            prevCpForKerning = cp;
        }

        return sb.toString();
    }

    public static String stripColorCodes(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) { i++; continue; }
            sb.append(c);
        }
        return sb.toString();
    }

    private static int darken(int color) {
        int a = ARGB.alpha(color);
        int r = (int) (ARGB.red(color) * 0.25f);
        int g = (int) (ARGB.green(color) * 0.25f);
        int b = (int) (ARGB.blue(color) * 0.25f);
        return ARGB.color(a, r, g, b);
    }

    private char getRandomCharSimilarWidth(char original) {
        float targetWidth = getCharWidth(original);

        for (int attempt = 0; attempt < 5; attempt++) {
            if (availableCodepoints.isEmpty()) break;
            int randomIndex = (int) (Math.random() * availableCodepoints.size());
            char candidate = (char) (int) availableCodepoints.get(randomIndex);
            float candidateWidth = getCharWidth(candidate);
            if (Math.abs(candidateWidth - targetWidth) < targetWidth * 0.3f) {
                return candidate;
            }
        }

        if (!availableCodepoints.isEmpty()) {
            return (char) (int) availableCodepoints.get((int) (Math.random() * availableCodepoints.size()));
        }
        return original;
    }

    private static ByteBuffer loadResource(String path) throws IOException {
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;

        InputStream is = CustomFont.class.getClassLoader().getResourceAsStream(cleanPath);
        if (is == null) is = CustomFont.class.getResourceAsStream(path);
        if (is == null) throw new IOException("Font resource not found: " + path);

        try {
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } finally {
            is.close();
        }
    }

    @Override
    public void close() {
        if (bakedChars != null) bakedChars.free();

        if (atlasLocation != null) {
            try {
                Minecraft.getInstance().getTextureManager().release(atlasLocation);
            } catch (Exception ignored) {}
        }

        if (atlasTexture != null) {
            try {
                atlasTexture.close();
            } catch (Exception ignored) {}
            atlasTexture = null;
        }
        atlasLocation = null;

        if (ttfBuffer != null) MemoryUtil.memFree(ttfBuffer);
    }
}