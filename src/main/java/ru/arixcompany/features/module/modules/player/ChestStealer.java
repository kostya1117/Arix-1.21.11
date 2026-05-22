package ru.arixcompany.features.module.modules.player;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.inventory.PlayerInventoryUtil;

public class ChestStealer extends Module {
    Timer stopWatch = new Timer();

    SelectSetting modeSetting = new SelectSetting("Тип")
            .value("ФанТайм", "Обычный");
    ValueSetting delaySetting = new ValueSetting("Задержка")
            .setValue(100).range(0, 1000).visible(() -> modeSetting.isSelected("Обычный"));

    public ChestStealer() {
        super("ChestStealer", Category.Player);
        setup(modeSetting, delaySetting);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null || mc.level == null) return;

        String mode = modeSetting.getSelected();
        switch (mode) {
            case "ФанТайм" -> {
                if (mc.screen instanceof ContainerScreen sh && sh.getTitle().getString().toLowerCase().contains("мистический") && !mc.player.getCooldowns().isOnCooldown(Items.GUNPOWDER.getDefaultInstance())) {
                    sh.getMenu().slots.stream().filter(s -> s.hasItem() && !s.container.equals(mc.player.getInventory()) && stopWatch.every(150))
                            .forEach(s -> PlayerInventoryUtil.clickSlot(s, 0, ClickType.QUICK_MOVE, true));
                }
            }
            case "Обычный" -> {
                if (mc.player.containerMenu instanceof ChestMenu sh) sh.slots.forEach(s -> {
                    boolean isDefaultMode = mode.equals("Обычный");
                    if (s.hasItem() && !s.container.equals(mc.player.getInventory()) && isDefaultMode && stopWatch.every((long) delaySetting.getValue())) {
                        PlayerInventoryUtil.clickSlot(s, 0, ClickType.QUICK_MOVE, true);
                    }
                });
            }
        }
    }
}
