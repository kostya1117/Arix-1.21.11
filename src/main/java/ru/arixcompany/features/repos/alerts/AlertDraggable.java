package ru.arixcompany.features.repos.alerts;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

public class AlertDraggable extends DraggableComponent {

    private static final float WIDTH = 120.0f;
    private static final float HEIGHT = 16.0f;
    private static final float SPACING = 3.0f;
    private static final float RADIUS = 8.0f;

    public AlertDraggable() {
        super("Уведомления", 10, 10, WIDTH, HEIGHT);
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                   float rx, float ry, float w, float h, float alpha) {
        AlertRepo.update();
        float currentY = ry;

        if (mc.screen instanceof ChatScreen) {
            AlertType[] types = AlertType.values();
            AlertType testType = types[(int)((System.currentTimeMillis() / 1000) % types.length)];
            drawNotification(graphics, rx, currentY, "Пример уведомления", testType, alpha);
            currentY += HEIGHT + SPACING;
        }

        for (AlertRepo.NotificationEntry entry : AlertRepo.getNotifications()) {
            float anim = entry.animation.getOutput();
            float xOffset = (1.0f - anim) * 8f;

            drawNotification(graphics, rx + xOffset, currentY, entry.text, entry.type, anim * alpha);
            currentY += (HEIGHT + SPACING) * anim;
        }

        this.height = Math.max(HEIGHT, currentY - ry);
    }

    private void drawNotification(GuiGraphics graphics, float x, float y, String text,
                                  AlertType type, float alpha) {

        RenderUtils.fillRoundRect(x, y, WIDTH, HEIGHT, RADIUS, Colors.bgPrimary(alpha * 0.8f));

        float iconSize = 8.0f;
        float pad = (HEIGHT - iconSize) / 2f;

        int iconAlpha = (int) (alpha * 255);
        int tintedColor = (type.getColor() & 0x00FFFFFF) | (iconAlpha << 24);

        FontManager.get(FontManager.Fonts.ICONS,12).drawString(graphics,type.getIconName(), x + pad + 1, y + pad,tintedColor);

        float textX = x + pad + iconSize + 4f;
        FontManager.get(10).drawString(graphics, text, textX, y + (HEIGHT - 9) / 2f - 1, Colors.textActive(alpha));
    }

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("Уведомления");
    }
}