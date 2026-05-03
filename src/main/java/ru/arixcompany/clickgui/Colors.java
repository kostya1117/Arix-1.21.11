package ru.arixcompany.clickgui;

import ru.arixcompany.Arix;
import ru.arixcompany.module.Theme;
import ru.arixcompany.utils.render.ColorUtil;

public final class Colors {

    private Colors() {}

    private static Theme theme() {
        if (Arix.getInstance() != null && Arix.getInstance().getCurrentTheme() != null) {
            return Arix.getInstance().getCurrentTheme();
        }
        return Theme.PURPLE;
    }

    public static int accent(float alpha) {
        return ColorUtil.replAlpha(theme().getMain().getRGB(), (int) (255.0F * alpha));
    }

    public static int accentDim(float alpha) {
        int mixed = ColorUtil.overCol(theme().getBg2().getRGB(), theme().getMain().getRGB(), 0.45F);
        return ColorUtil.replAlpha(mixed, (int) (255.0F * alpha));
    }

    public static int accentDark(float alpha) {
        int mixed = ColorUtil.overCol(theme().getBg().getRGB(), theme().getMain().getRGB(), 0.28F);
        return ColorUtil.replAlpha(mixed, (int) (255.0F * alpha));
    }

    public static int bgPrimary(float alpha) {
        return ColorUtil.replAlpha(theme().getBg().getRGB(), (int) (220.0F * alpha));
    }

    public static int bgSecondary(float alpha) {
        return ColorUtil.replAlpha(theme().getBg2().getRGB(), (int) (235.0F * alpha));
    }

    public static int bgElement(float alpha) {
        int mixed = ColorUtil.overCol(theme().getBg2().getRGB(), theme().getBg().getRGB(), 0.35F);
        return ColorUtil.replAlpha(mixed, (int) (210.0F * alpha));
    }

    public static int outline(float alpha) {
        return ColorUtil.replAlpha(theme().getOutline().getRGB(), (int) (80.0F * alpha));
    }

    public static int hoverBg(float mainAlpha, float hoverAnim) {
        int mixed = ColorUtil.overCol(theme().getBg2().getRGB(), theme().getMain().getRGB(), 0.35F);
        return ColorUtil.replAlpha(mixed, (int) (70.0F * mainAlpha * hoverAnim));
    }

    public static int textActive(float alpha) {
        return ColorUtil.replAlpha(theme().getText().getRGB(), (int) (255.0F * alpha));
    }

    public static int textInactive(float alpha) {
        return ColorUtil.replAlpha(theme().getText2().getRGB(), (int) (225.0F * alpha));
    }

    public static int moduleEnabledColor(float mainAlpha, float animPC) {
        return ColorUtil.overCol(textInactive(mainAlpha), accent(mainAlpha), animPC);
    }
}