package ru.arixcompany.features.module.modules.combat.aura.rotation.impl;

import net.minecraft.util.Mth;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.features.event.render.EventRender3D;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.module.modules.combat.aura.rotation.Component;
import ru.arixcompany.utils.player.MoveUtils;
import ru.arixcompany.utils.player.PlayerUtil;

public class RotationRepo extends Component {

    public static RotationTask currentTask    = RotationTask.IDLE;
    public static float currentYawSpeed;
    public static float currentPitchSpeed;
    public static float currentYawReturnSpeed;
    public static float currentPitchReturnSpeed;
    public static int   currentPriority;
    public static int   currentTimeout;
    public static int   idleTicks;
    public static Rotation targetRotation;

    public static boolean isRotating() {
        return currentTask != RotationTask.IDLE;
    }
    @EventHandler
    public void onEvent(EventInput event) {
        if (isRotating()) {
            MoveUtils.fixMovement(event, Mth.wrapDegrees(mc.player.getYRot()));
        }
    }

//    @EventHandler
//    public void onEvent(EventRender3D event) {
//        if (mc.player != null && isRotating()) {
//            mc.player.yBodyRot = PlayerUtil.calculateCorrectYawOffset(mc.player.getYRot());
//        }
//    }

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

    public static void update(
            Rotation target,
            float yawSpeed,
            float pitchSpeed,
            float yawReturnSpeed,
            float pitchReturnSpeed,
            int timeout,
            int priority,
            boolean clientRotation
    ) {
        if (currentPriority > priority) return;

        if (currentTask == RotationTask.IDLE && !clientRotation) {
            FreeLookUtil.active = true;
        }

        currentYawSpeed         = yawSpeed;
        currentPitchSpeed       = pitchSpeed;
        currentYawReturnSpeed   = yawReturnSpeed;
        currentPitchReturnSpeed = pitchReturnSpeed;
        currentTimeout          = timeout;
        currentPriority         = priority;
        currentTask             = RotationTask.AIM;
        targetRotation          = target;

        updateRotation(target, yawSpeed, pitchSpeed);
    }

    public static void update(
            Rotation target,
            float turnSpeed,
            float returnSpeed,
            int timeout,
            int priority
    ) {
        update(target, turnSpeed, turnSpeed, returnSpeed, returnSpeed, timeout, priority, false);
    }

    private void resetRotation() {
        Rotation freeRot = new Rotation(FreeLookUtil.freeYaw, FreeLookUtil.freePitch);
        if (updateRotation(freeRot, currentYawReturnSpeed, currentPitchReturnSpeed)) {
            stopRotation();
        }
    }

    static boolean updateRotation(Rotation target, float yawSpeed, float pitchSpeed) {
        if (mc.player == null) return false;

        float yawDelta   = Mth.wrapDegrees(target.yaw   - mc.player.getYRot());
        float pitchDelta = target.pitch - mc.player.getXRot();

        float clampedYaw   = Math.min(Math.abs(yawDelta),   yawSpeed);
        float clampedPitch = Math.min(Math.abs(pitchDelta), pitchSpeed);

        float yawStep   = getSensitivity(Mth.clamp(yawDelta,   -clampedYaw,   clampedYaw));
        float pitchStep = getSensitivity(Mth.clamp(pitchDelta, -clampedPitch, clampedPitch));

        float finalYaw   = mc.player.getYRot() + yawStep;
        float finalPitch = Mth.clamp(mc.player.getXRot() + pitchStep, -90.0F, 90.0F);

        mc.player.setYRot(finalYaw);
        mc.player.setXRot(finalPitch);

        idleTicks = 0;

        return Math.abs(Mth.wrapDegrees(target.yaw - mc.player.getYRot())) < 1.0F
                && Math.abs(target.pitch - mc.player.getXRot()) < 1.0F;
    }

    public void stopRotation() {
        currentTask     = RotationTask.IDLE;
        currentPriority = 0;
        FreeLookUtil.active = false;
    }

    public static float getSensitivity(float rot) {
        return getDeltaMouse(rot) * getGCDValue();
    }

    public static float getGCDValue() {
        return (float) (getGCD() * 0.15);
    }

    public static float getGCD() {
        float f = (float) ((double) mc.options.sensitivity().get() * 0.6 + 0.2);
        return f * f * f * 8.0F;
    }

    public static float getDeltaMouse(float delta) {
        return Math.round(delta / getGCDValue());
    }

    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }
}
