package dev.tr7zw.entityculling;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import dev.tr7zw.transition.mc.ComponentProvider;
import dev.tr7zw.transition.mc.GeneralUtil;
//? if >= 1.21.9 {

import dev.tr7zw.entityculling.debugEntries.*;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventGameTicked;
import ru.arixcompany.features.event.world.EventWorldTick;
//? }

public class EntityCullingMod extends EntityCullingModBase
{

    public EntityCullingMod() {
        EventRepo.register(this);
    }

    @EventHandler
    public void clientTick(EventGameTicked eventGameTicked) {
        clientTick();
    }
    @EventHandler
    public void worldTick(EventWorldTick eventWorldTick) {
        worldTick();
    }
    public void onInitializeClient() {
        super.onInitialize();
    }

    @Override
    public void initModloader() {

        //? if >= 1.21.9 {

        registerDebugLine("cull_timing", new CullTimingEntry());
        registerDebugLine("culled_entities", new CulledEntitiesEntry());
        registerDebugLine("culled_block_entities", new CulledBlockEntitiesEntry());
        registerDebugLine("tick_culling", new TickCullingEntry());
        //? }
    }

    @Override
    public AABB setupAABB(BlockEntity entity, BlockPos pos) {
        //? if fabric || neoforge {

        if (entity instanceof BannerBlockEntity) {
            return new AABB(pos).inflate(0, 1, 0);
        }
        return new AABB(pos);
        //? } else {
        /*
               return entity.getRenderBoundingBox();
        *///? }
    }

    //? if >= 1.21.9 {

    public static final net.minecraft.client.gui.components.debug.DebugEntryCategory DEBUG_CATEGORY = new net.minecraft.client.gui.components.debug.DebugEntryCategory(
            ComponentProvider.translatable("text.entityculling.title"), 10F);
    public static final /*? >= 1.21.11 {*/ Identifier /*?} else {*//* ResourceLocation *//*?}*/ DEBUG_CATEGORY_ID = GeneralUtil
            .getResourceLocation("entityculling", "debug");

    private static void registerDebugLine(String id, net.minecraft.client.gui.components.debug.DebugScreenEntry entry) {
        DebugScreenEntries
                .register(GeneralUtil.getResourceLocation("entityculling", id), entry);
    }
    //? }
}
