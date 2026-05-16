package ru.arixcompany.ui.clickgui;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ru.arixcompany.Arix;
import ru.arixcompany.ui.clickgui.components.PanelComponent;
import ru.arixcompany.ui.clickgui.components.module.ModuleComponent;
import ru.arixcompany.ui.clickgui.components.module.SettingComponentFactory;
import ru.arixcompany.ui.clickgui.widgets.SearchComponent;
import ru.arixcompany.ui.clickgui.widgets.ThemeComponent;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.Theme;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.features.module.setting.implement.TextSetting;
import ru.arixcompany.features.module.setting.implement.ColorSetting;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;
import ru.arixcompany.utils.math.ScrollUtil;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;

import java.util.*;

public final class Gui extends Screen implements IMinecraft {

    public static final Animation alphaPC = new EaseInOutQuad(300, 1.0);
    public static final Animation animation14 = new EaseInOutQuad(500, 1.0);
    public static final Animation animation15 = new EaseInOutQuad(300, 1.0);

    public static BindSetting activeBindSetting = null;
    public static TextSetting activeStringSetting = null;
    public static ValueSetting activeValueSetting = null;
    public static ColorSetting activeColorSetting = null;
    public static Module activeModuleBind = null;

    public static boolean moduleBindAwaitingMode = false;

    public static float sliderX = 0, sliderY = 0, sliderWidth = 0;

    public static float colorPickerX = 0, colorPickerY = 0, colorPickerWidth = 0;

    public static boolean exit = false;
    public static Category[] categories;
    public static Theme[] themes;
    public static Theme selectedTheme, preSelectedTheme;

    public static String searchText = "";
    public static boolean activeSearch = false;
    public static boolean backspaceHeld = false;
    public static long lastBackspaceTime = 0, firstBackspacePressTime = 0;

    public static final Set<Module> openSettingsModules = new HashSet<>();
    public static final Map<Module, Animation> moduleSettingsAnimations = new HashMap<>();
    public static final Map<Module, Animation> moduleSettingsAlphaAnimations = new HashMap<>();
    public static final Map<Module, Animation> moduleBindAnimations = new HashMap<>();
    public static final Map<Module, Animation> moduleHoverAnimations = new HashMap<>();

    public static final Map<Category, Animation> categoryDropdownAnimations = new HashMap<>();
    public static final Map<Category, Animation> categoryHoverAnimations = new HashMap<>();
    public static final Map<Category, ScrollUtil> categoryScrollUtils = new HashMap<>();
    public static final Map<Category, Float> categoryPanelX = new HashMap<>();
    public static final Map<Category, Float> categoryPanelY = new HashMap<>();

    private Category draggingCategory = null;
    private float dragXOffset = 0, dragYOffset = 0;

    @Getter private final SearchComponent searchComponent;
    @Getter private final ThemeComponent themeComponent;
    @Getter private ModuleComponent moduleComponent;
    @Getter private final PanelComponent panelComponent;

    public Gui() {
        super(Component.empty());
        this.searchComponent = new SearchComponent();
        this.themeComponent = new ThemeComponent();
        this.moduleComponent = new ModuleComponent(
                new SettingComponentFactory()
        );
        this.panelComponent = new PanelComponent(moduleComponent);
    }

    @Override
    public void init() {
        super.init();
        exit = false;
        categories = Category.values();
        themes = Theme.values();

        if (Arix.getInstance() != null) {
            selectedTheme = Arix.getInstance().getCurrentTheme();
            preSelectedTheme = selectedTheme;
        }

        alphaPC.setDirection(Direction.FORWARDS);
        alphaPC.reset();

        if (mc.mouseHandler != null) mc.mouseHandler.releaseMouse();

        initPanelPositions();
    }

    private void initPanelPositions() {
        float startX = 20.0F;
        float startY = 80.0F;
        for (Category c : categories) {
            if (!categoryPanelX.containsKey(c)) {
                categoryPanelX.put(c, startX);
                categoryPanelY.put(c, startY);
                startX += PanelComponent.PANEL_WIDTH + 10.0F;

                Animation anim = getCategoryDropdownAnimation(c);
                anim.setDirection(Direction.FORWARDS);
                anim.timerUtil.setTime(System.currentTimeMillis() - 9999);
            }
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        if (mc.getWindow() == null) return;
        float mainAlpha = (float) alphaPC.getOutput();

        RenderUtils.fillRoundRect(ctx, 0, 0,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),
                0, ColorUtil.rgba(0, 0, 0, (int)(120.0F * mainAlpha)));

        if (draggingCategory != null) {
            categoryPanelX.put(draggingCategory, mouseX - dragXOffset);
            categoryPanelY.put(draggingCategory, mouseY - dragYOffset);
        }

        if (!Gui.searchText.isEmpty()) {
            for (Category c : categories) {
                Animation anim = getCategoryDropdownAnimation(c);
                if (anim.getDirection() != Direction.FORWARDS) {
                    anim.setDirection(Direction.FORWARDS);
                }
            }
        }

        searchComponent.render(ctx, mouseX, mouseY, mainAlpha);
        themeComponent.render(ctx, mouseX, mouseY, mainAlpha);
        panelComponent.render(ctx, mouseX, mouseY, mainAlpha);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int button = event.button();

        if (activeBindSetting != null && button >= 0) {
            activeBindSetting.setKey(-100 - button);
            activeBindSetting.active = false;
            activeBindSetting = null;
            return true;
        }

        if (activeModuleBind != null && !moduleBindAwaitingMode && button >= 0) {
            activeModuleBind.bind = -100 - button;
            moduleBindAwaitingMode = true;
            return true;
        }

        if (searchComponent.mouseClicked(mx, my, button)) return true;
        if (themeComponent.mouseClicked(mx, my, button)) return true;

        for (int i = categories.length - 1; i >= 0; i--) {
            Category c = categories[i];
            float px = getCategoryPanelX(c);
            float py = getCategoryPanelY(c);

            if (PanelComponent.isHovered(mx, my, px, py, PanelComponent.PANEL_WIDTH, PanelComponent.PANEL_HEADER_HEIGHT)) {
                if (button == 0) {
                    draggingCategory = c;
                    dragXOffset = mx - px;
                    dragYOffset = my - py;
                } else if (button == 1) {
                    Animation anim = getCategoryDropdownAnimation(c);
                    anim.setDirection(anim.getDirection() == Direction.FORWARDS ? Direction.BACKWARDS : Direction.FORWARDS);
                }
                return true;
            }

            if (panelComponent.mouseClickedCategory(mx, my, button, c)) return true;
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingCategory = null;
        moduleComponent.mouseReleased();
        panelComponent.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int button = event.button();

        if (moduleComponent.mouseDragged(mx, my, button, dragX, dragY)) return true;
        if (panelComponent.mouseDragged(mx, my, button, dragX, dragY)) return true;

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (panelComponent.mouseScrolled((float) mouseX, (float) mouseY, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchComponent.keyPressed(event.key(), event.scancode(), event.modifiers())) return true;
        if (moduleComponent.keyPressed(event.key(), event.scancode(), event.modifiers())) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchComponent.charTyped((char) event.codepoint(), event.modifiers())) return true;
        if (moduleComponent.charTyped((char) event.codepoint(), event.modifiers())) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (!exit && alphaPC.getOutput() > 0.0) {
            alphaPC.setDirection(Direction.BACKWARDS);
            exit = true;
        }
        return false;
    }

    @Override
    public void onClose() {
        searchComponent.close();

        super.onClose();
    }

    @Override
    public void tick() {
        super.tick();
        searchComponent.tick();
        if (exit && alphaPC.finished(Direction.BACKWARDS)) {
            onClose();
            exit = false;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    public static Animation getModuleSettingsAnimation(Module m) { return moduleSettingsAnimations.computeIfAbsent(m, k -> backwardAnim()); }
    public static Animation getModuleSettingsAlphaAnimation(Module m) { return moduleSettingsAlphaAnimations.computeIfAbsent(m, k -> backwardAnim()); }
    public static Animation getModuleBindAnimation(Module m) { return moduleBindAnimations.computeIfAbsent(m, k -> new EaseInOutQuad(200, 1.0)); }
    public static Animation getModuleHoverAnimation(Module m) { return moduleHoverAnimations.computeIfAbsent(m, k -> backwardAnim()); }
    public static Animation getCategoryDropdownAnimation(Category c) { return categoryDropdownAnimations.computeIfAbsent(c, k -> new EaseInOutQuad(250, 1.0)); }
    public static Animation getCategoryHoverAnimation(Category c) { return categoryHoverAnimations.computeIfAbsent(c, k -> backwardAnim()); }
    public static ScrollUtil getCategoryScrollUtil(Category c) { return categoryScrollUtils.computeIfAbsent(c, k -> new ScrollUtil()); }
    public static float getCategoryPanelX(Category c) { return categoryPanelX.getOrDefault(c, 0.0F); }
    public static float getCategoryPanelY(Category c) { return categoryPanelY.getOrDefault(c, 0.0F); }

    public static void animRun(Animation a, double target) {
        Direction d = target > 0.5 ? Direction.FORWARDS : Direction.BACKWARDS;
        if (a.getDirection() != d) a.setDirection(d);
    }

    private static Animation backwardAnim() {
        Animation a = new EaseInOutQuad(200, 1.0);
        a.setDirection(Direction.BACKWARDS);
        a.timerUtil.setTime(System.currentTimeMillis() - 9999);
        return a;
    }
}