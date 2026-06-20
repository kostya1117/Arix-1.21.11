package ru.arixcompany.features.module.modules.combat.aura.aiming;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventPriority;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventGameTicked;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.features.event.player.EventLook;
import ru.arixcompany.features.event.player.EventRotation;
import ru.arixcompany.features.event.render.EventGameRender3D;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.event.world.EventRotationUpdate;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.MovementCorrection;
import ru.arixcompany.features.module.modules.combat.aura.rotation.Component;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.MoveUtils;

public class RotationManager extends Component implements RequestHandler.RequestProvider {

    private static RotationManager INSTANCE;

    public static RotationManager getInstance() {
        if (INSTANCE == null) INSTANCE = new RotationManager();
        return INSTANCE;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    private static final RequestHandler<RotationTarget> rotationTargetHandler = new RequestHandler<>();

    private static RotationTarget getRotationTarget() {
        return rotationTargetHandler.getActiveRequestValue();
    }

    public static RotationTarget previousRotationTarget = null;
    public static Rotation currentRotation = null;
    public static Rotation previousRotation = null;
    public static Rotation playerRotation = null;
    public static Rotation actualServerRotation = Rotation.ZERO;
    public static Rotation theoreticalServerRotation = Rotation.ZERO;

    public static float freeYaw = 0f;
    public static float freePitch = 0f;

    public boolean isRotating() {
        return getActiveRotationTarget() != null;
    }

    @Nullable
    public RotationTarget getActiveRotationTarget() {
        RotationTarget target = getRotationTarget();
        return (target != null) ? target : previousRotationTarget;
    }

    private boolean isSilentActive() {
        RotationTarget active = getActiveRotationTarget();
        return active != null && active.movementCorrection == MovementCorrection.SILENT;
    }

    public static void setRotationTarget(RotationTarget target, int priority, RequestHandler.RequestProvider provider) {
        if (!isRotatingAllowed(target)) return;
        rotationTargetHandler.request(
                new RequestHandler.Request<>(
                        target.movementCorrection == MovementCorrection.CHANGE_LOOK ? 1 : target.ticksUntilReset,
                        priority,
                        provider,
                        target
                )
        );
    }

    public static void setCurrentRotation(@Nullable Rotation value) {
        if (value == null) {
            previousRotation = null;
        } else {
            previousRotation = (currentRotation != null) ? currentRotation :
                    (mc.player != null ? new Rotation(mc.player.getYRot(), mc.player.getXRot(), true) : Rotation.ZERO);
        }
        currentRotation = value;
    }

    public static void setRotationTarget(RotationTarget target, int priority) {
        setRotationTarget(target, priority, RotationManager.getInstance());
    }

    public static void setRotationTarget(RotationTarget target) {
        setRotationTarget(target, 1);
    }

    public static boolean isRotatingAllowed(RotationTarget target) {
        if (mc.player == null) return false;
        if (mc.screen != null && !HitAura.extraSettings.isSelected("Игнорировать инвентарь")) return false;
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTick(EventGameTick event) {
        if (mc.player == null) return;
        EventRepo.call(new EventRotationUpdate());
        update();
    }

    public void update() {
        if (getActiveRotationTarget() != null && getActiveRotationTarget().movementCorrection == MovementCorrection.SILENT) {
            playerRotation = new Rotation(mc.gameRenderer.getMainCamera().yRot(), mc.gameRenderer.getMainCamera().xRot());
        } else {
            playerRotation = new Rotation(mc.player.getYRot(), mc.player.getXRot(), true);
        }

        RotationTarget rotationTarget = getRotationTarget();
        RotationTarget activeRotationTarget = getActiveRotationTarget();
        if (activeRotationTarget == null) return;

        if (isRotatingAllowed(activeRotationTarget)) {
            Rotation fromRotation = (currentRotation != null) ? currentRotation : playerRotation;
            Rotation rotation = activeRotationTarget.towards(fromRotation, rotationTarget == null).normalize();
            float diff = rotation.angleTo(playerRotation);

            if (rotationTarget == null && (activeRotationTarget.movementCorrection == MovementCorrection.CHANGE_LOOK
                    || activeRotationTarget.processors.isEmpty()
                    || diff <= activeRotationTarget.resetThreshold)) {

                if (currentRotation != null) {
//                    if (activeRotationTarget.movementCorrection == MovementCorrection.SILENT) {
//                        float yawDiff = Mth.wrapDegrees(freeYaw - mc.player.yRot);
//                        mc.player.setYRot(mc.player.yRot + yawDiff);
//                        mc.player.setXRot(freePitch);
//                        // playerRotation = new Rotation(mc.gameRenderer.getMainCamera().yRot(),mc.gameRenderer.getMainCamera().xRot(),true);
//                    }
                    if (activeRotationTarget.movementCorrection == MovementCorrection.SILENT) {
                        float yawDiff = Mth.wrapDegrees(freeYaw - mc.player.getYRot());
                        setPlayerRotation(mc.player.getYRot() + yawDiff, freePitch, true);
                    }
                }
                setCurrentRotation(null);
                previousRotationTarget = null;
            } else {
                setCurrentRotation(rotation);

//                if (activeRotationTarget.movementCorrection == MovementCorrection.SILENT) {
//                    mc.player.setYRot(currentRotation.yaw());
//                    mc.player.setXRot(currentRotation.pitch());
//                }
                if (activeRotationTarget.movementCorrection == MovementCorrection.SILENT) {
                    setPlayerRotation(currentRotation.yaw(), currentRotation.pitch(), true);
                }

                previousRotationTarget = activeRotationTarget;
            }
        }

        rotationTargetHandler.tick();
    }


    public static void setPlayerRotation(float yaw, float pitch, boolean updatePrevious) {
        if (mc.player == null) return;

        mc.player.setYRot(yaw);
        mc.player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));

        if (updatePrevious) {
            float deltaYaw = yaw - mc.player.getYRot();
            float deltaPitch = pitch - mc.player.getXRot();

            mc.player.yRotO += deltaYaw;
            mc.player.xRotO += deltaPitch;
            mc.player.xRotO = Mth.clamp(mc.player.xRotO, -90.0F, 90.0F);
        }
    }


    public static void setPlayerRotation(Rotation rotation) {
        Rotation normalizedRotation = rotation.normalize();

        float newYaw = normalizedRotation.yaw();
        float newPitch = Mth.clamp(normalizedRotation.pitch(), -90.0F, 90.0F);

        float deltaYaw = Mth.wrapDegrees(newYaw - mc.player.getYRot());
        float deltaPitch = newPitch - mc.player.getXRot();

        mc.player.setYRot(mc.player.getYRot() + deltaYaw);
        mc.player.setXRot(mc.player.getXRot() + deltaPitch);

        mc.player.yRotO += deltaYaw;
        mc.player.xRotO += deltaPitch;
        mc.player.xRotO = Mth.clamp(mc.player.xRotO, -90.0F, 90.0F);
    }


    @EventHandler
    private void onWorldRender(EventGameRender3D event) {
        RotationTarget active = getActiveRotationTarget();
        if (active == null) return;

        if (!isRotatingAllowed(active)) return;

        if (active.movementCorrection == MovementCorrection.CHANGE_LOOK) {
            if (playerRotation == null || currentRotation == null) return;
            var timerSpeed = Timer.INSTANCE.getTimerSpeed();
            float partialTick = timerSpeed * mc.gameRenderer.getMainCamera().getPartialTickTime();

            Rotation interpolated = playerRotation.interpolateToSmooth(currentRotation, partialTick);

            setPlayerRotation(interpolated);
        }
    }

    @EventHandler
    private void onMouseRotation(EventLook event) {
        RotationTarget active = getActiveRotationTarget();
        if (active == null || !isRotatingAllowed(active)) {
            return;
        }

        if (active.movementCorrection == MovementCorrection.CHANGE_LOOK) {
            float f = (float) event.pitch * 0.15f;
            float g = (float) event.yaw * 0.15f;

            if (playerRotation != null) {
                playerRotation = adjustRotation(playerRotation, f, g);
            }
            if (currentRotation != null) {
                setCurrentRotation(adjustRotation(currentRotation, f, g));
            }
        } else if (active.movementCorrection == MovementCorrection.SILENT) {
            float newPitch = freePitch + (float) event.getPitch() * 0.15f;
            float newYaw = freeYaw + (float) event.getYaw() * 0.15f;

            freeYaw = Mth.wrapDegrees(newYaw);
            freePitch = Mth.clamp(newPitch, -90.0F, 90.0F);
            event.cancel();
        }
    }

    @EventHandler
    private void onRotation(EventRotation event) {
        if (isSilentActive()) {
            event.setYaw(freeYaw);
            event.setPitch(freePitch);
        } else {
            freeYaw = event.getYaw();
            freePitch = event.getPitch();
        }
    }

    private Rotation adjustRotation(Rotation rotation, float f, float g) {
        float newYaw = rotation.yaw() + g;
        float newPitch = Math.clamp(rotation.pitch() + f, -90f, 90f);
        return new Rotation(newYaw, newPitch);
    }

    @EventHandler
    public void onInput(EventInput event) {
//        if (isRotating()) {
//            MoveUtils.fixMovement(event, mc.gameRenderer.getMainCamera().yRot());
//        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPacket(EventPacket event) {
        Rotation r;
        switch (event.getPacket()) {
            case ServerboundMovePlayerPacket p -> {
                if (!p.hasRotation()) return;
                r = new Rotation(p.yRot, p.xRot, true);
            }
            case ClientboundPlayerPositionPacket p -> r = new Rotation(p.change().yRot(), p.change().xRot(), true);
            case ServerboundUseItemPacket p -> r = new Rotation(p.getYRot(), p.getXRot(), true);
            case null, default -> {
                return;
            }
        }
        theoreticalServerRotation = r;
        if (!event.isCancelled()) {
            actualServerRotation = r;
        }
    }
}