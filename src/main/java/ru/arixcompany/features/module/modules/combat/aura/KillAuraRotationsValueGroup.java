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
package ru.arixcompany.features.module.modules.combat.aura;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.FailRotationProcessor;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.RotationProcessor;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.ShortStopRotationProcessor;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.AngleSmooth;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl.AccelerationAngleSmooth;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl.InterpolationAngleSmooth;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl.LinearAngleSmooth;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl.SpookyTimeAngleSmooth;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.PointInsideBox;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.PointTracker;
import ru.arixcompany.features.module.modules.combat.aura.aiming.point.exempts.ExemptBoxPart;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.utils.IMinecraft;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Mirrors LiquidBounce's KillAuraRotationsValueGroup + RotationsValueGroup logic.
 *
 * In LiquidBounce the rotation is submitted via RotationManager.setRotationTarget()
 * with a RotationTarget built from the chosen AngleSmooth processor.
 * This class replicates that exact flow in Java.
 *
 * RotationTiming values mirror KillAuraRotationTiming:
 *   Обычный  — always tracking the target (Normal)
 *   Снап     — aim only when the clicker is about to fire (Snap)
 *   По тику  — send rotation packet only on the attack tick (OnTick)
 */
public class KillAuraRotationsValueGroup implements IMinecraft {

    private final PointTracker pointTracker = new PointTracker();

    /**
     * Mirrors LiquidBounce's ModuleKillAura.processTarget().
     * <p>
     * Finds the rotation to the entity, calculates how many ticks it takes to reach it,
     * then decides whether to start aiming based on the rotation timing setting.
     *
     * @return true if the entity is a valid target (even if we are not aiming yet)
     */
    public boolean processTarget(LivingEntity entity, float attackDistance) {
        if (entity == null || mc.player == null) return false;

        Rotation targetRot = findRotation(entity, attackDistance);
        if (targetRot == null) return false;

        AngleSmooth smoother = buildAngleSmooth();

        /**
         * How long it takes to rotate to a rotation in ticks.
         *
         * Calculates the difference from the server rotation to the target rotation and divides it by the
         * minimum turn speed (to make sure we are always there in time)
         */
        int ticks = smoother.calculateTicks(RotationManager.actualServerRotation, targetRot);

        String timing = HitAura.rotationTiming.getSelected();

        switch (timing) {
            // Снап: If our click scheduler is not going to click the moment we reach the target,
            // we should not start aiming towards the target just yet.
            case "Снап" -> {
                if (ticks > 1) return true;
            }

            // По тику: will always instantly aim onto the target on attack, however, if
            // our rotation is unable to be ready in time, we can at least start aiming towards
            // the target.
            case "По тику" -> {
                if (ticks <= 1) return true;
            }

            // Обычный: Continue with regular aiming — always track
            default -> { /* no-op */ }
        }

        RotationManager.setRotationTarget(
                new RotationTarget(targetRot, buildProcessors(smoother), 5, 2f),
                1,
                this
        );
        return true;
    }

    /**
     * Called on the attack tick to snap rotation to target.
     * Mirrors the ON_TICK branch in ModuleKillAura.gameHandler.
     */
    public void rotateOnTick(LivingEntity target, float attackDistance) {
        if (target == null || mc.player == null) return;
        if (!"По тику".equals(HitAura.rotationTiming.getSelected())) return;

        Rotation targetRot = findRotation(target, attackDistance);
        if (targetRot == null) return;

        AngleSmooth smoother = buildAngleSmooth();
        RotationManager.setRotationTarget(
                new RotationTarget(targetRot, buildProcessors(smoother), 2, 2f),
                1,
                this
        );
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Rotation findRotation(LivingEntity entity, float attackDistance) {
        Vec3 eyes = mc.player.getEyePosition(1.0f);

        // Применяем настройки из HitAura к pointTracker
        applyPointTrackerSettings();

        // Получаем точку через PointTracker
        PointInsideBox point = pointTracker.findPoint(eyes, entity);
        double lengthY = entity.getBoundingBox().getYsize();
        if (HitAura.pointmode.isSelected("Дефолт"))
            return calcRotation(new Vec3(entity.getX(), entity.getY() + lengthY * 0.5, entity.getZ())).normalize();

        Vec3 pos = applyPointMode(point, entity);

        return calcRotation(pos).normalize();
    }

    /**
     * Синхронизирует настройки PointTracker из HitAura.
     */
    private void applyPointTrackerSettings() {
        // Исключённые части хитбокса
        EnumSet<ExemptBoxPart> exempt = EnumSet.noneOf(ExemptBoxPart.class);
        if (HitAura.exemptBoxParts.isSelected("Голова")) exempt.add(ExemptBoxPart.HEAD);
        if (HitAura.exemptBoxParts.isSelected("Тело")) exempt.add(ExemptBoxPart.BODY);
        if (HitAura.exemptBoxParts.isSelected("Ноги")) exempt.add(ExemptBoxPart.FEET);
        pointTracker.exemptBoxParts = exempt;

        // Gaussian
        pointTracker.gaussian.enabled = HitAura.gaussianEnabled.isValue();
        pointTracker.gaussian.yawFactorMin = HitAura.gaussianYawMin.getValue();
        pointTracker.gaussian.yawFactorMax = HitAura.gaussianYawMax.getValue();
        pointTracker.gaussian.pitchFactorMin = HitAura.gaussianPitchMin.getValue();
        pointTracker.gaussian.pitchFactorMax = HitAura.gaussianPitchMax.getValue();
        pointTracker.gaussian.chance = (int) HitAura.gaussianChance.getValue();
        pointTracker.gaussian.speedMin = HitAura.gaussianSpeedMin.getValue();
        pointTracker.gaussian.speedMax = HitAura.gaussianSpeedMax.getValue();

        // Lazy
        pointTracker.lazy.enabled = HitAura.lazyEnabled.isValue();
        pointTracker.lazy.thresholdMin = HitAura.lazyThresholdMin.getValue();
        pointTracker.lazy.thresholdMax = HitAura.lazyThresholdMax.getValue();

        // Delay
        pointTracker.delay.enabled = HitAura.delayEnabled.isValue();
        pointTracker.delay.delayMin = (int) HitAura.delayMin.getValue();
        pointTracker.delay.delayMax = (int) HitAura.delayMax.getValue();
    }

    /**
     * Корректирует Y-координату точки в зависимости от выбранного режима прицела.
     * "Ближайшая" — не трогаем, используем то что вернул PointTracker.
     */
    private Vec3 applyPointMode(PointInsideBox point, LivingEntity entity) {
        var box = entity.getBoundingBox();
        double height = box.getYsize();

        return switch (HitAura.pointMode.getSelected()) {
            case "Голова" -> new Vec3(point.pos().x, box.maxY - height / 6.0, point.pos().z);
            case "Тело" -> new Vec3(point.pos().x, box.minY + height / 2.0, point.pos().z);
            case "Ноги" -> new Vec3(point.pos().x, box.minY + height / 6.0, point.pos().z);
            default -> point.pos(); // Ближайшая
        };
    }

    private AngleSmooth buildAngleSmooth() {
        return switch (HitAura.angleSmooth.getSelected()) {
            case "Интерполяция" -> new InterpolationAngleSmooth(
                    (int) HitAura.horizontalTurnSpeedMin.getValue(),
                    (int) HitAura.horizontalTurnSpeedMax.getValue(),
                    (int) HitAura.verticalTurnSpeedMin.getValue(),
                    (int) HitAura.verticalTurnSpeedMax.getValue(),
                    95, 100, 0.35f
            );
            case "Ускорение" -> new AccelerationAngleSmooth(
                    HitAura.yawAccelerationMin.getValue(),
                    HitAura.yawAccelerationMax.getValue(),
                    HitAura.pitchAccelerationMin.getValue(),
                    HitAura.pitchAccelerationMax.getValue(),
                    false, 10f, 0.3f,
                    0.1f, 0.1f, 0.1f, 0.1f
            );
            case "SpookyTime" -> new SpookyTimeAngleSmooth();
            default -> new LinearAngleSmooth(
                    HitAura.horizontalTurnSpeedMin.getValue(),
                    HitAura.horizontalTurnSpeedMax.getValue(),
                    HitAura.verticalTurnSpeedMin.getValue(),
                    HitAura.verticalTurnSpeedMax.getValue()
            );
        };
    }

    private List<RotationProcessor> buildProcessors(AngleSmooth smoother) {
        List<RotationProcessor> list = new ArrayList<>();
        list.add(smoother);

        if (HitAura.shortStop.isValue()) {
            list.add(new ShortStopRotationProcessor(3, 1, 2));
        }

        if (HitAura.fail.isValue()) {
            var fp = new FailRotationProcessor();
            fp.enabled = true;
            list.add(fp);
        }

        return list;
    }

    private Rotation calcRotation(Vec3 targetPos) {
        Vec3 eye = mc.player.getEyePosition(1.0f);
        Vec3 dir = targetPos.subtract(eye).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Mth.clamp(
                -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))),
                -90.0, 90.0
        );
        return new Rotation(yaw, pitch);
    }
}
