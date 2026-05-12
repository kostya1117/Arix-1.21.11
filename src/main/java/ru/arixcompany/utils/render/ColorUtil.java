package ru.arixcompany.utils.render;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.features.module.Theme;

import java.awt.*;
import java.util.concurrent.*;

@UtilityClass
public class ColorUtil {

    private final long CACHE_EXPIRATION_TIME = 60 * 1000;
    private final ConcurrentHashMap<ColorKey, CacheEntry> colorCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cacheCleaner = Executors.newScheduledThreadPool(1);
    private final DelayQueue<CacheEntry> cleanupQueue = new DelayQueue<>();

    static {
        cacheCleaner.scheduleWithFixedDelay(() -> {
            CacheEntry entry = cleanupQueue.poll();
            while (entry != null) {
                if (entry.isExpired()) {
                    colorCache.remove(entry.getKey());
                }
                entry = cleanupQueue.poll();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static float getRed(int color) {
        return (color >> 16 & 0xFF) / 255.0F;
    }

    public static float getGreen(int color) {
        return (color >> 8 & 0xFF) / 255.0F;
    }

    public static float getBlue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    public static float getAlpha(int color) {
        return (color >> 24 & 0xFF) / 255.0F;
    }

    public static Color injectAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color TwoColoreffect(Color color, Color color2, double n) {
        float clamp = Mth.clamp((float) Math.sin((Math.PI * 6) * (n / 4.0 % 1.0)) / 2.0F + 0.5F, 0.0F, 1.0F);
        return new Color(
                Mth.lerp(color.getRed() / 255.0F, color2.getRed() / 255.0F, clamp),
                Mth.lerp(color.getGreen() / 255.0F, color2.getGreen() / 255.0F, clamp),
                Mth.lerp(color.getBlue() / 255.0F, color2.getBlue() / 255.0F, clamp),
                Mth.lerp(color.getAlpha() / 255.0F, color2.getAlpha() / 255.0F, clamp));
    }

    public static Color setAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static int setAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }

    public static int getClientColor() {
        return getMainColor(10, 255);
    }

    private static Theme getTheme() {
        return Gui.selectedTheme != null ? Gui.selectedTheme : Theme.PURPLE;
    }

    private static Theme getPreTheme() {
        return Gui.preSelectedTheme != null ? Gui.preSelectedTheme : Theme.PURPLE;
    }

    public static int[] getClientColor(int speed, int alpha) {
        Theme theme = getTheme();
        Theme preTheme = getPreTheme();
        return new int[] {
                applyOpacity(
                        gradient(speed, 0,
                                interpolate(theme.getMain().getRGB(), preTheme.getMain().getRGB(),
                                        1.0F - Gui.animation14.getOutput())),
                        alpha),
                applyOpacity(
                        gradient(speed, 90,
                                interpolate(theme.getMain().getRGB(), preTheme.getMain().getRGB(),
                                        1.0F - Gui.animation14.getOutput())),
                        alpha),
                applyOpacity(
                        gradient(speed, 180,
                                interpolate(theme.getMain().getRGB(), preTheme.getMain().getRGB(),
                                        1.0F - Gui.animation14.getOutput())),
                        alpha),
                applyOpacity(
                        gradient(speed, 270,
                                interpolate(theme.getMain().getRGB(), preTheme.getMain().getRGB(),
                                        1.0F - Gui.animation14.getOutput())),
                        alpha)
        };
    }

    public static int getBackGroundColor(int speed, int index) {
        Theme theme = getTheme();
        Theme preTheme = getPreTheme();
        return gradient2(
                interpolate(theme.getBg().getRGB(), preTheme.getBg().getRGB(), 1.0F - Gui.animation14.getOutput()),
                interpolate(theme.getBg().getRGB(), preTheme.getBg().getRGB(), 1.0F - Gui.animation14.getOutput()),
                speed,
                index);
    }

    public static int getBackGroundTwoColor(int speed, int index) {
        Theme theme = getTheme();
        Theme preTheme = getPreTheme();
        return gradient2(
                interpolate(theme.getBg2().getRGB(), preTheme.getBg2().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                interpolate(theme.getBg2().getRGB(), preTheme.getBg2().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                speed,
                index);
    }

    public static int getOutLineColor(int speed, int index) {
        Theme theme = getTheme();
        Theme preTheme = getPreTheme();
        return gradient2(
                interpolate(theme.getOutline().getRGB(), preTheme.getOutline().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                interpolate(theme.getOutline().getRGB(), preTheme.getOutline().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                speed,
                index);
    }

    public static int getMainColor(int speed, int index) {
        Theme theme = getTheme();
        Theme preTheme = getPreTheme();
        return gradient2(
                interpolate(theme.getMain().getRGB(), preTheme.getMain().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                interpolate(theme.getMain().getRGB(), preTheme.getMain().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                speed,
                index);
    }

    public static int getTextColor(int speed, int index) {
        Theme theme = getTheme();
        Theme preTheme = getPreTheme();
        return gradient2(
                interpolate(theme.getText().getRGB(), preTheme.getText().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                interpolate(theme.getText().getRGB(), preTheme.getText().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                speed,
                index);
    }

    public static int getTextTwoColor(int speed, int index) {
        Theme theme = getTheme();
        Theme preTheme = getPreTheme();
        return gradient2(
                interpolate(theme.getText2().getRGB(), preTheme.getText2().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                interpolate(theme.getText2().getRGB(), preTheme.getText2().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                speed,
                index);
    }

    public Color interpolate(Color color1, Color color2, double amount) {
        amount = 1.0 - amount;
        amount = (float) Mth.clamp(amount, 0.0, 1.0);
        return new Color(
                (int) Mth.lerp(color1.getRed(), color2.getRed(), amount),
                (int) Mth.lerp(color1.getGreen(), color2.getGreen(), amount),
                (int) Mth.lerp(color1.getBlue(), color2.getBlue(), amount),
                (int) Mth.lerp(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static Color interpolateTwoColors(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = 0;
        if (speed == 0) {
            angle = index % 360;
        } else {
            angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
        }

        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return trueColor ? interpolateColorHue(start, end, angle / 360.0F)
                : interpolateColorC(start, end, angle / 360.0F);
    }

    public static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1.0F, Math.max(0.0F, amount));
        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);
        Color resultColor = Color.getHSBColor(
                Mth.lerp(color1HSB[0], color2HSB[0], amount),
                Mth.lerp(color1HSB[1], color2HSB[1], amount),
                Mth.lerp(color1HSB[2], color2HSB[2], amount));
        return new Color(
                resultColor.getRed(),
                resultColor.getGreen(),
                resultColor.getBlue(),
                (int) Mth.lerp((float) color1.getAlpha(), (float) color2.getAlpha(), amount));
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        return new Color(
                Mth.lerp((float) color1.getRed(), (float) color2.getRed(), amount),
                Mth.lerp((float) color1.getGreen(), (float) color2.getGreen(), amount),
                Mth.lerp((float) color1.getBlue(), (float) color2.getBlue(), amount),
                Mth.lerp((float) color1.getAlpha(), (float) color2.getAlpha(), amount));
    }

    public static int gradient2(int color1, int color2, int speed, int index) {
        Color col1 = new Color(color1);
        Color col2 = new Color(color2);
        double angle = (System.currentTimeMillis() / speed + index) % 360L;
        double var13;
        float ratio = (float) ((var13 = angle % 360.0) / 360.0);
        int red = (int) (col1.getRed() * (1.0F - ratio) + col2.getRed() * ratio);
        int green = (int) (col1.getGreen() * (1.0F - ratio) + col2.getGreen() * ratio);
        int blue = (int) (col1.getBlue() * (1.0F - ratio) + col2.getBlue() * ratio);
        Color interpolatedColor = new Color(red, green, blue);
        return interpolatedColor.getRGB();
    }

    public static int interpolate(int color1, int color2, double amount) {
        amount = Math.max(0.0, Math.min(1.0, amount));

        int a1 = (color1 >> 24) & 0xFF, a2 = (color2 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF, r2 = (color2 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF,  g2 = (color2 >> 8) & 0xFF;
        int b1 = color1 & 0xFF,         b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * amount);
        int r = (int) (r1 + (r2 - r1) * amount);
        int g = (int) (g1 + (g2 - g1) * amount);
        int b = (int) (b1 + (b2 - b1) * amount);

        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int[] getRainbowColor(int speed) {
        int[] color1 = new int[4];
        if (speed == 0) {
            speed = 1;
        }

        color1[0] = rainbow(speed, 1, 1.0F, 1.0F, 1.0F);
        color1[1] = rainbow(speed, 90, 1.0F, 1.0F, 1.0F);
        color1[2] = rainbow(speed, 180, 1.0F, 1.0F, 1.0F);
        color1[3] = rainbow(speed, 270, 1.0F, 1.0F, 1.0F);
        return color1;
    }

    public static int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
        float hue = angle / 360.0F;
        int color = Color.HSBtoRGB(hue, saturation, brightness);
        return rgba(red(color), green(color), blue(color), Math.max(0, Math.min(255, (int) (opacity * 255.0F))));
    }

    public static int gradient(int speed, int index, int... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int colorIndex = (int) (angle / 360.0F * colors.length);
        if (colorIndex == colors.length) {
            colorIndex--;
        }

        int color1 = colors[colorIndex];
        int color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];
        return interpolateColor(color1, color2, angle / 360.0F * colors.length - colorIndex);
    }

    public static int interpolateColor(int color1, int color2, double offset) {
        float[] rgba1 = getRGBAf(color1);
        float[] rgba2 = getRGBAf(color2);
        double r = rgba1[0] + (rgba2[0] - rgba1[0]) * offset;
        double g = rgba1[1] + (rgba2[1] - rgba1[1]) * offset;
        double b = rgba1[2] + (rgba2[2] - rgba1[2]) * offset;
        double a = rgba1[3] + (rgba2[3] - rgba1[3]) * offset;
        return rgba((int) (r * 255.0), (int) (g * 255.0), (int) (b * 255.0), (int) (a * 255.0));
    }

    public static float[] getRGBAf(int c) {
        return new float[] { red(c) / 255.0F, green(c) / 255.0F, blue(c) / 255.0F, alpha(c) / 255.0F };
    }

    public static int skyRainbow(int speed, int index) {
        double angle = (int) ((System.currentTimeMillis() / speed + index) % 360L);
        double var4;
        return Color
                .getHSBColor((var4 = angle % 360.0) / 360.0 < 0.5 ? -((float) (var4 / 360.0)) : (float) (var4 / 360.0),
                        0.5F, 1.0F)
                .hashCode();
    }

    public static int[] getAstolfoColor(int speed) {
        int[] color1 = new int[4];
        if (speed == 0) {
            int var2 = 1;
        }

        color1[0] = skyRainbow(25, 1);
        color1[1] = skyRainbow(25, 90);
        color1[2] = skyRainbow(25, 180);
        color1[3] = skyRainbow(25, 270);
        return color1;
    }

    public static int applyOpacity(int n, float f) {
        return rgba(getRedInt(n), getGreenInt(n), getBlueInt(n), (int) (getAlphaInt(n) * f / 255.0F));
    }

    public static int getRedInt(int n) {
        return n >> 16 & 0xFF;
    }

    public static int getGreenInt(int n) {
        return n >> 8 & 0xFF;
    }

    public static int getBlueInt(int n) {
        return n & 0xFF;
    }

    public static int getAlphaInt(int n) {
        return n >> 24 & 0xFF;
    }

    public static float[] getColorComps(Color color) {
        return new float[] { color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F,
                color.getAlpha() / 255.0F };
    }

    public static int getClientColorOne(int speed, int index) {
        return gradient(
                interpolate(Gui.selectedTheme.getMain().getRGB(), Gui.preSelectedTheme.getMain().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                interpolate(Gui.selectedTheme.getMain().getRGB(), Gui.preSelectedTheme.getMain().getRGB(),
                        1.0F - Gui.animation14.getOutput()),
                speed,
                index);
    }

    public static int rgb(int r, int g, int b) {
        return 255 << 24 | r << 16 | g << 8 | b;
    }
    public static int rgba(int r, int g, int b, double a) {
        return (int)a << 24 | r << 16 | g << 8 | b;
    }

    public static int replAlpha(int c, int a) {
        return rgba(red(c), green(c), blue(c), a);
    }

    public int red(int c) {
        return c >> 16 & 0xFF;
    }

    public int green(int c) {
        return c >> 8 & 0xFF;
    }

    public int blue(int c) {
        return c & 0xFF;
    }

    public int alpha(int c) {
        return c >> 24 & 0xFF;
    }

    public int rgba(int red, int green, int blue, int alpha) {
        ColorKey key = new ColorKey(red, green, blue, alpha);
        CacheEntry cacheEntry = colorCache.computeIfAbsent(key, k -> {
            CacheEntry newEntry = new CacheEntry(k, computeColor(red, green, blue, alpha), CACHE_EXPIRATION_TIME);
            cleanupQueue.offer(newEntry);
            return newEntry;
        });
        return cacheEntry.getColor();
    }

    public int computeColor(int red, int green, int blue, int alpha) {
        return ((Mth.clamp(alpha, 0, 255) << 24) |
                (Mth.clamp(red, 0, 255) << 16) |
                (Mth.clamp(green, 0, 255) << 8) |
                Mth.clamp(blue, 0, 255));
    }

    public static int getRedFromColor(int color) {
        return color >> 16 & 0xFF;
    }

    public static int getGreenFromColor(int color) {
        return color >> 8 & 0xFF;
    }

    public static int getBlueFromColor(int color) {
        return color & 0xFF;
    }

    public static int getAlphaFromColor(int color) {
        return color >> 24 & 0xFF;
    }

    public static float[] rgb(int color) {
        return new float[] { (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                (color >> 24 & 0xFF) / 255.0F };
    }

    public int colorToHex(Color color) {
        int a = color.getAlpha();
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        return a << 24 | r << 16 | g << 8 | b;
    }

    public float[] rgba(int color) {
        return new float[] { (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                (color >> 24 & 0xFF) / 255.0F };
    }

    public int overCol(int color1, int color2, float percent01) {
        final float percent = clamp(percent01, 0F, 1F);
        return rgba(
                lerp(percent, red(color1), red(color2)),
                lerp(percent, green(color1), green(color2)),
                lerp(percent, blue(color1), blue(color2)),
                lerp(percent, alpha(color1), alpha(color2))
        );
    }
    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }
    public static int lerp(float delta, int start, int end) {
        return start + floor(delta * (float)(end - start));
    }
    public static int floor(float value) {
        int i = (int) value;
        return value < (float) i ? i - 1 : i;
    }

    public static int multAlpha(int color, float percent01) {
        return rgba(red(color), green(color), blue(color), Math.round(alpha(color) * percent01));
    }

    public static int argb(int alpha, int red, int green, float blue) {
        int b = Math.round(Math.max(0f, Math.min(blue, 255f)));

        return (alpha << 24) | (red << 16) | (green << 8) | b;
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    private static class ColorKey {
        final int red, green, blue, alpha;
    }

    @Getter
    private static class CacheEntry implements Delayed {
        private final ColorKey key;
        private final int color;
        private final long expirationTime;

        CacheEntry(ColorKey key, int color, long ttl) {
            this.key = key;
            this.color = color;
            this.expirationTime = System.currentTimeMillis() + ttl;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delay = expirationTime - System.currentTimeMillis();
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other instanceof CacheEntry) {
                return Long.compare(this.expirationTime, ((CacheEntry) other).expirationTime);
            }
            return 0;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

    }
}