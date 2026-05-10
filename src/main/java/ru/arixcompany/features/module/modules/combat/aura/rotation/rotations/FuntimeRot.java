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

import java.security.SecureRandom;

public class FuntimeRot implements AbstractRotation, IMinecraft {
    private static float randomLerp(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }

    @Override
    public void rotate(LivingEntity target, boolean isAttack, float attackDistance, boolean check) {
        Vec3 targetPos = getTargetPoint(target);
        double maxHeight = (AuraUtil.getStrictDistance(target) / attackDistance);
        Vec3 vec = targetPos
                .add(0, Mth.clamp(mc.player.getEyePosition().y - target.getY(), 0, maxHeight), 0)
                .subtract(mc.player.getEyePosition())
                .normalize();

        float rawYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float rawPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90F, 90F);

        float speed = new SecureRandom().nextBoolean() ? randomLerp(0.4F, 0.7F) : randomLerp(0.5F, 0.9F);

        float cos = (float) Math.cos(System.currentTimeMillis() / 100D);
        float sin = (float) Math.sin(System.currentTimeMillis() / 100D);
        float yawOffset = (float) Math.ceil(randomLerp(6, 11) * cos + (1F - cooldownFromLastSwing()) * (randomLerp(35, 55) * (Arix.getInstance().getModuleRepo().getModule(HitAura.class).count == 0 ? 1 : -1)));
        float pitchOffset = (float) Math.ceil(randomLerp(3, 9) * sin + (1F - cooldownFromLastSwing()) * (randomLerp(15, 35) * (Arix.getInstance().getModuleRepo().getModule(HitAura.class).count == 0 ? 1 : -1)));

        float targetYaw = rawYaw + yawOffset;
        float targetPitch = Mth.clamp(rawPitch + pitchOffset, -90F, 90F);

        float finalYaw = wrapLerp(speed, mc.player.yRot, Mth.wrapDegrees(targetYaw));
        float finalPitch = wrapLerp(speed / 2F, mc.player.xRot, targetPitch);

        Rotation rotation = new Rotation(
                mc.player.yRot + (float) Math.ceil(Mth.wrapDegrees(finalYaw) - Mth.wrapDegrees(mc.player.yRot)),
                mc.player.xRot + (float) Math.ceil(Mth.wrapDegrees(finalPitch) - Mth.wrapDegrees(mc.player.xRot))
        ).adjustSensitivity();

        boolean toFast = cooldownFromLastSwing() > 0.5F;
        RotationRepo.update(rotation, toFast && rayTrace() ? 15 : 25F, 10F, 6F, 4F, 0, 5, false);
    }

    private Vec3 getTargetPoint(LivingEntity entity) {
        double lengthY = entity.getBoundingBox().getYsize();
        return entity.getPosPlayer().add(0, lengthY * 0.5, 0);
    }

    private static float wrapLerp(float speed, float from, float to) {
        float delta = Mth.wrapDegrees(to - from);
        return from + delta * speed;
    }

    private float cooldownFromLastSwing() {
        return mc.player.getAttackStrengthScale(1.0f);
    }

    private boolean rayTrace() {
        HitResult result = mc.level.clip(new ClipContext(mc.player.getEyePosition(), Arix.getInstance().getModuleRepo().getModule(HitAura.class).getTarget().getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }
}