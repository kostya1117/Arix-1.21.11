package ru.arixcompany.features.module;

import lombok.Getter;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;

import java.awt.*;

@Getter
public enum Theme {

    PURPLE(
            new Color(176, 102, 255),
            new Color(18, 14, 28),
            new Color(26, 20, 38),
            new Color(176, 102, 255),
            new Color(235, 220, 255),
            new Color(140, 120, 180)
    ),

    CYAN(
            new Color(0, 255, 255),
            new Color(10, 22, 28),
            new Color(16, 32, 40),
            new Color(0, 255, 255),
            new Color(200, 255, 255),
            new Color(80, 180, 190)
    ),

    PINK(
            new Color(255, 105, 180),
            new Color(28, 14, 22),
            new Color(38, 20, 30),
            new Color(255, 105, 180),
            new Color(255, 220, 235),
            new Color(180, 120, 150)
    ),

    ORANGE(
            new Color(255, 140, 50),
            new Color(28, 18, 10),
            new Color(40, 26, 16),
            new Color(255, 140, 50),
            new Color(255, 230, 200),
            new Color(180, 130, 90)
    ),

    YELLOW(
            new Color(255, 220, 50),
            new Color(26, 24, 12),
            new Color(36, 34, 18),
            new Color(255, 220, 50),
            new Color(255, 250, 200),
            new Color(180, 165, 90)
    ),

    GREEN(
            new Color(80, 220, 100),
            new Color(12, 24, 14),
            new Color(18, 34, 20),
            new Color(80, 220, 100),
            new Color(200, 255, 210),
            new Color(100, 160, 110)
    ),

    RED(
            new Color(255, 60, 60),
            new Color(28, 12, 12),
            new Color(40, 18, 18),
            new Color(255, 60, 60),
            new Color(255, 210, 210),
            new Color(180, 100, 100)
    ),

    BLUE(
            new Color(70, 130, 255),
            new Color(12, 16, 28),
            new Color(18, 24, 40),
            new Color(70, 130, 255),
            new Color(200, 220, 255),
            new Color(100, 130, 180)
    ),

    TEAL(
            new Color(0, 200, 180),
            new Color(10, 24, 24),
            new Color(16, 34, 34),
            new Color(0, 200, 180),
            new Color(180, 255, 245),
            new Color(80, 160, 150)
    ),

    LIME(
            new Color(180, 255, 50),
            new Color(18, 26, 10),
            new Color(26, 36, 16),
            new Color(180, 255, 50),
            new Color(230, 255, 180),
            new Color(140, 180, 80)
    ),

    INDIGO(
            new Color(100, 80, 220),
            new Color(14, 12, 28),
            new Color(22, 18, 40),
            new Color(100, 80, 220),
            new Color(210, 200, 255),
            new Color(120, 110, 170)
    ),

    CORAL(
            new Color(255, 127, 100),
            new Color(28, 16, 14),
            new Color(40, 24, 20),
            new Color(255, 127, 100),
            new Color(255, 225, 215),
            new Color(180, 130, 120)
    ),

    GRAY(
            new Color(160, 165, 180),
            new Color(20, 20, 24),
            new Color(30, 30, 36),
            new Color(160, 165, 180),
            new Color(230, 232, 240),
            new Color(120, 125, 140)
    ),

    MINT(
            new Color(100, 255, 180),
            new Color(12, 26, 20),
            new Color(18, 36, 28),
            new Color(100, 255, 180),
            new Color(200, 255, 230),
            new Color(90, 170, 140)
    ),

    GOLD(
            new Color(255, 195, 50),
            new Color(28, 22, 10),
            new Color(40, 32, 16),
            new Color(255, 195, 50),
            new Color(255, 245, 200),
            new Color(190, 160, 90)
    );

    public Animation animation = new EaseInOutQuad(300, 1.0);

    private final Color main;
    private final Color bg;
    private final Color bg2;
    private final Color outline;
    private final Color text;
    private final Color text2;

    private Theme(Color main, Color bg, Color bg2, Color outline, Color text, Color text2) {
        this.main = main;
        this.bg = bg;
        this.bg2 = bg2;
        this.outline = outline;
        this.text = text;
        this.text2 = text2;
    }
}