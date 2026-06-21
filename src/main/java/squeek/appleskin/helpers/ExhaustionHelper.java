package squeek.appleskin.helpers;

import net.minecraft.world.entity.player.Player;

public class ExhaustionHelper {
    public static float getExhaustion(Player player) {
        return player.getFoodData().getExhaustionLevel();
    }

    public static void setExhaustion(Player player, float value) {
        player.getFoodData().setExhaustionLevel(value);
    }
}
