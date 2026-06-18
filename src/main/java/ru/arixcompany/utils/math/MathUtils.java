package ru.arixcompany.utils.math;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2fStack;
import ru.arixcompany.utils.IMinecraft;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtils implements IMinecraft {
    public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
    public static boolean isHovered(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
    public static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
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
    public static float randomValueLerp(float min, float max) {
        return lerp(max, min, new SecureRandom().nextFloat());
    }
    public static <T extends Number> T lerp(T input, T target, double step) {
        double start = input.doubleValue();
        double end = target.doubleValue();
        double result = start + step * (end - start);

        return switch (input) {
            case Integer ignored -> (T) Integer.valueOf((int) Math.round(result));
            case Double ignored -> (T) Double.valueOf(result);
            case Float ignored -> (T) Float.valueOf((float) result);
            case Long ignored -> (T) Long.valueOf(Math.round(result));
            case Short ignored -> (T) Short.valueOf((short) Math.round(result));
            case Byte ignored -> (T) Byte.valueOf((byte) Math.round(result));
            default -> throw new IllegalArgumentException("Unsupported type: " + input.getClass().getSimpleName());
        };
    }
    private static void validateRange(double min, double max) {
        if (max < min) {
            throw new IllegalArgumentException("max не может быть меньше min.");
        }
    }
    public static float getRandom(float min, float max) {
        return (float) (Math.random() * (max - min) + min);
    }
}
