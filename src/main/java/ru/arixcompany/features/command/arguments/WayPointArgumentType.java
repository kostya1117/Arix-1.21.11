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
import ru.arixcompany.features.repos.WayPointRepo;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class WayPointArgumentType implements ArgumentType<WayPointRepo.WayPoint> {
    private static final Collection<String> EXAMPLES = WayPointRepo.getWayPoints().stream()
            .map(WayPointRepo.WayPoint::getName)
            .limit(5)
            .toList();

    public static WayPointArgumentType create() {
        return new WayPointArgumentType();
    }

    @Override
    public WayPointRepo.WayPoint parse(StringReader reader) throws CommandSyntaxException {
        WayPointRepo.WayPoint wp = WayPointRepo.getWayPointByName(reader.readString());

        if (wp == null) throw new DynamicCommandExceptionType(
                name -> Component.literal("Вейпоинта " + name.toString() + " не существует")
        ).create(reader.readString());

        return wp;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(WayPointRepo.getWayPoints().stream().map(WayPointRepo.WayPoint::getName), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}