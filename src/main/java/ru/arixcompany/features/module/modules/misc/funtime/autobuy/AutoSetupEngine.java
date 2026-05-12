package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
import net.minecraft.ChatFormatting;
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

    private final List<ItemTarget> searchList;
    private final AutoBuy parent;
    private final int total;

    private final Timer pageTimer = new Timer();
    private final Timer refreshTimer = new Timer();
    private final Timer retryTimer = new Timer();
    private final Timer transitionTimer = new Timer();

    @Getter private boolean running = false;
    private boolean waitingForAuction = false;
    private boolean transitioning = false;
    private boolean finishingCurrent = false;

    private int currentIndex = 0;
    @Getter private int completed = 0;
    @Getter private int processed = 0;

    private boolean hasRetriedCurrent = false;

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
        transitioning = false;
        hasRetriedCurrent = false;
        foundPrices.clear();
        MessageSender.print("§eЗапущена автонастройка цен");
        if (mc.screen != null) mc.screen.onClose();
        searchCurrent();
    }

    public void stop() {
        running = false;
        transitioning = false;
        if (mc.screen != null) mc.screen.onClose();

        MessageSender.print("§cАвтонастройка остановлена: "
                + completed + " успешных, "
                + processed + " обработано, всего " + total);
    }

    public void tick(int searchDurationSeconds, int updateDelayMillis, int discountPercent) {
        if (!running) return;

        if (transitioning) {
            if (transitionTimer.finished(400)) {
                transitioning = false;
                searchCurrent();
            }
            return;
        }

        if (waitingForAuction) {
            if (mc.screen instanceof ContainerScreen) {
                waitingForAuction = false;
                foundPrices.clear();
                pageTimer.reset();
                refreshTimer.reset();
            } else {
                if (retryTimer.finished(1500)) {
                    searchCurrent();
                }
            }
            return;
        }

        if (mc.screen instanceof ContainerScreen screen) {
            if (pageTimer.getTimePassed() < searchDurationSeconds * 1000L) {
                scanPrices(screen);

                if (refreshTimer.finished(updateDelayMillis)) {
                    foundPrices.clear();
                    refresh(screen);
                    refreshTimer.reset();
                }
            } else {
                finishCurrent(discountPercent);
            }
        }
        else if (!waitingForAuction && !transitioning) {
            if (retryTimer.finished(1500)) {
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
        finishingCurrent = false;
        retryTimer.reset();

        if (mc.player != null) {
            mc.execute(() -> {
                if (mc.screen != null) mc.screen.onClose();
                mc.player.connection.sendCommand("ah search " + data.getSearchTerm());
            });
        }
    }

    private void scanPrices(ContainerScreen screen) {
        ItemTarget target = searchList.get(currentIndex);
        var handler = screen.getMenu();

        for (Slot slot : handler.slots) {
            if (slot.container == mc.player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;

            if (!FuntimeUtil.hasPrice(stack)) continue;
            String itemNameRaw = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
            if (itemNameRaw.contains("не актуален")) continue;

            boolean matches = false;
            if (target.getLoreKeywords() != null && !target.getLoreKeywords().isEmpty()) {
                List<String> itemLore = FuntimeUtil.getLore(stack);
                matches = FuntimeUtil.checkLoreFullMatch(itemLore, target.getLoreKeywords());
            }
            if (!matches && target.isCheckByName()) {
                String itemName = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
                String targetName = ChatFormatting.stripFormatting(target.getDisplayName()).toLowerCase();
                if (itemName.contains(targetName)) matches = true;
            }
            if (!matches && target.isCheckByItem()) {
                if (stack.getItem() == target.getDisplayStack().getItem()) matches = true;
            }

            if (!matches) continue;

            int pricePerItem = FuntimeUtil.getPricePerItem(stack);
            if (pricePerItem > 0) {
                foundPrices.add(pricePerItem);
            }
        }
    }

    private void finishCurrent(int discountPercent) {
        if (finishingCurrent) return;

        ItemTarget target = searchList.get(currentIndex);

        if (!foundPrices.isEmpty()) {
            int minPrice = foundPrices.first();

            if (minPrice < target.getBuyPrice() && !hasRetriedCurrent) {
                MessageSender.print(" Цена на §e" + target.getDisplayName() + " §7упала. Перенастройка...");
                hasRetriedCurrent = true;
                finishingCurrent = true;
                if (mc.screen != null) mc.screen.onClose();
                transitioning = true;
                transitionTimer.reset();
                return;
            }

            int buyPrice = (int) (minPrice * (1 - discountPercent / 100.0));
            if (buyPrice < 0) buyPrice = 0;

            parent.setPriceForItem(target.getId(), buyPrice);
            completed++;
            processed++;

            MessageSender.print("§a[+] §f" + target.getDisplayName() +
                    " §7| Мин цена: §e" + FuntimeUtil.formatPrice(minPrice) +
                    " §7| Твоя цена покупки : §6" + FuntimeUtil.formatPrice(buyPrice));

            hasRetriedCurrent = false;
            currentIndex++;
        }
        else {
            if (!hasRetriedCurrent) {
                MessageSender.print("§e[-] " + target.getDisplayName() + " не найден. Пробую еще раз...");
                hasRetriedCurrent = true;
                finishingCurrent = true;
                if (mc.screen != null) mc.screen.onClose();
                transitioning = true;
                transitionTimer.reset();
                return;
            } else {
                MessageSender.print("§c[-] " + target.getDisplayName() + " (не найден после 2-х попыток)");
                processed++;
                hasRetriedCurrent = false;
                currentIndex++;
            }
        }

        finishingCurrent = true;
        if (mc.screen != null) mc.screen.onClose();
        transitioning = true;
        transitionTimer.reset();
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

    private void finishAll() {
        running = false;
        transitioning = false;
        waitingForAuction = false;
        if (mc.screen != null) mc.execute(() -> mc.screen.onClose());
        MessageSender.print("§aАвтонастройка завершена: " + completed + "/" + total);
        Arix.getInstance().getModuleRepo().getModule(AutoBuy.class).autoSetupEnabled = false;

        new Thread(() -> {
            try {
                Thread.sleep(300);
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.connection.sendCommand("ah");
                        if (AutoBuy.autoBuyAfterSetup.isValue()) {
                            AutoBuy.balanceController.request();
                            AutoBuy.autoBuyEnabled = true;
                            AutoBuy.autoBuyEngine.resetState();
                        }
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }
}