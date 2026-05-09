package ru.arixcompany.features.module.modules.misc.funtime;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.*;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemTarget;
import ru.arixcompany.features.module.modules.misc.funtime.autobuy.items.ItemsRegistry;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.math.Timer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class AutoBuy extends Module {

    public static final BooleanSetting autoBuyAfterSetup = new BooleanSetting("Включить после сетапа");
    private final ValueSetting searchDuration =
            new ValueSetting("Время поиска (сек)").setValue(10).range(5, 60).step(1);

    private final ValueSetting updateDelay =
            new ValueSetting("Обновление (мс)").setValue(2000).range(1000, 5000).step(100);

    private final ValueSetting clickDelay =
            new ValueSetting("Задержка клика (мс)").setValue(200).range(50, 500).step(50);

    private final ValueSetting discountPercent =
            new ValueSetting("Скидка покупки (%)").setValue(50).range(10, 90).step(1);

    @Setter
    public static boolean autoBuyEnabled = false;
    public boolean autoSetupEnabled = false;

    private ContainerScreen lastScreen;

    private final Map<String, ItemTarget> targets = new LinkedHashMap<>();

    private final Timer clickCooldown = new Timer();
    private final Timer scanWatch = new Timer();
    private final Timer updateWatch = new Timer();

    @Getter
    public static final BalanceController balanceController = new BalanceController();
    public static final AuctionController auctionController = new AuctionController();
    public static AutoBuyEngine autoBuyEngine;
    public AutoSetupEngine autoSetupEngine;

    public AutoBuy() {
        super("AutoBuy", Category.Misc);
        setup(autoBuyAfterSetup,searchDuration, updateDelay, clickDelay, discountPercent);

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
                    auctionController.addButtons(
                            screen,
                            autoBuyEnabled,
                            autoSetupEnabled,
                            () -> {

                                autoBuyEnabled = !autoBuyEnabled;

                                if (autoBuyEnabled) {
                                    balanceController.request();
                                } else {
                                    auctionController.reset();
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

        auctionController.tick(
                autoBuyEnabled
        );

        autoBuyEngine.setClickDelay((int) clickDelay.getValue());

        if (autoBuyEnabled && !auctionController.isWaitingAfterAnarchy()) {
            autoBuyEngine.tick((int) updateDelay.getValue());
        }

        autoSetupEngine.tick(
                (int) searchDuration.getValue(),
                (int) updateDelay.getValue(),
                (int) discountPercent.getValue()
        );
    }

    @EventHandler
    public void onPacket(EventPacket e) {

        if (!(e.getPacket() instanceof ClientboundSystemChatPacket packet))
            return;

        String message = packet.content().getString().toLowerCase();

        if (message.contains("вы успешно купили")) {
            autoBuyEngine.confirmPurchase();
            balanceController.request();

            return;
        }

        if (message.contains("не хватает")
                || message.contains("уже купили")
                || message.contains("уже куплен")
                || message.contains("не удалось купить")
                || message.contains("лот недоступен")
                || message.contains("этот товар уже")) {

            autoBuyEngine.failAndReopen();
            return;
        }

        balanceController.handlePacket(packet);
        autoBuyEngine.setCurrentBalance(balanceController.getBalance());
    }

    public void setPriceForItem(String itemId, int price) {
        ItemTarget target = targets.get(itemId);
        if (target != null) {
            target.setBuyPrice(price);
        }
    }
}

