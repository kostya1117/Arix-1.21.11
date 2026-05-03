package ru.arixcompany.clickgui.components.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.Arix;
import ru.arixcompany.clickgui.Colors;
import ru.arixcompany.clickgui.Gui;
import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.clickgui.components.PanelComponent;
import ru.arixcompany.clickgui.components.module.settings.ColorSettingComponent;
import ru.arixcompany.module.Category;
import ru.arixcompany.module.setting.Setting;
import ru.arixcompany.module.Module;
import ru.arixcompany.module.setting.implement.ColorSetting;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.math.StringUtil;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public final class ModuleComponent implements IComponent {

    public static final float MODULE_HEIGHT = 18.5F;
    public static final float MODULE_GAP    = 2.0F;

    private final SettingComponentFactory settingFactory;

    @Getter
    private final Map<Setting, IComponent> settingComponentCache = new HashMap<>();

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (Gui.categories == null || Arix.getInstance() == null) return false;

        for (Category category : Gui.categories) {
            float openAnim = Gui.getCategoryDropdownAnimation(category).getOutput();
            if (openAnim <= 0.001F) continue;

            float panelX = Gui.getCategoryPanelX(category);
            float panelY = Gui.getCategoryPanelY(category);
            float bodyH  = PanelComponent.PANEL_BODY_HEIGHT * openAnim;

            if (!PanelComponent.isHovered(mouseX, mouseY,
                    panelX, panelY + PanelComponent.PANEL_HEADER_HEIGHT,
                    PanelComponent.PANEL_WIDTH, bodyH)) continue;

            if (processCategory(category, panelX, panelY, mouseX, mouseY, button))
                return true;
        }
        return false;
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
                Gui.animRun(Gui.getModuleBindAnimation(Gui.activeModuleBind), 0.0);
                Gui.activeModuleBind = null;
            } else {
                Gui.activeModuleBind.bind = keyCode;
                Gui.activeModuleBind.binding = false;
                Gui.animRun(Gui.getModuleBindAnimation(Gui.activeModuleBind), 1.0);
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

    private boolean processCategory(Category category,
                                    float panelX, float panelY,
                                    int mouseX, int mouseY, int button) {
        List<Module> modules = PanelComponent.getDisplayModules(category);
        float drawY = panelY + PanelComponent.PANEL_HEADER_HEIGHT
                + PanelComponent.PANEL_PADDING
                + Gui.getCategoryScrollUtil(category).getScroll();

        for (Module module : modules) {
            float moduleY      = drawY;
            float settingsAnim = Gui.getModuleSettingsAnimation(module).getOutput();

            if (settingsAnim > 0.98F && Gui.openSettingsModules.contains(module)) {
                float sX = getSettingsX(panelX);
                float sY = getSettingsStartY(moduleY);
                float sW = getSettingsWidth();

                for (Setting setting : module.getSettingsForGUI()) {
                    IComponent comp = getOrCreate(setting);
                    if (comp.handleClick(sX, sY, sW, mouseX, mouseY, button))
                        return true;
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
        Gui.selectedCategories = category;
        Gui.modules = Arix.getInstance().getModuleRepo().getModule(category);

        return switch (button) {
            case 0 -> { module.toggle(); yield true; }
            case 1 -> {
                if (module.getSettingsForGUI().isEmpty()) yield false;
                toggleSettings(module);
                yield true;
            }
            case 2 -> { handleBind(module); yield true; }
            default -> false;
        };
    }

    private void toggleSettings(Module module) {
        if (Gui.openSettingsModules.contains(module)) {
            Gui.openSettingsModules.remove(module);
            Gui.animRun(Gui.getModuleSettingsAnimation(module), 0.0);
            Gui.animRun(Gui.getModuleSettingsAlphaAnimation(module), 0.0);
            if (Gui.activeColorPicker != null && module.getSettingsForGUI().contains(Gui.activeColorPicker)) {
                Gui.animation15.setDirection(Direction.BACKWARDS);
                Gui.activeColorPicker = null;
                Gui.colorPickerX = Gui.colorPickerY = 0;
            }
        } else {
            Gui.openSettingsModules.add(module);
            Gui.animRun(Gui.getModuleSettingsAnimation(module), 1.0);
            Gui.animRun(Gui.getModuleSettingsAlphaAnimation(module), 1.0);
        }
    }

    private void handleBind(Module module) {
        if (module.binding) {
            module.binding = false;
            Gui.activeModuleBind = null;
            Gui.animRun(Gui.getModuleBindAnimation(module), 0.0);
        } else {
            if (Gui.activeModuleBind != null) {
                Gui.activeModuleBind.binding = false;
                Gui.animRun(Gui.getModuleBindAnimation(Gui.activeModuleBind), 0.0);
            }
            Gui.activeModuleBind = module;
            module.binding = true;
            Gui.animRun(Gui.getModuleBindAnimation(module), 1.0);
        }
    }

    public float renderModule(GuiGraphics guiGraphics, int mouseX, int mouseY,
                              float mainAlpha, float panelX, float moduleY,
                              Module module, Map<Setting, IComponent> cache) {
        float moduleX = getModuleX(panelX);
        float moduleW = getModuleWidth();

        Gui.animRun(module.animation, module.state ? 1.0 : 0.0);
        float toggleAnim = module.animation.getOutput();

        boolean hovered = PanelComponent.isHovered(mouseX, mouseY, moduleX, moduleY, moduleW, MODULE_HEIGHT);
        Gui.animRun(Gui.getModuleHoverAnimation(module), hovered ? 1.0 : 0.0);
        float hoverAnim = Gui.getModuleHoverAnimation(module).getOutput();

        RenderUtils.fillRoundRect(moduleX, moduleY, moduleW, MODULE_HEIGHT, 5.0F, Colors.bgElement(mainAlpha));
        RenderUtils.drawRoundRectOutline(moduleX, moduleY, moduleW, MODULE_HEIGHT, 5.0F,0.5F, Colors.outline(mainAlpha * 0.7F));

        if (hoverAnim > 0.001F)
            RenderUtils.fillRoundRect(moduleX, moduleY, moduleW, MODULE_HEIGHT, 5.0F, Colors.hoverBg(mainAlpha, hoverAnim));

        if (toggleAnim > 0.001F) {
            RenderUtils.pushRoundedClipRect(moduleX, moduleY, moduleW, MODULE_HEIGHT, 5, 5, 5, 5);
            RenderUtils.horizontalGradient(moduleX, moduleY, moduleW, MODULE_HEIGHT,
                    Colors.accentDark(mainAlpha * 0.22F * toggleAnim), Colors.accent(mainAlpha * 0.22F * toggleAnim));
            RenderUtils.popClipRect();
        }

        FontManager.get(11).drawString(guiGraphics,module.name,moduleX + 6, moduleY + 10,Colors.moduleEnabledColor(mainAlpha, toggleAnim));

        renderBindBadge(guiGraphics,mainAlpha, moduleX, moduleY, module);

//        if (!module.getSettingsForGUI().isEmpty()) {
//            float sAnim = Gui.getModuleSettingsAnimation(module).getOutput();
//            renderer2D.text(FontRegistry.ICONS, moduleX + moduleW - 23, moduleY + 10.2F, 10.0F,
//                    sAnim > 0.5F ? "S" : "R",
//                    sAnim > 0.5F ? Colors.accent(mainAlpha) : Colors.textInactive(mainAlpha));
//        }

        renderToggle(moduleX + moduleW - 14, moduleY + 6.2F, toggleAnim, mainAlpha);

        return renderSettingsBox(guiGraphics,mouseX, mouseY, mainAlpha, panelX, moduleY, module, cache);
    }

    private float renderSettingsBox(GuiGraphics guiGraphics,int mouseX, int mouseY,
                                    float mainAlpha, float panelX, float moduleY,
                                    Module module, Map<Setting, IComponent> cache) {
        float nextY        = moduleY + MODULE_HEIGHT + MODULE_GAP;
        float settingsAnim = Gui.getModuleSettingsAnimation(module).getOutput();
        float settingsAlpha = Gui.getModuleSettingsAlphaAnimation(module).getOutput();

        if (module.getSettingsForGUI().isEmpty() || (settingsAnim <= 0.001F && settingsAlpha <= 0.001F))
            return nextY;

        float fullH = calcSettingsHeight(module, cache);
        float visH  = fullH * settingsAnim;
        float boxX  = getSettingsBoxX(panelX);
        float boxY  = getSettingsBoxY(moduleY);
        float boxW  = getSettingsBoxWidth();

        RenderUtils.fillRoundRect(boxX, boxY, boxW, visH, 5.0F, Colors.bgSecondary(mainAlpha));
        RenderUtils.drawRoundRectOutline(boxX, boxY, boxW, visH, 5.0F,0.5F, Colors.outline(mainAlpha * settingsAlpha));

        RenderUtils.pushRoundedClipRect(boxX, boxY, boxW, visH, 5, 5, 5, 5);

        float sY = getSettingsStartY(moduleY);
        float sX = getSettingsX(panelX);
        float sW = getSettingsWidth();
        float sa = mainAlpha * settingsAlpha;

        for (Setting setting : module.getSettingsForGUI()) {
            IComponent comp = getOrCreate(setting);
            comp.render(guiGraphics,sX, sY, sW, mouseX, mouseY,
                    Colors.outline(sa), Colors.accent(sa),
                    ColorUtil.rgba(255, 255, 255, (int) (10.0F * sa)),
                    Colors.textInactive(sa), Colors.textActive(sa), sa);
            sY += comp.getHeight() + 1.0F;
        }

        RenderUtils.popClipRect();
        return boxY + visH + MODULE_GAP;
    }

    private void renderToggle(float x, float y, float anim, float alpha) {
        float w = 10, h = 6;
        RenderUtils.fillRoundRect(x, y, w, h, 3, Colors.bgSecondary(alpha));
        RenderUtils.drawRoundRectOutline(x, y, w, h, 3,0.5F, Colors.outline(alpha));
        if (anim > 0.001F) {
            RenderUtils.pushRoundedClipRect(x, y, w, h, 3, 3, 3, 3);
            RenderUtils.horizontalGradient(x, y, w, h, Colors.accentDim(alpha * 0.85F * anim), Colors.accent(alpha * 0.85F * anim));
            RenderUtils.popClipRect();
        }
        RenderUtils.fillRoundRect(x + 1 + 4 * anim, y + 1, 4, 4, 2, Colors.textActive(alpha));
    }

    private void renderBindBadge(GuiGraphics guiGraphics,float alpha, float mx, float my, Module m) {
        float bindAnim = Gui.getModuleBindAnimation(m).getOutput();
        if (!(m.binding || m.bind != -1 || bindAnim > 0.001F)) return;
        String keyText = m.binding ? "..." : (m.bind != -1 ? StringUtil.getBindName(m.bind) : "");
        if (keyText.isEmpty()) return;

        float nameW = FontManager.get(11).getWidth(m.getName());
        float keyW = FontManager.get(10).getWidth(keyText);
        float badgeW = Math.max(14, keyW + 6), badgeX = mx + 6 + nameW + 4, badgeY = my + 4.5F;

        RenderUtils.fillRoundRect(badgeX, badgeY, badgeW, 9.5F, 3, ColorUtil.multAlpha(Colors.bgSecondary(alpha), bindAnim));
        RenderUtils.drawRoundRectOutline(badgeX, badgeY, badgeW, 9.5F, 3,0.5F, ColorUtil.multAlpha(Colors.outline(alpha), bindAnim));
        FontManager.get(10).drawString(guiGraphics,keyText,badgeX + badgeW / 2 - keyW / 2, badgeY + 6.6F,ColorUtil.multAlpha(m.binding ? Colors.accent(alpha) : Colors.textInactive(alpha), bindAnim));
    }

    public float[] findColorPickerPosition(ColorSetting colorSetting) {
        if (colorSetting == null || Gui.categories == null) return null;
        for (Category category : Gui.categories) {
            float openAnim = Gui.getCategoryDropdownAnimation(category).getOutput();
            if (openAnim <= 0.001F) continue;
            float panelX = Gui.getCategoryPanelX(category), panelY = Gui.getCategoryPanelY(category);
            float drawY = panelY + PanelComponent.PANEL_HEADER_HEIGHT + PanelComponent.PANEL_PADDING
                    + Gui.getCategoryScrollUtil(category).getScroll();
            for (Module module : PanelComponent.getDisplayModules(category)) {
                float moduleY = drawY;
                if (Gui.openSettingsModules.contains(module)) {
                    float sX = getSettingsX(panelX), sY = getSettingsStartY(moduleY);
                    for (Setting setting : module.getSettingsForGUI()) {
                        if (setting == colorSetting) return new float[]{ sX + getSettingsWidth() - 15, sY - 5 };
                        sY += getOrCreate(setting).getHeight() + 1;
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
        float total = 8;
        for (Setting s : m.getSettingsForGUI()) total += getOrCreate(s).getHeight() + 1;
        return Math.max(18, total);
    }

    public float getNextModuleY(Module m, float moduleY) {
        float next = moduleY + MODULE_HEIGHT + MODULE_GAP;
        float sA = Gui.getModuleSettingsAnimation(m).getOutput();
        float saA = Gui.getModuleSettingsAlphaAnimation(m).getOutput();
        if (sA > 0.001F || saA > 0.001F)
            next = getSettingsBoxY(moduleY) + calcSettingsHeight(m, settingComponentCache) * sA + MODULE_GAP;
        return next;
    }

    public static float getModuleX(float p)          { return p + 4; }
    public static float getModuleWidth()              { return PanelComponent.PANEL_WIDTH - 8; }
    public static float getSettingsBoxX(float p)     { return p + 6; }
    public static float getSettingsBoxWidth()         { return PanelComponent.PANEL_WIDTH - 12; }
    public static float getSettingsBoxY(float my)    { return my + MODULE_HEIGHT + 1; }
    public static float getSettingsX(float p)        { return p + 8; }
    public static float getSettingsWidth()            { return PanelComponent.PANEL_WIDTH - 16; }
    public static float getSettingsStartY(float my)  { return getSettingsBoxY(my) + 4; }

    public IComponent getOrCreate(Setting setting) {
        return settingComponentCache.computeIfAbsent(setting, settingFactory::create);
    }
}