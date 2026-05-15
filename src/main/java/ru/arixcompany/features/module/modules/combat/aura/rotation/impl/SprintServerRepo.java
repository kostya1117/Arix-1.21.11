package ru.arixcompany.features.module.modules.combat.aura.rotation.impl;

import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.module.modules.combat.aura.rotation.Component;


public class SprintServerRepo extends Component {
    public static boolean lockSprint, serverSprint;
    @EventHandler
    public void onPacketSend(EventPacket e) {
        if (e.getPacket() instanceof ServerboundPlayerCommandPacket c && e.isSend()) {
            if (c.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING || c.getAction() == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
                if (lockSprint) {
                    e.cancel();
                    return;
                }

                switch (c.getAction()) {
                    case START_SPRINTING -> serverSprint = true;
                    case STOP_SPRINTING -> serverSprint = false;
                }
            }
        }
    }
}
