package ru.arixcompany.utils.player;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.utils.IMinecraft;

@UtilityClass
public class MoveUtils implements IMinecraft {

    public boolean isMoving() {
        return mc.player.input.forwardImpulse != 0f || mc.player.input.leftImpulse != 0f;
    }

    public boolean isFlying() {
        return mc.player.input.jumping || mc.player.input.shiftKeyDown;
    }

    public static void fixMovement(final EventInput event, float yaw) {
        final float forward = event.getForward();
        final float strafe = event.getStrafe();
        final double angle = Mth.wrapDegrees(Math.toDegrees(direction(mc.player.isFallFlying() ? yaw : mc.player.getYRot(), forward, strafe)));

        if (forward == 0 && strafe == 0) {
            return;
        }

        float closestForward = 0, closestStrafe = 0, closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1F; predictedForward <= 1F; predictedForward += 1F) {
            for (float predictedStrafe = -1F; predictedStrafe <= 1F; predictedStrafe += 1F) {
                if (predictedStrafe == 0 && predictedForward == 0) continue;

                final double predictedAngle = Mth.wrapDegrees(Math.toDegrees(direction(yaw, predictedForward, predictedStrafe)));
                final double difference = Math.abs(angle - predictedAngle);

                if (difference < closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
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

    public void setMotion(final double speed, double Y) {

        KeyMapping[] mappings = new KeyMapping[]{
                mc.options.keyUp,
                mc.options.keyDown,
                mc.options.keyLeft,
                mc.options.keyRight,
        };

        boolean condition = false;

        for (KeyMapping keyMapping : mappings) {
            if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), keyMapping.getDefaultKey().getValue())) {
                condition = true;
            }
        }
        if (!condition) {
            mc.player.setDeltaMovement(0, 0, 0);
        } else {
            final double yaw = getDirection(true);
            if (mc.player.input.shiftKeyDown && isFlying()) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -Y, mc.player.getDeltaMovement().z);
            }
            if (mc.player.input.jumping && isFlying()) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, Y, mc.player.getDeltaMovement().z);
            }
            if (!isMoving())
                return;
            if (isFlying() && mc.player.input.jumping) {
                mc.player.setDeltaMovement(-Math.sin(yaw) * speed, Y, Math.cos(yaw) * speed);
            }
            if (isFlying() && mc.player.input.shiftKeyDown) {
                mc.player.setDeltaMovement(-Math.sin(yaw) * speed, -Y, Math.cos(yaw) * speed);
            }
            if (!isFlying() && isMoving()) {
                mc.player.setDeltaMovement(-Math.sin(yaw) * speed, mc.player.getMotionDirection().getStepY(), Math.cos(yaw) * speed);
            }
        }
    }

    public double getDirection(final boolean toRadians) {
        float rotationYaw = mc.player.getYRot();
        if (mc.player.zza < 0F)
            rotationYaw += 180F;
        float forward = 1F;
        if (mc.player.zza < 0F)
            forward = -0.5F;
        else if (mc.player.zza > 0F)
            forward = 0.5F;

        if (mc.player.xxa > 0F)
            rotationYaw -= 90F * forward;
        if (mc.player.xxa < 0F)
            rotationYaw += 90F * forward;

        return toRadians ? Math.toRadians(rotationYaw) : rotationYaw;
    }
}