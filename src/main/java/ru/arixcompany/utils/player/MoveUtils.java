package ru.arixcompany.utils.player;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.utils.IMinecraft;

import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class MoveUtils implements IMinecraft {

    private static final float INPUT_EPSILON = 1.0E-4F;

    public final Set<String> lockRequests = new HashSet<>();

    public boolean isMoving() {
        if (mc.player == null || mc.player.input == null) {
            return false;
        }

        Input input = mc.player.input.keyPresses;
        return input.forward() != input.backward() || input.left() != input.right();
    }

    public void fixMovement(final EventInput event, float yaw) {
        final float forward = calculateImpulse(event.isForward(), event.isBackward());
        final float strafe = calculateImpulse(event.isLeft(), event.isRight());

        if (Math.abs(forward) <= INPUT_EPSILON && Math.abs(strafe) <= INPUT_EPSILON) {
            return;
        }

        final double angle = Mth.wrapDegrees(Math.toDegrees(direction(yaw, forward, strafe)));

        float closestForward = 0.0F;
        float closestStrafe = 0.0F;
        double closestDifference = Double.MAX_VALUE;

        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward += 1.0F) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe += 1.0F) {
                if (predictedForward == 0.0F && predictedStrafe == 0.0F) {
                    continue;
                }

                final double predictedAngle = Mth.wrapDegrees(Math.toDegrees(direction(yaw, predictedForward, predictedStrafe)));
                final double difference = Math.abs(Mth.wrapDegrees(angle - predictedAngle));

                if (difference < closestDifference) {
                    closestDifference = difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward > 0.0F);
        event.setBackward(closestForward < 0.0F);
        event.setLeft(closestStrafe > 0.0F);
        event.setRight(closestStrafe < 0.0F);
    }

    public double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0.0F) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;

        if (moveForward < 0.0F) {
            forward = -0.5F;
        } else if (moveForward > 0.0F) {
            forward = 0.5F;
        }

        if (moveStrafing > 0.0F) {
            rotationYaw -= 90.0F * forward;
        }

        if (moveStrafing < 0.0F) {
            rotationYaw += 90.0F * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public double getDirection() {
        if (mc.player == null || mc.player.input == null) {
            return 0.0D;
        }

        return getDirection(mc.player.getYRot(), getForwardImpulse(), getLeftImpulse());
    }

    private double getDirection(float rotationYaw, float forwardImpulse, float leftImpulse) {
        if (forwardImpulse < 0.0F) {
            rotationYaw += 180.0F;
        }

        float forward = 1.0F;

        if (forwardImpulse < 0.0F) {
            forward = -0.5F;
        } else if (forwardImpulse > 0.0F) {
            forward = 0.5F;
        }

        if (leftImpulse > 0.0F) {
            rotationYaw -= 90.0F * forward;
        }

        if (leftImpulse < 0.0F) {
            rotationYaw += 90.0F * forward;
        }

        return rotationYaw;
    }

    public float getPlayerDirection() {
        if (mc.player == null || mc.player.input == null) {
            return 0.0F;
        }

        float yaw = mc.player.getYRot();
        float strafe = 45.0F;

        float forwardImpulse = getForwardImpulse();
        float leftImpulse = getLeftImpulse();

        if (forwardImpulse < 0.0F) {
            strafe = -45.0F;
            yaw += 180.0F;
        }

        if (leftImpulse > 0.0F) {
            yaw -= strafe;

            if (forwardImpulse == 0.0F) {
                yaw -= 45.0F;
            }
        } else if (leftImpulse < 0.0F) {
            yaw += strafe;

            if (forwardImpulse == 0.0F) {
                yaw += 45.0F;
            }
        }

        return yaw;
    }

    public float getForwardImpulse() {
        if (mc.player == null || mc.player.input == null) {
            return 0.0F;
        }

        Input input = mc.player.input.keyPresses;
        return calculateImpulse(input.forward(), input.backward());
    }

    public float getLeftImpulse() {
        if (mc.player == null || mc.player.input == null) {
            return 0.0F;
        }

        Input input = mc.player.input.keyPresses;
        return calculateImpulse(input.left(), input.right());
    }

    private float calculateImpulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }

        return positive ? 1.0F : -1.0F;
    }

    public void lockMovement(String moduleName) {
        if (mc.player != null && mc.player.isAlive() && mc.level != null) {
            lockRequests.add(moduleName);
            setMovementKeys(false);
        }
    }

    public void unlockMovement(String moduleName) {
        if (mc.player != null && mc.player.isAlive() && mc.level != null) {
            lockRequests.remove(moduleName);

            if (lockRequests.isEmpty() && mc.screen == null) {
                setMovementKeys(true);
            }
        }
    }

    public boolean isMovementLocked() {
        return !lockRequests.isEmpty();
    }

    private void setMovementKeys(boolean state) {
        KeyMapping[] movementKeys = new KeyMapping[]{
                mc.options.keyUp,
                mc.options.keyDown,
                mc.options.keyLeft,
                mc.options.keyRight,
                mc.options.keyJump
        };

        for (KeyMapping keyBinding : movementKeys) {
            boolean pressed = state && InputConstants.isKeyDown(mc.getWindow(), keyBinding.getDefaultKey().getValue());
            keyBinding.setDown(pressed);
        }
    }
}