package ru.arixcompany.features.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import ru.arixcompany.Arix;
import ru.arixcompany.features.command.AbstractCommand;
import ru.arixcompany.features.command.arguments.ConfigArgumentType;
import ru.arixcompany.features.file.Directories;
import ru.arixcompany.utils.MessageSender;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ConfigCommand extends AbstractCommand {

    public ConfigCommand() {
        super("cfg", "Управление конфигами клиента");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("save").then(argument("name", StringArgumentType.word()).executes(context -> {
            String name = context.getArgument("name", String.class);
            try {
                Arix.getInstance().getFileController().saveFile(name + ".json");

                MutableComponent message = Component.literal("✓ Конфиг ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" сохранён."));

                MessageSender.print(message);

            } catch (Exception e) {
                MessageSender.print(Component.literal("❌ Ошибка при сохранении!")
                        .withStyle(ChatFormatting.RED));
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("load").then(argument("name", ConfigArgumentType.create()).executes(context -> {
            String name = context.getArgument("name", String.class);
            File file = new File(Directories.directoryPathConfig, name + ".json");

            if (!file.exists()) {
                MutableComponent message = Component.literal("⚠ Конфиг ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" не найден."));

                MessageSender.print(message);
                return SINGLE_SUCCESS;
            }

            try {
                Arix.getInstance().getFileController().loadFile(name + ".json");

                MutableComponent message = Component.literal("✓ Конфиг ")
                        .withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(name).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" загружен."));

                MessageSender.print(message);

            } catch (Exception e) {
                MessageSender.print(Component.literal("❌ Ошибка при загрузке!")
                        .withStyle(ChatFormatting.RED));
            }
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("dir").executes(context -> {
            File configDir = Directories.configDirectory;
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            Util.getPlatform().openUri(configDir.toURI());

            return SINGLE_SUCCESS;
        }));

        builder.then(literal("list").executes(context -> {
            MessageSender.print(Component.literal("📃 Найденные конфиги:")
                    .withStyle(ChatFormatting.GOLD));

            for (String config : ConfigArgumentType.getConfigs()) {
                MutableComponent message = Component.literal("• ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.literal(config).withStyle(ChatFormatting.AQUA));

                MessageSender.print(message);
            }
            return SINGLE_SUCCESS;
        }));
    }
}