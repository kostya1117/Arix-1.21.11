package net.minecraft.client.gui;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import net.minecraft.client.gui.contextualbar.JumpableVehicleBarRenderer;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.optifine.Config;
import net.optifine.CustomItems;
import net.optifine.TextureAnimations;
import net.optifine.reflect.Reflector;
import org.apache.commons.lang3.tuple.Pair;
import ru.arixcompany.Arix;
import ru.arixcompany.ui.draggable.DraggableRepo;
import ru.arixcompany.ui.draggable.draggables.ArmorHudDraggable;
import ru.arixcompany.ui.draggable.draggables.ScoreboardDraggable;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.render.EventRender2D;
import ru.arixcompany.features.module.modules.render.Animations;
import ru.arixcompany.features.module.modules.render.NoRender;

public class Gui {
    private static final Identifier CROSSHAIR_SPRITE = Identifier.withDefaultNamespace("hud/crosshair");
    private static final Identifier CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_full");
    private static final Identifier CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_background");
    private static final Identifier CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/crosshair_attack_indicator_progress");
    private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("hud/effect_background_ambient");
    private static final Identifier EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/effect_background");
    private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier HOTBAR_SELECTION_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final Identifier HOTBAR_OFFHAND_LEFT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
    private static final Identifier HOTBAR_OFFHAND_RIGHT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_right");
    private static final Identifier HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_background");
    private static final Identifier HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_progress");
    private static final Identifier ARMOR_EMPTY_SPRITE = Identifier.withDefaultNamespace("hud/armor_empty");
    private static final Identifier ARMOR_HALF_SPRITE = Identifier.withDefaultNamespace("hud/armor_half");
    private static final Identifier ARMOR_FULL_SPRITE = Identifier.withDefaultNamespace("hud/armor_full");
    private static final Identifier FOOD_EMPTY_HUNGER_SPRITE = Identifier.withDefaultNamespace("hud/food_empty_hunger");
    private static final Identifier FOOD_HALF_HUNGER_SPRITE = Identifier.withDefaultNamespace("hud/food_half_hunger");
    private static final Identifier FOOD_FULL_HUNGER_SPRITE = Identifier.withDefaultNamespace("hud/food_full_hunger");
    private static final Identifier FOOD_EMPTY_SPRITE = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_HALF_SPRITE = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier FOOD_FULL_SPRITE = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier AIR_SPRITE = Identifier.withDefaultNamespace("hud/air");
    private static final Identifier AIR_POPPING_SPRITE = Identifier.withDefaultNamespace("hud/air_bursting");
    private static final Identifier AIR_EMPTY_SPRITE = Identifier.withDefaultNamespace("hud/air_empty");
    private static final Identifier HEART_VEHICLE_CONTAINER_SPRITE = Identifier.withDefaultNamespace("hud/heart/vehicle_container");
    private static final Identifier HEART_VEHICLE_FULL_SPRITE = Identifier.withDefaultNamespace("hud/heart/vehicle_full");
    private static final Identifier HEART_VEHICLE_HALF_SPRITE = Identifier.withDefaultNamespace("hud/heart/vehicle_half");
    private static final Identifier VIGNETTE_LOCATION = Identifier.withDefaultNamespace("textures/misc/vignette.png");
    public static final Identifier NAUSEA_LOCATION = Identifier.withDefaultNamespace("textures/misc/nausea.png");
    private static final Identifier SPYGLASS_SCOPE_LOCATION = Identifier.withDefaultNamespace("textures/misc/spyglass_scope.png");
    private static final Identifier POWDER_SNOW_OUTLINE_LOCATION = Identifier.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    private static final Comparator<PlayerScoreEntry> SCORE_DISPLAY_ORDER = Comparator.comparing(PlayerScoreEntry::value)
        .reversed()
        .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER);
    private static final Component DEMO_EXPIRED_TEXT = Component.translatable("demo.demoExpired");
    private static final Component SAVING_TEXT = Component.translatable("menu.savingLevel");
    private final RandomSource random = RandomSource.create();
    private final Minecraft minecraft;
    private final ChatComponent chat;
    public int tickCount;

    private  Component overlayMessageString;
    private int overlayMessageTime;
    private boolean animateOverlayMessageColor;
    private boolean chatDisabledByPlayerShown;
    public float vignetteBrightness = 1.0F;
    private int toolHighlightTimer;
    private ItemStack lastToolHighlight = ItemStack.EMPTY;
    protected DebugScreenOverlay debugOverlay;
    private final SubtitleOverlay subtitleOverlay;
    private final SpectatorGui spectatorGui;
    private final PlayerTabOverlay tabList;
    private final BossHealthOverlay bossOverlay;
    private int titleTime;
    private  Component title;
    private  Component subtitle;
    private int titleFadeInTime;
    private int titleStayTime;
    private int titleFadeOutTime;
    private int lastHealth;
    private int displayHealth;
    private long lastHealthTime;
    private long healthBlinkTime;
    private int lastBubblePopSoundPlayed;
    private  Runnable deferredSubtitles;
    private float autosaveIndicatorValue;
    private float lastAutosaveIndicatorValue;
    private Pair<Gui.ContextualInfo, ContextualBarRenderer> contextualInfoBar = Pair.of(Gui.ContextualInfo.EMPTY, ContextualBarRenderer.EMPTY);
    private final Map<Gui.ContextualInfo, Supplier<ContextualBarRenderer>> contextualInfoBarRenderers;
    private float scopeScale;

    public float hotbarChatOffset = 0.0F;
    private float chatSlideOffset = 0.0F;

    // Анимация подъёма хотбара — используется и хотбаром и ArmorHUD
    public static final ru.arixcompany.utils.animation.Animation hotbarRiseAnim =
            new ru.arixcompany.utils.animation.impl.EaseInOutQuad(200, 1.0);

    static {
        hotbarRiseAnim.setDirection(ru.arixcompany.utils.animation.Direction.BACKWARDS);
        hotbarRiseAnim.timerUtil.setTime(System.currentTimeMillis() - 9999);
    }

    public Gui(Minecraft p_330021_) {
        this.minecraft = p_330021_;
        this.debugOverlay = new DebugScreenOverlay(p_330021_);
        this.spectatorGui = new SpectatorGui(p_330021_);
        this.chat = new ChatComponent(p_330021_);
        this.tabList = new PlayerTabOverlay(p_330021_, this);
        this.bossOverlay = new BossHealthOverlay(p_330021_);
        this.subtitleOverlay = new SubtitleOverlay(p_330021_);
        this.contextualInfoBarRenderers = ImmutableMap.of(
            Gui.ContextualInfo.EMPTY,
            () -> ContextualBarRenderer.EMPTY,
            Gui.ContextualInfo.EXPERIENCE,
            () -> new ExperienceBarRenderer(p_330021_),
            Gui.ContextualInfo.LOCATOR,
            () -> new LocatorBarRenderer(p_330021_),
            Gui.ContextualInfo.JUMPABLE_VEHICLE,
            () -> new JumpableVehicleBarRenderer(p_330021_)
        );
        this.resetTitleTimes();
        Reflector.ForgeLayeredDraw_init.call(this, p_330021_);
    }

    public void resetTitleTimes() {
        this.titleFadeInTime = 10;
        this.titleStayTime = 70;
        this.titleFadeOutTime = 20;
    }

    public void render(GuiGraphics p_282884_, DeltaTracker p_342095_) {
        if (!(this.minecraft.screen instanceof LevelLoadingScreen)) {
            if (Reflector.ForgeLayeredDraw_beginRender.exists()) {
                Reflector.ForgeLayeredDraw_beginRender.call(p_282884_, p_342095_);
                return;
            }

            if (!this.minecraft.options.hideGui) {
                this.renderCameraOverlays(p_282884_, p_342095_);
                this.renderCrosshair(p_282884_, p_342095_);
                p_282884_.nextStratum();
                this.renderHotbarAndDecorations(p_282884_, p_342095_);
                this.renderEffects(p_282884_, p_342095_);
                this.renderBossOverlay(p_282884_, p_342095_);
            }

            this.renderSleepOverlay(p_282884_, p_342095_);

            if (!this.minecraft.options.hideGui) {
                this.renderScoreboardSidebar(p_282884_, p_342095_);
                this.renderOverlayMessage(p_282884_, p_342095_);
                this.renderTitle(p_282884_, p_342095_);
                this.renderChat(p_282884_, p_342095_);
                this.renderTabList(p_282884_, p_342095_);
                this.renderSubtitleOverlay(p_282884_,
                        this.minecraft.screen == null || this.minecraft.screen.isInGameUi());
            } else if (this.minecraft.screen != null && this.minecraft.screen.isInGameUi()) {
                this.renderSubtitleOverlay(p_282884_, true);
            }
        }

        if (!(this.minecraft.screen instanceof ChatScreen)) {
            renderDraggableOverlay(p_282884_, p_342095_);
        } else {
            renderArmorHudOnly(p_282884_, p_342095_);
        }

        EventRepo.call(new EventRender2D(p_282884_));
    }

    private void renderArmorHudOnly(GuiGraphics graphics, DeltaTracker delta) {
        if (Arix.getInstance() == null) return;
        DraggableRepo repo = Arix.getInstance().getDraggableRepo();
        if (repo == null) return;

        Window window = this.minecraft.getWindow();
        int mouseX = (int)(this.minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getWidth());
        int mouseY = (int)(this.minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getHeight());

        repo.updateAll();

        ArmorHudDraggable armorHud = repo.getArmorHud();
        if (armorHud != null && armorHud.shouldRender()) {
            armorHud.renderComponent(graphics, mouseX, mouseY, delta.getGameTimeDeltaPartialTick(false));
        }
    }

    private void renderDraggableOverlay(GuiGraphics graphics, DeltaTracker delta) {
        if (Arix.getInstance() == null) return;
        DraggableRepo repo = Arix.getInstance().getDraggableRepo();
        if (repo == null) return;

        Window window = this.minecraft.getWindow();
        int mouseX = (int)(this.minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getWidth());
        int mouseY = (int)(this.minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getHeight());

        repo.updateAll();
        repo.renderAll(graphics, mouseX, mouseY, delta.getGameTimeDeltaPartialTick(false));
    }

    private void renderBossOverlay(GuiGraphics p_407400_, DeltaTracker p_407876_) {
        this.bossOverlay.render(p_407400_);
    }

    public void renderDebugOverlay(GuiGraphics p_410614_) {
        this.debugOverlay.render(p_410614_);
    }

    private void renderSubtitleOverlay(GuiGraphics p_406760_, boolean p_422895_) {
        if (p_422895_) {
            this.deferredSubtitles = () -> this.subtitleOverlay.render(p_406760_);
        } else {
            this.deferredSubtitles = null;
            this.subtitleOverlay.render(p_406760_);
        }
    }

    public void renderDeferredSubtitles() {
        if (this.deferredSubtitles != null) {
            this.deferredSubtitles.run();
            this.deferredSubtitles = null;
        }
    }

    private void renderCameraOverlays(GuiGraphics p_333627_, DeltaTracker p_344236_) {
        if (Config.isVignetteEnabled()) {
            this.renderVignette(p_333627_, this.minecraft.getCameraEntity());
        }

        LocalPlayer localplayer = this.minecraft.player;
        float f = p_344236_.getGameTimeDeltaTicks();
        this.scopeScale = Mth.lerp(0.5F * f, this.scopeScale, 1.125F);
        if (this.minecraft.options.getCameraType().isFirstPerson()) {
            if (localplayer.isScoping()) {
                this.renderSpyglassOverlay(p_333627_, this.scopeScale);
            } else {
                this.scopeScale = 0.5F;

                for (EquipmentSlot equipmentslot : EquipmentSlot.values()) {
                    ItemStack itemstack = localplayer.getItemBySlot(equipmentslot);
                    Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
                    if (equippable != null && equippable.slot() == equipmentslot && equippable.cameraOverlay().isPresent()) {
                        this.renderTextureOverlay(p_333627_, equippable.cameraOverlay().get().withPath(nameIn -> "textures/" + nameIn + ".png"), 1.0F);
                    }
                }
            }
        }

        if (localplayer.getTicksFrozen() > 0) {
            this.renderTextureOverlay(p_333627_, POWDER_SNOW_OUTLINE_LOCATION, localplayer.getPercentFrozen());
        }

        float f1 = p_344236_.getGameTimeDeltaPartialTick(false);
        float f2 = Mth.lerp(f1, localplayer.oPortalEffectIntensity, localplayer.portalEffectIntensity);
        float f3 = localplayer.getEffectBlendFactor(MobEffects.NAUSEA, f1);
        if (f2 > 0.0F) {
            this.renderPortalOverlay(p_333627_, f2);
        } else if (f3 > 0.0F) {
            float f4 = this.minecraft.options.screenEffectScale().get().floatValue();
            if (f4 < 1.0F) {
                float f5 = f3 * (1.0F - f4);
                this.renderConfusionOverlay(p_333627_, f5);
            }
        }
    }

    private void renderSleepOverlay(GuiGraphics p_329087_, DeltaTracker p_345225_) {
        if (this.minecraft.player.getSleepTimer() > 0) {
            Profiler.get().push("sleep");
            p_329087_.nextStratum();
            float f = this.minecraft.player.getSleepTimer();
            float f1 = f / 100.0F;
            if (f1 > 1.0F) {
                f1 = 1.0F - (f - 100.0F) / 10.0F;
            }

            int i = (int)(220.0F * f1) << 24 | 1052704;
            p_329087_.fill(0, 0, p_329087_.guiWidth(), p_329087_.guiHeight(), i);
            Profiler.get().pop();
        }
    }

    private void renderOverlayMessage(GuiGraphics p_330258_, DeltaTracker p_345514_) {
        Font font = this.getFont();
        if (this.overlayMessageString != null && this.overlayMessageTime > 0) {
            Profiler.get().push("overlayMessage");
            float f = this.overlayMessageTime - p_345514_.getGameTimeDeltaPartialTick(false);
            int i = (int)(f * 255.0F / 20.0F);
            if (i > 255) {
                i = 255;
            }

            if (i > 0) {
                p_330258_.nextStratum();
                p_330258_.pose().pushMatrix();

                p_330258_.pose().translate((float)p_330258_.guiWidth() / 2.0F, (float)p_330258_.guiHeight() - 68.0F - this.hotbarChatOffset);

                int j;
                if (this.animateOverlayMessageColor) {
                    j = Mth.hsvToArgb(f / 50.0F, 0.7F, 0.6F, i);
                } else {
                    j = ARGB.white(i);
                }

                int k = font.width(this.overlayMessageString);
                p_330258_.drawStringWithBackdrop(font, this.overlayMessageString, -k / 2, -4, k, j);
                p_330258_.pose().popMatrix();
            }

            Profiler.get().pop();
        }
    }

    private void renderTitle(GuiGraphics p_331218_, DeltaTracker p_344700_) {
        if (this.title != null && this.titleTime > 0) {
            Font font = this.getFont();
            Profiler.get().push("titleAndSubtitle");
            float f = this.titleTime - p_344700_.getGameTimeDeltaPartialTick(false);
            int i = 255;
            if (this.titleTime > this.titleFadeOutTime + this.titleStayTime) {
                float f1 = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime - f;
                i = (int)(f1 * 255.0F / this.titleFadeInTime);
            }

            if (this.titleTime <= this.titleFadeOutTime) {
                i = (int)(f * 255.0F / this.titleFadeOutTime);
            }

            i = Mth.clamp(i, 0, 255);
            if (i > 0) {
                p_331218_.nextStratum();
                p_331218_.pose().pushMatrix();
                p_331218_.pose().translate(p_331218_.guiWidth() / 2, p_331218_.guiHeight() / 2);
                p_331218_.pose().pushMatrix();
                p_331218_.pose().scale(4.0F, 4.0F);
                int l = font.width(this.title);
                int j = ARGB.white(i);
                p_331218_.drawStringWithBackdrop(font, this.title, -l / 2, -10, l, j);
                p_331218_.pose().popMatrix();
                if (this.subtitle != null) {
                    p_331218_.pose().pushMatrix();
                    p_331218_.pose().scale(2.0F, 2.0F);
                    int k = font.width(this.subtitle);
                    p_331218_.drawStringWithBackdrop(font, this.subtitle, -k / 2, 5, k, j);
                    p_331218_.pose().popMatrix();
                }

                p_331218_.pose().popMatrix();
            }

            Profiler.get().pop();
        }
    }

    private void renderChat(GuiGraphics p_329202_, DeltaTracker p_342328_) {
        if (!this.chat.isChatFocused()) {
            Window window = this.minecraft.getWindow();
            int i = Mth.floor(this.minecraft.mouseHandler.getScaledXPos(window));
            int j = Mth.floor(this.minecraft.mouseHandler.getScaledYPos(window));
            p_329202_.nextStratum();

            if (Animations.isEnabled("Чат")) {
                boolean isChatOpen = this.minecraft.screen instanceof ChatScreen;
                float targetChatSlide = isChatOpen ? 0.0F : 8.0F;
                this.chatSlideOffset = Mth.lerp(0.2F, this.chatSlideOffset, targetChatSlide);

                p_329202_.pose().pushMatrix();
                p_329202_.pose().translate(0.0F, this.chatSlideOffset);
            }

            if (Reflector.ForgeHooksClient_onCustomizeChatEvent.exists()) {
                Reflector.ForgeHooksClient_onCustomizeChatEvent.callVoid(p_329202_, this.chat, window, i, j, this.tickCount, this.getFont());
            } else {
                this.chat.render(p_329202_, this.getFont(), this.tickCount, i, j, false, false);
            }

            if (Animations.isEnabled("Чат")) {
                p_329202_.pose().popMatrix();
            }
        }
    }

    private void renderScoreboardSidebar(GuiGraphics p_332744_, DeltaTracker p_344235_) {
        if (ScoreboardDraggable.isCustomScoreboardActive()) {
            return;
        }

        Scoreboard scoreboard = this.minecraft.level.getScoreboard();
        Objective objective = null;
        PlayerTeam playerteam = scoreboard.getPlayersTeam(this.minecraft.player.getScoreboardName());
        if (playerteam != null) {
            DisplaySlot displayslot = DisplaySlot.teamColorToSlot(playerteam.getColor());
            if (displayslot != null) {
                objective = scoreboard.getDisplayObjective(displayslot);
            }
        }

        Objective objective1 = objective != null ? objective : scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective1 != null) {
            p_332744_.nextStratum();
            this.displayScoreboardSidebar(p_332744_, objective1);
        }
    }

    private void renderTabList(GuiGraphics p_330031_, DeltaTracker p_343599_) {
        Scoreboard scoreboard = this.minecraft.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);

        boolean shouldShow = this.minecraft.options.keyPlayerList.isDown()
                && (!this.minecraft.isLocalServer() || this.minecraft.player.connection.getListedOnlinePlayers().size() > 1 || objective != null);

        this.tabList.setVisible(shouldShow);

        if (this.tabList.shouldRender()) {
            p_330031_.nextStratum();
            this.tabList.render(p_330031_, p_330031_.guiWidth(), scoreboard, objective);
        }
    }

    private void renderCrosshair(GuiGraphics p_282828_, DeltaTracker p_343490_) {
        Options options = this.minecraft.options;
        if (options.getCameraType().isFirstPerson()
                && (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR || this.canRenderCrosshairForSpectator(this.minecraft.hitResult))
                && !this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR)) {
            p_282828_.nextStratum();
            int i = 15;
            p_282828_.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_SPRITE, (p_282828_.guiWidth() - 15) / 2, (p_282828_.guiHeight() - 15) / 2, 15, 15);
            if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.CROSSHAIR) {
                float f = this.minecraft.player.getAttackStrengthScale(0.0F);
                boolean flag = false;
                if (this.minecraft.crosshairPickEntity != null && this.minecraft.crosshairPickEntity instanceof LivingEntity && f >= 1.0F) {
                    flag = this.minecraft.player.getCurrentItemAttackStrengthDelay() > 5.0F;
                    flag &= this.minecraft.crosshairPickEntity.isAlive();
                    AttackRange attackrange = this.minecraft.player.getActiveItem().get(DataComponents.ATTACK_RANGE);
                    flag &= attackrange == null || attackrange.isInRange(this.minecraft.player, this.minecraft.hitResult.getLocation());
                }

                int l = p_282828_.guiHeight() / 2 - 7 + 16;
                int j = p_282828_.guiWidth() / 2 - 8;
                if (flag) {
                    p_282828_.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_FULL_SPRITE, j, l, 16, 16);
                } else if (f < 1.0F) {
                    int k = (int)(f * 17.0F);
                    p_282828_.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_BACKGROUND_SPRITE, j, l, 16, 4);
                    p_282828_.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR_ATTACK_INDICATOR_PROGRESS_SPRITE, 16, 4, 0, 0, j, l, k, 4);
                }
            }
        }
    }

    private boolean canRenderCrosshairForSpectator( HitResult p_93025_) {
        if (p_93025_ == null) {
            return false;
        } else if (p_93025_.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult)p_93025_).getEntity() instanceof MenuProvider;
        } else if (p_93025_.getType() == HitResult.Type.BLOCK) {
            BlockPos blockpos = ((BlockHitResult)p_93025_).getBlockPos();
            Level level = this.minecraft.level;
            return level.getBlockState(blockpos).getMenuProvider(level, blockpos) != null;
        } else {
            return false;
        }
    }

    private void renderEffects(GuiGraphics p_282812_, DeltaTracker p_343719_) {
        Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
        if (!collection.isEmpty() && (this.minecraft.screen == null || !this.minecraft.screen.showsActiveEffects())) {
            int i = 0;
            int j = 0;

            for (MobEffectInstance mobeffectinstance : Ordering.natural().reverse().sortedCopy(collection)) {
                Holder<MobEffect> holder = mobeffectinstance.getEffect();
                IClientMobEffectExtensions iclientmobeffectextensions = IClientMobEffectExtensions.of(mobeffectinstance);
                if ((iclientmobeffectextensions == null || iclientmobeffectextensions.isVisibleInGui(mobeffectinstance)) && mobeffectinstance.showIcon()) {
                    int k = p_282812_.guiWidth();
                    int l = 1;

                    if (holder.value().isBeneficial()) {
                        i++;
                        k -= 25 * i;
                    } else {
                        j++;
                        k -= 25 * j;
                        l += 26;
                    }

                    float f = 1.0F;
                    if (mobeffectinstance.isAmbient()) {
                        p_282812_.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_AMBIENT_SPRITE, k, l, 24, 24);
                    } else {
                        p_282812_.blitSprite(RenderPipelines.GUI_TEXTURED, EFFECT_BACKGROUND_SPRITE, k, l, 24, 24);
                        if (mobeffectinstance.endsWithin(200)) {
                            int i1 = mobeffectinstance.getDuration();
                            int j1 = 10 - i1 / 20;
                            f = Mth.clamp(i1 / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
                                + Mth.cos(i1 * (float) Math.PI / 5.0F) * Mth.clamp(j1 / 10.0F * 0.25F, 0.0F, 0.25F);
                            f = Mth.clamp(f, 0.0F, 1.0F);
                        }
                    }

                    if (iclientmobeffectextensions == null || !iclientmobeffectextensions.renderGuiIcon(mobeffectinstance, this, p_282812_, k, l, 0.0F, f)) {
                        p_282812_.blitSprite(RenderPipelines.GUI_TEXTURED, getMobEffectSprite(holder), k + 3, l + 3, 18, 18, ARGB.white(f));
                    }
                }
            }
        }
    }

    public static Identifier getMobEffectSprite(Holder<MobEffect> p_409701_) {
        return p_409701_.unwrapKey().map(ResourceKey::identifier).map(loc2In -> loc2In.withPrefix("mob_effect/")).orElseGet(MissingTextureAtlasSprite::getLocation);
    }

    private void renderHotbarAndDecorations(GuiGraphics p_333625_, DeltaTracker p_344796_) {
        boolean isChatOpen = this.minecraft.screen instanceof ChatScreen;

        if (Animations.isEnabled("Хотбар")) {
            ru.arixcompany.utils.animation.Direction target = isChatOpen
                    ? ru.arixcompany.utils.animation.Direction.FORWARDS
                    : ru.arixcompany.utils.animation.Direction.BACKWARDS;
            if (hotbarRiseAnim.getDirection() != target) hotbarRiseAnim.setDirection(target);
            this.hotbarChatOffset = (float) hotbarRiseAnim.getOutput() * 14.0F;
        } else {
            hotbarRiseAnim.setDirection(ru.arixcompany.utils.animation.Direction.BACKWARDS);
            hotbarRiseAnim.timerUtil.setTime(System.currentTimeMillis() - 9999);
            this.hotbarChatOffset = isChatOpen ? 14.0F : 0.0F;
        }

        p_333625_.pose().pushMatrix();
        p_333625_.pose().translate(0.0F, -this.hotbarChatOffset);

        if (this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            this.spectatorGui.renderHotbar(p_333625_);
        } else {
            this.renderItemHotbar(p_333625_, p_344796_);
        }

        if (this.minecraft.gameMode.canHurtPlayer()) {
            this.renderPlayerHealth(p_333625_);
        }
        this.renderVehicleHealth(p_333625_);

        Gui.ContextualInfo gui$contextualinfo = this.nextContextualInfoState();
        if (gui$contextualinfo != this.contextualInfoBar.getKey()) {
            this.contextualInfoBar = Pair.of(gui$contextualinfo, this.contextualInfoBarRenderers.get(gui$contextualinfo).get());
        }

        this.contextualInfoBar.getValue().renderBackground(p_333625_, p_344796_);
        if (this.minecraft.gameMode.hasExperience() && this.minecraft.player.experienceLevel > 0) {
            ContextualBarRenderer.renderExperienceLevel(p_333625_, this.minecraft.font,
                    this.minecraft.player.experienceLevel);
        }
        this.contextualInfoBar.getValue().render(p_333625_, p_344796_);

        if (this.minecraft.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            this.renderSelectedItemName(p_333625_);
        } else if (this.minecraft.player.isSpectator()) {
            this.spectatorGui.renderAction(p_333625_);
        }

        p_333625_.pose().popMatrix();
    }

    private void renderItemHotbar(GuiGraphics p_332738_, DeltaTracker p_342619_) {
        Player player = this.getCameraPlayer();
        if (player != null) {
            ItemStack itemstack = player.getOffhandItem();
            HumanoidArm humanoidarm = player.getMainArm().getOpposite();
            int i = p_332738_.guiWidth() / 2;
            int j = 182;
            int k = 91;
            p_332738_.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, i - 91, p_332738_.guiHeight() - 22, 182, 22);
            p_332738_.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, i - 91 - 1 + player.getInventory().getSelectedSlot() * 20, p_332738_.guiHeight() - 22 - 1, 24, 23);
            if (!itemstack.isEmpty()) {
                if (humanoidarm == HumanoidArm.LEFT) {
                    p_332738_.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, i - 91 - 29, p_332738_.guiHeight() - 23, 29, 24);
                } else {
                    p_332738_.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_RIGHT_SPRITE, i + 91, p_332738_.guiHeight() - 23, 29, 24);
                }
            }

            int l = 1;
            CustomItems.setRenderOffHand(false);

            for (int i1 = 0; i1 < 9; i1++) {
                int j1 = i - 90 + i1 * 20 + 2;
                int k1 = p_332738_.guiHeight() - 16 - 3;
                this.renderSlot(p_332738_, j1, k1, p_342619_, player, player.getInventory().getItem(i1), l++);
            }

            if (!itemstack.isEmpty()) {
                CustomItems.setRenderOffHand(true);
                int i2 = p_332738_.guiHeight() - 16 - 3;
                if (humanoidarm == HumanoidArm.LEFT) {
                    this.renderSlot(p_332738_, i - 91 - 26, i2, p_342619_, player, itemstack, l++);
                } else {
                    this.renderSlot(p_332738_, i + 91 + 10, i2, p_342619_, player, itemstack, l++);
                }

                CustomItems.setRenderOffHand(false);
            }

            if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
                float f = this.minecraft.player.getAttackStrengthScale(0.0F);
                if (f < 1.0F) {
                    int j2 = p_332738_.guiHeight() - 20;
                    int k2 = i + 91 + 6;
                    if (humanoidarm == HumanoidArm.RIGHT) {
                        k2 = i - 91 - 22;
                    }

                    int l1 = (int)(f * 19.0F);
                    p_332738_.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE, k2, j2, 18, 18);
                    p_332738_.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE, 18, 18, 0, 18 - l1, k2, j2 + 18 - l1, 18, l1);
                }
            }
        }
    }

    private void renderSelectedItemName(GuiGraphics p_283501_) {
        this.renderSelectedItemName(p_283501_, 0);
    }

    public void renderSelectedItemName(GuiGraphics graphicsIn, int yShift) {
        Profiler.get().push("selectedItemName");
        if (this.toolHighlightTimer > 0 && !this.lastToolHighlight.isEmpty()) {
            MutableComponent mutablecomponent = Component.empty().append(this.lastToolHighlight.getHoverName()).withStyle(this.lastToolHighlight.getRarity().color());
            if (this.lastToolHighlight.has(DataComponents.CUSTOM_NAME)) {
                mutablecomponent.withStyle(ChatFormatting.ITALIC);
            }

            Component component = mutablecomponent;
            if (Reflector.IForgeItemStack_getHighlightTip.exists()) {
                component = (Component)Reflector.call(this.lastToolHighlight, Reflector.IForgeItemStack_getHighlightTip, component);
            }

            Font font = null;
            IClientItemExtensions iclientitemextensions = IClientItemExtensions.of(this.lastToolHighlight);
            if (iclientitemextensions != null) {
                font = iclientitemextensions.getFont(this.lastToolHighlight, IClientItemExtensions.FontContext.SELECTED_ITEM_NAME);
            }

            if (font == null) {
                font = this.getFont();
            }

            int i = font.width(component);
            int j = (graphicsIn.guiWidth() - i) / 2;
            int k = graphicsIn.guiHeight() - Math.max(yShift, 59);
            if (!this.minecraft.gameMode.canHurtPlayer()) {
                k += 14;
            }

            int l = (int)(this.toolHighlightTimer * 256.0F / 10.0F);
            if (l > 255) {
                l = 255;
            }

            if (l > 0) {
                graphicsIn.drawStringWithBackdrop(font, component, j, k, i, ARGB.white(l));
            }
        }

        Profiler.get().pop();
    }

    private void displayScoreboardSidebar(GuiGraphics p_282008_, Objective p_283455_) {
        Scoreboard scoreboard = p_283455_.getScoreboard();
        NumberFormat numberformat = p_283455_.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        Gui$1DisplayEntry[] agui$1displayentry = scoreboard.listPlayerScores(p_283455_)
            .stream()
            .filter(entryIn -> !entryIn.isHidden())
            .sorted(SCORE_DISPLAY_ORDER)
            .limit(15L)
            .map(entry2In -> {
                PlayerTeam playerteam = scoreboard.getPlayersTeam(entry2In.owner());
                Component component1 = entry2In.ownerName();
                Component component2 = PlayerTeam.formatNameForTeam(playerteam, component1);
                Component component3 = entry2In.formatValue(numberformat);
                int k3 = this.getFont().width(component3);
                return new Gui$1DisplayEntry(component2, component3, k3);
            })
            .toArray(Gui$1DisplayEntry[]::new);
        Component component = p_283455_.getDisplayName();
        int i = this.getFont().width(component);
        int j = i;
        int k = this.getFont().width(": ");

        for (Gui$1DisplayEntry gui$1displayentry : agui$1displayentry) {
            j = Math.max(j, this.getFont().width(gui$1displayentry.name()) + (gui$1displayentry.scoreWidth() > 0 ? k + gui$1displayentry.scoreWidth() : 0));
        }

        int k2 = agui$1displayentry.length;
        int l2 = k2 * 9;
        int i3 = p_282008_.guiHeight() / 2 + l2 / 3;
        int j3 = 3;
        int l = p_282008_.guiWidth() - j - 3;
        int i1 = p_282008_.guiWidth() - 3 + 2;
        int j1 = this.minecraft.options.getBackgroundColor(0.3F);
        int k1 = this.minecraft.options.getBackgroundColor(0.4F);
        int l1 = i3 - k2 * 9;
        p_282008_.fill(l - 2, l1 - 9 - 1, i1, l1 - 1, k1);
        p_282008_.fill(l - 2, l1 - 1, i1, i3, j1);
        p_282008_.drawString(this.getFont(), component, l + j / 2 - i / 2, l1 - 9, -1, false);

        for (int i2 = 0; i2 < k2; i2++) {
            Gui$1DisplayEntry gui$1displayentry1 = agui$1displayentry[i2];
            int j2 = i3 - (k2 - i2) * 9;
            p_282008_.drawString(this.getFont(), gui$1displayentry1.name(), l, j2, -1, false);
            p_282008_.drawString(this.getFont(), gui$1displayentry1.score(), i1 - gui$1displayentry1.scoreWidth(), j2, -1, false);
        }
    }

    private  Player getCameraPlayer() {
        return this.minecraft.getCameraEntity() instanceof Player player ? player : null;
    }

    private  LivingEntity getPlayerVehicleWithHealth() {
        Player player = this.getCameraPlayer();
        if (player != null) {
            Entity entity = player.getVehicle();
            if (entity == null) {
                return null;
            }

            if (entity instanceof LivingEntity) {
                return (LivingEntity)entity;
            }
        }

        return null;
    }

    private int getVehicleMaxHearts( LivingEntity p_93023_) {
        if (p_93023_ != null && p_93023_.showVehicleHealth()) {
            float f = p_93023_.getMaxHealth();
            int i = (int)(f + 0.5F) / 2;
            if (i > 30) {
                i = 30;
            }

            return i;
        } else {
            return 0;
        }
    }

    private int getVisibleVehicleHeartRows(int p_93013_) {
        return (int)Math.ceil(p_93013_ / 10.0);
    }

    private void renderPlayerHealth(GuiGraphics p_283143_) {
        Player player = this.getCameraPlayer();
        if (player != null) {
            int i = Mth.ceil(player.getHealth());
            boolean flag = this.healthBlinkTime > this.tickCount && (this.healthBlinkTime - this.tickCount) / 3L % 2L == 1L;
            long j = Util.getMillis();
            if (i < this.lastHealth && player.invulnerableTime > 0) {
                this.lastHealthTime = j;
                this.healthBlinkTime = this.tickCount + 20;
            } else if (i > this.lastHealth && player.invulnerableTime > 0) {
                this.lastHealthTime = j;
                this.healthBlinkTime = this.tickCount + 10;
            }

            if (j - this.lastHealthTime > 1000L) {
                this.displayHealth = i;
                this.lastHealthTime = j;
            }

            this.lastHealth = i;
            int k = this.displayHealth;
            this.random.setSeed(this.tickCount * 312871);
            int l = p_283143_.guiWidth() / 2 - 91;
            int i1 = p_283143_.guiWidth() / 2 + 91;
            int j1 = p_283143_.guiHeight() - 39;
            float f = Math.max((float)player.getAttributeValue(Attributes.MAX_HEALTH), Math.max(k, i));
            int k1 = Mth.ceil(player.getAbsorptionAmount());
            int l1 = Mth.ceil((f + k1) / 2.0F / 10.0F);
            int i2 = Math.max(10 - (l1 - 2), 3);
            int j2 = j1 - 10;
            int k2 = -1;
            if (player.hasEffect(MobEffects.REGENERATION)) {
                k2 = this.tickCount % Mth.ceil(f + 5.0F);
            }

            Profiler.get().push("armor");
            renderArmor(p_283143_, player, j1, l1, i2, l);
            Profiler.get().popPush("health");
            this.renderHearts(p_283143_, player, l, j1, i2, k2, f, i, k, k1, flag);
            LivingEntity livingentity = this.getPlayerVehicleWithHealth();
            int l2 = this.getVehicleMaxHearts(livingentity);
            if (l2 == 0) {
                Profiler.get().popPush("food");
                this.renderFood(p_283143_, player, j1, i1);
                j2 -= 10;
            }

            Profiler.get().popPush("air");
            this.renderAirBubbles(p_283143_, player, l2, j2, i1);
            Profiler.get().pop();

            if (appleskin.client.HUDOverlayHandler.INSTANCE != null)
                appleskin.client.HUDOverlayHandler.INSTANCE.onRenderHealth(p_283143_, player, l, j1);
        }
    }

    private static void renderArmor(GuiGraphics p_332897_, Player p_332999_, int p_330861_, int p_331335_, int p_329919_, int p_329454_) {
        int i = p_332999_.getArmorValue();
        if (i > 0) {
            int j = p_330861_ - (p_331335_ - 1) * p_329919_ - 10;

            for (int k = 0; k < 10; k++) {
                int l = p_329454_ + k * 8;
                if (k * 2 + 1 < i) {
                    p_332897_.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_FULL_SPRITE, l, j, 9, 9);
                }

                if (k * 2 + 1 == i) {
                    p_332897_.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_HALF_SPRITE, l, j, 9, 9);
                }

                if (k * 2 + 1 > i) {
                    p_332897_.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_EMPTY_SPRITE, l, j, 9, 9);
                }
            }
        }
    }

    private void renderHearts(
        GuiGraphics p_282497_,
        Player p_168690_,
        int p_168691_,
        int p_168692_,
        int p_168693_,
        int p_168694_,
        float p_168695_,
        int p_168696_,
        int p_168697_,
        int p_168698_,
        boolean p_168699_
    ) {
        Gui.HeartType gui$hearttype = Gui.HeartType.forPlayer(p_168690_);
        boolean flag = p_168690_.level().getLevelData().isHardcore();
        int i = Mth.ceil(p_168695_ / 2.0);
        int j = Mth.ceil(p_168698_ / 2.0);
        int k = i * 2;

        for (int l = i + j - 1; l >= 0; l--) {
            int i1 = l / 10;
            int j1 = l % 10;
            int k1 = p_168691_ + j1 * 8;
            int l1 = p_168692_ - i1 * p_168693_;
            if (p_168696_ + p_168698_ <= 4) {
                l1 += this.random.nextInt(2);
            }

            if (l < i && l == p_168694_) {
                l1 -= 2;
            }

            this.renderHeart(p_282497_, Gui.HeartType.CONTAINER, k1, l1, flag, p_168699_, false);
            int i2 = l * 2;
            boolean flag1 = l >= i;
            if (flag1) {
                int j2 = i2 - k;
                if (j2 < p_168698_) {
                    boolean flag2 = j2 + 1 == p_168698_;
                    this.renderHeart(p_282497_, gui$hearttype == Gui.HeartType.WITHERED ? gui$hearttype : Gui.HeartType.ABSORBING, k1, l1, flag, false, flag2);
                }
            }

            if (p_168699_ && i2 < p_168697_) {
                boolean flag3 = i2 + 1 == p_168697_;
                this.renderHeart(p_282497_, gui$hearttype, k1, l1, flag, true, flag3);
            }

            if (i2 < p_168696_) {
                boolean flag4 = i2 + 1 == p_168696_;
                this.renderHeart(p_282497_, gui$hearttype, k1, l1, flag, false, flag4);
            }
        }
    }

    private void renderHeart(
        GuiGraphics p_283024_, Gui.HeartType p_281393_, int p_283636_, int p_283279_, boolean p_283440_, boolean p_282496_, boolean p_301416_
    ) {
        p_283024_.blitSprite(RenderPipelines.GUI_TEXTURED, p_281393_.getSprite(p_283440_, p_301416_, p_282496_), p_283636_, p_283279_, 9, 9);
    }

    private void renderAirBubbles(GuiGraphics p_362039_, Player p_362951_, int p_361107_, int p_367174_, int p_368454_) {
        int i = p_362951_.getMaxAirSupply();
        int j = Math.clamp(p_362951_.getAirSupply(), 0, i);
        boolean flag = p_362951_.isEyeInFluid(FluidTags.WATER);
        if (flag || j < i) {
            p_367174_ = this.getAirBubbleYLine(p_361107_, p_367174_);
            int k = getCurrentAirSupplyBubble(j, i, -2);
            int l = getCurrentAirSupplyBubble(j, i, 0);
            int i1 = 10 - getCurrentAirSupplyBubble(j, i, getEmptyBubbleDelayDuration(j, flag));
            boolean flag1 = k != l;
            if (!flag) {
                this.lastBubblePopSoundPlayed = 0;
            }

            for (int j1 = 1; j1 <= 10; j1++) {
                int k1 = p_368454_ - (j1 - 1) * 8 - 9;
                if (j1 <= k) {
                    p_362039_.blitSprite(RenderPipelines.GUI_TEXTURED, AIR_SPRITE, k1, p_367174_, 9, 9);
                } else if (flag1 && j1 == l && flag) {
                    p_362039_.blitSprite(RenderPipelines.GUI_TEXTURED, AIR_POPPING_SPRITE, k1, p_367174_, 9, 9);
                    this.playAirBubblePoppedSound(j1, p_362951_, i1);
                } else if (j1 > 10 - i1) {
                    int l1 = i1 == 10 && this.tickCount % 2 == 0 ? this.random.nextInt(2) : 0;
                    p_362039_.blitSprite(RenderPipelines.GUI_TEXTURED, AIR_EMPTY_SPRITE, k1, p_367174_ + l1, 9, 9);
                }
            }
        }
    }

    private int getAirBubbleYLine(int p_366666_, int p_361146_) {
        int i = this.getVisibleVehicleHeartRows(p_366666_) - 1;
        return p_361146_ - i * 10;
    }

    private static int getCurrentAirSupplyBubble(int p_364683_, int p_367314_, int p_368617_) {
        return Mth.ceil((float)((p_364683_ + p_368617_) * 10) / p_367314_);
    }

    private static int getEmptyBubbleDelayDuration(int p_363282_, boolean p_362908_) {
        return p_363282_ != 0 && p_362908_ ? 1 : 0;
    }

    private void playAirBubblePoppedSound(int p_360863_, Player p_365458_, int p_362524_) {
        if (this.lastBubblePopSoundPlayed != p_360863_) {
            float f = 0.5F + 0.1F * Math.max(0, p_362524_ - 3 + 1);
            float f1 = 1.0F + 0.1F * Math.max(0, p_362524_ - 5 + 1);
            p_365458_.playSound(SoundEvents.BUBBLE_POP, f, f1);
            this.lastBubblePopSoundPlayed = p_360863_;
        }
    }

    private void renderFood(GuiGraphics p_330960_, Player p_328268_, int p_331606_, int p_330339_) {
        // AppleSkin: exhaustion underlay (before vanilla food)
        if (appleskin.client.HUDOverlayHandler.INSTANCE != null)
            appleskin.client.HUDOverlayHandler.INSTANCE.onPreRenderFood(p_330960_, p_328268_, p_331606_, p_330339_);

        FoodData fooddata = p_328268_.getFoodData();
        int i = fooddata.getFoodLevel();

        for (int j = 0; j < 10; j++) {
            int k = p_331606_;
            Identifier identifier;
            Identifier identifier1;
            Identifier identifier2;
            if (p_328268_.hasEffect(MobEffects.HUNGER)) {
                identifier = FOOD_EMPTY_HUNGER_SPRITE;
                identifier1 = FOOD_HALF_HUNGER_SPRITE;
                identifier2 = FOOD_FULL_HUNGER_SPRITE;
            } else {
                identifier = FOOD_EMPTY_SPRITE;
                identifier1 = FOOD_HALF_SPRITE;
                identifier2 = FOOD_FULL_SPRITE;
            }

            if (p_328268_.getFoodData().getSaturationLevel() <= 0.0F && this.tickCount % (i * 3 + 1) == 0) {
                k = p_331606_ + (this.random.nextInt(3) - 1);
            }

            int l = p_330339_ - j * 8 - 9;
            p_330960_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, l, k, 9, 9);
            if (j * 2 + 1 < i) {
                p_330960_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier2, l, k, 9, 9);
            }

            if (j * 2 + 1 == i) {
                p_330960_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier1, l, k, 9, 9);
            }
        }

        // AppleSkin: saturation + hunger overlay (after vanilla food)
        if (appleskin.client.HUDOverlayHandler.INSTANCE != null)
            appleskin.client.HUDOverlayHandler.INSTANCE.onRenderFood(p_330960_, p_328268_, p_331606_, p_330339_);
    }

    private void renderVehicleHealth(GuiGraphics p_283368_) {
        LivingEntity livingentity = this.getPlayerVehicleWithHealth();
        if (livingentity != null) {
            int i = this.getVehicleMaxHearts(livingentity);
            if (i != 0) {
                int j = (int)Math.ceil(livingentity.getHealth());
                Profiler.get().popPush("mountHealth");
                int k = p_283368_.guiHeight() - 39;
                int l = p_283368_.guiWidth() / 2 + 91;
                int i1 = k;

                for (int j1 = 0; i > 0; j1 += 20) {
                    int k1 = Math.min(i, 10);
                    i -= k1;

                    for (int l1 = 0; l1 < k1; l1++) {
                        int i2 = l - l1 * 8 - 9;
                        p_283368_.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_VEHICLE_CONTAINER_SPRITE, i2, i1, 9, 9);
                        if (l1 * 2 + 1 + j1 < j) {
                            p_283368_.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_VEHICLE_FULL_SPRITE, i2, i1, 9, 9);
                        }

                        if (l1 * 2 + 1 + j1 == j) {
                            p_283368_.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_VEHICLE_HALF_SPRITE, i2, i1, 9, 9);
                        }
                    }

                    i1 -= 10;
                }
            }
        }
    }

    private void renderTextureOverlay(GuiGraphics p_282304_, Identifier p_457421_, float p_281504_) {
        int i = ARGB.white(p_281504_);
        p_282304_.blit(
            RenderPipelines.GUI_TEXTURED,
            p_457421_,
            0,
            0,
            0.0F,
            0.0F,
            p_282304_.guiWidth(),
            p_282304_.guiHeight(),
            p_282304_.guiWidth(),
            p_282304_.guiHeight(),
            i
        );
    }

    private void renderSpyglassOverlay(GuiGraphics p_282069_, float p_283442_) {
        NoRender mod = (NoRender) Arix.getInstance().getModuleRepo().getModule(NoRender.class);
        if (mod != null && mod.noBadEffects()) return;

        float f = Math.min(p_282069_.guiWidth(), p_282069_.guiHeight());
        float f1 = Math.min(p_282069_.guiWidth() / f, p_282069_.guiHeight() / f) * p_283442_;
        int i = Mth.floor(f * f1);
        int j = Mth.floor(f * f1);
        int k = (p_282069_.guiWidth() - i) / 2;
        int l = (p_282069_.guiHeight() - j) / 2;
        int i1 = k + i;
        int j1 = l + j;
        p_282069_.blit(RenderPipelines.GUI_TEXTURED, SPYGLASS_SCOPE_LOCATION, k, l, 0.0F, 0.0F, i, j, i, j);
        p_282069_.fill(RenderPipelines.GUI, 0, j1, p_282069_.guiWidth(), p_282069_.guiHeight(), -16777216);
        p_282069_.fill(RenderPipelines.GUI, 0, 0, p_282069_.guiWidth(), l, -16777216);
        p_282069_.fill(RenderPipelines.GUI, 0, l, k, j1, -16777216);
        p_282069_.fill(RenderPipelines.GUI, i1, l, p_282069_.guiWidth(), j1, -16777216);
    }

    private void updateVignetteBrightness(Entity p_93021_) {
        BlockPos blockpos = BlockPos.containing(p_93021_.getX(), p_93021_.getEyeY(), p_93021_.getZ());
        float f = LightTexture.getBrightness(p_93021_.level().dimensionType(), p_93021_.level().getMaxLocalRawBrightness(blockpos));
        float f1 = Mth.clamp(1.0F - f, 0.0F, 1.0F);
        this.vignetteBrightness = this.vignetteBrightness + (f1 - this.vignetteBrightness) * 0.01F;
    }

    private void renderVignette(GuiGraphics p_283063_,  Entity p_283439_) {
        NoRender mod = (NoRender) Arix.getInstance().getModuleRepo().getModule(NoRender.class);
        if (mod != null && mod.noBadEffects()) return;

        if (!Config.isVignetteEnabled()) {
            GlStateManager._enableDepthTest();
            GlStateManager._blendFuncSeparate(770, 771, 1, 0);
        } else {
            WorldBorder worldborder = this.minecraft.level.getWorldBorder();
            float f = 0.0F;
            if (p_283439_ != null) {
                float f1 = (float)worldborder.getDistanceToBorder(p_283439_);
                double d0 = Math.min(worldborder.getLerpSpeed() * worldborder.getWarningTime(), Math.abs(worldborder.getLerpTarget() - worldborder.getSize()));
                double d1 = Math.max(worldborder.getWarningBlocks(), d0);
                if (f1 < d1) {
                    f = 1.0F - (float)(f1 / d1);
                }
            }

            int i;
            if (f > 0.0F) {
                f = Mth.clamp(f, 0.0F, 1.0F);
                i = ARGB.colorFromFloat(1.0F, 0.0F, f, f);
            } else {
                float f2 = this.vignetteBrightness;
                f2 = Mth.clamp(f2, 0.0F, 1.0F);
                i = ARGB.colorFromFloat(1.0F, f2, f2, f2);
            }

            p_283063_.blit(
                RenderPipelines.VIGNETTE,
                VIGNETTE_LOCATION,
                0,
                0,
                0.0F,
                0.0F,
                p_283063_.guiWidth(),
                p_283063_.guiHeight(),
                p_283063_.guiWidth(),
                p_283063_.guiHeight(),
                i
            );
        }
    }

    private void renderPortalOverlay(GuiGraphics p_283375_, float p_283296_) {
        NoRender mod = (NoRender) Arix.getInstance().getModuleRepo().getModule(NoRender.class);
        if (mod != null && mod.noBadEffects()) return;

        if (p_283296_ < 1.0F) {
            p_283296_ *= p_283296_;
            p_283296_ *= p_283296_;
            p_283296_ = p_283296_ * 0.8F + 0.2F;
        }

        int i = ARGB.white(p_283296_);
        TextureAtlasSprite textureatlassprite = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.NETHER_PORTAL.defaultBlockState());
        p_283375_.blitSprite(RenderPipelines.GUI_TEXTURED, textureatlassprite, 0, 0, p_283375_.guiWidth(), p_283375_.guiHeight(), i);
    }

    private void renderConfusionOverlay(GuiGraphics p_365616_, float p_366912_) {
        NoRender mod = (NoRender) Arix.getInstance().getModuleRepo().getModule(NoRender.class);
        if (mod != null && mod.noBadEffects()) return;

        int i = p_365616_.guiWidth();
        int j = p_365616_.guiHeight();
        p_365616_.pose().pushMatrix();
        float f = Mth.lerp(p_366912_, 2.0F, 1.0F);
        p_365616_.pose().translate(i / 2.0F, j / 2.0F);
        p_365616_.pose().scale(f, f);
        p_365616_.pose().translate(-i / 2.0F, -j / 2.0F);
        float f1 = 0.2F * p_366912_;
        float f2 = 0.4F * p_366912_;
        float f3 = 0.2F * p_366912_;
        p_365616_.blit(RenderPipelines.GUI_NAUSEA_OVERLAY, NAUSEA_LOCATION, 0, 0, 0.0F, 0.0F, i, j, i, j, ARGB.colorFromFloat(1.0F, f1, f2, f3));
        p_365616_.pose().popMatrix();
    }

    private void renderSlot(GuiGraphics p_283283_, int p_283213_, int p_281301_, DeltaTracker p_344149_, Player p_283644_, ItemStack p_283317_, int p_283261_) {
        if (!p_283317_.isEmpty()) {
            float f = p_283317_.getPopTime() - p_344149_.getGameTimeDeltaPartialTick(false);
            if (f > 0.0F) {
                float f1 = 1.0F + f / 5.0F;
                p_283283_.pose().pushMatrix();
                p_283283_.pose().translate(p_283213_ + 8, p_281301_ + 12);
                p_283283_.pose().scale(1.0F / f1, (f1 + 1.0F) / 2.0F);
                p_283283_.pose().translate(-(p_283213_ + 8), -(p_281301_ + 12));
            }

            p_283283_.renderItem(p_283644_, p_283317_, p_283213_, p_281301_, p_283261_);
            if (f > 0.0F) {
                p_283283_.pose().popMatrix();
            }

            p_283283_.renderItemDecorations(this.minecraft.font, p_283317_, p_283213_, p_281301_);
        }
    }

    public void tick(boolean p_193833_) {
        this.tickAutosaveIndicator();
        if (!p_193833_) {
            this.tick();
        }
    }

    private void tick() {
        if (this.minecraft.level == null) {
            TextureAnimations.updateAnimations();
        }

        if (this.overlayMessageTime > 0) {
            this.overlayMessageTime--;
        }

        if (this.titleTime > 0) {
            this.titleTime--;
            if (this.titleTime <= 0) {
                this.title = null;
                this.subtitle = null;
            }
        }

        this.tickCount++;
        Entity entity = this.minecraft.getCameraEntity();
        if (entity != null) {
            this.updateVignetteBrightness(entity);
        }

        if (this.minecraft.player != null) {
            ItemStack itemstack = this.minecraft.player.getInventory().getSelectedItem();
            boolean flag = true;
            if (Reflector.IForgeItemStack_getHighlightTip.exists()) {
                Component component = (Component)Reflector.call(itemstack, Reflector.IForgeItemStack_getHighlightTip, itemstack.getHoverName());
                Component component1 = (Component)Reflector.call(this.lastToolHighlight, Reflector.IForgeItemStack_getHighlightTip, this.lastToolHighlight.getHoverName());
                flag = Config.equals(component, component1);
            }

            if (itemstack.isEmpty()) {
                this.toolHighlightTimer = 0;
            } else if (this.lastToolHighlight.isEmpty()
                || !itemstack.is(this.lastToolHighlight.getItem())
                || !itemstack.getHoverName().equals(this.lastToolHighlight.getHoverName())
                || !flag) {
                this.toolHighlightTimer = (int)(40.0 * this.minecraft.options.notificationDisplayTime().get());
            } else if (this.toolHighlightTimer > 0) {
                this.toolHighlightTimer--;
            }

            this.lastToolHighlight = itemstack;
        }

        this.chat.tick();
    }

    private void tickAutosaveIndicator() {
        MinecraftServer minecraftserver = this.minecraft.getSingleplayerServer();
        boolean flag = minecraftserver != null && minecraftserver.isCurrentlySaving();
        this.lastAutosaveIndicatorValue = this.autosaveIndicatorValue;
        this.autosaveIndicatorValue = Mth.lerp(0.2F, this.autosaveIndicatorValue, flag ? 1.0F : 0.0F);
    }

    public void setNowPlaying(Component p_93056_) {
        Component component = Component.translatable("record.nowPlaying", p_93056_);
        this.setOverlayMessage(component, true);
        this.minecraft.getNarrator().saySystemNow(component);
    }

    public void setOverlayMessage(Component p_93064_, boolean p_93065_) {
        this.setChatDisabledByPlayerShown(false);
        this.overlayMessageString = p_93064_;
        this.overlayMessageTime = 60;
        this.animateOverlayMessageColor = p_93065_;
    }

    public void setChatDisabledByPlayerShown(boolean p_238398_) {
        this.chatDisabledByPlayerShown = p_238398_;
    }

    public boolean isShowingChatDisabledByPlayer() {
        return this.chatDisabledByPlayerShown && this.overlayMessageTime > 0;
    }

    public void setTimes(int p_168685_, int p_168686_, int p_168687_) {
        if (p_168685_ >= 0) {
            this.titleFadeInTime = p_168685_;
        }

        if (p_168686_ >= 0) {
            this.titleStayTime = p_168686_;
        }

        if (p_168687_ >= 0) {
            this.titleFadeOutTime = p_168687_;
        }

        if (this.titleTime > 0) {
            this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
        }
    }

    public void setSubtitle(Component p_168712_) {
        this.subtitle = p_168712_;
    }

    public void setTitle(Component p_168715_) {
        this.title = p_168715_;
        this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
    }

    public void clearTitles() {
        this.title = null;
        this.subtitle = null;
        this.titleTime = 0;
    }

    public ChatComponent getChat() {
        return this.chat;
    }

    public int getGuiTicks() {
        return this.tickCount;
    }

    public Font getFont() {
        return this.minecraft.font;
    }

    public SpectatorGui getSpectatorGui() {
        return this.spectatorGui;
    }

    public PlayerTabOverlay getTabList() {
        return this.tabList;
    }

    public void onDisconnected() {
        this.tabList.reset();
        this.bossOverlay.reset();
        this.minecraft.getToastManager().clear();
        this.debugOverlay.reset();
        this.chat.clearMessages(true);
        this.clearTitles();
        this.resetTitleTimes();
    }

    public BossHealthOverlay getBossOverlay() {
        return this.bossOverlay;
    }

    public DebugScreenOverlay getDebugOverlay() {
        return this.debugOverlay;
    }

    public void clearCache() {
        this.debugOverlay.clearChunkCache();
    }

    public void renderSavingIndicator(GuiGraphics p_282761_, DeltaTracker p_344404_) {
        if (this.minecraft.options.showAutosaveIndicator().get() && (this.autosaveIndicatorValue > 0.0F || this.lastAutosaveIndicatorValue > 0.0F)) {
            int i = Mth.floor(255.0F * Mth.clamp(Mth.lerp(p_344404_.getRealtimeDeltaTicks(), this.lastAutosaveIndicatorValue, this.autosaveIndicatorValue), 0.0F, 1.0F));
            if (i > 0) {
                Font font = this.getFont();
                int j = font.width(SAVING_TEXT);
                int k = ARGB.color(i, -1);
                int l = p_282761_.guiWidth() - j - 5;
                int i1 = p_282761_.guiHeight() - 9 - 5;
                p_282761_.nextStratum();
                p_282761_.drawStringWithBackdrop(font, SAVING_TEXT, l, i1, j, k);
            }
        }
    }

    private boolean willPrioritizeExperienceInfo() {
        return this.minecraft.player.experienceDisplayStartTick + 100 > this.minecraft.player.tickCount;
    }

    private boolean willPrioritizeJumpInfo() {
        return this.minecraft.player.getJumpRidingScale() > 0.0F || Optionull.mapOrDefault(this.minecraft.player.jumpableVehicle(), PlayerRideableJumping::getJumpCooldown, 0) > 0;
    }

    private Gui.ContextualInfo nextContextualInfoState() {
        boolean flag = this.minecraft.player.connection.getWaypointManager().hasWaypoints();
        boolean flag1 = this.minecraft.player.jumpableVehicle() != null;
        boolean flag2 = this.minecraft.gameMode.hasExperience();
        if (flag) {
            if (flag1 && this.willPrioritizeJumpInfo()) {
                return Gui.ContextualInfo.JUMPABLE_VEHICLE;
            } else {
                return flag2 && this.willPrioritizeExperienceInfo() ? Gui.ContextualInfo.EXPERIENCE : Gui.ContextualInfo.LOCATOR;
            }
        } else if (flag1) {
            return Gui.ContextualInfo.JUMPABLE_VEHICLE;
        } else {
            return flag2 ? Gui.ContextualInfo.EXPERIENCE : Gui.ContextualInfo.EMPTY;
        }
    }

    enum ContextualInfo {
        EMPTY,
        EXPERIENCE,
        LOCATOR,
        JUMPABLE_VEHICLE;
    }

    enum HeartType {
        CONTAINER(
            Identifier.withDefaultNamespace("hud/heart/container"),
            Identifier.withDefaultNamespace("hud/heart/container_blinking"),
            Identifier.withDefaultNamespace("hud/heart/container"),
            Identifier.withDefaultNamespace("hud/heart/container_blinking"),
            Identifier.withDefaultNamespace("hud/heart/container_hardcore"),
            Identifier.withDefaultNamespace("hud/heart/container_hardcore_blinking"),
            Identifier.withDefaultNamespace("hud/heart/container_hardcore"),
            Identifier.withDefaultNamespace("hud/heart/container_hardcore_blinking")
        ),
        NORMAL(
            Identifier.withDefaultNamespace("hud/heart/full"),
            Identifier.withDefaultNamespace("hud/heart/full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/half"),
            Identifier.withDefaultNamespace("hud/heart/half_blinking"),
            Identifier.withDefaultNamespace("hud/heart/hardcore_full"),
            Identifier.withDefaultNamespace("hud/heart/hardcore_full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/hardcore_half"),
            Identifier.withDefaultNamespace("hud/heart/hardcore_half_blinking")
        ),
        POISIONED(
            Identifier.withDefaultNamespace("hud/heart/poisoned_full"),
            Identifier.withDefaultNamespace("hud/heart/poisoned_full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/poisoned_half"),
            Identifier.withDefaultNamespace("hud/heart/poisoned_half_blinking"),
            Identifier.withDefaultNamespace("hud/heart/poisoned_hardcore_full"),
            Identifier.withDefaultNamespace("hud/heart/poisoned_hardcore_full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/poisoned_hardcore_half"),
            Identifier.withDefaultNamespace("hud/heart/poisoned_hardcore_half_blinking")
        ),
        WITHERED(
            Identifier.withDefaultNamespace("hud/heart/withered_full"),
            Identifier.withDefaultNamespace("hud/heart/withered_full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/withered_half"),
            Identifier.withDefaultNamespace("hud/heart/withered_half_blinking"),
            Identifier.withDefaultNamespace("hud/heart/withered_hardcore_full"),
            Identifier.withDefaultNamespace("hud/heart/withered_hardcore_full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/withered_hardcore_half"),
            Identifier.withDefaultNamespace("hud/heart/withered_hardcore_half_blinking")
        ),
        ABSORBING(
            Identifier.withDefaultNamespace("hud/heart/absorbing_full"),
            Identifier.withDefaultNamespace("hud/heart/absorbing_full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/absorbing_half"),
            Identifier.withDefaultNamespace("hud/heart/absorbing_half_blinking"),
            Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_full"),
            Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_half"),
            Identifier.withDefaultNamespace("hud/heart/absorbing_hardcore_half_blinking")
        ),
        FROZEN(
            Identifier.withDefaultNamespace("hud/heart/frozen_full"),
            Identifier.withDefaultNamespace("hud/heart/frozen_full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/frozen_half"),
            Identifier.withDefaultNamespace("hud/heart/frozen_half_blinking"),
            Identifier.withDefaultNamespace("hud/heart/frozen_hardcore_full"),
            Identifier.withDefaultNamespace("hud/heart/frozen_hardcore_full_blinking"),
            Identifier.withDefaultNamespace("hud/heart/frozen_hardcore_half"),
            Identifier.withDefaultNamespace("hud/heart/frozen_hardcore_half_blinking")
        );

        private final Identifier full;
        private final Identifier fullBlinking;
        private final Identifier half;
        private final Identifier halfBlinking;
        private final Identifier hardcoreFull;
        private final Identifier hardcoreFullBlinking;
        private final Identifier hardcoreHalf;
        private final Identifier hardcoreHalfBlinking;

        HeartType(
            final Identifier p_451192_,
            final Identifier p_450281_,
            final Identifier p_457874_,
            final Identifier p_457783_,
            final Identifier p_459313_,
            final Identifier p_457879_,
            final Identifier p_459484_,
            final Identifier p_454594_
        ) {
            this.full = p_451192_;
            this.fullBlinking = p_450281_;
            this.half = p_457874_;
            this.halfBlinking = p_457783_;
            this.hardcoreFull = p_459313_;
            this.hardcoreFullBlinking = p_457879_;
            this.hardcoreHalf = p_459484_;
            this.hardcoreHalfBlinking = p_454594_;
        }

        public Identifier getSprite(boolean p_297692_, boolean p_299675_, boolean p_299889_) {
            if (!p_297692_) {
                if (p_299675_) {
                    return p_299889_ ? this.halfBlinking : this.half;
                } else {
                    return p_299889_ ? this.fullBlinking : this.full;
                }
            } else if (p_299675_) {
                return p_299889_ ? this.hardcoreHalfBlinking : this.hardcoreHalf;
            } else {
                return p_299889_ ? this.hardcoreFullBlinking : this.hardcoreFull;
            }
        }

        static Gui.HeartType forPlayer(Player p_168733_) {
            Gui.HeartType gui$hearttype;
            if (p_168733_.hasEffect(MobEffects.POISON)) {
                gui$hearttype = POISIONED;
            } else if (p_168733_.hasEffect(MobEffects.WITHER)) {
                gui$hearttype = WITHERED;
            } else if (p_168733_.isFullyFrozen()) {
                gui$hearttype = FROZEN;
            } else {
                gui$hearttype = NORMAL;
            }

            return gui$hearttype;
        }
    }

    public interface RenderFunction {
        void render(GuiGraphics p_405987_, DeltaTracker p_407092_);
    }
}
