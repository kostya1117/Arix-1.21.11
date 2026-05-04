package ru.arixcompany.features.command.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import ru.arixcompany.features.command.AbstractCommand;
import ru.arixcompany.features.command.arguments.FriendArgumentType;
import ru.arixcompany.features.command.arguments.PlayerListEntryArgumentType;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.MessageSender;

import java.util.List;

public class FriendCommand extends AbstractCommand {

    public FriendCommand() {
        super("friend", "Управление списком друзей");
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {

        builder.then(literal("add")
                .then(argument("player", PlayerListEntryArgumentType.create())
                        .executes(context -> {
                            GameProfile profile = PlayerListEntryArgumentType.get(context).getProfile();
                            String name = profile.name();

                            if (FriendRepo.isFriend(name)) {
                                MessageSender.print(Component.literal("❌ ")
                                        .append(Component.literal(name).withStyle(ChatFormatting.RED))
                                        .append(Component.literal(" уже есть в списке друзей.")));
                            } else {
                                FriendRepo.add(name);
                                MessageSender.print(Component.literal("✅ ")
                                        .append(Component.literal(name).withStyle(ChatFormatting.GREEN))
                                        .append(Component.literal(" добавлен в друзья.")));
                            }

                            return SINGLE_SUCCESS;
                        }))
        );

        builder.then(literal("remove")
                .then(argument("player", FriendArgumentType.create())
                        .executes(context -> {
                            String name = context.getArgument("player", String.class);

                            if (FriendRepo.isFriend(name)) {
                                FriendRepo.remove(name);
                                MessageSender.print(Component.literal("🗑️ ")
                                        .append(Component.literal(name).withStyle(ChatFormatting.YELLOW))
                                        .append(Component.literal(" удалён из списка друзей.")));
                            } else {
                                MessageSender.print(Component.literal("❌ ")
                                        .append(Component.literal(name).withStyle(ChatFormatting.RED))
                                        .append(Component.literal(" не найден в списке друзей.")));
                            }

                            return SINGLE_SUCCESS;
                        }))
        );

        builder.then(literal("clear")
                .executes(context -> {
                    FriendRepo.clear();
                    MessageSender.print(Component.literal("🧹 Список друзей был очищен.")
                            .withStyle(ChatFormatting.YELLOW));
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("list")
                .executes(context -> {
                    List<FriendRepo.Friend> friends = FriendRepo.getFriends();
                    if (friends.isEmpty()) {
                        MessageSender.print(Component.literal("📭 У тебя нет друзей.")
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        MutableComponent message = Component.literal("📒 Друзья: ")
                                .withStyle(ChatFormatting.AQUA);

                        for (int i = 0; i < friends.size(); i++) {
                            FriendRepo.Friend f = friends.get(i);
                            message.append(Component.literal(f.getName()).withStyle(ChatFormatting.WHITE));
                            if (i < friends.size() - 1) {
                                message.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
                            }
                        }

                        MessageSender.print(message);
                    }
                    return SINGLE_SUCCESS;
                })
        );
    }
}