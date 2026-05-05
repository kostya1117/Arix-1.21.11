package ru.arixcompany.features.module.modules.combat;

import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.aurautil.AuraUtil;
import ru.arixcompany.utils.aurautil.rotsystem.Rotation;
import ru.arixcompany.utils.aurautil.rotsystem.RotationSystem;
import ru.arixcompany.utils.math.Timer;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class KillAura extends Module {

    @Getter
    private LivingEntity target;
    @Getter
    private Vec2 rotateVector = new Vec2(0, 0);
    private boolean lookingAtHitbox = false;
    private final Timer attackTimer = new Timer();
    private final Timer cpsTimer = new Timer();
    private int count = 0;

    private SelectSetting rotationModeImpl;
    ValueSetting attackRange = new ValueSetting("Дистанция атаки")
            .setStep(0.1f)
            .range(2f, 6f)
            .setValue(4.2f);
    private ValueSetting preRange;
    private ValueSetting fov;
    private ListSetting targets;
    private ListSetting options;
    private BooleanSetting cpsBypass;

    public KillAura() {
        super("KillAura", Category.Combat);
        rotationModeImpl = new SelectSetting("Ротация головы")
                .value("None", "FunTime");

        preRange = new ValueSetting("Доп. дистанция")
                .setStep(0.1f)
                .range(0f, 3f)
                .setValue(1.0f);

        fov = new ValueSetting("FOV")
                .setStep(1f)
                .range(30f, 180f)
                .setValue(90f);

        targets = new ListSetting("Таргеты")
                .value("Игроки", "Мобы", "Животные", "Невидимки", "Голые");

        options = new ListSetting("Опции")
                .value("РэйКаст", "Не бить через стены", "Коррекция движения", "Только криты", "Ломать щит");
        options.enable("Коррекция движения");
        
        cpsBypass = new BooleanSetting("Обход CPS")
                .setValue(true);

        this.setup(rotationModeImpl, attackRange, preRange, fov, targets, options, cpsBypass);
    }

    @EventHandler
    public void onUpdate(EventGameTick event) {
        if (mc.player == null || mc.level == null)
            return;

        updateTarget();
        if (target == null) {
            reset();
            return;
        }

        double ranges = attackRange.getValue() + preRange.getValue();
        if (isValidTarget(target)) {
            if (!rotationModeImpl.is("None")) {
                updateRotation(ranges);
            } else {
                // Even without rotation mode, update lookingAtHitbox
                lookingAtHitbox = AuraUtil.checkRtx(mc.player.getYRot(), mc.player.getXRot(), (float) ranges, 0.5f);
            }
            if (shouldAttack() && attackTimer.hasReached(100) && canPerformCrit() && isRotationValidForAttack(ranges)) {
                performAttack();
                attackTimer.reset();
            }
        } else {
            reset();
        }
    }


    private boolean isRotationValidForAttack(double maxRange) {
        if (lookingAtHitbox) {
            return true;
        }
        if (options.is("РэйКаст")) {
            return isInRange(target, maxRange);
        }
        return AuraUtil.checkRtx(mc.player.getYRot(), mc.player.getXRot(), (float) maxRange, 0.5f);
    }

    private void updateTarget() {
        List<LivingEntity> entities = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && isValidTarget(living)) {
                entities.add(living);
            }
        }

        if (entities.isEmpty()) {
            target = null;
            return;
        }

        entities.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
        target = entities.get(0);
    }


    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive() || entity.isInvulnerable()) {
            return false;
        }

        // if (Relake.getInstance().getFriendManager().isFriend(entity.getName().getString())) return false;

        if (mc.player.distanceTo(entity) > attackRange.getValue() + preRange.getValue()) {
            return false;
        }
        if (entity instanceof Player && !targets.is("Игроки")) {
            return false;
        }
        if (entity instanceof net.minecraft.world.entity.monster.Monster && !targets.is("Мобы")) {
            return false;
        }
        if (entity instanceof net.minecraft.world.entity.animal.Animal && !targets.is("Животные")) {
            return false;
        }
        if (entity.isInvisible() && !targets.is("Невидимки")) {
            return false;
        }
        if (entity instanceof Player && entity.getArmorValue() == 0 && !targets.is("Голые")) {
            return false;
        }
        return !options.is("Не бить через стены") || canSeeThroughWall(entity);
    }


    private boolean canSeeThroughWall(Entity entity) {
        HitResult result = mc.level.clip(new ClipContext(mc.player.getEyePosition(0.0F), entity.getEyePosition(0.0F), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }


    private boolean isInRange(Entity entity, double maxRange) {
        if (options.is("РэйКаст")) {
            float[] rotation;
            float halfBox = (float) (entity.getBoundingBox().getXsize() / 2f);
            for (float x = -halfBox; x <= halfBox; x += 0.15f) {
                for (float z = -halfBox; z <= halfBox; z += 0.15f) {
                    for (float y = 0.05f; y <= entity.getBoundingBox().getYsize(); y += 0.25f) {
                        Vec3 point = new Vec3(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                        if (squaredDistanceFromEyes(point) > maxRange * maxRange) {
                            continue;
                        }
                        rotation = calcAngle(point);
                        if (AuraUtil.checkRtx(rotation[0], rotation[1], (float) maxRange, 0)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return squaredDistanceFromEyes(entity.getPosition(0F).add(0, entity.getEyeHeight(entity.getPose()), 0)) <= maxRange * maxRange;
    }


    private boolean shouldAttack() {
        if (cpsBypass.isValue() && !cpsTimer.hasReached(50)) {
            return false;
        }
        float attackStrength = mc.player.getAttackStrengthScale(1.0f);
        return attackStrength >= 1f;
    }


    private boolean canPerformCrit() {
        if (!options.is("Только криты")) {
            return true;
        }
        return !mc.player.onGround() && mc.player.fallDistance > 0 && !mc.player.isAutoJumpEnabled() &&
                !mc.player.isInWater() && !mc.player.isInLava() && !mc.player.onClimbable() && mc.player.getDeltaMovement().y < 0;
    }


    private void performAttack() {
        if (target instanceof Player && options.is("Ломать щит") && target.isBlocking()) {
            // TODO: Implement shield-breaking logic (e.g., switch to axe)
        }
        mc.player.setSprinting(false);
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.player.setSprinting(true);
        lookingAtHitbox = false;
        cpsTimer.reset();
        count = (count + 1) % 2; // Update count for FunTime rotation
    }


    private void updateRotation(double maxRange) {
        if (target == null || rotationModeImpl.is("None")) {
            return;
        }

        Vec3 targetPos = getTargetPoint(target);
        double maxHeight = (AuraUtil.getStrictDistance(target) / attackRange.getValue());
        Vec3 vec = targetPos
                .add(0, Mth.clamp(mc.player.getEyePosition(0.0F).y - target.getY(), 0, maxHeight), 0)
                .subtract(mc.player.getEyePosition(0.0F))
                .normalize();

        float rawYaw = (float) Math.toDegrees(Math.atan2(-vec.x, vec.z));
        float rawPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))), -90F, 90F);

        float speed = new SecureRandom().nextBoolean() ? randomLerp(0.3F, 0.4F) : randomLerp(0.5F, 0.6F);

        float cos = (float) Math.cos(System.currentTimeMillis() / 100D);
        float sin = (float) Math.sin(System.currentTimeMillis() / 100D);
        float yaw = (float) Math.ceil(randomLerp(6F, 12) * cos + (1F - cooldownFromLastSwing()) * (randomLerp(60, 90) * (count == 0 ? 1 : -1)));
        float pitch = (float) Math.ceil(randomLerp(6F, 12) * sin + (1F - cooldownFromLastSwing()) * (randomLerp(15, 45) * (count == 0 ? 1 : -1)));

        rotateVector = new Vec2(wrapLerp(speed, Mth.wrapDegrees(rotateVector.x), Mth.wrapDegrees(rawYaw + yaw)), wrapLerp(speed / 2F, rotateVector.y, Mth.clamp(rawPitch + pitch, -90F, 90F)));

        Rotation rotation = new Rotation(mc.player.yRot + (float) Math.ceil(Mth.wrapDegrees(rotateVector.x) - Mth.wrapDegrees(mc.player.yRot)), mc.player.xRot + (float) Math.ceil(Mth.wrapDegrees(rotateVector.y) - Mth.wrapDegrees(mc.player.xRot)));

        boolean toFast = cooldownFromLastSwing() > 0.5F;
        RotationSystem.update(rotation, toFast && rayTrace() ? new Random().nextFloat() : 3F, 10F, 3F, 3F, 1, 5, false);

        lookingAtHitbox = AuraUtil.checkRtx(mc.player.getYRot(), mc.player.getXRot(), (float) maxRange, 0.5f);
    }


    private Vec3 getTargetPoint(LivingEntity entity) {
        double lengthY = entity.getBoundingBox().getYsize();
        return entity.getPosition(0F).add(0, lengthY * 0.5, 0);
    }


    private float[] calcAngle(Vec3 to) {
        double diffX = to.x - mc.player.getEyePosition().x;
        double diffY = (to.y - mc.player.getEyePosition().y) * -1F;
        double diffZ = to.z - mc.player.getEyePosition().z;
        double dist = Mth.sqrt((float) (diffX * diffX + diffZ * diffZ));
        return new float[]{
                (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0),
                (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(diffY, dist)))
        };
    }


    private float squaredDistanceFromEyes(@NotNull Vec3 vec) {
        double d0 = vec.x - mc.player.getX();
        double d1 = vec.z - mc.player.getZ();
        double d2 = vec.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        return (float) (d0 * d0 + d1 * d1 + d2 * d2);
    }


    private void reset() {
        target = null;
        if (mc.player == null) return;
        rotateVector = new Vec2(mc.player.yRot, mc.player.xRot);
        attackTimer.reset();
        cpsTimer.reset();
        count = 0;
        RotationSystem.getInstance().stopRotation();
    }

    @Override
    public void activate() {
        super.activate();
        reset();
    }

    @Override
    public void toggle() {
        super.toggle();
        reset();
    }


    private static float randomLerp(float min, float max) {
        return min + (float) Math.random() * (max - min);
    }


    private static float wrapLerp(float speed, float from, float to) {
        float delta = Mth.wrapDegrees(to - from);
        return from + delta * speed;
    }


    private float cooldownFromLastSwing() {
        return mc.player.getAttackStrengthScale(1.0f);
    }


    private boolean rayTrace() {
        if (target == null) return false;
        HitResult result = mc.level.clip(new ClipContext(mc.player.getEyePosition(0.0F), target.getEyePosition(0.0F), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }
}