package ru.arixcompany.utils.render.font;

import lombok.Getter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FontManager {
    private static final Map<String, CustomFont> fonts = new ConcurrentHashMap<>();

    @Getter
    public enum Fonts {
        SF_MEDIUM("/assets/arix/fonts/sf_medium.ttf"),
        SF_SEMIBOLD("/assets/arix/fonts/sf_semibold.ttf"),
        SF_REGULAR("/assets/arix/fonts/sf_regular.ttf"),
        SF_BOLD("/assets/arix/fonts/sf_bold.ttf");

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
                System.err.println("[FontManager] Failed to load font '"
                        + font.name() + "' size " + roundedSize + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    public static CustomFont get(float size) {
        float roundedSize = Math.round(size * 2.0f) / 2.0f;
        String key = Fonts.SF_MEDIUM.name() + "_" + roundedSize;

        return fonts.computeIfAbsent(key, k -> {
            try {
                return new CustomFont(Fonts.SF_MEDIUM.getPath(), roundedSize);
            } catch (IOException e) {
                System.err.println("[FontManager] Failed to load font '"
                        + Fonts.SF_MEDIUM.name() + "' size " + roundedSize + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    public static void init() {
        for (Fonts font : Fonts.values()) {
            for (int size = 1; size <= 50; size++) {
                get(font, size);
            }
        }
    }
}