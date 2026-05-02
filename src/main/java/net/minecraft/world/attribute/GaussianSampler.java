package net.minecraft.world.attribute;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class GaussianSampler {
    private static final int GAUSSIAN_SAMPLE_RADIUS = 2;
    private static final int GAUSSIAN_SAMPLE_BREADTH = 6;
    private static final double[] GAUSSIAN_SAMPLE_KERNEL = new double[]{0.0, 1.0, 4.0, 6.0, 4.0, 1.0, 0.0};

    public static <V> void sample(Vec3 p_459604_, GaussianSampler.Sampler<V> p_459950_, GaussianSampler.Accumulator<V> p_451084_) {
        double d0 = p_459604_.x - 0.5;
        double d1 = p_459604_.y - 0.5;
        double d2 = p_459604_.z - 0.5;
        int i = Mth.floor(d0);
        int j = Mth.floor(d1);
        int k = Mth.floor(d2);
        double d3 = d0 - i;
        double d4 = d1 - j;
        double d5 = d2 - k;

        for (int l = 0; l < 6; l++) {
            double d6 = Mth.lerp(d5, GAUSSIAN_SAMPLE_KERNEL[l + 1], GAUSSIAN_SAMPLE_KERNEL[l]);
            int i1 = k - 2 + l;

            for (int j1 = 0; j1 < 6; j1++) {
                double d7 = Mth.lerp(d3, GAUSSIAN_SAMPLE_KERNEL[j1 + 1], GAUSSIAN_SAMPLE_KERNEL[j1]);
                int k1 = i - 2 + j1;

                for (int l1 = 0; l1 < 6; l1++) {
                    double d8 = Mth.lerp(d4, GAUSSIAN_SAMPLE_KERNEL[l1 + 1], GAUSSIAN_SAMPLE_KERNEL[l1]);
                    int i2 = j - 2 + l1;
                    double d9 = d7 * d8 * d6;
                    V v = p_459950_.get(k1, i2, i1);
                    p_451084_.accumulate(d9, v);
                }
            }
        }
    }

    public static <V> void sampleM(Vec3 vectorIn, GaussianSampler.Sampler<V> samplerIn, GaussianSampler.Accumulator<V> accumulatorIn) {
    }

    @FunctionalInterface
    public interface Accumulator<V> {
        void accumulate(double p_458241_, V p_456955_);
    }

    @FunctionalInterface
    public interface Sampler<V> {
        V get(int p_454195_, int p_450528_, int p_453501_);
    }
}
