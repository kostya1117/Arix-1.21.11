package ru.arixcompany.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import ru.arixcompany.Arix;

import java.util.Arrays;
import java.util.stream.Stream;

import static ru.arixcompany.utils.render.ColorUtil.interpolateColor;

public class MessageSender implements IMinecraft {
    private static final String RESET = "\u001B[0m";
    private static final String BLACK = "\u001B[30m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";
    private static final String GRAY = "\u001B[90m";
    private static final String BRIGHT_RED = "\u001B[91m";
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_PURPLE = "\u001B[95m";
    private static final String BRIGHT_CYAN = "\u001B[96m";
    private static final String BRIGHT_WHITE = "\u001B[97m";
    private static final String PREFIX = "[Debug]";

    static int[] getGradientColors() {
        return new int[]{
                Arix.getInstance().getCurrentTheme().getMain().getRGB(),
                Arix.getInstance().getCurrentTheme().getMain().getRGB()
        };
    }

    public static MutableComponent gradientText(String text, int startRGB, int endRGB) {
        MutableComponent result = Component.literal("");
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float progress = (length == 1) ? 0f : (float) i / (length - 1);
            int rgb = interpolateColor(startRGB, endRGB, progress);
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }

        return result;
    }

    static MutableComponent getPrefix(String string) {
        int[] colors = getGradientColors();

        MutableComponent gradientPart = gradientText(string, colors[0], colors[1]);

        return Component.empty()
                .append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY))
                .append(gradientPart)
                .append(Component.literal(") -> ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void print(Component... components) {
        MutableComponent component = Component.literal("").append(getPrefix("Arix").append(Component.literal(" ")));
        Arrays.stream(components).forEach(component::append);

        if (mc.player != null) {
            mc.gui.getChat().addMessage(component);
        }
    }

    public static void print(String message, ChatFormatting color) {
        Stream.of(message.split("\n")).forEach(line -> {
            MutableComponent component = Component.literal(line.replace("\t", "   "))
                    .setStyle(Style.EMPTY.withColor(color));
            print(component);
        });
    }

    public static void print(String msg) {
        print(msg, ChatFormatting.GRAY);
    }

    public static void sendOverlayMessage(Component text) {
        if (mc != null && mc.gui != null) {
            mc.gui.setOverlayMessage(text, false);
        }
    }

    public static void log(String message) {
        System.out.println(GRAY + PREFIX + message + RESET);
    }

    public static void success(String message) {
        System.out.println(BRIGHT_GREEN + PREFIX + "✓ " + message + RESET);
    }

    public static void error(String message) {
        System.out.println(BRIGHT_RED + PREFIX + "✗ " + message + RESET);
    }

    public static void warn(String message) {
        System.out.println(BRIGHT_YELLOW + PREFIX + "⚠ " + message + RESET);
    }

    public static void info(String message) {
        System.out.println(BRIGHT_CYAN + PREFIX + "ℹ " + message + RESET);
    }

    public static void debug(String message) {
        System.out.println(PURPLE + PREFIX + "[DEBUG] " + message + RESET);
    }

    public static void log(String message, LogLevel level) {
        switch (level) {
            case INFO -> info(message);
            case SUCCESS -> success(message);
            case WARNING -> warn(message);
            case ERROR -> error(message);
            case DEBUG -> debug(message);
            default -> log(message);
        }
    }

    public static void printSuccess(String message) {
        System.out.println(BRIGHT_GREEN + message + RESET);
    }

    public static void printError(String message) {
        System.out.println(BRIGHT_RED + message + RESET);
    }

    public static void newLine() {
        System.out.println();
    }

    public enum LogLevel {
        INFO,
        SUCCESS,
        WARNING,
        ERROR,
        DEBUG
    }
}
