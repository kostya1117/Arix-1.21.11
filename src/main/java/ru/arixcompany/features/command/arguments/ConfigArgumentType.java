package ru.arixcompany.features.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import ru.arixcompany.features.file.Directories;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConfigArgumentType implements ArgumentType<String> {
    private static final List<String> EXAMPLES = List.of("Test");

    public static ConfigArgumentType create() {
        return new ConfigArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String configName = reader.readString();
        if (!getConfigs().contains(configName)) {
            throw new DynamicCommandExceptionType(
                    name -> Component.literal("Конфигурации с именем " + name + " не существует")
            ).create(configName);
        }
        return configName;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> configs = getConfigs();
        return SharedSuggestionProvider.suggest(configs, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        File[] configFiles = Directories.configDirectory.listFiles();

        if (configFiles != null) {
            for (File configFile : configFiles) {
                if (configFile.isFile() && configFile.getName().endsWith(".json")) {
                    String configName = configFile.getName().replace(".json", "");
                    configs.add(configName);
                }
            }
        }

        return configs;
    }
}
