package ru.arixcompany.features.module.modules.combat;

import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventDamage;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;

public class Velocity extends Module {
    public Velocity() {
        super("Velocity", Category.Combat);
    }

    @EventHandler
    public void onDamage(EventDamage event) {
        if (event.isThorns()) {
            event.cancel();
        }
    }
}
