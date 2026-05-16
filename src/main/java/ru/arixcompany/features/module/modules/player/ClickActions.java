package ru.arixcompany.features.module.modules.player;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.*;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.PlayerUtil;
import ru.arixcompany.utils.player.inv.InventoryUtility;
import ru.arixcompany.utils.player.inv.SearchInvResult;
import ru.arixcompany.utils.player.inv.UseHandler;

import static ru.arixcompany.utils.player.inv.InventoryUtility.clickSlot;

public class ClickActions extends Module {
    private final UseHandler useHandler = new UseHandler();

    private SelectSetting mode = new SelectSetting("Режим")
            .value("Обычный",
                    "Незаметный");
    private ValueSetting swapDelay = new ValueSetting("Задержка свапа")
            .range(30,300)
            .setValue(100)
            .setStep(1);
    private final BindSetting pearl = new BindSetting("Эндер жемчуг");

    //элитры
    BooleanSetting delay = new BooleanSetting("Задержка");
    BindSetting eKey = new BindSetting("Кнопка элитры");
    BindSetting fKey = new BindSetting("Кнопка фейерверк");
    BooleanSetting startFireWork = new BooleanSetting("Авто фейерверк");

    private final Timer switchTimer = new Timer();
    private final Timer fireworkTimer = new Timer();

    public static boolean swapping = false;

    public ClickActions() {
        super("ClickActions", Category.Player);
        setup(mode,pearl,swapDelay,
                new GroupSetting("Элитры",delay,eKey,fKey,startFireWork));
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (event.getAction() != 1 || mc.screen != null) return;

        int key = event.getKey();

        if (key == pearl.getKey()) {
            usePearl();
        }
        if (eKey.getKey() == key && switchTimer.every(250))
            swapChest();

        if (fKey.getKey() == key && mc.player.isFallFlying())
            useFireWork();
    }

    private void usePearl() {
            useHandler
                    .setMode(mode.isSelected("Незаметный") ? UseHandler.UseMode.SILENT : UseHandler.UseMode.NORMAL)
                    .setSwapDelay(swapDelay.getInt())
                    .use(Items.ENDER_PEARL);
    }

    private void useFireWork() {
        useHandler
                .setMode(mode.isSelected("Незаметный") ? UseHandler.UseMode.SILENT : UseHandler.UseMode.NORMAL)
                .setSwapDelay(swapDelay.getInt())
                .use(Items.FIREWORK_ROCKET);
    }


    public static int getChestPlateSlot() {
        Item[] items = {Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE};
        for (Item item : items) {
            SearchInvResult slot = InventoryUtility.findItemInInventory(item);
            if (slot.found()) {
                return slot.slot();
            }
        }
        return -1;
    }

    private void swapChest() {
        SearchInvResult result = InventoryUtility.findItemInInventory(Items.ELYTRA);

        if (mc.player.getInventory().getItem(38).getItem() == Items.ELYTRA) {
            int slot = getChestPlateSlot();
            if (slot != -1) {
                if (delay.isValue()) {
                    swapping = true;
                    clickSlot(slot);
                    try {
                        Thread.sleep(100);
                    } catch (Exception ignored) {
                    }
                    clickSlot(6);
                    try {
                        Thread.sleep(100);
                    } catch (Exception ignored) {
                    }
                    clickSlot(slot);
                    mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                    swapping = false;
                } else {
                    clickSlot(slot);
                    clickSlot(6);
                    clickSlot(slot);
                    mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                }
            } else {
                print("У тебя нет нагрудника!");
            }
        } else if (result.found()) {
            if (delay.isValue())
                new Thread(() -> {
                    swapping = true;
                    clickSlot(result.slot());
                    try {
                        Thread.sleep(200);
                    } catch (Exception ignored) {
                    }
                    clickSlot(6);
                    try {
                        Thread.sleep(200);
                    } catch (Exception ignored) {
                    }
                    clickSlot(result.slot());
                    mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                    if (startFireWork.isValue() && mc.player.fallDistance > 0)
                        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    swapping = false;
                }).start();
            else {
                clickSlot(result.slot());
                clickSlot(6);
                clickSlot(result.slot());
                mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                if (startFireWork.isValue() && mc.player.fallDistance > 0)
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
        } else {
            print("У тебя нет элитры!");
        }
    }
}
