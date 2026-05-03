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
      if (bindName.equals("INSERT")) {
         return "INS";
      } else if (bindName.equals("PAGE DOWN")) {
         return "P DOWN";
      } else if (bindName.equals("PAGE UP")) {
         return "P UP";
      } else if (bindName.equals("PRINT SCREEN")) {
         return "PR SC";
      } else if (bindName.equals("NUMPAD 0")) {
         return "NUM 0";
      } else if (bindName.equals("NUMPAD 1")) {
         return "NUM 1";
      } else if (bindName.equals("NUMPAD 2")) {
         return "NUM 2";
      } else if (bindName.equals("NUMPAD 3")) {
         return "NUM 3";
      } else if (bindName.equals("NUMPAD 4")) {
         return "NUM 4";
      } else if (bindName.equals("NUMPAD 5")) {
         return "NUM 5";
      } else if (bindName.equals("NUMPAD 6")) {
         return "NUM 6";
      } else if (bindName.equals("NUMPAD 7")) {
         return "NUM 7";
      } else if (bindName.equals("NUMPAD 8")) {
         return "NUM 8";
      } else if (bindName.equals("NUMPAD 9")) {
         return "NUM 9";
      } else if (bindName.equals("ESCAPE")) {
         return "ESC";
      } else if (bindName.equals("BACKSPACE")) {
         return "BACKSPC";
      } else if (bindName.equals("TAB")) {
         return "TAB";
      } else if (bindName.equals("CAPS LOCK")) {
         return "CAPS";
      } else if (bindName.equals("LEFT SHIFT")) {
         return "L SHIFT";
      } else if (bindName.equals("RIGHT SHIFT")) {
         return "R SHIFT";
      } else if (bindName.equals("LEFT CONTROL")) {
         return "L CTRL";
      } else if (bindName.equals("RIGHT CONTROL")) {
         return "R CTRL";
      } else if (bindName.equals("LEFT ALT")) {
         return "L ALT";
      } else if (bindName.equals("RIGHT ALT")) {
         return "R ALT";
      } else if (bindName.equals("SPACE")) {
         return "SPACE";
      } else if (bindName.equals("ENTER")) {
         return "ENTER";
      } else if (bindName.equals("DELETE")) {
         return "DEL";
      }
      return bindName;
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
