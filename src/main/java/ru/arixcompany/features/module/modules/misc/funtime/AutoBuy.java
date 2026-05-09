package ru.arixcompany.features.module.modules.misc.funtime;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventMouseScroll;
import ru.arixcompany.features.event.render.EventScreen;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.*;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemsRegistry;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.MessageSender;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.math.Timer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutoBuy extends Module {

    public static final BooleanSetting autoBuyAfterSetup = new BooleanSetting("Включить после сетапа");
    private final ValueSetting discountPercent =
            new ValueSetting("Скидка покупки (%)").setValue(50).range(10, 90).step(1);

    public static final BooleanSetting autoSell = new BooleanSetting("Авто-продажа");
    private final ValueSetting sellMargin =
            new ValueSetting("Наценка (%)").setValue(10).range(5, 300).step(5)
                    .visible(autoSell::isValue);

    private final BooleanSetting autoSetupScheduler =
            new BooleanSetting("Авто‑рестарт сетапа");
    private final ValueSetting autoSetupInterval =
            new ValueSetting("Интервал (мин)")
                    .setValue(15)
                    .range(10, 60)
                    .step(5)
                    .visible(autoSetupScheduler::isValue);

    @Setter
    public static boolean autoBuyEnabled = false;
    public boolean autoSetupEnabled = false;

    private ContainerScreen lastScreen;

    private final Map<String, ItemTarget> targets = new LinkedHashMap<>();

    private final Timer clickCooldown = new Timer();
    private final Timer scanWatch = new Timer();
    private final Timer updateWatch = new Timer();
    private final Timer autoSetupTimer = new Timer();

    @Getter
    public static final BalanceController balanceController = new BalanceController();
    public static final AuctionRenderer auctionRenderer = new AuctionRenderer();
    public static AutoBuyEngine autoBuyEngine;
    public AutoSetupEngine autoSetupEngine;
    public static final AutoSellEngine autoSellEngine = new AutoSellEngine();

    public AutoBuy() {
        super("AutoBuy", Category.Misc);
        setup(autoBuyAfterSetup,discountPercent,
                autoSell,sellMargin,
                autoSetupScheduler,autoSetupInterval);

        ItemsRegistry.register(targets);

        autoBuyEngine = new AutoBuyEngine(
                targets,
                clickCooldown,
                scanWatch,
                updateWatch
        );

        autoSetupEngine = new AutoSetupEngine(
                new ArrayList<>(targets.values()),
                this
        );
    }

    @EventHandler
    public void onTick(EventTick e) {

        if (mc.screen instanceof ContainerScreen screen) {

            String title = screen.getTitle().getString().toLowerCase();

            if (title.contains("аукцион") || title.contains("auction")) {

                if (screen != lastScreen) {
                    lastScreen = screen;
                    auctionRenderer.addButtons(
                            screen,
                            autoBuyEnabled,
                            autoSetupEnabled,
                            () -> {

                                autoBuyEnabled = !autoBuyEnabled;

                                if (autoBuyEnabled) {
                                    balanceController.request();
                                } else {
                                    autoBuyEngine.resetState();
                                }
                            },
                            () -> {
                                if (!autoSetupEngine.isRunning()) {
                                    autoSetupEngine.start();
                                    autoSetupEnabled = true;
                                } else {
                                    autoSetupEngine.stop();
                                    autoSetupEnabled = false;
                                }
                            }
                    );
                }

                autoBuyEngine.setCurrentAuctionScreen(screen);
            }

        } else {
            lastScreen = null;
            autoBuyEngine.setCurrentAuctionScreen(null);
        }

        autoSellEngine.tick();
        autoBuyEngine.setClickDelay((int) getClickDelay());

        if (autoBuyEnabled && !autoSellEngine.isSelling()) {
            autoBuyEngine.tick((int) getUpdateDelay());
        }

        autoSetupEngine.tick(
                (int) getSearchTime(),
                (int) getUpdateDelay(),
                (int) discountPercent.getValue()
        );

        if (autoSetupScheduler.isValue()) {
            // Интервал от 10 до 60 минут
            long intervalMs = (long) autoSetupInterval.getValue() * 60_000L;

            if (autoSetupTimer.finished(intervalMs)) {
                MessageSender.print("§e Время обновлять цены...");

                // 1. Выключаем автобай
                autoBuyEnabled = false;
                autoBuyEngine.resetState();

                // 2. Включаем сетап
                if (!autoSetupEngine.isRunning()) {
                    autoSetupEngine.start();
                    autoSetupEnabled = true;
                }

                autoSetupTimer.reset();
            }
        }
    }


    @EventHandler
    public void onPacket(EventPacket e) {
        if (!(e.getPacket() instanceof ClientboundSystemChatPacket packet)) return;
        String msg = packet.content().getString().toLowerCase();

        if (msg.contains("вы успешно купили")) {
            autoBuyEngine.confirmPurchase();
            ItemTarget target = autoBuyEngine.getLastTarget();

            if (target != null) {
                int count = autoBuyEngine.getLastBoughtCount();
                int totalBuyPrice = autoBuyEngine.getLastTotalPrice();
                int perOne = totalBuyPrice / (count > 0 ? count : 1);

                if (autoSell.isValue()) {
                    // Это запускает процесс: ищет предмет в инвентаре и начинает цепочку действий
                    autoSellEngine.processSell(target, totalBuyPrice, (int) sellMargin.getValue());
                } else {
                    auctionRenderer.addPurchase(target.getDisplayStack(), target.getDisplayName(),
                            perOne, count, totalBuyPrice, 0);
                }
            }
            balanceController.request();
        }

        if (msg.contains("не хватает")
                || msg.contains("уже купили")
                || msg.contains("уже куплен")
                || msg.contains("не удалось купить")
                || msg.contains("лот недоступен")
                || msg.contains("этот товар уже")) {

            autoBuyEngine.failAndReopen();
            return;
        }

        balanceController.handlePacket(packet);
        autoBuyEngine.setCurrentBalance(balanceController.getBalance());
        autoSellEngine.onChat(msg);
    }

    @EventHandler
    public void onMouseScroll(EventMouseScroll e) {
        if (mc.screen instanceof ContainerScreen screen) {
            String title = screen.getTitle().getString().toLowerCase();

            if (title.contains("аукцион") || title.contains("auction")) {
                auctionRenderer.handleScroll(
                        e.getDeltaY(),
                        e.getMouseX(),
                        e.getMouseY()
                );
            }
        }
    }

    @EventHandler
    public void onRender2D(EventScreen e) {
        if (mc.screen instanceof ContainerScreen screen) {

            String title = screen.getTitle().getString().toLowerCase();

            if (title.contains("аукцион") || title.contains("auction")) {

                auctionRenderer.render(
                        e.getGuiGraphics(),
                        screen,
                        targets
                );
            }
        }
    }

    public void setPriceForItem(String itemId, int price) {
        ItemTarget target = targets.get(itemId);
        if (target != null) {
            target.setBuyPrice(price);
        }
    }

    public static float getUpdateDelay() {
        float min = 750;
        float max = 2000;
        return MathUtils.getSmartRandom(min,max);
    }

    public static float getClickDelay() {
        float min = 50;
        float max = 500;
        return MathUtils.getSmartRandom(min,max);
    }
    public static float getSearchTime() {
        float min = 5;
        float max = 20;

        return MathUtils.getSmartRandom(min,max);
    }
}

