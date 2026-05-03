package ru.arixcompany.clickgui.components.module.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.module.setting.implement.MultiSelectSetting;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class ListSettingComponent implements IComponent {

    @Getter private final MultiSelectSetting setting;
    private final Map<String, Float> itemAnimations = new HashMap<>();

    private static final float SPACING = 3.0F, ITEM_H = 10.0F, PADDING = 4.0F, CALC_W = 105.47F;

    /**
     * Безопасно получаем список — никогда не вернёт null.
     */
    private List<String> safeList() {
        List<String> list = setting.getList();
        return list != null ? list : Collections.emptyList();
    }

    @Override
    public float getHeight() {
        List<String> list = safeList();
        if (list.isEmpty()) return 0;

        float curX = 0, curY = 10.0F;
        for (String name : list) {
            float itemW = FontManager.get(12).getWidth(name) + PADDING * 2;
            if (curX + itemW > CALC_W) {
                curX = 0;
                curY += ITEM_H + SPACING;
            }
            curX += itemW + SPACING;
        }
        return curY + ITEM_H;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {

        List<String> list = safeList();
        if (list.isEmpty()) return;

        FontManager.get(13).drawString(guiGraphics, setting.getName(), x, y + 7.0F, textInactive);

        float curX = x, curY = y + 10.0F;
        for (String name : list) {
            float nameW = FontManager.get(12).getWidth(name);
            float itemW  = nameW + PADDING * 2;
            if (curX + itemW > x + width) {
                curX = x;
                curY += ITEM_H + SPACING;
            }

            RenderUtils.drawRoundRectOutline(curX, curY, itemW, ITEM_H, 3.0F, 0.1F, outlineColor);
            RenderUtils.fillRoundRect(curX, curY, itemW, ITEM_H, 3.0F, bgColor);

            String animKey = setting.getName() + "_" + name;
            float anim = itemAnimations.getOrDefault(animKey, setting.isSelected(name) ? 1.0F : 0.0F);
            anim = MathUtils.fast(anim, setting.isSelected(name) ? 1.0F : 0.0F, 10.0F);
            itemAnimations.put(animKey, anim);

            FontManager.get(13).drawString(
                    guiGraphics, name,
                    curX + PADDING, curY + 3.0F - 1.0F + 5.0F,
                    ColorUtil.overCol(textInactive, accentColor, anim)
            );
            curX += itemW + SPACING;
        }
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        List<String> list = safeList();
        if (list.isEmpty()) return false;

        float curX = x, curY = y + 10.0F;
        for (String name : list) {
            float nameW = FontManager.get(12).getWidth(name);
            float itemW  = nameW + PADDING * 2;
            if (curX + itemW > x + width) {
                curX = x;
                curY += ITEM_H + SPACING;
            }
            if (hovered(mouseX, mouseY, curX, curY, itemW, ITEM_H)) {
                setting.toggleSelected(name);
                return true;
            }
            curX += itemW + SPACING;
        }
        return false;
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}