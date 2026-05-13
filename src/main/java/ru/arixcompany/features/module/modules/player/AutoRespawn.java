package ru.arixcompany.features.module.modules.player;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.DeathScreen;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.world.EventUpdate;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;
import ru.arixcompany.features.repos.WayPointRepo;
import ru.arixcompany.utils.math.Timer;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", Category.Player);
        setup(deystviya);
    }
    ListSetting deystviya = new ListSetting("Действия")
            .value("Сохранить корды",
                    "Вейпоинт");

    private boolean flag;
    private int waypointCount = 0;
    private final Timer timer = new Timer();

    @EventHandler
    public void onUpdate(EventUpdate e) {
        if (fullNullCheck()) return;

        if (timer.isReached(2100)) {
            timer.reset();
        }

        if (mc.screen instanceof DeathScreen) {
            if (flag){
                waypointCount += 1;
                if (deystviya.isSelected("Сохранить корды"))
                    print(ChatFormatting.GOLD + "[PlayerDeath] " + ChatFormatting.YELLOW + (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ());
                if (deystviya.isSelected("Вейпоинт")) {
                    WayPointRepo.WayPoint wp = new WayPointRepo.WayPoint((int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ(), "Смерть №" + waypointCount, (mc.isSingleplayer() ? "SinglePlayer" : mc.getConnection().getServerData().ip));
                    WayPointRepo.addWayPoint(wp);
                }
                mc.player.respawn();
                mc.setScreen(null);

                flag = false;
            }
        } else {
            flag = true;
        }
    }
}
