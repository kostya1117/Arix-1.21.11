package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.optifine.Config;
import net.optifine.RandomEntities;
import net.optifine.player.CapeUtils;
import net.optifine.player.PlayerConfigurations;
import net.optifine.reflect.Reflector;
import net.optifine.util.PlayerUtils;
import org.jspecify.annotations.Nullable;

public abstract class AbstractClientPlayer extends Player implements ClientAvatarEntity {
    private @Nullable PlayerInfo playerInfo;
    private final boolean showExtraEars;
    private final ClientAvatarState clientAvatarState = new ClientAvatarState();
    private Identifier locationOfCape = null;
    private long reloadCapeTimeMs = 0L;
    private boolean elytraOfCape = false;
    private String nameClear = null;
    public ShoulderRidingEntity entityShoulderLeft;
    public ShoulderRidingEntity entityShoulderRight;
    public ShoulderRidingEntity lastAttachedEntity;
    public float capeFlap;
    public float capeLean;
    public float capeLean2;
    private static final Identifier TEXTURE_ELYTRA = new Identifier("textures/entity/elytra.png");

    public AbstractClientPlayer(ClientLevel p_250460_, GameProfile p_249912_) {
        super(p_250460_, p_249912_);
        this.showExtraEars = "deadmau5".equals(this.getGameProfile().name());
        this.nameClear = p_249912_.name();
        if (this.nameClear != null && !this.nameClear.isEmpty()) {
            this.nameClear = StringUtil.stripColor(this.nameClear);
        }

        CapeUtils.downloadCape(this);
        PlayerConfigurations.getPlayerConfiguration(this);
    }

    @Override
    public @Nullable GameType gameMode() {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo != null ? playerinfo.getGameMode() : null;
    }

    protected @Nullable PlayerInfo getPlayerInfo() {
        if (this.playerInfo == null) {
            this.playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.getUUID());
        }

        return this.playerInfo;
    }

    @Override
    public void tick() {
        this.clientAvatarState.tick(this.position(), this.getDeltaMovement());
        super.tick();
        if (this.lastAttachedEntity != null) {
            RandomEntities.checkEntityShoulder(this.lastAttachedEntity, true);
            this.lastAttachedEntity = null;
        }
    }

    protected void addWalkedDistance(float p_423097_) {
        this.clientAvatarState.addWalkDistance(p_423097_);
    }

    @Override
    public ClientAvatarState avatarState() {
        return this.clientAvatarState;
    }

    @Override
    public @Nullable Component belowNameDisplay() {
        Scoreboard scoreboard = this.level().getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
        if (objective != null) {
            ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(this, objective);
            Component component = ReadOnlyScoreInfo.safeFormatValue(readonlyscoreinfo, objective.numberFormatOrDefault(StyledFormat.NO_STYLE));
            return Component.empty().append(component).append(CommonComponents.SPACE).append(objective.getDisplayName());
        } else {
            return null;
        }
    }

    @Override
    public PlayerSkin getSkin() {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo == null ? DefaultPlayerSkin.get(this.getUUID()) : playerinfo.getSkin();
    }

    @Override
    public Parrot.@Nullable Variant getParrotVariantOnShoulder(boolean p_422582_) {
        return (p_422582_ ? this.getShoulderParrotLeft() : this.getShoulderParrotRight()).orElse(null);
    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.avatarState().resetBob();
    }

    @Override
    public void aiStep() {
        this.updateBob();
        super.aiStep();
    }

    protected void updateBob() {
        float f;
        if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
            f = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
        } else {
            f = 0.0F;
        }

        this.avatarState().updateBob(f);
    }

    public float getFieldOfViewModifier(boolean p_361176_, float p_362521_) {
        float f = 1.0F;
        if (this.getAbilities().flying) {
            f *= 1.1F;
        }

        float f1 = this.getAbilities().getWalkingSpeed();
        if (f1 != 0.0F) {
            float f2 = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) / f1;
            f *= (f2 + 1.0F) / 2.0F;
        }

        if (this.isUsingItem()) {
            if (this.getUseItem().is(Items.BOW)) {
                float f3 = Math.min(this.getTicksUsingItem() / 20.0F, 1.0F);
                f *= 1.0F - Mth.square(f3) * 0.15F;
            } else if (p_361176_ && this.isScoping()) {
                return 0.1F;
            }
        }

        if (Reflector.ForgeEventFactoryClient_fireFovModifierEvent.exists()) {
            ComputeFovModifierEvent computefovmodifierevent = (ComputeFovModifierEvent)Reflector.ForgeEventFactoryClient_fireFovModifierEvent
                .call(this, f, p_362521_);
            if (computefovmodifierevent != null) {
                return computefovmodifierevent.getNewFovModifier();
            }
        }

        return Mth.lerp(p_362521_, 1.0F, f);
    }

    @Override
    public boolean showExtraEars() {
        return this.showExtraEars;
    }

    public String getNameClear() {
        return this.nameClear;
    }

    public Identifier getLocationOfCape() {
        return this.locationOfCape;
    }

    public void setLocationOfCape(Identifier locationOfCape) {
        this.locationOfCape = locationOfCape;
    }

    public boolean hasElytraCape() {
        Identifier identifier = this.getLocationCape();
        if (identifier == null) {
            return false;
        } else {
            return identifier == this.locationOfCape ? this.elytraOfCape : true;
        }
    }

    public void setElytraOfCape(boolean elytraOfCape) {
        this.elytraOfCape = elytraOfCape;
    }

    public boolean isElytraOfCape() {
        return this.elytraOfCape;
    }

    public long getReloadCapeTimeMs() {
        return this.reloadCapeTimeMs;
    }

    public void setReloadCapeTimeMs(long reloadCapeTimeMs) {
        this.reloadCapeTimeMs = reloadCapeTimeMs;
    }

    public @Nullable Identifier getLocationCape() {
        if (!Config.isShowCapes()) {
            return null;
        }
          //  if (Arix.getInstance().getFunctionRegistry().getCape().isState() && hasCustomCape()) {

        if (this.equals(Minecraft.getInstance().player)) {
            return Identifier.arix("images/Cape.png");
        }
           // }

        if (this.reloadCapeTimeMs != 0L && System.currentTimeMillis() > this.reloadCapeTimeMs) {
            CapeUtils.reloadCape(this);
            this.reloadCapeTimeMs = 0L;
            PlayerConfigurations.setPlayerConfiguration(this.getNameClear(), null);
        }

        return this.locationOfCape != null ? this.locationOfCape : PlayerUtils.getTexturePath(this.getSkin().cape());
    }

    public Identifier getLocationElytra() {
        return this.hasElytraCape() ? this.locationOfCape : PlayerUtils.getTexturePath(this.getSkin().elytra());
    }

    public Identifier getSkinTextureLocation() {
        PlayerInfo playerinfo = this.getPlayerInfo();
        return playerinfo == null ? DefaultPlayerSkin.get(this.getUUID()).body().texturePath() : playerinfo.getSkin().body().texturePath();
    }
}
