package ru.arixcompany.utils.player.inventory;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import ru.arixcompany.utils.IMinecraft;

import java.util.Map;

@UtilityClass
public class CooldownHelper implements IMinecraft {

    public int getRemainingTicks(Player player, ItemStack stack) {
        if (player == null || stack.isEmpty()) return 0;

        ItemCooldowns manager = player.getCooldowns();

        Identifier group = manager.getCooldownGroup(stack);
        Map<Identifier, ItemCooldowns.CooldownInstance> entries = manager.cooldowns;
        ItemCooldowns.CooldownInstance rawEntry = entries.get(group);

        if (rawEntry == null) return 0;

        return Math.max(0, rawEntry.endTime() - rawEntry.startTime());
    }

    public float getRemainingSeconds(Player player, ItemStack stack, float tickDelta) {
        int ticks = getRemainingTicks(player, stack);
        return Math.max(0.0f, (ticks - tickDelta) / 20.0f);
    }

    public float getRemainingSeconds(Player player, ItemStack stack) {
        return getRemainingTicks(player, stack) / 20.0f;
    }

    public float getProgress(Player player, ItemStack stack) {
        int total = getRemainingTicks(player, stack);
        if (total <= 0) return 0.0f;
        return (float) getRemainingTicks(player, stack) / total;
    }
}