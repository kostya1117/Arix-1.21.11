package ru.arixcompany.features.module.modules.combat;

import lombok.Getter;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventMovementTick;
import ru.arixcompany.features.event.player.EventSprint;
import ru.arixcompany.features.event.render.EventRender2D;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventPreTick;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.TargetHandler;
import ru.arixcompany.features.module.modules.combat.aura.rotation.rotations.FunTimeRotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.rotations.FuntimeRot;
import ru.arixcompany.features.module.modules.combat.aura.rotation.rotations.SnapRotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.rotations.SpookyTimeRot;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.movement.AutoSprint;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.MessageSender;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.render.RenderUtils;

import java.util.concurrent.ThreadLocalRandom;

public class HitAura extends Module {
    public static final ValueSetting attackRange =
            new ValueSetting("Радиус атаки")
                    .range(3.0f, 6.0f)
                    .setValue(3.0f)
                    .setStep(0.1f);

    public static final ValueSetting preRange =
            new ValueSetting("Радиус обнаружения")
                    .range(0.0f, 5.0f)
                    .setValue(1.0f)
                    .setStep(0.1f);

    public static final SelectSetting rotationType =
            new SelectSetting("Режим ротации")
                    .value("Funtime","FuntimeRot", "Snap","SpookyTime");

    public static final SelectSetting snapSetting =
            new SelectSetting("Режим снапа")
                    .value("Быстрый", "Плавный", "Рандомный")
                    .visible(() -> rotationType.isSelected("Snap"));

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
                    .value("Умные криты","Сброс спринта","Фикс удара при HurtTime");

    public final SelectSetting sprintReset =
            new SelectSetting("Режим сброса спринта")
                    .value("Пакет", "Легит")
                    .visible(() -> extraSettings.isSelected("Сброс спринта"));

    public static final SelectSetting motion =
            new SelectSetting("Режим движения")
                    .value("Без", "Свободная", "Сфокусированная");

    public static final BooleanSetting fovMode = new BooleanSetting("FOV режим")
            .setValue(false);

    public static final ValueSetting fovAngle = new ValueSetting("FOV угол")
            .range(30.0f, 180.0f)
            .setValue(90.0f)
            .setStep(1.0f)
            .visible(fovMode::isValue);


    @Getter
    public static LivingEntity target;
    private final TargetHandler targetHandler = new TargetHandler();

    public int count;

    public HitAura() {
        super("HitAura", Category.Combat);
        setup(
                attackRange,
                preRange,
                rotationType,
                snapSetting,
                targets,
                misc,
                extraSettings,
                motion,
                sprintReset,
                fovMode,
                fovAngle
        );
    }

   @EventHandler
    public void onEvent(EventGameTick e) {
        if (target != null && mc.player != null && mc.level != null) {
            this.updateRotation();
        }
    }

    @EventHandler
    public void onPreAttack(EventPreTick e) {
        if (target == null || mc.player == null || mc.level == null)
            return;

        boolean needSprintReset = shouldResetSprintForCrit();
        
        if (needSprintReset && hasStopSprint() && AuraUtil.validDistance(target, attackRange.getValue())) {
            if (sprintReset.isSelected("Легит") && extraSettings.isSelected("Сброс спринта")) {
                mc.player.setSprinting(false);
            }
        }
    }
    @EventHandler
    public void onPreAttack(EventSprint e) {
        if (target == null || mc.player == null || mc.level == null)
            return;

        boolean needSprintReset = shouldResetSprintForCrit();
        if (needSprintReset && hasStopSprint() && AuraUtil.validDistance(target, attackRange.getValue())) {
            if (sprintReset.isSelected("Легит") && extraSettings.isSelected("Сброс спринта")) {
                e.setSprinting(false);
            }
        }
    }

    private boolean shouldResetSprintForCrit() {
        return mc.player.fallDistance > 0.0F || AttackHandler.isBestMomentToHit();
    }

    public boolean hasStopSprint() {
        return !AttackHandler.hasMovementRestrictions();
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
            if (fovMode.isValue() && !isTargetInFov(target)) return;

            if (!this.checkToAttack()) {
                AttackHandler.performAttack(
                        target,
                        misc.isSelected("Райкаст"),
                        attackRange.getValue()
                );
            }
        } else {
            this.reset();
        }
    }

    private final FunTimeRotation funTimeRotation = new FunTimeRotation();
    private final FuntimeRot funTimeRot = new FuntimeRot();
    private final SnapRotation snapRotation = new SnapRotation();
    private final SpookyTimeRot spookyTimeRot = new SpookyTimeRot();

    private void updateRotation() {
        if (target == null) return;
        if (fovMode.isValue()) return;

        boolean shouldAttack =
                AttackHandler.shouldAttack(
                        target,
                        false,
                        true,
                        attackRange.getValue()
                );

        switch (rotationType.getSelected()) {
            case "Funtime":
                funTimeRotation.rotate(
                        target,
                        shouldAttack,
                        attackRange.getValue(),
                        this.checkToAttack()
                );
                break;
            case "SpookyTime":
                spookyTimeRot.rotate(
                        target,
                        shouldAttack,
                        attackRange.getValue(),
                        this.checkToAttack()
                );
                break;
            case "FuntimeRot":
                funTimeRot.rotate(
                        target,
                        shouldAttack,
                        attackRange.getValue(),
                        this.checkToAttack()
                );
                break;

            case "Snap":
                snapRotation.rotate(
                        target,
                        shouldAttack
                );
                break;
        }
    }

    public static boolean isTargetInFov(LivingEntity entity) {
        if (mc.player == null || entity == null) return false;

        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 closest = AuraUtil.getClosestVec(eyePos, entity);
        Vec3 dir = closest.subtract(eyePos).normalize();

        float yaw   = (float) Math.toRadians(mc.player.getYRot());
        float pitch = (float) Math.toRadians(mc.player.getXRot());
        Vec3 look = new Vec3(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                 Math.cos(yaw) * Math.cos(pitch)
        );

        double dot = look.dot(dir);
        double angle = Math.toDegrees(Math.acos(Mth.clamp(dot, -1.0, 1.0)));
        return angle <= fovAngle.getValue() / 2.0;
    }

    @EventHandler
    public void onRender2D(EventRender2D e) {
        if (!fovMode.isValue() || mc.player == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        float cx = sw / 2.0f;
        float cy = sh / 2.0f;

        float radius = (sh / 2.0f) * ((float) fovAngle.getValue() / 180.0f);

        boolean inFov = target != null && isTargetInFov(target);
        int color = inFov ? 0xFFFFFFFF : 0x80FFFFFF;

        RenderUtils.drawCircle(cx, cy, 0, 360, radius, 1.0f, color);
    }

   @Override
   public void toggle() {
      super.toggle();
      this.reset();
   }

   private void reset() {
      target = null;
      if (mc.player != null) {
         count = 0;
      }
   }

   private boolean checkToAttack() {
      return mc.player.isUsingItem() && misc.isSelected("Не бить если кушаеш") && !(mc.player.getActiveItem().getItem() instanceof ShieldItem)
         || mc.screen != null && misc.isSelected("Не атакавать в контейнере")
         || !mc.player.getMainHandItem().is(ItemTags.SWORDS)
            && !mc.player.getMainHandItem().is(ItemTags.AXES)
            && misc.isSelected("Бить только оружием");
   }
    @Override
    public void activate() {
        super.activate();
    }

   @Override
   public void deactivate() {
      super.deactivate();
   }
}
