package ru.arixcompany.features.module.modules.combat.aura.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.combat.HitAura;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.rotation.AbstractRotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.RotationRepo;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.combat.aura.utils.Rotation;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.MathUtils;

public class SpookyTimeRot implements AbstractRotation, IMinecraft {

    public float pitchAcceleration = 1f;

    @Override
    public void rotate(LivingEntity target, boolean isAttack, float attackDistance, boolean check) {
        if (target == null || mc.player == null) return;

        Vec3 targetPos = getTargetPoint(target);
        double maxHeight = (AuraUtil.getStrictDistance(target) / attackDistance);
        float f = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float f1 = mc.level.tickRateManager().isEntityFrozen(mc.player) ? 1.0F : f;
        pitchAcceleration = AttackHandler.anyEntityOnRay(target, Arix.getInstance().getModuleRepo().getModule(HitAura.class).attackRange.getValue() + Arix.getInstance().getModuleRepo().getModule(HitAura.class).preRange.getValue())
                ? 0.5f : pitchAcceleration < MathUtils.randomValue(0.5f,2.5f) ? pitchAcceleration * 1.5f : MathUtils.randomValue(0.5f,2.5f);

        Vec3 vec = targetPos
                .add(0, Mth.clamp(mc.player.getEyePosition(1.0f).y - target.getY(), 0, maxHeight), 0)
                .subtract(mc.player.getEyePosition(1.0f))
                .normalize();

        float sin = (float) Math.sin(Util.getNanos() / 1.0E9 * Math.TAU * 2);
        float cos = (float) Math.cos(Util.getNanos() / 1.0E9 * Math.TAU * 2);

        float yawOffset = MathUtils.randomValue(10,15) * sin;
        float pitchOffest = 5 * cos;

        float rawYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float rawPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90F, 90F);

        float targetYaw = rawYaw + yawOffset;
        float targetPitch = rawPitch + pitchOffest;
        Rotation rotation = new Rotation(targetYaw, targetPitch).normalize();

        RotationRepo.update(
                rotation,
                MathUtils.randomValue(9,11),
                pitchAcceleration,
                MathUtils.randomValue(6,8),
                2,
                0,
                5,
                false
        );
    }

    private Vec3 getTargetPoint(LivingEntity entity) {
        double lengthY = entity.getBoundingBox().getYsize();
        return entity.getPosPlayer().add(0, lengthY * 0.5, 0);
    }
}