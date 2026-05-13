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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;

public class AutoSellEngine implements IMinecraft {

    public record SellTask(ItemTarget target, int count, int buyPricePerOne) {}

    /**
     * Состояния машины:
     * IDLE          — ничего не делаем
     * CLOSE_SCREEN  — ждём 2с после закрытия экрана перед отправкой команды
     * WAIT_SCREEN   — отправили /ah search, ждём открытия экрана (до 3с, потом retry)
     * SCAN          — сканируем цены N секунд
     * CLOSE_FOR_SELL— закрыли аукцион, ждём 2с перед выставлением
     * WAIT_SELL     — отправили /ah sell, ждём подтверждения (до 6с)
     * DONE_DELAY    — продали, ждём 2с перед открытием /ah
     */
    private enum Phase {
        IDLE, CLOSE_SCREEN, WAIT_SCREEN, SCAN, CLOSE_FOR_SELL, WAIT_SELL, DONE_DELAY
    }

    private static final int STEP_DELAY  = 2000; // 2с между действиями
    private static final int SCREEN_WAIT = 3000; // макс ожидание открытия экрана
    private static final int SELL_WAIT   = 6000; // макс ожидание подтверждения продажи

    private final Queue<SellTask>        queue      = new ArrayDeque<>();
    private final Map<Integer, Integer>  slotPrices = new LinkedHashMap<>();
    private final Timer                  timer      = new Timer();

    @Getter private boolean running = false;
    private Phase    phase     = Phase.IDLE;
    private SellTask task      = null;
    private int      sellPrice = 0;
    private Runnable onDone    = null;

    private int discountPct = 25;
    private int scanMs      = 5000;
    private int refreshMs   = 2500;

    // для обновления страницы во время сканирования
    private final Timer refreshTimer = new Timer();

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

    /** Вызывается каждый тик из AutoBuy — только на render thread */
    public void tick() {
        if (!running || task == null) return;

        switch (phase) {

            // Закрываем экран, ждём 2с, потом отправляем /ah search
            case CLOSE_SCREEN -> {
                if (mc.screen != null) mc.screen.onClose();
                if (timer.finished(STEP_DELAY)) {
                    mc.player.connection.sendCommand("ah search " + task.target().getSearchTerm());
                    timer.reset();
                    phase = Phase.WAIT_SCREEN;
                }
            }

            // Ждём открытия экрана аукциона
            case WAIT_SCREEN -> {
                if (mc.screen instanceof ContainerScreen) {
                    slotPrices.clear();
                    timer.reset();
                    refreshTimer.reset();
                    phase = Phase.SCAN;
                } else if (timer.finished(SCREEN_WAIT)) {
                    // Экран не открылся — повторяем
                    mc.player.connection.sendCommand("ah search " + task.target().getSearchTerm());
                    timer.reset();
                }
            }

            // Сканируем цены
            case SCAN -> {
                if (!(mc.screen instanceof ContainerScreen screen)) {
                    // Экран закрылся — переоткрываем
                    timer.reset();
                    phase = Phase.CLOSE_SCREEN;
                    return;
                }
                scanPrices(screen);
                if (refreshTimer.finished(refreshMs)) {
                    slotPrices.clear();
                    refresh(screen);
                    refreshTimer.reset();
                }
                if (timer.finished(scanMs)) {
                    finishScan();
                }
            }

            // Закрыли аукцион после сканирования, ждём 2с перед выставлением
            case CLOSE_FOR_SELL -> {
                if (mc.screen != null) mc.screen.onClose();
                if (timer.finished(STEP_DELAY)) {
                    placeItem();
                }
            }

            // Ждём подтверждения от сервера
            case WAIT_SELL -> {
                if (timer.finished(SELL_WAIT)) {
                    MessageSender.print("§cАвтопродажа: таймаут. Пропускаю...");
                    finish();
                }
            }

            // Продали, ждём 2с, потом открываем /ah
            case DONE_DELAY -> {
                if (timer.finished(STEP_DELAY)) {
                    running = false;
                    phase = Phase.IDLE;
                    if (onDone != null) onDone.run();
                }
            }
        }
    }

    public boolean handleMessage(String msg) {
        if (!running || task == null) return false;

        // Сервер просит подтвердить высокую цену — повторяем команду
        if (msg.contains("слишком дорого") && msg.contains("введите команду продажи")) {
            if (sellPrice > 0 && phase == Phase.WAIT_SELL) {
                int total = sellPrice * task.count();
                mc.execute(() -> {
                    if (mc.player != null) {
                        mc.player.connection.sendCommand("ah sell " + total);
                        timer.reset();
                    }
                });
            }
            return true;
        }

        if (phase == Phase.WAIT_SELL) {
            if (msg.contains("выставлен на продажу") || msg.contains("лот выставлен")
                    || msg.contains("товар выставлен") || msg.contains("выставлен на аукцион")
                    || msg.contains("успешно выставлен")) {
                int profit = (sellPrice - task.buyPricePerOne()) * task.count();
                MessageSender.print("§a[Продажа] §f" + task.target().getDisplayName()
                        + " §7x" + task.count()
                        + " §7| Цена: §e" + FuntimeUtil.formatPrice(sellPrice) + "/шт"
                        + " §7| Прибыль: " + (profit >= 0 ? "§a+" : "§c") + FuntimeUtil.formatPrice(profit));
                mc.execute(this::finish);
                return true;
            }
            if (msg.contains("нет предмета") || msg.contains("инвентарь пуст")
                    || msg.contains("не удалось выставить") || msg.contains("не найден в инвентаре")) {
                MessageSender.print("§c[Продажа] Ошибка выставления " + task.target().getDisplayName());
                mc.execute(this::finish);
                return true;
            }
        }
        return false;
    }

    public void stop() {
        running = false; phase = Phase.IDLE; task = null;
        queue.clear(); slotPrices.clear(); sellPrice = 0;
        if (mc.screen != null) mc.execute(() -> mc.screen.onClose());
    }

    // ── Приватные ────────────────────────────────────────────────────────────

    private void startNext() {
        task = queue.poll();
        if (task == null) { running = false; return; }
        running = true;
        sellPrice = 0;
        slotPrices.clear();
        MessageSender.print("§e[Продажа] Анализирую цены на §f" + task.target().getDisplayName() + "§e...");
        timer.reset();
        phase = Phase.CLOSE_SCREEN;
    }

    private void scanPrices(ContainerScreen screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == mc.player.getInventory()) continue;
            if (slotPrices.containsKey(slot.index)) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !FuntimeUtil.hasPrice(stack)) continue;
            String name = ChatFormatting.stripFormatting(stack.getHoverName().getString()).toLowerCase();
            if (name.contains("не актуален") || name.contains("обновить") || name.contains("refresh")) continue;
            if (!matches(stack)) continue;
            int p = FuntimeUtil.getPricePerItem(stack);
            if (p > 0) slotPrices.put(slot.index, p);
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
        if (slotPrices.isEmpty()) {
            MessageSender.print("§e[Продажа] §f" + task.target().getDisplayName() + " §e— цены не найдены. Повторяю...");
            timer.reset();
            phase = Phase.CLOSE_SCREEN;
            return;
        }

        var vals = slotPrices.values();
        double avg = vals.stream().mapToInt(i -> i).average().orElse(0);
        int min    = vals.stream().mapToInt(i -> i).min().orElse(0);

        sellPrice = Math.max(
                (int) (avg * (1.0 - discountPct / 100.0)),
                task.buyPricePerOne()
        );

        MessageSender.print("§e[Продажа] §f" + task.target().getDisplayName()
                + " §7| Ср: §e" + FuntimeUtil.formatPrice((int) avg)
                + " §7| Мин: §e" + FuntimeUtil.formatPrice(min)
                + " §7| Продаю: §a" + FuntimeUtil.formatPrice(sellPrice) + "/шт"
                + (sellPrice == task.buyPricePerOne() ? " §7(скорректировано)" : ""));

        timer.reset();
        phase = Phase.CLOSE_FOR_SELL;
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

        // Перемещаем в хотбар если нужно
        if (slot >= 9) {
            int hotbar = mc.player.getInventory().selected;
            InvUtil.clickSlot(slot, 0, ClickType.PICKUP, false);
            InvUtil.clickSlot(hotbar, 0, ClickType.PICKUP, false);
            count = mc.player.getInventory().getItem(hotbar).getCount();
            if (count == 0) count = task.count(); // fallback
        } else {
            mc.player.getInventory().selected = slot;
        }

        mc.player.connection.sendCommand("ah sell " + (sellPrice * count));
        phase = Phase.WAIT_SELL;
        timer.reset();
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
        task = null; slotPrices.clear(); sellPrice = 0;

        if (!queue.isEmpty()) {
            // Следующая задача — тоже через CLOSE_SCREEN с задержкой
            task = queue.poll();
            sellPrice = 0;
            slotPrices.clear();
            MessageSender.print("§e[Продажа] Анализирую цены на §f" + task.target().getDisplayName() + "§e...");
            timer.reset();
            phase = Phase.CLOSE_SCREEN;
        } else {
            // Очередь пуста — ждём 2с и открываем /ah
            timer.reset();
            phase = Phase.DONE_DELAY;
        }
    }

    public SellTask getCurrentTask() { return task; }
    public int getQueueSize()        { return queue.size(); }
    public int getSellPrice()        { return sellPrice; }
    public int getPricesCount()      { return slotPrices.size(); }
}
