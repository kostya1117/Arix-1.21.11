package ru.arixcompany.features.module.modules.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BindSetting;
import ru.arixcompany.features.module.setting.implement.GroupSetting;
import ru.arixcompany.features.repos.FriendRepo;
import ru.arixcompany.utils.player.inventory.PlayerInventoryComponent;
import ru.arixcompany.utils.player.inventory.PlayerInventoryUtil;

import java.util.ArrayList;
import java.util.List;

public class ClickActions extends Module {

    public ClickActions() {
        super("ClickActions", Category.Player);
        init();
    }

    private final BindSetting friendBind = new BindSetting("Добавить друга");
    private final BindSetting elytraSetting = new BindSetting("Кнопка свапа");
    private final BindSetting fireworkSetting = new BindSetting("Кнопка фейерверка");

    private final List<KeyBind> keyBindings = new ArrayList<>();

    public void init() {
        keyBindings.add(new KeyBind(Items.ENDER_PEARL, new BindSetting("Эндер перл")));
        keyBindings.add(new KeyBind(Items.WIND_CHARGE, new BindSetting("Заряд ветра")));

        setup(friendBind,
                new GroupSetting("Элитры",
                        elytraSetting,
                        fireworkSetting
                )
        );
        for (KeyBind k : keyBindings) {
            setup(k.setting);
        }
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (e.isKeyDown(friendBind)
                && mc.hitResult instanceof EntityHitResult result
                && result.getEntity() instanceof Player player) {
            String name = player.getGameProfile().name();
            if (FriendRepo.isFriend(name)) FriendRepo.remove(name);
            else FriendRepo.add(name);
        }

        for (KeyBind bind : keyBindings) {
            if (e.isKeyDown(bind.setting())) {
                PlayerInventoryComponent.addTask( ()-> {
                    PlayerInventoryUtil.swapAndUse(bind.item());
                },1,1);
            }
        }

        if (e.isKeyDown(elytraSetting.getKey())) {
            Slot slot = PlayerInventoryUtil.chestPlate();
            if (slot != null) {
                PlayerInventoryComponent.addTask( ()-> {
                    PlayerInventoryUtil.moveItem(slot, 6, true);
                    PlayerInventoryUtil.closeScreen(true);
                },1,1);
            }
        } else if (e.isKeyDown(fireworkSetting.getKey()) && mc.player.isFallFlying()) {
            PlayerInventoryComponent.addTask( ()-> {
                PlayerInventoryUtil.swapAndUse(Items.FIREWORK_ROCKET);
            },1,1);
        }
    }

    public record KeyBind(Item item, BindSetting setting) {
    }
}
