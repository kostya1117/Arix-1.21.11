package ru.arixcompany.utils.animation;

import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * Улучшенный класс интерполяции.
 * Оптимизирован расход ресурсов (замена Math.pow на умножение)
 * и улучшена структура кода.
 */
public final class Interpolation {
    private static final double BACK_C1 = 1.70158;
    private static final double BACK_C2 = BACK_C1 * 1.525;
    private static final double BACK_C3 = BACK_C1 + 1.0;

    private static final double ELASTIC_C4 = (2.0 * Mth.PI) / 3.0;
    private static final double ELASTIC_C5 = (2.0 * Mth.PI) / 4.5;

    private static final double HALF_PI = Mth.PI * 0.5;
    private static final double BOUNCE_N1 = 7.5625;
    private static final double BOUNCE_D1 = 2.75;

    private Interpolation() {} // Утилитный класс не должен наследоваться или создаваться

    /**
     * Основной метод интерполяции значения.
     */
    public static double interpolate(double from, double to, double percent, Type type, Ease ease) {
        if (!Double.isFinite(percent)) return from;
        if (percent <= 0.0) return from;
        if (percent >= 1.0) return to;
        if (from == to) return from;

        double t = getRawCoefficient(percent, type, ease);

        t = Math.clamp(t, -1.0, 1.0); //не знаю

        return Math.fma(to - from, t, from);
    }

    /**
     * Возвращает только коэффициент трансформации времени (от 0 до 1, иногда за пределами для Back/Elastic).
     */
    public static double getRawCoefficient(double percent, Type type, Ease ease) {
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(ease, "Ease cannot be null");

        return switch (type) {
            case LINEAR -> percent;
            case SINE -> sine(percent, ease);
            case QUAD -> quad(percent, ease);
            case CUBIC -> cubic(percent, ease);
            case QUART -> quart(percent, ease);
            case QUINT -> quint(percent, ease);
            case EXPO -> expo(percent, ease);
            case CIRC -> circ(percent, ease);
            case BACK -> back(percent, ease);
            case BOUNCE -> bounce(percent, ease);
            case ELASTIC -> elastic(percent, ease);
        };
    }

    private static double sine(double p, Ease ease) {
        return switch (ease) {
            case IN -> 1.0 - Mth.cos(p * HALF_PI);
            case OUT -> Mth.sin(p * HALF_PI);
            case IN_OUT -> 0.5 * (1.0 - Math.cos(Math.PI * p));
        };
    }

    private static double quad(double p, Ease ease) {
        return switch (ease) {
            case IN -> p * p;
            case OUT -> 1.0 - (1.0 - p) * (1.0 - p);
            case IN_OUT -> p < 0.5 ? 2.0 * p * p : 1.0 - Math.pow(-2.0 * p + 2.0, 2) * 0.5;
        };
    }

    private static double cubic(double p, Ease ease) {
        return switch (ease) {
            case IN -> p * p * p;
            case OUT -> 1.0 - (1.0 - p) * (1.0 - p) * (1.0 - p);
            case IN_OUT -> p < 0.5 ? 4.0 * p * p * p : 1.0 - Math.pow(-2.0 * p + 2.0, 3) * 0.5;
        };
    }

    private static double quart(double p, Ease ease) {
        return switch (ease) {
            case IN -> p * p * p * p;
            case OUT -> {
                double inv = 1.0 - p;
                yield 1.0 - (inv * inv * inv * inv);
            }
            case IN_OUT -> {
                if (p < 0.5) yield 8.0 * p * p * p * p;
                double inv = -2.0 * p + 2.0;
                yield 1.0 - (inv * inv * inv * inv) * 0.5;
            }
        };
    }

    private static double quint(double p, Ease ease) {
        return switch (ease) {
            case IN -> p * p * p * p * p;
            case OUT -> 1.0 - Math.pow(1.0 - p, 5);
            case IN_OUT -> p < 0.5 ? 16.0 * p * p * p * p * p : 1.0 - Math.pow(-2.0 * p + 2.0, 5) * 0.5;
        };
    }

    private static double expo(double p, Ease ease) {
        return switch (ease) {
            case IN -> p == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * p - 10.0);
            case OUT -> p == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * p);
            case IN_OUT -> {
                if (p == 0.0) yield 0.0;
                if (p == 1.0) yield 1.0;
                yield p < 0.5
                        ? 0.5 * Math.pow(2.0, 20.0 * p - 10.0)
                        : 1.0 - 0.5 * Math.pow(2.0, -20.0 * p + 10.0);
            }
        };
    }

    private static double circ(double p, Ease ease) {
        return switch (ease) {
            case IN -> 1.0 - Math.sqrt(1.0 - p * p);
            case OUT -> Math.sqrt(1.0 - (p - 1.0) * (p - 1.0));
            case IN_OUT -> p < 0.5
                    ? 0.5 * (1.0 - Math.sqrt(1.0 - (2.0 * p) * (2.0 * p)))
                    : 0.5 * (Math.sqrt(1.0 - Math.pow(-2.0 * p + 2.0, 2)) + 1.0);
        };
    }

    private static double back(double p, Ease ease) {
        return switch (ease) {
            case IN -> BACK_C3 * p * p * p - BACK_C1 * p * p;
            case OUT -> 1.0 + BACK_C3 * Math.pow(p - 1.0, 3) + BACK_C1 * Math.pow(p - 1.0, 2);
            case IN_OUT -> p < 0.5
                    ? (Math.pow(2.0 * p, 2) * ((BACK_C2 + 1.0) * 2.0 * p - BACK_C2)) * 0.5
                    : (Math.pow(2.0 * p - 2.0, 2) * ((BACK_C2 + 1.0) * (p * 2.0 - 2.0) + BACK_C2) + 2.0) * 0.5;
        };
    }

    private static double bounce(double p, Ease ease) {
        return switch (ease) {
            case IN -> 1.0 - bounceOut(1.0 - p);
            case OUT -> bounceOut(p);
            case IN_OUT -> p < 0.5
                    ? 0.5 * (1.0 - bounceOut(1.0 - 2.0 * p))
                    : 0.5 * (1.0 + bounceOut(2.0 * p - 1.0));
        };
    }

    private static double bounceOut(double t) {
        if (t < 1.0 / BOUNCE_D1) {
            return BOUNCE_N1 * t * t;
        } else if (t < 2.0 / BOUNCE_D1) {
            return BOUNCE_N1 * (t -= 1.5 / BOUNCE_D1) * t + 0.75;
        } else if (t < 2.5 / BOUNCE_D1) {
            return BOUNCE_N1 * (t -= 2.25 / BOUNCE_D1) * t + 0.9375;
        } else {
            return BOUNCE_N1 * (t -= 2.625 / BOUNCE_D1) * t + 0.984375;
        }
    }

    private static double elastic(double p, Ease ease) {
        if (p == 0.0 || p == 1.0) return p;
        return switch (ease) {
            case IN -> -Math.pow(2.0, 10.0 * p - 10.0) * Mth.sin((p * 10.0 - 10.75) * ELASTIC_C4);
            case OUT -> Math.pow(2.0, -10.0 * p) * Mth.sin((p * 10.0 - 0.75) * ELASTIC_C4) + 1.0;
            case IN_OUT -> p < 0.5
                    ? -0.5 * Math.pow(2.0, 20.0 * p - 10.0) * Mth.sin((20.0 * p - 11.125) * ELASTIC_C5)
                    : 0.5 * Math.pow(2.0, -20.0 * p + 10.0) * Mth.sin((20.0 * p - 11.125) * ELASTIC_C5) + 1.0;
        };
    }

    public enum Type {
        LINEAR, SINE, QUAD, CUBIC, QUART, QUINT, EXPO, CIRC, BACK, BOUNCE, ELASTIC
    }

    public enum Ease {
        IN, OUT, IN_OUT
    }
}