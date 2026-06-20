package dev.tr7zw.waveycapes;

import dev.tr7zw.waveycapes.support.MinecraftCapesSupport;
import dev.tr7zw.waveycapes.support.SupportManager;
//? if fabric {


public class WaveyCapesMod extends WaveyCapesBase {
    //? } else {
/*
public class WaveyCapesMod extends WaveyCapesBase {
    *///? }

    @Override
    public void initSupportHooks() {
        super.initSupportHooks();
            SupportManager.mods.add(new MinecraftCapesSupport());
            LOGGER.info("Wavey Capes loaded MinecraftCapes support!");
        }

    @Override
    public void init() {
        super.init();
    }

}
