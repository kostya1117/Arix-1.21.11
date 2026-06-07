package ru.arixcompany.features.module.modules.render;

import net.minecraft.world.entity.Avatar;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.BooleanSetting;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.repos.FriendRepo;

public class CustomModels extends Module {
    public static final String RABBIT = "Crazy Rabbit";
    public static final String FREDDY = "Freddy Bear";
    public static final String TUNG = "тунг тунг сахур";

    public final SelectSetting models = new SelectSetting(
            "Моделька"
    ).value(RABBIT,
            FREDDY,
            TUNG);
    public final BooleanSetting friends = new BooleanSetting("Друзья");

    public CustomModels() {
        super("CustomModels", Category.Render);
        setup(models, friends);
    }

    public String getModelName() {
        return models.getSelected();
    }

    public boolean shouldApplyTo(Avatar player) {
        if (!isState() || player == null || player.isSpectator()) {
            return false;
        }

        boolean localPlayer = mc != null && mc.player != null && player == mc.player;
        if (localPlayer) {
            return true;
        }

        return friends.isValue() && FriendRepo.isFriend(player.getName().getString());
    }
}
