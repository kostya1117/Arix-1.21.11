package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class AutoSetupEngine implements IMinecraft {

    private static final int TOP_COUNT = 3;

    private final List<ItemTarget> searchList;
    private final AutoBuy parent;
    private final int total;

    private final Timer pageTimer = new Timer();
    private final Timer refreshTimer = new Timer();
    private final Timer retryTimer = new Timer();

    @Getter
    private boolean running = false;
    private boolean waitingForAuction = false;
    private boolean retrying = false;
    private boolean finishingCurrent = false;

    private int currentIndex = 0;

    @Getter
    private int completed = 0;

    @Getter
    private int processed = 0;

    private final TreeSet<Integer> foundPrices = new TreeSet<>();

    public AutoSetupEngine(List<ItemTarget> searchList, AutoBuy parent) {
        this.searchList = new ArrayList<>(searchList);
        this.parent = parent;
        this.total = this.searchList.size();
    }

    public void start() {
        running = true;
        currentIndex = 0;
        completed = 0;
        processed = 0;
        foundPrices.clear();

        MessageSender.print("§eЗапущена автонастройка цен");

        if (mc.screen != null) mc.screen.onClose();

        searchCurrent();
    }

    public void stop() {
        running = false;
        waitingForAuction = false;
        retrying = false;
        finishingCurrent = false;

        if (mc.screen != null)
            mc.screen.onClose();

        MessageSender.print("§cАвтонастройка остановлена: "
                + completed + " успешных, "
                + processed + " обработано, всего " + total);
    }

    public void tick(int searchDurationSeconds, int updateDelayMillis, int discountPercent) {
        if (!running) return;

        if (waitingForAuction) {
            if (mc.screen instanceof ContainerScreen) {
                waitingForAuction = false;
                foundPrices.clear();
                pageTimer.reset();
                refreshTimer.reset();
            } else {
                if (!retrying) { retryTimer.reset(); retrying = true; }
                if (retryTimer.finished(1500)) { retrying = false; searchCurrent(); }
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
            if (!retrying && currentIndex < total) {
                retryTimer.reset();
                retrying = true;
            }
            if (retrying && retryTimer.finished(1500)) {
                retrying = false;
                searchCurrent();
            }
        }
    }

    private void searchCurrent() {
        if (currentIndex >= total) {
            finishAll();
            return;
        }

        ItemTarget data = searchList.get(currentIndex);

        waitingForAuction = true;
        retrying = false;
        finishingCurrent = false;

        if (mc.player != null)
            mc.player.connection.sendCommand("ah search " + data.getSearchTerm());
    }

    private void searchNext() {
        if (currentIndex >= total) {
            finishAll();
            return;
        }
        searchCurrent();
    }

    private void scanPrices(ContainerScreen screen) {
        var handler = screen.getMenu();
        for (Slot slot : handler.slots) {
            if (slot.container == mc.player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            if (!FuntimeUtil.hasPrice(stack)) continue;
            int pricePerItem = FuntimeUtil.getPricePerItem(stack);

            if (pricePerItem > 0) {
                foundPrices.add(pricePerItem);
            }
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
        if (finishingCurrent) return;
        finishingCurrent = true;
        processed++;

        ItemTarget target = searchList.get(currentIndex);

        if (!foundPrices.isEmpty()) {
            int minPrice = foundPrices.first();

            int buyPrice = (int) (minPrice * (1 - discountPercent / 100.0));

            if (buyPrice < 0) buyPrice = 0;

            parent.setPriceForItem(target.getId(), buyPrice);
            completed++;

            MessageSender.print("§a[+] §f" + target.getDisplayName() +
                    " §7| Мин цена: §e" + FuntimeUtil.formatPrice(minPrice) +
                    " §7| Твоя цена: §6" + FuntimeUtil.formatPrice(buyPrice));
        } else {
            MessageSender.print("§c[-] " + target.getDisplayName() + " (не найден)");
        }

        if (mc.screen != null) mc.screen.onClose();
        currentIndex++;

        new Thread(() -> {
            try { Thread.sleep(150); mc.execute(this::searchNext); } catch (Exception ignored) {}
        }).start();
    }

    private void finishAll() {
        running = false;
        waitingForAuction = false;
        retrying = false;
        finishingCurrent = false;

        if (mc.screen != null) {
            mc.execute(() -> mc.screen.onClose());
        }

        MessageSender.print("§aАвтонастройка завершена: "
                + completed + " успешных, "
                + processed + " обработано, всего " + total);

        Arix.getInstance().getModuleRepo().getModule(AutoBuy.class).autoSetupEnabled = false;

        new Thread(() -> {
            try {
                Thread.sleep(300);

                if (AutoBuy.autoBuyAfterSetup.isValue()) {
                    mc.execute(() -> {
                        if (mc.player != null) {
                            AutoBuy.setAutoBuyEnabled(true);

                            AutoBuy.autoBuyEngine.resetState();
                            AutoBuy.auctionController.reset();

                            AutoBuy.balanceController.request();

                            mc.player.connection.sendCommand("ah");
                        }
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }
}