package ru.arixcompany.utils.math;

import net.minecraft.util.Mth;

import java.util.Map;

public class Interpolate {
    public static final float EPSILON = 1.0E-4F;

    @FunctionalInterface
    public interface Curve { float shape(float t); }

    public record StepProfile(Curve curve, float base, float minFactor, float minFloor, float overshoot) {}

    public static final Map<String, StepProfile> PROFILES = Map.ofEntries(
            Map.entry("Линейное", new StepProfile(InterpCurves::linear, 1.00F, 0.00F, 0.00F, 1.00F)),
            Map.entry("Синусоида (Вход)",       new StepProfile(InterpCurves::easeInSine,      0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Синусоида (Выход)",      new StepProfile(InterpCurves::easeOutSine,     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Синусоида (Вход/Выход)", new StepProfile(InterpCurves::easeInOutSine,   0.25F, 0.15F, 0.00F, 1.00F)),

            // Квадратичные (Quad)
            Map.entry("Квадратичное (Вход)",       new StepProfile(InterpCurves::easeInQuad,      0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квадратичное (Выход)",      new StepProfile(InterpCurves::easeOutQuad,     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квадратичное (Вход/Выход)", new StepProfile(InterpCurves::easeInOutQuad,   0.25F, 0.15F, 0.00F, 1.00F)),

            // Кубические (Cubic)
            Map.entry("Кубическое (Вход)",       new StepProfile(InterpCurves::easeInCubic,     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Кубическое (Выход)",      new StepProfile(InterpCurves::easeOutCubic,    0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Кубическое (Вход/Выход)", new StepProfile(InterpCurves::easeInOutCubic,  0.25F, 0.15F, 0.00F, 1.00F)),

            // Квартичные (Quart)
            Map.entry("Квартичное (Вход)",       new StepProfile(InterpCurves::easeInQuart,     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квартичное (Выход)",      new StepProfile(InterpCurves::easeOutQuart,    0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квартичное (Вход/Выход)", new StepProfile(InterpCurves::easeInOutQuart,  0.25F, 0.15F, 0.00F, 1.00F)),

            // Квинтичные (Quint)
            Map.entry("Квинтичное (Вход)",       new StepProfile(InterpCurves::easeInQuint,     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квинтичное (Выход)",      new StepProfile(InterpCurves::easeOutQuint,    0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Квинтичное (Вход/Выход)", new StepProfile(InterpCurves::easeInOutQuint,  0.25F, 0.15F, 0.00F, 1.00F)),

            // Экспоненциальные (Expo)
            Map.entry("Экспонента (Вход)",       new StepProfile(InterpCurves::easeInExpo,      0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Экспонента (Выход)",      new StepProfile(InterpCurves::easeOutExpo,     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Экспонента (Вход/Выход)", new StepProfile(InterpCurves::easeInOutExpo,   0.25F, 0.15F, 0.00F, 1.00F)),

            // Круговые (Circ)
            Map.entry("Круговое (Вход)",       new StepProfile(InterpCurves::easeInCirc,      0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Круговое (Выход)",      new StepProfile(InterpCurves::easeOutCirc,     0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Круговое (Вход/Выход)", new StepProfile(InterpCurves::easeInOutCirc,   0.25F, 0.15F, 0.00F, 1.00F)),

            // С замахом (Back)
            Map.entry("Замах (Вход)",       new StepProfile(InterpCurves::easeInBack,      0.25F, 0.15F, 0.00F, 1.05F)),
            Map.entry("Замах (Выход)",      new StepProfile(InterpCurves::easeOutBack,     0.25F, 0.15F, 0.00F, 1.05F)),
            Map.entry("Замах (Вход/Выход)", new StepProfile(InterpCurves::easeInOutBack,   0.25F, 0.15F, 0.00F, 1.05F)),

            // Пружинистые (Elastic)
            Map.entry("Пружина (Вход)",       new StepProfile(InterpCurves::easeInElastic,   0.25F, 0.15F, 0.00F, 1.15F)),
            Map.entry("Пружина (Выход)",      new StepProfile(InterpCurves::easeOutElastic,  0.25F, 0.15F, 0.00F, 1.15F)),
            Map.entry("Пружина (Вход/Выход)", new StepProfile(InterpCurves::easeInOutElastic,0.25F, 0.15F, 0.00F, 1.15F)),

            // Отскок (Bounce)
            Map.entry("Отскок (Вход)",       new StepProfile(InterpCurves::easeInBounce,    0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Отскок (Выход)",      new StepProfile(InterpCurves::easeOutBounce,   0.25F, 0.15F, 0.00F, 1.00F)),
            Map.entry("Отскок (Вход/Выход)", new StepProfile(InterpCurves::easeInOutBounce, 0.25F, 0.15F, 0.00F, 1.00F))
    );

    public static float applyStep(float delta, float maxStep, StepProfile p) {
        float absDelta = Math.abs(delta);

        float t = Mth.clamp(absDelta / Math.max(maxStep, EPSILON), 0.0F, 1.0F);
        float curved = p.curve().shape(t);

        float minStep = Math.max(maxStep * p.minFactor(), p.minFloor());
        minStep = Math.min(minStep, absDelta);

        float target = maxStep * (p.base() + curved * (1.0F - p.base()));
        return Math.min(absDelta * p.overshoot(), Math.max(minStep, target));
    }

    public static final class InterpCurves {
        private InterpCurves() {}

        public static float linear(float t) { return t; }

        // Sine
        public static float easeInSine(float t) { return 1.0F - Mth.cos((t * Mth.PI) / 2.0F); }
        public static float easeOutSine(float t) { return Mth.sin((t * Mth.PI) / 2.0F); }
        public static float easeInOutSine(float t) { return (float) (-(Mth.cos(Mth.PI * t) - 1.0) / 2.0); }

        // Quad
        public static float easeInQuad(float t) { return t * t; }
        public static float easeOutQuad(float t) { return 1.0F - (1.0F - t) * (1.0F - t); }
        public static float easeInOutQuad(float t) { return t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2.0) / 2.0F; }

        // Cubic
        public static float easeInCubic(float t) { return t * t * t; }
        public static float easeOutCubic(float t) { return 1.0F - (float) Math.pow(1.0F - t, 3.0); }
        public static float easeInOutCubic(float t) { return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F; }

        // Quart
        public static float easeInQuart(float t) { return t * t * t * t; }
        public static float easeOutQuart(float t) { return 1.0F - (float) Math.pow(1.0F - t, 4.0); }
        public static float easeInOutQuart(float t) { return t < 0.5F ? 8.0F * t * t * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 4.0) / 2.0F; }

        // Quint
        public static float easeInQuint(float t) { return t * t * t * t * t; }
        public static float easeOutQuint(float t) { return 1.0F - (float) Math.pow(1.0F - t, 5.0); }
        public static float easeInOutQuint(float t) { return t < 0.5F ? 16.0F * t * t * t * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 5.0) / 2.0F; }

        // Expo
        public static float easeInExpo(float t) { return t == 0.0F ? 0.0F : (float) Math.pow(2.0, 10.0F * t - 10.0F); }
        public static float easeOutExpo(float t) { return t == 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0F * t); }
        public static float easeInOutExpo(float t) {
            if (t == 0.0F) return 0.0F;
            if (t == 1.0F) return 1.0F;
            return t < 0.5F
                    ? (float) Math.pow(2.0, 20.0F * t - 10.0F) / 2.0F
                    : (2.0F - (float) Math.pow(2.0, -20.0F * t + 10.0F)) / 2.0F;
        }

        // Circ
        public static float easeInCirc(float t) { return 1.0F - Mth.sqrt(1.0F - (float) Math.pow(t, 2.0)); }
        public static float easeOutCirc(float t) { return Mth.sqrt(1.0F - (float) Math.pow(t - 1.0F, 2.0)); }
        public static float easeInOutCirc(float t) {
            return t < 0.5F
                    ? (1.0F - Mth.sqrt(1.0F - (float) Math.pow(2.0F * t, 2.0))) / 2.0F
                    : (Mth.sqrt(1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2.0)) + 1.0F) / 2.0F;
        }

        // Back
        public static float easeInBack(float t) {
            float c1 = 1.70158F;
            float c3 = c1 + 1.0F;
            return c3 * t * t * t - c1 * t * t;
        }
        public static float easeOutBack(float t) {
            float c1 = 1.70158F;
            float c3 = c1 + 1.0F;
            return 1.0F + c3 * (float) Math.pow(t - 1.0F, 3.0) + c1 * (float) Math.pow(t - 1.0F, 2.0);
        }
        public static float easeInOutBack(float t) {
            float c1 = 1.70158F;
            float c2 = c1 * 1.525F;
            return t < 0.5F
                    ? (float) (Math.pow(2.0F * t, 2.0) * ((c2 + 1.0F) * 2.0F * t - c2)) / 2.0F
                    : (float) (Math.pow(2.0F * t - 2.0F, 2.0) * ((c2 + 1.0F) * (t * 2.0F - 2.0F) + c2) + 2.0F) / 2.0F;
        }

        // Elastic
        public static float easeInElastic(float t) {
            float c4 = (2.0F * Mth.PI) / 3.0F;
            if (t == 0.0F) return 0.0F;
            if (t == 1.0F) return 1.0F;
            return -(float) Math.pow(2.0, 10.0F * t - 10.0F) * Mth.sin((t * 10.0F - 10.75F) * c4);
        }
        public static float easeOutElastic(float t) {
            float c4 = (2.0F * Mth.PI) / 3.0F;
            if (t == 0.0F) return 0.0F;
            if (t == 1.0F) return 1.0F;
            return (float) Math.pow(2.0, -10.0F * t) * Mth.sin((t * 10.0F - 0.75F) * c4) + 1.0F;
        }
        public static float easeInOutElastic(float t) {
            float c5 = (2.0F * Mth.PI) / 4.5F;
            if (t == 0.0F) return 0.0F;
            if (t == 1.0F) return 1.0F;
            return t < 0.5F
                    ? -((float) Math.pow(2.0, 20.0F * t - 10.0F) * Mth.sin((20.0F * t - 11.125F) * c5)) / 2.0F
                    : ((float) Math.pow(2.0, -20.0F * t + 10.0F) * Mth.sin((20.0F * t - 11.125F) * c5)) / 2.0F + 1.0F;
        }

        // Bounce
        public static float easeInBounce(float t) {
            return 1.0F - easeOutBounce(1.0F - t);
        }
        public static float easeOutBounce(float t) {
            float n1 = 7.5625F;
            float d1 = 2.75F;

            if (t < 1.0F / d1) {
                return n1 * t * t;
            } else if (t < 2.0F / d1) {
                t -= 1.5F / d1;
                return n1 * t * t + 0.75F;
            } else if (t < 2.5F / d1) {
                t -= 2.25F / d1;
                return n1 * t * t + 0.9375F;
            } else {
                t -= 2.625F / d1;
                return n1 * t * t + 0.984375F;
            }
        }
        public static float easeInOutBounce(float t) {
            return t < 0.5F
                    ? (1.0F - easeOutBounce(1.0F - 2.0F * t)) / 2.0F
                    : (1.0F + easeOutBounce(2.0F * t - 1.0F)) / 2.0F;
        }
    }
}