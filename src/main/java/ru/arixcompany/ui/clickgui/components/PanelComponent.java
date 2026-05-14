package ru.arixcompany.ui.clickgui.components;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.Arix;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.ui.clickgui.components.module.ModuleComponent;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.utils.math.ScrollUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class PanelComponent implements IComponent {

    public static final float PANEL_WIDTH = 135.0F; // Чуть шире для современного вида
    public static final float PANEL_HEADER_HEIGHT = 24.0F;
    public static final float PANEL_MAX_BODY_HEIGHT = 280.0F;
    public static final float PANEL_PADDING = 5.0F;

    private final ModuleComponent moduleComponent;

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float alpha) {
        if (Gui.categories == null || Arix.getInstance() == null) return;
        for (Category category : Gui.categories) {
            renderPanel(guiGraphics, mouseX, mouseY, alpha, category);
        }
    }

    private void renderPanel(GuiGraphics guiGraphics, int mouseX, int mouseY, float mainAlpha, Category category) {
        float panelX = Gui.getCategoryPanelX(category);
        float panelY = Gui.getCategoryPanelY(category);

        boolean headerHovered = isHovered(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, PANEL_HEADER_HEIGHT);
        Gui.animRun(Gui.getCategoryHoverAnimation(category), headerHovered ? 1.0 : 0.0);

        float openAnim = Gui.getCategoryDropdownAnimation(category).getOutput();
        float hoverAnim = Gui.getCategoryHoverAnimation(category).getOutput();

        float contentH = calcContentHeight(category);
        float targetBodyH = Math.min(contentH, PANEL_MAX_BODY_HEIGHT);
        float bodyH = targetBodyH * openAnim;
        float totalH = PANEL_HEADER_HEIGHT + bodyH;

        // Главный фон панели (Body)
        RenderUtils.fillRoundRect(guiGraphics, panelX, panelY, PANEL_WIDTH, totalH, 6.0F, Colors.bgPrimary(mainAlpha));
        // Красивая обводка всей панели
        RenderUtils.drawRoundRectOutline(panelX, panelY, PANEL_WIDTH, totalH, 6.0F, 1.0F, Colors.outline(mainAlpha));

        // Фон шапки (Header)
        RenderUtils.pushClipRect(panelX, panelY, PANEL_WIDTH, PANEL_HEADER_HEIGHT);
        RenderUtils.fillRoundRect(guiGraphics, panelX, panelY, PANEL_WIDTH, PANEL_HEADER_HEIGHT, 0, Colors.bgSecondary(mainAlpha));
        if (hoverAnim > 0.001F) {
            RenderUtils.fillRoundRect(guiGraphics, panelX, panelY, PANEL_WIDTH, PANEL_HEADER_HEIGHT, 0, Colors.hoverBg(mainAlpha, hoverAnim));
        }
        RenderUtils.popClipRect();

        // Разделительная линия между шапкой и телом, если панель открыта
        if (openAnim > 0.01F) {
            RenderUtils.fillRoundRect(guiGraphics, panelX, panelY + PANEL_HEADER_HEIGHT - 1, PANEL_WIDTH, 1, 0, Colors.outline(mainAlpha * openAnim));
        }

        // Текст категории (по центру для стиля Dropdown)
        String catName = category.getName();
        float textW = FontManager.get(13).getWidth(catName);
        FontManager.get(13).drawString(guiGraphics, catName, panelX + (PANEL_WIDTH / 2) - (textW / 2), panelY + 8.0F, Colors.textActive(mainAlpha));

        if (bodyH <= 0.001F) return;

        // Скроллинг
        ScrollUtil scroll = Gui.getCategoryScrollUtil(category);
        scroll.setSpeed(8);
        scroll.setEnabled(isHovered(mouseX, mouseY, panelX, panelY + PANEL_HEADER_HEIGHT, PANEL_WIDTH, bodyH));
        scroll.update();
        // max = contentHeight - visibleHeight (положительное значение)
        scroll.setMax(contentH - targetBodyH);

        // Используем -scroll для смещения контента вверх
        float scrollValue = -scroll.getScroll();
        float drawY = panelY + PANEL_HEADER_HEIGHT + PANEL_PADDING + scrollValue;

        // Применяем ОБА clip'а:
        // - RenderUtils.pushClipRect для RoundRectShader (GL11 напрямую)
        // - guiGraphics.enableScissor для текста (Minecraft pipeline)
        float clipX = panelX;
        float clipY = panelY + PANEL_HEADER_HEIGHT;
        float clipW = PANEL_WIDTH;
        float clipH = bodyH;

        RenderUtils.pushClipRect(clipX, clipY, clipW, clipH);
        guiGraphics.enableScissor((int) clipX, (int) clipY, (int) (clipX + clipW), (int) (clipY + clipH));

        for (Module module : getDisplayModules(category)) {
            drawY = moduleComponent.renderModule(
                    guiGraphics, mouseX, mouseY, mainAlpha * openAnim,
                    panelX, drawY, module, category, moduleComponent.getSettingComponentCache()
            );
        }

        guiGraphics.disableScissor();
        RenderUtils.popClipRect();

        // Полоса прокрутки (Scrollbar)
        if (contentH > targetBodyH) {
            float scrollProgress = scroll.getScroll() / scroll.getMax();
            float scrollBarH = Math.max(15.0F, (targetBodyH / contentH) * targetBodyH);
            float scrollBarY = panelY + PANEL_HEADER_HEIGHT + 2 + ((targetBodyH - scrollBarH - 4) * scrollProgress);

            RenderUtils.fillRoundRect(guiGraphics, panelX + PANEL_WIDTH - 4, scrollBarY, 2, scrollBarH, 1, Colors.accent(mainAlpha * 0.7f));
        }
    }

    public boolean mouseClickedCategory(int mouseX, int mouseY, int button, Category category) {
        float openAnim = Gui.getCategoryDropdownAnimation(category).getOutput();
        if (openAnim <= 0.001F) return false;

        float panelX = Gui.getCategoryPanelX(category);
        float panelY = Gui.getCategoryPanelY(category);
        float bodyH = Math.min(calcContentHeight(category), PANEL_MAX_BODY_HEIGHT) * openAnim;

        if (!isHovered(mouseX, mouseY, panelX, panelY + PANEL_HEADER_HEIGHT, PANEL_WIDTH, bodyH)) return false;

        // Делегируем клики в модули
        return moduleComponent.processCategoryClicks(category, panelX, panelY, mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(float mouseX, float mouseY, double scrollY) {
        if (Gui.categories == null) return false;
        for (Category category : Gui.categories) {
            float openAnim = Gui.getCategoryDropdownAnimation(category).getOutput();
            if (openAnim <= 0.001F) continue;
            
            float panelX = Gui.getCategoryPanelX(category);
            float panelY = Gui.getCategoryPanelY(category);
            float contentH = calcContentHeight(category);
            float targetBodyH = Math.min(contentH, PANEL_MAX_BODY_HEIGHT);
            float bodyH = targetBodyH * openAnim;

            // Проверяем что мышь над панелью
            if (isHovered(mouseX, mouseY, panelX, panelY + PANEL_HEADER_HEIGHT, PANEL_WIDTH, bodyH)) {
                // Проверяем что контент больше чем видимая область
                if (contentH > targetBodyH) {
                    ScrollUtil scroll = Gui.getCategoryScrollUtil(category);
                    scroll.handleScroll(scrollY);
                    return true;
                }
            }
        }
        return false;
    }

    public float calcContentHeight(Category category) {
        float total = PANEL_PADDING;
        for (Module module : getDisplayModules(category)) {
            total += ModuleComponent.MODULE_HEIGHT + ModuleComponent.MODULE_GAP;
            float sAnim = Gui.getModuleSettingsAnimation(module).getOutput();
            if (sAnim > 0.001F) {
                total += moduleComponent.calcSettingsHeight(module, moduleComponent.getSettingComponentCache()) * sAnim + ModuleComponent.MODULE_GAP;
            }
        }
        return total + PANEL_PADDING;
    }

    public static List<Module> getDisplayModules(Category category) {
        if (Arix.getInstance() == null || Arix.getInstance().getModuleRepo() == null) return new ArrayList<>();
        List<Module> list = Arix.getInstance().getModuleRepo().getModule(category);
        if (list == null) return new ArrayList<>();

        String search = Gui.searchText.trim().toLowerCase();
        if (search.isEmpty()) return list;

        List<Module> filtered = new ArrayList<>();
        for (Module m : list) {
            if (m.name.toLowerCase().contains(search)) {
                filtered.add(m);
            }
        }
        return filtered;
    }

    public static boolean isHovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}