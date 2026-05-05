package ru.arixcompany.features.module.modules.combat.aura.rotation.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventMouseLook;
import ru.arixcompany.features.event.player.EventRotation;
import ru.arixcompany.features.module.modules.combat.aura.IComponent;

public class FreeLookController extends IComponent {

    @Getter
    @Setter
    private static boolean active;
    @Getter
    @Setter
    private static float freeYaw, freePitch;

    @EventHandler
    public void onEvent(EventMouseLook event) {
        if (active) {
            rotateTowards(event.getYaw(), event.getPitch());
            event.cancel();
        }
    }

    @EventHandler
    public void onEvent(EventRotation event) {
        if (active) {
            event.setYaw(freeYaw);
            event.setPitch(freePitch);
        } else {
            freeYaw = event.getYaw();
            freePitch = event.getPitch();
        }
    }

    private void rotateTowards(double targetYaw, double targetPitch) {
        freePitch = Mth.clamp((float) (freePitch + targetPitch * 0.15D), -90.0F, 90.0F);
        freeYaw = (float) (freeYaw + targetYaw * 0.15D);
    }
}