package ru.arixcompany.features.draggable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.ui.clickgui.Colors;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.ui.clickgui.components.module.settings.BooleanSettingComponent;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.*;

public final class DraggableSettingsRenderer {

    private DraggableSettingsRenderer() {}

    private static final float WIDTH   = 130f, PAD = 6f, RADIUS = 4f, HEADER_H = 16f;
    private static final Map<Setting, IComponent> cache = new WeakHashMap<>();

    private static IComponent getOrCreate(Setting s) {
        return cache.computeIfAbsent(s, k -> k instanceof BooleanSetting bs ? new BooleanSettingComponent(bs) : null);
    }

    private static float popupHeight(DraggableComponent c) {
        float h = HEADER_H + PAD;
        for (Setting s : c.getSettingsForGUI()) {
            IComponent comp = getOrCreate(s);
            if (comp != null) h += comp.getHeight();
        }
        return h + PAD;
    }

    private static float[] popupPos(DraggableComponent c) {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
        float ph = popupHeight(c);
        float cr = c.getRenderX() + c.getWidth();

        float px = cr + 4 + WIDTH <= sw ? cr + 4 : c.getRenderX() - WIDTH - 4;
        float py = Math.max(4, Math.min(c.getRenderY(), sh - ph - 4));
        return new float[]{px, py};
    }

    public static void render(GuiGraphics g, DraggableComponent c, int mx, int my, float alpha) {
        List<Setting> settings = c.getSettingsForGUI();
        if (settings.isEmpty()) return;

        float[] p = popupPos(c);
        float px = p[0], py = p[1], ph = popupHeight(c);

        RenderUtils.fillRoundRect(px, py, WIDTH, ph, RADIUS, Colors.bgPrimary(alpha));
        RenderUtils.drawRoundRectOutline(px, py, WIDTH, ph, RADIUS, 1f, Colors.outline(alpha * 0.5f));

        var font = FontManager.get(9f);
        font.drawString(g, c.getName(), px + PAD, py + (HEADER_H - font.getHeight()) / 2f, Colors.textActive(alpha));

        float sepY = py + HEADER_H;
        RenderUtils.fillRoundRect(px + PAD, sepY, WIDTH - PAD * 2, 0.5f, 0, Colors.outline(alpha * 0.4f));

        float sy = sepY + PAD, sx = px + PAD, sw = WIDTH - PAD * 2;
        int outline = Colors.outline(alpha), accent = Colors.accent(alpha);
        int bg = Colors.bgElement(alpha), txtOff = Colors.textInactive(alpha), txtOn = Colors.textActive(alpha);

        for (Setting s : settings) {
            IComponent comp = getOrCreate(s);
            if (comp == null) continue;
            comp.render(g, sx, sy, sw, mx, my, outline, accent, bg, txtOff, txtOn, alpha);
            sy += comp.getHeight();
        }
    }

    public static boolean mouseClicked(DraggableComponent c, double mx, double my, int btn) {
        List<Setting> settings = c.getSettingsForGUI();
        if (settings.isEmpty()) return false;

        float[] p = popupPos(c);
        if (!inside(mx, my, p[0], p[1], WIDTH, popupHeight(c))) return false;

        float sy = p[1] + HEADER_H + PAD, sx = p[0] + PAD, sw = WIDTH - PAD * 2;
        for (Setting s : settings) {
            IComponent comp = getOrCreate(s);
            if (comp == null) continue;
            if (comp.handleClick(sx, sy, sw, (int) mx, (int) my, btn)) return true;
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