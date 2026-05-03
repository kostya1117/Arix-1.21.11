package ru.arixcompany.clickgui.components.module.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.module.setting.implement.SelectSetting;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public final class SelectSettingComponent implements IComponent {

    @Getter private final SelectSetting setting;
    private final Map<String, Float> modeAnimations = new HashMap<>();

    private static final float MODE_SPACING = 2.0F, MODE_HEIGHT = 10.075F;
    private static final float PADDING = 3.0F, VERT_SPACING = -2.0F;

    @Override
    public float getHeight() {
        return computeLayout(105.47F).totalY + MODE_HEIGHT + 12.0F;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {
        FontManager.get(13).drawString(guiGraphics,setting.getName(),x, y + 7.0F,textInactive);

        LayoutResult layout = computeLayout(width);
        float containerY = y + 10.0F;
        float containerH = layout.totalY + MODE_HEIGHT;

        RenderUtils.drawRoundRectOutline(x, containerY, width, containerH, 3.0F,0.1F, outlineColor);
        RenderUtils.fillRoundRect(x, containerY, width, containerH, 3.0F, bgColor);

        float curX = PADDING, curY = 1.5F;
        for (String mode : setting.getList()) {
            float modeW = FontManager.get(12).getWidth(mode) + PADDING * 2.0F;
            if (curX + modeW > width && curX > PADDING) { curX = PADDING; curY += MODE_HEIGHT + VERT_SPACING; }

            float anim = modeAnimations.getOrDefault(mode, 0.0F);
            anim = MathUtils.fast(anim, mode.equals(setting.getSelected()) ? 1.0F : 0.0F, 10.0F);
            modeAnimations.put(mode, anim);

            FontManager.get(12).drawString(guiGraphics,mode, x + curX, containerY + curY + 5.5F, ColorUtil.overCol(textInactive, accentColor, anim));
            curX += modeW + MODE_SPACING;
        }
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        LayoutResult layout = computeLayout(width);
        float containerY = y + 10.0F;
        float containerH = layout.totalY + MODE_HEIGHT;
        if (!hovered(mouseX, mouseY, x, containerY, width, containerH)) return false;

        float curX = PADDING, curY = 1.5F;
        for (String mode : setting.getList()) {
            float modeW = FontManager.get(12).getWidth(mode) + + PADDING * 2.0F;
            if (curX + modeW > width && curX > PADDING) { curX = PADDING; curY += MODE_HEIGHT + VERT_SPACING; }
            if (hovered(mouseX, mouseY, x + curX, containerY + curY, modeW, MODE_HEIGHT)) {
                setting.setSelected(mode);
                return true;
            }
            curX += modeW + MODE_SPACING;
        }
        return false;
    }

    private LayoutResult computeLayout(float width) {
        float curX = PADDING, curY = 0.0F;
        for (String mode : setting.getList()) {
            float modeW = FontManager.get(12).getWidth(mode) + + PADDING * 2.0F;
            if (curX + modeW > width && curX > PADDING) { curX = PADDING; curY += MODE_HEIGHT + VERT_SPACING; }
            curX += modeW + MODE_SPACING;
        }
        return new LayoutResult(curY);
    }

    private record LayoutResult(float totalY) {}

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}