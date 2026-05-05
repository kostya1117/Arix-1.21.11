package ru.arixcompany.features.module.modules.combat;

import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventAttack;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.repos.FriendRepo;

public class NoFriendDMG extends Module {
    public NoFriendDMG() {
        super("NoFriendDMG", Category.Combat);
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        if (FriendRepo.isFriend(e.getTarget())) {
            e.cancel();
        }
    }
}
