package ru.arixcompany.features.module.modules.combat;

import lombok.Getter;
import net.minecraft.util.Mth;
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
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.features.module.modules.combat.aura.attack.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.attack.AuraUtil;
import ru.arixcompany.features.module.modules.combat.aura.rotation.Rotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.RotationController;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Aura extends Module {

    @Getter
    private LivingEntity target;

    @Getter
    private Vec2 rotateVector = new Vec2(0, 0);

    private boolean lookingAtHitbox;

    private final AttackHandler attackHandler;

    private final SelectSetting rotationMode;
    private final ValueSetting attackRange;
    private final ValueSetting preRange;
    private final ListSetting targets;
    private final ListSetting options;

    public Aura() {
        super("Aura", Category.Combat);

        rotationMode = new SelectSetting("Ротация головы")
                .value("None", "FunTime");

        attackRange = new ValueSetting("Дистанция атаки")
                .setStep(0.1f)
                .range(2f, 6f)
                .setValue(4.2f);

        preRange = new ValueSetting("Доп. дистанция")
                .setStep(0.1f)
                .range(0f, 3f)
                .setValue(1.0f);

        targets = new ListSetting("Таргеты")
                .value("Игроки", "Мобы", "Животные", "Невидимки", "Голые");

        options = new ListSetting("Опции")
                .value("РэйКаст", "Не бить через стены", "Коррекция движения", "Только криты", "Ломать щит");

        options.enable("Коррекция движения");

        attackHandler = new AttackHandler(options);

        setup(rotationMode, attackRange, preRange, targets, options);
    }

    @EventHandler
    public void onUpdate(EventGameTick event) {
        if (mc.player == null || mc.level == null) return;

        updateTarget();

        if (target == null) {
            reset();
            return;
        }

        double range = attackRange.getValue() + preRange.getValue();

        if (!isValidTarget(target)) {
            reset();
            return;
        }

        if (!rotationMode.is("None")) {
            updateRotation(range);
        } else {
            lookingAtHitbox = AuraUtil.checkRtx(
                    mc.player.getYRot(),
                    mc.player.getXRot(),
                    (float) range,
                    0.5f
            );
        }

        boolean rotationValid = lookingAtHitbox || isInRange(target, range);
        attackHandler.handle(target, rotationValid);
    }

    private void updateTarget() {
        List<LivingEntity> entities = new ArrayList<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && isValidTarget(living)) {
                entities.add(living);
            }
        }

        entities.sort(Comparator.comparingDouble(e -> mc.player.distanceTo(e)));
        target = entities.isEmpty() ? null : entities.get(0);
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity == null || entity == mc.player || !entity.isAlive() || entity.isInvulnerable())
            return false;

        if (mc.player.distanceTo(entity) > attackRange.getValue() + preRange.getValue())
            return false;

        if (entity instanceof Player && !targets.is("Игроки")) return false;
        if (entity.isInvisible() && !targets.is("Невидимки")) return false;
        if (entity instanceof Player && entity.getArmorValue() == 0 && !targets.is("Голые")) return false;

        return !options.is("Не бить через стены") || canSee(entity);
    }

    private boolean canSee(Entity entity) {
        HitResult result = mc.level.clip(new ClipContext(
                mc.player.getEyePosition(0F),
                entity.getEyePosition(0F),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mc.player
        ));
        return result.getType() == HitResult.Type.MISS;
    }

    private boolean isInRange(Entity entity, double maxRange) {
        Vec3 eye = entity.getEyePosition(0F);
        return squaredDistanceFromEyes(eye) <= maxRange * maxRange;
    }

    private void updateRotation(double range) {
        if (target == null) return;

        Vec3 targetPos = target.getEyePosition(0F);
        Vec3 diff = targetPos.subtract(mc.player.getEyePosition(0F)).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y,
                Math.sqrt(diff.x * diff.x + diff.z * diff.z)));

        float speed = new SecureRandom().nextBoolean() ? 0.4F : 0.6F;

        rotateVector = new Vec2(
                wrapLerp(speed, rotateVector.x, yaw),
                wrapLerp(speed / 2F, rotateVector.y, pitch)
        );

        Rotation rotation = new Rotation(rotateVector.x, rotateVector.y);
        RotationController.update(rotation, 3F, 10F, 3F, 3F, 1, 5, false);

        lookingAtHitbox = AuraUtil.checkRtx(
                mc.player.getYRot(),
                mc.player.getXRot(),
                (float) range,
                0.5f
        );
    }

    private float squaredDistanceFromEyes(@NotNull Vec3 vec) {
        Vec3 eyes = mc.player.getEyePosition();
        return (float) eyes.distanceToSqr(vec);
    }

    private void reset() {
        target = null;
        if (mc.player == null) return;

        rotateVector = new Vec2(mc.player.yRot, mc.player.xRot);

        attackHandler.reset();
        RotationController.getInstance().stopRotation();
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

    private static float wrapLerp(float speed, float from, float to) {
        float delta = Mth.wrapDegrees(to - from);
        return from + delta * speed;
    }
}