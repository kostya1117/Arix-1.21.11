package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.MessageSender;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.InvUtil;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.TreeSet;

public class AutoSellEngine implements IMinecraft {

    public record SellTask(ItemTarget target, int count, int buyPricePerOne) {}

    private enum Phase { IDLE, WAIT_SCREEN, SCAN, WAIT_SELL }

    private final Queue<SellTask>    queue       = new ArrayDeque<>();
    private final TreeSet<Integer>   prices      = new TreeSet<>();
    private final Timer              timer       = new Timer();
    private final Timer              retryTimer  = new Timer();

    @Getter private boolean running = false;
    private Phase    phase          = Phase.IDLE;
    private SellTask task           = null;
    @Getter
    private int      sellPrice      = 0;
    private boolean  transitioning  = false;
    private Runnable onDone         = null;

    private int discountPct    = 25;
    private int scanMs         = 5000;
    private int refreshMs      = 2500;

    public void setParams(int discountPct, int scanMs, int refreshMs) {
        this.discountPct = discountPct;
        this.scanMs      = scanMs;
        this.refreshMs   = refreshMs;
    }

    public void setOnDoneCallback(Runnable cb) { this.onDone = cb; }

    public boolean isActive() { return running; }

    public void enqueueSell(ItemTarget target, int count, int buyPricePerOne) {
        queue.add(new SellTask(target, count, buyPricePerOne));
        if (!running) startNext();
    }

    public void tick() {
        if (!running || task == null) return;

        if (transitioning) {
            if (timer.finished(400)) { transitioning = false; openSearch(); }
            return;
        }

        switch (phase) {
            case WAIT_SCREEN -> {
                if (mc.screen instanceof ContainerScreen) {
                    prices.clear();
                    timer.reset();
                    retryTimer.reset();
                    phase = Phase.SCAN;
                } else if (retryTimer.finished(1500)) {
                    openSearch();
                }
            }
            case SCAN -> {
                if (!(mc.screen instanceof ContainerScreen screen)) {
                    openSearch();
                    return;
                }
                scanPrices(screen);
                if (retryTimer.finished(refreshMs)) {
                    prices.clear();
                    refresh(screen);
                    retryTimer.reset();
                }
                if (timer.finished(scanMs)) finishScan();
            }
            case WAIT_SELL -> {
                if (timer.finished(5000)) {
                    MessageSender.print("§cАвтопродажа: таймаут. Пропускаю...");
                    finish();
                }
            }
        }
    }

    public boolean handleMessage(String msg) {
        if (!running || task == null) return false;

        if (msg.contains("слишком дорого") && msg.contains("введите команду продажи")) {
            if (sellPrice > 0 && task != null) {
                int total = sellPrice * task.count();
                mc.execute(() -> sendSell(total));
            }
            return true;
        }

        if (phase == Phase.WAIT_SELL) {
            if (msg.contains("выставлен на продажу") || msg.contains("лот выставлен")
                    || msg.contains("товар выставлен") || msg.contains("выставлен на аукцион")
                    || msg.contains("успешно выставлен")) {
                int profit = sellPrice * task.count() - task.buyPricePerOne() * task.count();
                MessageSender.print("§a[Продажа] §f" + task.target().getDisplayName()
                        + " §7x" + task.count()
                        + " §7| Цена: §e" + FuntimeUtil.formatPrice(sellPrice) + "/шт"
                        + " §7| Прибыль: " + (profit >= 0 ? "§a+" : "§c") + FuntimeUtil.formatPrice(profit));
                finish();
                return true;
            }
            if (msg.contains("нет предмета") || msg.contains("инвентарь пуст")
                    || msg.contains("не удалось выставить") || msg.contains("не найден в инвентаре")) {
                MessageSender.print("§c[Продажа] Ошибка выставления " + task.target().getDisplayName());
                finish();
                return true;
            }
        }
        return false;
    }

    public void stop() {
        running = false; phase = Phase.IDLE; task = null;
        queue.clear(); prices.clear(); sellPrice = 0; transitioning = false;
        if (mc.screen != null) mc.execute(() -> mc.screen.onClose());
    }

    private void startNext() {
        task = queue.poll();
        if (task == null) { running = false; return; }
        running = true; sellPrice = 0; transitioning = false;
        prices.clear();
        MessageSender.print("§e[Продажа] Анализирую цены на §f" + task.target().getDisplayName() + "§e...");
        if (mc.screen != null) mc.screen.onClose();
        openSearch();
    }

    private void openSearch() {
        phase = Phase.WAIT_SCREEN;
        retryTimer.reset();
        if (mc.player != null) mc.execute(() -> {
            if (mc.screen != null) mc.screen.onClose();
            mc.player.connection.sendCommand("ah search " + task.target().getSearchTerm());
        });
    }

    private void scanPrices(ContainerScreen screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == mc.player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !FuntimeUtil.hasPrice(stack)) continue;
            String name = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
            if (name.contains("не актуален") || name.contains("обновить") || name.contains("refresh")) continue;
            if (!matches(stack)) continue;
            int p = FuntimeUtil.getPricePerItem(stack);
            if (p > 0) prices.add(p);
        }
    }

    private boolean matches(ItemStack stack) {
        ItemTarget t = task.target();
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
        if (t.getLoreKeywords() != null && !t.getLoreKeywords().isEmpty())
            if (FuntimeUtil.checkLoreFullMatch(FuntimeUtil.getLore(stack), t.getLoreKeywords())) return true;
        if (t.isCheckByName())
            if (name.contains(ChatFormatting.stripFormatting(t.getDisplayName()).toLowerCase())) return true;
        if (t.isCheckByItem())
            if (stack.getItem() == t.getDisplayStack().getItem()) return true;
        return false;
    }

    private void finishScan() {
        if (prices.isEmpty()) {
            MessageSender.print("§e[Продажа] §f" + task.target().getDisplayName() + " §e— цены не найдены. Повторяю...");
            if (mc.screen != null) mc.screen.onClose();
            transitioning = true;
            timer.reset();
            return;
        }

        double avg = prices.stream().mapToInt(i -> i).average().orElse(0);
        sellPrice = Math.max(
                (int) (avg * (1.0 - discountPct / 100.0)),
                task.buyPricePerOne()
        );
        boolean adjusted = sellPrice == (int)(task.buyPricePerOne());

        MessageSender.print("§e[Продажа] §f" + task.target().getDisplayName()
                + " §7| Ср: §e" + FuntimeUtil.formatPrice((int) avg)
                + " §7| Мин: §e" + FuntimeUtil.formatPrice(prices.first())
                + " §7| Продаю: §a" + FuntimeUtil.formatPrice(sellPrice) + "/шт"
                + (adjusted ? " §7(скорректировано)" : ""));

        if (mc.screen != null) mc.screen.onClose();
        new Thread(() -> {
            try { Thread.sleep(300); } catch (Exception ignored) {}
            mc.execute(this::placeItem);
        }).start();
    }

    private void placeItem() {
        if (mc.player == null || task == null) return;
        var inv = mc.player.getInventory();
        int slot = -1, count = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && matches(s)) { slot = i; count = s.getCount(); break; }
        }

        if (slot == -1) {
            MessageSender.print("§c[Продажа] §f" + task.target().getDisplayName() + " §cне найден в инвентаре.");
            finish();
            return;
        }

        final int finalSlot = slot, total = sellPrice * count;
        mc.execute(() -> {
            if (mc.player == null) return;
            if (finalSlot >= 9) {
                int hotbar = mc.player.getInventory().selected;
                InvUtil.clickSlot(finalSlot, 0, ClickType.PICKUP, false);
                InvUtil.clickSlot(hotbar, 0, ClickType.PICKUP, false);
            } else {
                mc.player.getInventory().selected = finalSlot;
            }
            new Thread(() -> {
                try { Thread.sleep(150); } catch (Exception ignored) {}
                mc.execute(() -> sendSell(total));
            }).start();
        });
    }

    private void sendSell(int total) {
        if (mc.player == null) return;
        mc.player.connection.sendCommand("ah sell " + total);
        phase = Phase.WAIT_SELL;
        mc.execute(timer::reset);
    }

    private void refresh(ContainerScreen screen) {
        for (Slot slot : screen.getMenu().slots) {
            String name = slot.getItem().getHoverName().getString().toLowerCase();
            if (name.contains("обновить") || name.contains("refresh")) {
                InvUtil.clickSlot(slot.index, 0, ClickType.PICKUP, false);
                break;
            }
        }
    }

    private void finish() {
        task = null; prices.clear(); sellPrice = 0; phase = Phase.IDLE;

        if (!queue.isEmpty()) {
            transitioning = true;
            timer.reset();
        } else {
            running = false;
            if (onDone != null) new Thread(() -> {
                try { Thread.sleep(600); } catch (Exception ignored) {}
                mc.execute(onDone);
            }).start();
        }
    }
}
