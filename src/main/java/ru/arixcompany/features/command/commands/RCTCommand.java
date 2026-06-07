package ru.arixcompany.features.command.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import ru.arixcompany.features.command.AbstractCommand;
import ru.arixcompany.utils.MessageSender;
import ru.arixcompany.utils.player.NetworkUtil;

public class RCTCommand extends AbstractCommand {

    public RCTCommand() {
        super("rct", "Перезаходит на анархию (FunTime/SpookyTime)");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(this::executeRct);
    }

    private int executeRct(com.mojang.brigadier.context.CommandContext<ClientSuggestionProvider> context) {
        if (!NetworkUtil.isFunTime() && !NetworkUtil.isCopyTime()) {
            MessageSender.print(Component.literal("❌ RCT работает только на FunTime и SpookyTime!")
                    .withStyle(ChatFormatting.RED));
            return SINGLE_SUCCESS;
        }

        if (NetworkUtil.isOnPvP()) {
            MessageSender.print(Component.literal("❌ RCT не работает в PvP режиме!")
                    .withStyle(ChatFormatting.RED));
            return SINGLE_SUCCESS;
        }

        String anarchyNumber = NetworkUtil.getAnarchyNumberFromTabOverlay();

        if (anarchyNumber == null) {
            MessageSender.print(Component.literal("❌ Не удалось определить режим: вы не на FunTime/SpookyTime.")
                    .withStyle(ChatFormatting.RED));
            return SINGLE_SUCCESS;
        }

        if (anarchyNumber.equals("none")) {
            MessageSender.print(Component.literal("❌ Не удалось определить режим: анархия или гриф.")
                    .withStyle(ChatFormatting.RED));
            return SINGLE_SUCCESS;
        }

        final String finalAnarchy = anarchyNumber;

        new Thread(() -> {
            try {
                assert mc.player != null;
                mc.player.connection.sendCommand("hub");
                Thread.sleep(1500);
                if (!finalAnarchy.equals("none")) {
                    mc.player.connection.sendCommand("an" + finalAnarchy);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }).start();

        MessageSender.print(Component.literal("🔄 Перезаход...")
                .withStyle(ChatFormatting.YELLOW));
        return SINGLE_SUCCESS;
    }
}