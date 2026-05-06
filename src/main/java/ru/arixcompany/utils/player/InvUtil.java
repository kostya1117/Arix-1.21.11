package ru.arixcompany.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import ru.arixcompany.utils.IMinecraft;

@UtilityClass
public class InvUtil implements IMinecraft {
    public void clickSlot(Slot slot, int button, ClickType clickType, boolean silent) {
        if (slot != null) clickSlot(slot.index, button, clickType, silent);
    }

    public void clickSlot(int slotId, int buttonId, ClickType clickType, boolean silent) {
        clickSlot(mc.player.containerMenu.containerId, slotId, buttonId, clickType, silent);
    }

    public void clickSlot(int windowId, int slotId, int buttonId, ClickType clickType, boolean silent) {
        mc.gameMode.handleInventoryMouseClick(windowId, slotId, buttonId, clickType, mc.player);
        if (silent) mc.player.containerMenu.clicked(slotId, buttonId, clickType, mc.player);
    }
}
