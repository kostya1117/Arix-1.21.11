package ru.arixcompany.features.module.modules.combat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import ru.arixcompany.Arix;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventGameTicked;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.event.player.EventSprint;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.movement.AutoSprint;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.utils.player.inventory.PlayerInventoryComponent;
import ru.arixcompany.utils.player.inventory.PlayerInventoryUtil;

import java.util.Comparator;

public class AutoSwap extends Module {

    public AutoSwap() {
        super("AutoSwap", Category.Combat);
        setup(itemType,swapType,keyToSwap);
    }

    private final SelectSetting itemType = new SelectSetting("Предмет")
            .value("Щит", "Геплы", "Тотем", "Шар");
    private final SelectSetting swapType = new SelectSetting("Свапать на")
            .value("Щит", "Геплы", "Тотем", "Шар");
    private final BindSetting keyToSwap = new BindSetting("Кнопка");

    // EventUpdate — каждый тик
    @EventHandler
    public void onUpdate(EventUpdate e) {
        PlayerInventoryComponent.onUpdate();
    }

    // EventSprint — блокируем спринт пока active
    @EventHandler
    public void onSprint(EventSprint e) {
        if (mc.player == null || mc.level == null) return;

        if (PlayerInventoryComponent.isSprintBlocked() && (e.getSource() == EventSprint.Source.MOVEMENT_TICK
                || e.getSource() == EventSprint.Source.INPUT)) {
            e.setSprinting(false);
            mc.player.setSprinting(false);
            AutoSprint autoSprint = Arix.getInstance().getModuleRepo().getModule(AutoSprint.class);
            if (autoSprint != null && autoSprint.isState()) {
                autoSprint.sprint = false;
            }
        }
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (mc.screen != null) return;
        if (event.getAction() != 1) return;
        if (event.getKey() != keyToSwap.getKey()) return;

        Slot first = PlayerInventoryUtil.getSlot(
                getItemByType(itemType.getSelected()),
                Comparator.comparing(s -> s.getItem().isEnchanted()),
                s -> s.index != 46 && s.index != 45
        );

        Slot second = PlayerInventoryUtil.getSlot(
                getItemByType(swapType.getSelected()),
                Comparator.comparing(s -> s.getItem().isEnchanted()),
                s -> s.index != 46 && s.index != 45
        );

        Slot validSlot = first != null && mc.player.getOffhandItem().getItem() != first.getItem().getItem()
                ? first
                : second;

        if (validSlot == null) {
            return;
        }

        //String itemName = validSlot.getItem().getHoverName().getString();

        PlayerInventoryComponent.addTask(() -> {
            try {
                PlayerInventoryUtil.swapHand(validSlot, InteractionHand.OFF_HAND, false);
                PlayerInventoryUtil.closeScreen(true);
                //sendOverlayMessage(Text.of("Свапнул на " + Formatting.GREEN + itemName));
            } catch (Exception e) {
                //sendOverlayMessage(Text.of(Formatting.RED + "Не удалось свапнуть"));
            }
        });
    }

    private Item getItemByType(String type) {
        return switch (type) {
            case "Щит" -> Items.SHIELD;
            case "Тотем" -> Items.TOTEM_OF_UNDYING ;
            case "Геплы" -> Items.GOLDEN_APPLE;
            case "Шар" -> Items.PLAYER_HEAD;
            default -> Items.AIR;
        };
    }
}