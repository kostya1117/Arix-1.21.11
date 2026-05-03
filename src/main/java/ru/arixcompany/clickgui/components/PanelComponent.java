package ru.arixcompany.clickgui.components;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import ru.arixcompany.Arix;
import ru.arixcompany.clickgui.Colors;
import ru.arixcompany.clickgui.Gui;
import ru.arixcompany.clickgui.components.module.ModuleComponent;
import ru.arixcompany.module.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import ru.arixcompany.module.Module;
import ru.arixcompany.utils.math.ScrollUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

@RequiredArgsConstructor
public final class PanelComponent implements IComponent {

    public static final float PANEL_WIDTH         = 126.0F;
    public static final float PANEL_HEADER_HEIGHT = 19.0F;
    public static final float PANEL_BODY_HEIGHT   = 220.0F;
    public static final float PANEL_PADDING       = 4.0F;

    private final ModuleComponent moduleComponent;

    @Override
    public void render(GuiGraphics guiGraphics,
                       int mouseX, int mouseY, float alpha) {
        if (Gui.categories == null || Arix.getInstance() == null) return;

        for (Category category : Gui.categories) {
            renderPanel(guiGraphics,mouseX, mouseY, alpha, category);
        }
    }

    private void renderPanel(GuiGraphics guiGraphics,int mouseX, int mouseY,
                             float mainAlpha, Category category) {
        float panelX = Gui.getCategoryPanelX(category);
        float panelY = Gui.getCategoryPanelY(category);

        boolean headerHovered = isHovered(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEADER_HEIGHT);
        Gui.animRun(Gui.getCategoryHoverAnimation(category), headerHovered ? 1.0 : 0.0);

        float openAnim  = Gui.getCategoryDropdownAnimation(category).getOutput();
        float alphaAnim = Gui.getCategoryDropdownAlphaAnimation(category).getOutput();
        float hoverAnim = Gui.getCategoryHoverAnimation(category).getOutput();
        float bodyH     = PANEL_BODY_HEIGHT * openAnim;
        float totalH    = PANEL_HEADER_HEIGHT + bodyH;

        RenderUtils.fillRoundRect(panelX, panelY, PANEL_WIDTH, totalH, 7, Colors.bgPrimary(mainAlpha));
        RenderUtils.drawRoundRectOutline(panelX, panelY, PANEL_WIDTH, totalH, 7,0.7F, Colors.outline(mainAlpha));

        RenderUtils.fillRoundRect(panelX, panelY, PANEL_WIDTH, PANEL_HEADER_HEIGHT,
                7, 7, 0, 0, Colors.bgSecondary(mainAlpha));;
        if (hoverAnim > 0.001F) {
            RenderUtils.fillRoundRect(panelX, panelY, PANEL_WIDTH, PANEL_HEADER_HEIGHT,
                    7, 7, 0, 0, Colors.hoverBg(mainAlpha, hoverAnim));
        }

//        renderer2D.text(FontRegistry.ICONS, panelX + 5, panelY + 9.8F, 12,
//                category.getIcon(), Colors.accent(mainAlpha));

        FontManager.get(11).drawString(guiGraphics,category.getName(), panelX + 17, panelY + 9.9F,Colors.textActive(mainAlpha));

        if (bodyH <= 0.001F) return;

        float contentH   = calcContentHeight(category);
        ScrollUtil scroll = Gui.getCategoryScrollUtil(category);
        scroll.setSpeed(6);
        scroll.setEnabled(isHovered(mouseX, mouseY, panelX, panelY + PANEL_HEADER_HEIGHT, PANEL_WIDTH, bodyH));
        scroll.update();
        scroll.setMax(
                Mth.clamp(contentH + PANEL_PADDING * 2, bodyH, 9999),
                Math.max(1, bodyH - 4)
        );

        RenderUtils.pushRoundedClipRect(
                panelX + 1, panelY + PANEL_HEADER_HEIGHT,
                PANEL_WIDTH - 2, Math.max(1, bodyH - 1),
                0, 0, 7, 7
        );

        float drawY = panelY + PANEL_HEADER_HEIGHT + PANEL_PADDING + scroll.getScroll();
        for (Module module : getDisplayModules(category)) {
            drawY = moduleComponent.renderModule(guiGraphics,mouseX, mouseY,
                    mainAlpha * alphaAnim,
                    panelX, drawY, module,
                    moduleComponent.getSettingComponentCache()
            );
        }

        RenderUtils.popClipRect();

        if (contentH > bodyH - 8) {
            scroll.render(
                    panelX + PANEL_WIDTH - 3,
                    panelY + PANEL_HEADER_HEIGHT + 2,
                    2, Math.max(1, bodyH - 4),
                    mainAlpha);
        }
    }

    @Override
    public boolean mouseScrolled(float mouseX, float mouseY, double scrollY) {
        if (Gui.categories == null) return false;

        for (Category category : Gui.categories) {
            float openAnim = Gui.getCategoryDropdownAnimation(category).getOutput();
            if (openAnim <= 0.001F) continue;

            float panelX = Gui.getCategoryPanelX(category);
            float panelY = Gui.getCategoryPanelY(category);
            float bodyH  = PANEL_BODY_HEIGHT * openAnim;

            if (isHovered(mouseX, mouseY,
                    panelX, panelY + PANEL_HEADER_HEIGHT,
                    PANEL_WIDTH, bodyH)) {
                Gui.getCategoryScrollUtil(category).handleScroll(scrollY);
                return true;
            }
        }
        return false;
    }

    public float calcContentHeight(Category category) {
        float total = PANEL_PADDING;
        for (Module module : getDisplayModules(category)) {
            total += ModuleComponent.MODULE_HEIGHT + ModuleComponent.MODULE_GAP;
            float sAnim  = Gui.getModuleSettingsAnimation(module).getOutput();
            float saAnim = Gui.getModuleSettingsAlphaAnimation(module).getOutput();
            if (sAnim > 0.001F || saAnim > 0.001F) {
                total += moduleComponent.calcSettingsHeight(module,
                        moduleComponent.getSettingComponentCache()) * sAnim + ModuleComponent.MODULE_GAP;
            }
        }
        return total + PANEL_PADDING;
    }

    public static List<Module> getDisplayModules(Category category) {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null)
            return new ArrayList<>();

        List<Module> list = Arix.getInstance().getModuleRepo().getModule(category);
        if (list == null) return new ArrayList<>();

        if (Gui.activeSearch && !Gui.searchText.isEmpty()) {
            String s = Gui.searchText.toLowerCase().trim();
            return list.stream()
                    .filter(m -> m.name.toLowerCase().contains(s))
                    .collect(Collectors.toList());
        }
        return list;
    }

    public static boolean isHovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}