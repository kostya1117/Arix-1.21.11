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
}
