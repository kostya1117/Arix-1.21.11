package ru.arixcompany.features.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import ru.arixcompany.features.repos.FriendRepo;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FriendArgumentType implements ArgumentType<String> {
    private static final List<String> EXAMPLES = FriendRepo.getFriends().stream().limit(5).map(FriendRepo.Friend::getName).toList();

    public static FriendArgumentType create() {
        return new FriendArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String friend = reader.readString();
        if (!FriendRepo.isFriend(friend)) {
            throw new DynamicCommandExceptionType(
                    name -> Component.literal("Друга с именем " + name.toString() + " не существует")
            ).create(friend);
        }

        return friend;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(FriendRepo.getFriends().stream().map(FriendRepo.Friend::getName), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
