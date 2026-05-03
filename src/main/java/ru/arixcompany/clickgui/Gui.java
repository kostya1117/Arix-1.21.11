package ru.arixcompany.clickgui;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import ru.arixcompany.Arix;
import ru.arixcompany.clickgui.components.PanelComponent;
import ru.arixcompany.clickgui.components.module.ModuleComponent;
import ru.arixcompany.clickgui.components.module.SettingComponentFactory;
import ru.arixcompany.clickgui.widgets.SearchComponent;
import ru.arixcompany.clickgui.widgets.ThemeComponent;
import ru.arixcompany.module.Category;
import ru.arixcompany.module.Theme;
import ru.arixcompany.module.setting.implement.BindSetting;
import ru.arixcompany.module.setting.implement.ColorSetting;
import ru.arixcompany.module.setting.implement.SliderSetting;
import ru.arixcompany.module.setting.implement.TextSetting;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.math.ScrollUtil;
import ru.arixcompany.module.Module;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;

import java.util.*;

public final class Gui extends Screen implements IMinecraft {

    public static final Animation mainAnimation = new EaseInOutQuad(200, 1.0);
    public static final Animation animation14   = new EaseInOutQuad(500, 1.0);
    public static final Animation animation15   = new EaseInOutQuad(1000, 1.0);
    public static final Animation alphaPC       = new EaseInOutQuad(400, 1.0);

    public static ColorSetting  activeColorPicker   = null;
    public static BindSetting   activeBindSetting   = null;
    public static TextSetting   activeStringSetting = null;
    public static SliderSetting activeValueSetting  = null;
    public static Module        activeModuleBind    = null;

    public static float   colorPickerX = 0, colorPickerY = 0;
    public static boolean pickingSaturationBrightness = false;
    public static boolean pickingHue = false, pickingAlpha = false;

    public static float sliderX = 0, sliderY = 0, sliderWidth = 0;

    public static String  searchText  = "";
    public static boolean activeSearch = false;
    public static boolean backspaceHeld = false;
    public static long    lastBackspaceTime = 0, firstBackspacePressTime = 0;

    public static boolean  exit = false;
    public static float    x, y, width, height;
    public static int      currentMouseX = 0, currentMouseY = 0;

    public static Category[]   categories;
    public static Theme[]      themes;
    public static Theme        selectedTheme, preSelectedTheme;
    public static Category     selectedCategories;
    public static List<Module> modules;

    public static final Set<Module>   openSettingsModules = new HashSet<>();
    public static final Set<Category> openCategories      = new HashSet<>();

    public static final Map<Module,   Animation>  moduleSettingsAnimations        = new HashMap<>();
    public static final Map<Module,   Animation>  moduleSettingsAlphaAnimations   = new HashMap<>();
    public static final Map<Module,   Animation>  moduleBindAnimations            = new HashMap<>();
    public static final Map<Module,   Animation>  moduleHoverAnimations           = new HashMap<>();
    public static final Map<Category, Animation>  categoryDropdownAnimations      = new HashMap<>();
    public static final Map<Category, Animation>  categoryDropdownAlphaAnimations = new HashMap<>();
    public static final Map<Category, Animation>  categoryHoverAnimations         = new HashMap<>();
    public static final Map<Category, ScrollUtil> categoryScrollUtils             = new HashMap<>();
    public static final Map<Category, Float>      categoryPanelX                 = new HashMap<>();
    public static final Map<Category, Float>      categoryPanelY                 = new HashMap<>();

    private static final Set<Module> initializedBindAnimations = new HashSet<>();

    // =========================================================
    //  Animation helpers
    // =========================================================

    public static Animation getModuleSettingsAnimation(Module m) {
        return moduleSettingsAnimations.computeIfAbsent(m, k -> backwardAnim());
    }

    public static Animation getModuleSettingsAlphaAnimation(Module m) {
        return moduleSettingsAlphaAnimations.computeIfAbsent(m, k -> backwardAnim());
    }

    public static Animation getModuleBindAnimation(Module m) {
        Animation a = moduleBindAnimations.computeIfAbsent(m, k -> new EaseInOutQuad(200, 1.0));
        if (m.bind != -1 && initializedBindAnimations.add(m)) finishForward(a);
        return a;
    }

    public static Animation getModuleHoverAnimation(Module m) {
        return moduleHoverAnimations.computeIfAbsent(m, k -> backwardAnim());
    }

    public static Animation getCategoryDropdownAnimation(Category c) {
        return categoryDropdownAnimations.computeIfAbsent(c, k -> new EaseInOutQuad(200, 1.0));
    }

    public static Animation getCategoryDropdownAlphaAnimation(Category c) {
        return categoryDropdownAlphaAnimations.computeIfAbsent(c, k -> new EaseInOutQuad(200, 1.0));
    }

    public static Animation getCategoryHoverAnimation(Category c) {
        return categoryHoverAnimations.computeIfAbsent(c, k -> backwardAnim());
    }

    public static ScrollUtil getCategoryScrollUtil(Category c) {
        return categoryScrollUtils.computeIfAbsent(c, k -> new ScrollUtil());
    }

    public static float getCategoryPanelX(Category c) {
        return categoryPanelX.getOrDefault(c, 0.0F);
    }

    public static float getCategoryPanelY(Category c) {
        return categoryPanelY.getOrDefault(c, 0.0F);
    }

    public static void setCategoryPanelPos(Category c, float px, float py) {
        categoryPanelX.put(c, px);
        categoryPanelY.put(c, py);
    }

    public static void finishForward(Animation a) {
        a.setDirection(Direction.FORWARDS);
        a.timerUtil.setTime(System.currentTimeMillis() - a.getDuration() - 1);
    }

    public static void finishBackward(Animation a) {
        a.setDirection(Direction.BACKWARDS);
        a.timerUtil.setTime(System.currentTimeMillis() - a.getDuration() - 1);
    }

    public static void animRun(Animation a, double target) {
        Direction d = target > 0.5 ? Direction.FORWARDS : Direction.BACKWARDS;
        if (a.getDirection() != d) a.setDirection(d);
    }

    private static Animation backwardAnim() {
        Animation a = new EaseInOutQuad(200, 1.0);
        a.setDirection(Direction.BACKWARDS);
        a.timerUtil.setTime(System.currentTimeMillis() - 999999L);
        return a;
    }

    // =========================================================
    //  Fields
    // =========================================================

    @Getter private final SearchComponent searchComponent;
    @Getter private final ThemeComponent  themeComponent;
    @Getter private       ModuleComponent moduleComponent;
    @Getter private final PanelComponent  panelComponent;

    public Gui() {
        super(Component.empty());
        this.searchComponent = new SearchComponent();
        this.themeComponent  = new ThemeComponent();
        this.moduleComponent = new ModuleComponent(
                new SettingComponentFactory(
                        cs -> this.moduleComponent.findColorPickerPosition(cs)
                )
        );
        this.panelComponent = new PanelComponent(moduleComponent);
    }

    // =========================================================
    //  Screen lifecycle
    // =========================================================

    @Override
    public void init() {
        super.init();

        alphaPC.setDirection(Direction.BACKWARDS);
        finishBackward(alphaPC);
        alphaPC.setDirection(Direction.FORWARDS);
        alphaPC.reset();
        exit = false;
        mainAnimation.reset();

        if (mc.mouseHandler != null) mc.mouseHandler.releaseMouse();

        categories = Category.values();
        themes     = Theme.values();
        width      = 178.0F;
        height     = 26.0F;
        x = mc.getWindow().getScreenWidth()  / 2.0F - width  / 2.0F;
        y = 14.0F;

        selectedTheme    = Arix.getInstance().getCurrentTheme();
        preSelectedTheme = Arix.getInstance().getCurrentTheme();
        modules = Arix.getInstance().getModuleRepo().getModule(selectedCategories);

        initCategoryPanels();
    }

    /**
     * Главный метод рендера — здесь всё рисуется.
     */
    @Override
    public void render(GuiGraphics ctx, int mx, int my, float dt) {
        // Не вызываем super.render — он рисует фон vanilla, нам не нужно
        renderGui(ctx, mx, my, dt);
    }

    /**
     * Фон экрана — переопределяем пустым чтобы не рисовался стандартный blur/dirt.
     */
    @Override
    public void renderBackground(GuiGraphics ctx, int mx, int my, float d) {
        // Намеренно пусто — свой фон рисуем в renderGui
    }

    // =========================================================
    //  Input
    // =========================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean p_431348_) {
        if (handleMouseClicked(click.x(), click.y(), click.button())) return true;
        return super.mouseClicked(click, p_431348_);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent p_429390_) {
        handleMouseReleased();
        return super.mouseReleased(p_429390_);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (handleMouseDragged(click.x(), click.y(), click.button(), dx, dy)) return true;
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        float[] calc = MathUtils.calc((float) mx, (float) my);
        if (panelComponent.mouseScrolled(calc[0], calc[1], sy)) return true;
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (handleKeyPressed(input.key(), input.scancode(), input.modifiers())) return true;
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (handleCharTyped((char) input.codepoint(), input.modifiers())) return true;
        return super.charTyped(input);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (!exit && alphaPC.getOutput() > 0.0) {
            alphaPC.setDirection(Direction.BACKWARDS);
            alphaPC.reset();
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
        if (exit && alphaPC.finished(Direction.BACKWARDS)) {
            onClose();
            exit = false;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // =========================================================
    //  Render logic
    // =========================================================

    public void renderGui(GuiGraphics guiGraphics, int rawMouseX, int rawMouseY, float delta) {
        if (mc.getWindow() == null) return;

        int viewW = mc.getWindow().getScreenWidth();
        int viewH = mc.getWindow().getScreenHeight();
        if (viewW <= 0 || viewH <= 0) return;

        // Пересчёт координат мыши из screen-пространства в scaled-GUI
        currentMouseX = (int) MathUtils.calc((float) rawMouseX, (float) rawMouseY)[0];
        currentMouseY = (int) MathUtils.calc((float) rawMouseX, (float) rawMouseY)[1];

        float mainAlpha = (float) alphaPC.getOutput();
        if (mainAlpha <= 0.001F) return;

        float scaledW = mc.getWindow().getGuiScaledWidth();
        float scaledH = mc.getWindow().getGuiScaledHeight();

        // Полупрозрачный чёрный фон
        RenderUtils.fillRoundRect(
                0, 0, scaledW, scaledH, 0,
                ColorUtil.rgba(0, 0, 0, (int) (115.0F * mainAlpha))
        );

        // Компоненты
        searchComponent.tick();
        searchComponent.render(guiGraphics, currentMouseX, currentMouseY, mainAlpha);
        panelComponent.render(guiGraphics, currentMouseX, currentMouseY, mainAlpha);
        moduleComponent.renderOverlay(mainAlpha);
        themeComponent.render(guiGraphics, currentMouseX, currentMouseY, mainAlpha);
    }

    // =========================================================
    //  Input handlers
    // =========================================================

    private boolean handleCharTyped(char codePoint, int modifiers) {
        if (moduleComponent.charTyped(codePoint, modifiers)) return true;
        return searchComponent.charTyped(codePoint, modifiers);
    }

    private boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (moduleComponent.keyPressed(keyCode, scanCode, modifiers)) return true;
        return searchComponent.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean handleMouseClicked(double rawMX, double rawMY, int button) {
        float[] calc = MathUtils.calc((float) rawMX, (float) rawMY);
        int mouseX = (int) calc[0];
        int mouseY = (int) calc[1];

        x = Mth.clamp(x, 0, MathUtils.calc(mc.getWindow().getScreenWidth())  - width);
        y = Mth.clamp(y, 0, MathUtils.calc(mc.getWindow().getScreenHeight()) - height);

        if (!exit) {
            if (searchComponent.mouseClicked(mouseX, mouseY, button)) return true;
            if (panelComponent.mouseClicked(mouseX, mouseY, button))  return true;
            if (moduleComponent.mouseClicked(mouseX, mouseY, button)) return true;
            if (themeComponent.mouseClicked(mouseX, mouseY, button))  return true;
        }

        if (activeBindSetting != null && button >= 0 && button <= 2) {
            activeBindSetting.setKey(-100 - button);
            activeBindSetting.active = false;
            activeBindSetting        = null;
            return true;
        }

        return false;
    }

    private boolean handleMouseDragged(double rawMX, double rawMY, int button, double dx, double dy) {
        float[] calc = MathUtils.calc((float) rawMX, (float) rawMY);
        int mouseX = (int) calc[0];
        int mouseY = (int) calc[1];

        if (moduleComponent.mouseDragged(mouseX, mouseY, button, dx, dy)) return true;
        if (panelComponent.mouseDragged(mouseX, mouseY, button, dx, dy))  return true;

        return false;
    }

    private void handleMouseReleased() {
        moduleComponent.mouseReleased();
        panelComponent.mouseReleased();
    }

    // =========================================================
    //  Init helpers
    // =========================================================

    private void initCategoryPanels() {
        float pw  = PanelComponent.PANEL_WIDTH;
        float ph  = PanelComponent.PANEL_HEADER_HEIGHT + PanelComponent.PANEL_BODY_HEIGHT;
        float spX = 8, spY = 12;

        float sw = mc.getWindow().getGuiScaledWidth();
        float sh = mc.getWindow().getGuiScaledHeight();

        int cols = Math.max(1, (int) ((sw + spX) / (pw + spX)));
        cols = Math.min(cols, categories.length);
        int rows = (int) Math.ceil((double) categories.length / cols);

        float totalW = cols * pw + (cols - 1) * spX;
        float totalH = rows * ph + (rows - 1) * spY;

        float topOffset = 8.0F + 18.0F + 6.0F + 21.25F + 8.0F;

        float startX     = (sw - totalW) / 2.0F;
        float availableH = sh - topOffset;
        float startY     = topOffset + (availableH - totalH) / 2.0F;

        startX = Math.max(2, startX);
        startY = Math.max(topOffset + 4, startY);

        for (int i = 0; i < categories.length; i++) {
            Category c = categories[i];
            float px = startX + (i % cols) * (pw + spX);
            float py = startY + (i / cols) * (ph + spY);
            setCategoryPanelPos(c, px, py);

            openCategories.add(c);
            finishForward(getCategoryDropdownAnimation(c));
            finishForward(getCategoryDropdownAlphaAnimation(c));
        }
    }
}