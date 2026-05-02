package ru.arixcompany.utils.render.font;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FontManager {

    // ConcurrentHashMap на случай обращений из разных потоков
    private static final Map<String, CustomFont> fonts = new ConcurrentHashMap<>();

    // ============================================================
    // Доступные шрифты
    // ============================================================

    public enum Fonts {
        SF("/assets/arix/fonts/sfmedium.otf");

        private final String path;

        Fonts(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    private static final float DEFAULT_SIZE = 16.0f;

    // ============================================================
    // Получение шрифта
    // ============================================================

    /**
     * Получить шрифт с размером по умолчанию (16px).
     */
    public static CustomFont get(Fonts font) {
        return get(font, DEFAULT_SIZE);
    }

    /**
     * Получить шрифт указанного размера. Кэшируется по имени + размеру.
     * Размер округляется до 0.5px для разумного кэширования.
     */
    public static CustomFont get(Fonts font, float size) {
        // Округляем до 0.5 для кэша (избегаем бесконечного количества ключей)
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

    // ============================================================
    // Инициализация и очистка
    // ============================================================

    /**
     * Предзагрузка часто используемых размеров.
     * Вызывать при старте клиента (в главном потоке OpenGL).
     */
    public static void init() {
        // Загружаем только реально нужные размеры, а не все 30
        float[] commonSizes = {7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 24};

        for (Fonts font : Fonts.values()) {
            for (float size : commonSizes) {
                CustomFont loaded = get(font, size);
                if (loaded != null) {
                    System.out.println("[FontManager] Preloaded: "
                            + font.name() + " @ " + size + "px");
                }
            }
        }
    }

    /**
     * Очистка всех шрифтов. Вызывать при закрытии клиента.
     */
    public static void cleanup() {
        fonts.values().forEach(font -> {
            if (font != null) {
                try {
                    font.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        fonts.clear();
    }

    /**
     * Перезагрузка всех шрифтов (например, при смене ресурспаков).
     */
    public static void reload() {
        cleanup();
        init();
    }
}