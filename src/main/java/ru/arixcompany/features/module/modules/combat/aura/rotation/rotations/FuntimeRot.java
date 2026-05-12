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

    private static Rotation lastRotation = null;
    private static float targetPitchSmooth = 0;
    private static boolean wasOnTarget = false;

    @Override
    public void rotate(LivingEntity target, boolean isAttack, float attackDistance, boolean check) {
        if (target == null || mc.player == null) return;

        long now = System.currentTimeMillis();

        Vec3 targetPos = getTargetPoint(target);
        double maxHeight = (AuraUtil.getStrictDistance(target) / attackDistance);
        Vec3 vec = targetPos
                .add(0, Mth.clamp(mc.player.getEyePosition(1.0f).y - target.getY(), 0, maxHeight), 0)
                .subtract(mc.player.getEyePosition(1.0f))
                .normalize();

        float rawYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float rawPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90F, 90F);

        // Проверяем находимся ли на таргете
        boolean onTarget = isAttack && AuraUtil.getStrictDistance(target) < attackDistance && !check;

        // Более плавные волны для естественного движения
        float cos = (float) Math.cos(now / 150D);
        float sin = (float) Math.sin(now / 200D);
        
        // Меньше смещения для менее заметного движения
        float yawOffset = (MathUtils.randomValue(2.5f, 5.5f) * cos);
        float pitchOffset = (MathUtils.randomValue(2.5f, 5.5f) * sin);

        float targetYaw = rawYaw + yawOffset;
        
        // Логика для питча:
        // Когда на таргете - питч почти не меняется
        // Когда не на таргете - плавно вводится к целевому питчу
        float targetPitch;
        if (onTarget) {
            // На таргете - питч остается стабильным
            targetPitch = rawPitch;
            targetPitchSmooth = rawPitch;
            wasOnTarget = true;
        } else {
            // Не на таргете - плавно вводим питч
            if (wasOnTarget) {
                targetPitchSmooth = mc.player.getXRot();
                wasOnTarget = false;
            }
            
            // Плавно интерполируем к целевому питчу
            float pitchDelta = rawPitch - targetPitchSmooth;
            float pitchSpeed = 0.15f;  // Скорость интерполяции
            targetPitchSmooth += pitchDelta * pitchSpeed;
            
            targetPitch = Mth.clamp(targetPitchSmooth + pitchOffset, -90F, 90F);
        }

        Rotation rotation = new Rotation(targetYaw, targetPitch).normalize();

        // Интерполируем для плавности используя partialTicks из Minecraft
        if (lastRotation != null) {
            float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            rotation = lastRotation.interpolateTo(rotation, partialTicks);
        }
        lastRotation = rotation;

        // Более естественные скорости
        RotationRepo.update(
                rotation,
                25F,  // yawSpeed - более плавный
                onTarget ? 3F : 6F,   // pitchSpeed - медленнее когда на таргете
                15F,  // yawReturnSpeed
                8F,   // pitchReturnSpeed
                0,
                5,
                false
        );
    }

    private Vec3 getTargetPoint(LivingEntity entity) {
        double lengthY = entity.getBoundingBox().getYsize();
        return entity.getPosPlayer().add(0, lengthY * 0.4, 0);
    }
}
