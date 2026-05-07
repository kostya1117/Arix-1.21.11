package ru.arixcompany.features.module.modules.combat.aura.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import ru.arixcompany.utils.IMinecraft;

@UtilityClass
public class AuraUtil implements IMinecraft {

    public double getStrictDistance(Entity entity) {
        return getClosestVec(entity).length();
    }

    public boolean validDistance(Entity entity, float distance) {
        return getStrictDistance(entity) < distance;
    }

    public Vec3 getClosestVec(Entity entity) {
        Vec3 eyePos = mc.player.getEyePosition();
        return getClosestVec(eyePos, entity).subtract(eyePos);
    }

    public Vec3 getClosestVec(Vec3 vec, AABB aabb) {
        return new Vec3(
                Mth.clamp(vec.x, aabb.minX, aabb.maxX),
                Mth.clamp(vec.y, aabb.minY, aabb.maxY),
                Mth.clamp(vec.z, aabb.minZ, aabb.maxZ)
        );
    }

    public Vec3 getClosestVec(Vec3 vec, Entity entity) {
        return getClosestVec(vec, entity.getBoundingBox());
    }

    public double direction(float rotationYaw, float moveForward, float moveStrafing) {
        if (moveForward < 0.0F) rotationYaw += 180.0F;

        float forward = 1.0F;
        if (moveForward < 0.0F) forward = -0.5F;
        else if (moveForward > 0.0F) forward = 0.5F;

        if (moveStrafing > 0.0F) rotationYaw -= 90.0F * forward;
        if (moveStrafing < 0.0F) rotationYaw += 90.0F * forward;

        return Math.toRadians(rotationYaw);
    }
}
