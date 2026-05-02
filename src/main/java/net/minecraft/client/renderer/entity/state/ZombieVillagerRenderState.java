package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public class ZombieVillagerRenderState extends ZombieRenderState implements VillagerDataHolderRenderState {
    public  VillagerData villagerData;

    @Override
    public  VillagerData getVillagerData() {
        return this.villagerData;
    }
}
