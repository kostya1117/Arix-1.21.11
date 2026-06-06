package ru.arixcompany.utils.render.font;

import lombok.Getter;
import ru.arixcompany.utils.MessageSender;

import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FontManager {
    private static final Map<String, CustomFont> fonts = new ConcurrentHashMap<>();

    /**
     * Системный fallback-шрифт — подбирается под ОС
     */
    @Getter
    private static Font commonFont;

    /**
     * CJK fallback — для китайских/японских/корейских символов
     */
    @Getter
    private static Font cjkFont;

    static {
        String os = System.getProperty("os.name", "").toLowerCase();

        // Общий fallback
        if (os.contains("win")) {
            commonFont = new Font("Segoe UI", Font.PLAIN, 1);
        } else if (os.contains("mac")) {
            commonFont = new Font("Helvetica", Font.PLAIN, 1);
        } else {
            commonFont = new Font("DejaVu Sans", Font.PLAIN, 1);
        }

        // CJK fallback
        if (os.contains("win")) {
            cjkFont = new Font("Microsoft YaHei", Font.PLAIN, 1);
        } else if (os.contains("mac")) {
            cjkFont = new Font("PingFang SC", Font.PLAIN, 1);
        } else {
            cjkFont = tryFont("Noto Sans CJK SC",
                    "Noto Sans CJK",
                    "WenQuanYi Micro Hei",
                    "Droid Sans Fallback");
        }
    }

    private static Font tryFont(String... names) {
        for (String name : names) {
            return new Font(name, Font.PLAIN, 1);
        }
        return null;
    }

    @Getter
    public enum Fonts {
        SF_MEDIUM("/assets/arix/fonts/sf_medium.ttf"),
        SF_SEMIBOLD("/assets/arix/fonts/sf_semibold.ttf"),
        SF_REGULAR("/assets/arix/fonts/sf_regular.ttf"),
        SF_BOLD("/assets/arix/fonts/sf_bold.ttf"),
        ICONS("/assets/arix/fonts/icons.ttf");

        private final String path;

        Fonts(String path) {
            this.path = path;
        }
    }

    private static final float DEFAULT_SIZE = 16.0f;

    public static CustomFont get(Fonts font) {
        return get(font, DEFAULT_SIZE);
    }

    public static CustomFont get(Fonts font, float size) {
        float roundedSize = Math.round(size * 2.0f) / 2.0f;
        String key = font.name() + "_" + roundedSize;

        return fonts.computeIfAbsent(key, k -> {
            try {
                return new CustomFont(font.getPath(), roundedSize);
            } catch (IOException e) {
                MessageSender.error("Ошибка при загрузке шрифта '"
                        + font.name() + "' размер " + roundedSize + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    public static CustomFont get(float size) {
        return get(Fonts.SF_MEDIUM, size);
    }

    public static void init() {
        for (Fonts font : Fonts.values()) {
            for (int size = 1; size <= 50; size++) {
                get(font, size);
            }
        }
    }
}