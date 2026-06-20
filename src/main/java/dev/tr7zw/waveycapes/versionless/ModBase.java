package dev.tr7zw.waveycapes.versionless;

import dev.tr7zw.waveycapes.versionless.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.tr7zw.waveycapes.versionless.nms.MinecraftPlayer;
import dev.tr7zw.waveycapes.versionless.util.Vector3;
import lombok.Getter;

public abstract class ModBase {

    public static final Logger LOGGER = LogManager.getLogger("WaveyCapes");
    @Getter
    public static ModBase INSTANCE;
    public static Config config;

    public void init() {
        INSTANCE = this;
        config = new Config();
    }

    public abstract void initSupportHooks();

    public abstract Vector3 applyModAnimations(MinecraftPlayer player, Vector3 pos);

}
