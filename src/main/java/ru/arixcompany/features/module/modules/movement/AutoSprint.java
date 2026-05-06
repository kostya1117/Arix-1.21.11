package ru.arixcompany.features.module.modules.movement;

import net.minecraft.world.effect.MobEffects;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventTick;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;

public class AutoSprint extends Module {

    public AutoSprint() {
        super("AutoSprint", Category.Movement);
        setup(settings);
    }

    public static int tickStop;

    ListSetting settings = new ListSetting("Игнорировать эффект")
            .value("Замедление", "Слепота");

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null) return;

        boolean horizontal = mc.player.horizontalCollision && !mc.player.minorHorizontalCollision;
        boolean sneaking = mc.player.isShiftKeyDown() && !mc.player.isSwimming();

        if (!(settings.isSelected("Слепота") && mc.player.hasEffect(MobEffects.BLINDNESS))
                && !(settings.isSelected("Замедление") && mc.player.hasEffect(MobEffects.SLOWNESS))
                && tickStop > 0 || sneaking) {
            mc.player.setSprinting(false);
        } else if (!horizontal && mc.player.zza > 0) {
            mc.player.setSprinting(true);
        }

        tickStop--;
    }
}
