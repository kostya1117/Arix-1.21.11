package ru.arixcompany.features.module.modules.combat.aura;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationManager;
import ru.arixcompany.features.module.modules.combat.aura.aiming.RotationTarget;
import ru.arixcompany.features.module.modules.combat.aura.aiming.data.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.RotationProcessor;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.FactorAngleSmooth;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.processors.anglesmooth.impl.*;
import ru.arixcompany.features.module.modules.combat.aura.aiming.features.MovementCorrection;
import ru.arixcompany.features.module.modules.combat.aura.utils.BoxPoints;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.PredictUtils;

import java.util.ArrayList;
import java.util.List;

public class KillAuraRotationsValueGroup implements IMinecraft {

    public boolean processTarget(LivingEntity entity,float distanceattack) {
        if (entity == null || mc.player == null) return false;

        Rotation targetRot = findRotation(entity,distanceattack);
        FactorAngleSmooth smoother;
        if (mc.player.isFallFlying() && HitAura.elytraTarget.isValue())
            smoother = new ElytraAngleSmooth();
        else
            smoother = buildAngleSmooth();
        MovementCorrection correction = getCorrectionMode();

        RotationManager.setRotationTarget(
                new RotationTarget(targetRot,entity, buildProcessors(smoother), 5, 2f, correction),
                1,
                RotationManager.getInstance()
        );
        return true;
    }

    private MovementCorrection getCorrectionMode() {
        return switch (HitAura.motion.getSelected()) {
            case "Изменять взгяд игрока" -> MovementCorrection.CHANGE_LOOK;
            case "Направлять"       -> MovementCorrection.SILENT;
            default                -> MovementCorrection.OFF;
        };
    }

    private Rotation findRotation(LivingEntity entity,float distanceattack) {
        Vec3 eyes = mc.player.getEyePosition();

        Vec3 targetPos;
        if (mc.player.isFallFlying() && HitAura.elytraTarget.isValue()) {
            targetPos = PredictUtils.predict(entity,4);
            return Rotation.lookingAt(targetPos, eyes);
        }

        return Rotation.calculateToEntity(entity);
    }

    private FactorAngleSmooth buildAngleSmooth() {
        return switch (HitAura.angleSmooth.getSelected()) {
            case "Интерполяция" -> new InterpolationAngleSmooth(
                    (int) HitAura.horizontalTurnSpeedMin.getValue(),
                    (int) HitAura.horizontalTurnSpeedMax.getValue(),
                    (int) HitAura.verticalTurnSpeedMin.getValue(),
                    (int) HitAura.verticalTurnSpeedMax.getValue(),
                    55, 85, 0.35f
            );
            case "FuntimeSnap"  -> new FuntimeSnap();
            case "SpookyTime"   -> new SpookyTimeAngleSmooth();
            default             -> new LinearAngleSmooth(
                    HitAura.horizontalTurnSpeedMin.getValue(),
                    HitAura.horizontalTurnSpeedMax.getValue(),
                    HitAura.verticalTurnSpeedMin.getValue(),
                    HitAura.verticalTurnSpeedMax.getValue()
            );
        };
    }

    private List<RotationProcessor> buildProcessors(FactorAngleSmooth smoother) {
        List<RotationProcessor> list = new ArrayList<>();
        list.add(smoother);
        return list;
    }
}
