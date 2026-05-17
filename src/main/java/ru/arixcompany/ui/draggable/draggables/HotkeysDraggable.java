package ru.arixcompany.ui.draggable.draggables;

import lombok.AllArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.setting.implement.GroupSetting;
import ru.arixcompany.ui.draggable.DraggableComponent;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.render.Interface;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.math.StringUtil;
import ru.arixcompany.utils.render.font.CustomFont;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.ArrayList;
import java.util.List;

public class HotkeysDraggable extends DraggableComponent {
    private static final float FONT_SIZE     = 10f;
    private static final float HEADER_HEIGHT = 14f;
    private static final float LINE_HEIGHT   = 12f;
    private static final float PADDING       = 5f;
    private static final float MIN_WIDTH     = 90f;

    public HotkeysDraggable() {
        super("Hotkeys", 100, 100, 100, 20);
    }

    @Override
    protected void updateVisibility() {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) {
            this.visible = false;
            return;
        }
        Interface iface = Arix.getInstance().getModuleRepo().getModule(Interface.class);
        boolean ifaceEnabled = iface != null && iface.isState() && iface.elements.isSelected("Кейбинды");

        this.visible = ifaceEnabled && !getHotkeyList().isEmpty();
    }

    private List<HotkeyEntry> getHotkeyList() {
        List<HotkeyEntry> list = new ArrayList<>();
        if (Arix.getInstance() == null) return list;

        for (Module m : Arix.getInstance().getModuleRepo().getModules()) {
            this.updateModuleAnimation(m);
            float anim = m.getAnimation().getOutput();

            if (anim <= ALPHA_EPSILON) continue;

            if (m.getBind() != GLFW.GLFW_KEY_UNKNOWN) {
                list.add(new HotkeyEntry(m.getName(), StringUtil.getBindName(m.getBind()), anim));
            }

            if (m.isState() && !m.isForced()) {
                for (Setting s : m.getSettingsForGUI()) {
                    if (s instanceof BindSetting bs) {
                        if (bs.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
                            list.add(new HotkeyEntry(s.getName(), StringUtil.getBindName(bs.getKey()), anim));
                        }
                    }
                    if (s instanceof GroupSetting g) {
                        for (Setting s1 : g.getChildren()) {
                            if (s1 instanceof BindSetting bs) {
                                if (bs.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
                                    list.add(new HotkeyEntry(bs.getName(), StringUtil.getBindName(bs.getKey()), anim));
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    @Override
    protected void renderDraggable(GuiGraphics graphics, int mouseX, int mouseY, float delta, float rx, float ry, float w, float h, float alpha) {
        CustomFont font = FontManager.get(FONT_SIZE);
        List<HotkeyEntry> entries = getHotkeyList();

        if (entries.isEmpty()) return;

        float maxW = MIN_WIDTH;
        float totalContentHeight = 0;

        for (HotkeyEntry entry : entries) {
            float nameW = font.getWidth(entry.name);
            float bindW = font.getWidth(entry.bind);
            maxW = Math.max(maxW, nameW + bindW + PADDING * 4 + 4);

            totalContentHeight += LINE_HEIGHT * entry.anim;
        }

        this.width = maxW;
        this.height = HEADER_HEIGHT + totalContentHeight + 2;

        Interface.drawClientRect(rx, ry, width, height, 5, Colors.bgPrimary(alpha * 0.85f));
        font.drawString(graphics, "Кейбинды", rx + PADDING, ry + (HEADER_HEIGHT - font.getHeight()) / 2f + 1, Colors.textActive(alpha));

        float yOff = ry + HEADER_HEIGHT;

        for (HotkeyEntry entry : entries) {
            float anim = entry.anim;
            float finalAlpha = alpha * anim;

            if (anim > 0.15f) {
                float textY = yOff + (LINE_HEIGHT - font.getHeight()) / 2f;

                font.drawString(graphics, entry.name, rx + PADDING, textY, Colors.textActive(finalAlpha));

                String bText = entry.bind;
                float bWidth = font.getWidth(bText);
                float rectW = bWidth + 4;
                float rectH = font.getHeight() + 1;
                float rectX = rx + width - PADDING - rectW;
                float rectY = textY - 0.5f;

                Interface.drawClientRect(rectX, rectY, rectW, rectH, 2, Colors.accent(finalAlpha * 0.85f));

                font.drawString(graphics, bText, rectX + 2, textY, Colors.textActive(finalAlpha));
            }

            yOff += LINE_HEIGHT * anim;
        }
    }

    private void updateModuleAnimation(Module module) {
        Direction target = module.isState() ? Direction.FORWARDS : Direction.BACKWARDS;
        if (module.getAnimation().getDirection() != target) {
            module.getAnimation().setDirection(target);
        }
    }

    @AllArgsConstructor
    private static class HotkeyEntry {
        String name;
        String bind;
        float anim;
    }
}