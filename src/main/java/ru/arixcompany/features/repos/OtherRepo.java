package ru.arixcompany.features.repos;

import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.TimerUtils;

public class OtherRepo implements IMinecraft {
    public static int serverSideSlot;
    public final TimerUtils switchTimer = new TimerUtils();

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
