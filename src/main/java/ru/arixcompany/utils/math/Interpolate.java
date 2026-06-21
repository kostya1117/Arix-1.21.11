package ru.arixcompany.utils.math;

import net.minecraft.util.Mth;
import penner.easing.*; // Импортируем все классы анимаций

import java.util.Map;

public class Interpolate {
    public static final float EPSILON = 1.0E-4F;

    @FunctionalInterface
    public interface Curve { float shape(float t); }

    public record StepProfile(Curve curve, float base, float minFactor, float minFloor, float overshoot) {}

    // Карта профилей, где мы вызываем методы Penner напрямую через лямбды t -> Method(t, 0, 1, 1)
    public static final Map<String, StepProfile> PROFILES = Map.ofEntries(
            Map.entry("Линейное", new StepProfile(t -> Linear.easeNone(t, 0, 1, 1), 1.00F, 0.00F, 0.00F, 1.00F)),

            // Синусоида (Sine)
            Map.entry("Синусоида (Вход)",       new StepProfile(t -> Sine.easeIn(t, 0, 1, 1),      0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Синусоида (Выход)",      new StepProfile(t -> Sine.easeOut(t, 0, 1, 1),     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Синусоида (Вход/Выход)", new StepProfile(t -> Sine.easeInOut(t, 0, 1, 1),   0.25F, 0.15F, 0.00F, 1.00F)),

            // Квадратичные (Quad)
            Map.entry("Квадратичное (Вход)",       new StepProfile(t -> Quad.easeIn(t, 0, 1, 1),      0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квадратичное (Выход)",      new StepProfile(t -> Quad.easeOut(t, 0, 1, 1),     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квадратичное (Вход/Выход)", new StepProfile(t -> Quad.easeInOut(t, 0, 1, 1),   0.25F, 0.15F, 0.00F, 1.00F)),

            // Кубические (Cubic)
            Map.entry("Кубическое (Вход)",       new StepProfile(t -> Cubic.easeIn(t, 0, 1, 1),     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Кубическое (Выход)",      new StepProfile(t -> Cubic.easeOut(t, 0, 1, 1),    0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Кубическое (Вход/Выход)", new StepProfile(t -> Cubic.easeInOut(t, 0, 1, 1),  0.25F, 0.15F, 0.00F, 1.00F)),

            // Квартичные (Quart)
            Map.entry("Квартичное (Вход)",       new StepProfile(t -> Quart.easeIn(t, 0, 1, 1),     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квартичное (Выход)",      new StepProfile(t -> Quart.easeOut(t, 0, 1, 1),    0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квартичное (Вход/Выход)", new StepProfile(t -> Quart.easeInOut(t, 0, 1, 1),  0.25F, 0.15F, 0.00F, 1.00F)),

            // Квинтичные (Quint)
            Map.entry("Квинтичное (Вход)",       new StepProfile(t -> Quint.easeIn(t, 0, 1, 1),     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квинтичное (Выход)",      new StepProfile(t -> Quint.easeOut(t, 0, 1, 1),    0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квинтичное (Вход/Выход)", new StepProfile(t -> Quint.easeInOut(t, 0, 1, 1),  0.25F, 0.15F, 0.00F, 1.00F)),

            // Экспоненциальные (Expo)
            Map.entry("Экспонента (Вход)",       new StepProfile(t -> Expo.easeIn(t, 0, 1, 1),      0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Экспонента (Выход)",      new StepProfile(t -> Expo.easeOut(t, 0, 1, 1),     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Экспонента (Вход/Выход)", new StepProfile(t -> Expo.easeInOut(t, 0, 1, 1),   0.25F, 0.15F, 0.00F, 1.00F)),

            // Круговые (Circ)
            Map.entry("Круговое (Вход)",       new StepProfile(t -> Circ.easeIn(t, 0, 1, 1),      0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Круговое (Выход)",      new StepProfile(t -> Circ.easeOut(t, 0, 1, 1),     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Круговое (Вход/Выход)", new StepProfile(t -> Circ.easeInOut(t, 0, 1, 1),   0.25F, 0.15F, 0.00F, 1.00F)),

            // С замахом (Back)
            Map.entry("Замах (Вход)",       new StepProfile(t -> Back.easeIn(t, 0, 1, 1),      0.25F, 0.15F, 0.00F, 1.05F)),
            Map.entry("Замах (Выход)",      new StepProfile(t -> Back.easeOut(t, 0, 1, 1),     0.25F, 0.15F, 0.00F, 1.05F)),
            Map.entry("Замах (Вход/Выход)", new StepProfile(t -> Back.easeInOut(t, 0, 1, 1),   0.25F, 0.15F, 0.00F, 1.05F)),

            // Пружинистые (Elastic)
            Map.entry("Пружина (Вход)",       new StepProfile(t -> Elastic.easeIn(t, 0, 1, 1),   0.25F, 0.15F, 0.00F, 1.15F)),
            Map.entry("Пружина (Выход)",      new StepProfile(t -> Elastic.easeOut(t, 0, 1, 1),  0.25F, 0.15F, 0.00F, 1.15F)),
            Map.entry("Пружина (Вход/Выход)", new StepProfile(t -> Elastic.easeInOut(t, 0, 1, 1),0.25F, 0.15F, 0.00F, 1.15F)),

            // Отскок (Bounce)
            Map.entry("Отскок (Вход)",       new StepProfile(t -> Bounce.easeIn(t, 0, 1, 1),    0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Отскок (Выход)",      new StepProfile(t -> Bounce.easeOut(t, 0, 1, 1),   0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Отскок (Вход/Выход)", new StepProfile(t -> Bounce.easeInOut(t, 0, 1, 1), 0.25F, 0.15F, 0.00F, 1.00F))
    );

    public static float applyStep(float delta, float maxStep, StepProfile p) {
        float absDelta = Math.abs(delta);
        float t = Mth.clamp(absDelta / Math.max(maxStep, EPSILON), 0.0F, 1.0F);

        // Вызываем shape(t), который внутри дернет метод из penner.easing
        float curved = p.curve().shape(t);

        float minStep = Math.max(maxStep * p.minFactor(), p.minFloor());
        minStep = Math.min(minStep, absDelta);

        float target = maxStep * (p.base() + curved * (1.0F - p.base()));
        return Math.min(absDelta * p.overshoot(), Math.max(minStep, target));
    }
}