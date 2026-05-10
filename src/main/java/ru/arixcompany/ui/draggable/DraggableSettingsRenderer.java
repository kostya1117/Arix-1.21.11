package ru.arixcompany.ui.draggable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.ui.clickgui.components.module.settings.BooleanSettingComponent;
import ru.arixcompany.ui.clickgui.components.module.settings.SelectSettingComponent;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class DraggableSettingsRenderer {

    private DraggableSettingsRenderer() {}

    private static final float WIDTH    = 130f;
    private static final float PAD      = 6f;
    private static final float RADIUS   = 4f;
    private static final float HEADER_H = 16f;

    private static final float CONTENT_W = WIDTH - PAD * 2;

    private static final Map<Setting, IComponent> cache = new WeakHashMap<>();

    private static IComponent getOrCreate(Setting s) {
        return cache.computeIfAbsent(s, k -> {
            if (k instanceof BooleanSetting bs) return new BooleanSettingComponent(bs);
            if (k instanceof SelectSetting  ss) return new SelectSettingComponent(ss);
            return null;
        });
    }

    private static void prepare(IComponent comp) {
        if (comp instanceof SelectSettingComponent select) {
            select.setLayoutWidth(CONTENT_W);
        }
    }

    private static float popupHeight(DraggableComponent c) {
        float h = HEADER_H + PAD;
        for (Setting s : c.getSettingsForGUI()) {
            IComponent comp = getOrCreate(s);
            if (comp == null) continue;
            prepare(comp);
            h += comp.getHeight();
        }
        return h + PAD;
    }

    private static float[] popupPos(DraggableComponent c) {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        float ph = popupHeight(c);
        float cr = c.getRenderX() + c.getWidth();

        float px = cr + 4 + WIDTH <= sw ? cr + 4 : c.getRenderX() - WIDTH - 4;
        float py = Math.max(4, Math.min(c.getRenderY(), sh - ph - 4));
        return new float[]{px, py};
    }

    public static void render(GuiGraphics g, DraggableComponent c, int mx, int my, float alpha) {
        List<Setting> settings = c.getSettingsForGUI();
        if (settings.isEmpty()) return;

        float[] p  = popupPos(c);
        float px   = p[0];
        float py   = p[1];
        float ph   = popupHeight(c);

        RenderUtils.fillRoundRect(px, py, WIDTH, ph, RADIUS, Colors.bgPrimary(alpha));
        RenderUtils.drawRoundRectOutline(px, py, WIDTH, ph, RADIUS, 1f, Colors.outline(alpha * 0.5f));

        var font = FontManager.get(10f);
        font.drawString(g, c.getName(),
                px + PAD,
                py + (HEADER_H - font.getHeight()) / 2f,
                Colors.textActive(alpha));

        float sepY = py + HEADER_H;
        RenderUtils.fillRoundRect(px + PAD, sepY, CONTENT_W, 0.5f, 0, Colors.outline(alpha * 0.4f));

        int outline = Colors.outline(alpha);
        int accent  = Colors.accent(alpha);
        int bg      = Colors.bgElement(alpha);
        int txtOff  = Colors.textInactive(alpha);
        int txtOn   = Colors.textActive(alpha);

        float sx = px + PAD;
        float sy = sepY + PAD;

        for (Setting s : settings) {
            IComponent comp = getOrCreate(s);
            if (comp == null) continue;

            prepare(comp);
            comp.render(g, sx, sy, CONTENT_W, mx, my, outline, accent, bg, txtOff, txtOn, alpha);
            sy += comp.getHeight();
        }
    }

    public static boolean mouseClicked(DraggableComponent c, double mx, double my, int btn) {
        List<Setting> settings = c.getSettingsForGUI();
        if (settings.isEmpty()) return false;

        float[] p  = popupPos(c);
        float   ph = popupHeight(c);

        if (!inside(mx, my, p[0], p[1], WIDTH, ph)) return false;

        float sx = p[0] + PAD;
        float sy = p[1] + HEADER_H + PAD;

        for (Setting s : settings) {
            IComponent comp = getOrCreate(s);
            if (comp == null) continue;

            prepare(comp);
            if (comp.handleClick(sx, sy, CONTENT_W, (int) mx, (int) my, btn)) return true;
            sy += comp.getHeight();
        }

        return true;
    }

    public static boolean isOver(DraggableComponent c, double mx, double my) {
        if (c.settings().isEmpty()) return false;
        float[] p = popupPos(c);
        return inside(mx, my, p[0], p[1], WIDTH, popupHeight(c));
    }

    private static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}