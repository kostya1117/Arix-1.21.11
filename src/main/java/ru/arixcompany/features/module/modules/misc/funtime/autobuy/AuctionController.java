package ru.arixcompany.features.module.modules.misc.funtime.autobuy;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import ru.arixcompany.features.module.modules.misc.funtime.utils.FuntimeUtil;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.Timer;

public class AuctionController implements IMinecraft {
    private final Timer anarchyWatch = new Timer();
    private final Timer waitTimer = new Timer();

    @Getter
    private boolean waitingAfterAnarchy = false;

    public void tick(boolean autoBuyEnabled) {

        if (!autoBuyEnabled) return;

        if (waitingAfterAnarchy) {

            if (waitTimer.finished(11000)) {
                waitingAfterAnarchy = false;

                if (mc.player != null) {
                    mc.player.connection.sendCommand("ah");
                }
            }

            return;
        }
    }

    private void changeRandomAnarchy() {

        if (mc.player == null) return;

        int randomAnarchy = FuntimeUtil.getRandomAnarchy();

        mc.player.connection.sendCommand("an" + randomAnarchy);
    }

    public void reset() {
        anarchyWatch.reset();
        waitingAfterAnarchy = false;
    }

    public void addButtons(ContainerScreen screen,
                           boolean autoBuyEnabled,
                           boolean autoSetupEnabled,
                           Runnable toggleBuy,
                           Runnable toggleSetup) {

        int x = screen.leftPos + screen.imageWidth + 5;
        int y = screen.topPos + 10;

        Button autoBuyButton = Button.builder(
                Component.literal("AutoBuy: ")
                        .append(Component.literal(
                                        autoBuyEnabled ? "Вкл" : "Выкл")
                                .withStyle(autoBuyEnabled ?
                                        ChatFormatting.GREEN :
                                        ChatFormatting.RED)),
                btn -> toggleBuy.run()
        ).bounds(x, y, 100, 20).build();

        Button autoSetupButton = Button.builder(
                Component.literal("AutoSetup: ")
                        .append(Component.literal(
                                        autoSetupEnabled ? "Вкл" : "Выкл")
                                .withStyle(autoSetupEnabled ?
                                        ChatFormatting.GREEN :
                                        ChatFormatting.RED)),
                btn -> toggleSetup.run()
        ).bounds(x, y + 25, 100, 20).build();

        screen.addRenderableWidget(autoBuyButton);
        screen.addRenderableWidget(autoSetupButton);
    }
}