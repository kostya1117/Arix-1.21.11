package ru.arixcompany.features.module.modules.player;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.GroupSetting;
import ru.arixcompany.utils.player.inventory.PlayerInventoryComponent;
import ru.arixcompany.utils.player.inventory.PlayerInventoryUtil;

import java.util.ArrayList;
import java.util.List;

public class ServerAssist extends Module {
    public ServerAssist() {
        super("ServerAssist", Category.Player);
        init();
    }

    private final List<KeyBind> keyBindings = new ArrayList<>();

    public void init() {
        //фт
        keyBindings.add(new KeyBind(Items.ENDER_EYE, new BindSetting("Дезик"), Server.Funtime));
        keyBindings.add(new KeyBind(Items.NETHERITE_SCRAP, new BindSetting("Трапка"), Server.Funtime));

        //хв
        keyBindings.add(new KeyBind(Items.NETHER_STAR, new BindSetting("Стан"), Server.Holik));
        keyBindings.add(new KeyBind(Items.PRISMARINE_SHARD, new BindSetting("Взрывная Трапка"), Server.Holik));
        keyBindings.add(new KeyBind(Items.SNOWBALL, new BindSetting("Снежок"), Server.Holik));

        for (Server server : Server.values()) {
            List<BindSetting> serverSettings = new ArrayList<>();
            for (KeyBind k : keyBindings) {
                if (k.server == server) {
                    serverSettings.add(k.setting);
                }
            }

            setup(new GroupSetting(server.getName(), serverSettings.toArray(new BindSetting[0])));
        }
    }

    @EventHandler
    public void onKey(EventKey e) {
        for (KeyBind bind : keyBindings) {
            if (e.isKeyDown(bind.setting())) {
                PlayerInventoryComponent.addTask(() -> {
                    PlayerInventoryUtil.swapAndUse(bind.item());
                });
            }
        }
    }

    public record KeyBind(Item item, BindSetting setting, Server server) {}

    @AllArgsConstructor
    @Getter
    private enum Server {
        Funtime("Фантайм"),
        Rilik("Рилик"),
        Holik("Холик");

        private final String name;
    }
}