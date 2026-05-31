package ru.arixcompany.features.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.server.permissions.PermissionSet;
import ru.arixcompany.features.command.commands.ConfigCommand;
import ru.arixcompany.features.command.commands.FriendCommand;
import ru.arixcompany.features.command.commands.WayPointCommand;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.world.EventChat;
import ru.arixcompany.utils.IMinecraft;

import java.util.List;

@Getter
public class CommandRepo implements IMinecraft {
    public static String COMMAND_TARGET = ".";
    private final List<AbstractCommand> commandList = Lists.newArrayList();

    private final CommandDispatcher<ClientSuggestionProvider> commandDispatcher = new CommandDispatcher<>();
    private final ClientSuggestionProvider source = new ClientSuggestionProvider(null, mc, PermissionSet.ALL_PERMISSIONS);

    public CommandRepo() {
        EventRepo.register(this);
    }

    public void setup() {
        System.out.println("[CommandRepo] Setting up commands...");
        List.of(
                new ConfigCommand(),
                new FriendCommand(),
                new WayPointCommand()
        ).forEach(command -> {
            System.out.println("[CommandRepo] Registering command: " + command.getName());
            commandList.add(command);
            LiteralArgumentBuilder<ClientSuggestionProvider> builder = LiteralArgumentBuilder.literal(command.getName());
            command.build(builder);
            commandDispatcher.register(builder);
        });
        System.out.println("[CommandRepo] Commands setup complete. Total: " + commandList.size());
    }

    @EventHandler
    private void onChat(EventChat event) {
        String message = event.getMessage();
        if (message.startsWith(COMMAND_TARGET)) {
            message = message.substring(1);
            if (message.isEmpty()) {
                return;
            }
            event.cancel();
            try {
                commandDispatcher.execute(message, source);
            } catch (CommandSyntaxException ignored) {
            }
        }
    }
}