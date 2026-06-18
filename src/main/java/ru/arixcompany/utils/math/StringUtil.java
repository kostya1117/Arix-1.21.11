package ru.arixcompany.utils.math;

import com.mojang.blaze3d.platform.InputConstants;
import ru.arixcompany.utils.IMinecraft;

import java.util.Random;

public class StringUtil implements IMinecraft {
    public static String getBindName(int key) {
        if (key <= -100) {
            int btn = Math.abs(key) - 100;
            return switch (btn) {
                case 0 -> "LMB";
                case 1 -> "RMB";
                case 2 -> "MMB";
                case 3 -> "Mouse4";
                case 4 -> "Mouse5";
                case 5 -> "Mouse6";
                case 6 -> "Mouse7";
                case 7 -> "Mouse8";
                default -> "M" + btn;
            };
        }

        if (key == -1) return "N/A";

        try {
            InputConstants.Key isMouse = key < 8 ? InputConstants.Type.MOUSE.getOrCreate(key) : InputConstants.Type.KEYSYM.getOrCreate(key);
            String bindName = isMouse.getName()
                    .replace("key.keyboard.", "")
                    .replace("key.mouse.", "mouse ")
                    .replace(".", " ")
                    .toUpperCase();
            return shortenBindName(bindName);
        } catch (Exception e) {
            return "KEY " + key;
        }
    }

   private static String shortenBindName(String bindName) {
       return switch (bindName) {
           case "INSERT" -> "INS";
           case "PAGE DOWN" -> "P DOWN";
           case "PAGE UP" -> "P UP";
           case "PRINT SCREEN" -> "PR SC";
           case "NUMPAD 0" -> "NUM 0";
           case "NUMPAD 1" -> "NUM 1";
           case "NUMPAD 2" -> "NUM 2";
           case "NUMPAD 3" -> "NUM 3";
           case "NUMPAD 4" -> "NUM 4";
           case "NUMPAD 5" -> "NUM 5";
           case "NUMPAD 6" -> "NUM 6";
           case "NUMPAD 7" -> "NUM 7";
           case "NUMPAD 8" -> "NUM 8";
           case "NUMPAD 9" -> "NUM 9";
           case "ESCAPE" -> "ESC";
           case "BACKSPACE" -> "BACKSPC";
           case "TAB" -> "TAB";
           case "CAPS LOCK" -> "CAPS";
           case "LEFT SHIFT" -> "L SHIFT";
           case "RIGHT SHIFT" -> "R SHIFT";
           case "LEFT CONTROL" -> "L CTRL";
           case "RIGHT CONTROL" -> "R CTRL";
           case "LEFT ALT" -> "L ALT";
           case "RIGHT ALT" -> "R ALT";
           case "SPACE" -> "SPACE";
           case "ENTER" -> "ENTER";
           case "DELETE" -> "DEL";
           default -> bindName;
       };
   }

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    public static String generateString(int length) {
        Random random = new Random();
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    public static String formatTicks(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public static boolean isKeyDown(int keyCode) {
        return InputConstants.isKeyDown(mc.getWindow(), keyCode);
    }
}
