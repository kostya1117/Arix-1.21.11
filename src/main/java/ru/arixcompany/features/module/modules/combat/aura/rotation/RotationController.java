package ru.arixcompany.features.module.modules.combat.aura.rotation;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.module.modules.combat.aura.IComponent;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.FreeLookController;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.SensUtils;
import ru.arixcompany.utils.player.MoveUtils;

@Getter
@Setter
public class RotationController extends IComponent {
    private static final RotationController INSTANCE = new RotationController();

    public static RotationController getInstance() {
        return INSTANCE;
    }

    private RotationTask currentTask = RotationTask.IDLE;
    private float currentYawSpeed;
    private float currentPitchSpeed;
    private float currentYawReturnSpeed;
    private float currentPitchReturnSpeed;
    private int currentPriority;
    private int currentTimeout;
    private int idleTicks;
    private Rotation targetRotation;

    @EventHandler
    public void onEvent(EventInput event) {
        if (isRotating()) {
            MoveUtils.fixMovement(event, Mth.wrapDegrees(mc.player.getYRot()));
        }
    }

    private void resetRotation() {
        Rotation targetRotation = new Rotation(mc.player.getYRot(), mc.player.getXRot());
        if (updateRotation(targetRotation, currentYawReturnSpeed, currentPitchReturnSpeed)) {
            stopRotation();
        }
    }

    @EventHandler
    public void onEvent(EventGameTick event) {
        if (currentTask == RotationTask.AIM && idleTicks > currentTimeout) {
            currentTask = RotationTask.RESET;
        }

        if (currentTask == RotationTask.RESET) {
            resetRotation();
        }
        idleTicks++;
    }

    public static void update(Rotation target, float yawSpeed, float pitchSpeed, float yawReturnSpeed, float pitchReturnSpeed, int timeout, int priority, boolean clientRotation) {
        final RotationController instance = getInstance();
        if (instance.currentPriority > priority) {
            return;
        }
        if (instance.currentTask == RotationTask.IDLE && !clientRotation) {
            FreeLookController.setActive(true);
        }

        instance.currentYawSpeed = yawSpeed;
        instance.currentPitchSpeed = pitchSpeed;
        instance.currentYawReturnSpeed = yawReturnSpeed;
        instance.currentPitchReturnSpeed = pitchReturnSpeed;
        instance.currentTimeout = timeout;
        instance.currentPriority = priority;
        instance.currentTask = RotationTask.AIM;
        instance.targetRotation = target;

        instance.updateRotation(target, yawSpeed, pitchSpeed);
    }

    public static void update(Rotation targetRotation, float turnSpeed, float returnSpeed, int timeout, int priority) {
        update(targetRotation, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    private boolean updateRotation(Rotation targetRotation, float lazinessH, float lazinessV) {
        if (mc.player == null) return false;

        float newYaw = smoothRotation(mc.player.getYRot(), targetRotation.getYaw(), lazinessH);
        float newPitch = Mth.clamp(smoothRotation(mc.player.getXRot(), targetRotation.getPitch(), lazinessV), -90F, 90F);

        mc.player.setYRot(newYaw);
        mc.player.setXRot(newPitch);

        idleTicks = 0;
        return new Rotation(mc.player.getYRot(), mc.player.getXRot()).getDelta(targetRotation) < 1F;
    }

    public void stopRotation() {
        currentTask = RotationTask.IDLE;
        currentPriority = 0;
        if (!getInstance().isRotating()) {
            FreeLookController.setActive(false);
        }
    }

    public boolean isRotating() {
        return currentTask != RotationTask.IDLE;
    }

    private float smoothRotation(float currentAngle, double targetAngle, float smoothFactor) {
        float angleDifference = (float) Mth.wrapDegrees(targetAngle - currentAngle);
        float adjustmentSpeed = Math.abs(angleDifference / smoothFactor);
        float angleAdjustment = adjustmentSpeed * Math.signum(angleDifference);

        if (Math.abs(angleAdjustment) > Math.abs(angleDifference)) {
            angleAdjustment = angleDifference;
        }

        return currentAngle + SensUtils.getSensitivity(angleAdjustment);
    }

    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }
}