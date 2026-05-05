package ru.arixcompany.utils.aurautil;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.arixcompany.utils.IMinecraft;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;

public class AuraUtil implements IMinecraft {
    public static boolean checkRtx(float yaw, float pitch, float distance, float wallDistance) {
        Vec3 eyePos = mc.player.getPosition(1F).add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        Vec3 vec3d2 = getRotationVector(yaw, pitch);
        Vec3 vec3d3 = eyePos.add(vec3d2.x * distance, vec3d2.y * distance, vec3d2.z * distance);
        AABB box = mc.player.getBoundingBox().expandTowards(vec3d2.scale(distance)).inflate(1.0, 1.0, 1.0);
        double distancePow2 = Math.pow(distance, 2);
        EntityHitResult entityHitResult = raycast(mc.player, eyePos, vec3d3, box, (entity) -> !entity.isSpectator() && entity.isPickable(), distancePow2);
        if (entityHitResult != null) {
            if (entityHitResult.getEntity() instanceof FireworkRocketEntity)
                return false;
            double hitDistSq = eyePos.distanceToSqr(entityHitResult.getLocation());
            return hitDistSq <= distancePow2;
        }
        return false;
    }
    public static double getStrictDistance(LivingEntity entity) {
        return Minecraft.getInstance().player.distanceTo(entity);
    }

    public static double calculateFOVFromCamera(Entity target) {
        Vec3 playerLook = Minecraft.getInstance().player.getViewVector(1.0F);
        Vec3 toTarget = target.getPosition(1.0F).subtract(Minecraft.getInstance().player.getEyePosition(1.0F)).normalize();
        double dot = playerLook.dot(toTarget);
        return Math.toDegrees(Math.acos(dot));
    }
    @Nullable
    public static EntityHitResult raycast(Entity entity, Vec3 min, Vec3 max, AABB box, Predicate<Entity> predicate, double maxDistance) {
        Level world = entity.level();
        double d = maxDistance;
        Entity entity2 = null;
        Vec3 vec3d = null;
        Iterator var12 = world.getEntities(entity, box, predicate).iterator();

        while(true) {
            while(var12.hasNext()) {
                Entity entity3 = (Entity)var12.next();
                AABB box2 = entity3.getBoundingBox().inflate(0);

                Optional<Vec3> optional = box2.clip(min, max);
                if (box2.contains(min)) {
                    if (d >= 0.0) {
                        entity2 = entity3;
                        vec3d = (Vec3)optional.orElse(min);
                        d = 0.0;
                    }
                } else if (optional.isPresent()) {
                    Vec3 vec3d2 = (Vec3)optional.get();
                    double e = min.distanceToSqr(vec3d2);
                    if (e < d || d == 0.0) {
                        if (entity3.getRootVehicle() == entity.getRootVehicle()) {
                            if (d == 0.0) {
                                entity2 = entity3;
                                vec3d = vec3d2;
                            }
                        } else {
                            entity2 = entity3;
                            vec3d = vec3d2;
                            d = e;
                        }
                    }
                }
            }

            if (entity2 == null) {
                return null;
            }

            return new EntityHitResult(entity2, vec3d);
        }
    }
    public static HitResult rayTrace(double dst, float yaw, float pitch) {
        Vec3 vec3d = mc.player.getEyePosition(Minecraft.getInstance().getFrameTimeNs());
        Vec3 vec3d2 = getRotationVector(yaw, pitch);
        Vec3 vec3d3 = vec3d.add(vec3d2.x * dst, vec3d2.y * dst, vec3d2.z * dst);
        return mc.level.clip(new ClipContext(vec3d, vec3d3, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    }
    private static @NotNull Vec3 getRotationVector(float yaw, float pitch) {
        float yawRad = yaw * 0.017453292F;
        float pitchRad = pitch * 0.017453292F;
        float cosYaw = Mth.cos(-yawRad - (float) Math.PI);
        float sinYaw = Mth.sin(-yawRad - (float) Math.PI);
        float cosPitch = -Mth.cos(-pitchRad);
        float sinPitch = Mth.sin(-pitchRad);
        return new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

}
