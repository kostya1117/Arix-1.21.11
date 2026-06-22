package ru.arixcompany.utils.player.inv;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class CooldownHelper {

    public static int getRemainingTicks(Player player, ItemStack stack) {
        if (player == null || stack.isEmpty()) return 0;

        ItemCooldowns manager = player.getCooldowns();
        float progress = manager.getCooldownPercent(stack, 0f);
        if (progress >= 1.0f) return 0;

        Identifier group = manager.getCooldownGroup(stack);
        Map<Identifier, ItemCooldowns.CooldownInstance> entries = manager.cooldowns;
        ItemCooldowns.CooldownInstance rawEntry = entries.get(group);
        if (rawEntry == null) return 0;

        int total = rawEntry.endTime() - rawEntry.startTime();
        return Math.max(0, Math.round(total * (1.0f - progress)));
    }

    public static int getTotalTicks(Player player, ItemStack stack) {
        if (player == null || stack.isEmpty()) return 0;

        ItemCooldowns manager = player.getCooldowns();
        Identifier group = manager.getCooldownGroup(stack);
        Map<Identifier, ItemCooldowns.CooldownInstance> entries = manager.cooldowns;
        ItemCooldowns.CooldownInstance rawEntry = entries.get(group);
        if (rawEntry == null) return 0;

        return Math.max(0, rawEntry.endTime() - rawEntry.startTime());
    }

    public static float getRemainingSeconds(Player player, ItemStack stack, float tickDelta) {
        int ticks = getRemainingTicks(player, stack);
        return Math.max(0.0f, (ticks - tickDelta) / 20.0f);
    }

    public static float getRemainingSeconds(Player player, ItemStack stack) {
        return getRemainingTicks(player, stack) / 20.0f;
    }

    public static float getProgress(Player player, ItemStack stack) {
        int total = getTotalTicks(player, stack);
        if (total <= 0) return 0.0f;
        return (float) getRemainingTicks(player, stack) / total;
    }
}
