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
import ru.arixcompany.Arix;
import ru.arixcompany.features.module.Module;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ModuleArgumentType implements ArgumentType<Module > {
    private static final Collection<String> EXAMPLES = Arix.getInstance().getModuleRepo().getModules().stream()
            .map(Module::getName)
            .limit(5)
            .toList();

    public static ModuleArgumentType create() {
        return new ModuleArgumentType();
    }

    @Override
    public Module parse(StringReader reader) throws CommandSyntaxException {
        Module module = Arix.getInstance().getModuleRepo().getModuleByName(reader.readString());
        if (module == null) throw new DynamicCommandExceptionType(
                name -> Component.literal("Модуля " + name.toString() + " не существует")
        ).create(reader.readString());

        return module;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arix.getInstance().getModuleRepo().getModules().stream().map(Module::getName), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
