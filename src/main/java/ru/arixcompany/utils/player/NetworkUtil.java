package ru.arixcompany.utils.player;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.*;
import org.apache.commons.lang3.StringUtils;
import ru.arixcompany.utils.IMinecraft;

import java.net.SocketAddress;
import java.util.Locale;

@UtilityClass
public class NetworkUtil implements IMinecraft {

    public static boolean isServerContains(String searchString) {
        if (searchString == null || searchString.isEmpty()) {
            return false;
        } else if (!nullCheck() && mc.getConnection() != null) {
            String serverAddress = null;
            if (mc.getCurrentServer() != null) {
                serverAddress = mc.getCurrentServer().ip;
            }

            if ((serverAddress == null || serverAddress.isEmpty()) && mc.getConnection().getConnection() != null) {
                SocketAddress address = mc.getConnection().getConnection().getRemoteAddress();
                if (address != null) {
                    serverAddress = address.toString();
                    if (serverAddress.startsWith("/")) {
                        serverAddress = serverAddress.substring(1);
                    }
                }
            }

            if (mc.isLocalServer()) {
                serverAddress = "localhost";
            }

            return serverAddress != null && !serverAddress.isEmpty() ? serverAddress.toLowerCase().contains(searchString.toLowerCase()) : false;
        } else {
            return false;
        }
    }
    public static boolean isCopyTime() {
        return isServerContains(("CopyTime")) || isServerContains("SpookyTime") || isServerContains("Funsky");
    }
    public static boolean isFunTime() {return isServerContains("FunTime");}
    public static boolean isReallyWorld() {return isServerContains("ReallyWorld");}
    public static boolean isHolyWorld() {return isServerContains("HolyWorld");}
    public static boolean isVanilla() {return isServerContains("Vanilla");}

    public static boolean isOnPvP() {
        for (LerpingBossEvent event : mc.gui.getBossOverlay().events.values()) {
            String name = event.getName().getString().toLowerCase(Locale.ROOT);
            if (name.contains("pvp") || name.contains("пвп")) {
                return true;
            }
        }
        return false;
    }

    public static String getAnarchyNumberFromTabOverlay() {
        if (!isFunTime() && !isCopyTime()) return null;
        String anarchy = "none";

        PlayerTabOverlay tabOverlay = mc.gui.getTabList();
        Component header = tabOverlay.header;
        if (header == null) return anarchy;

        for (String line : header.getString().split("\n")) {
            String normalized = line.toLowerCase(Locale.ROOT)
                    .replaceAll("§.", "")
                    .trim();
            if (normalized.contains("режим: анархия-")) {
                anarchy = normalized.replace("режим: анархия-", "").trim();
                break;
            }
        }
        return anarchy;
    }

    public static String getWorldType() {
        return mc.level.dimension().identifier().getPath();
    }

    public static boolean nullCheck() {
        return mc.player == null || mc.level == null;
    }
}