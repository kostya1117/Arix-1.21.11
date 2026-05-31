package ru.arixcompany.utils.math;

import java.util.concurrent.ThreadLocalRandom;

public class Randomizer {
    private long state;

    public Randomizer() {
        // ThreadLocalRandom даёт качественный seed без корреляций между инстансами
        this.state = ThreadLocalRandom.current().nextLong();
    }

    public Randomizer(long seed) {
        // Прогоняем seed через SplitMix чтобы "размазать" плохие сиды (0, 1, 2...)
        this.state = seed;
        nextLong(); // Прогрев
    }

    public int nextInt(int min, int max) {
        if (min >= max) return min;

        long range = (long) max - min + 1;
        // Убираем modulo bias через rejection sampling
        long bits, val;
        do {
            bits = nextLong() >>> 1;
            val = bits % range;
        } while (bits - val + (range - 1) < 0);

        return min + (int) val;
    }

    public boolean nextBoolean() {
        return (nextLong() >>> 63) != 0;
    }

    public float nextFloat(float min, float max) {
        if (min >= max) return min;
        return min + nextFloat() * (max - min);
    }

    public float nextFloat() {
        // 24 бита мантиссы → [0.0, 1.0)
        return (nextLong() >>> 40) * 0x1.0p-24f;
    }

    public double nextDouble(double min, double max) {
        if (min >= max) return min;
        return min + nextDouble() * (max - min);
    }

    public double nextDouble() {
        // 53 бита мантиссы → [0.0, 1.0)
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    // Гауссово распределение (полезно для естественного джиттера)
    private boolean hasSpare = false;
    private double spare;

    public double nextGaussian(double mean, double stddev) { // min/max
        if (hasSpare) {
            hasSpare = false;
            return mean + stddev * spare;
        }
        // Marsaglia polar method
        double u, v, s;
        do {
            u = nextDouble() * 2.0 - 1.0;
            v = nextDouble() * 2.0 - 1.0;
            s = u * u + v * v;
        } while (s >= 1.0 || s == 0.0);

        double mul = Math.sqrt(-2.0 * Math.log(s) / s);
        spare = v * mul;
        hasSpare = true;
        return mean + stddev * u * mul;
    }

    // SplitMix64
    private long nextLong() {
        long z = (state += 0x9e3779b97f4a7c15L);
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
