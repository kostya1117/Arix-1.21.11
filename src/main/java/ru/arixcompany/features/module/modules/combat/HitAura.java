package ru.arixcompany.features.module.modules.combat;

import lombok.Getter;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.Vec3;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventInput;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.TargetHandler;
import ru.arixcompany.features.module.modules.combat.aura.rotation.impl.FreeLookUtil;
import ru.arixcompany.features.module.modules.combat.aura.rotation.rotations.FunTimeRotation;
import ru.arixcompany.features.module.modules.combat.aura.rotation.rotations.FuntimeRot;
import ru.arixcompany.features.module.modules.combat.aura.rotation.rotations.SnapRotation;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.player.MoveUtils;

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
                    .value("Funtime","FuntimeRot", "Snap");

    public static final SelectSetting snapSetting =
            new SelectSetting("Режим снапа")
                    .value("Быстрый", "Плавный", "Рандомный")
                    .visible(() -> rotationType.isSelected("Snap"));

    public static final ListSetting targets =
            new ListSetting("Цели")
                    .value("Игроки", "Инвизки", "Голые", "Мобы")
                    .selected("Игроки");

    public static final SelectSetting attackDelay =
            new SelectSetting("Тайминг удара")
                    .value("Быстрый", "Динамичный");

    public static final ListSetting misc =
            new ListSetting("Проверки до удара")
                    .value(
                            "Бить через блоки",
                            "Бить только оружием",
                            "Не бить если кушаеш",
                            "Не атакавать в контейнере",
                            "Райкаст",
                            "Продвинутый райкаст"
                    );

    public static final ListSetting extraSettings =
            new ListSetting("Доп.настройка")
                    .value("Легитный спринт", "Умные криты");

    public static final SelectSetting motion =
            new SelectSetting("Режим движения")
                    .value("Без", "Свободная", "Сфокусированная");

    @Getter
    public static LivingEntity target;
    private final TargetHandler targetHandler = new TargetHandler();

    public static long lastLookUpTime = 0L;
    public static long nextLookUpDelay =
            ThreadLocalRandom.current().nextLong(90000L, 180000L);
    public static boolean isLookingUp = false;
    public static long lookUpStartTime = 0L;
    public static int lookUpDuration = 0;
    public int count;

    public HitAura() {
        super("HitAura", Category.Combat);
        setup(
                attackRange,
                preRange,
                rotationType,
                snapSetting,
                targets,
                attackDelay,
                misc,
                extraSettings,
                motion
        );
    }

   @EventHandler
   public void onEvent(EventGameTick e) {
      if (target != null && mc.player != null && mc.level != null) {
         this.updateRotation();
      }
   }

   @EventHandler
   public void onEvent(EventUpdate e) {
      if (!mc.player.isAlive()) {
         this.toggle();
      } else {
          targetHandler.updateTarget();
          target = targetHandler.getTarget();

         if (target != null && mc.player != null && mc.level != null) {

//            if (AttackHandler.resetSprintTick(target, getRanges())) {
//               mc.options.keySprint.setDown(false);
//            }


             if (!this.checkToAttack()) {
                 AttackHandler.performAttack(
                         target,
                         misc.isSelected("Райкаст"),
                         extraSettings.isSelected("Легитный спринт"),
                         getRanges()
                 );
             }
         } else {
            this.reset();
         }
      }
   }

   public static float[] getRanges() {
      return new float[]{attackRange.getValue(), preRange.getValue()};
   }


    private final FunTimeRotation funTimeRotation = new FunTimeRotation();
    private final FuntimeRot funTimeRot = new FuntimeRot();
    private final SnapRotation snapRotation = new SnapRotation();
    private void updateRotation() {
        if (target == null) return;

        boolean shouldAttack =
                AttackHandler.shouldAttack(
                        target,
                        false,
                        true,
                        true,
                        -50L,
                        getRanges()
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
   @Override
   public void toggle() {
      super.toggle();
      this.reset();
   }

   private void reset() {
      target = null;
      if (mc.player != null) {
         isLookingUp = false;
         lookUpStartTime = 0L;
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
   public void deactivate() {
      super.deactivate();
   }
}
