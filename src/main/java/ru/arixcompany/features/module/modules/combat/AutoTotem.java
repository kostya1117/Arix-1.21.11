package ru.arixcompany.features.module.modules.combat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventGameTicked;
import ru.arixcompany.features.event.player.EventSprint;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.movement.AutoSprint;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.player.inventory.PlayerInventoryComponent;
import ru.arixcompany.utils.player.inventory.PlayerInventoryUtil;

public class AutoTotem extends Module {

    public AutoTotem() {
        super("AutoTotem", Category.Combat);
        setup(health,proverki,elytraHealth);
    }

    private final ValueSetting health = new ValueSetting("Здоровье")
            .range(0,36)
            .setValue(5)
            .setStep(1);

    private final ListSetting proverki = new ListSetting("Доп проверки")
            .value("Элитры","Падение");

    private final ValueSetting elytraHealth = new ValueSetting("Здровье на элитрах")
            .range(0,36)
            .setValue(10)
            .setStep(1)
            .visible(() -> proverki.isSelected("Элитры"));

    private int cooldownTicks = 0;
    private Item previousItem = null;

    @EventHandler
    public void onPlayerTick(EventGameTicked event) {
        PlayerInventoryComponent.onUpdate();
    }
    @EventHandler
    public void onSprint(EventSprint e) {
        if (mc.player == null || mc.level == null) return;

        if (PlayerInventoryComponent.isSprintBlocked()
                && (e.getSource() == EventSprint.Source.MOVEMENT_TICK
                || e.getSource() == EventSprint.Source.INPUT)) {
            e.setSprinting(false);
            mc.player.setSprinting(false);
            AutoSprint autoSprint = Arix.getInstance().getModuleRepo().getModule(AutoSprint.class);
            if (autoSprint != null && autoSprint.isState()) {
                autoSprint.sprint = false;
            }
        } else {
            AutoSprint autoSprint = Arix.getInstance().getModuleRepo().getModule(AutoSprint.class);
            if (autoSprint != null && autoSprint.isState() && !autoSprint.sprint) {
                autoSprint.sprint = true;
            }
        }
    }

    @EventHandler
    public void onPlayerTick(EventUpdate event) {
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;
        if (mc.player.isUsingItem()) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        final Item current = mc.player.getOffhandItem().isEmpty()
                ? null : mc.player.getOffhandItem().getItem();

        if (shouldUseTotem()) {
            if (current != Items.TOTEM_OF_UNDYING) {

                Slot slot = PlayerInventoryUtil.getSlot(Items.TOTEM_OF_UNDYING);
                if (slot != null) {
                    swapToOffhand(slot);
                    previousItem = current;
                }
            }
        } else if (current == Items.TOTEM_OF_UNDYING && previousItem != null) {
            Slot slot = PlayerInventoryUtil.getSlot(previousItem);
            if (slot != null) {
                swapToOffhand(slot);
            }
            previousItem = null;
        }
    }

    private void swapToOffhand(Slot slot) {
        PlayerInventoryComponent.addTask( ()-> {
            PlayerInventoryUtil.swapHand(slot, InteractionHand.OFF_HAND,false);
            PlayerInventoryUtil.closeScreen(true);
        });
        cooldownTicks = 0;
    }

    private boolean shouldUseTotem() {
        float healthValue = getHealth();

        if (healthValue <= health.getValue()) return true;

        if (proverki.isSelected("Падение") && (healthValue - (((mc.player.fallDistance - 3) / 2F) + 3.5F) < 0.5)) return true;

        return proverki.isSelected("Элитры")
                && mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA
                && healthValue <= elytraHealth.getValue();
    }

    private float getHealth() {
        return mc.player.getHealth() + mc.player.getAbsorptionAmount();
    }
}