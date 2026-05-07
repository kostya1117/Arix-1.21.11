package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.misc.funtime.AutoBuy;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.MessageSender;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.InvUtil;

import java.util.List;

public class AutoSetupEngine implements IMinecraft {

    private final List<ItemTarget> searchList;
    private final AutoBuy parent;

    private final Timer pageTimer = new Timer();
    private final Timer refreshTimer = new Timer();
    private final Timer retryTimer = new Timer();

    @Getter
    private boolean running = false;
    private boolean waitingForAuction = false;
    private boolean retrying = false;

    private int currentIndex = 0;
    @Getter
    private int completed = 0;
    private int bestPrice = Integer.MAX_VALUE;

    public AutoSetupEngine(List<ItemTarget> searchList,
                           AutoBuy parent) {

        this.searchList = searchList;
        this.parent = parent;
    }

    public void start(int discountPercent) {

        running = true;
        currentIndex = 0;
        completed = 0;
        retrying = false;
        bestPrice = Integer.MAX_VALUE;

        MessageSender.print("§eЗапущена автонастройка цен...");

        if (mc.screen != null)
            mc.screen.onClose();

        searchCurrent();
    }

    public void stop() {

        running = false;
        waitingForAuction = false;
        retrying = false;

        if (mc.screen != null)
            mc.screen.onClose();

        MessageSender.print("§cАвтонастройка остановлена: "
                + completed + " / " + searchList.size());
    }

    public void tick(int searchDurationSeconds,
                     int updateDelayMillis,
                     int discountPercent) {

        if (!running) return;

        if (waitingForAuction) {

            if (mc.screen instanceof ContainerScreen screen) {
                waitingForAuction = false;
                retrying = false;

                bestPrice = Integer.MAX_VALUE;

                pageTimer.reset();
                refreshTimer.reset();

                MessageSender.print("Меню открыто, начинаю сканирование...");


            } else {
                if (!retrying) {
                    retrying = true;
                    retryTimer.reset();
                }

                if (retrying && retryTimer.finished(1500)) {
                    retrying = false;
                    searchCurrent();
                }
            }

            return;
        }

        if (mc.screen instanceof ContainerScreen screen) {

            if (pageTimer.getTimePassed() < searchDurationSeconds * 1000L) {

                scanPrices(screen);

                if (refreshTimer.finished(updateDelayMillis)) {
                    refresh(screen);
                    refreshTimer.reset();
                }

            } else {
                finishCurrent(discountPercent);
            }

        } else {

            if (!retrying && currentIndex < searchList.size()) {
                retrying = true;
                retryTimer.reset();
            }

            if (retrying && retryTimer.finished(1500)) {
                retrying = false;
                searchCurrent();
            }
        }
    }

    private void searchNext() {

        if (currentIndex >= searchList.size()) {
            finishAll();
            return;
        }

        searchCurrent();
    }

    private void searchCurrent() {

        if (currentIndex >= searchList.size()) {
            finishAll();
            return;
        }

        ItemTarget data = searchList.get(currentIndex);

        bestPrice = Integer.MAX_VALUE;
        waitingForAuction = true;
        retrying = false;

        if (mc.player != null)
            mc.player.connection.sendCommand("ah search " + data.getSearchTerm());
    }

    private void scanPrices(ContainerScreen screen) {

        var handler = screen.getMenu();

        for (Slot slot : handler.slots) {

            if (slot.container == mc.player.getInventory())
                continue;

            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            String name = stack.getHoverName().getString().toLowerCase();
            if (name.contains("обновить") || name.contains("refresh")) continue;

            if (!FuntimeUtil.hasPrice(stack)) continue;

            int pricePerItem = FuntimeUtil.getPricePerItem(stack);

            if (pricePerItem > 0 && pricePerItem < bestPrice)
                bestPrice = pricePerItem;
        }
    }

    private void refresh(ContainerScreen screen) {

        var handler = screen.getMenu();

        for (Slot slot : handler.slots) {

            String name = slot.getItem().getHoverName().getString().toLowerCase();

            if (name.contains("обновить") || name.contains("refresh")) {
                InvUtil.clickSlot(slot.index, 0, net.minecraft.world.inventory.ClickType.PICKUP, false);
                break;
            }
        }
    }

    private void finishCurrent(int discountPercent) {
        completed++;

        if (bestPrice != Integer.MAX_VALUE && bestPrice > 0) {

            int discounted =
                    (int) (bestPrice * (1 - discountPercent / 100.0));

            if (discounted < 0) discounted = 0;

            parent.setPriceForItem(
                    searchList.get(currentIndex).getId(),
                    discounted
            );

            MessageSender.print("§aУстановлена цена "
                    + discounted
                    + " для "
                    + searchList.get(currentIndex).getId());
        }

        if (mc.screen != null)
            mc.screen.onClose();

        currentIndex++;

        new Thread(() -> {
            try {
                Thread.sleep(500);
                mc.execute(this::searchNext);
            } catch (Exception ignored) {}
        }).start();
    }

    private void finishAll() {

        running = false;

        if (mc.screen != null)
            mc.screen.onClose();

        MessageSender.print("§aАвтонастройка завершена: "
                + completed + " / " + searchList.size());
        Arix.getInstance().getModuleRepo().getModule(AutoBuy.class).autoSetupEnabled = false;
    }
}