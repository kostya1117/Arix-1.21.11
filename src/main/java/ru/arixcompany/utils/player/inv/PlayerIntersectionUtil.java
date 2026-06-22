package ru.arixcompany.utils.player.inv;

import lombok.experimental.UtilityClass;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.player.NetworkUtil;

@UtilityClass
public class PlayerIntersectionUtil implements IMinecraft {

    public void sendSequencedPacket(PredictiveAction packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;
        try (BlockStatePredictionHandler pendingUpdateManager = mc.level.blockStatePredictionHandler.startPredicting()) {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
        }
    }

    public void useItem(InteractionHand hand) {
        sendSequencedPacket(i -> new ServerboundUseItemPacket(hand, i, mc.player.getYRot(), mc.player.getXRot()));
    }

    public float getHealth(LivingEntity entity) {
        float hp = entity.getHealth() + entity.getAbsorptionAmount();
        if (entity instanceof Player player) {
            if (NetworkUtil.isFunTime() || NetworkUtil.isReallyWorld()) {
                Scoreboard scoreboard = player.level().getScoreboard();
                Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
                if (objective != null) {
                    try {
                        String scoreText = scoreboard.listPlayerScores(objective).stream()
                                .filter(e -> e.owner().equals(player.getScoreboardName()))
                                .findFirst()
                                .map(entry -> {
                                    NumberFormat numberFormat = objective.numberFormatOrDefault(null);
                                    if (numberFormat != null) {
                                        return entry.formatValue(numberFormat).getString().replaceAll("§[0-9a-fk-or]", "");
                                    }
                                    return String.valueOf(entry.value());
                                })
                                .orElse(null);
                        if (scoreText != null) {
                            hp = Float.parseFloat(scoreText);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return Mth.clamp(hp, 0, entity.getMaxHealth());
    }
}
