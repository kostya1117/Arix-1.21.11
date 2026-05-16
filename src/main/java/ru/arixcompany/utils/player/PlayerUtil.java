package ru.arixcompany.utils.player;


import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.util.Mth;
import ru.arixcompany.utils.IMinecraft;

public class PlayerUtil implements IMinecraft {
    public static float calculateCorrectYawOffset(float yaw) {
        double xDiff = mc.player.getX() - mc.player.xOld;
        double zDiff = mc.player.getZ() - mc.player.zOld;
        float distSquared = (float)(xDiff * xDiff + zDiff * zDiff);
        float renderYawOffset = mc.player.yBodyRotO;
        float offset = renderYawOffset;
        if (distSquared > 0.0025000002F) {
            offset = (float) Mth.atan2(zDiff, xDiff) * 180.0F / (float) Math.PI - 90.0F;
        }

        if (mc.player != null && mc.player.attackAnim > 0.0F) {
            offset = yaw;
        }

        float yawOffsetDiff = Mth.wrapDegrees(yaw - (renderYawOffset + Mth.wrapDegrees(offset - renderYawOffset) * 0.3F));
        yawOffsetDiff = Mth.clamp(yawOffsetDiff, -32.0F, 32.0F);
        renderYawOffset = yaw - yawOffsetDiff;
        if (yawOffsetDiff * yawOffsetDiff > 2500.0F) {
            renderYawOffset += yawOffsetDiff * 0.2F;
        }

        return renderYawOffset;
    }

    public static void sendSequencedPacket(PredictiveAction packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;
        try (BlockStatePredictionHandler pendingUpdateManager = mc.level.blockStatePredictionHandler.startPredicting();) {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
        }
    }
}
