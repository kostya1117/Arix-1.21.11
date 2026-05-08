package ru.arixcompany.features.module.modules.player;

import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventPush;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.ListSetting;

public class NoPush extends Module {
    public NoPush() {
        super("NoPush", Category.Player);
    }

    ListSetting cancelPush = new ListSetting("Отменить от")
            .value("Блоков","Игроков","Жидкостей");

    @EventHandler
    public void onPushPlayer(EventPush eventPush) {
        if (eventPush.getPushEnum() == EventPush.PushEnum.Blocks && cancelPush.isSelected("Блоков")) {
            eventPush.cancel();
        } if (eventPush.getPushEnum() == EventPush.PushEnum.Players && cancelPush.isSelected("Игроков")) {
            eventPush.cancel();
        } if (eventPush.getPushEnum() == EventPush.PushEnum.Fluids && cancelPush.isSelected("Жидкостей")) {
            eventPush.cancel();
        }
    }
}
