package ru.arixcompany.features.module.modules.combat.aura.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.rotation.AbstractRotation;
import ru.arixcompany.features.module.modules.combat.aura.utils.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.RotationRepo;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.MathUtils;

import java.security.SecureRandom;

public class FuntimeRot implements AbstractRotation, IMinecraft {

    @Override
    public void rotate(LivingEntity target, boolean isAttack, float attackDistance, boolean check) {
        Vec3 targetPos = getTargetPoint(target);
        double maxHeight = (AuraUtil.getStrictDistance(target) / attackDistance);
        Vec3 vec = targetPos
                .add(0, Mth.clamp(mc.player.getEyePosition(1.0f).y - target.getY(), 0, maxHeight), 0)
                .subtract(mc.player.getEyePosition(1.0f))
                .normalize();

        float rawYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float rawPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90F, 90F);

        float cos = (float) Math.cos(System.currentTimeMillis() / 100D);
        float sin = (float) Math.sin(System.currentTimeMillis() / 100D);
        float yawOffset =  (MathUtils.randomValue(4, 8) * cos);
        float pitchOffset = (MathUtils.randomValue(4, 8) * sin);

        float targetYaw = rawYaw + yawOffset;
        float targetPitch = Mth.clamp(rawPitch + pitchOffset, -90F, 90F);

        Rotation rotation = new Rotation(
                targetYaw,
                targetPitch
        ).adjustSensitivity();

        RotationRepo.update(rotation,10F, 3, 8F, 3F, 0, 5, false);
    }

    private Vec3 getTargetPoint(LivingEntity entity) {
        double lengthY = entity.getBoundingBox().getYsize();
        return entity.getPosPlayer().add(0, lengthY * 0.4, 0);
    }
}