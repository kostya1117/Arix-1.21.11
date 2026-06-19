package ru.arixcompany.utils.animation;

import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;


public final class Interpolation {

    private static final int STEPS = 80;
    private static final int TABLE_SIZE = STEPS + 1;
    private static final double STEP_SIZE = 2.0 / STEPS;
    private static final double INV_STEP_SIZE = 1.0 / STEP_SIZE;

    private static final double[][][] TABLE;

    static {
        Curve[] curves = Curve.values();
        Mode[] modes = Mode.values();
        TABLE = new double[curves.length][modes.length][TABLE_SIZE];

        for (Curve curve : curves) {
            for (Mode mode : modes) {
                double[] column = TABLE[curve.ordinal()][mode.ordinal()];
                for (int i = 0; i < TABLE_SIZE; i++) {
                    double t = -1.0 + i * STEP_SIZE;
                    column[i] = computeRawCoefficient(t, curve, mode);
                }
            }
        }
    }

    public enum Curve {
        LINEAR, QUAD, CUBIC, QUART, QUINT, SINE, EXPO, CIRC, ELASTIC, BACK, BOUNCE, SMOOTHSTEP, SMOOTHERSTEP
    }

    public enum Mode {
        IN, OUT, IN_OUT
    }

    private Interpolation() {
    }

    public static double getRawCoefficient(double t, Curve curve, Mode mode) {
        if (Double.isNaN(t)) return 0.0;
        if (t <= -1.0) return TABLE[curve.ordinal()][mode.ordinal()][0];
        if (t >= 1.0) return TABLE[curve.ordinal()][mode.ordinal()][TABLE_SIZE - 1];

        double[] column = TABLE[curve.ordinal()][mode.ordinal()];
        double scaled = (t + 1.0) * INV_STEP_SIZE;
        int idx = (int) scaled;
        double frac = scaled - idx;

        double v0 = column[idx];
        double v1 = column[idx + 1];
        return v0 + frac * (v1 - v0);
    }

    public static double interpolate(double from, double to, double t, Curve curve, Mode mode) {
        double coefficient = getRawCoefficient(t, curve, mode);
        return Math.fma(to - from, coefficient, from);
    }

    public static Rotation interpolateRotation(Rotation current, Rotation target,
                                               float yawSpeed, float pitchSpeed) {
        return interpolateRotation(
                current.yaw(), current.pitch(),
                target.yaw(), target.pitch(),
                yawSpeed, pitchSpeed
        );
    }

    public static Rotation interpolateRotation(float currentYaw, float currentPitch,
                                               float targetYaw, float targetPitch,
                                               float yawSpeed, float pitchSpeed) {
        float nextYaw = interpolateAxis(currentYaw, targetYaw, yawSpeed);
        float nextPitch = interpolateAxis(currentPitch, targetPitch, pitchSpeed);
        return new Rotation(nextYaw, nextPitch);
    }

    private static float interpolateAxis(float current, float target, float speed) {
        if (Float.isNaN(current) || Float.isNaN(target) || Float.isNaN(speed)) {
            return current;
        }

        float limit = Math.max(0.0f, speed);
        float delta = target - current;
        float absDelta = Math.abs(delta);

        if (absDelta < 1.0E-4f || limit == 0.0f) {
            return clamp(current, -1.0f, 1.0f);
        }

        float t = Math.min(1.0f, limit / absDelta);

        float coefficient = (float) getRawCoefficient(t, Curve.CIRC, Mode.IN_OUT);
        float result = current + delta * coefficient;

        return clamp(result, -1.0f, 1.0f);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }


    private static double computeRawCoefficient(double t, Curve curve, Mode mode) {
        if (t == 0.0) return 0.0;
        if (t == 1.0) return 1.0;
        if (t == -1.0) return -1.0;

        double p = Math.abs(t);
        double base = switch (curve) {
            case LINEAR -> p;
            case QUAD -> easeQuad(p, mode);
            case CUBIC -> easeCubic(p, mode);
            case QUART -> easeQuart(p, mode);
            case QUINT -> easeQuint(p, mode);
            case SINE -> easeSine(p, mode);
            case EXPO -> easeExpo(p, mode);
            case CIRC -> easeCirc(p, mode);
            case ELASTIC -> easeElastic(p, mode);
            case BACK -> easeBack(p, mode);
            case BOUNCE -> easeBounce(p, mode);
            case SMOOTHSTEP -> easeSmoothstep(p, mode);
            case SMOOTHERSTEP -> easeSmootherstep(p, mode);
        };

        return t > 0.0 ? base : -base;
    }

    private static final double PI = Math.PI;
    private static final double HALF_PI = PI / 2.0;
    private static final double C1 = 1.70158;
    private static final double C2 = C1 * 1.525;
    private static final double C3 = C1 + 1.0;
    private static final double C4 = (2.0 * PI) / 3.0;
    private static final double C5 = (2.0 * PI) / 4.5;
    private static final double N1 = 7.5625;
    private static final double D1 = 2.75;

    private static double easeQuad(double p, Mode mode) {
        return switch (mode) {
            case IN -> p * p;
            case OUT -> 1.0 - (1.0 - p) * (1.0 - p);
            case IN_OUT -> {
                if (p < 0.5) yield 2.0 * p * p;
                double inv = -2.0 * p + 2.0;
                yield 1.0 - (inv * inv) / 2.0;
            }
        };
    }

    private static double easeCubic(double p, Mode mode) {
        return switch (mode) {
            case IN -> p * p * p;
            case OUT -> {
                double inv = 1.0 - p;
                yield 1.0 - inv * inv * inv;
            }
            case IN_OUT -> {
                if (p < 0.5) yield 4.0 * p * p * p;
                double inv = -2.0 * p + 2.0;
                yield 1.0 - (inv * inv * inv) / 2.0;
            }
        };
    }

    private static double easeQuart(double p, Mode mode) {
        return switch (mode) {
            case IN -> p * p * p * p;
            case OUT -> {
                double inv = 1.0 - p;
                yield 1.0 - inv * inv * inv * inv;
            }
            case IN_OUT -> {
                if (p < 0.5) yield 8.0 * p * p * p * p;
                double inv = -2.0 * p + 2.0;
                yield 1.0 - (inv * inv * inv * inv) / 2.0;
            }
        };
    }

    private static double easeQuint(double p, Mode mode) {
        return switch (mode) {
            case IN -> p * p * p * p * p;
            case OUT -> {
                double inv = 1.0 - p;
                yield 1.0 - inv * inv * inv * inv * inv;
            }
            case IN_OUT -> {
                if (p < 0.5) yield 16.0 * p * p * p * p * p;
                double inv = -2.0 * p + 2.0;
                yield 1.0 - (inv * inv * inv * inv * inv) / 2.0;
            }
        };
    }

    private static double easeSine(double p, Mode mode) {
        return switch (mode) {
            case IN -> 1.0 - Math.cos(p * HALF_PI);
            case OUT -> Math.sin(p * HALF_PI);
            case IN_OUT -> -(Math.cos(PI * p) - 1.0) / 2.0;
        };
    }

    private static double easeExpo(double p, Mode mode) {
        return switch (mode) {
            case IN -> p == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * p - 10.0);
            case OUT -> p == 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * p);
            case IN_OUT -> {
                if (p == 0.0) yield 0.0;
                if (p == 1.0) yield 1.0;
                if (p < 0.5) yield Math.pow(2.0, 20.0 * p - 10.0) / 2.0;
                yield (2.0 - Math.pow(2.0, -20.0 * p + 10.0)) / 2.0;
            }
        };
    }

    private static double easeCirc(double p, Mode mode) {
        return switch (mode) {
            case IN -> 1.0 - Math.sqrt(1.0 - p * p);
            case OUT -> Math.sqrt(1.0 - (p - 1.0) * (p - 1.0));
            case IN_OUT -> {
                if (p < 0.5) yield (1.0 - Math.sqrt(1.0 - 4.0 * p * p)) / 2.0;
                double shifted = 2.0 * p - 2.0;
                yield (Math.sqrt(1.0 - shifted * shifted) + 1.0) / 2.0;
            }
        };
    }

    private static double easeElastic(double p, Mode mode) {
        return switch (mode) {
            case IN -> {
                if (p == 0.0) yield 0.0;
                if (p == 1.0) yield 1.0;
                yield -Math.pow(2.0, 10.0 * p - 10.0) * Math.sin((p * 10.0 - 10.75) * C4);
            }
            case OUT -> {
                if (p == 0.0) yield 0.0;
                if (p == 1.0) yield 1.0;
                yield Math.pow(2.0, -10.0 * p) * Math.sin((p * 10.0 - 0.75) * C4) + 1.0;
            }
            case IN_OUT -> {
                if (p == 0.0) yield 0.0;
                if (p == 1.0) yield 1.0;
                if (p < 0.5)
                    yield -(Math.pow(2.0, 20.0 * p - 10.0) * Math.sin((20.0 * p - 11.125) * C5)) / 2.0;
                yield (Math.pow(2.0, -20.0 * p + 10.0) * Math.sin((20.0 * p - 11.125) * C5)) / 2.0 + 1.0;
            }
        };
    }

    private static double easeBack(double p, Mode mode) {
        return switch (mode) {
            case IN -> C3 * p * p * p - C1 * p * p;
            case OUT -> {
                double shifted = p - 1.0;
                yield 1.0 + C3 * shifted * shifted * shifted + C1 * shifted * shifted;
            }
            case IN_OUT -> {
                if (p < 0.5) {
                    double scaled = 2.0 * p;
                    yield (scaled * scaled * ((C2 + 1.0) * scaled - C2)) / 2.0;
                }
                double scaled = 2.0 * p - 2.0;
                yield (scaled * scaled * ((C2 + 1.0) * scaled + C2) + 2.0) / 2.0;
            }
        };
    }

    private static double easeBounce(double p, Mode mode) {
        return switch (mode) {
            case IN -> 1.0 - bounceOut(1.0 - p);
            case OUT -> bounceOut(p);
            case IN_OUT -> {
                if (p < 0.5) yield (1.0 - bounceOut(1.0 - 2.0 * p)) / 2.0;
                yield (1.0 + bounceOut(2.0 * p - 1.0)) / 2.0;
            }
        };
    }

    private static double bounceOut(double p) {
        if (p < 1.0 / D1) {
            return N1 * p * p;
        } else if (p < 2.0 / D1) {
            double adj = p - 1.5 / D1;
            return N1 * adj * adj + 0.75;
        } else if (p < 2.5 / D1) {
            double adj = p - 2.25 / D1;
            return N1 * adj * adj + 0.9375;
        } else {
            double adj = p - 2.625 / D1;
            return N1 * adj * adj + 0.984375;
        }
    }

    private static double easeSmoothstep(double p, Mode mode) {
        return switch (mode) {
            case IN -> p * p * (3.0 - 2.0 * p);
            case OUT -> {
                double inv = 1.0 - p;
                yield 1.0 - inv * inv * (3.0 - 2.0 * inv);
            }
            case IN_OUT -> p * p * (3.0 - 2.0 * p);
        };
    }

    private static double easeSmootherstep(double p, Mode mode) {
        return switch (mode) {
            case IN -> p * p * p * (p * (p * 6.0 - 15.0) + 10.0);
            case OUT -> {
                double inv = 1.0 - p;
                yield 1.0 - inv * inv * inv * (inv * (inv * 6.0 - 15.0) + 10.0);
            }
            case IN_OUT -> p * p * p * (p * (p * 6.0 - 15.0) + 10.0);
        };
    }
}