package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import ru.arixcompany.Arix;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.render.font.FontManager;
import static ru.arixcompany.utils.render.ColorUtil.argb;

import java.util.Collection;

public class BossBarDraggable extends DraggableComponent {

    private static final float FONT_SIZE   = 10f;
    private static final float MIN_BOX_W   = 182f;
    private static final float BOX_HEIGHT  = 20f;
    private static final float BAR_HEIGHT  = 4f;
    private static final float GAP         = 3f;
    private static final float PAD_X       = 6f;
    private static final float PAD_Y       = 3f;

    public BossBarDraggable() {
        super("BossBar", 0, 12, (int) MIN_BOX_W, (int) BOX_HEIGHT);
        setPinned(true);
    }

    @Override
    protected void updateVisibility() {
        this.visible = isCustomBossBarActive();
    }

    public static boolean isCustomBossBarActive() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return false;
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        return iface != null && iface.isState() && iface.elements.isSelected("Боссбар");
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        if (mc.gui == null) return;

        BossHealthOverlay overlay = mc.gui.getBossOverlay();
        if (overlay == null) return;

        Collection<LerpingBossEvent> events = overlay.events.values();
        if (events.isEmpty()) return;

        CustomFont font = FontManager.get(FONT_SIZE);

        float boxWidth = MIN_BOX_W;
        for (LerpingBossEvent event : events) {
            Component name = event.getName();
            float nameW = font.getComponentWidth(name) + PAD_X * 2f;
            if (nameW > boxWidth) boxWidth = nameW;
        }

        float drawX = graphics.guiWidth() / 2f - boxWidth / 2f;
        float drawY = ry;

        this.x = drawX;
        this.renderX = drawX;

        int rendered = 0;

        for (LerpingBossEvent event : events) {
            if (drawY >= graphics.guiHeight() / 3f) break;

            Component name = event.getName();
            float progress = Mth.clamp(event.getProgress(), 0f, 1f);

            int accent     = getBossColor(event.getColor(), alpha);
            int bgColor    = Colors.bgPrimary(alpha * 0.85f);
            int barBg      = argb(40,  40,  40,  alpha * 0.95f);
            int textColor  = argb(255, 255, 255, alpha);
            int notchColor = argb(0,   0,   0,   alpha * 0.5f);

            Interface.drawClientRect(drawX, drawY, boxWidth, BOX_HEIGHT, 4f, bgColor);

            float nameW = font.getComponentWidth(name);
            float textX = drawX + (boxWidth - nameW) / 2f;
            float textY = drawY + 1f;
            font.drawComponent(graphics, name, textX, textY, textColor);

            float barX  = drawX + PAD_X;
            float barY  = drawY + BOX_HEIGHT - PAD_Y - BAR_HEIGHT;
            float barW  = boxWidth - PAD_X * 2f;
            float fillW = barW * progress;

            Interface.drawClientRect(barX, barY, barW, BAR_HEIGHT, 2f, barBg);
            if (fillW > 0f) {
                Interface.drawClientRect(barX, barY, fillW, BAR_HEIGHT, 2f, accent);
            }

            drawNotches(event.getOverlay(), barX, barY, barW, BAR_HEIGHT, notchColor);

            drawY += BOX_HEIGHT + GAP;
            rendered++;
        }

        this.width  = (int) boxWidth;
        this.height = (int) (rendered * BOX_HEIGHT + Math.max(0, rendered - 1) * GAP);
    }

    private void drawNotches(BossEvent.BossBarOverlay overlay,
                             float x, float y, float w, float h, int color) {
        int count = switch (overlay) {
            case NOTCHED_6  -> 6;
            case NOTCHED_10 -> 10;
            case NOTCHED_12 -> 12;
            case NOTCHED_20 -> 20;
            default         -> 0;
        };
        if (count <= 1) return;
        for (int i = 1; i < count; i++) {
            float lx = x + w * i / count;
            Interface.drawClientRect(lx - 0.5f, y, 1f, h, 0f, color);
        }
    }

    private int getBossColor(BossEvent.BossBarColor color, float alpha) {
        return switch (color) {
            case PINK   -> argb(255, 105, 180, alpha);
            case BLUE   -> argb(90,  140, 255, alpha);
            case RED    -> argb(255,  70,  70, alpha);
            case GREEN  -> argb(80,  220, 120, alpha);
            case YELLOW -> argb(255, 210,  70, alpha);
            case PURPLE -> argb(170,  90, 255, alpha);
            default     -> argb(245, 245, 245, alpha);
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }
}