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
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import net.minecraft.ChatFormatting;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class CustomFont implements AutoCloseable {

    private static final int ATLAS_SIZE = 2048;
    private static final int OVERSAMPLE = 2;
    private static final float INV_OS = 1f / OVERSAMPLE;
    private static final int PAD = 2;
    private static final int SCAN_S = 0x0020;
    private static final int SCAN_E = 0xFFFF;

    private static int getColorForCode(char code) {
        ChatFormatting fmt = ChatFormatting.getByCode(code);
        if (fmt == null) return -1;
        Integer color = fmt.getColor();
        if (color == null) return -1;
        return 0xFF000000 | color;
    }

    private final STBTTFontinfo fontInfo;
    private final ByteBuffer ttfBuffer;
    @Getter private final float fontSize;
    private final float scale;
    private final float asc, desc, gap, lineH;

    private final List<Integer> codepoints = new ArrayList<>();
    private final Map<Integer, Integer> idxMap = new HashMap<>();
    private final STBTTBakedChar.Buffer baked;
    private final Map<Integer, Float> widths = new HashMap<>();

    private DynamicTexture atlasTexture;
    private Identifier atlasId;

    public CustomFont(String path, float sz) throws IOException {
        fontSize = sz;
        ttfBuffer = load(path);
        fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, ttfBuffer)) {
            MemoryUtil.memFree(ttfBuffer);
            throw new IOException("Cannot init font: " + path);
        }
        scale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, sz);

        try (MemoryStack st = MemoryStack.stackPush()) {
            IntBuffer a = st.mallocInt(1), d = st.mallocInt(1), g = st.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, a, d, g);
            asc = a.get(0) * scale;
            desc = d.get(0) * scale;
            gap = g.get(0) * scale;
        }
        lineH = asc - desc + gap;

        for (int cp = SCAN_S; cp <= SCAN_E; cp++) {
            if (STBTruetype.stbtt_FindGlyphIndex(fontInfo, cp) != 0) {
                codepoints.add(cp);
                try (MemoryStack st = MemoryStack.stackPush()) {
                    IntBuffer adv = st.mallocInt(1), lsb = st.mallocInt(1);
                    STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, cp, adv, lsb);
                    widths.put(cp, adv.get(0) * scale);
                }
            }
        }
        if (codepoints.isEmpty()) throw new IOException("No glyphs: " + path);

        baked = STBTTBakedChar.malloc(codepoints.size());
        ByteBuffer bmp = MemoryUtil.memCalloc(ATLAS_SIZE * ATLAS_SIZE);

        int bakedCnt = 0;
        for (int cp : codepoints) packGlyph(cp, bmp, bakedCnt++);

        NativeImage img = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);
        for (int y = 0; y < ATLAS_SIZE; y++)
            for (int x = 0; x < ATLAS_SIZE; x++) {
                int alpha = bmp.get(y * ATLAS_SIZE + x) & 0xFF;
                img.setPixelABGR(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        MemoryUtil.memFree(bmp);

        String name = "cf_" + path.hashCode() + "_" + (int) sz;
        atlasTexture = new DynamicTexture(() -> name, img);
        atlasTexture.setSampler(RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        atlasId = Identifier.withDefaultNamespace("dynamic/" + name);
        Minecraft.getInstance().getTextureManager().register(atlasId, atlasTexture);
    }

    private int packX = 0, packY = 0, packRowH = 0;

    private void packGlyph(int cp, ByteBuffer bmp, int slot) {
        try (MemoryStack st = MemoryStack.stackPush()) {
            IntBuffer adv = st.mallocInt(1), lsb = st.mallocInt(1);
            STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, cp, adv, lsb);
            IntBuffer x0 = st.mallocInt(1), y0 = st.mallocInt(1),
                    x1 = st.mallocInt(1), y1 = st.mallocInt(1);
            STBTruetype.stbtt_GetCodepointBitmapBox(fontInfo, cp,
                    scale * OVERSAMPLE, scale * OVERSAMPLE, x0, y0, x1, y1);

            int gw = x1.get(0) - x0.get(0), gh = y1.get(0) - y0.get(0);
            boolean has = gw > 0 && gh > 0;
            int px = 0, py = 0;

            if (has) {
                if (packX + gw + PAD >= ATLAS_SIZE && packX > 0) {
                    packX = 0;
                    packY += packRowH;
                    packRowH = 0;
                }
                if (packY + gh + PAD >= ATLAS_SIZE) return;
                px = packX;
                py = packY;

                IntBuffer pw = st.mallocInt(1), ph = st.mallocInt(1),
                        ox = st.mallocInt(1), oy = st.mallocInt(1);
                ByteBuffer gb = STBTruetype.stbtt_GetCodepointBitmap(fontInfo,
                        scale * OVERSAMPLE, scale * OVERSAMPLE, cp, pw, ph, ox, oy);
                if (gb != null) {
                    int aw = pw.get(0), ah = ph.get(0);
                    for (int gy = 0; gy < ah; gy++)
                        for (int gx = 0; gx < aw; gx++)
                            bmp.put((py + gy) * ATLAS_SIZE + px + gx,
                                    gb.get(gy * aw + gx));
                    STBTruetype.stbtt_FreeBitmap(gb);
                }
                packX += gw + PAD;
                packRowH = Math.max(packRowH, gh + PAD);
            }

            long adr = baked.get(slot).address();
            MemoryUtil.memPutShort(adr + STBTTBakedChar.X0, (short) (has ? px : 0));
            MemoryUtil.memPutShort(adr + STBTTBakedChar.Y0, (short) (has ? py : 0));
            MemoryUtil.memPutShort(adr + STBTTBakedChar.X1, (short) (has ? px + gw : 0));
            MemoryUtil.memPutShort(adr + STBTTBakedChar.Y1, (short) (has ? py + gh : 0));
            MemoryUtil.memPutFloat(adr + STBTTBakedChar.XOFF, (float) x0.get(0));
            MemoryUtil.memPutFloat(adr + STBTTBakedChar.YOFF, (float) y0.get(0));
            MemoryUtil.memPutFloat(adr + STBTTBakedChar.XADVANCE,
                    adv.get(0) * scale * OVERSAMPLE);

            idxMap.put(cp, slot);
        }
    }

    // ==================== RENDER ====================

    public void drawString(GuiGraphics g, String t, float x, float y, int col) {
        drawString(g, t, x, y, col, false);
    }

    public void drawString(GuiGraphics g, String t, float x, float y, int col, boolean sh) {
        if (isEmpty(t)) return;
        if (sh) render(g, t, x + 1, y + 1, darken(col), true);
        render(g, t, x, y, col, false);
    }

    public void drawCenteredString(GuiGraphics g, String t, float x, float y, int col) {
        drawString(g, t, x - getWidth(t) / 2, y, col);
    }

    public void drawCenteredString(GuiGraphics g, String t, float x, float y, int col, boolean sh) {
        drawString(g, t, x - getWidth(t) / 2, y, col, sh);
    }

    public void drawRightString(GuiGraphics g, String t, float x, float y, int col) {
        drawString(g, t, x - getWidth(t), y, col);
    }

    public void drawRightString(GuiGraphics g, String t, float x, float y, int col, boolean sh) {
        drawString(g, t, x - getWidth(t), y, col, sh);
    }

    // ==================== COMPONENT RENDER (Minecraft native) ====================

    public void drawComponent(GuiGraphics g, net.minecraft.network.chat.Component comp, float x, float y, int col) {
        drawComponent(g, comp, x, y, col, false);
    }

    public void drawComponent(GuiGraphics g, net.minecraft.network.chat.Component comp, float x, float y, int col, boolean sh) {
        if (comp == null) return;
        if (sh) renderComponent(g, comp, x + 1, y + 1, darken(col), true);
        renderComponent(g, comp, x, y, col, false);
    }

    public void drawCenteredComponent(GuiGraphics g, net.minecraft.network.chat.Component comp, float x, float y, int col) {
        drawComponent(g, comp, x - getComponentWidth(comp) / 2, y, col);
    }

    public void drawCenteredComponent(GuiGraphics g, net.minecraft.network.chat.Component comp, float x, float y, int col, boolean sh) {
        drawComponent(g, comp, x - getComponentWidth(comp) / 2, y, col, sh);
    }

    public void drawRightComponent(GuiGraphics g, net.minecraft.network.chat.Component comp, float x, float y, int col) {
        drawComponent(g, comp, x - getComponentWidth(comp), y, col);
    }

    public void drawRightComponent(GuiGraphics g, net.minecraft.network.chat.Component comp, float x, float y, int col, boolean sh) {
        drawComponent(g, comp, x - getComponentWidth(comp), y, col, sh);
    }

    private void renderComponent(GuiGraphics g, net.minecraft.network.chat.Component comp, float x, float y, int baseCol, boolean shadow) {
        float cx = x, by = y + asc;
        renderComponentRec(g, comp, cx, by, baseCol, shadow, -1);
    }

    private float renderComponentRec(GuiGraphics g, net.minecraft.network.chat.Component comp, float cx, float by, int baseCol, boolean shadow, int prev) {
        net.minecraft.network.chat.Style style = comp.getStyle();

        int col = baseCol;
        if (style.getColor() != null) {
            Integer styleColor = style.getColor().getValue();
            if (styleColor != null) {
                int alpha = ARGB.alpha(baseCol);
                col = ARGB.color(alpha, ARGB.red(styleColor), ARGB.green(styleColor), ARGB.blue(styleColor));
                if (shadow) col = darken(col);
            }
        }

        boolean bold = style.isBold();
        boolean ital = style.isItalic();
        boolean und = style.isUnderlined();
        boolean strk = style.isStrikethrough();
        boolean obf = style.isObfuscated();

        StringBuilder contentBuilder = new StringBuilder();
        comp.getContents().visit((text) -> {
            contentBuilder.append(text);
            return java.util.Optional.empty();
        });
        String content = contentBuilder.toString();
        
        float lsx = cx;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);

            if (ch == '§' && i + 1 < content.length()) {
                char code = Character.toLowerCase(content.charAt(i + 1));
                i++;

                if (code == 'r') {
                    col = shadow ? darken(baseCol) : baseCol;
                    bold = ital = und = strk = obf = false;
                    prev = -1;
                    continue;
                }

                int mappedColor = getColorForCode(code);
                if (mappedColor != -1) {
                    int alpha = ARGB.alpha(baseCol);
                    col = ARGB.color(alpha,
                            ARGB.red(mappedColor),
                            ARGB.green(mappedColor),
                            ARGB.blue(mappedColor));
                    if (shadow) col = darken(col);
                } else {
                    switch (code) {
                        case 'l' -> bold = true;
                        case 'o' -> ital = true;
                        case 'n' -> und = true;
                        case 'm' -> strk = true;
                        case 'k' -> obf = true;
                    }
                }
                
                prev = -1;
                continue;
            }
            
            if (ch == '\n') {
                if (und) fill(g, lsx, cx, by + 1, col);
                if (strk) fill(g, lsx, cx, by - asc * 0.35f, col);
                cx = lsx;
                by += lineH;
                prev = -1;
                continue;
            }

            char rc = obf && ch != ' ' ? randChar(ch) : ch;
            int cp = (int) rc;

            if (prev != -1) cx += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, cp) * scale;

            Integer idx = idxMap.get(cp);
            if (idx == null) {
                float adv = fontSize * 0.5f;
                if (bold) adv += 1;
                cx += adv;
                prev = cp;
                continue;
            }

            STBTTBakedChar cd = baked.get(idx);
            int sx = cd.x0(), sy = cd.y0(), sw = cd.x1() - sx, sh = cd.y1() - sy;
            float gx = cx + cd.xoff() * INV_OS;
            float gy = by + cd.yoff() * INV_OS;

            if (sw > 0 && sh > 0) {
                drawGlyph(g, gx, gy, cd, ital, col);
                if (bold) drawGlyph(g, gx + 1, gy, cd, ital, col);
            }

            float adv = cd.xadvance() * INV_OS;
            if (bold) adv += 1;
            cx += adv;
            prev = cp;
        }

        if (und) fill(g, lsx, cx, by + 1, col);
        if (strk) fill(g, lsx, cx, by - asc * 0.35f, col);

        // Siblings
        for (net.minecraft.network.chat.Component sib : comp.getSiblings()) {
            cx = renderComponentRec(g, sib, cx, by, baseCol, shadow, prev);
        }

        return cx;
    }

    public float getComponentWidth(net.minecraft.network.chat.Component comp) {
        if (comp == null) return 0;
        return getCompWidthRec(comp, -1);
    }

    private float getCompWidthRec(net.minecraft.network.chat.Component comp, int prev) {
        float w = 0;
        boolean bold = comp.getStyle().isBold();
        
        // Используем visit для правильного извлечения текста
        StringBuilder contentBuilder = new StringBuilder();
        comp.getContents().visit((text) -> {
            contentBuilder.append(text);
            return java.util.Optional.empty();
        });
        String content = contentBuilder.toString();

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            
            // Обработка §-кодов (пропускаем их при подсчёте ширины)
            if (ch == '§' && i + 1 < content.length()) {
                char code = Character.toLowerCase(content.charAt(i + 1));
                i++; // Пропускаем код
                
                // §r сбрасывает форматирование
                if (code == 'r') {
                    bold = false;
                } else if (code == 'l') {
                    bold = true;
                }
                // Цветовые коды НЕ сбрасывают bold
                
                prev = -1;
                continue;
            }
            
            if (ch == '\n') { prev = -1; continue; }
            
            int cp = (int) ch;
            if (prev != -1) w += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, cp) * scale;
            
            float cw = getCharW(ch);
            if (bold) cw += 1;
            w += cw;
            prev = cp;
        }

        for (net.minecraft.network.chat.Component sib : comp.getSiblings()) {
            w += getCompWidthRec(sib, prev);
        }

        return w;
    }

    // ★★★ STATE — как у них ★★★
    private static final class State {
        int col;
        boolean bold, ital, und, strk, obf;

        void reset(int base, boolean shadow) {
            col = shadow ? darken(base) : base;
            bold = ital = und = strk = obf = false;
        }
    }

    private void render(GuiGraphics g, String t, float x, float y,
                        int baseCol, boolean shadow) {
        float bx = x, cx = x, by = y + asc;
        State s = new State();
        s.reset(baseCol, shadow);
        int prev = -1;

        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);

            // ★★★ ИХ ЛОГИКА: проверка null + RESET ★★★
            if (ch == '§' && i + 1 < t.length()) {
                ChatFormatting fmt = ChatFormatting.getByCode(t.charAt(i + 1));
                i++; // пропускаем код

                if (fmt == null) continue; // ★★★ НЕВАЛИДНЫЙ КОД — пропуск ★★★

                if (fmt == ChatFormatting.RESET) {
                    s.reset(baseCol, shadow);
                    prev = -1;
                    continue;
                }

                if (fmt.isColor()) {
                    // Цвет: alpha из base, RGB из формата
                    s.col = ARGB.color(ARGB.alpha(baseCol),
                            ARGB.red(fmt.getColor()),
                            ARGB.green(fmt.getColor()),
                            ARGB.blue(fmt.getColor()));
                    if (shadow) s.col = darken(s.col);
                } else {
                    // Форматирование
                    switch (fmt) {
                        case BOLD -> s.bold = true;
                        case ITALIC -> s.ital = true;
                        case UNDERLINE -> s.und = true;
                        case STRIKETHROUGH -> s.strk = true;
                        case OBFUSCATED -> s.obf = true;
                        default -> {
                        }
                    }
                }
                continue;
            }

            if (ch == '\n') {
                drawDeco(g, bx, cx, by, s);
                cx = bx;
                by += lineH;
                prev = -1;
                continue;
            }

            char rc = s.obf && ch != ' ' ? randChar(ch) : ch;
            int cp = rc;

            if (prev != -1)
                cx += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, cp) * scale;
            prev = cp;

            Integer slot = idxMap.get(cp);
            if (slot == null) {
                cx += fontSize * .5f + (s.bold ? 1 : 0);
                continue;
            }

            STBTTBakedChar bc = baked.get(slot);
            int sw = bc.x1() - bc.x0(), sh = bc.y1() - bc.y0();
            float gx = cx + bc.xoff() * INV_OS, gy = by + bc.yoff() * INV_OS;
            if (sw > 0 && sh > 0) {
                drawGlyph(g, gx, gy, bc, s.ital, s.col);
                if (s.bold) drawGlyph(g, gx + 1, gy, bc, s.ital, s.col);
            }
            cx += bc.xadvance() * INV_OS + (s.bold ? 1 : 0);
        }
        drawDeco(g, bx, cx, by, s);
    }

    private void drawDeco(GuiGraphics g, float fx, float tx, float by, State s) {
        int c = s.col;
        if (s.und) fill(g, fx, tx, by + 1, c);
        if (s.strk) fill(g, fx, tx, by - asc * .35f, c);
    }

    private void drawGlyph(GuiGraphics g, float x, float y,
                           STBTTBakedChar bc, boolean ital, int col) {
        int sw = bc.x1() - bc.x0(), sh = bc.y1() - bc.y0();
        if (sw <= 0 || sh <= 0) return;
        g.pose().pushMatrix();
        g.pose().translateLocal(x, y);
        g.pose().scale(INV_OS, INV_OS);
        if (ital) {
            g.pose().translateLocal(.25f * sh * INV_OS, 0);
            g.pose().shearX(-.25f);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, atlasId, 0, 0,
                bc.x0(), bc.y0(), sw, sh, sw, sh, ATLAS_SIZE, ATLAS_SIZE, col);
        g.pose().popMatrix();
    }

    private void fill(GuiGraphics g, float x1, float x2, float yy, int col) {
        int y = Math.round(yy), a = Math.round(x1), b = Math.round(x2);
        if (b > a) g.fill(a, y, b, y + 1, col);
    }

    // ==================== METRICS ====================

    public float getWidth(String t) {
        if (isEmpty(t)) return 0;
        float w = 0, max = 0;
        boolean bold = false;
        int prev = -1;

        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);

            // ★★★ ИХ ЛОГИКА: проверка null ★★★
            if (ch == '§' && i + 1 < t.length()) {
                ChatFormatting fmt = ChatFormatting.getByCode(t.charAt(i + 1));
                i++;

                if (fmt == null) continue; // невалидный код

                if (fmt == ChatFormatting.RESET) {
                    bold = false;
                    prev = -1;
                    continue;
                }
                if (fmt == ChatFormatting.BOLD) bold = true;
                continue;
            }

            if (ch == '\n') {
                max = Math.max(max, w);
                w = 0;
                prev = -1;
                continue;
            }

            if (prev != -1)
                w += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, ch) * scale;
            prev = ch;
            w += getCharW(ch) + (bold ? 1 : 0);
        }
        return Math.max(max, w);
    }

    public float getWidth(char c) {
        return getCharW(c);
    }

    private float getCharW(char c) {
        Float v = widths.get((int) c);
        return v != null ? v : fontSize * .3f;
    }

    public float getHeight(String t) {
        if (isEmpty(t)) return lineH;
        int l = 1;
        for (int i = 0; i < t.length(); i++) {
            if (t.charAt(i) == '\n') l++;
            else if (t.charAt(i) == '§') i++;
        }
        return l * lineH;
    }

    public float getHeight() {
        return lineH;
    }

    public float getAscent() {
        return asc;
    }

    public float getDescent() {
        return -desc;
    }

    public String trimToWidth(String t, float max) {
        if (isEmpty(t)) return "";
        StringBuilder sb = new StringBuilder();
        float w = 0;
        boolean bold = false;
        int prev = -1;

        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);

            if (ch == '§' && i + 1 < t.length()) {
                ChatFormatting fmt = ChatFormatting.getByCode(t.charAt(i + 1));
                i++;
                sb.append(ch).append(t.charAt(i));

                if (fmt == null) continue;
                if (fmt == ChatFormatting.RESET) {
                    bold = false;
                    prev = -1;
                    continue;
                }
                if (fmt == ChatFormatting.BOLD) bold = true;
                continue;
            }

            float k = prev != -1 ?
                    STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, ch) * scale : 0;
            float cw = getCharW(ch) + k + (bold ? 1 : 0);
            if (w + cw > max) break;
            sb.append(ch);
            w += cw;
            prev = ch;
        }
        return sb.toString();
    }

    public static String strip(String t) {
        if (isEmpty(t)) return "";
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++)
            if (t.charAt(i) == '§') i++;
            else sb.append(t.charAt(i));
        return sb.toString();
    }

    // ==================== UTILS ====================

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static int darken(int c) {
        return ARGB.color(ARGB.alpha(c),
                (int) (ARGB.red(c) * .25f), (int) (ARGB.green(c) * .25f), (int) (ARGB.blue(c) * .25f));
    }

    private char randChar(char orig) {
        float tw = getCharW(orig);
        for (int a = 0; a < 5; a++) {
            char cand = (char) (int) codepoints.get((int) (Math.random() * codepoints.size()));
            if (Math.abs(getCharW(cand) - tw) < tw * .3f) return cand;
        }
        return codepoints.isEmpty() ? orig : (char) (int) codepoints.get((int) (Math.random() * codepoints.size()));
    }

    private static ByteBuffer load(String p) throws IOException {
        String cp = p.startsWith("/") ? p.substring(1) : p;
        InputStream is = CustomFont.class.getClassLoader().getResourceAsStream(cp);
        if (is == null) throw new IOException("Not found: " + p);
        try {
            byte[] b = is.readAllBytes();
            ByteBuffer bb = MemoryUtil.memAlloc(b.length);
            bb.put(b).flip();
            return bb;
        } finally {
            is.close();
        }
    }

    @Override
    public void close() {
        if (baked != null) baked.free();
        if (atlasId != null) Minecraft.getInstance().getTextureManager().release(atlasId);
        if (atlasTexture != null) {
            atlasTexture.close();
            atlasTexture = null;
        }
        atlasId = null;
        if (ttfBuffer != null) MemoryUtil.memFree(ttfBuffer);
    }
}