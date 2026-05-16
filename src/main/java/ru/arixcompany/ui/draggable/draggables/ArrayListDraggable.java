package ru.arixcompany.ui.draggable.draggables;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.Arix;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.math.StringUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.List;
import java.util.stream.Collectors;

public class ArrayListDraggable extends DraggableComponent {
    private static final float FONT_SIZE    = 10f;
    private static final float LINE_HEIGHT  = 11f;
    private static final float PAD_X        = 4f;
    private static final float PAD_Y        = 1f;
    private static final float ACCENT_WIDTH = 1.5f;
    private static final float BIND_GAP     = 4f;

    public BooleanSetting lowerCase = new BooleanSetting("Маленькие буквы");

    public ArrayListDraggable() {
        super("ArrayList", 0, 2, 100, 10);
        setup(lowerCase);

        if (mc.getWindow() != null) {
            this.x = mc.getWindow().getGuiScaledWidth() - 120;
            this.renderX = this.x;
        }
    }

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        this.visible = iface != null && iface.isState() && iface.elements.isSelected("Аррай лист");
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta, float rx, float ry, float w, float h, float alpha) {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return;

        CustomFont font = FontManager.get(FONT_SIZE);

        List<Module> activeModules = Arix.getInstance().getModuleRepo().getModules().stream()
                .peek(this::updateModuleAnimation)
                .filter(m -> m.getAnimation().getOutput() > ALPHA_EPSILON)
                .collect(Collectors.toList());

        if (activeModules.isEmpty()) return;

        activeModules.sort((m1, m2) -> Float.compare(getModuleWidth(m2, font), getModuleWidth(m1, font)));

        float maxW = 0;
        float totalH = 0;
        for (Module m : activeModules) {
            maxW = Math.max(maxW, getModuleWidth(m, font) + PAD_X * 2);
            totalH += LINE_HEIGHT * (float) m.getAnimation().getOutput();
        }

        this.width = maxW + ACCENT_WIDTH;
        this.height = totalH;

        boolean right = (rx + this.width / 2f) > (mc.getWindow().getGuiScaledWidth() / 2f);

        float accentX = right ? rx + this.width - ACCENT_WIDTH : rx;
        Interface.drawClientRect(accentX, ry, ACCENT_WIDTH, totalH, 1, Colors.accent(alpha));

        float yOff = ry;
        for (Module module : activeModules) {
            float anim = (float) module.getAnimation().getOutput();
            float finalAlpha = alpha * anim;

            String name = lowerCase.isValue() ? module.getName().toLowerCase() : module.getName();

            boolean hasBind = module.getBind() != GLFW.GLFW_KEY_UNKNOWN;
            String bind = hasBind ? StringUtil.getBindName(module.getBind()) : "";

            float nameW = font.getWidth(name);
            float bindW = (hasBind && !bind.isEmpty()) ? font.getWidth(bind) : 0;
            float lineW = nameW + (bindW > 0 ? BIND_GAP + bindW : 0) + PAD_X * 2;

            float lineX = right ? rx + this.width - ACCENT_WIDTH - lineW : rx + ACCENT_WIDTH;

            Interface.drawClientRect(lineX, yOff, lineW, LINE_HEIGHT, 3, Colors.bgPrimary(finalAlpha * 0.9f));

            float textY = yOff + (LINE_HEIGHT - font.getHeight()) / 2f + PAD_Y;

            font.drawString(graphics, name, lineX + PAD_X, textY, Colors.textActive(finalAlpha));

            if (hasBind && !bind.isEmpty()) {
                float bindX = right ? lineX + lineW - PAD_X - bindW : lineX + PAD_X + nameW + BIND_GAP;
                font.drawString(graphics, bind, bindX, textY, Colors.textInactive(finalAlpha * 0.6f));
            }

            yOff += LINE_HEIGHT * anim;
        }
    }

    private float getModuleWidth(Module m, CustomFont font) {
        String name = lowerCase.isValue() ? m.getName().toLowerCase() : m.getName();
        float width = font.getWidth(name);

        if (m.getBind() != GLFW.GLFW_KEY_UNKNOWN) {
            String bind = StringUtil.getBindName(m.getBind());
            if (!bind.isEmpty()) {
                width += BIND_GAP + font.getWidth(bind);
            }
        }
        return width;
    }

    private void updateModuleAnimation(Module module) {
        Direction target = module.isState() ? Direction.FORWARDS : Direction.BACKWARDS;
        if (module.getAnimation().getDirection() != target) {
            module.getAnimation().setDirection(target);
        }
    }
}