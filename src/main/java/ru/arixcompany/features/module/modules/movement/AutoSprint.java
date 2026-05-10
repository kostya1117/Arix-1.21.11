package ru.arixcompany.features.module.modules.movement;

import lombok.Getter;
import lombok.Setter;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventMovementTick;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;

import javax.annotation.processing.SupportedSourceVersion;

public class AutoSprint extends Module {
    public AutoSprint() {
        super("AutoSprint", Category.Movement);
    }
//    @EventHandler
//    public void onUpdate(EventMovementTick event) {
//        if (mc.player == null || mc.level == null)
//            return;
//        mc.options.keySprint.setDown(true);
//    }
}
