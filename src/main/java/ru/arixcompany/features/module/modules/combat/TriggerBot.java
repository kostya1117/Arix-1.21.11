package ru.arixcompany.features.module.modules.combat;

import lombok.Getter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ShieldItem;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.combat.aura.AttackHandler;
import ru.arixcompany.features.module.modules.combat.aura.utils.RayTraceUtil;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.features.repos.FriendRepo;


public class TriggerBot extends Module {
    public static final ValueSetting attackRange =
            new ValueSetting("Радиус атаки")
                    .range(3.0f, 6.0f)
                    .setValue(3.0f)
                    .setStep(0.1f);

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
                            "Не атакавать в контейнере"
                    );

    public static final ListSetting extraSettings =
            new ListSetting("Доп.настройка")
                    .value("Легитный спринт", "Умные криты");

    public static final SelectSetting motion =
            new SelectSetting("Режим движения")
                    .value("Без", "Свободная", "Сфокусированная");

    @Getter
    public static LivingEntity target;

    public TriggerBot() {
        super("TriggerBot", Category.Combat);
        setup(
                attackRange,
                targets,
                attackDelay,
                misc,
                extraSettings,
                motion
        );
    }

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.level == null) return;

        if (!mc.player.isAlive()) {
            toggle();
            return;
        }

        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        boolean ignoreWalls = misc.isSelected("Бить через блоки");

        Entity found = RayTraceUtil.getRtxTarget(yaw, pitch, attackRange.getValue(), ignoreWalls);

        if (found instanceof LivingEntity living && isValidTarget(living)) {
            target = living;
        } else {
            target = null;
            return;
        }

        if (!this.checkToAttack()) {
            AttackHandler.performAttack(
                    target,
                    false,
                    attackRange.getValue()
            );
        } else {
            this.reset();
        }
    }

   @Override
   public void toggle() {
      super.toggle();
      this.reset();
   }

   private void reset() {
      target = null;
   }

   private boolean checkToAttack() {
      return mc.player.isUsingItem() && misc.isSelected("Не бить если кушаеш") && !(mc.player.getActiveItem().getItem() instanceof ShieldItem)
         || mc.screen != null && misc.isSelected("Не атакавать в контейнере")
         || !mc.player.getMainHandItem().is(ItemTags.SWORDS)
            && !mc.player.getMainHandItem().is(ItemTags.AXES)
            && misc.isSelected("Бить только оружием");
   }

    private boolean isValidTarget(LivingEntity entity) {
        if (entity instanceof LocalPlayer) return false;
        if (!entity.isAlive()) return false;
        if (entity.isInvulnerable()) return false;
        if (entity instanceof ArmorStand) return false;

        if (mc.player.distanceTo(entity) > attackRange.getValue()) return false;

        if (!misc.isSelected("Бить через блоки") && !mc.player.hasLineOfSight(entity))
            return false;

        if (entity instanceof Player player) {
            if (!targets.isSelected("Игроки")) return false;
            if (player.isCreative()) return false;
            if (FriendRepo.isFriend(player.getName().getString())) return false;

            boolean hasArmor = false;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                        && !player.getItemBySlot(slot).isEmpty()) {
                    hasArmor = true;
                    break;
                }
            }

            if (!targets.isSelected("Голые") && !hasArmor) return false;
            if (!targets.isSelected("Инвизки") && player.isInvisible()) return false;

            return true;
        }

        if (entity instanceof Monster || entity instanceof Slime
                || entity instanceof Villager || entity instanceof Animal) {
            return targets.isSelected("Мобы");
        }

        return false;
    }
}
