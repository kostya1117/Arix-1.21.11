package ru.arixcompany.utils.player.inventory;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import ru.arixcompany.utils.IMinecraft;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class PlayerIntersectionUtil implements IMinecraft {

    public static void sendSequencedPacket(PredictiveAction packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;
        try (BlockStatePredictionHandler pendingUpdateManager = mc.level.blockStatePredictionHandler.startPredicting()) {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
        }
    }

    public void sendPacketWithOutEvent(Packet<?> packet) {
        mc.getConnection().getConnection().send(packet, null);
    }

    public InputConstants.Type getKeyType(int key) {
        return key < 8 ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
    }

    public Stream<Entity> streamEntities() {
        return StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false);
    }

    public boolean isChat(Screen screen) {return screen instanceof ChatScreen;}
    public boolean nullCheck() {return mc.player == null || mc.level == null;}

    public void useItem(InteractionHand hand) {
        sendSequencedPacket(i -> new ServerboundUseItemPacket(hand, i, mc.player.getYRot(), mc.player.getXRot()));
    }
}