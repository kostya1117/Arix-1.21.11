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
package ru.arixcompany.features.module.modules.combat;

import lombok.Getter;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ShieldItem;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventSprint;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventPreTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.KillAuraRotationsValueGroup;
import ru.arixcompany.features.module.modules.combat.aura.TargetHandler;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;

public class HitAura extends Module {

    // ── Дальность ─────────────────────────────────────────────────────────
    public static final ValueSetting attackRange =
        new ValueSetting("Радиус атаки")
            .range(3.0f, 6.0f).setValue(3.0f).setStep(0.1f);

    public static final ValueSetting preRange =
        new ValueSetting("Радиус обнаружения")
            .range(0.0f, 5.0f).setValue(1.0f).setStep(0.1f);

    // ── Сглаживание угла (AngleSmooth) ────────────────────────────────────
    public static final SelectSetting angleSmooth =
        new SelectSetting("Сглаживание")
            .value("Линейное", "Интерполяция", "Ускорение", "SpookyTime");

    // ── Тайминг ротации (RotationTiming) ──────────────────────────────────
    public static final SelectSetting rotationTiming =
        new SelectSetting("Тайминг ротации")
            .value("Обычный", "Снап", "По тику");

    // ── Сквозь стены (ThroughWalls) ───────────────────────────────────────
    public static final BooleanSetting throughWalls =
        new BooleanSetting("Сквозь стены").setValue(false);

    // ── Горизонтальная скорость (HorizontalTurnSpeed) ─────────────────────
    public static final ValueSetting horizontalTurnSpeedMin =
        new ValueSetting("Горизонталь мин.")
            .range(1.0f, 180.0f).setValue(20.0f).setStep(1.0f)
            .visible(() -> !angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting horizontalTurnSpeedMax =
        new ValueSetting("Горизонталь макс.")
            .range(1.0f, 180.0f).setValue(45.0f).setStep(1.0f)
            .visible(() -> !angleSmooth.isSelected("Ускорение"));

    // ── Вертикальная скорость (VerticalTurnSpeed) ─────────────────────────
    public static final ValueSetting verticalTurnSpeedMin =
        new ValueSetting("Вертикаль мин.")
            .range(1.0f, 180.0f).setValue(10.0f).setStep(1.0f)
            .visible(() -> !angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting verticalTurnSpeedMax =
        new ValueSetting("Вертикаль макс.")
            .range(1.0f, 180.0f).setValue(25.0f).setStep(1.0f)
            .visible(() -> !angleSmooth.isSelected("Ускорение"));

    // ── Ускорение по Yaw (YawAcceleration) ───────────────────────────────
    public static final ValueSetting yawAccelerationMin =
        new ValueSetting("Ускорение Yaw мин.")
            .range(1.0f, 180.0f).setValue(20.0f).setStep(1.0f)
            .visible(() -> angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting yawAccelerationMax =
        new ValueSetting("Ускорение Yaw макс.")
            .range(1.0f, 180.0f).setValue(25.0f).setStep(1.0f)
            .visible(() -> angleSmooth.isSelected("Ускорение"));

    // ── Ускорение по Pitch (PitchAcceleration) ────────────────────────────
    public static final ValueSetting pitchAccelerationMin =
        new ValueSetting("Ускорение Pitch мин.")
            .range(1.0f, 180.0f).setValue(20.0f).setStep(1.0f)
            .visible(() -> angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting pitchAccelerationMax =
        new ValueSetting("Ускорение Pitch макс.")
            .range(1.0f, 180.0f).setValue(25.0f).setStep(1.0f)
            .visible(() -> angleSmooth.isSelected("Ускорение"));

    public static final SelectSetting pointmode =
            new SelectSetting("Выбор точки")
                    .value("Дефолт", "Продвинутое");

    // ── Короткая остановка (ShortStop) ────────────────────────────────────
    public static final BooleanSetting shortStop =
        new BooleanSetting("Короткая остановка").setValue(false);

    // ── Промахи (Fail) ────────────────────────────────────────────────────
    public static final BooleanSetting fail =
        new BooleanSetting("Промахи").setValue(false);


    // ── PointTracker — выбор точки ────────────────────────────────────────
    public static final SelectSetting pointMode =
        new SelectSetting("Точка прицела")
            .value("Ближайшая", "Голова", "Тело", "Ноги").visible(() -> pointmode.isSelected("Продвинутое"));

    public static final ListSetting exemptBoxParts =
        new ListSetting("Исключить части хитбокса")
            .value("Голова", "Тело", "Ноги").visible(() -> pointmode.isSelected("Продвинутое"));

    // ── PointTracker — Gaussian (рандомизация) ────────────────────────────
    public static final BooleanSetting gaussianEnabled =
        new BooleanSetting("Гауссовый шум").setValue(false).visible(() -> pointmode.isSelected("Продвинутое"));

    public static final ValueSetting gaussianYawMin =
        new ValueSetting("Гаусс Yaw мин.")
            .range(0.0f, 2.0f).setValue(0.3f).setStep(0.01f)
            .visible(() -> gaussianEnabled.isValue());

    public static final ValueSetting gaussianYawMax =
        new ValueSetting("Гаусс Yaw макс.")
            .range(0.0f, 2.0f).setValue(0.6f).setStep(0.01f)
            .visible(() -> gaussianEnabled.isValue());

    public static final ValueSetting gaussianPitchMin =
        new ValueSetting("Гаусс Pitch мин.")
            .range(0.0f, 2.0f).setValue(0.3f).setStep(0.01f)
            .visible(() -> gaussianEnabled.isValue());

    public static final ValueSetting gaussianPitchMax =
        new ValueSetting("Гаусс Pitch макс.")
            .range(0.0f, 2.0f).setValue(0.6f).setStep(0.01f)
            .visible(() -> gaussianEnabled.isValue());

    public static final ValueSetting gaussianChance =
        new ValueSetting("Гаусс шанс")
            .range(0, 100).setValue(100).setStep(1f)
            .visible(() -> gaussianEnabled.isValue());

    public static final ValueSetting gaussianSpeedMin =
        new ValueSetting("Гаусс скорость мин.")
            .range(0.01f, 1.0f).setValue(0.1f).setStep(0.01f)
            .visible(() -> gaussianEnabled.isValue());

    public static final ValueSetting gaussianSpeedMax =
        new ValueSetting("Гаусс скорость макс.")
            .range(0.01f, 1.0f).setValue(0.2f).setStep(0.01f)
            .visible(() -> gaussianEnabled.isValue());

    // ── PointTracker — Lazy (порог смены точки) ───────────────────────────
    public static final BooleanSetting lazyEnabled =
        new BooleanSetting("Ленивая точка").setValue(false).visible(() -> pointmode.isSelected("Продвинутое"));

    public static final ValueSetting lazyThresholdMin =
        new ValueSetting("Ленивый порог мин.")
            .range(0.0f, 1.0f).setValue(0.1f).setStep(0.01f)
            .visible(() -> lazyEnabled.isValue());

    public static final ValueSetting lazyThresholdMax =
        new ValueSetting("Ленивый порог макс.")
            .range(0.0f, 1.0f).setValue(0.2f).setStep(0.01f)
            .visible(() -> lazyEnabled.isValue());

    // ── PointTracker — Delay (задержка смены точки) ───────────────────────
    public static final BooleanSetting delayEnabled =
        new BooleanSetting("Задержка точки").setValue(false).visible(() -> pointmode.isSelected("Продвинутое"));

    public static final ValueSetting delayMin =
        new ValueSetting("Задержка мин. (тики)")
            .range(1, 20).setValue(2).setStep(1f)
            .visible(() -> delayEnabled.isValue());

    public static final ValueSetting delayMax =
        new ValueSetting("Задержка макс. (тики)")
            .range(1, 20).setValue(4).setStep(1f)
            .visible(() -> delayEnabled.isValue());

    // ── Цели и проверки ───────────────────────────────────────────────────
    public static final ListSetting targets =
        new ListSetting("Цели")
            .value("Игроки", "Инвизки", "Голые", "Мобы")
            .selected("Игроки");

    public static final ListSetting misc =
        new ListSetting("Проверки до удара")
            .value(
                "Бить через блоки",
                "Бить только оружием",
                "Не бить если кушаеш",
                "Не атакавать в контейнере",
                "Райкаст"
            );

    public static final ListSetting extraSettings =
        new ListSetting("Доп.настройка")
            .value("Умные криты", "Сброс спринта", "Фикс удара при HurtTime");

    public final SelectSetting sprintReset =
        new SelectSetting("Режим сброса спринта")
            .value("Пакет", "Легит")
            .visible(() -> extraSettings.isSelected("Сброс спринта"));

    public static final SelectSetting motion =
        new SelectSetting("Режим движения")
            .value("Без", "Свободная", "Сфокусированная");

    // ── Состояние ─────────────────────────────────────────────────────────
    @Getter
    public static LivingEntity target;
    private final TargetHandler targetHandler = new TargetHandler();
    private final KillAuraRotationsValueGroup rotations = new KillAuraRotationsValueGroup();

    public int count;

    // ── Конструктор ───────────────────────────────────────────────────────
    public HitAura() {
        super("HitAura", Category.Combat);
        setup(
            attackRange,
            preRange,
            angleSmooth,
            rotationTiming,
            throughWalls,
            horizontalTurnSpeedMin,
            horizontalTurnSpeedMax,
            verticalTurnSpeedMin,
            verticalTurnSpeedMax,
            yawAccelerationMin,
            yawAccelerationMax,
            pitchAccelerationMin,
            pitchAccelerationMax,
            pointmode,
            shortStop,
            fail,
            // PointTracker
            pointMode,
            exemptBoxParts,
            gaussianEnabled,
            gaussianYawMin,
            gaussianYawMax,
            gaussianPitchMin,
            gaussianPitchMax,
            gaussianChance,
            gaussianSpeedMin,
            gaussianSpeedMax,
            lazyEnabled,
            lazyThresholdMin,
            lazyThresholdMax,
            delayEnabled,
            delayMin,
            delayMax,
            // Цели
            targets,
            misc,
            extraSettings,
            motion,
            sprintReset
        );
    }

    // ── События ───────────────────────────────────────────────────────────

    @EventHandler
    public void onEvent(EventGameTick e) {
        if (target != null && mc.player != null && mc.level != null) {
            updateRotation();
        }
    }

    @EventHandler
    public void onPreAttack(EventPreTick e) {
        if (target == null || mc.player == null || mc.level == null) return;
        if (shouldResetSprintForCrit() && hasStopSprint() && AuraUtil.validDistance(target, attackRange.getValue())) {
            if (sprintReset.isSelected("Легит") && extraSettings.isSelected("Сброс спринта")) {
                mc.player.setSprinting(false);
            }
        }
    }

    @EventHandler
    public void onPreAttack(EventSprint e) {
        if (target == null || mc.player == null || mc.level == null) return;
        if (shouldResetSprintForCrit() && hasStopSprint() && AuraUtil.validDistance(target, attackRange.getValue())) {
            if (sprintReset.isSelected("Легит") && extraSettings.isSelected("Сброс спринта")) {
                e.setSprinting(false);
            }
        }
    }

    @EventHandler
    public void onEventsss(EventPreTick e) {
        if (mc.player == null || !mc.player.isAlive() || mc.player.isDeadOrDying()) {
            this.toggle();
            return;
        }

        targetHandler.updateTarget();
        target = targetHandler.getTarget();

        if (target != null && mc.player != null && mc.level != null) {
            if (!checkToAttack()) {
                // Mirrors LiquidBounce's ON_TICK branch: snap rotation on the attack tick
                rotations.rotateOnTick(target, attackRange.getValue());
                AttackHandler.performAttack(target, misc.isSelected("Райкаст"), attackRange.getValue());
            }
        } else {
            reset();
        }
    }

    // ── Ротация ───────────────────────────────────────────────────────────

    private void updateRotation() {
        if (target == null) return;
        // Mirrors LiquidBounce's ModuleKillAura.processTarget() call from updateTarget()
        rotations.processTarget(target, attackRange.getValue());
    }

    // ── Вспомогательные ───────────────────────────────────────────────────

    @Override
    public void toggle() {
        super.toggle();
        reset();
    }

    private void reset() {
        target = null;
        if (mc.player != null) count = 0;
    }

    private boolean shouldResetSprintForCrit() {
        return mc.player.fallDistance > 0.0f;
    }

    public boolean hasStopSprint() {
        return !AttackHandler.hasMovementRestrictions();
    }

    private boolean checkToAttack() {
        return mc.player.isUsingItem()
            && misc.isSelected("Не бить если кушаеш")
            && !(mc.player.getActiveItem().getItem() instanceof ShieldItem)
            || mc.screen != null && misc.isSelected("Не атакавать в контейнере")
            || !mc.player.getMainHandItem().is(ItemTags.SWORDS)
            && !mc.player.getMainHandItem().is(ItemTags.AXES)
            && misc.isSelected("Бить только оружием");
    }
}
