package ru.arixcompany.features.module.modules.combat;

import lombok.Getter;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventGameTicked;
import ru.arixcompany.features.event.player.EventSprint;
import ru.arixcompany.features.event.world.EventGameTick;
import ru.arixcompany.features.event.world.EventRotationUpdate;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.KillAuraRotationsValueGroup;
import ru.arixcompany.features.module.modules.combat.aura.TargetHandler;
import ru.arixcompany.features.module.modules.combat.aura.utils.AuraUtil;
import ru.arixcompany.features.module.modules.movement.AutoSprint;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.math.TimerUtils;
import ru.arixcompany.utils.player.TickLoopTaskExecutor;

public class HitAura extends Module {


    public static final SelectSetting angleSmooth =
            new SelectSetting("Ротация")
                    .value("Линейное", "Интерполяция", "SpookyTime", "FuntimeSnap");

    public static final SelectSetting interrot =
            new SelectSetting("Сглаживание").value(
                    "Линейное",
                    "Синусоида (Вход)", "Синусоида (Выход)", "Синусоида (Вход/Выход)",
                    "Квадратичное (Вход)", "Квадратичное (Выход)", "Квадратичное (Вход/Выход)",
                    "Кубическое (Вход)", "Кубическое (Выход)", "Кубическое (Вход/Выход)",
                    "Квартичное (Вход)", "Квартичное (Выход)", "Квартичное (Вход/Выход)",
                    "Квинтичное (Вход)", "Квинтичное (Выход)", "Квинтичное (Вход/Выход)",
                    "Экспонента (Вход)", "Экспонента (Выход)", "Экспонента (Вход/Выход)",
                    "Круговое (Вход)", "Круговое (Выход)", "Круговое (Вход/Выход)",
                    "Замах (Вход)", "Замах (Выход)", "Замах (Вход/Выход)",
                    "Пружина (Вход)", "Пружина (Выход)", "Пружина (Вход/Выход)",
                    "Отскок (Вход)", "Отскок (Выход)", "Отскок (Вход/Выход)"
            );

    public static final SelectSetting motion =
            new SelectSetting("Режим ротация")
                    .value("Не менять", "Направлять", "Изменять взгяд игрока");

    public final SelectSetting sprintReset =
            new SelectSetting("Режим сброса спринта")
                    .value("Пакет", "Легит")
                    .visible(() -> extraSettings.isSelected("Сброс спринта"));

    public static final ValueSetting attackRange =
            new ValueSetting("Радиус атаки")
                    .range(3.0f, 6.0f).setValue(3.0f).setStep(0.1f);

    public static final ValueSetting preRange =
            new ValueSetting("Радиус обнаружения")
                    .range(0.0f, 5.0f).setValue(1.0f).setStep(0.1f);

    public static final BooleanSetting elytraTarget =
            new BooleanSetting("Элитра таргет").setValue(false);

    public static final ValueSetting elytraRange =
            new ValueSetting("Радиус обнаружения на элитре")
                    .range(5.0f, 30.0f).setValue(15.0f).setStep(1)
                    .visible(elytraTarget::isValue);

    public static final BooleanSetting throughWalls =
            new BooleanSetting("Сквозь стены").setValue(false);

    public static final BooleanSetting randomfalldist =
            new BooleanSetting("Рандомные удары").setValue(false);

    public static final ValueSetting horizontalTurnSpeedMin =
            new ValueSetting("Горизонталь мин.")
                    .range(1.0f, 180.0f).setValue(20.0f).setStep(1.0f)
                    .visible(() -> !angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting horizontalTurnSpeedMax =
            new ValueSetting("Горизонталь макс.")
                    .range(1.0f, 180.0f).setValue(45.0f).setStep(1.0f)
                    .visible(() -> !angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting verticalTurnSpeedMin =
            new ValueSetting("Вертикаль мин.")
                    .range(1.0f, 180.0f).setValue(10.0f).setStep(1.0f)
                    .visible(() -> !angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting verticalTurnSpeedMax =
            new ValueSetting("Вертикаль макс.")
                    .range(1.0f, 180.0f).setValue(25.0f).setStep(1.0f)
                    .visible(() -> !angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting yawAccelerationMin =
            new ValueSetting("Ускорение Yaw мин.")
                    .range(1.0f, 180.0f).setValue(20.0f).setStep(1.0f)
                    .visible(() -> angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting yawAccelerationMax =
            new ValueSetting("Ускорение Yaw макс.")
                    .range(1.0f, 180.0f).setValue(25.0f).setStep(1.0f)
                    .visible(() -> angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting pitchAccelerationMin =
            new ValueSetting("Ускорение Pitch мин.")
                    .range(1.0f, 180.0f).setValue(20.0f).setStep(1.0f)
                    .visible(() -> angleSmooth.isSelected("Ускорение"));

    public static final ValueSetting pitchAccelerationMax =
            new ValueSetting("Ускорение Pitch макс.")
                    .range(1.0f, 180.0f).setValue(25.0f).setStep(1.0f)
                    .visible(() -> angleSmooth.isSelected("Ускорение"));

    public static final ListSetting targets =
            new ListSetting("Цели")
                    .value("Игроки", "Инвизки", "Голые", "Мобы")
                    .selected("Игроки");

    public static final ListSetting misc =
            new ListSetting("Проверки до удара")
                    .value(
                            "Бить через блоки",
                            "Бить только оружием",
                            "Отжимать щит",
                            "Ломать щит",
                            "Не бить когда ешь",
                            "Не бить в контейнере",
                            "Райкаст"
                    );

    public static final ListSetting extraSettings =
            new ListSetting("Доп.настройка")
                    .value("Умные криты", "Сброс спринта", "Фикс удара при HurtTime",
                            "Игнорировать инвентарь");

    @Getter
    public static LivingEntity target;
    private final TargetHandler targetHandler = new TargetHandler();
    private final KillAuraRotationsValueGroup rotations = new KillAuraRotationsValueGroup();
    public final TimerUtils sprintTimer = new TimerUtils();
    public int count;
    @Getter
    public static InteractionHand enforcedBlockingHand = null;

    public boolean prevKeyUse;
//    public void onShield(EventShield e) {
//        if (mc.player == null || target == null) return;
//        if ((e.getSource() == EventShield.Source.POST || e.getSource() == EventShield.Source.PRE) && AttackHandler.shouldAttack()) {
//            prevKeyUse = mc.options.keyUse.isDown();
//            e.setShieldUse(false);
//            mc.options.keyUse.setDown(false);
//            MessageSender.print(e.isShieldUse() + "");
//        } /*else {
//            if (prevKeyUse) {
//                mc.options.keyUse.setDown(true);
//                prevKeyUse = false;
//            }
//        }*/
//    }
//@EventHandler
//public void onShield(EventShield e) {
//    if (mc.player == null) return;
//
//    // Просто ставим флаг.
//    // Логика в handleKeybinds сама вызовет releaseUsingItem на основе этого флага.
//    if (AttackHandler.shouldAttack()) {
//        e.setShieldUse(false);
//    }
//}

//    @EventHandler
//    public void onShield(EventShield e) {
//        if (mc.player == null || target == null) return;
//
//        boolean isAuraHittable = AttackHandler.shouldAttack() && AuraUtil.validDistance(target, attackRange.getValue());
//
//        if (e.getSource() == EventShield.Source.PRE) {
//            if (isAuraHittable) {
//                prevKeyUse = mc.options.keyUse.isDown();
//                e.setShieldUse(false);
//                mc.options.keyUse.setDown(false);
//            }
//        }
//        else if (e.getSource() == EventShield.Source.POST) {
//            if (prevKeyUse) {
//                mc.options.keyUse.setDown(true);
//                prevKeyUse = false;
//            }
//        }
//    }

    public HitAura() {
        super("HitAura", Category.Combat);
        setup(
                // Range
                angleSmooth,interrot,sprintReset,motion,
                attackRange, preRange,
                // Elytra
                elytraTarget, elytraRange,
                // Rotation
                throughWalls,randomfalldist,
                horizontalTurnSpeedMin, horizontalTurnSpeedMax,
                verticalTurnSpeedMin, verticalTurnSpeedMax,
                yawAccelerationMin, yawAccelerationMax,
                pitchAccelerationMin, pitchAccelerationMax,
                // Targets
                targets, misc,

                extraSettings
        );
    }

    @EventHandler
    public void onEvent(EventRotationUpdate e) {
        if (target != null && mc.player != null && mc.level != null) {
            rotations.processTarget(target);
        }
    }

    public boolean stopBlocking() {
        if (mc.player == null || mc.gameMode == null) return false;

        if (!isBlockingServerside(mc.player)) {
            return false;
        }
        TickLoopTaskExecutor.executeInTickLoop(() -> {
            mc.gameMode.releaseUsingItem(mc.player);
        });
        return true;
    }

    public static boolean isInHand(LivingEntity entity, ItemStack itemStack, InteractionHand hand) {
        return entity.getItemInHand(hand) == itemStack;
    }

    /**
     * Проверка блокировки на стороне сервера (с учетом кросс-версионных особенностей).
     */
    public static boolean isBlockingServerside(LivingEntity entity) {
        if (entity.isBlocking()) {
            return true;
        }

        if (entity.isUsingItem()) {
            ItemStack usingItem = entity.getUseItem();
            return isInHand(entity, usingItem, InteractionHand.OFF_HAND) && usingItem.getItem() instanceof ShieldItem;
        }

        return false;
    }

    @EventHandler
    public void onSprint(EventSprint e) {
        if (target == null || mc.player == null || mc.level == null) return;
        if (shouldBlockSprinting() && (e.getSource() == EventSprint.Source.MOVEMENT_TICK || e.getSource() == EventSprint.Source.INPUT)) {
            e.setSprinting(false);
            mc.player.setSprinting(false);
            if (Arix.getInstance().getModuleRepo().getModule(AutoSprint.class).isState()) {
                Arix.getInstance().getModuleRepo().getModule(AutoSprint.class).sprint = false;
            }
        } else {
            if (Arix.getInstance().getModuleRepo().getModule(AutoSprint.class).isState() && !Arix.getInstance().getModuleRepo().getModule(AutoSprint.class).sprint) {
                Arix.getInstance().getModuleRepo().getModule(AutoSprint.class).sprint = true;
            }
        }
        //MessageSender.print("AutoSprint: " +  Arix.getInstance().getModuleRepo().getModule(AutoSprint.class).sprint  + "," + "EventSprint" + e.isSprinting());
    }

    @EventHandler
    public void onTick(EventGameTicked e) {
        if (mc.player == null || !mc.player.isAlive()) return;

        targetHandler.updateTarget();
        target = targetHandler.getTarget();

        if (target != null) {
            if (skipAttack()) return;

            float rangeToHit = attackRange.getValue();

            //if (AuraUtil.validDistance(target, rangeToHit)) {
            boolean sprint = true;
            if (!mc.player.onGround()) {
                sprint = !mc.player.isSprinting();
            }
            //if (!this.misc.isSelected("Не бить когда ешь") || !this.isUseItems()) {
                if (AuraUtil.validDistance(target, rangeToHit)) {
                    AttackHandler.performAttack(target, misc.isSelected("Райкаст"), rangeToHit);
                }
           // }
        } else {
            reset();
        }
    }

    @Override
    public void toggle() {
        super.toggle();
        reset();
    }

    @Override
    public void activate() {
        super.activate();
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    public boolean allowsCriticalHit() {
        return allowsCriticalHit(false);
    }

    public boolean allowsCriticalHit(boolean ignoreOnGround) {
        boolean[] blockingConditions = new boolean[]{
                mc.player.isInLava(),
                mc.player.isInWater(),
                mc.player.isPassenger(),
                mc.player.hasEffect(MobEffects.LEVITATION),
                mc.player.hasEffect(MobEffects.BLINDNESS),
                mc.player.hasEffect(MobEffects.SLOW_FALLING),
                mc.player.onClimbable(),
                mc.player.isNoGravity(),
                mc.player.isHandsBusy(),
                mc.player.getAbilities().flying,
                mc.player.onGround() && !ignoreOnGround
        };

        for (boolean cond : blockingConditions) {
            if (cond) {
                return false;
            }
        }
        return true;
    }

    public boolean canDoCriticalHit(boolean ignoreOnGround, boolean ignoreSprint) {
        return allowsCriticalHit(ignoreOnGround)
                && mc.player.getAttackStrengthScale(0.5f) > 0.9f;
    }

    public boolean canDoCriticalHit() {
        return canDoCriticalHit(false, false);
    }

    public boolean wouldDoCriticalHit() {
        return allowsCriticalHit(true) && mc.player.getAttackStrengthScale(0.5f) > 0.9f && mc.player.fallDistance > 0.0f;
    }

    public boolean hasStopSprint() {
        return !AttackHandler.hasMovementRestrictions();
    }

    private boolean shouldBlockSprinting() {
        return extraSettings.isSelected("Сброс спринта")
                && (AttackHandler.shouldAttack() || this.sprintTimer.getTimePassed() > 0L)
                && hasStopSprint()
                && AuraUtil.validDistance(target, attackRange.getValue());
    }

    private void reset() {
        target = null;
        if (mc.player != null) {
            count = 0;
            if (Arix.getInstance().getModuleRepo().getModule(AutoSprint.class).isState() && !Arix.getInstance().getModuleRepo().getModule(AutoSprint.class).sprint) {
                Arix.getInstance().getModuleRepo().getModule(AutoSprint.class).sprint = true;
            }
        }

        this.sprintTimer.setTime(0L);
    }

    public boolean isUseItems() {
        return mc.player.isUsingItem();
    }


    private boolean skipAttack() {
        return mc.screen != null && misc.isSelected("Не атакавать в контейнере")
                && !extraSettings.isSelected("Игнорировать инвентарь")
                || !mc.player.getMainHandItem().is(ItemTags.SWORDS)
                && !mc.player.getMainHandItem().is(ItemTags.AXES)
                && misc.isSelected("Бить только оружием");
    }

    public static boolean canAttackNow() {
        if (mc.player == null) return false;

        if (mc.player.isUsingItem() && !(mc.player.getActiveItem().getItem() instanceof ShieldItem)) {
            return false;
        }

        if (mc.screen != null && !extraSettings.isSelected("Игнорировать инвентарь")) {
            return false;
        }

        return true;
    }
}