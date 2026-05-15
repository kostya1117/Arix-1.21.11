package ru.arixcompany.features.module.modules.player;

import net.minecraft.world.item.Items;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.utils.math.Timer;
import ru.arixcompany.utils.player.UseHandler;

public class ClickActions extends Module {
    private final UseHandler useHandler = new UseHandler();

    private SelectSetting mode = new SelectSetting("Режим")
            .value("Обычный",
                    "Незаметный");
    private ValueSetting swapDelay = new ValueSetting("Задержка свапа")
            .range(30,300)
            .setValue(100)
            .setStep(1);
    private final BindSetting pearl = new BindSetting("Эндер жемчуг");

    public ClickActions() {
        super("ClickActions", Category.Player);
        setup(mode,pearl,swapDelay);
    }

    @EventHandler
    public void onKey(EventKey event) {
        if (event.getAction() != 1 || mc.screen != null) return;

        int key = event.getKey();

        if (key == pearl.getKey()) {
            usePearl();
        }
    }

    private void usePearl() {
            useHandler
                    .setMode(mode.isSelected("Незаметный") ? UseHandler.UseMode.SILENT : UseHandler.UseMode.NORMAL)
                    .setSwapDelay(swapDelay.getInt())
                    .use(Items.ENDER_PEARL);
    }
}
