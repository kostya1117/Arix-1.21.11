package ru.arixcompany.features.module.modules.combat.aura.rotation.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.features.event.player.EventMotion;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.module.modules.combat.aura.rotation.Component;
import ru.arixcompany.features.module.modules.combat.aura.utils.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.utils.SensitivityUtil;
import ru.arixcompany.utils.player.MoveUtils;

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
    @Getter
    @Setter
    public static Rotation serverAngle = new Rotation(0,0);
    public static boolean isRotating() {
        return currentTask != RotationTask.IDLE;
    }
    @EventHandler
    public void onEvent(EventInput event) {
        if (isRotating()) {
            MoveUtils.fixMovement(event, mc.gameRenderer.getMainCamera().yRot());
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
            FreeLookRepo.active = true;
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
        Rotation freeRot = new Rotation(FreeLookRepo.freeYaw, FreeLookRepo.freePitch);
        if (updateRotation(freeRot, currentYawReturnSpeed, currentPitchReturnSpeed)) {
            stopRotation();
        }
    }
    static boolean updateRotation(Rotation target, float yawSpeed, float pitchSpeed) {
    if (mc.player == null) return false;

    Rotation minerot = new Rotation(mc.player);

    float yawDelta   = Mth.wrapDegrees(target.yaw - minerot.yaw);
    float pitchDelta = target.pitch - minerot.pitch;

    float finalYaw   = minerot.yaw   + Mth.clamp(yawDelta,   -yawSpeed,   yawSpeed);
    float finalPitch = Mth.clamp(minerot.pitch + Mth.clamp(pitchDelta, -pitchSpeed, pitchSpeed), -90.0F, 90.0F);

    mc.player.setYRot(finalYaw);
    mc.player.setXRot(finalPitch);

    idleTicks = 0;
    return Math.abs(Mth.wrapDegrees(target.yaw - mc.player.getYRot())) < 1
            && Math.abs(target.pitch - mc.player.getXRot()) < 1;
}

//    @EventHandler
//    public void onSwimming(EventMotion eventMotion) {
//        if (mc.player == null) return;
//        if (FreeLookRepo.active) {
//            eventMotion.setYaw(mc.player.getYRot());
//            eventMotion.setPitch(mc.player.getXRot());
//        }
//    }

    public void stopRotation() {
        currentTask     = RotationTask.IDLE;
        currentPriority = 0;
        FreeLookRepo.active = false;
    }
    @EventHandler
    public void onPacket(EventPacket event) {
        if (event.isCancelled()) return;
        switch (event.getPacket()) {
            case ServerboundMovePlayerPacket player when player.hasRotation() -> serverAngle = new Rotation(player.yRot, player.xRot,true);
            case ClientboundPlayerPositionPacket player -> serverAngle = new Rotation(player.change().yRot(), player.change().xRot(),true);
            case ServerboundUseItemPacket player -> serverAngle = new Rotation(player.getYRot(), player.getXRot(),true);
            default -> {}
        }
    }

    public enum RotationTask {
        AIM,
        RESET,
        IDLE
    }
}
