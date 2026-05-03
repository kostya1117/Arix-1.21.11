package de.florianmichael.viamcp;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.protocol.ProtocolPipelineImpl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ViaProtocolStripper {

    private static final String TARGET =
            "com.viaversion.viaversion.protocols.v1_21_4to1_21_5.Protocol1_21_4To1_21_5";

    public static void removeTargetProtocol(UserConnection user) {
        try {
            ProtocolPipelineImpl pipeline = (ProtocolPipelineImpl) user.getProtocolInfo().getPipeline();

            Field protocolListField = ProtocolPipelineImpl.class.getDeclaredField("protocolList");
            Field reversedProtocolListField = ProtocolPipelineImpl.class.getDeclaredField("reversedProtocolList");
            Field protocolSetField = ProtocolPipelineImpl.class.getDeclaredField("protocolSet");
            Field baseProtocolsField = ProtocolPipelineImpl.class.getDeclaredField("baseProtocols");

            protocolListField.setAccessible(true);
            reversedProtocolListField.setAccessible(true);
            protocolSetField.setAccessible(true);
            baseProtocolsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<Protocol> protocolList = (List<Protocol>) protocolListField.get(pipeline);

            @SuppressWarnings("unchecked")
            List<Protocol> reversedProtocolList = (List<Protocol>) reversedProtocolListField.get(pipeline);

            @SuppressWarnings("unchecked")
            Set<Class<? extends Protocol>> protocolSet =
                    (Set<Class<? extends Protocol>>) protocolSetField.get(pipeline);

            int baseProtocols = baseProtocolsField.getInt(pipeline);

            boolean removed = protocolList.removeIf(p ->
                    p != null && p.getClass().getName().equals(TARGET)
            );

            if (!removed) {
                return;
            }

            protocolSet.removeIf(c -> c != null && c.getName().equals(TARGET));

            List<Protocol> newReversed = new ArrayList<>(protocolList.size());
            for (int i = 0; i < baseProtocols && i < protocolList.size(); i++) {
                newReversed.add(protocolList.get(i));
            }
            for (int i = protocolList.size() - 1; i >= baseProtocols; i--) {
                newReversed.add(protocolList.get(i));
            }

            reversedProtocolList.clear();
            reversedProtocolList.addAll(newReversed);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}