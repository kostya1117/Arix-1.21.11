package ru.arixcompany.features.repos;

import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventTotemPop;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.math.Timer;

import java.util.HashMap;

public class OtherRepo implements IMinecraft {
    public static int serverSideSlot;
    public final Timer switchTimer = new Timer();
    public HashMap<String, Integer> popList = new HashMap<>();

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
        if (event.isReceive()) {
            if (event.getPacket() instanceof ClientboundSetHeldSlotPacket slot) {
                switchTimer.reset();
                serverSideSlot = slot.slot();
            }

            if (event.getPacket() instanceof ClientboundEntityEventPacket pac) {
                if (pac.getEventId() == EntityEvent.PROTECTED_FROM_DEATH) {
                    Entity ent = pac.getEntity(mc.level);
                    if (!(ent instanceof Player)) return;
                    if (popList == null) {
                        popList = new HashMap<>();
                    }
                    if (popList.get(ent.getName().getString()) == null) {
                        popList.put(ent.getName().getString(), 1);
                    } else if (popList.get(ent.getName().getString()) != null) {
                        popList.put(ent.getName().getString(), popList.get(ent.getName().getString()) + 1);
                    }
                    EventRepo.call(new EventTotemPop((Player) ent, popList.get(ent.getName().getString())));
                }
            }
        }
    }
}
