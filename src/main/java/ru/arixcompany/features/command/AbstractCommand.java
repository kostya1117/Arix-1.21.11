package ru.arixcompany.features.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import lombok.Getter;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.NotNull;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.MessageSender;

@Getter
public abstract class AbstractCommand implements IMinecraft {
    public String name, shortDesc;
    protected final int SINGLE_SUCCESS = Command.SINGLE_SUCCESS;

    public AbstractCommand(String name, String shortDesc) {
        this.name = name;
        this.shortDesc = shortDesc;
    }

    public abstract void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder);

    public static @NotNull LiteralArgumentBuilder<SharedSuggestionProvider> literal(final String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    protected static <T> @NotNull RequiredArgumentBuilder<SharedSuggestionProvider, T> argument(final String name, final ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public void sendMessage(String msg) {
        MessageSender.print(msg);
    }
}