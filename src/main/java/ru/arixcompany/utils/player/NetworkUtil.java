package ru.arixcompany.utils.player;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.world.scores.*;
import org.apache.commons.lang3.StringUtils;
import ru.arixcompany.utils.IMinecraft;

import java.net.SocketAddress;

@UtilityClass
public class NetworkUtil implements IMinecraft {
    @Setter
    public static boolean hasCT;
    @Setter
    @Getter
    public static int ctTime;

    public void tick() {
    }

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

    public static int getAnarchyMode() {
        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (isFunTime()) {
            if (objective != null) {
                String[] string = objective.getDisplayName().getString().split("-");
                if (string.length > 1) return Integer.parseInt(string[1]);
            }
        }
        if (isHolyWorld()) {
            for (PlayerScoreEntry scoreboardEntry : scoreboard.listPlayerScores(objective)) {
                String text = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(scoreboardEntry.owner()), scoreboardEntry.ownerName()).getString();
                if (!text.isEmpty()) {
                    String string = StringUtils.substringBetween(text, "#", " -◆-");
                    if (string != null && !string.isEmpty()) return Integer.parseInt(string.replace(" (1.20)", ""));
                }
            }
        }
        return -1;
    }

    public static String getWorldType() {
        return mc.level.dimension().identifier().getPath();
    }

    public static boolean nullCheck() {
        return mc.player == null || mc.level == null;
    }
}