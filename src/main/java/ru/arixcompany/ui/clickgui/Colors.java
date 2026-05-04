package ru.arixcompany.ui.clickgui;

import ru.arixcompany.Arix;
import ru.arixcompany.features.module.Theme;
import ru.arixcompany.utils.render.ColorUtil;

public final class Colors {
    private Colors() {}
    private static Theme theme() { return Arix.getInstance() != null && Arix.getInstance().getCurrentTheme() != null ? Arix.getInstance().getCurrentTheme() : Theme.PURPLE; }

    // Главный акцентный цвет
    public static int accent(float alpha) { return ColorUtil.replAlpha(theme().getMain().getRGB(), (int) (255.0F * alpha)); }

    // Полупрозрачный задний фон панелей (Glassmorphism эффект)
    public static int bgPrimary(float alpha) { return ColorUtil.rgba(20, 20, 22, (int) (210.0F * alpha)); }

    // Элементы, шапка, фоны настроек
    public static int bgSecondary(float alpha) { return ColorUtil.rgba(30, 30, 33, (int) (230.0F * alpha)); }

    // Модули
    public static int bgElement(float alpha) { return ColorUtil.rgba(35, 35, 38, (int) (180.0F * alpha)); }

    // Цвет обводки (тонкие линии)
    public static int outline(float alpha) { return ColorUtil.rgba(65, 65, 70, (int) (150.0F * alpha)); }

    // При наведении мыши
    public static int hoverBg(float alpha, float anim) { return ColorUtil.rgba(45, 45, 50, (int) (180.0F * alpha * anim)); }

    public static int textActive(float alpha) { return ColorUtil.rgba(255, 255, 255, (int) (255.0F * alpha)); }
    public static int textInactive(float alpha) { return ColorUtil.rgba(160, 160, 170, (int) (255.0F * alpha)); }
    public static int moduleEnabledColor(float alpha, float animPC) { return ColorUtil.overCol(textInactive(alpha), textActive(alpha), animPC); }
}