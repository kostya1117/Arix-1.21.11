package ru.arixcompany.utils.aurautil.rotsystem;

import net.minecraft.util.Mth;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.aurautil.System;
import ru.arixcompany.utils.aurautil.freelooksystem.FreeLookSystem;
import ru.arixcompany.utils.aurautil.other.SensUtils;
import ru.arixcompany.utils.player.MoveUtils;

public class RotationSystem extends System {
    private static final RotationSystem INSTANCE = new RotationSystem();

    public static RotationSystem getInstance() {
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
        final RotationSystem instance = getInstance();
        if (instance.currentPriority > priority) {
            return;
        }
        if (instance.currentTask == RotationTask.IDLE && !clientRotation) {
            FreeLookSystem.setActive(true);
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
            FreeLookSystem.setActive(false);
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

    public RotationTask currentTask() {
        return currentTask;
    }

    public void currentTask(RotationTask task) {
        this.currentTask = task;
    }

    public float currentYawSpeed() {
        return currentYawSpeed;
    }

    public void currentYawSpeed(float speed) {
        this.currentYawSpeed = speed;
    }

    public float currentPitchSpeed() {
        return currentPitchSpeed;
    }

    public void currentPitchSpeed(float speed) {
        this.currentPitchSpeed = speed;
    }

    public float currentYawReturnSpeed() {
        return currentYawReturnSpeed;
    }

    public void currentYawReturnSpeed(float speed) {
        this.currentYawReturnSpeed = speed;
    }

    public float currentPitchReturnSpeed() {
        return currentPitchReturnSpeed;
    }

    public void currentPitchReturnSpeed(float speed) {
        this.currentPitchReturnSpeed = speed;
    }

    public int currentPriority() {
        return currentPriority;
    }

    public void currentPriority(int priority) {
        this.currentPriority = priority;
    }

    public int currentTimeout() {
        return currentTimeout;
    }

    public void currentTimeout(int timeout) {
        this.currentTimeout = timeout;
    }

    public int idleTicks() {
        return idleTicks;
    }

    public void idleTicks(int ticks) {
        this.idleTicks = ticks;
    }

    public Rotation targetRotation() {
        return targetRotation;
    }

    public void targetRotation(Rotation rotation) {
        this.targetRotation = rotation;
    }
}