package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import ru.arixcompany.Arix;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PotionsDraggable extends DraggableComponent {
    private static final float FONT_SIZE = 10f;
    private static final float HEADER_HEIGHT = 15f;
    private static final float LINE_HEIGHT = 13f;
    private static final float PADDING = 6f;
    private static final float ICON_SIZE = 10f;
    private static final float MIN_WIDTH = 110f;

    public PotionsDraggable() {
        super("Potions", 10, 50, 110, 20);
    }

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("Зелья");
    }

    public static boolean isCustomPotionsActive() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return false;
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        return iface != null && iface.isState() && iface.elements.isSelected("Зелья");
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta, float rx, float ry, float w, float h, float alpha) {
        if (mc.player == null) return;

        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty() && !isDragging()) return;

        CustomFont font = FontManager.get(FONT_SIZE);

        List<MobEffectInstance> sortedEffects = effects.stream()
                .sorted(Comparator.comparingInt(MobEffectInstance::getDuration).reversed())
                .collect(Collectors.toList());

        float maxContentWidth = MIN_WIDTH;
        for (MobEffectInstance instance : sortedEffects) {
            String name = getEffectName(instance);
            String duration = getDurationString(instance);

            float widthNeeded = PADDING + ICON_SIZE + 4 + font.getWidth(name) + 20 + font.getWidth(duration) + PADDING;
            maxContentWidth = Math.max(maxContentWidth, widthNeeded);
        }

        this.width = maxContentWidth;
        this.height = HEADER_HEIGHT + (sortedEffects.size() * LINE_HEIGHT) + (sortedEffects.isEmpty() ? 0 : 3);

        Interface.drawClientRect(rx, ry, width, height, 4, Colors.bgPrimary(alpha * 0.85f));

        font.drawString(graphics, "Зелья", rx + PADDING, ry + (HEADER_HEIGHT - font.getHeight()) / 2f + 1, Colors.textActive(alpha));

        float yOff = ry + HEADER_HEIGHT;
        for (MobEffectInstance instance : sortedEffects) {
            Holder<MobEffect> holder = instance.getEffect();

            Identifier effectSprite = Gui.getMobEffectSprite(holder);

            int iconX = (int) (rx + PADDING);
            int iconY = (int) (yOff + (LINE_HEIGHT - ICON_SIZE) / 2f);

            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, effectSprite, iconX, iconY, (int)ICON_SIZE, (int)ICON_SIZE);

            String name = getEffectName(instance);
            font.drawString(graphics, name, rx + PADDING + ICON_SIZE + 4, yOff + (LINE_HEIGHT - font.getHeight()) / 2f, Colors.textActive(alpha));

            String duration = getDurationString(instance);
            float durW = font.getWidth(duration);

            font.drawString(graphics, duration, rx + width - PADDING - durW, yOff + (LINE_HEIGHT - font.getHeight()) / 2f, Colors.textInactive(alpha * 0.8f));

            yOff += LINE_HEIGHT;
        }
    }

    private String getEffectName(MobEffectInstance instance) {
        String name = instance.getEffect().value().getDisplayName().getString();

        if (instance.getAmplifier() > 0) {
            name += " " + (instance.getAmplifier() + 1);
        }
        return name;
    }

    private String getDurationString(MobEffectInstance instance) {
        if (instance.isInfiniteDuration()) return "∞";

        int ticks = instance.getDuration();
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds %= 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}