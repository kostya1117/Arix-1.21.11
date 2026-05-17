package ru.arixcompany.features.module.modules.player;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.*;
import ru.arixcompany.features.repos.alerts.AlertRepo;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.inv.InventoryUtility;
import ru.arixcompany.utils.player.inv.SearchInvResult;
import ru.arixcompany.utils.player.inv.UseHandler;

import static ru.arixcompany.utils.player.inv.InventoryUtility.clickSlot;

public class ClickActions extends Module {
    private final UseHandler useHandler = new UseHandler();

    private final SelectSetting mode = new SelectSetting("Режим").value("Обычный", "Незаметный");
    private final ValueSetting swapDelay = new ValueSetting("Задержка свапа")
            .range(30, 300)
            .setValue(100)
            .setStep(1);

    //предметы
    private final BindSetting pearl = new BindSetting("Эндер жемчуг");
    private final BindSetting zaradvetra = new BindSetting("Заряд ветра");

    private final BindSetting eKey = new BindSetting("Кнопка элитры");
    private final BindSetting fKey = new BindSetting("Кнопка фейерверк");
    private final BooleanSetting startFireWork = new BooleanSetting("Авто фейерверк");

    private final Timer switchTimer = new Timer();
    public static boolean swapping = false;

    public ClickActions() {
        super("ClickActions", Category.Player);
        setup(mode, swapDelay, pearl, zaradvetra,
                new GroupSetting("Элитры", eKey, fKey, startFireWork));
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (event.getAction() != 1 || mc.screen != null) return;
        int key = event.getKey();

        if (key == pearl.getKey()) usePearl();
        if (key == zaradvetra.getKey()) useZarad();

        if (eKey.getKey() == key && switchTimer.every(200)) {
            swapChest();
        }

        if (fKey.getKey() == key && mc.player.isFallFlying()) {
            useFireWork();
        }
    }

    private void usePearl() {
        useHandler
                .setMode(mode.isSelected("Незаметный") ? UseHandler.UseMode.SILENT : UseHandler.UseMode.NORMAL)
                .setSwapDelay(swapDelay.getInt())
                .use(Items.ENDER_PEARL);
    }

    private void useZarad() {
        useHandler
                .setMode(mode.isSelected("Незаметный") ? UseHandler.UseMode.SILENT : UseHandler.UseMode.NORMAL)
                .setSwapDelay(swapDelay.getInt())
                .use(Items.WIND_CHARGE);
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
            SearchInvResult res = InventoryUtility.findItemInInventory(item);
            if (res.found()) return res.slot();
        }
        return -1;
    }

    private void swapChest() {
        if (swapping) return;

        boolean hasElytraOn = mc.player.getInventory().getItem(38).getItem() == Items.ELYTRA;
        int targetSlot;

        if (hasElytraOn) {
            targetSlot = getChestPlateSlot();
            if (targetSlot == -1) {
                AlertRepo.error("Нагрудник не найден!");
                return;
            }
        } else {
            SearchInvResult result = InventoryUtility.findItemInInventory(Items.ELYTRA);
            if (!result.found()) {
                AlertRepo.error("Элитры не найдены!");
                return;
            }
            targetSlot = result.slot();
        }

        new Thread(() -> {
            try {
                swapping = true;
                clickSlot(targetSlot);
                Thread.sleep(swapDelay.getInt());
                clickSlot(6);
                Thread.sleep(swapDelay.getInt());
                clickSlot(targetSlot);
                Thread.sleep(swapDelay.getInt());

                mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));

                if (!hasElytraOn && startFireWork.isValue() && mc.player.fallDistance > 0.05) {
                    mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }

                AlertRepo.success(hasElytraOn ? "Надет нагрудник" : "Надеты элитры");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                swapping = false;
            }
        }).start();
    }
}