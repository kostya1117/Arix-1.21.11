package ru.arixcompany.features.module.modules.misc;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventPacket;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.repos.FriendRepo;

import java.util.Locale;

//public class AutoAccept extends Module {
//    public AutoAccept() {
//        super("AutoAccept", Category.Misc);
//        setup(onlyFriends);
//    }
//
//    public BooleanSetting onlyFriends = new BooleanSetting("Только друзья");
//
//    @EventHandler
//    public void onPacket(EventPacket event) {
//        if (mc.player == null || mc.level == null) return;
//        if (!event.isReceive()) return;
//
//        if (event.getPacket() instanceof ClientboundSystemChatPacket packet) {
//            String raw = packet.content().getString().toLowerCase(Locale.ROOT);
//            if (raw.contains("телепортироваться") || raw.contains("has requested teleport") || raw.contains("просит к вам телепортироваться")) {
//                if (onlyFriends.isValue()) {
//                    boolean yes = false;
//
//                    for (FriendRepo.Friend friend : FriendRepo.getFriends()) {
//                        if (raw.contains(friend.getName().toLowerCase(Locale.ROOT))) {
//                            yes = true;
//                            break;
//                        }
//                    }
//
//                    if (!yes) return;
//                }
//
//                mc.player.connection.sendCommand("tpaccept");
//            }
//        }
//    }
//}

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Objects;

public class AutoAccept extends Module {

    private String lastInviter = null;

    public BooleanSetting onlyFriends = new BooleanSetting("Только друзья");
    public BooleanSetting clanAccept = new BooleanSetting("Принимать");

    public AutoAccept() {
        super("AutoAccept", Category.Misc);
        setup(onlyFriends);
    }

    @EventHandler
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.level == null) return;
        if (!event.isReceive()) return;

        if (event.getPacket() instanceof ClientboundSystemChatPacket packet) {
            Component textMessage = packet.content();
            String stringMessage = textMessage.getString();

            if (stringMessage.contains("телепортироваться") || stringMessage.contains("tpaccept")) {
                if (onlyFriends.isValue()) {
                    if (FriendRepo.isFriend(solveName(stringMessage))) {
                        acceptTeleport();
                    }
                } else {
                    acceptTeleport();
                }
            }

            if (stringMessage.contains("приглашает Вас в клан") || stringMessage.contains("invites you to join the clan")) {
                lastInviter = solveName(stringMessage);
            }

            if (clanAccept.isValue() && stringMessage.contains("Вступить") || stringMessage.contains("Отклонить") || stringMessage.contains("Accept") || stringMessage.contains("Decline")) {
                if (onlyFriends.isValue()) {
                    if (lastInviter != null && FriendRepo.isFriend(lastInviter)) {
                        clickClanAcceptButton(textMessage);
                    }
                } else {
                    clickClanAcceptButton(textMessage);
                }

                lastInviter = null;
            }
        }
    }

    private void acceptTeleport() {
        mc.player.connection.sendCommand("tpaccept");
    }

    private void clickClanAcceptButton(Component text) {
        List<Component> components = getTextComponents(text);

        for (Component component : components) {
            Style style = component.getStyle();
            ClickEvent clickEvent = style.getClickEvent();
            String content = component.getString();

            if (clickEvent != null && (content.contains("Вступить") || content.contains("Принять") || content.contains("Accept"))) {
                String command = null;

                if (clickEvent.action() == ClickEvent.Action.RUN_COMMAND) {
                    command = ((ClickEvent.RunCommand) clickEvent).command();
                } else if (clickEvent.action() == ClickEvent.Action.SUGGEST_COMMAND) {
                    command = ((ClickEvent.SuggestCommand) clickEvent).command();
                }

                if (command != null) {
                    if (command.startsWith("/")) command = command.substring(1);
                    mc.player.connection.sendCommand(command);
                    return;
                }
            }
        }
    }

    private List<Component> getTextComponents(Component text) {
        List<Component> components = new ArrayList<>();
        components.add(text);
        for (Component sibling : text.getSiblings()) {
            components.addAll(getTextComponents(sibling));
        }
        return components;
    }

    private String solveName(String notSolved) {
        AtomicReference<String> mb = new AtomicReference<>("");
        Objects.requireNonNull(mc.getConnection()).getOnlinePlayers().forEach(entry -> {
            if (notSolved.contains(entry.getProfile().name())) {
                mb.set(entry.getProfile().name());
            }
        });
        return mb.get();
    }
}