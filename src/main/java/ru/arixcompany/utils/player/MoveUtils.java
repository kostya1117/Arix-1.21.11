package ru.arixcompany.utils.player;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.utils.IMinecraft;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class MoveUtils implements IMinecraft {
    public final Set<String> lockRequests = new HashSet<>();

    public void lockMovement(String moduleName) {
        if (mc.player != null && mc.player.isAlive() && mc.level != null) {
            HitAura.canSwap = true;
            lockRequests.add(moduleName);
            setMovementKeys(false);
        }
    }

    public void unlockMovement(String moduleName) {
        if (mc.player != null && mc.player.isAlive() && mc.level != null) {
            lockRequests.remove(moduleName);
            if (lockRequests.isEmpty() && mc.screen == null) {
                setMovementKeys(true);
                HitAura.canSwap = false;
            }
        }
    }

    private void setMovementKeys(boolean state) {
        KeyMapping[] movementKeys = new KeyMapping[]{
                mc.options.keyUp,
                mc.options.keyDown,
                mc.options.keyLeft,
                mc.options.keyRight,
                mc.options.keyJump,
                mc.options.keySprint
        };

        for (KeyMapping key : movementKeys) {
            key.setDown(state && key.isDown());
        }
    }

    public static void targetMovement(float cameraYaw, Vec3 position) {
        float[] movement = getMovementFromKeys();
        float forward = movement[0];
        float strafe = movement[1];
        if (forward != 0.0F || strafe != 0.0F) {
            AABB box = HitAura.target.getBoundingBox();
            double randX = Mth.lerp(Math.random(), box.minX, box.maxX);
            double randY = Mth.lerp(Math.random(), box.minY, box.maxY);
            double randZ = Mth.lerp(Math.random(), box.minZ, box.maxZ);
            randY = Mth.clamp(randY, HitAura.target.getY() + 0.2, HitAura.target.getY() + HitAura.target.getBbHeight() - 0.2);
            Vec3 randomHitVec = new Vec3(randX, randY, randZ);
            Vec3 direction = randomHitVec.subtract(mc.player.getEyePosition()).normalize();
            float targetYaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0);
            double angle = Mth.wrapDegrees(
                    Math.toDegrees(direction(mc.player.isFlyingVehicle() ? mc.player.getYRot() : targetYaw, forward, strafe))
            );
            float closestForward = 0.0F;
            float closestStrafe = 0.0F;
            float closestDifference = Float.MAX_VALUE;

            for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
                for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
                    if (predictedStrafe != 0.0F || predictedForward != 0.0F) {
                        double predictedAngle = Mth.wrapDegrees(Math.toDegrees(direction(mc.player.getYRot(), predictedForward, predictedStrafe)));
                        double difference = Math.abs(angle - predictedAngle);
                        if (difference < closestDifference) {
                            closestDifference = (float)difference;
                            closestForward = predictedForward;
                            closestStrafe = predictedStrafe;
                        }
                    }
                }
            }

            mc.options.keyUp.setDown(closestForward > 0.0F);
            mc.options.keyDown.setDown(closestForward < 0.0F);
            mc.options.keyLeft.setDown(closestStrafe > 0.0F);
            mc.options.keyRight.setDown(closestStrafe < 0.0F);
        }
    }

    public static void fixMovement(float cameraYaw) {
        float[] movement = getMovementFromKeys();
        float forward = movement[0];
        float strafe = movement[1];
        if (forward != 0.0F || strafe != 0.0F) {
            double angle = Mth.wrapDegrees(
                    Math.toDegrees(direction(mc.player.isFlyingVehicle() ? mc.player.getYRot() : cameraYaw, forward, strafe))
            );
            float closestForward = 0.0F;
            float closestStrafe = 0.0F;
            float closestDifference = Float.MAX_VALUE;

            for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
                for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
                    if (predictedStrafe != 0.0F || predictedForward != 0.0F) {
                        double predictedAngle = Mth.wrapDegrees(Math.toDegrees(direction(mc.player.getYRot(), predictedForward, predictedStrafe)));
                        double difference = Math.abs(angle - predictedAngle);
                        if (difference < closestDifference) {
                            closestDifference = (float)difference;
                            closestForward = predictedForward;
                            closestStrafe = predictedStrafe;
                        }
                    }
                }
            }

            mc.options.keyUp.setDown(closestForward > 0.0F);
            mc.options.keyDown.setDown(closestForward < 0.0F);
            mc.options.keyLeft.setDown(closestStrafe > 0.0F);
            mc.options.keyRight.setDown(closestStrafe < 0.0F);
        }
    }

    public static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }

    public static float[] getMovementFromKeys() {
        float forward = 0.0F;
        float strafe = 0.0F;

        long window = Minecraft.getInstance().getWindow().handle();

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS)
            forward++;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS)
            forward--;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS)
            strafe++;

        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS)
            strafe--;

        return new float[]{forward, strafe};
    }
}