package ru.arixcompany.utils.math;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;
import ru.arixcompany.utils.IMinecraft;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtils implements IMinecraft {
    public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
    public static float fast(float end, float start, float multiple) {
        return (1.0F - Mth.clamp(deltaTime() * multiple, 0.0F, 1.0F)) * end + Mth.clamp(deltaTime() * multiple, 0.0F, 1.0F) * start;
    }
    public static float deltaTime() {
        float debugFPS = mc.getFps();
        return debugFPS > 0.0F ? 1.0F / debugFPS : 1.0F;
    }
    public static float randomValue(float min, float max) {
        validateRange(min, max);
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }
    private static void validateRange(double min, double max) {
        if (max < min) {
            throw new IllegalArgumentException("max не может быть меньше min.");
        }
    }

    public static float getSmartRandom(float min,float max) {
        Random random = ThreadLocalRandom.current();

        double gaussian = random.nextGaussian();

        float average = (min + max) / 2.0f;
        float deviation = (max - min) / 4.0f;

        float delay = (float) (average + gaussian * deviation);

        if (random.nextFloat() < 0.05f) {
            delay += 200 + random.nextFloat() * 300;
        }

        return Math.max(min, Math.min(max, delay));
    }
}
