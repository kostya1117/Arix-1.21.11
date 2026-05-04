package ru.arixcompany.ui.alt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.util.Optional;
import java.util.UUID;

public class SessionUtil {

    public static void setSession(String username) {

        Minecraft mc = Minecraft.getInstance();

        UUID uuid = UUID.randomUUID();

        User newUser = new User(
                username,
                uuid,
                "0",
                Optional.empty(),
                Optional.empty()
        );

        mc.setUser(newUser);

        System.out.println("Logged as: " + username);
    }
}