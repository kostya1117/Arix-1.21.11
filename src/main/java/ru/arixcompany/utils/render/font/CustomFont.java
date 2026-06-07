package ru.arixcompany.utils.render.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.Font;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CustomFont implements AutoCloseable {

    private static final int ATLAS_SIZE = 1024;
    private static final int OVERSAMPLE = 4;
    private static final float INV_OS = 1f / OVERSAMPLE;
    private static final int PAD = 4;

    private final STBTTFontinfo fontInfo;
    private final ByteBuffer ttfBuffer;
    @Getter private final float fontSize;
    private final float scale;
    private final float asc, desc, gap, lineH;

    private static final Map<String, FallbackData> fallbackPool = new ConcurrentHashMap<>();
    private final Map<Integer, String> cpFontMap = new ConcurrentHashMap<>();

    private final RandomSource random = RandomSource.create();
    private final Map<Integer, char[]> obfByAdvance = new HashMap<>();

    private static class FallbackData {
        final STBTTFontinfo info;
        final ByteBuffer buffer;
        FallbackData(STBTTFontinfo info, ByteBuffer buffer) {
            this.info = info;
            this.buffer = buffer;
        }
    }

    private final Map<Integer, float[]> glyphCache = new ConcurrentHashMap<>();
    private final Map<Integer, Float> widthCache = new ConcurrentHashMap<>();

    private ByteBuffer atlasBitmap;
    private NativeImage atlasImage;
    private DynamicTexture atlasTexture;
    private Identifier atlasId;
    private boolean atlasDirty = false;
    private int packX = 0, packY = 0, packRowH = 0;

    private int dirtyX0 = ATLAS_SIZE, dirtyY0 = ATLAS_SIZE, dirtyX1 = 0, dirtyY1 = 0;

    public CustomFont(String path, float sz) throws IOException {
        fontSize = sz;
        ttfBuffer = loadResource(path);
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

        try (MemoryStack st = MemoryStack.stackPush()) {
            IntBuffer adv = st.mallocInt(1), lsb = st.mallocInt(1);
            for (int cp = 0x0020; cp <= 0xFFFF; cp++) {
                if (STBTruetype.stbtt_FindGlyphIndex(fontInfo, cp) != 0) {
                    STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, cp, adv, lsb);
                    widthCache.put(cp, adv.get(0) * scale);
                }
            }
        }

        atlasBitmap = MemoryUtil.memCalloc(ATLAS_SIZE * ATLAS_SIZE);
        atlasImage = new NativeImage(NativeImage.Format.RGBA, ATLAS_SIZE, ATLAS_SIZE, false);
        String texId = "cf_" + path.hashCode() + "_" + (int) sz;
        atlasTexture = new DynamicTexture(() -> texId, atlasImage);
        atlasTexture.setSampler(RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        atlasId = Identifier.withDefaultNamespace("dynamic/" + texId);
        Minecraft.getInstance().getTextureManager().register(atlasId, atlasTexture);

        for (int cp = 0x0020; cp <= 0xFFFF; cp++) {
            if (widthCache.containsKey(cp)) {
                getOrBakeGlyph(cp);
            }
        }
        buildObfMap();
        flushAtlas();
    }

    /** Как в ваниле: ceil(advance) → char[] — только из уже запечённых ASCII */
    private void buildObfMap() {
        Map<Integer, List<Character>> tmp = new HashMap<>();
        for (int cp = 0x0020; cp <= 0xFFFF; cp++) {
            float[] ge = glyphCache.get(cp);
            if (ge != null && ge[2] > 0 && ge[6] > 0) {
                int advanceKey = (int) Math.ceil(ge[6]);
                tmp.computeIfAbsent(advanceKey, k -> new ArrayList<>()).add((char) cp);
            }
        }
        for (Map.Entry<Integer, List<Character>> e : tmp.entrySet()) {
            List<Character> list = e.getValue();
            char[] arr = new char[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
            obfByAdvance.put(e.getKey(), arr);
        }
    }

    private String findFontForCodepoint(int cp) {
        String cached = cpFontMap.get(cp);
        if (cached != null) return cached;

        if (STBTruetype.stbtt_FindGlyphIndex(fontInfo, cp) != 0) return null;

        char c = (char) cp;

        Font common = FontManager.getCommonFont();
        if (common != null && common.canDisplay(c)) {
            String name = common.getFontName();
            ensureFallbackLoaded(name);
            if (fallbackPool.containsKey(name)) {
                cpFontMap.put(cp, name);
                return name;
            }
        }

        Font cjk = FontManager.getCjkFont();
        if (cjk != null && cjk.canDisplay(c)) {
            String name = cjk.getFontName();
            ensureFallbackLoaded(name);
            if (fallbackPool.containsKey(name)) {
                cpFontMap.put(cp, name);
                return name;
            }
        }

        cpFontMap.put(cp, "");
        return "";
    }

    private static void ensureFallbackLoaded(String fontName) {
        if (fallbackPool.containsKey(fontName)) return;

        fallbackPool.computeIfAbsent(fontName, name -> {
            ByteBuffer buf = findFontFile(name);
            if (buf == null) return null;

            STBTTFontinfo info = STBTTFontinfo.create();
            if (!STBTruetype.stbtt_InitFont(info, buf)) {
                MemoryUtil.memFree(buf);
                return null;
            }
            return new FallbackData(info, buf);
        });

        fallbackPool.values().removeIf(Objects::isNull);
    }

    private static ByteBuffer findFontFile(String fontName) {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> dirs = new ArrayList<>();

        if (os.contains("win")) {
            String windir = System.getenv("WINDIR");
            if (windir != null) dirs.add(windir + "\\Fonts");
            String local = System.getenv("LOCALAPPDATA");
            if (local != null) dirs.add(local + "\\Microsoft\\Windows\\Fonts");
        } else if (os.contains("mac")) {
            dirs.add("/System/Library/Fonts");
            dirs.add("/System/Library/Fonts/Supplemental");
            dirs.add("/Library/Fonts");
            dirs.add(System.getProperty("user.home") + "/Library/Fonts");
        } else {
            dirs.add("/usr/share/fonts");
            dirs.add("/usr/local/share/fonts");
            dirs.add(System.getProperty("user.home") + "/.fonts");
            dirs.add(System.getProperty("user.home") + "/.local/share/fonts");
        }

        String clean = fontName.replace(" ", "").toLowerCase();
        for (String dir : dirs) {
            ByteBuffer found = searchDir(new File(dir), clean, 3);
            if (found != null) return found;
        }
        return null;
    }

    private static ByteBuffer searchDir(File dir, String nameLower, int depth) {
        if (depth <= 0 || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isDirectory()) {
                ByteBuffer found = searchDir(f, nameLower, depth - 1);
                if (found != null) return found;
            } else {
                String fn = f.getName().toLowerCase();
                if ((fn.endsWith(".ttf") || fn.endsWith(".otf"))
                        && fn.replace("-", "").replace("_", "").contains(nameLower)) {
                    try (FileInputStream fis = new FileInputStream(f)) {
                        byte[] bytes = fis.readAllBytes();
                        ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
                        buf.put(bytes).flip();
                        return buf;
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    private FallbackData getFallback(int cp) {
        String fontName = findFontForCodepoint(cp);
        if (fontName == null || fontName.isEmpty()) return null;
        return fallbackPool.get(fontName);
    }

    private boolean isFallback(int cp) {
        String name = cpFontMap.get(cp);
        return name != null && !name.isEmpty();
    }

    private float[] getOrBakeGlyph(int cp) {
        float[] cached = glyphCache.get(cp);
        if (cached != null) return cached;
        synchronized (fontInfo) {
            cached = glyphCache.get(cp);
            if (cached != null) return cached;
            cached = bakeGlyph(cp);
            glyphCache.put(cp, cached);
        }
        return cached;
    }

    private float[] bakeGlyph(int cp) {
        FallbackData fb = getFallback(cp);
        STBTTFontinfo info = fb != null ? fb.info : fontInfo;
        float sc = fb != null ? STBTruetype.stbtt_ScaleForPixelHeight(info, fontSize) : scale;

        if (STBTruetype.stbtt_FindGlyphIndex(info, cp) == 0)
            return new float[]{0, 0, 0, 0, 0, 0, fontSize * 0.3f};

        float fbAsc = asc;
        if (fb != null) {
            try (MemoryStack st = MemoryStack.stackPush()) {
                IntBuffer a = st.mallocInt(1), d = st.mallocInt(1), g = st.mallocInt(1);
                STBTruetype.stbtt_GetFontVMetrics(info, a, d, g);
                fbAsc = a.get(0) * sc;
            }
        }

        try (MemoryStack st = MemoryStack.stackPush()) {
            IntBuffer adv = st.mallocInt(1), lsb = st.mallocInt(1);
            STBTruetype.stbtt_GetCodepointHMetrics(info, cp, adv, lsb);
            float advance = adv.get(0) * sc;
            widthCache.putIfAbsent(cp, advance);

            IntBuffer x0 = st.mallocInt(1), y0 = st.mallocInt(1),
                    x1 = st.mallocInt(1), y1 = st.mallocInt(1);
            STBTruetype.stbtt_GetCodepointBitmapBoxSubpixel(info, cp,
                    sc * OVERSAMPLE, sc * OVERSAMPLE, 0f, 0f, x0, y0, x1, y1);

            float offX = x0.get(0) * INV_OS;
            float offY = y0.get(0) * INV_OS + (fb != null ? (asc - fbAsc) : 0f);

            int gw = x1.get(0) - x0.get(0), gh = y1.get(0) - y0.get(0);
            if (gw <= 0 || gh <= 0) return new float[]{0, 0, 0, 0, offX, offY, advance};

            IntBuffer pw = st.mallocInt(1), ph = st.mallocInt(1),
                    ox = st.mallocInt(1), oy = st.mallocInt(1);
            ByteBuffer gb = STBTruetype.stbtt_GetCodepointBitmapSubpixel(info,
                    sc * OVERSAMPLE, sc * OVERSAMPLE, 0f, 0f, cp, pw, ph, ox, oy);
            if (gb == null) return new float[]{0, 0, 0, 0, offX, offY, advance};

            int aw = pw.get(0), ah = ph.get(0);
            int px, py;
            synchronized (this) {
                if (packX + aw + PAD >= ATLAS_SIZE) { packX = 0; packY += packRowH; packRowH = 0; }
                if (packY + ah + PAD >= ATLAS_SIZE) { STBTruetype.stbtt_FreeBitmap(gb); return new float[]{0, 0, 0, 0, offX, offY, advance}; }
                px = packX; py = packY;
                packX += aw + PAD;
                packRowH = Math.max(packRowH, ah + PAD);
            }

            for (int gy = 0; gy < ah; gy++)
                for (int gx = 0; gx < aw; gx++)
                    atlasBitmap.put((py + gy) * ATLAS_SIZE + (px + gx), gb.get(gy * aw + gx));
            STBTruetype.stbtt_FreeBitmap(gb);

            atlasDirty = true;
            dirtyX0 = Math.min(dirtyX0, px);
            dirtyY0 = Math.min(dirtyY0, py);
            dirtyX1 = Math.max(dirtyX1, px + aw);
            dirtyY1 = Math.max(dirtyY1, py + ah);

            return new float[]{px, py, aw, ah, offX, offY, advance};
        }
    }

    private void flushAtlas() {
        if (!atlasDirty) return;
        atlasDirty = false;

        int x0 = Math.max(0, dirtyX0 - 1);
        int y0 = Math.max(0, dirtyY0 - 1);
        int x1 = Math.min(ATLAS_SIZE, dirtyX1 + 1);
        int y1 = Math.min(ATLAS_SIZE, dirtyY1 + 1);
        dirtyX0 = ATLAS_SIZE; dirtyY0 = ATLAS_SIZE; dirtyX1 = 0; dirtyY1 = 0;

        for (int y = y0; y < y1; y++) {
            int row = y * ATLAS_SIZE;
            for (int x = x0; x < x1; x++) {
                int c = atlasBitmap.get(row + x) & 255;
                int l = x > 0 ? atlasBitmap.get(row + x - 1) & 255 : c;
                int r = x + 1 < ATLAS_SIZE ? atlasBitmap.get(row + x + 1) & 255 : c;
                int u = y > 0 ? atlasBitmap.get(row - ATLAS_SIZE + x) & 255 : c;
                int d = y + 1 < ATLAS_SIZE ? atlasBitmap.get(row + ATLAS_SIZE + x) & 255 : c;
                float v = (c * 4f + l + r + u + d) / 8f / 255f;
                int a = Math.clamp(Math.round(v * 255f), 0, 255);
                atlasImage.setPixelABGR(x, y, (a << 24) | 0x00FFFFFF);
            }
        }
        atlasTexture.upload();
    }

    // ═══════════════════════════════════════════════
    //  Rendering
    // ═══════════════════════════════════════════════

    private float snap(float v) { return Math.round(v * 2f) * 0.5f; }

    public void drawString(GuiGraphics g, String t, float x, float y, int col) { drawString(g, t, x, y, col, false); }

    public void drawString(GuiGraphics g, String t, float x, float y, int col, boolean shadow) {
        if (isNullOrEmpty(t)) return;
        flushAtlas();
        if (shadow) render(g, t, x + 1, y + 1, col, true);
        render(g, t, x, y, col, false);
    }

    public void drawCenteredString(GuiGraphics g, String t, float x, float y, int col) { drawString(g, t, x - getWidth(t) / 2f, y, col); }
    public void drawCenteredString(GuiGraphics g, String t, float x, float y, int col, boolean sh) { drawString(g, t, x - getWidth(t) / 2f, y, col, sh); }
    public void drawRightString(GuiGraphics g, String t, float x, float y, int col) { drawString(g, t, x - getWidth(t), y, col); }
    public void drawRightString(GuiGraphics g, String t, float x, float y, int col, boolean sh) { drawString(g, t, x - getWidth(t), y, col, sh); }

    public void drawComponent(GuiGraphics g, Component comp, float x, float y, int col) { drawComponent(g, comp, x, y, col, false); }
    public void drawComponent(GuiGraphics g, Component comp, float x, float y, int col, boolean shadow) {
        if (comp == null) return;
        flushAtlas();
        if (shadow) renderComponent(g, comp, x + 1, y + 1, col, true);
        renderComponent(g, comp, x, y, col, false);
    }
    public void drawCenteredComponent(GuiGraphics g, Component comp, float x, float y, int col) { drawComponent(g, comp, x - getComponentWidth(comp) / 2f, y, col); }
    public void drawCenteredComponent(GuiGraphics g, Component comp, float x, float y, int col, boolean sh) { drawComponent(g, comp, x - getComponentWidth(comp) / 2f, y, col, sh); }
    public void drawRightComponent(GuiGraphics g, Component comp, float x, float y, int col) { drawComponent(g, comp, x - getComponentWidth(comp), y, col); }
    public void drawRightComponent(GuiGraphics g, Component comp, float x, float y, int col, boolean sh) { drawComponent(g, comp, x - getComponentWidth(comp), y, col, sh); }


    public static int getRarityColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0xFFFFFFFF;
        ChatFormatting rarityFormatting = stack.getRarity().color();
        Integer rarityRgb = rarityFormatting.getColor();
        if (rarityRgb != null) {
            return 0xFF000000 | (rarityRgb & 0x00FFFFFF);
        }
        return 0xFFFFFFFF;
    }

    public void drawItemName(GuiGraphics g, ItemStack stack, float x, float y) {
        drawItemName(g, stack, x, y, false);
    }

    public void drawItemName(GuiGraphics g, ItemStack stack, float x, float y, boolean shadow) {
        if (stack == null || stack.isEmpty()) return;
        Component name = stack.getHoverName();
        int col = getRarityColor(stack);
        drawComponent(g, name, x, y, col, shadow);
    }

    public void drawCenteredItemName(GuiGraphics g, ItemStack stack, float x, float y) {
        drawCenteredItemName(g, stack, x, y, false);
    }

    public void drawCenteredItemName(GuiGraphics g, ItemStack stack, float x, float y, boolean shadow) {
        if (stack == null || stack.isEmpty()) return;
        Component name = stack.getHoverName();
        int col = getRarityColor(stack);
        drawCenteredComponent(g, name, x, y, col, shadow);
    }

    public void drawRightItemName(GuiGraphics g, ItemStack stack, float x, float y) {
        drawRightItemName(g, stack, x, y, false);
    }

    public void drawRightItemName(GuiGraphics g, ItemStack stack, float x, float y, boolean shadow) {
        if (stack == null || stack.isEmpty()) return;
        Component name = stack.getHoverName();
        int col = getRarityColor(stack);
        drawRightComponent(g, name, x, y, col, shadow);
    }

    public void drawItemNameString(GuiGraphics g,Component name, ItemStack stack, float x, float y) {
        drawItemNameString(g,name, stack, x, y, false);
    }

    public void drawItemNameString(GuiGraphics g,Component name,ItemStack stack, float x, float y, boolean shadow) {
        if (stack == null || stack.isEmpty()) return;
        int col = getRarityColor(stack);
        drawComponent(g, name, x, y, col, shadow);
    }

    public float getItemNameWidth(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0f;
        return getComponentWidth(stack.getHoverName());
    }

    // ═══════════════════════════════════════════════

    private void render(GuiGraphics g, String t, float x, float y, int baseCol, boolean shadow) {
        float bx = x, cx = x, by = y + asc;
        int col = shadow ? shadowColor(baseCol) : baseCol;
        boolean bold = false, ital = false, und = false, strk = false, obf = false;
        int prev = -1;

        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == '§' && i + 1 < t.length()) {
                ChatFormatting fmt = ChatFormatting.getByCode(t.charAt(i + 1)); i++;
                if (fmt == null) continue;
                if (fmt == ChatFormatting.RESET) { col = shadow ? shadowColor(baseCol) : baseCol; bold = ital = und = strk = obf = false; prev = -1; continue; }
                if (fmt.isColor() && fmt.getColor() != null) { col = ARGB.color(ARGB.alpha(baseCol), fmt.getColor()); if (shadow) col = shadowColor(col); }
                else switch (fmt) { case BOLD -> bold = true; case ITALIC -> ital = true; case UNDERLINE -> und = true; case STRIKETHROUGH -> strk = true; case OBFUSCATED -> obf = true; default -> {} }
                continue;
            }
            if (ch == '\n') { if (und) drawEffect(g, bx, cx, by + 1, col); if (strk) drawEffect(g, bx, cx, by - asc * 0.35f, col); cx = bx; by += lineH; prev = -1; continue; }

            int cp = obf && ch != ' ' ? getRandomGlyph(ch) : ch;
            if (prev != -1 && !isFallback(cp) && !isFallback(prev)) cx += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, cp) * scale;
            prev = cp;

            float[] ge = getOrBakeGlyph(cp);
            float boldOff = bold ? getBoldOffset() : 0f;
            if (ge[2] > 0 && ge[3] > 0) { float gx = snap(cx + ge[4]), gy = snap(by + ge[5]); drawGlyph(g, gx, gy, ge, ital, col); if (bold) drawGlyph(g, gx + boldOff, gy, ge, ital, col); }
            cx += ge[6] + (bold ? boldOff : 0f);
        }
        if (und) drawEffect(g, bx, cx, by + 1, col);
        if (strk) drawEffect(g, bx, cx, by - asc * 0.35f, col);
    }

    private void renderComponent(GuiGraphics g, Component comp, float x, float y, int baseCol, boolean shadow) {
        renderComponentRec(g, comp, x, y + asc, baseCol, shadow, -1);
    }

    private float renderComponentRec(GuiGraphics g, Component comp, float cx, float by, int baseCol, boolean shadow, int prev) {
        Style style = comp.getStyle();
        int col = resolveStyleColor(style, baseCol, shadow);
        boolean bold = style.isBold(), ital = style.isItalic(), und = style.isUnderlined(), strk = style.isStrikethrough(), obf = style.isObfuscated();
        String content = extractContents(comp);
        float lx = cx;

        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '§' && i + 1 < content.length()) {
                char code = Character.toLowerCase(content.charAt(++i));
                if (code == 'r') { col = shadow ? shadowColor(baseCol) : baseCol; bold = ital = und = strk = obf = false; }
                else { ChatFormatting fmt = ChatFormatting.getByCode(code);
                    if (fmt != null && fmt.isColor() && fmt.getColor() != null) { col = ARGB.color(ARGB.alpha(baseCol), fmt.getColor()); if (shadow) col = shadowColor(col); }
                    else if (fmt != null) switch (fmt) { case BOLD -> bold = true; case ITALIC -> ital = true; case UNDERLINE -> und = true; case STRIKETHROUGH -> strk = true; case OBFUSCATED -> obf = true; default -> {} }
                } prev = -1; continue;
            }
            if (ch == '\n') { if (und) drawEffect(g, lx, cx, by + 1, col); if (strk) drawEffect(g, lx, cx, by - asc * 0.35f, col); lx = cx; by += lineH; prev = -1; continue; }

            int cp = obf && ch != ' ' ? getRandomGlyph(ch) : ch;
            if (prev != -1 && !isFallback(cp) && !isFallback(prev)) cx += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, cp) * scale;
            prev = cp;

            float[] ge = getOrBakeGlyph(cp);
            float boldOff = bold ? getBoldOffset() : 0f;
            if (ge[2] > 0 && ge[3] > 0) { float gx = snap(cx + ge[4]), gy = snap(by + ge[5]); drawGlyph(g, gx, gy, ge, ital, col); if (bold) drawGlyph(g, gx + boldOff, gy, ge, ital, col); }
            cx += ge[6] + (bold ? boldOff : 0f);
        }
        if (und) drawEffect(g, lx, cx, by + 1, col);
        if (strk) drawEffect(g, lx, cx, by - asc * 0.35f, col);
        for (Component sib : comp.getSiblings()) cx = renderComponentRec(g, sib, cx, by, baseCol, shadow, prev);
        return cx;
    }

    private void drawGlyph(GuiGraphics g, float x, float y, float[] ge, boolean italic, int col) {
        int aw = (int) ge[2], ah = (int) ge[3];
        if (aw <= 0 || ah <= 0) return;
        g.pose().pushMatrix(); g.pose().translateLocal(x, y); g.pose().scale(INV_OS, INV_OS);
        if (italic) { g.pose().translateLocal(0.25f * ah * INV_OS, 0); g.pose().shearX(-0.25f); }
        g.blit(RenderPipelines.GUI_TEXTURED, atlasId, 0, 0, (int) ge[0], (int) ge[1], aw, ah, aw, ah, ATLAS_SIZE, ATLAS_SIZE, col);
        g.pose().popMatrix();
    }

    private void drawEffect(GuiGraphics g, float x1, float x2, float yy, int col) {
        int y = Math.round(yy), a = Math.round(x1), b = Math.round(x2);
        if (b > a) g.fill(a, y, b, y + 1, col);
    }

    private float getBoldOffset() { return Math.max(0.35f, fontSize / 28f); }

    private int resolveStyleColor(Style style, int baseCol, boolean shadow) {
        if (style.getColor() != null) { int c = ARGB.color(ARGB.alpha(baseCol), style.getColor().getValue()); return shadow ? shadowColor(c) : c; }
        return shadow ? shadowColor(baseCol) : baseCol;
    }

    private static int shadowColor(int c) { return ARGB.color(ARGB.alpha(c), (int)(ARGB.red(c)*0.25f), (int)(ARGB.green(c)*0.25f), (int)(ARGB.blue(c)*0.25f)); }

    private static String extractContents(Component comp) {
        StringBuilder sb = new StringBuilder();
        comp.getContents().visit(txt -> { sb.append(txt); return Optional.empty(); });
        return sb.toString();
    }

    // ═══════════════════════════════════════════════
    //  Metrics
    // ═══════════════════════════════════════════════

    public float getWidth(String t) {
        if (isNullOrEmpty(t)) return 0f;
        float w = 0f, max = 0f; boolean bold = false; int prev = -1;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == '§' && i + 1 < t.length()) { ChatFormatting fmt = ChatFormatting.getByCode(t.charAt(i+1)); i++; if (fmt == ChatFormatting.RESET) { bold = false; prev = -1; } else if (fmt == ChatFormatting.BOLD) bold = true; continue; }
            if (ch == '\n') { max = Math.max(max, w); w = 0; prev = -1; continue; }
            if (prev != -1 && !isFallback(ch) && !isFallback(prev)) w += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, ch) * scale;
            prev = ch; w += getCharWidth(ch) + (bold ? getBoldOffset() : 0f);
        }
        return Math.max(max, w);
    }

    public float getComponentWidth(Component comp) { return comp == null ? 0f : measureComponentRec(comp, -1); }

    private float measureComponentRec(Component comp, int prev) {
        float w = 0; boolean bold = comp.getStyle().isBold(); String content = extractContents(comp);
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (ch == '§' && i + 1 < content.length()) { ChatFormatting fmt = ChatFormatting.getByCode(content.charAt(i+1)); i++; if (fmt == ChatFormatting.RESET) bold = false; else if (fmt == ChatFormatting.BOLD) bold = true; prev = -1; continue; }
            if (ch == '\n') { prev = -1; continue; }
            if (prev != -1 && !isFallback(ch) && !isFallback(prev)) w += STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, ch) * scale;
            prev = ch; w += getCharWidth(ch) + (bold ? getBoldOffset() : 0f);
        }
        for (Component sib : comp.getSiblings()) w += measureComponentRec(sib, prev);
        return w;
    }

    public float getWidth(char c) { return getCharWidth(c); }

    private float getCharWidth(char c) {
        Float v = widthCache.get((int) c);
        if (v != null) return v;
        getOrBakeGlyph(c);
        v = widthCache.get((int) c);
        return v != null ? v : fontSize * 0.3f;
    }

    public float getHeight(String t) {
        if (isNullOrEmpty(t)) return lineH;
        int lines = 1; for (int i = 0; i < t.length(); i++) { if (t.charAt(i) == '\n') lines++; else if (t.charAt(i) == '§') i++; }
        return lines * lineH;
    }

    public float getHeight() { return lineH; }
    public float getAscent() { return asc; }
    public float getDescent() { return -desc; }

    public String trimToWidth(String t, float maxWidth) {
        if (isNullOrEmpty(t)) return "";
        StringBuilder result = new StringBuilder(); float w = 0; boolean bold = false; int prev = -1;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == '§' && i + 1 < t.length()) { ChatFormatting fmt = ChatFormatting.getByCode(t.charAt(i+1)); result.append(ch).append(t.charAt(i+1)); i++; if (fmt == ChatFormatting.RESET) { bold = false; prev = -1; } else if (fmt == ChatFormatting.BOLD) bold = true; continue; }
            float kern = (prev != -1 && !isFallback(ch) && !isFallback(prev)) ? STBTruetype.stbtt_GetCodepointKernAdvance(fontInfo, prev, ch) * scale : 0f;
            float cw = getCharWidth(ch) + kern + (bold ? getBoldOffset() : 0f);
            if (w + cw > maxWidth) break;
            result.append(ch); w += cw; prev = ch;
        }
        return result.toString();
    }

    public static String stripFormatting(String t) {
        if (isNullOrEmpty(t)) return "";
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) { if (t.charAt(i) == '§') i++; else sb.append(t.charAt(i)); }
        return sb.toString();
    }

    // ── Как в ваниле: getGlyph → ceil(advance) → getRandomGlyph(random, advance) ──
    private char getRandomGlyph(char orig) {
        float[] ge = glyphCache.get((int) orig);
        if (ge == null) {
            Float w = widthCache.get((int) orig);
            if (w == null) return orig;
            int adv = (int) Math.ceil(w);
            char[] pool = obfByAdvance.get(adv);
            if (pool != null && pool.length > 0) return pool[random.nextInt(pool.length)];
            return orig;
        }
        int adv = (int) Math.ceil(ge[6]);
        char[] pool = obfByAdvance.get(adv);
        if (pool != null && pool.length > 0) return pool[random.nextInt(pool.length)];
        return orig;
    }

    private static boolean isNullOrEmpty(String s) { return s == null || s.isEmpty(); }

    private static ByteBuffer loadResource(String path) throws IOException {
        String cp = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream is = CustomFont.class.getClassLoader().getResourceAsStream(cp)) {
            if (is == null) throw new IOException("Resource not found: " + path);
            byte[] bytes = is.readAllBytes(); ByteBuffer buf = MemoryUtil.memAlloc(bytes.length); buf.put(bytes).flip(); return buf;
        }
    }

    @Override
    public void close() {
        glyphCache.clear(); cpFontMap.clear();
        if (atlasId != null) { Minecraft.getInstance().getTextureManager().release(atlasId); atlasId = null; }
        if (atlasTexture != null) { atlasTexture.close(); atlasTexture = null; }
        if (atlasBitmap != null) MemoryUtil.memFree(atlasBitmap);
        if (ttfBuffer != null) MemoryUtil.memFree(ttfBuffer);
    }
}