/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package ru.arixcompany.features.module.modules.combat.aura.aiming;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.features.event.player.EventLook;
import ru.arixcompany.features.event.player.EventMotion;
import ru.arixcompany.features.event.player.EventRotation;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.FailRotationProcessor;
import ru.arixcompany.features.module.modules.combat.aura.rotation.Component;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.utils.player.MoveUtils;

/**
 * A rotation manager
 */
public class RotationManager extends Component {

    private static RotationManager INSTANCE;

    public static RotationManager getInstance() {
        if (INSTANCE == null) INSTANCE = new RotationManager();
        return INSTANCE;
    }

    /**
     * Our final target rotation. This rotation is only used to define our current rotation.
     * Mirrors LiquidBounce's rotationTargetHandler.
     */
    private static final RequestHandler<RotationTarget> rotationTargetHandler = new RequestHandler<>();

    private static RotationTarget getRotationTarget() {
        return rotationTargetHandler.getActiveRequestValue();
    }

    /**
     * activeRotationTarget = rotationTarget ?: previousRotationTarget
     * Mirrors LiquidBounce's activeRotationTarget property.
     */
    public static RotationTarget activeRotationTarget = null;
    public static RotationTarget previousRotationTarget = null;

    /**
     * The rotation we want to aim at. This DOES NOT mean that the server already received this rotation.
     */
    public static Rotation currentRotation = null;

    // Used for rotation interpolation
    public static Rotation previousRotation = null;

    // Used for player rotation tracking (mirrors LiquidBounce's playerRotation)
    public static Rotation playerRotation = null;

    /**
     * The rotation that was already sent to the server and is currently active.
     */
    public static Rotation actualServerRotation = Rotation.ZERO;

    public static boolean isRotating() {
        return activeRotationTarget != null;
    }

    /**
     * Mirrors LiquidBounce's setRotationTarget(plan, priority, provider).
     * provider = the KillAuraRotationsValueGroup instance (used for deduplication).
     */
    public static void setRotationTarget(RotationTarget target, int priority, Object provider) {
        if (!isRotatingAllowed(target)) return;

        rotationTargetHandler.request(
            new RequestHandler.Request<>(target.ticksUntilReset, priority, provider, target)
        );
    }

    public static void setRotationTarget(RotationTarget target, int priority) {
        setRotationTarget(target, priority, RotationManager.class);
    }

    public static void setRotationTarget(RotationTarget target) {
        setRotationTarget(target, 1);
    }

    /**
     * Checks if the rotation is allowed to be updated.
     * Mirrors LiquidBounce's isRotatingAllowed().
     */
    public static boolean isRotatingAllowed(RotationTarget target) {
        if (mc.player == null) return false;
        if (mc.screen != null) return false;
        return true;
    }

    /**
     * Immediately clears all rotation state.
     */
    public static void stopRotation() {
        currentRotation        = null;
        previousRotation       = null;
        activeRotationTarget   = null;
        previousRotationTarget = null;
    }

    // ── Tick handler ──────────────────────────────────────────────────────

    @EventHandler
    public void onTick(EventGameTick event) {
        if (mc.player == null) return;

        // Tick FailRotationProcessor if present
        if (activeRotationTarget != null) {
            for (var p : activeRotationTarget.processors) {
                if (p instanceof FailRotationProcessor fp) {
                    fp.onTick(event);
                }
            }
        }

        update();
    }

    private static void update() {
        if (mc.player == null) return;

        Rotation playerRot = new Rotation(mc.player);
        playerRotation = playerRot;

        RotationTarget rotationTarget = getRotationTarget();
        RotationTarget active = rotationTarget != null ? rotationTarget : previousRotationTarget;

        activeRotationTarget = active;

        if (active == null) {
            if (currentRotation != null) {
                currentRotation = null;
            } else {
                freeYaw = mc.player.getYRot();
                freePitch = mc.player.getXRot();
            }
            rotationTargetHandler.tick();
            return;
        }

        if (isRotatingAllowed(active)) {
            Rotation fromRotation = currentRotation != null ? currentRotation : playerRot;
            boolean isResetting = rotationTarget == null;
            Rotation rotation = active.towards(fromRotation, isResetting).normalize();

            float diff = rotation.angleTo(playerRot);

            if (isResetting && (active.processors.isEmpty() || diff <= active.resetThreshold)) {
                if (currentRotation != null) {
                    mc.player.setYRot(freeYaw);
                    mc.player.setXRot(freePitch);
                }
                currentRotation        = null;
                previousRotationTarget = null;
            } else {
                previousRotation       = fromRotation;
                currentRotation        = rotation;
                previousRotationTarget = active;

                mc.player.setYRot(rotation.yaw());
                mc.player.setXRot(rotation.pitch());
            }
        }

        rotationTargetHandler.tick();
    }

    /**
     * Applies the current rotation to the player via EventMotion (серверный пакет).
     */
    @EventHandler
    public void onRotation(EventMotion event) {
        if (currentRotation == null) return;
        event.setYaw(currentRotation.yaw());
        event.setPitch(currentRotation.pitch());
    }

    /**
     * freeYaw/freePitch — куда смотрит камера.
     * Синхронизируются из Camera пока аура не активна (как в FreeLookRepo).
     * Когда аура активна — обновляются только дельтами мыши.
     */
    public static float freeYaw   = 0f;
    public static float freePitch = 0f;

    /**
     * EventRotation — контролирует камеру (Camera.java).
     * В режиме "Свободная": подставляем freeYaw/freePitch.
     * Иначе — не трогаем, Camera читает из mc.player.
     */
    @EventHandler
    public void onCameraRotation(EventRotation event) {
        if (currentRotation == null) {
            // Аура не активна — синхронизируем freeYaw/freePitch с камерой (как FreeLookRepo)
            freeYaw   = event.getYaw();
            freePitch = event.getPitch();
            return;
        }

        if ("Свободная".equals(ru.arixcompany.features.module.modules.combat.HitAura.motion.getSelected())) {
            event.setYaw(freeYaw);
            event.setPitch(freePitch);
        }
    }

    /**
     * onLook — движение мыши.
     * Когда аура активна: обновляем freeYaw/freePitch дельтой мыши.
     * Когда аура не активна: не трогаем — freeYaw синхронизируется из Camera в onCameraRotation.
     */
    @EventHandler
    public void onLook(EventLook event) {
        if (currentRotation == null) return;

        float f = (float) (event.getPitch() * 0.15);
        float g = (float) (event.getYaw()   * 0.15);

        freePitch = Mth.clamp(freePitch + f, -90f, 90f);
        freeYaw   = freeYaw + g;

        if ("Свободная".equals(ru.arixcompany.features.module.modules.combat.HitAura.motion.getSelected())) {
            event.cancel();
            return;
        }

        if (playerRotation != null) {
            playerRotation = new Rotation(
                playerRotation.yaw()   + g,
                Mth.clamp(playerRotation.pitch() + f, -90f, 90f)
            );
        }

        currentRotation = new Rotation(
            currentRotation.yaw()   + g,
            Mth.clamp(currentRotation.pitch() + f, -90f, 90f)
        );

        event.cancel();
    }

    // ── EventInput — movement correction ─────────────────────────────────

    @EventHandler
    public void onInput(EventInput event) {
        if (isRotating()) {
            MoveUtils.fixMovement(event, mc.gameRenderer.getMainCamera().yRot());
        }
    }

    // ── Packet tracking ───────────────────────────────────────────────────

    /**
     * Track rotation changes
     */
    @EventHandler
    public static void onPacket(EventPacket event) {
        Rotation r;
        switch (event.getPacket()) {
            case ServerboundMovePlayerPacket p -> {
                if (!p.hasRotation()) return;
                r = new Rotation(p.yRot, p.xRot, true);
            }
            case ClientboundPlayerPositionPacket p ->
                r = new Rotation(p.change().yRot(), p.change().xRot(), true);
            case ServerboundUseItemPacket p ->
                r = new Rotation(p.getYRot(), p.getXRot(), true);
            case null, default -> { return; }
        }
        if (!event.isCancelled()) {
            actualServerRotation = r;
        }
    }
}
