package net.minecraft.client.resources.server;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public interface PackReloadConfig {
    void scheduleReload(PackReloadConfig.Callbacks p_312695_);

    
    interface Callbacks {
        void onSuccess();

        void onFailure(boolean p_313000_);

        List<PackReloadConfig.IdAndPath> packsToLoad();
    }

    
    record IdAndPath(UUID id, Path path) {
    }
}
