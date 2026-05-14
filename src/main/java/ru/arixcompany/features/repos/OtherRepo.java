package ru.arixcompany.features.repos;

import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import org.jetbrains.annotations.NotNull;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.Timer;

public class OtherRepo implements IMinecraft {
    public static int serverSideSlot;
    public final Timer switchTimer = new Timer();


    public OtherRepo() {
        EventRepo.register(this);
    }

    @EventHandler
    public void onSyncWithServer(EventPacket event) {
        if (!event.isSend()) return;
        if (event.getPacket() instanceof ClientboundSetHeldSlotPacket slot) {
            switchTimer.reset();
            serverSideSlot = slot.slot();
        }
    }

    @EventHandler
    public void onPacketReceive(EventPacket event) {
        if (event.getPacket() instanceof ClientboundSetHeldSlotPacket slot) {
            switchTimer.reset();
            serverSideSlot = slot.slot();
        }
    }
}
