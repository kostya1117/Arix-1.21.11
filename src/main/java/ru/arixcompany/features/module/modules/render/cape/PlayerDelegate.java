package ru.arixcompany.features.module.modules.render.cape;

import net.minecraft.client.player.AbstractClientPlayer;

public class PlayerDelegate implements MinecraftPlayer {
    private final AbstractClientPlayer player;

    public PlayerDelegate(AbstractClientPlayer player) {
        this.player = player;
    }

    @Override
    public boolean isVisuallySwimming() { return player.isVisuallySwimming(); }

    @Override
    public float getXRot() { return player.getXRot(); }

    @Override
    public boolean isCrouching() { return player.isCrouching(); }

    @Override
    public double getY() { return player.getY(); }

    @Override
    public float getYRot() { return player.getYRot(); }

    @Override
    public double getZ() { return player.getZ(); }

    @Override
    public double getX() { return player.getX(); }

    @Override
    public boolean isUnderWater() { return player.isUnderWater(); }

    @Override
    public double getXCloak() { return player.avatarState().getInterpolatedCloakX(1f); }

    @Override
    public double getZCloak() { return player.avatarState().getInterpolatedCloakZ(1f); }

    @Override
    public float getYBodyRotO() { return player.yBodyRotO; }

    @Override
    public float getYBodyRot() { return player.yBodyRot; }

    @Override
    public double getYo() { return player.yo; }

    @Override
    public double getXo() { return player.xo; }

    @Override
    public double getZo() { return player.zo; }
}
