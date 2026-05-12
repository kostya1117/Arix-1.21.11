package ru.arixcompany.ui.clickgui.components.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.ui.clickgui.components.PanelComponent;
import ru.arixcompany.ui.clickgui.components.module.settings.ColorSettingComponent;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.setting.Setting;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ColorSetting;
import ru.arixcompany.utils.math.StringUtil;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class ModuleComponent implements IComponent {

    public static final float MODULE_HEIGHT = 20.0F;
    public static final float MODULE_GAP = 3.0F;

    private final SettingComponentFactory settingFactory;

    @Getter
    private final Map<Setting, IComponent> settingComponentCache = new HashMap<>();

    public boolean processCategoryClicks(Category category, float panelX, float panelY, int mouseX, int mouseY, int button) {
        List<Module> modules = PanelComponent.getDisplayModules(category);
        float drawY = panelY + PanelComponent.PANEL_HEADER_HEIGHT + PanelComponent.PANEL_PADDING - Gui.getCategoryScrollUtil(category).getScroll();

        for (Module module : modules) {
            float moduleY = drawY;
            float settingsAnim = Gui.getModuleSettingsAnimation(module).getOutput();

            if (settingsAnim > 0.98F && Gui.openSettingsModules.contains(module)) {
                float sX = getSettingsX(panelX);
                float sY = getSettingsStartY(moduleY);
                float sW = getSettingsWidth();

                for (Setting setting : module.getSettingsForGUI()) {
                    IComponent comp = getOrCreate(setting);
                    if (comp.handleClick(sX, sY, sW, mouseX, mouseY, button)) return true;
                    sY += comp.getHeight() + 1.0F;
                }
            }

            float moduleX = getModuleX(panelX);
            float moduleW = getModuleWidth();
            if (PanelComponent.isHovered(mouseX, mouseY, moduleX, moduleY, moduleW, MODULE_HEIGHT)) {
                return handleModuleClick(category, module, button);
            }
            drawY = getNextModuleY(module, moduleY);
        }
        return false;
    }

    private boolean handleModuleClick(Category category, Module module, int button) {
        if (button == 0) {
            module.toggle();
            return true;
        }
        if (button == 1) {
            if (module.getSettingsForGUI().isEmpty()) return false;
            if (Gui.openSettingsModules.contains(module)) {
                Gui.openSettingsModules.remove(module);
                Gui.animRun(Gui.getModuleSettingsAnimation(module), 0.0);
            } else {
                Gui.openSettingsModules.add(module);
                Gui.animRun(Gui.getModuleSettingsAnimation(module), 1.0);
            }
            return true;
        }
        if (button == 2) {
            module.binding = !module.binding;
            Gui.activeModuleBind = module.binding ? module : null;
            return true;
        }
        return false;
    }

    public float renderModule(GuiGraphics guiGraphics, int mouseX, int mouseY, float mainAlpha, float panelX, float moduleY, Module module, Category category, Map<Setting, IComponent> cache) {
        float moduleX = getModuleX(panelX);
        float moduleW = getModuleWidth();

        Gui.animRun(module.animation, module.state ? 1.0 : 0.0);
        float toggleAnim = module.animation.getOutput();

        boolean hovered = PanelComponent.isHovered(mouseX, mouseY, moduleX, moduleY, moduleW, MODULE_HEIGHT);
        Gui.animRun(Gui.getModuleHoverAnimation(module), hovered ? 1.0 : 0.0);
        float hoverAnim = Gui.getModuleHoverAnimation(module).getOutput();

        int baseBg = Colors.bgElement(mainAlpha);
        int accentBg = ColorUtil.multAlpha(Colors.accent(mainAlpha), 0.15F);
        int moduleBg = ColorUtil.overCol(baseBg, accentBg, toggleAnim);
        RenderUtils.fillRoundRect(guiGraphics, moduleX, moduleY, moduleW, MODULE_HEIGHT, 4.0F, moduleBg);
        if (hoverAnim > 0.001F) RenderUtils.fillRoundRect(guiGraphics, moduleX, moduleY, moduleW, MODULE_HEIGHT, 4.0F, Colors.hoverBg(mainAlpha, hoverAnim));

        float textY = moduleY + (MODULE_HEIGHT / 2.0F) - (FontManager.get(13).getHeight() / 2.0F);
        FontManager.get(13).drawString(guiGraphics, module.name, moduleX + 6, textY, Colors.textActive(mainAlpha));

        if (module.bind != -1 || module.binding) {
            String keyText = module.binding ? "..." : StringUtil.getBindName(module.bind);
            FontManager.get(10).drawString(guiGraphics, "[" + keyText + "]", moduleX + 6 + FontManager.get(13).getWidth(module.name) + 4, textY + 1, Colors.textInactive(mainAlpha));
        }

        renderModernToggle(guiGraphics, moduleX + moduleW - 20, moduleY + (MODULE_HEIGHT / 2) - 4, toggleAnim, mainAlpha);

        return renderSettingsBox(guiGraphics, mouseX, mouseY, mainAlpha, panelX, moduleY, module, cache);
    }

    private void renderModernToggle(GuiGraphics guiGraphics, float x, float y, float anim, float alpha) {
        float w = 16, h = 8;

        RenderUtils.fillRoundRect(guiGraphics, x, y, w, h, 4, ColorUtil.overCol(Colors.bgSecondary(alpha), Colors.accent(alpha), anim));

        float thumbX = x + 1 + (w - 10) * anim;
        RenderUtils.fillRoundRect(guiGraphics, thumbX, y + 1, 6, 6, 3, Colors.textActive(alpha));
    }

    private float renderSettingsBox(GuiGraphics guiGraphics, int mouseX, int mouseY, float mainAlpha, float panelX, float moduleY, Module module, Map<Setting, IComponent> cache) {
        float nextY = moduleY + MODULE_HEIGHT + MODULE_GAP;
        float settingsAnim = Gui.getModuleSettingsAnimation(module).getOutput();

        if (module.getSettingsForGUI().isEmpty() || settingsAnim <= 0.001F) return nextY;

        float fullH = calcSettingsHeight(module, cache);
        float visH = fullH * settingsAnim;
        float boxX = getSettingsBoxX(panelX);
        float boxY = getSettingsBoxY(moduleY);
        float boxW = getSettingsBoxWidth();

        float effectiveAlpha = mainAlpha * settingsAnim;

        RenderUtils.fillRoundRect(guiGraphics, boxX, boxY, boxW, visH, 4.0F, Colors.bgSecondary(effectiveAlpha));
        RenderUtils.drawRoundRectOutline(boxX, boxY, boxW, visH, 4.0F, 0.5F, Colors.outline(effectiveAlpha));

        float sY = getSettingsStartY(moduleY);
        float sX = getSettingsX(panelX);
        float sW = getSettingsWidth();

        for (Setting setting : module.getSettingsForGUI()) {
            IComponent comp = getOrCreate(setting);
            comp.render(guiGraphics, sX, sY, sW, mouseX, mouseY,
                    Colors.outline(effectiveAlpha), Colors.accent(effectiveAlpha), Colors.bgPrimary(effectiveAlpha),
                    Colors.textInactive(effectiveAlpha), Colors.textActive(effectiveAlpha), effectiveAlpha);
            sY += comp.getHeight() + 1.0F;
        }

        return boxY + visH + MODULE_GAP;
    }

    @Override
    public boolean mouseDragged(int mouseX, int mouseY, int button, double dragX, double dragY) {
        for (IComponent comp : settingComponentCache.values()) {
            if (comp.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        return false;
    }

    @Override
    public void mouseReleased() {
        settingComponentCache.values().forEach(IComponent::mouseReleased);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Gui.activeModuleBind != null) {
            if (keyCode == 256) { // ESC
                Gui.activeModuleBind.binding = false;
                Gui.activeModuleBind = null;
            } else if (keyCode == 261) { // Delete
                Gui.activeModuleBind.bind = -1;
                Gui.activeModuleBind.binding = false;
                Gui.activeModuleBind = null;
            } else {
                Gui.activeModuleBind.bind = keyCode;
                Gui.activeModuleBind.binding = false;
                Gui.activeModuleBind = null;
            }
            return true;
        }

        for (IComponent comp : settingComponentCache.values()) {
            if (comp.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        for (IComponent comp : settingComponentCache.values()) {
            if (comp.charTyped(codePoint, modifiers)) return true;
        }
        return false;
    }

    public  float[] findColorPickerPosition(ColorSetting colorSetting) {
        if (colorSetting == null || Gui.categories == null) return null;
        for (Category category : Gui.categories) {
            float openAnim = Gui.getCategoryDropdownAnimation(category).getOutput();
            if (openAnim <= 0.001F) continue;

            float panelX = Gui.getCategoryPanelX(category);
            float panelY = Gui.getCategoryPanelY(category);
            float drawY = panelY + PanelComponent.PANEL_HEADER_HEIGHT + PanelComponent.PANEL_PADDING - Gui.getCategoryScrollUtil(category).getScroll();

            for (Module module : PanelComponent.getDisplayModules(category)) {
                float moduleY = drawY;
                if (Gui.openSettingsModules.contains(module)) {
                    float sX = getSettingsX(panelX);
                    float sY = getSettingsStartY(moduleY);
                    for (Setting setting : module.getSettingsForGUI()) {
                        if (setting == colorSetting) {
                            return new float[]{ sX + getSettingsWidth() - 15, sY - 5 };
                        }
                        sY += getOrCreate(setting).getHeight() + 1.0F;
                    }
                }
                drawY = getNextModuleY(module, moduleY);
            }
        }
        return null;
    }

    public void renderOverlay(float mainAlpha) {
        if (Gui.activeColorPicker == null) return;
        IComponent comp = getOrCreate(Gui.activeColorPicker);
        if (comp instanceof ColorSettingComponent colorComp) {
            colorComp.renderColorPicker(mainAlpha);
        }
    }

    public float calcSettingsHeight(Module m, Map<Setting, IComponent> cache) {
        float total = 4;
        for (Setting s : m.getSettingsForGUI()) total += getOrCreate(s).getHeight() + 1;
        return Math.max(10, total);
    }

    public float getNextModuleY(Module m, float moduleY) {
        float next = moduleY + MODULE_HEIGHT + MODULE_GAP;
        float sA = Gui.getModuleSettingsAnimation(m).getOutput();
        if (sA > 0.001F) next = getSettingsBoxY(moduleY) + calcSettingsHeight(m, settingComponentCache) * sA + MODULE_GAP;
        return next;
    }

    public static float getModuleX(float p) { return p + 4; }
    public static float getModuleWidth() { return PanelComponent.PANEL_WIDTH - 8; }
    public static float getSettingsBoxX(float p) { return p + 6; }
    public static float getSettingsBoxWidth() { return PanelComponent.PANEL_WIDTH - 12; }
    public static float getSettingsBoxY(float my) { return my + MODULE_HEIGHT; }
    public static float getSettingsX(float p) { return p + 8; }
    public static float getSettingsWidth() { return PanelComponent.PANEL_WIDTH - 16; }
    public static float getSettingsStartY(float my) { return getSettingsBoxY(my) + 4; }

    public IComponent getOrCreate(Setting setting) {
        return settingComponentCache.computeIfAbsent(setting, settingFactory::create);
    }
}