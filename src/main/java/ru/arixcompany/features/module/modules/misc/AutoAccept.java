package ru.arixcompany.features.module.modules.misc;

import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.repos.FriendRepo;

import java.util.Locale;

public class AutoAccept extends Module {
    public AutoAccept() {
        super("AutoAccept", Category.Misc);
        setup(onlyFriends);
    }

    public BooleanSetting onlyFriends = new BooleanSetting("Только друзья");

    @EventHandler
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.level == null) return;
        if (!event.isReceive()) return;

        if (event.getPacket() instanceof ClientboundSystemChatPacket packet) {
            String raw = packet.content().getString().toLowerCase(Locale.ROOT);
            if (raw.contains("телепортироваться") || raw.contains("has requested teleport") || raw.contains("просит к вам телепортироваться")) {
                if (onlyFriends.isValue()) {
                    boolean yes = false;

                    for (FriendRepo.Friend friend : FriendRepo.getFriends()) {
                        if (raw.contains(friend.getName().toLowerCase(Locale.ROOT))) {
                            yes = true;
                            break;
                        }
                    }

                    if (!yes) return;
                }

                mc.player.connection.sendCommand("tpaccept");
            }
        }
    }
}