package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.features.module.modules.misc.funtime.AutoBuy;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.MessageSender;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.InvUtil;

import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

public class AutoSellEngine implements IMinecraft {

    @Getter private boolean selling = false;

    // ── Флаги состояния — точно как в AutoSetupEngine ─────────────────────────
    private boolean waitingForAuction = false;
    private boolean transitioning     = false;
    private boolean finishingCurrent  = false;

    private ItemTarget currentTarget;
    private int boughtPrice;
    private int margin;
    private int stackSize;

    // Таймеры — те же что в AutoSetupEngine
    private final Timer pageTimer       = new Timer();   // длительность сканирования
    private final Timer refreshTimer    = new Timer();   // интервал обновления страницы
    private final Timer retryTimer      = new Timer();   // ретрай если аукцион не открылся
    private final Timer transitionTimer = new Timer();   // пауза между шагами
    private final Timer postTimer       = new Timer();   // пауза перед /ah sell

    private final TreeSet<Integer> foundPrices = new TreeSet<>();

    private long scanDurationMs  = 2000; // рандом 1-3 сек, задаётся при старте сканирования
    private long randomSellDelay = 1500;
    private int  finalSellPrice  = 0;
    private boolean waitingToPost = false;

    // ── Точка входа ──────────────────────────────────────────────────────────

    public void processSell(ItemTarget target, int boughtTotalPrice, int marginPercent) {
        if (selling) return;

        this.currentTarget = target;
        this.boughtPrice   = boughtTotalPrice;
        this.margin        = marginPercent;
        this.selling       = true;
        this.foundPrices.clear();
        this.waitingToPost    = false;
        this.finishingCurrent = false;

        // Останавливаем AutoBuy
        AutoBuy.setAutoBuyEnabled(false);
        AutoBuy.autoBuyEngine.resetState();

        // Закрываем экран и запускаем переход — точно как AutoSetupEngine.start()
        if (mc.screen != null) mc.screen.onClose();
        transitioning = true;
        transitionTimer.reset();
    }

    // ── Главный тик — структура 1:1 с AutoSetupEngine.tick() ─────────────────

    public void tick() {
        if (!selling) return;

        // 1. Переход (пауза после закрытия экрана) — как в AutoSetupEngine
        if (transitioning) {
            if (transitionTimer.finished(400)) {
                transitioning = false;
                doSearch();
            }
            return;
        }

        // 2. Ожидание открытия аукциона — как в AutoSetupEngine
        if (waitingForAuction) {
            if (mc.screen instanceof ContainerScreen) {
                // Аукцион открылся
                waitingForAuction = false;
                foundPrices.clear();
                scanDurationMs = ThreadLocalRandom.current().nextLong(1000, 3001);
                pageTimer.reset();
                refreshTimer.reset();
            } else {
                // Ретрай через 1.5 сек — как в AutoSetupEngine
                if (retryTimer.finished(1500)) {
                    doSearch();
                }
            }
            return;
        }

        // 3. Пауза перед /ah sell
        if (waitingToPost) {
            if (postTimer.finished(randomSellDelay)) {
                postToAuction();
            }
            return;
        }

        // 4. Сканирование — точная копия AutoSetupEngine scanning-блока
        if (mc.screen instanceof ContainerScreen screen) {
            // Сканируем пока не истёк таймер
            if (pageTimer.getTimePassed() < scanDurationMs) {
                scanPrices(screen);

                // Обновляем страницу — как в AutoSetupEngine
                if (refreshTimer.finished(600)) {
                    foundPrices.clear(); // сбрасываем как в AutoSetupEngine перед refresh
                    refresh(screen);
                    refreshTimer.reset();
                }
            } else {
                // Время вышло — считаем цену
                calculatePrice();
            }
        } else if (!waitingForAuction && !transitioning && !waitingToPost) {
            // Экран закрылся — ретрай как в AutoSetupEngine
            if (retryTimer.finished(1500)) {
                doSearch();
            }
        }
    }

    // ── /ah search — точная копия AutoSetupEngine.searchCurrent() ────────────

    private void doSearch() {
        if (mc.player == null) { stop(); return; }

        int invSlot = findItem(currentTarget);
        if (invSlot == -1) {
            log("§cПредмет не найден в инвентаре!");
            stop();
            return;
        }

        ItemStack stack = mc.player.getInventory().getItem(invSlot);
        this.stackSize = Math.max(1, stack.getCount());

        // Берём предмет в руку
        if (invSlot < 9) {
            mc.player.getInventory().selected = invSlot;
        } else {
            mc.gameMode.handleInventoryMouseClick(
                    mc.player.containerMenu.containerId,
                    invSlot, 0, ClickType.SWAP, mc.player);
            mc.player.getInventory().selected = 0;
        }

        waitingForAuction = true;
        finishingCurrent  = false;
        retryTimer.reset();

        // Закрываем экран и шлём команду — как AutoSetupEngine.searchCurrent()
        if (mc.screen != null) mc.screen.onClose();
        mc.player.connection.sendCommand("ah search " + currentTarget.getSearchTerm());
        log("§7[Продажа] Ищу §f" + currentTarget.getDisplayName() + "§7...");
    }

    // ── Сканирование цен — точная копия AutoSetupEngine.scanPrices() ─────────

    private void scanPrices(ContainerScreen screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == mc.player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || !FuntimeUtil.hasPrice(stack)) continue;

            String nameRaw = ChatFormatting.stripFormatting(
                    stack.getHoverName().getString()).toLowerCase();
            if (nameRaw.contains("не актуален")) continue;

            boolean matches = false;

            if (currentTarget.getLoreKeywords() != null && !currentTarget.getLoreKeywords().isEmpty()) {
                matches = FuntimeUtil.checkLoreFullMatch(
                        FuntimeUtil.getLore(stack), currentTarget.getLoreKeywords());
            }
            if (!matches && currentTarget.isCheckByName()) {
                String targetName = ChatFormatting.stripFormatting(
                        currentTarget.getDisplayName()).toLowerCase();
                if (nameRaw.contains(targetName)) matches = true;
            }
            if (!matches && currentTarget.isCheckByItem()) {
                if (stack.getItem() == currentTarget.getDisplayStack().getItem()) matches = true;
            }

            if (!matches) continue;

            // getPricePerItem — цена за 1 шт, как в AutoSetupEngine
            int pricePerItem = FuntimeUtil.getPricePerItem(stack);
            if (pricePerItem > 0) foundPrices.add(pricePerItem);
        }
    }

    // ── Расчёт цены — логика наценки ─────────────────────────────────────────

    private void calculatePrice() {
        if (finishingCurrent) return;

        // цена за 1 шт — как AutoSetupEngine использует minPrice
        int pricePerOneBought = stackSize > 0 ? boughtPrice / stackSize : boughtPrice;
        int sellPricePerOne;

        if (foundPrices.isEmpty()) {
            sellPricePerOne = (int) (pricePerOneBought * (1.0 + margin / 100.0));
            log("§7[Продажа] Конкурентов нет, наценка §e+" + margin + "%");
        } else {
            // foundPrices.first() — минимальная цена за 1 шт, как AutoSetupEngine
            int minMarket = foundPrices.first();
            int undercut  = minMarket - ThreadLocalRandom.current().nextInt(100, 600);
            int minProfit = (int) (pricePerOneBought * (1.0 + margin / 100.0));
            sellPricePerOne = Math.max(undercut, minProfit);

            log("§7[Продажа] Мин: §e" + FuntimeUtil.formatPrice(minMarket)
                    + " §7→ §a" + FuntimeUtil.formatPrice(sellPricePerOne) + " §7за шт");
        }

        // /ah sell принимает цену за весь лот
        this.finalSellPrice  = sellPricePerOne * stackSize;
        this.randomSellDelay = ThreadLocalRandom.current().nextLong(1000, 3001);

        finishingCurrent = true;

        // Закрываем аукцион — как AutoSetupEngine.finishCurrent()
        if (mc.screen != null) mc.screen.onClose();

        // Переход перед постом
        transitioning = false;
        waitingToPost = true;
        postTimer.reset();
    }

    // ── Выставление на аукцион ────────────────────────────────────────────────

    private void postToAuction() {
        if (mc.player == null) { stop(); return; }

        mc.player.connection.sendCommand("ah sell " + finalSellPrice);

        log("§a[Продажа] §f" + currentTarget.getDisplayName()
                + " §7x" + stackSize
                + " за §e" + FuntimeUtil.formatPrice(finalSellPrice));

        AutoBuy.auctionRenderer.addPurchase(
                currentTarget.getDisplayStack(),
                currentTarget.getDisplayName(),
                stackSize > 0 ? boughtPrice / stackSize : boughtPrice,
                stackSize,
                boughtPrice,
                finalSellPrice
        );

        stop();

        // Возвращаемся на /ah и включаем AutoBuy — как AutoSetupEngine.finishAll()
        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            mc.execute(() -> {
                if (mc.player == null) return;
                mc.player.connection.sendCommand("ah");
                AutoBuy.balanceController.request();
                AutoBuy.setAutoBuyEnabled(true);
                AutoBuy.autoBuyEngine.resetState();
            });
        }).start();
    }

    // ── Вспомогательные ──────────────────────────────────────────────────────

    // Refresh — точная копия AutoSetupEngine.refresh()
    private void refresh(ContainerScreen screen) {
        for (Slot slot : screen.getMenu().slots) {
            String name = slot.getItem().getHoverName().getString().toLowerCase();
            if (name.contains("обновить") || name.contains("refresh")) {
                InvUtil.clickSlot(slot.index, 0, ClickType.PICKUP, false);
                break;
            }
        }
    }

    private int findItem(ItemTarget target) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            String name = ChatFormatting.stripFormatting(
                    stack.getHoverName().getString()).toLowerCase();

            boolean matches = false;
            if (target.getLoreKeywords() != null && !target.getLoreKeywords().isEmpty()) {
                matches = FuntimeUtil.checkLoreFullMatch(
                        FuntimeUtil.getLore(stack), target.getLoreKeywords());
            }
            if (!matches && target.isCheckByName()) {
                String targetName = ChatFormatting.stripFormatting(
                        target.getDisplayName()).toLowerCase();
                if (name.contains(targetName)) matches = true;
            }
            if (!matches && target.isCheckByItem()) {
                if (stack.getItem() == target.getDisplayStack().getItem()) matches = true;
            }
            if (matches) return i;
        }
        return -1;
    }

    private void stop() {
        this.selling          = false;
        this.waitingForAuction = false;
        this.transitioning    = false;
        this.finishingCurrent = false;
        this.waitingToPost    = false;
        this.foundPrices.clear();
    }

    public void onChat(String msg) {}

    private void log(String msg) { MessageSender.print(msg); }
}
