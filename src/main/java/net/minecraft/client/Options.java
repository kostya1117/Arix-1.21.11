package net.minecraft.client;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.renderer.GpuWarnlistManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.SoundPreviewHandler;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LenientJsonParser;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomGuis;
import net.optifine.CustomSky;
import net.optifine.DynamicLights;
import net.optifine.Lang;
import net.optifine.Log;
import net.optifine.NaturalTextures;
import net.optifine.config.FloatOptions;
import net.optifine.config.IPersitentOption;
import net.optifine.config.Option;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import net.optifine.util.ArrayUtils;
import net.optifine.util.FontUtils;
import net.optifine.util.KeyUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Options {
    static final Logger LOGGER = LogUtils.getLogger();
    static final Gson GSON = new Gson();
    private static final TypeToken<List<String>> LIST_OF_STRINGS_TYPE = new TypeToken<List<String>>() {};
    public static final int RENDER_DISTANCE_SHORT = 4;
    public static final int RENDER_DISTANCE_FAR = 12;
    public static final int RENDER_DISTANCE_REALLY_FAR = 16;
    public static final int RENDER_DISTANCE_EXTREME = 32;
    private static final Splitter OPTION_SPLITTER = Splitter.on(':').limit(2);
    public static final String DEFAULT_SOUND_DEVICE = "";
    private static final Component ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND = Component.translatable("options.darkMojangStudiosBackgroundColor.tooltip");
    private final OptionInstance<Boolean> darkMojangStudiosBackground = OptionInstance.createBoolean(
        "options.darkMojangStudiosBackgroundColor", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND), false
    );
    private static final Component ACCESSIBILITY_TOOLTIP_HIDE_LIGHTNING_FLASHES = Component.translatable("options.hideLightningFlashes.tooltip");
    private final OptionInstance<Boolean> hideLightningFlash = OptionInstance.createBoolean("options.hideLightningFlashes", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_HIDE_LIGHTNING_FLASHES), false);
    private static final Component ACCESSIBILITY_TOOLTIP_HIDE_SPLASH_TEXTS = Component.translatable("options.hideSplashTexts.tooltip");
    private final OptionInstance<Boolean> hideSplashTexts = OptionInstance.createBoolean("options.hideSplashTexts", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_HIDE_SPLASH_TEXTS), false);
    private final OptionInstance<Double> sensitivity = new OptionInstance<>("options.sensitivity", OptionInstance.noTooltip(), (p_232095_0_, p_232095_1_) -> {
        if (p_232095_1_ == 0.0) {
            return genericValueLabel(p_232095_0_, Component.translatable("options.sensitivity.min"));
        } else {
            return p_232095_1_ == 1.0 ? genericValueLabel(p_232095_0_, Component.translatable("options.sensitivity.max")) : percentValueLabel(p_232095_0_, 2.0 * p_232095_1_);
        }
    }, OptionInstance.UnitDouble.INSTANCE, 0.5, p_232114_0_ -> {});
    private final OptionInstance<Integer> renderDistance;
    private final OptionInstance<Integer> simulationDistance;
    private int serverRenderDistance = 0;
    private final OptionInstance<Double> entityDistanceScaling = new OptionInstance<>(
        "options.entityDistanceScaling",
        OptionInstance.noTooltip(),
        Options::percentValueLabel,
        new OptionInstance.IntRange(2, 20).xmap(p_232019_0_ -> p_232019_0_ / 4.0, p_232053_0_ -> (int)(p_232053_0_ * 4.0), true),
        Codec.doubleRange(0.5, 5.0),
        1.0,
        p_438311_1_ -> this.setGraphicsPresetToCustom()
    );
    public static final int UNLIMITED_FRAMERATE_CUTOFF = 260;
    private final OptionInstance<Integer> framerateLimit = new OptionInstance<>(
        "options.framerateLimit",
        OptionInstance.noTooltip(),
        (p_232047_0_, p_232047_1_) -> {
            if (this.enableVsync().get()) {
                return genericValueLabel(p_232047_0_, Component.translatable("of.options.framerateLimit.vsync"));
            } else {
                return p_232047_1_ == 260
                    ? genericValueLabel(p_232047_0_, Component.translatable("options.framerateLimit.max"))
                    : genericValueLabel(p_232047_0_, Component.translatable("options.framerate", p_232047_1_));
            }
        },
        new OptionInstance.IntRange(0, 52).xmap(p_232002_0_ -> p_232002_0_ * 5, p_232093_0_ -> p_232093_0_ / 5, true),
        Codec.intRange(0, 260),
        120,
        p_348924_0_ -> {
            this.enableVsync().set(p_348924_0_ == 0);
            Minecraft.getInstance().getFramerateLimitTracker().setFramerateLimit(p_348924_0_);
        }
    );
    private boolean isApplyingGraphicsPreset;
    private final OptionInstance<GraphicsPreset> graphicsPreset = new OptionInstance<>(
        "options.graphics.preset",
        OptionInstance.cachedConstantTooltip(Component.translatable("options.graphics.preset.tooltip")),
        (p_438294_0_, p_438294_1_) -> genericValueLabel(p_438294_0_, Component.translatable(p_438294_1_.getKey())),
        new OptionInstance.SliderableEnum<>(List.of(GraphicsPreset.values()), GraphicsPreset.CODEC),
        GraphicsPreset.CODEC,
        GraphicsPreset.FANCY,
        this::applyGraphicsPreset
    );
    private static final Component INACTIVITY_FPS_LIMIT_TOOLTIP_MINIMIZED = Component.translatable("options.inactivityFpsLimit.minimized.tooltip");
    private static final Component INACTIVITY_FPS_LIMIT_TOOLTIP_AFK = Component.translatable("options.inactivityFpsLimit.afk.tooltip");
    private final OptionInstance<InactivityFpsLimit> inactivityFpsLimit = new OptionInstance<>(
        "options.inactivityFpsLimit",
        p_348922_0_ -> {
            return switch (p_348922_0_) {
                case MINIMIZED -> Tooltip.create(INACTIVITY_FPS_LIMIT_TOOLTIP_MINIMIZED);
                case AFK -> Tooltip.create(INACTIVITY_FPS_LIMIT_TOOLTIP_AFK);
            };
        },
        (p_438324_0_, p_438324_1_) -> p_438324_1_.caption(),
        new OptionInstance.Enum<>(Arrays.asList(InactivityFpsLimit.values()), InactivityFpsLimit.CODEC),
        InactivityFpsLimit.AFK,
        p_232023_0_ -> {}
    );
    private final OptionInstance<CloudStatus> cloudStatus = new OptionInstance<>(
        "options.renderClouds",
        OptionInstance.noTooltip(),
        (p_438322_0_, p_438322_1_) -> p_438322_1_.caption(),
        new OptionInstance.Enum<>(
            Arrays.asList(CloudStatus.values()),
            Codec.withAlternative(CloudStatus.CODEC, Codec.BOOL, p_232081_0_ -> p_232081_0_ ? CloudStatus.FANCY : CloudStatus.OFF)
        ),
        CloudStatus.FANCY,
        p_438328_1_ -> this.setGraphicsPresetToCustom()
    );
    private final OptionInstance<Integer> cloudRange = new OptionInstance<>(
        "options.renderCloudsDistance",
        OptionInstance.noTooltip(),
        (p_263859_0_, p_263859_1_) -> genericValueLabel(p_263859_0_, Component.translatable("options.chunks", p_263859_1_)),
        new OptionInstance.IntRange(2, 128, true),
        128,
        p_438289_1_ -> {
            operateOnLevelRenderer(p_438326_0_ -> p_438326_0_.getCloudRenderer().markForRebuild());
            this.setGraphicsPresetToCustom();
        }
    );
    private static final Component GRAPHICS_TOOLTIP_WEATHER_RADIUS = Component.translatable("options.weatherRadius.tooltip");
    private final OptionInstance<Integer> weatherRadius = new OptionInstance<>(
        "options.weatherRadius",
        OptionInstance.noTooltip(),
        (p_401251_0_, p_401251_1_) -> p_401251_1_ < 3 ? genericValueLabel(p_401251_0_, CommonComponents.OPTION_OFF) : genericValueLabel(p_401251_0_, p_401251_1_),
        new OptionInstance.IntRange(2, 10, true),
        10,
        p_438309_1_ -> this.setGraphicsPresetToCustom()
    );
    private static final Component GRAPHICS_TOOLTIP_CUTOUT_LEAVES = Component.translatable("options.cutoutLeaves.tooltip");
    private final OptionInstance<Boolean> cutoutLeaves = OptionInstance.createBoolean(
        "options.cutoutLeaves", OptionInstance.cachedConstantTooltip(GRAPHICS_TOOLTIP_CUTOUT_LEAVES), true, p_438316_1_ -> {
            operateOnLevelRenderer(LevelRenderer::allChanged);
            this.setGraphicsPresetToCustom();
        }
    );
    private static final Component GRAPHICS_TOOLTIP_VIGNETTE = Component.translatable("options.vignette.tooltip");
    private final OptionInstance<Boolean> vignette = OptionInstance.createBoolean("options.vignette", OptionInstance.noTooltip(), true);
    private static final Component GRAPHICS_TOOLTIP_IMPROVED_TRANSPARENCY = Component.translatable("options.improvedTransparency.tooltip");
    private final OptionInstance<Boolean> improvedTransparency = OptionInstance.createBoolean(
        "options.improvedTransparency", OptionInstance.noTooltip(), false, p_438318_1_ -> {
            Minecraft minecraft = Minecraft.getInstance();
            GpuWarnlistManager gpuwarnlistmanager = minecraft.getGpuWarnlistManager();
            if (p_438318_1_ && gpuwarnlistmanager.willShowWarning()) {
                gpuwarnlistmanager.showWarning();
            } else {
                operateOnLevelRenderer(LevelRenderer::allChanged);
                this.setGraphicsPresetToCustom();
            }
        }
    );
    private final OptionInstance<Boolean> ambientOcclusion = OptionInstance.createBoolean("options.ao", true, p_438319_1_ -> {
        operateOnLevelRenderer(LevelRenderer::allChanged);
        this.setGraphicsPresetToCustom();
    });
    private static final Component GRAPHICS_TOOLTIP_CHUNK_FADE = Component.translatable("options.chunkFade.tooltip");
    private final OptionInstance<Double> chunkSectionFadeInTime = new OptionInstance<>(
        "options.chunkFade",
        OptionInstance.cachedConstantTooltip(GRAPHICS_TOOLTIP_CHUNK_FADE),
        (p_241715_0_, p_241715_1_) -> p_241715_1_ <= 0.0
            ? Component.translatable("options.chunkFade.none")
            : Component.translatable("options.chunkFade.seconds", String.format(Locale.ROOT, "%.2f", p_241715_1_)),
        new OptionInstance.IntRange(0, 40).xmap(p_231985_0_ -> p_231985_0_ / 20.0, p_232111_0_ -> (int)(p_232111_0_ * 20.0), true),
        Codec.doubleRange(0.0, 2.0),
        0.75,
        p_231987_0_ -> {}
    );
    private static final Component PRIORITIZE_CHUNK_TOOLTIP_NONE = Component.translatable("options.prioritizeChunkUpdates.none.tooltip");
    private static final Component PRIORITIZE_CHUNK_TOOLTIP_PLAYER_AFFECTED = Component.translatable("options.prioritizeChunkUpdates.byPlayer.tooltip");
    private static final Component PRIORITIZE_CHUNK_TOOLTIP_NEARBY = Component.translatable("options.prioritizeChunkUpdates.nearby.tooltip");
    private final OptionInstance<PrioritizeChunkUpdates> prioritizeChunkUpdates = new OptionInstance<>(
        "options.prioritizeChunkUpdates",
        p_317297_0_ -> {
            if (Boolean.TRUE) {
                return null;
            }

            return switch (p_317297_0_) {
                case NONE -> Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_NONE);
                case PLAYER_AFFECTED -> Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_PLAYER_AFFECTED);
                case NEARBY -> Tooltip.create(PRIORITIZE_CHUNK_TOOLTIP_NEARBY);
            };
        },
        (p_438312_0_, p_438312_1_) -> p_438312_1_.caption(),
        new OptionInstance.Enum<>(Arrays.asList(PrioritizeChunkUpdates.values()), PrioritizeChunkUpdates.LEGACY_CODEC),
        PrioritizeChunkUpdates.NONE,
        p_438295_1_ -> this.setGraphicsPresetToCustom()
    );
    public List<String> resourcePacks = Lists.newArrayList();
    public List<String> incompatibleResourcePacks = Lists.newArrayList();
    private final OptionInstance<ChatVisiblity> chatVisibility = new OptionInstance<>(
        "options.chat.visibility",
        OptionInstance.noTooltip(),
        (p_438302_0_, p_438302_1_) -> p_438302_1_.caption(),
        new OptionInstance.Enum<>(Arrays.asList(ChatVisiblity.values()), ChatVisiblity.LEGACY_CODEC),
        ChatVisiblity.FULL,
        p_231946_0_ -> {}
    );
    private final OptionInstance<Double> chatOpacity = new OptionInstance<>(
        "options.chat.opacity",
        OptionInstance.noTooltip(),
        (p_232087_0_, p_232087_1_) -> percentValueLabel(p_232087_0_, p_232087_1_ * 0.9 + 0.1),
        OptionInstance.UnitDouble.INSTANCE,
        1.0,
        p_232105_0_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatLineSpacing = new OptionInstance<>(
        "options.chat.line_spacing", OptionInstance.noTooltip(), Options::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 0.0, p_231874_0_ -> {}
    );
    private static final Component MENU_BACKGROUND_BLURRINESS_TOOLTIP = Component.translatable("options.accessibility.menu_background_blurriness.tooltip");
    private static final int BLURRINESS_DEFAULT_VALUE = 5;
    private final OptionInstance<Integer> menuBackgroundBlurriness = new OptionInstance<>(
        "options.accessibility.menu_background_blurriness",
        OptionInstance.cachedConstantTooltip(MENU_BACKGROUND_BLURRINESS_TOOLTIP),
        Options::genericValueOrOffLabel,
        new OptionInstance.IntRange(0, 10),
        5,
        p_438296_1_ -> this.setGraphicsPresetToCustom()
    );
    private final OptionInstance<Double> textBackgroundOpacity = new OptionInstance<>(
        "options.accessibility.text_background_opacity",
        OptionInstance.noTooltip(),
        Options::percentValueLabel,
        OptionInstance.UnitDouble.INSTANCE,
        0.5,
        p_232099_0_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> panoramaSpeed = new OptionInstance<>(
        "options.accessibility.panorama_speed", OptionInstance.noTooltip(), Options::percentValueLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, p_231989_0_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_CONTRAST_MODE = Component.translatable("options.accessibility.high_contrast.tooltip");
    private final OptionInstance<Boolean> highContrast = OptionInstance.createBoolean(
        "options.accessibility.high_contrast", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_CONTRAST_MODE), false, p_275764_1_ -> {
            PackRepository packrepository = Minecraft.getInstance().getResourcePackRepository();
            boolean flag1 = packrepository.getSelectedIds().contains("high_contrast");
            if (!flag1 && p_275764_1_) {
                if (packrepository.addPack("high_contrast")) {
                    this.updateResourcePacks(packrepository);
                }
            } else if (flag1 && !p_275764_1_ && packrepository.removePack("high_contrast")) {
                this.updateResourcePacks(packrepository);
            }
        }
    );
    private static final Component HIGH_CONTRAST_BLOCK_OUTLINE_TOOLTIP = Component.translatable("options.accessibility.high_contrast_block_outline.tooltip");
    private final OptionInstance<Boolean> highContrastBlockOutline = OptionInstance.createBoolean(
        "options.accessibility.high_contrast_block_outline", OptionInstance.cachedConstantTooltip(HIGH_CONTRAST_BLOCK_OUTLINE_TOOLTIP), false
    );
    private final OptionInstance<Boolean> narratorHotkey = OptionInstance.createBoolean(
        "options.accessibility.narrator_hotkey",
        OptionInstance.cachedConstantTooltip(
            InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY
                ? Component.translatable("options.accessibility.narrator_hotkey.mac.tooltip")
                : Component.translatable("options.accessibility.narrator_hotkey.tooltip")
        ),
        true
    );
    public @Nullable String fullscreenVideoModeString;
    public boolean hideServerAddress;
    public boolean advancedItemTooltips;
    public boolean pauseOnLostFocus = true;
    private final Set<PlayerModelPart> modelParts = EnumSet.allOf(PlayerModelPart.class);
    private final OptionInstance<HumanoidArm> mainHand = new OptionInstance<>(
        "options.mainHand",
        OptionInstance.noTooltip(),
        (p_438320_0_, p_438320_1_) -> p_438320_1_.caption(),
        new OptionInstance.Enum<>(Arrays.asList(HumanoidArm.values()), HumanoidArm.CODEC),
        HumanoidArm.RIGHT,
        p_231971_0_ -> {}
    );
    public int overrideWidth;
    public int overrideHeight;
    private final OptionInstance<Double> chatScale = new OptionInstance<>(
        "options.chat.scale",
        OptionInstance.noTooltip(),
        (p_232077_0_, p_232077_1_) -> p_232077_1_ == 0.0 ? CommonComponents.optionStatus(p_232077_0_, false) : percentValueLabel(p_232077_0_, p_232077_1_),
        OptionInstance.UnitDouble.INSTANCE,
        1.0,
        p_232091_0_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatWidth = new OptionInstance<>(
        "options.chat.width",
        OptionInstance.noTooltip(),
        (p_232067_0_, p_232067_1_) -> pixelValueLabel(p_232067_0_, (int)(ChatComponent.getWidth(p_232067_1_) / 4.0571431)),
        OptionInstance.UnitDouble.INSTANCE,
        1.0,
        p_232083_0_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatHeightUnfocused = new OptionInstance<>(
        "options.chat.height.unfocused",
        OptionInstance.noTooltip(),
        (p_232057_0_, p_232057_1_) -> pixelValueLabel(p_232057_0_, ChatComponent.getHeight(p_232057_1_)),
        OptionInstance.UnitDouble.INSTANCE,
        ChatComponent.defaultUnfocusedPct(),
        p_232073_0_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatHeightFocused = new OptionInstance<>(
        "options.chat.height.focused",
        OptionInstance.noTooltip(),
        (p_232044_0_, p_232044_1_) -> pixelValueLabel(p_232044_0_, ChatComponent.getHeight(p_232044_1_)),
        OptionInstance.UnitDouble.INSTANCE,
        1.0,
        p_232063_0_ -> Minecraft.getInstance().gui.getChat().rescaleChat()
    );
    private final OptionInstance<Double> chatDelay = new OptionInstance<>(
        "options.chat.delay_instant",
        OptionInstance.noTooltip(),
        (p_438297_0_, p_438297_1_) -> p_438297_1_ <= 0.0
            ? Component.translatable("options.chat.delay_none")
            : Component.translatable("options.chat.delay", String.format(Locale.ROOT, "%.1f", p_438297_1_)),
        new OptionInstance.IntRange(0, 60).xmap(p_263860_0_ -> p_263860_0_ / 10.0, p_263861_0_ -> (int)(p_263861_0_ * 10.0), true),
        Codec.doubleRange(0.0, 6.0),
        0.0,
        p_240679_0_ -> Minecraft.getInstance().getChatListener().setMessageDelay(p_240679_0_)
    );
    private static final Component ACCESSIBILITY_TOOLTIP_NOTIFICATION_DISPLAY_TIME = Component.translatable("options.notifications.display_time.tooltip");
    private final OptionInstance<Double> notificationDisplayTime = new OptionInstance<>(
        "options.notifications.display_time",
        OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_NOTIFICATION_DISPLAY_TIME),
        (p_438307_0_, p_438307_1_) -> genericValueLabel(p_438307_0_, Component.translatable("options.multiplier", p_438307_1_)),
        new OptionInstance.IntRange(5, 100).xmap(p_438315_0_ -> p_438315_0_ / 10.0, p_438304_0_ -> (int)(p_438304_0_ * 10.0), true),
        Codec.doubleRange(0.5, 10.0),
        1.0,
        p_231948_0_ -> {}
    );
    private final OptionInstance<Integer> mipmapLevels = new OptionInstance<>("options.mipmapLevels", OptionInstance.noTooltip(), (p_232032_0_, p_232032_1_) -> {
        if (p_232032_1_.intValue() >= 4.0) {
            return genericValueLabel(p_232032_0_, Component.translatable("of.general.max"));
        } else {
            return p_232032_1_ == 0 ? CommonComponents.optionStatus(p_232032_0_, false) : genericValueLabel(p_232032_0_, p_232032_1_);
        }
    }, new OptionInstance.IntRange(0, 4), 4, p_438325_1_ -> {
        this.setGraphicsPresetToCustom();
        this.updateMipmaps();
    });
    private static final Component GRAPHICS_TOOLTIP_ANISOTROPIC_FILTERING = Component.translatable("options.maxAnisotropy.tooltip");
    private final OptionInstance<Integer> maxAnisotropyBit = new OptionInstance<>(
        "options.maxAnisotropy",
        OptionInstance.noTooltip(),
        (p_438323_0_, p_438323_1_) -> p_438323_1_ == 0
            ? CommonComponents.optionStatus(p_438323_0_, false)
            : genericValueLabel(p_438323_0_, Component.translatable("options.multiplier", Integer.toString(1 << p_438323_1_))),
        new OptionInstance.IntRange(1, 3),
        2,
        p_438305_1_ -> {
            this.setGraphicsPresetToCustom();
            operateOnLevelRenderer(LevelRenderer::resetSampler);
        }
    );
    private static final Component FILTERING_NONE_TOOLTIP = Component.translatable("options.textureFiltering.none.tooltip");
    private static final Component FILTERING_RGSS_TOOLTIP = Component.translatable("options.textureFiltering.rgss.tooltip");
    private static final Component FILTERING_ANISOTROPIC_TOOLTIP = Component.translatable("options.textureFiltering.anisotropic.tooltip");
    private final OptionInstance<TextureFilteringMethod> textureFiltering = new OptionInstance<>(
        "options.textureFiltering",
        p_438314_0_ -> {
            return switch (p_438314_0_) {
                case NONE -> Tooltip.create(FILTERING_NONE_TOOLTIP);
                case RGSS -> Tooltip.create(FILTERING_RGSS_TOOLTIP);
                case ANISOTROPIC -> Tooltip.create(FILTERING_ANISOTROPIC_TOOLTIP);
            };
        },
        (p_438292_0_, p_438292_1_) -> p_438292_1_.caption(),
        new OptionInstance.Enum<>(Arrays.asList(TextureFilteringMethod.values()), TextureFilteringMethod.LEGACY_CODEC),
        TextureFilteringMethod.NONE,
        p_438298_1_ -> {
            this.setGraphicsPresetToCustom();
            operateOnLevelRenderer(LevelRenderer::resetSampler);
        }
    );
    private boolean useNativeTransport = true;
    private final OptionInstance<AttackIndicatorStatus> attackIndicator = new OptionInstance<>(
        "options.attackIndicator",
        OptionInstance.noTooltip(),
        (p_438291_0_, p_438291_1_) -> p_438291_1_.caption(),
        new OptionInstance.Enum<>(Arrays.asList(AttackIndicatorStatus.values()), AttackIndicatorStatus.LEGACY_CODEC),
        AttackIndicatorStatus.CROSSHAIR,
        p_231876_0_ -> {}
    );
    public TutorialSteps tutorialStep = TutorialSteps.MOVEMENT;
    public boolean joinedFirstServer = false;
    private final OptionInstance<Integer> biomeBlendRadius = new OptionInstance<>(
        "options.biomeBlendRadius", OptionInstance.noTooltip(), (p_232015_0_, p_232015_1_) -> {
            int k = p_232015_1_ * 2 + 1;
            return genericValueLabel(p_232015_0_, Component.translatable("options.biomeBlendRadius." + k));
        }, new OptionInstance.IntRange(0, 7, false), 2, p_438327_1_ -> {
            operateOnLevelRenderer(LevelRenderer::allChanged);
            this.setGraphicsPresetToCustom();
        }
    );
    private final OptionInstance<Double> mouseWheelSensitivity = new OptionInstance<>(
        "options.mouseWheelSensitivity",
        OptionInstance.noTooltip(),
        (p_241716_0_, p_241716_1_) -> genericValueLabel(p_241716_0_, Component.literal(String.format(Locale.ROOT, "%.2f", p_241716_1_))),
        new OptionInstance.IntRange(-200, 100).xmap(Options::logMouse, Options::unlogMouse, false),
        Codec.doubleRange(logMouse(-200), logMouse(100)),
        logMouse(0),
        p_231973_0_ -> {}
    );
    private final OptionInstance<Boolean> rawMouseInput = OptionInstance.createBoolean("options.rawMouseInput", true, p_232061_0_ -> {
        Window window = Minecraft.getInstance().getWindow();
        if (window != null) {
            window.updateRawMouseInput(p_232061_0_);
        }
    });
    private static final Component ALLOW_CURSOR_CHANGES_TOOLTIP = Component.translatable("options.allowCursorChanges.tooltip");
    private final OptionInstance<Boolean> allowCursorChanges = OptionInstance.createBoolean(
        "options.allowCursorChanges", OptionInstance.cachedConstantTooltip(ALLOW_CURSOR_CHANGES_TOOLTIP), true, p_414886_0_ -> {
            Window window = Minecraft.getInstance().getWindow();
            if (window != null) {
                window.setAllowCursorChanges(p_414886_0_);
            }
        }
    );
    public int glDebugVerbosity = 1;
    private final OptionInstance<Boolean> autoJump = OptionInstance.createBoolean("options.autoJump", false);
    private static final Component ACCESSIBILITY_TOOLTIP_ROTATE_WITH_MINECART = Component.translatable("options.rotateWithMinecart.tooltip");
    private final OptionInstance<Boolean> rotateWithMinecart = OptionInstance.createBoolean("options.rotateWithMinecart", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_ROTATE_WITH_MINECART), false);
    private final OptionInstance<Boolean> operatorItemsTab = OptionInstance.createBoolean("options.operatorItemsTab", false);
    private final OptionInstance<Boolean> autoSuggestions = OptionInstance.createBoolean("options.autoSuggestCommands", true);
    private final OptionInstance<Boolean> chatColors = OptionInstance.createBoolean("options.chat.color", true);
    private final OptionInstance<Boolean> chatLinks = OptionInstance.createBoolean("options.chat.links", true);
    private final OptionInstance<Boolean> chatLinksPrompt = OptionInstance.createBoolean("options.chat.links.prompt", true);
    private final OptionInstance<Boolean> enableVsync = OptionInstance.createBoolean("options.vsync", true, p_232051_0_ -> {
        if (Minecraft.getInstance().getWindow() != null) {
            Minecraft.getInstance().getWindow().updateVsync(p_232051_0_);
        }
    });
    private final OptionInstance<Boolean> entityShadows = OptionInstance.createBoolean(
        "options.entityShadows", OptionInstance.noTooltip(), true, p_438290_1_ -> this.setGraphicsPresetToCustom()
    );
    private final OptionInstance<Boolean> forceUnicodeFont = OptionInstance.createBoolean("options.forceUnicodeFont", false, p_317299_0_ -> updateFontOptions());
    private final OptionInstance<Boolean> japaneseGlyphVariants = OptionInstance.createBoolean(
        "options.japaneseGlyphVariants",
        OptionInstance.cachedConstantTooltip(Component.translatable("options.japaneseGlyphVariants.tooltip")),
        japaneseGlyphVariantsDefault(),
        p_317300_0_ -> updateFontOptions()
    );
    private final OptionInstance<Boolean> invertXMouse = OptionInstance.createBoolean("options.invertMouseX", false);
    private final OptionInstance<Boolean> invertYMouse = OptionInstance.createBoolean("options.invertMouseY", false);
    private final OptionInstance<Boolean> discreteMouseScroll = OptionInstance.createBoolean("options.discrete_mouse_scroll", false);
    private static final Component REALMS_NOTIFICATIONS_TOOLTIP = Component.translatable("options.realmsNotifications.tooltip");
    private final OptionInstance<Boolean> realmsNotifications = OptionInstance.createBoolean("options.realmsNotifications", OptionInstance.cachedConstantTooltip(REALMS_NOTIFICATIONS_TOOLTIP), true);
    private static final Component ALLOW_SERVER_LISTING_TOOLTIP = Component.translatable("options.allowServerListing.tooltip");
    private final OptionInstance<Boolean> allowServerListing = OptionInstance.createBoolean(
        "options.allowServerListing", OptionInstance.cachedConstantTooltip(ALLOW_SERVER_LISTING_TOOLTIP), true, p_231868_0_ -> {}
    );
    private final OptionInstance<Boolean> reducedDebugInfo = OptionInstance.createBoolean(
        "options.reducedDebugInfo", OptionInstance.noTooltip(), false, p_414880_0_ -> Minecraft.getInstance().debugEntries.rebuildCurrentList()
    );
    private final Map<SoundSource, OptionInstance<Double>> soundSourceVolumes = Util.makeEnumMap(
        SoundSource.class, p_383645_1_ -> this.createSoundSliderOptionInstance("soundCategory." + p_383645_1_.getName(), p_383645_1_)
    );
    private static final Component CLOSED_CAPTIONS_TOOLTIP = Component.translatable("options.showSubtitles.tooltip");
    private final OptionInstance<Boolean> showSubtitles = OptionInstance.createBoolean("options.showSubtitles", OptionInstance.cachedConstantTooltip(CLOSED_CAPTIONS_TOOLTIP), false);
    private static final Component DIRECTIONAL_AUDIO_TOOLTIP_ON = Component.translatable("options.directionalAudio.on.tooltip");
    private static final Component DIRECTIONAL_AUDIO_TOOLTIP_OFF = Component.translatable("options.directionalAudio.off.tooltip");
    private final OptionInstance<Boolean> directionalAudio = OptionInstance.createBoolean(
        "options.directionalAudio", p_257068_0_ -> p_257068_0_ ? Tooltip.create(DIRECTIONAL_AUDIO_TOOLTIP_ON) : Tooltip.create(DIRECTIONAL_AUDIO_TOOLTIP_OFF), false, p_401248_0_ -> {
            SoundManager soundmanager = Minecraft.getInstance().getSoundManager();
            soundmanager.reload();
            soundmanager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    );
    private final OptionInstance<Boolean> backgroundForChatOnly = new OptionInstance<>(
        "options.accessibility.text_background",
        OptionInstance.noTooltip(),
        (p_231975_0_, p_231975_1_) -> p_231975_1_
            ? Component.translatable("options.accessibility.text_background.chat")
            : Component.translatable("options.accessibility.text_background.everywhere"),
        OptionInstance.BOOLEAN_VALUES,
        true,
        p_241717_0_ -> {}
    );
    private final OptionInstance<Boolean> touchscreen = OptionInstance.createBoolean("options.touchscreen", false);
    private final OptionInstance<Boolean> fullscreen = OptionInstance.createBoolean("options.fullscreen", false, p_231969_1_ -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() != null && minecraft.getWindow().isFullscreen() != p_231969_1_) {
            minecraft.getWindow().toggleFullScreen();
            this.fullscreen().set(minecraft.getWindow().isFullscreen());
        }
    });
    private final OptionInstance<Boolean> bobView = OptionInstance.createBoolean("options.viewBobbing", true);
    private static final Component KEY_TOGGLE = Component.translatable("options.key.toggle");
    private static final Component KEY_HOLD = Component.translatable("options.key.hold");
    private final OptionInstance<Boolean> toggleCrouch = new OptionInstance<>(
        "key.sneak",
        OptionInstance.noTooltip(),
        (p_414879_0_, p_414879_1_) -> p_414879_1_ ? KEY_TOGGLE : KEY_HOLD,
        OptionInstance.BOOLEAN_VALUES,
        false,
        p_260742_0_ -> {}
    );
    private final OptionInstance<Boolean> toggleSprint = new OptionInstance<>(
        "key.sprint",
        OptionInstance.noTooltip(),
        (p_414884_0_, p_414884_1_) -> p_414884_1_ ? KEY_TOGGLE : KEY_HOLD,
        OptionInstance.BOOLEAN_VALUES,
        false,
        p_263858_0_ -> {}
    );
    private final OptionInstance<Boolean> toggleAttack = new OptionInstance<>(
        "key.attack",
        OptionInstance.noTooltip(),
        (p_414887_0_, p_414887_1_) -> p_414887_1_ ? KEY_TOGGLE : KEY_HOLD,
        OptionInstance.BOOLEAN_VALUES,
        false,
        p_267500_0_ -> {}
    );
    private final OptionInstance<Boolean> toggleUse = new OptionInstance<>(
        "key.use",
        OptionInstance.noTooltip(),
        (p_414878_0_, p_414878_1_) -> p_414878_1_ ? KEY_TOGGLE : KEY_HOLD,
        OptionInstance.BOOLEAN_VALUES,
        false,
        p_268764_0_ -> {}
    );
    private static final Component SPRINT_WINDOW_TOOLTIP = Component.translatable("options.sprintWindow.tooltip");
    private final OptionInstance<Integer> sprintWindow = new OptionInstance<>(
        "options.sprintWindow",
        OptionInstance.cachedConstantTooltip(SPRINT_WINDOW_TOOLTIP),
        (p_414883_0_, p_414883_1_) -> p_414883_1_ == 0
            ? genericValueLabel(p_414883_0_, Component.translatable("options.off"))
            : genericValueLabel(p_414883_0_, Component.translatable("options.value", p_414883_1_)),
        new OptionInstance.IntRange(0, 10),
        7,
        p_348921_0_ -> {}
    );
    public boolean skipMultiplayerWarning;
    private static final Component CHAT_TOOLTIP_HIDE_MATCHED_NAMES = Component.translatable("options.hideMatchedNames.tooltip");
    private final OptionInstance<Boolean> hideMatchedNames = OptionInstance.createBoolean("options.hideMatchedNames", OptionInstance.cachedConstantTooltip(CHAT_TOOLTIP_HIDE_MATCHED_NAMES), true);
    private final OptionInstance<Boolean> showAutosaveIndicator = OptionInstance.createBoolean("options.autosaveIndicator", true);
    private static final Component CHAT_TOOLTIP_ONLY_SHOW_SECURE = Component.translatable("options.onlyShowSecureChat.tooltip");
    private final OptionInstance<Boolean> onlyShowSecureChat = OptionInstance.createBoolean("options.onlyShowSecureChat", OptionInstance.cachedConstantTooltip(CHAT_TOOLTIP_ONLY_SHOW_SECURE), false);
    private static final Component CHAT_TOOLTIP_SAVE_DRAFTS = Component.translatable("options.chat.drafts.tooltip");
    private final OptionInstance<Boolean> saveChatDrafts = OptionInstance.createBoolean("options.chat.drafts", OptionInstance.cachedConstantTooltip(CHAT_TOOLTIP_SAVE_DRAFTS), false);
    public final KeyMapping keyUp = new KeyMapping("key.forward", 87, KeyMapping.Category.MOVEMENT);
    public final KeyMapping keyLeft = new KeyMapping("key.left", 65, KeyMapping.Category.MOVEMENT);
    public final KeyMapping keyDown = new KeyMapping("key.back", 83, KeyMapping.Category.MOVEMENT);
    public final KeyMapping keyRight = new KeyMapping("key.right", 68, KeyMapping.Category.MOVEMENT);
    public final KeyMapping keyJump = new KeyMapping("key.jump", 32, KeyMapping.Category.MOVEMENT);
    public final KeyMapping keyShift = new ToggleKeyMapping("key.sneak", 340, KeyMapping.Category.MOVEMENT, this.toggleCrouch::get, true);
    public final KeyMapping keySprint = new ToggleKeyMapping("key.sprint", 341, KeyMapping.Category.MOVEMENT, this.toggleSprint::get, true);
    public final KeyMapping keyInventory = new KeyMapping("key.inventory", 69, KeyMapping.Category.INVENTORY);
    public final KeyMapping keySwapOffhand = new KeyMapping("key.swapOffhand", 70, KeyMapping.Category.INVENTORY);
    public final KeyMapping keyDrop = new KeyMapping("key.drop", 81, KeyMapping.Category.INVENTORY);
    public final KeyMapping keyUse = new ToggleKeyMapping(
        "key.use", InputConstants.Type.MOUSE, 1, KeyMapping.Category.GAMEPLAY, this.toggleUse::get, false
    );
    public final KeyMapping keyAttack = new ToggleKeyMapping(
        "key.attack", InputConstants.Type.MOUSE, 0, KeyMapping.Category.GAMEPLAY, this.toggleAttack::get, true
    );
    public final KeyMapping keyPickItem = new KeyMapping("key.pickItem", InputConstants.Type.MOUSE, 2, KeyMapping.Category.GAMEPLAY);
    public final KeyMapping keyChat = new KeyMapping("key.chat", 84, KeyMapping.Category.MULTIPLAYER);
    public final KeyMapping keyPlayerList = new KeyMapping("key.playerlist", 258, KeyMapping.Category.MULTIPLAYER);
    public final KeyMapping keyCommand = new KeyMapping("key.command", 47, KeyMapping.Category.MULTIPLAYER);
    public final KeyMapping keySocialInteractions = new KeyMapping("key.socialInteractions", 80, KeyMapping.Category.MULTIPLAYER);
    public final KeyMapping keyScreenshot = new KeyMapping("key.screenshot", 291, KeyMapping.Category.MISC);
    public final KeyMapping keyTogglePerspective = new KeyMapping("key.togglePerspective", 294, KeyMapping.Category.MISC);
    public final KeyMapping keySmoothCamera = new KeyMapping("key.smoothCamera", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);
    public final KeyMapping keyFullscreen = new KeyMapping("key.fullscreen", 300, KeyMapping.Category.MISC);
    public final KeyMapping keyAdvancements = new KeyMapping("key.advancements", 76, KeyMapping.Category.MISC);
    public final KeyMapping keyQuickActions = new KeyMapping("key.quickActions", 71, KeyMapping.Category.MISC);
    public final KeyMapping keyToggleGui = new KeyMapping("key.toggleGui", 290, KeyMapping.Category.MISC);
    public final KeyMapping keyToggleSpectatorShaderEffects = new KeyMapping("key.toggleSpectatorShaderEffects", 293, KeyMapping.Category.MISC);
    public final KeyMapping[] keyHotbarSlots = new KeyMapping[]{
        new KeyMapping("key.hotbar.1", 49, KeyMapping.Category.INVENTORY),
        new KeyMapping("key.hotbar.2", 50, KeyMapping.Category.INVENTORY),
        new KeyMapping("key.hotbar.3", 51, KeyMapping.Category.INVENTORY),
        new KeyMapping("key.hotbar.4", 52, KeyMapping.Category.INVENTORY),
        new KeyMapping("key.hotbar.5", 53, KeyMapping.Category.INVENTORY),
        new KeyMapping("key.hotbar.6", 54, KeyMapping.Category.INVENTORY),
        new KeyMapping("key.hotbar.7", 55, KeyMapping.Category.INVENTORY),
        new KeyMapping("key.hotbar.8", 56, KeyMapping.Category.INVENTORY),
        new KeyMapping("key.hotbar.9", 57, KeyMapping.Category.INVENTORY)
    };
    public final KeyMapping keySaveHotbarActivator = new KeyMapping("key.saveToolbarActivator", 67, KeyMapping.Category.CREATIVE);
    public final KeyMapping keyLoadHotbarActivator = new KeyMapping("key.loadToolbarActivator", 88, KeyMapping.Category.CREATIVE);
    public final KeyMapping keySpectatorOutlines = new KeyMapping("key.spectatorOutlines", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.SPECTATOR);
    public final KeyMapping keySpectatorHotbar = new KeyMapping("key.spectatorHotbar", InputConstants.Type.MOUSE, 2, KeyMapping.Category.SPECTATOR);
    public final KeyMapping keyDebugOverlay = new KeyMapping("key.debug.overlay", InputConstants.Type.KEYSYM, 292, KeyMapping.Category.DEBUG, -2);
    public final KeyMapping keyDebugModifier = new KeyMapping("key.debug.modifier", InputConstants.Type.KEYSYM, 292, KeyMapping.Category.DEBUG, -1);
    public final KeyMapping keyDebugCrash = new KeyMapping("key.debug.crash", InputConstants.Type.KEYSYM, 67, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugReloadChunk = new KeyMapping("key.debug.reloadChunk", InputConstants.Type.KEYSYM, 65, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugShowHitboxes = new KeyMapping("key.debug.showHitboxes", InputConstants.Type.KEYSYM, 66, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugClearChat = new KeyMapping("key.debug.clearChat", InputConstants.Type.KEYSYM, 68, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugShowChunkBorders = new KeyMapping("key.debug.showChunkBorders", InputConstants.Type.KEYSYM, 71, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugShowAdvancedTooltips = new KeyMapping("key.debug.showAdvancedTooltips", InputConstants.Type.KEYSYM, 72, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugCopyRecreateCommand = new KeyMapping("key.debug.copyRecreateCommand", InputConstants.Type.KEYSYM, 73, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugSpectate = new KeyMapping("key.debug.spectate", InputConstants.Type.KEYSYM, 78, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugSwitchGameMode = new KeyMapping("key.debug.switchGameMode", InputConstants.Type.KEYSYM, 293, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugDebugOptions = new KeyMapping("key.debug.debugOptions", InputConstants.Type.KEYSYM, 295, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugFocusPause = new KeyMapping("key.debug.focusPause", InputConstants.Type.KEYSYM, 80, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugDumpDynamicTextures = new KeyMapping("key.debug.dumpDynamicTextures", InputConstants.Type.KEYSYM, 83, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugReloadResourcePacks = new KeyMapping("key.debug.reloadResourcePacks", InputConstants.Type.KEYSYM, 84, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugProfiling = new KeyMapping("key.debug.profiling", InputConstants.Type.KEYSYM, 76, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugCopyLocation = new KeyMapping("key.debug.copyLocation", InputConstants.Type.KEYSYM, 67, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugDumpVersion = new KeyMapping("key.debug.dumpVersion", InputConstants.Type.KEYSYM, 86, KeyMapping.Category.DEBUG);
    public final KeyMapping keyDebugPofilingChart = new KeyMapping("key.debug.profilingChart", InputConstants.Type.KEYSYM, 49, KeyMapping.Category.DEBUG, 1);
    public final KeyMapping keyDebugFpsCharts = new KeyMapping("key.debug.fpsCharts", InputConstants.Type.KEYSYM, 50, KeyMapping.Category.DEBUG, 2);
    public final KeyMapping keyDebugNetworkCharts = new KeyMapping("key.debug.networkCharts", InputConstants.Type.KEYSYM, 51, KeyMapping.Category.DEBUG, 3);
    public final KeyMapping[] debugKeys = new KeyMapping[]{
        this.keyDebugReloadChunk,
        this.keyDebugShowHitboxes,
        this.keyDebugClearChat,
        this.keyDebugCrash,
        this.keyDebugShowChunkBorders,
        this.keyDebugShowAdvancedTooltips,
        this.keyDebugCopyRecreateCommand,
        this.keyDebugSpectate,
        this.keyDebugSwitchGameMode,
        this.keyDebugDebugOptions,
        this.keyDebugFocusPause,
        this.keyDebugDumpDynamicTextures,
        this.keyDebugReloadResourcePacks,
        this.keyDebugProfiling,
        this.keyDebugCopyLocation,
        this.keyDebugDumpVersion,
        this.keyDebugPofilingChart,
        this.keyDebugFpsCharts,
        this.keyDebugNetworkCharts
    };
    public KeyMapping[] keyMappings = Stream.of(
            new KeyMapping[]{
                this.keyAttack,
                this.keyUse,
                this.keyUp,
                this.keyLeft,
                this.keyDown,
                this.keyRight,
                this.keyJump,
                this.keyShift,
                this.keySprint,
                this.keyDrop,
                this.keyInventory,
                this.keyChat,
                this.keyPlayerList,
                this.keyPickItem,
                this.keyCommand,
                this.keySocialInteractions,
                this.keyToggleGui,
                this.keyToggleSpectatorShaderEffects,
                this.keyScreenshot,
                this.keyTogglePerspective,
                this.keySmoothCamera,
                this.keyFullscreen,
                this.keySpectatorOutlines,
                this.keySpectatorHotbar,
                this.keySwapOffhand,
                this.keySaveHotbarActivator,
                this.keyLoadHotbarActivator,
                this.keyAdvancements,
                this.keyQuickActions,
                this.keyDebugOverlay,
                this.keyDebugModifier
            },
            this.keyHotbarSlots,
            this.debugKeys
        )
        .flatMap(Stream::of)
        .toArray(KeyMapping[]::new);
    protected Minecraft minecraft;
    private final File optionsFile;
    public boolean hideGui;
    private CameraType cameraType = CameraType.FIRST_PERSON;
    public String lastMpIp = "";
    public boolean smoothCamera;
    private final OptionInstance<Integer> fov = new OptionInstance<>(
        "options.fov",
        OptionInstance.noTooltip(),
        (p_231998_0_, p_231998_1_) -> {
            return switch (p_231998_1_) {
                case 70 -> genericValueLabel(p_231998_0_, Component.translatable("options.fov.min"));
                case 110 -> genericValueLabel(p_231998_0_, Component.translatable("options.fov.max"));
                default -> genericValueLabel(p_231998_0_, p_231998_1_);
            };
        },
        new OptionInstance.IntRange(30, 110),
        Codec.DOUBLE.xmap(p_232006_0_ -> (int)(p_232006_0_ * 40.0 + 70.0), p_232008_0_ -> (p_232008_0_.intValue() - 70.0) / 40.0),
        70,
        p_438317_0_ -> operateOnLevelRenderer(LevelRenderer::needsUpdate)
    );
    private static final Component TELEMETRY_TOOLTIP = Component.translatable(
        "options.telemetry.button.tooltip", Component.translatable("options.telemetry.state.minimal"), Component.translatable("options.telemetry.state.all")
    );
    private final OptionInstance<Boolean> telemetryOptInExtra = OptionInstance.createBoolean(
        "options.telemetry.button",
        OptionInstance.cachedConstantTooltip(TELEMETRY_TOOLTIP),
        (p_260741_0_, p_260741_1_) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (!minecraft.allowsTelemetry()) {
                return Component.translatable("options.telemetry.state.none");
            } else {
                return p_260741_1_ && minecraft.extraTelemetryAvailable()
                    ? Component.translatable("options.telemetry.state.all")
                    : Component.translatable("options.telemetry.state.minimal");
            }
        },
        false,
        p_231855_0_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_SCREEN_EFFECT = Component.translatable("options.screenEffectScale.tooltip");
    private final OptionInstance<Double> screenEffectScale = new OptionInstance<>(
        "options.screenEffectScale", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_SCREEN_EFFECT), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, p_231870_0_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_FOV_EFFECT = Component.translatable("options.fovEffectScale.tooltip");
    private final OptionInstance<Double> fovEffectScale = new OptionInstance<>(
        "options.fovEffectScale",
        OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_FOV_EFFECT),
        Options::percentValueOrOffLabel,
        OptionInstance.UnitDouble.INSTANCE.xmap(Mth::square, Math::sqrt),
        Codec.doubleRange(0.0, 1.0),
        1.0,
        p_231843_0_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_DARKNESS_EFFECT = Component.translatable("options.darknessEffectScale.tooltip");
    private final OptionInstance<Double> darknessEffectScale = new OptionInstance<>(
        "options.darknessEffectScale",
        OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DARKNESS_EFFECT),
        Options::percentValueOrOffLabel,
        OptionInstance.UnitDouble.INSTANCE.xmap(Mth::square, Math::sqrt),
        1.0,
        p_232102_0_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_GLINT_SPEED = Component.translatable("options.glintSpeed.tooltip");
    private final OptionInstance<Double> glintSpeed = new OptionInstance<>(
        "options.glintSpeed", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_GLINT_SPEED), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 0.5, p_232108_0_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_GLINT_STRENGTH = Component.translatable("options.glintStrength.tooltip");
    private final OptionInstance<Double> glintStrength = new OptionInstance<>(
        "options.glintStrength", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_GLINT_STRENGTH), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 0.75, p_232038_0_ -> {}
    );
    private static final Component ACCESSIBILITY_TOOLTIP_DAMAGE_TILT_STRENGTH = Component.translatable("options.damageTiltStrength.tooltip");
    private final OptionInstance<Double> damageTiltStrength = new OptionInstance<>(
        "options.damageTiltStrength", OptionInstance.cachedConstantTooltip(ACCESSIBILITY_TOOLTIP_DAMAGE_TILT_STRENGTH), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, p_232040_0_ -> {}
    );
    private final OptionInstance<Double> gamma = new OptionInstance<>("options.gamma", OptionInstance.noTooltip(), (p_231912_0_, p_231912_1_) -> {
        int k = (int)(p_231912_1_ * 100.0);
        if (k == 0) {
            return genericValueLabel(p_231912_0_, Component.translatable("options.gamma.min"));
        } else if (k == 50) {
            return genericValueLabel(p_231912_0_, Component.translatable("options.gamma.default"));
        } else {
            return k == 100 ? genericValueLabel(p_231912_0_, Component.translatable("options.gamma.max")) : genericValueLabel(p_231912_0_, k);
        }
    }, OptionInstance.UnitDouble.INSTANCE, 0.5, p_231851_0_ -> {});
    public static final int AUTO_GUI_SCALE = 0;
    private static final int MAX_GUI_SCALE_INCLUSIVE = 2147483646;
    private final OptionInstance<Integer> guiScale = new OptionInstance<>(
        "options.guiScale",
        OptionInstance.noTooltip(),
        (p_231981_0_, p_231981_1_) -> p_231981_1_ == 0 ? Component.translatable("options.guiScale.auto") : Component.literal(Integer.toString(p_231981_1_)),
        new OptionInstance.ClampingLazyMaxIntRange(0, () -> {
            Minecraft minecraft = Minecraft.getInstance();
            return !minecraft.isRunning() ? 2147483646 : minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
        }, 2147483646),
        0,
        p_317301_1_ -> this.minecraft.resizeDisplay()
    );
    private final OptionInstance<ParticleStatus> particles = new OptionInstance<>(
        "options.particles",
        OptionInstance.noTooltip(),
        (p_438301_0_, p_438301_1_) -> p_438301_1_.caption(),
        new OptionInstance.Enum<>(Arrays.asList(ParticleStatus.values()), ParticleStatus.LEGACY_CODEC),
        ParticleStatus.ALL,
        p_438308_1_ -> this.setGraphicsPresetToCustom()
    );
    private final OptionInstance<NarratorStatus> narrator = new OptionInstance<>(
        "options.narrator",
        OptionInstance.noTooltip(),
        (p_240390_1_, p_240390_2_) -> this.minecraft.getNarrator().isActive() ? p_240390_2_.getName() : Component.translatable("options.narrator.notavailable"),
        new OptionInstance.Enum<>(Arrays.asList(NarratorStatus.values()), NarratorStatus.LEGACY_CODEC),
        NarratorStatus.OFF,
        p_240389_1_ -> this.minecraft.getNarrator().updateNarratorStatus(p_240389_1_)
    );
    public String languageCode = "en_us";
    private final OptionInstance<String> soundDevice = new OptionInstance<>(
        "options.audioDevice",
        OptionInstance.noTooltip(),
        (p_231918_0_, p_231918_1_) -> {
            if ("".equals(p_231918_1_)) {
                return Component.translatable("options.audioDevice.default");
            } else {
                return p_231918_1_.startsWith("OpenAL Soft on ")
                    ? Component.literal(p_231918_1_.substring(SoundEngine.OPEN_AL_SOFT_PREFIX_LENGTH))
                    : Component.literal(p_231918_1_);
            }
        },
        new OptionInstance.LazyEnum<>(
            () -> Stream.concat(Stream.of(""), Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().stream()).toList(),
            p_232010_0_ -> Minecraft.getInstance().isRunning()
                    && (p_232010_0_ == null || p_232010_0_.isEmpty())
                    && !Minecraft.getInstance().getSoundManager().getAvailableSoundDevices().contains(p_232010_0_)
                ? Optional.empty()
                : Optional.of(p_232010_0_),
            Codec.STRING
        ),
        "",
        p_401246_0_ -> {
            SoundManager soundmanager = Minecraft.getInstance().getSoundManager();
            soundmanager.reload();
            soundmanager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    );
    public boolean onboardAccessibility = true;
    private static final Component MUSIC_FREQUENCY_TOOLTIP = Component.translatable("options.music_frequency.tooltip");
    private final OptionInstance<MusicManager.MusicFrequency> musicFrequency = new OptionInstance<>(
        "options.music_frequency",
        OptionInstance.cachedConstantTooltip(MUSIC_FREQUENCY_TOOLTIP),
        (p_438306_0_, p_438306_1_) -> p_438306_1_.caption(),
        new OptionInstance.Enum<>(Arrays.asList(MusicManager.MusicFrequency.values()), MusicManager.MusicFrequency.CODEC),
        MusicManager.MusicFrequency.DEFAULT,
        p_401247_0_ -> Minecraft.getInstance().getMusicManager().setMinutesBetweenSongs(p_401247_0_)
    );
    private final OptionInstance<MusicToastDisplayState> musicToast = new OptionInstance<>(
        "options.musicToast",
        p_438299_0_ -> Tooltip.create(p_438299_0_.tooltip()),
        (p_438293_0_, p_438293_1_) -> p_438293_1_.text(),
        new OptionInstance.Enum<>(Arrays.asList(MusicToastDisplayState.values()), MusicToastDisplayState.CODEC),
        MusicToastDisplayState.NEVER,
        p_438321_1_ -> this.minecraft.getToastManager().setMusicToastDisplayState(p_438321_1_)
    );
    public boolean syncWrites;
    public boolean startedCleanly = true;
    private final Map<String, String> unknownKeys = new HashMap<>();
    public int ofFogType = 1;
    public float ofFogStart = 0.8F;
    public int ofMipmapType = 0;
    public boolean ofOcclusionFancy = false;
    public boolean ofSmoothFps = false;
    public boolean ofSmoothWorld = Config.isSingleProcessor();
    public boolean ofLazyChunkLoading = Config.isSingleProcessor();
    public boolean ofRenderRegions = false;
    public boolean ofSmartAnimations = false;
    public double ofAoLevel = 1.0;
    public int ofAaLevel = 0;
    public int ofAfLevel = 1;
    public double ofCloudsHeight = 0.0;
    public int ofTrees = 2;
    public int ofBetterGrass = 3;
    public int ofAutoSaveTicks = 4000;
    public boolean ofLagometer = false;
    public boolean ofProfiler = false;
    public boolean ofWeather = true;
    public boolean ofSky = true;
    public boolean ofStars = true;
    public boolean ofSunMoon = true;
    public int ofChunkUpdates = 1;
    public boolean ofChunkUpdatesDynamic = false;
    public int ofTime = 0;
    public boolean ofBetterSnow = false;
    public boolean ofSwampColors = true;
    public boolean ofRandomEntities = true;
    public boolean ofCustomFonts = true;
    public boolean ofCustomColors = true;
    public boolean ofCustomSky = true;
    public boolean ofShowCapes = true;
    public int ofConnectedTextures = 2;
    public boolean ofCustomItems = true;
    public boolean ofNaturalTextures = false;
    public boolean ofEmissiveTextures = true;
    public boolean ofFastMath = false;
    public boolean ofFastRender = false;
    public boolean ofAlternateBlocks = true;
    public int ofDynamicLights = 3;
    public boolean ofCustomEntityModels = true;
    public boolean ofCustomGuis = true;
    public boolean ofShowGlErrors = true;
    public int ofScreenshotSize = 1;
    public int ofChatBackground = 0;
    public boolean ofChatShadow = true;
    public int ofTelemetry = 0;
    public boolean ofHeldItemTooltips = true;
    public int ofAnimatedWater = 0;
    public int ofAnimatedLava = 0;
    public boolean ofAnimatedFire = true;
    public boolean ofAnimatedPortal = true;
    public boolean ofAnimatedRedstone = true;
    public boolean ofAnimatedExplosion = true;
    public boolean ofAnimatedFlame = true;
    public boolean ofAnimatedSmoke = true;
    public boolean ofVoidParticles = true;
    public boolean ofWaterParticles = true;
    public boolean ofRainSplash = true;
    public boolean ofPortalParticles = true;
    public boolean ofPotionParticles = true;
    public boolean ofFireworkParticles = true;
    public boolean ofDrippingWaterLava = true;
    public boolean ofAnimatedTerrain = true;
    public boolean ofAnimatedTextures = true;
    public boolean ofQuickInfo = false;
    public int ofQuickInfoFps = Option.FULL.getValue();
    public boolean ofQuickInfoChunks = true;
    public boolean ofQuickInfoEntities = true;
    public boolean ofQuickInfoParticles = false;
    public boolean ofQuickInfoUpdates = true;
    public boolean ofQuickInfoGpu = false;
    public int ofQuickInfoPos = Option.COMPACT.getValue();
    public int ofQuickInfoFacing = Option.OFF.getValue();
    public boolean ofQuickInfoBiome = false;
    public boolean ofQuickInfoLight = false;
    public int ofQuickInfoMemory = Option.OFF.getValue();
    public int ofQuickInfoNativeMemory = Option.OFF.getValue();
    public int ofQuickInfoTargetBlock = Option.OFF.getValue();
    public int ofQuickInfoTargetFluid = Option.OFF.getValue();
    public int ofQuickInfoTargetEntity = Option.OFF.getValue();
    public int ofQuickInfoLabels = Option.COMPACT.getValue();
    public boolean ofQuickInfoBackground = false;
    public static final int DEFAULT = 0;
    public static final int FAST = 1;
    public static final int FANCY = 2;
    public static final int OFF = 3;
    public static final int SMART = 4;
    public static final int COMPACT = 5;
    public static final int FULL = 6;
    public static final int DETAILED = 7;
    public static final int ANIM_ON = 0;
    public static final int ANIM_GENERATED = 1;
    public static final int ANIM_OFF = 2;
    public static final String DEFAULT_STR = "Default";
    public static final double CHAT_WIDTH_SCALE = 4.0571431;
    public static final int[] VALS_FAST_FANCY_OFF = new int[]{1, 2, 3};
    private static final int[] OF_TREES_VALUES = new int[]{1, 4, 2};
    private static final int[] OF_DYNAMIC_LIGHTS = new int[]{3, 1, 2};
    private static final String[] KEYS_DYNAMIC_LIGHTS = new String[]{"options.off", "options.graphics.fast", "options.graphics.fancy"};
    public static final int TELEM_ON = 0;
    public static final int TELEM_ANON = 1;
    public static final int TELEM_OFF = 2;
    private static final int[] OF_TELEMETRY = new int[]{0, 1, 2};
    private static final String[] KEYS_TELEMETRY = new String[]{"options.on", "of.general.anonymous", "options.off"};
    public KeyMapping ofKeyBindZoom;
    private File optionsFileOF;
    public final OptionInstance GRAPHICS = this.graphicsPreset;
    public final OptionInstance RENDER_DISTANCE;
    public final OptionInstance SIMULATION_DISTANCE;
    public final OptionInstance AO = this.ambientOcclusion;
    public final OptionInstance FRAMERATE_LIMIT = this.framerateLimit;
    public final OptionInstance GUI_SCALE = this.guiScale;
    public final OptionInstance ENTITY_SHADOWS = this.entityShadows;
    public final OptionInstance GAMMA = this.gamma;
    public final OptionInstance ATTACK_INDICATOR = this.attackIndicator;
    public final OptionInstance PARTICLES = this.particles;
    public final OptionInstance VIEW_BOBBING = this.bobView;
    public final OptionInstance AUTOSAVE_INDICATOR = this.showAutosaveIndicator;
    public final OptionInstance ENTITY_DISTANCE_SCALING = this.entityDistanceScaling;
    public final OptionInstance BIOME_BLEND_RADIUS = this.biomeBlendRadius;
    public final OptionInstance FULLSCREEN = this.fullscreen;
    public final OptionInstance PRIORITIZE_CHUNK_UPDATES = this.prioritizeChunkUpdates;
    public final OptionInstance MIPMAP_LEVELS = this.mipmapLevels;
    public final OptionInstance SCREEN_EFFECT_SCALE = this.screenEffectScale;
    public final OptionInstance FOV_EFFECT_SCALE = this.fovEffectScale;
    public final OptionInstance INACTIVITY_FPS_LIMIT = this.inactivityFpsLimit;
    public final OptionInstance CLOUDS = this.cloudStatus;
    public final OptionInstance CLOUDS_RANGE = this.cloudRange;
    public final OptionInstance WEATHER_RADIUS = this.weatherRadius;
    public final OptionInstance VIGNETTE = this.vignette;
    public final OptionInstance CHUNK_FADE = this.chunkSectionFadeInTime;

    private static void operateOnLevelRenderer(Consumer<LevelRenderer> p_457625_) {
        LevelRenderer levelrenderer = Minecraft.getInstance().levelRenderer;
        if (levelrenderer != null) {
            p_457625_.accept(levelrenderer);
        }
    }

    public OptionInstance<Boolean> darkMojangStudiosBackground() {
        return this.darkMojangStudiosBackground;
    }

    public OptionInstance<Boolean> hideLightningFlash() {
        return this.hideLightningFlash;
    }

    public OptionInstance<Boolean> hideSplashTexts() {
        return this.hideSplashTexts;
    }

    public OptionInstance<Double> sensitivity() {
        return this.sensitivity;
    }

    public OptionInstance<Integer> renderDistance() {
        return this.renderDistance;
    }

    public OptionInstance<Integer> simulationDistance() {
        return this.simulationDistance;
    }

    public OptionInstance<Double> entityDistanceScaling() {
        return this.entityDistanceScaling;
    }

    public OptionInstance<Integer> framerateLimit() {
        return this.framerateLimit;
    }

    public void applyGraphicsPreset(GraphicsPreset p_458079_) {
        this.isApplyingGraphicsPreset = true;
        p_458079_.apply(this.minecraft);
        this.isApplyingGraphicsPreset = false;
        this.postApplyOptions();
    }

    public OptionInstance<GraphicsPreset> graphicsPreset() {
        return this.graphicsPreset;
    }

    public OptionInstance<InactivityFpsLimit> inactivityFpsLimit() {
        return this.inactivityFpsLimit;
    }

    public OptionInstance<CloudStatus> cloudStatus() {
        return this.cloudStatus;
    }

    public OptionInstance<Integer> cloudRange() {
        return this.cloudRange;
    }

    public OptionInstance<Integer> weatherRadius() {
        return this.weatherRadius;
    }

    public OptionInstance<Boolean> cutoutLeaves() {
        return this.cutoutLeaves;
    }

    public OptionInstance<Boolean> vignette() {
        return this.vignette;
    }

    public OptionInstance<Boolean> improvedTransparency() {
        return this.improvedTransparency;
    }

    public OptionInstance<Boolean> ambientOcclusion() {
        return this.ambientOcclusion;
    }

    public OptionInstance<Double> chunkSectionFadeInTime() {
        return this.chunkSectionFadeInTime;
    }

    public OptionInstance<PrioritizeChunkUpdates> prioritizeChunkUpdates() {
        return this.prioritizeChunkUpdates;
    }

    public void updateResourcePacks(PackRepository p_275268_) {
        List<String> list = ImmutableList.copyOf(this.resourcePacks);
        this.resourcePacks.clear();
        this.incompatibleResourcePacks.clear();

        for (Pack pack : p_275268_.getSelectedPacks()) {
            if (!pack.isFixedPosition()) {
                this.resourcePacks.add(pack.getId());
                if (!pack.getCompatibility().isCompatible()) {
                    this.incompatibleResourcePacks.add(pack.getId());
                }
            }
        }

        this.save();
        List<String> list1 = ImmutableList.copyOf(this.resourcePacks);
        if (!list1.equals(list)) {
            this.minecraft.reloadResourcePacks();
        }
    }

    public OptionInstance<ChatVisiblity> chatVisibility() {
        return this.chatVisibility;
    }

    public OptionInstance<Double> chatOpacity() {
        return this.chatOpacity;
    }

    public OptionInstance<Double> chatLineSpacing() {
        return this.chatLineSpacing;
    }

    public OptionInstance<Integer> menuBackgroundBlurriness() {
        return this.menuBackgroundBlurriness;
    }

    public int getMenuBackgroundBlurriness() {
        return this.menuBackgroundBlurriness().get();
    }

    public OptionInstance<Double> textBackgroundOpacity() {
        return this.textBackgroundOpacity;
    }

    public OptionInstance<Double> panoramaSpeed() {
        return this.panoramaSpeed;
    }

    public OptionInstance<Boolean> highContrast() {
        return this.highContrast;
    }

    public OptionInstance<Boolean> highContrastBlockOutline() {
        return this.highContrastBlockOutline;
    }

    public OptionInstance<Boolean> narratorHotkey() {
        return this.narratorHotkey;
    }

    public OptionInstance<HumanoidArm> mainHand() {
        return this.mainHand;
    }

    public OptionInstance<Double> chatScale() {
        return this.chatScale;
    }

    public OptionInstance<Double> chatWidth() {
        return this.chatWidth;
    }

    public OptionInstance<Double> chatHeightUnfocused() {
        return this.chatHeightUnfocused;
    }

    public OptionInstance<Double> chatHeightFocused() {
        return this.chatHeightFocused;
    }

    public OptionInstance<Double> chatDelay() {
        return this.chatDelay;
    }

    public OptionInstance<Double> notificationDisplayTime() {
        return this.notificationDisplayTime;
    }

    public OptionInstance<Integer> mipmapLevels() {
        return this.mipmapLevels;
    }

    public OptionInstance<Integer> maxAnisotropyBit() {
        return this.maxAnisotropyBit;
    }

    public int maxAnisotropyValue() {
        return Math.min(1 << this.maxAnisotropyBit.get(), RenderSystem.getDevice().getMaxSupportedAnisotropy());
    }

    public OptionInstance<TextureFilteringMethod> textureFiltering() {
        return this.textureFiltering;
    }

    public OptionInstance<AttackIndicatorStatus> attackIndicator() {
        return this.attackIndicator;
    }

    public OptionInstance<Integer> biomeBlendRadius() {
        return this.biomeBlendRadius;
    }

    private static double logMouse(int p_231966_) {
        return Math.pow(10.0, p_231966_ / 100.0);
    }

    private static int unlogMouse(double p_231840_) {
        return Mth.floor(Math.log10(p_231840_) * 100.0);
    }

    public OptionInstance<Double> mouseWheelSensitivity() {
        return this.mouseWheelSensitivity;
    }

    public OptionInstance<Boolean> rawMouseInput() {
        return this.rawMouseInput;
    }

    public OptionInstance<Boolean> allowCursorChanges() {
        return this.allowCursorChanges;
    }

    public OptionInstance<Boolean> autoJump() {
        return this.autoJump;
    }

    public OptionInstance<Boolean> rotateWithMinecart() {
        return this.rotateWithMinecart;
    }

    public OptionInstance<Boolean> operatorItemsTab() {
        return this.operatorItemsTab;
    }

    public OptionInstance<Boolean> autoSuggestions() {
        return this.autoSuggestions;
    }

    public OptionInstance<Boolean> chatColors() {
        return this.chatColors;
    }

    public OptionInstance<Boolean> chatLinks() {
        return this.chatLinks;
    }

    public OptionInstance<Boolean> chatLinksPrompt() {
        return this.chatLinksPrompt;
    }

    public OptionInstance<Boolean> enableVsync() {
        return this.enableVsync;
    }

    public OptionInstance<Boolean> entityShadows() {
        return this.entityShadows;
    }

    private static void updateFontOptions() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() != null) {
            minecraft.updateFontOptions();
            minecraft.resizeDisplay();
        }
    }

    public OptionInstance<Boolean> forceUnicodeFont() {
        return this.forceUnicodeFont;
    }

    private static boolean japaneseGlyphVariantsDefault() {
        return Locale.getDefault().getLanguage().equalsIgnoreCase("ja");
    }

    public OptionInstance<Boolean> japaneseGlyphVariants() {
        return this.japaneseGlyphVariants;
    }

    public OptionInstance<Boolean> invertMouseX() {
        return this.invertXMouse;
    }

    public OptionInstance<Boolean> invertMouseY() {
        return this.invertYMouse;
    }

    public OptionInstance<Boolean> discreteMouseScroll() {
        return this.discreteMouseScroll;
    }

    public OptionInstance<Boolean> realmsNotifications() {
        return this.realmsNotifications;
    }

    public OptionInstance<Boolean> allowServerListing() {
        return this.allowServerListing;
    }

    public OptionInstance<Boolean> reducedDebugInfo() {
        return this.reducedDebugInfo;
    }

    public final float getFinalSoundSourceVolume(SoundSource p_408775_) {
        return p_408775_ == SoundSource.MASTER ? this.getSoundSourceVolume(p_408775_) : this.getSoundSourceVolume(p_408775_) * this.getSoundSourceVolume(SoundSource.MASTER);
    }

    public final float getSoundSourceVolume(SoundSource p_92148_) {
        return this.getSoundSourceOptionInstance(p_92148_).get().floatValue();
    }

    public final OptionInstance<Double> getSoundSourceOptionInstance(SoundSource p_251574_) {
        return Objects.requireNonNull(this.soundSourceVolumes.get(p_251574_));
    }

    private OptionInstance<Double> createSoundSliderOptionInstance(String p_250353_, SoundSource p_249262_) {
        return new OptionInstance<>(p_250353_, OptionInstance.noTooltip(), Options::percentValueOrOffLabel, OptionInstance.UnitDouble.INSTANCE, 1.0, valueIn -> {
            Minecraft minecraft = Minecraft.getInstance();
            SoundManager soundmanager = minecraft.getSoundManager();
            if ((p_249262_ == SoundSource.MASTER || p_249262_ == SoundSource.MUSIC) && this.getFinalSoundSourceVolume(SoundSource.MUSIC) > 0.0F) {
                minecraft.getMusicManager().showNowPlayingToastIfNeeded();
            }

            soundmanager.refreshCategoryVolume(p_249262_);
            if (minecraft.level == null) {
                SoundPreviewHandler.preview(soundmanager, p_249262_, valueIn.floatValue());
            }
        });
    }

    public OptionInstance<Boolean> showSubtitles() {
        return this.showSubtitles;
    }

    public OptionInstance<Boolean> directionalAudio() {
        return this.directionalAudio;
    }

    public OptionInstance<Boolean> backgroundForChatOnly() {
        return this.backgroundForChatOnly;
    }

    public OptionInstance<Boolean> touchscreen() {
        return this.touchscreen;
    }

    public OptionInstance<Boolean> fullscreen() {
        return this.fullscreen;
    }

    public OptionInstance<Boolean> bobView() {
        return this.bobView;
    }

    public OptionInstance<Boolean> toggleCrouch() {
        return this.toggleCrouch;
    }

    public OptionInstance<Boolean> toggleSprint() {
        return this.toggleSprint;
    }

    public OptionInstance<Boolean> toggleAttack() {
        return this.toggleAttack;
    }

    public OptionInstance<Boolean> toggleUse() {
        return this.toggleUse;
    }

    public OptionInstance<Integer> sprintWindow() {
        return this.sprintWindow;
    }

    public OptionInstance<Boolean> hideMatchedNames() {
        return this.hideMatchedNames;
    }

    public OptionInstance<Boolean> showAutosaveIndicator() {
        return this.showAutosaveIndicator;
    }

    public OptionInstance<Boolean> onlyShowSecureChat() {
        return this.onlyShowSecureChat;
    }

    public OptionInstance<Boolean> saveChatDrafts() {
        return this.saveChatDrafts;
    }

    private void setGraphicsPresetToCustom() {
        if (!this.isApplyingGraphicsPreset) {
            this.graphicsPreset.set(GraphicsPreset.CUSTOM);
            if (this.minecraft.screen instanceof OptionsSubScreen optionssubscreen) {
                optionssubscreen.resetOption(this.graphicsPreset);
            }
        }
    }

    public OptionInstance<Integer> fov() {
        return this.fov;
    }

    public OptionInstance<Boolean> telemetryOptInExtra() {
        return this.telemetryOptInExtra;
    }

    public OptionInstance<Double> screenEffectScale() {
        return this.screenEffectScale;
    }

    public OptionInstance<Double> fovEffectScale() {
        return this.fovEffectScale;
    }

    public OptionInstance<Double> darknessEffectScale() {
        return this.darknessEffectScale;
    }

    public OptionInstance<Double> glintSpeed() {
        return this.glintSpeed;
    }

    public OptionInstance<Double> glintStrength() {
        return this.glintStrength;
    }

    public OptionInstance<Double> damageTiltStrength() {
        return this.damageTiltStrength;
    }

    public OptionInstance<Double> gamma() {
        return this.gamma;
    }

    public OptionInstance<Integer> guiScale() {
        return this.guiScale;
    }

    public OptionInstance<ParticleStatus> particles() {
        return this.particles;
    }

    public OptionInstance<NarratorStatus> narrator() {
        return this.narrator;
    }

    public OptionInstance<String> soundDevice() {
        return this.soundDevice;
    }

    public void onboardingAccessibilityFinished() {
        this.onboardAccessibility = false;
        this.save();
    }

    public OptionInstance<MusicManager.MusicFrequency> musicFrequency() {
        return this.musicFrequency;
    }

    public OptionInstance<MusicToastDisplayState> musicToast() {
        return this.musicToast;
    }

    public Options(Minecraft p_92138_, File p_92139_) {
        this.setForgeKeybindProperties();
        long i = 1000000L;
        int j = 32;
        if (Runtime.getRuntime().maxMemory() >= 1500L * i) {
            j = 48;
        }

        if (Runtime.getRuntime().maxMemory() >= 2500L * i) {
            j = 64;
        }

        this.minecraft = p_92138_;
        this.optionsFile = new File(p_92139_, "options.txt");
        boolean flag = Runtime.getRuntime().maxMemory() >= 1000000000L;
        this.renderDistance = new OptionInstance<>(
            "options.renderDistance",
            OptionInstance.noTooltip(),
            (p_231961_0_, p_231961_1_) -> genericValueLabel(p_231961_0_, Component.translatable("options.chunks", p_231961_1_)),
            new OptionInstance.IntRange(2, flag ? j : 16, false),
            12,
            p_438313_1_ -> {
                operateOnLevelRenderer(LevelRenderer::needsUpdate);
                this.setGraphicsPresetToCustom();
            }
        );
        this.simulationDistance = new OptionInstance<>(
            "options.simulationDistance",
            OptionInstance.noTooltip(),
            (p_231915_0_, p_231915_1_) -> genericValueLabel(p_231915_0_, Component.translatable("options.chunks", p_231915_1_)),
            new OptionInstance.IntRange(SharedConstants.DEBUG_ALLOW_LOW_SIM_DISTANCE ? 2 : 5, flag ? 32 : 16, false),
            12,
            p_438300_1_ -> this.setGraphicsPresetToCustom()
        );
        this.syncWrites = Util.getPlatform() == Util.OS.WINDOWS;
        this.RENDER_DISTANCE = this.renderDistance;
        this.SIMULATION_DISTANCE = this.simulationDistance;
        this.optionsFileOF = new File(p_92139_, "optionsof.txt");
        this.framerateLimit().set(this.framerateLimit().getMaxValue());
        this.ofKeyBindZoom = new KeyMapping("of.key.zoom", 67, KeyMapping.Category.MISC);
        this.keyMappings = (KeyMapping[])ArrayUtils.addObjectToArray(this.keyMappings, this.ofKeyBindZoom);
        KeyUtils.fixKeyConflicts(this.keyMappings, new KeyMapping[]{this.ofKeyBindZoom});
        this.renderDistance.set(8);
        this.load();
        Config.initGameSettings(this);
    }

    public float getBackgroundOpacity(float p_92142_) {
        return this.backgroundForChatOnly.get() ? p_92142_ : this.textBackgroundOpacity().get().floatValue();
    }

    public int getBackgroundColor(float p_92171_) {
        return ARGB.colorFromFloat(this.getBackgroundOpacity(p_92171_), 0.0F, 0.0F, 0.0F);
    }

    public int getBackgroundColor(int p_92144_) {
        return this.backgroundForChatOnly.get() ? p_92144_ : ARGB.colorFromFloat(this.textBackgroundOpacity.get().floatValue(), 0.0F, 0.0F, 0.0F);
    }

    private void processDumpedOptions(Options.OptionAccess p_329807_) {
        p_329807_.process("ao", this.ambientOcclusion);
        p_329807_.process("biomeBlendRadius", this.biomeBlendRadius);
        p_329807_.process("chunkSectionFadeInTime", this.chunkSectionFadeInTime);
        p_329807_.process("cutoutLeaves", this.cutoutLeaves);
        p_329807_.process("enableVsync", this.enableVsync);
        p_329807_.process("entityDistanceScaling", this.entityDistanceScaling);
        p_329807_.process("entityShadows", this.entityShadows);
        p_329807_.process("forceUnicodeFont", this.forceUnicodeFont);
        p_329807_.process("japaneseGlyphVariants", this.japaneseGlyphVariants);
        p_329807_.process("fov", this.fov);
        p_329807_.process("fovEffectScale", this.fovEffectScale);
        p_329807_.process("darknessEffectScale", this.darknessEffectScale);
        p_329807_.process("glintSpeed", this.glintSpeed);
        p_329807_.process("glintStrength", this.glintStrength);
        p_329807_.process("graphicsPreset", this.graphicsPreset);
        p_329807_.process("prioritizeChunkUpdates", this.prioritizeChunkUpdates);
        p_329807_.process("fullscreen", this.fullscreen);
        p_329807_.process("gamma", this.gamma);
        p_329807_.process("guiScale", this.guiScale);
        p_329807_.process("maxAnisotropyBit", this.maxAnisotropyBit);
        p_329807_.process("textureFiltering", this.textureFiltering);
        p_329807_.process("maxFps", this.framerateLimit);
        p_329807_.process("improvedTransparency", this.improvedTransparency);
        p_329807_.process("inactivityFpsLimit", this.inactivityFpsLimit);
        p_329807_.process("mipmapLevels", this.mipmapLevels);
        p_329807_.process("narrator", this.narrator);
        p_329807_.process("particles", this.particles);
        p_329807_.process("reducedDebugInfo", this.reducedDebugInfo);
        p_329807_.process("renderClouds", this.cloudStatus);
        p_329807_.process("cloudRange", this.cloudRange);
        p_329807_.process("renderDistance", this.renderDistance);
        p_329807_.process("simulationDistance", this.simulationDistance);
        p_329807_.process("screenEffectScale", this.screenEffectScale);
        p_329807_.process("soundDevice", this.soundDevice);
        p_329807_.process("vignette", this.vignette);
        p_329807_.process("weatherRadius", this.weatherRadius);
    }

    private void processOptions(Options.FieldAccess p_168428_) {
        this.processDumpedOptions(p_168428_);
        p_168428_.process("autoJump", this.autoJump);
        p_168428_.process("rotateWithMinecart", this.rotateWithMinecart);
        p_168428_.process("operatorItemsTab", this.operatorItemsTab);
        p_168428_.process("autoSuggestions", this.autoSuggestions);
        p_168428_.process("chatColors", this.chatColors);
        p_168428_.process("chatLinks", this.chatLinks);
        p_168428_.process("chatLinksPrompt", this.chatLinksPrompt);
        p_168428_.process("discrete_mouse_scroll", this.discreteMouseScroll);
        p_168428_.process("invertXMouse", this.invertXMouse);
        p_168428_.process("invertYMouse", this.invertYMouse);
        p_168428_.process("realmsNotifications", this.realmsNotifications);
        p_168428_.process("showSubtitles", this.showSubtitles);
        p_168428_.process("directionalAudio", this.directionalAudio);
        p_168428_.process("touchscreen", this.touchscreen);
        p_168428_.process("bobView", this.bobView);
        p_168428_.process("toggleCrouch", this.toggleCrouch);
        p_168428_.process("toggleSprint", this.toggleSprint);
        p_168428_.process("toggleAttack", this.toggleAttack);
        p_168428_.process("toggleUse", this.toggleUse);
        p_168428_.process("sprintWindow", this.sprintWindow);
        p_168428_.process("darkMojangStudiosBackground", this.darkMojangStudiosBackground);
        p_168428_.process("hideLightningFlashes", this.hideLightningFlash);
        p_168428_.process("hideSplashTexts", this.hideSplashTexts);
        p_168428_.process("mouseSensitivity", this.sensitivity);
        p_168428_.process("damageTiltStrength", this.damageTiltStrength);
        p_168428_.process("highContrast", this.highContrast);
        p_168428_.process("highContrastBlockOutline", this.highContrastBlockOutline);
        p_168428_.process("narratorHotkey", this.narratorHotkey);
        this.resourcePacks = p_168428_.process("resourcePacks", this.resourcePacks, Options::readListOfStrings, GSON::toJson);
        this.incompatibleResourcePacks = p_168428_.process("incompatibleResourcePacks", this.incompatibleResourcePacks, Options::readListOfStrings, GSON::toJson);
        this.lastMpIp = p_168428_.process("lastServer", this.lastMpIp);
        this.languageCode = p_168428_.process("lang", this.languageCode);
        p_168428_.process("chatVisibility", this.chatVisibility);
        p_168428_.process("chatOpacity", this.chatOpacity);
        p_168428_.process("chatLineSpacing", this.chatLineSpacing);
        p_168428_.process("textBackgroundOpacity", this.textBackgroundOpacity);
        p_168428_.process("backgroundForChatOnly", this.backgroundForChatOnly);
        this.hideServerAddress = p_168428_.process("hideServerAddress", this.hideServerAddress);
        this.advancedItemTooltips = p_168428_.process("advancedItemTooltips", this.advancedItemTooltips);
        this.pauseOnLostFocus = p_168428_.process("pauseOnLostFocus", this.pauseOnLostFocus);
        this.overrideWidth = p_168428_.process("overrideWidth", this.overrideWidth);
        this.overrideHeight = p_168428_.process("overrideHeight", this.overrideHeight);
        p_168428_.process("chatHeightFocused", this.chatHeightFocused);
        p_168428_.process("chatDelay", this.chatDelay);
        p_168428_.process("chatHeightUnfocused", this.chatHeightUnfocused);
        p_168428_.process("chatScale", this.chatScale);
        p_168428_.process("chatWidth", this.chatWidth);
        p_168428_.process("notificationDisplayTime", this.notificationDisplayTime);
        this.useNativeTransport = p_168428_.process("useNativeTransport", this.useNativeTransport);
        p_168428_.process("mainHand", this.mainHand);
        p_168428_.process("attackIndicator", this.attackIndicator);
        this.tutorialStep = p_168428_.process("tutorialStep", this.tutorialStep, TutorialSteps::getByName, TutorialSteps::getName);
        p_168428_.process("mouseWheelSensitivity", this.mouseWheelSensitivity);
        p_168428_.process("rawMouseInput", this.rawMouseInput);
        p_168428_.process("allowCursorChanges", this.allowCursorChanges);
        this.glDebugVerbosity = p_168428_.process("glDebugVerbosity", this.glDebugVerbosity);
        this.skipMultiplayerWarning = p_168428_.process("skipMultiplayerWarning", this.skipMultiplayerWarning);
        p_168428_.process("hideMatchedNames", this.hideMatchedNames);
        this.joinedFirstServer = p_168428_.process("joinedFirstServer", this.joinedFirstServer);
        this.syncWrites = p_168428_.process("syncChunkWrites", this.syncWrites);
        p_168428_.process("showAutosaveIndicator", this.showAutosaveIndicator);
        p_168428_.process("allowServerListing", this.allowServerListing);
        p_168428_.process("onlyShowSecureChat", this.onlyShowSecureChat);
        p_168428_.process("saveChatDrafts", this.saveChatDrafts);
        p_168428_.process("panoramaScrollSpeed", this.panoramaSpeed);
        p_168428_.process("telemetryOptInExtra", this.telemetryOptInExtra);
        this.onboardAccessibility = p_168428_.process("onboardAccessibility", this.onboardAccessibility);
        p_168428_.process("menuBackgroundBlurriness", this.menuBackgroundBlurriness);
        this.startedCleanly = p_168428_.process("startedCleanly", this.startedCleanly);
        p_168428_.process("musicToast", this.musicToast);
        p_168428_.process("musicFrequency", this.musicFrequency);
        this.processOptionsKeysOnly(p_168428_);
        this.processOptionsEnd(p_168428_);
    }

    private void processOptionsKeysOnly(Options.FieldAccess fieldAccessIn) {
        for (KeyMapping keymapping : this.keyMappings) {
            String s = keymapping.saveString();
            if (Reflector.ForgeKeyBinding_getKeyModifier.exists()) {
                Object object = Reflector.call(keymapping, Reflector.ForgeKeyBinding_getKeyModifier);
                Object object1 = Reflector.getFieldValue(Reflector.KeyModifier_NONE);
                s = keymapping.saveString() + (object != object1 ? ":" + object : "");
            }

            String s1 = fieldAccessIn.process("key_" + keymapping.getName(), s);
            if (!s.equals(s1)) {
                keymapping.setKey(InputConstants.getKey(s1));
                if (Reflector.KeyModifier_valueFromString.exists()) {
                    if (s1.indexOf(58) != -1) {
                        String[] astring = s1.split(":");
                        Object object2 = Reflector.call(Reflector.KeyModifier_valueFromString, astring[1]);
                        Reflector.call(keymapping, Reflector.ForgeKeyBinding_setKeyModifierAndCode, object2, InputConstants.getKey(astring[0]));
                    } else {
                        Object object3 = Reflector.getFieldValue(Reflector.KeyModifier_NONE);
                        Reflector.call(keymapping, Reflector.ForgeKeyBinding_setKeyModifierAndCode, object3, InputConstants.getKey(s1));
                    }
                }
            }
        }
    }

    private void processOptionsEnd(Options.FieldAccess fieldAccessIn) {
        for (SoundSource soundsource : SoundSource.values()) {
            fieldAccessIn.process("soundCategory_" + soundsource.getName(), this.soundSourceVolumes.get(soundsource));
        }

        for (PlayerModelPart playermodelpart : PlayerModelPart.values()) {
            boolean flag = this.modelParts.contains(playermodelpart);
            boolean flag1 = fieldAccessIn.process("modelPart_" + playermodelpart.getId(), flag);
            if (flag1 != flag) {
                this.setModelPart(playermodelpart, flag1);
            }
        }
    }

    public void load() {
        this.load(false);
    }

    public void load(boolean limited) {
        try {
            if (!this.optionsFile.exists()) {
                return;
            }

            CompoundTag compoundtag = new CompoundTag();

            try (BufferedReader bufferedreader = Files.newReader(this.optionsFile, StandardCharsets.UTF_8)) {
                bufferedreader.lines().forEach(lineIn -> {
                    try {
                        Iterator<String> iterator = OPTION_SPLITTER.split(lineIn).iterator();
                        compoundtag.putString(iterator.next(), iterator.next());
                    } catch (Exception exception1) {
                        LOGGER.warn("Skipping bad option: {}", lineIn);
                    }
                });
            }

            final CompoundTag compoundtag1 = this.dataFix(compoundtag);
            Consumer<Options.FieldAccess> consumer = limited ? this::processOptionsKeysOnly : this::processOptions;
            consumer.accept(
                new Options.FieldAccess() {
                    private @Nullable String getValue(String p_394210_) {
                        Tag tag = compoundtag1.get(p_394210_);
                        if (tag == null) {
                            return null;
                        } else if (tag instanceof StringTag(String s)) {
                            return s;
                        } else {
                            throw new IllegalStateException("Cannot read field of wrong type, expected string: " + tag);
                        }
                    }

                    @Override
                    public <T> void process(String p_232125_, OptionInstance<T> p_232126_) {
                        String s = this.getValue(p_232125_);
                        if (s != null) {
                            JsonElement jsonelement = LenientJsonParser.parse(s.isEmpty() ? "\"\"" : s);
                            p_232126_.codec()
                                .parse(JsonOps.INSTANCE, jsonelement)
                                .ifError(
                                    p_383646_2_ -> Options.LOGGER
                                        .error("Error parsing option value {} for option {}: {}", s, p_232126_, p_383646_2_.message())
                                )
                                .ifSuccess(p_232126_::set);
                        }
                    }

                    @Override
                    public int process(String p_168467_, int p_168468_) {
                        String s = this.getValue(p_168467_);
                        if (s != null) {
                            try {
                                return Integer.parseInt(s);
                            } catch (NumberFormatException numberformatexception) {
                                Options.LOGGER.warn("Invalid integer value for option {} = {}", p_168467_, s, numberformatexception);
                            }
                        }

                        return p_168468_;
                    }

                    @Override
                    public boolean process(String p_168483_, boolean p_168484_) {
                        String s = this.getValue(p_168483_);
                        return s != null ? Options.isTrue(s) : p_168484_;
                    }

                    @Override
                    public String process(String p_168480_, String p_168481_) {
                        return MoreObjects.firstNonNull(this.getValue(p_168480_), p_168481_);
                    }

                    @Override
                    public float process(String p_168464_, float p_168465_) {
                        String s = this.getValue(p_168464_);
                        if (s == null) {
                            return p_168465_;
                        }

                        if (Options.isTrue(s)) {
                            return 1.0F;
                        }

                        if (Options.isFalse(s)) {
                            return 0.0F;
                        }

                        try {
                            return Float.parseFloat(s);
                        } catch (NumberFormatException numberformatexception) {
                            Options.LOGGER.warn("Invalid floating point value for option {} = {}", p_168464_, s, numberformatexception);
                            return p_168465_;
                        }
                    }

                    @Override
                    public <T> T process(String p_168470_, T p_168471_, Function<String, T> p_168472_, Function<T, String> p_168473_) {
                        String s = this.getValue(p_168470_);
                        return s == null ? p_168471_ : p_168472_.apply(s);
                    }
                }
            );
            compoundtag1.getString("fullscreenResolution").ifPresent(p_383644_1_ -> this.fullscreenVideoModeString = p_383644_1_);
            KeyMapping.resetMapping();
            if (limited) {
                this.unknownKeys.clear();
            } else {
                Set<String> set = Arrays.stream(this.keyMappings).map(k -> "key_" + k.getName()).collect(Collectors.toSet());

                for (Entry<String, Tag> entry : compoundtag1.entrySet()) {
                    if (entry.getKey().startsWith("key_") && entry.getValue().asString().isPresent() && !set.contains(entry.getKey())) {
                        this.unknownKeys.put(entry.getKey(), entry.getValue().asString().get());
                    }
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to load options", exception);
        }

        this.loadOfOptions();
        this.postLoadOptions();
    }

    static boolean isTrue(String p_168436_) {
        return "true".equals(p_168436_);
    }

    static boolean isFalse(String p_168441_) {
        return "false".equals(p_168441_);
    }

    private CompoundTag dataFix(CompoundTag p_92165_) {
        int i = 0;

        try {
            i = p_92165_.getString("version").map(Integer::parseInt).orElse(0);
        } catch (RuntimeException runtimeexception) {
        }

        return DataFixTypes.OPTIONS.updateToCurrentVersion(this.minecraft.getFixerUpper(), p_92165_, i);
    }

    public void save() {
        try (final PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.optionsFile), StandardCharsets.UTF_8))) {
            printwriter.println("version:" + SharedConstants.getCurrentVersion().dataVersion().version());
            final HashSet<String> hashset = new HashSet<>();
            this.processOptions(
                new Options.FieldAccess() {
                    public void writePrefix(String p_168491_) {
                        hashset.add(p_168491_);
                        printwriter.print(p_168491_);
                        printwriter.print(':');
                    }

                    @Override
                    public <T> void process(String p_232135_, OptionInstance<T> p_232136_) {
                        p_232136_.codec()
                            .encodeStart(JsonOps.INSTANCE, p_232136_.get())
                            .ifError(p_414889_1_ -> Options.LOGGER.error("Error saving option {}: {}", p_232136_, p_414889_1_.message()))
                            .ifSuccess(jsonElemIn -> {
                                this.writePrefix(p_232135_);
                                printwriter.println(Options.GSON.toJson(jsonElemIn));
                            });
                    }

                    @Override
                    public int process(String p_168499_, int p_168500_) {
                        this.writePrefix(p_168499_);
                        printwriter.println(p_168500_);
                        return p_168500_;
                    }

                    @Override
                    public boolean process(String p_168515_, boolean p_168516_) {
                        this.writePrefix(p_168515_);
                        printwriter.println(p_168516_);
                        return p_168516_;
                    }

                    @Override
                    public String process(String p_168512_, String p_168513_) {
                        this.writePrefix(p_168512_);
                        printwriter.println(p_168513_);
                        return p_168513_;
                    }

                    @Override
                    public float process(String p_168496_, float p_168497_) {
                        this.writePrefix(p_168496_);
                        printwriter.println(p_168497_);
                        return p_168497_;
                    }

                    @Override
                    public <T> T process(String p_168502_, T p_168503_, Function<String, T> p_168504_, Function<T, String> p_168505_) {
                        this.writePrefix(p_168502_);
                        printwriter.println(p_168505_.apply(p_168503_));
                        return p_168503_;
                    }
                }
            );
            String s = this.getFullscreenVideoModeString();
            if (s != null) {
                printwriter.println("fullscreenResolution:" + s);
            }

            for (Entry<String, String> entry : this.unknownKeys.entrySet()) {
                if (!hashset.contains(entry.getKey())) {
                    printwriter.println(entry.getKey() + ":" + entry.getValue());
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to save options", exception);
        }

        this.saveOfOptions();
        this.broadcastOptions();
    }

    private @Nullable String getFullscreenVideoModeString() {
        Window window = this.minecraft.getWindow();
        if (window == null) {
            return this.fullscreenVideoModeString;
        } else {
            return window.getPreferredFullscreenVideoMode().isPresent() ? window.getPreferredFullscreenVideoMode().get().write() : null;
        }
    }

    public ClientInformation buildPlayerInformation() {
        int i = 0;

        for (PlayerModelPart playermodelpart : this.modelParts) {
            i |= playermodelpart.getMask();
        }

        return new ClientInformation(
            this.languageCode,
            this.renderDistance.get(),
            this.chatVisibility.get(),
            this.chatColors.get(),
            i,
            this.mainHand.get(),
            this.minecraft.isTextFilteringEnabled(),
            this.allowServerListing.get(),
            this.particles.get()
        );
    }

    public void broadcastOptions() {
        if (!Reflector.ClientModLoader_isLoading.exists() || !Reflector.callBoolean(Reflector.ClientModLoader_isLoading)) {
            if (this.minecraft.player != null) {
                this.minecraft.player.connection.broadcastClientInformation(this.buildPlayerInformation());
            }
        }
    }

    public void setModelPart(PlayerModelPart p_92155_, boolean p_92156_) {
        if (p_92156_) {
            this.modelParts.add(p_92155_);
        } else {
            this.modelParts.remove(p_92155_);
        }
    }

    public boolean isModelPartEnabled(PlayerModelPart p_168417_) {
        return this.modelParts.contains(p_168417_);
    }

    public CloudStatus getCloudsType() {
        return this.cloudStatus.get();
    }

    public boolean useNativeTransport() {
        return this.useNativeTransport;
    }

    public void setOptionFloatValueOF(OptionInstance option, double val) {
        if (option == Option.CLOUD_HEIGHT) {
            this.ofCloudsHeight = val;
        }

        if (option == Option.AO_LEVEL) {
            this.ofAoLevel = val;
            this.minecraft.levelRenderer.allChanged();
        }

        if (option == Option.AA_LEVEL) {
            int i = (int)val;
            if (i > 0 && Config.isShaders()) {
                Config.showGuiMessage(Lang.get("of.message.aa.shaders1"), Lang.get("of.message.aa.shaders2"));
                return;
            }

            if (i > 0 && Config.isGraphicsFabulous()) {
                Config.showGuiMessage(Lang.get("of.message.aa.gf1"), Lang.get("of.message.aa.gf2"));
                return;
            }

            if (i > 0 && Config.getGameSettings().textureFiltering.get() != TextureFilteringMethod.NONE) {
                Config.showGuiMessage(Lang.get("of.message.aa.tf1"), Lang.get("of.message.aa.tf2"));
                return;
            }

            this.ofAaLevel = i;
            this.ofAaLevel = Config.limit(this.ofAaLevel, 0, 16);
        }

        if (option == Option.AF_LEVEL) {
            int j = (int)val;
            if (j > 1 && Config.getGameSettings().textureFiltering.get() != TextureFilteringMethod.NONE) {
                Config.showGuiMessage(Lang.get("of.message.af.tf1"), Lang.get("of.message.af.tf2"));
                return;
            }

            this.ofAfLevel = j;
            this.ofAfLevel = Config.limit(this.ofAfLevel, 1, 16);
            this.minecraft.delayTextureReload();
            Shaders.uninit();
        }

        if (option == Option.MIPMAP_TYPE) {
            int k = (int)val;
            this.ofMipmapType = Config.limit(k, 0, 3);
            this.updateMipmaps();
        }

        this.saveOfOptions();
    }

    public double getOptionFloatValueOF(OptionInstance settingOption) {
        if (settingOption == Option.CLOUD_HEIGHT) {
            return this.ofCloudsHeight;
        } else if (settingOption == Option.AO_LEVEL) {
            return this.ofAoLevel;
        } else if (settingOption == Option.AA_LEVEL) {
            return this.ofAaLevel;
        } else if (settingOption == Option.AF_LEVEL) {
            return this.ofAfLevel;
        } else {
            return settingOption == Option.MIPMAP_TYPE ? this.ofMipmapType : Float.MAX_VALUE;
        }
    }

    public void setOptionValueOF(OptionInstance par1EnumOptions, int par2) {
        if (par1EnumOptions == Option.FOG_FANCY) {
            switch (this.ofFogType) {
                case 2:
                    this.ofFogType = 3;
                    break;
                default:
                    this.ofFogType = 2;
            }
        }

        if (par1EnumOptions == Option.FOG_START) {
            this.ofFogStart += 0.2F;
            if (this.ofFogStart > 0.81F) {
                this.ofFogStart = 0.2F;
            }
        }

        if (par1EnumOptions == Option.SMOOTH_FPS) {
            this.ofSmoothFps = !this.ofSmoothFps;
        }

        if (par1EnumOptions == Option.SMOOTH_WORLD) {
            this.ofSmoothWorld = !this.ofSmoothWorld;
            Config.updateThreadPriorities();
        }

        if (par1EnumOptions == Option.TREES) {
            this.ofTrees = nextValue(this.ofTrees, OF_TREES_VALUES);
            this.cutoutLeaves.set(this.ofTrees != 1);
            this.minecraft.levelRenderer.allChanged();
        }

        if (par1EnumOptions == Option.ANIMATED_WATER) {
            this.ofAnimatedWater++;
            if (this.ofAnimatedWater == 1) {
                this.ofAnimatedWater++;
            }

            if (this.ofAnimatedWater > 2) {
                this.ofAnimatedWater = 0;
            }
        }

        if (par1EnumOptions == Option.ANIMATED_LAVA) {
            this.ofAnimatedLava++;
            if (this.ofAnimatedLava == 1) {
                this.ofAnimatedLava++;
            }

            if (this.ofAnimatedLava > 2) {
                this.ofAnimatedLava = 0;
            }
        }

        if (par1EnumOptions == Option.ANIMATED_FIRE) {
            this.ofAnimatedFire = !this.ofAnimatedFire;
        }

        if (par1EnumOptions == Option.ANIMATED_PORTAL) {
            this.ofAnimatedPortal = !this.ofAnimatedPortal;
        }

        if (par1EnumOptions == Option.ANIMATED_REDSTONE) {
            this.ofAnimatedRedstone = !this.ofAnimatedRedstone;
        }

        if (par1EnumOptions == Option.ANIMATED_EXPLOSION) {
            this.ofAnimatedExplosion = !this.ofAnimatedExplosion;
        }

        if (par1EnumOptions == Option.ANIMATED_FLAME) {
            this.ofAnimatedFlame = !this.ofAnimatedFlame;
        }

        if (par1EnumOptions == Option.ANIMATED_SMOKE) {
            this.ofAnimatedSmoke = !this.ofAnimatedSmoke;
        }

        if (par1EnumOptions == Option.VOID_PARTICLES) {
            this.ofVoidParticles = !this.ofVoidParticles;
        }

        if (par1EnumOptions == Option.WATER_PARTICLES) {
            this.ofWaterParticles = !this.ofWaterParticles;
        }

        if (par1EnumOptions == Option.PORTAL_PARTICLES) {
            this.ofPortalParticles = !this.ofPortalParticles;
        }

        if (par1EnumOptions == Option.POTION_PARTICLES) {
            this.ofPotionParticles = !this.ofPotionParticles;
        }

        if (par1EnumOptions == Option.FIREWORK_PARTICLES) {
            this.ofFireworkParticles = !this.ofFireworkParticles;
        }

        if (par1EnumOptions == Option.DRIPPING_WATER_LAVA) {
            this.ofDrippingWaterLava = !this.ofDrippingWaterLava;
        }

        if (par1EnumOptions == Option.ANIMATED_TERRAIN) {
            this.ofAnimatedTerrain = !this.ofAnimatedTerrain;
        }

        if (par1EnumOptions == Option.ANIMATED_TEXTURES) {
            this.ofAnimatedTextures = !this.ofAnimatedTextures;
        }

        if (par1EnumOptions == Option.RAIN_SPLASH) {
            this.ofRainSplash = !this.ofRainSplash;
        }

        if (par1EnumOptions == Option.LAGOMETER) {
            this.ofLagometer = !this.ofLagometer;
            if (this.minecraft.debugEntries.isOverlayVisible() && this.minecraft.getDebugOverlay().isRenderFpsCharts() != this.ofLagometer) {
                this.minecraft.getDebugOverlay().toggleFpsCharts();
            }
        }

        if (par1EnumOptions == Option.AUTOSAVE_TICKS) {
            int i = 900;
            this.ofAutoSaveTicks = Math.max(this.ofAutoSaveTicks / i * i, i);
            this.ofAutoSaveTicks *= 2;
            if (this.ofAutoSaveTicks > 32 * i) {
                this.ofAutoSaveTicks = i;
            }
        }

        if (par1EnumOptions == Option.BETTER_GRASS) {
            this.ofBetterGrass++;
            if (this.ofBetterGrass > 3) {
                this.ofBetterGrass = 1;
            }

            this.minecraft.levelRenderer.allChanged();
        }

        if (par1EnumOptions == Option.CONNECTED_TEXTURES) {
            this.ofConnectedTextures++;
            if (this.ofConnectedTextures > 3) {
                this.ofConnectedTextures = 1;
            }

            if (this.ofConnectedTextures == 2) {
                this.minecraft.levelRenderer.allChanged();
            } else {
                this.minecraft.delayTextureReload();
            }
        }

        if (par1EnumOptions == Option.WEATHER) {
            this.ofWeather = !this.ofWeather;
        }

        if (par1EnumOptions == Option.SKY) {
            this.ofSky = !this.ofSky;
        }

        if (par1EnumOptions == Option.STARS) {
            this.ofStars = !this.ofStars;
        }

        if (par1EnumOptions == Option.SUN_MOON) {
            this.ofSunMoon = !this.ofSunMoon;
        }

        if (par1EnumOptions == Option.CHUNK_UPDATES) {
            this.ofChunkUpdates++;
            if (this.ofChunkUpdates > 5) {
                this.ofChunkUpdates = 1;
            }
        }

        if (par1EnumOptions == Option.CHUNK_UPDATES_DYNAMIC) {
            this.ofChunkUpdatesDynamic = !this.ofChunkUpdatesDynamic;
        }

        if (par1EnumOptions == Option.TIME) {
            this.ofTime++;
            if (this.ofTime > 2) {
                this.ofTime = 0;
            }
        }

        if (par1EnumOptions == Option.PROFILER) {
            this.ofProfiler = !this.ofProfiler;
            if (this.minecraft.debugEntries.isOverlayVisible() && this.minecraft.getDebugOverlay().isRenderProfilerChart() != this.ofProfiler) {
                this.minecraft.getDebugOverlay().toggleProfilerChart();
            }
        }

        if (par1EnumOptions == Option.BETTER_SNOW) {
            this.ofBetterSnow = !this.ofBetterSnow;
            this.minecraft.levelRenderer.allChanged();
        }

        if (par1EnumOptions == Option.SWAMP_COLORS) {
            this.ofSwampColors = !this.ofSwampColors;
            this.minecraft.levelRenderer.allChanged();
        }

        if (par1EnumOptions == Option.RANDOM_ENTITIES) {
            this.ofRandomEntities = !this.ofRandomEntities;
            this.minecraft.delayTextureReload();
        }

        if (par1EnumOptions == Option.CUSTOM_FONTS) {
            this.ofCustomFonts = !this.ofCustomFonts;
            FontUtils.reloadFonts();
        }

        if (par1EnumOptions == Option.CUSTOM_COLORS) {
            this.ofCustomColors = !this.ofCustomColors;
            CustomColors.update();
            this.minecraft.levelRenderer.allChanged();
        }

        if (par1EnumOptions == Option.CUSTOM_ITEMS) {
            this.ofCustomItems = !this.ofCustomItems;
            this.minecraft.delayTextureReload();
        }

        if (par1EnumOptions == Option.CUSTOM_SKY) {
            this.ofCustomSky = !this.ofCustomSky;
            CustomSky.update();
        }

        if (par1EnumOptions == Option.SHOW_CAPES) {
            this.ofShowCapes = !this.ofShowCapes;
        }

        if (par1EnumOptions == Option.NATURAL_TEXTURES) {
            this.ofNaturalTextures = !this.ofNaturalTextures;
            NaturalTextures.update();
            this.minecraft.levelRenderer.allChanged();
        }

        if (par1EnumOptions == Option.EMISSIVE_TEXTURES) {
            this.ofEmissiveTextures = !this.ofEmissiveTextures;
            this.minecraft.delayTextureReload();
        }

        if (par1EnumOptions == Option.FAST_MATH) {
            this.ofFastMath = !this.ofFastMath;
            Mth.fastMath = this.ofFastMath;
        }

        if (par1EnumOptions == Option.FAST_RENDER) {
            this.ofFastRender = !this.ofFastRender;
        }

        if (par1EnumOptions == Option.LAZY_CHUNK_LOADING) {
            this.ofLazyChunkLoading = !this.ofLazyChunkLoading;
        }

        if (par1EnumOptions == Option.RENDER_REGIONS) {
            this.ofRenderRegions = !this.ofRenderRegions;
            this.minecraft.levelRenderer.allChanged();
        }

        if (par1EnumOptions == Option.SMART_ANIMATIONS) {
            this.ofSmartAnimations = !this.ofSmartAnimations;
            this.minecraft.levelRenderer.allChanged();
        }

        if (par1EnumOptions == Option.ALTERNATE_BLOCKS) {
            this.ofAlternateBlocks = !this.ofAlternateBlocks;
            this.minecraft.levelRenderer.allChanged();
        }

        if (par1EnumOptions == Option.DYNAMIC_LIGHTS) {
            this.ofDynamicLights = nextValue(this.ofDynamicLights, OF_DYNAMIC_LIGHTS);
            DynamicLights.removeLights(this.minecraft.levelRenderer);
        }

        if (par1EnumOptions == Option.SCREENSHOT_SIZE) {
            this.ofScreenshotSize++;
            if (this.ofScreenshotSize > 4) {
                this.ofScreenshotSize = 1;
            }
        }

        if (par1EnumOptions == Option.CUSTOM_ENTITY_MODELS) {
            this.ofCustomEntityModels = !this.ofCustomEntityModels;
            this.minecraft.delayTextureReload();
        }

        if (par1EnumOptions == Option.CUSTOM_GUIS) {
            this.ofCustomGuis = !this.ofCustomGuis;
            CustomGuis.update();
        }

        if (par1EnumOptions == Option.SHOW_GL_ERRORS) {
            this.ofShowGlErrors = !this.ofShowGlErrors;
        }

        if (par1EnumOptions == Option.HELD_ITEM_TOOLTIPS) {
            this.ofHeldItemTooltips = !this.ofHeldItemTooltips;
        }

        if (par1EnumOptions == Option.ADVANCED_TOOLTIPS) {
            this.advancedItemTooltips = !this.advancedItemTooltips;
        }

        if (par1EnumOptions == Option.CHAT_BACKGROUND) {
            if (this.ofChatBackground == 0) {
                this.ofChatBackground = 5;
            } else if (this.ofChatBackground == 5) {
                this.ofChatBackground = 3;
            } else {
                this.ofChatBackground = 0;
            }
        }

        if (par1EnumOptions == Option.CHAT_SHADOW) {
            this.ofChatShadow = !this.ofChatShadow;
        }

        if (par1EnumOptions == Option.TELEMETRY) {
            this.ofTelemetry = nextValue(this.ofTelemetry, OF_TELEMETRY);
        }

        this.saveOfOptions();
    }

    public Component getKeyComponentOF(OptionInstance option) {
        String s = this.getKeyBindingOF(option);
        return s == null ? null : Component.literal(s);
    }

    public String getKeyBindingOF(OptionInstance par1EnumOptions) {
        String s = I18n.get(par1EnumOptions.getResourceKey()) + ": ";
        if (s == null) {
            s = par1EnumOptions.getResourceKey();
        }

        String s1 = s;
        if (par1EnumOptions == this.RENDER_DISTANCE) {
            int j1 = this.renderDistance().get();
            String s2 = I18n.get("of.options.renderDistance.tiny");
            int i = 2;
            if (j1 >= 4) {
                s2 = I18n.get("of.options.renderDistance.short");
                i = 4;
            }

            if (j1 >= 8) {
                s2 = I18n.get("of.options.renderDistance.normal");
                i = 8;
            }

            if (j1 >= 16) {
                s2 = I18n.get("of.options.renderDistance.far");
                i = 16;
            }

            if (j1 >= 32) {
                s2 = Lang.get("of.options.renderDistance.extreme");
                i = 32;
            }

            if (j1 >= 48) {
                s2 = Lang.get("of.options.renderDistance.insane");
                i = 48;
            }

            if (j1 >= 64) {
                s2 = Lang.get("of.options.renderDistance.ludicrous");
                i = 64;
            }

            int j = this.renderDistance().get() - i;
            String s3 = s2;
            if (j > 0) {
                s3 = s3 + "+";
            }

            return s + j1 + " " + s3;
        } else if (par1EnumOptions == Option.FOG_FANCY) {
            switch (this.ofFogType) {
                case 1:
                    return s1 + Lang.getFast();
                case 2:
                    return s1 + Lang.getFancy();
                case 3:
                    return s1 + Lang.getOff();
                default:
                    return s1 + Lang.getOff();
            }
        } else {
            if (par1EnumOptions == Option.FOG_START) {
                return s1 + this.ofFogStart;
            }

            if (par1EnumOptions == Option.MIPMAP_TYPE) {
                return FloatOptions.getText(par1EnumOptions, this.ofMipmapType);
            }

            if (par1EnumOptions == Option.SMOOTH_FPS) {
                return this.ofSmoothFps ? s1 + Lang.getOn() : s1 + Lang.getOff();
            }

            if (par1EnumOptions == Option.SMOOTH_WORLD) {
                return this.ofSmoothWorld ? s1 + Lang.getOn() : s1 + Lang.getOff();
            }

            if (par1EnumOptions == Option.TREES) {
                switch (this.ofTrees) {
                    case 1:
                        return s1 + Lang.getFast();
                    case 2:
                        return s1 + Lang.getFancy();
                    case 3:
                    default:
                        return s1 + Lang.getDefault();
                    case 4:
                        return s1 + Lang.get("of.general.smart");
                }
            } else if (par1EnumOptions == Option.ANIMATED_WATER) {
                switch (this.ofAnimatedWater) {
                    case 1:
                        return s1 + Lang.get("of.options.animation.dynamic");
                    case 2:
                        return s1 + Lang.getOff();
                    default:
                        return s1 + Lang.getOn();
                }
            } else if (par1EnumOptions == Option.ANIMATED_LAVA) {
                switch (this.ofAnimatedLava) {
                    case 1:
                        return s1 + Lang.get("of.options.animation.dynamic");
                    case 2:
                        return s1 + Lang.getOff();
                    default:
                        return s1 + Lang.getOn();
                }
            } else {
                if (par1EnumOptions == Option.ANIMATED_FIRE) {
                    return this.ofAnimatedFire ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.ANIMATED_PORTAL) {
                    return this.ofAnimatedPortal ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.ANIMATED_REDSTONE) {
                    return this.ofAnimatedRedstone ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.ANIMATED_EXPLOSION) {
                    return this.ofAnimatedExplosion ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.ANIMATED_FLAME) {
                    return this.ofAnimatedFlame ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.ANIMATED_SMOKE) {
                    return this.ofAnimatedSmoke ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.VOID_PARTICLES) {
                    return this.ofVoidParticles ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.WATER_PARTICLES) {
                    return this.ofWaterParticles ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.PORTAL_PARTICLES) {
                    return this.ofPortalParticles ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.POTION_PARTICLES) {
                    return this.ofPotionParticles ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.FIREWORK_PARTICLES) {
                    return this.ofFireworkParticles ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.DRIPPING_WATER_LAVA) {
                    return this.ofDrippingWaterLava ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.ANIMATED_TERRAIN) {
                    return this.ofAnimatedTerrain ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.ANIMATED_TEXTURES) {
                    return this.ofAnimatedTextures ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.RAIN_SPLASH) {
                    return this.ofRainSplash ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.LAGOMETER) {
                    return this.ofLagometer ? s1 + Lang.getOn() : s1 + Lang.getOff();
                }

                if (par1EnumOptions == Option.AUTOSAVE_TICKS) {
                    int i1 = 900;
                    if (this.ofAutoSaveTicks <= i1) {
                        return s1 + Lang.get("of.options.save.45s");
                    } else if (this.ofAutoSaveTicks <= 2 * i1) {
                        return s1 + Lang.get("of.options.save.90s");
                    } else if (this.ofAutoSaveTicks <= 4 * i1) {
                        return s1 + Lang.get("of.options.save.3min");
                    } else if (this.ofAutoSaveTicks <= 8 * i1) {
                        return s1 + Lang.get("of.options.save.6min");
                    } else {
                        return this.ofAutoSaveTicks <= 16 * i1 ? s1 + Lang.get("of.options.save.12min") : s1 + Lang.get("of.options.save.24min");
                    }
                } else if (par1EnumOptions == Option.BETTER_GRASS) {
                    switch (this.ofBetterGrass) {
                        case 1:
                            return s1 + Lang.getFast();
                        case 2:
                            return s1 + Lang.getFancy();
                        default:
                            return s1 + Lang.getOff();
                    }
                } else if (par1EnumOptions == Option.CONNECTED_TEXTURES) {
                    switch (this.ofConnectedTextures) {
                        case 1:
                            return s1 + Lang.getFast();
                        case 2:
                            return s1 + Lang.getFancy();
                        default:
                            return s1 + Lang.getOff();
                    }
                } else {
                    if (par1EnumOptions == Option.WEATHER) {
                        return this.ofWeather ? s1 + Lang.getOn() : s1 + Lang.getOff();
                    }

                    if (par1EnumOptions == Option.SKY) {
                        return this.ofSky ? s1 + Lang.getOn() : s1 + Lang.getOff();
                    }

                    if (par1EnumOptions == Option.STARS) {
                        return this.ofStars ? s1 + Lang.getOn() : s1 + Lang.getOff();
                    }

                    if (par1EnumOptions == Option.SUN_MOON) {
                        return this.ofSunMoon ? s1 + Lang.getOn() : s1 + Lang.getOff();
                    }

                    if (par1EnumOptions == Option.CHUNK_UPDATES) {
                        return s1 + this.ofChunkUpdates;
                    }

                    if (par1EnumOptions == Option.CHUNK_UPDATES_DYNAMIC) {
                        return this.ofChunkUpdatesDynamic ? s1 + Lang.getOn() : s1 + Lang.getOff();
                    }

                    if (par1EnumOptions == Option.TIME) {
                        if (this.ofTime == 1) {
                            return s1 + Lang.get("of.options.time.dayOnly");
                        } else {
                            return this.ofTime == 2 ? s1 + Lang.get("of.options.time.nightOnly") : s1 + Lang.getDefault();
                        }
                    } else {
                        if (par1EnumOptions == Option.AA_LEVEL) {
                            return FloatOptions.getText(par1EnumOptions, this.ofAaLevel);
                        }

                        if (par1EnumOptions == Option.AF_LEVEL) {
                            return FloatOptions.getText(par1EnumOptions, this.ofAfLevel);
                        }

                        if (par1EnumOptions == Option.PROFILER) {
                            return this.ofProfiler ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.BETTER_SNOW) {
                            return this.ofBetterSnow ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.SWAMP_COLORS) {
                            return this.ofSwampColors ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.RANDOM_ENTITIES) {
                            return this.ofRandomEntities ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.CUSTOM_FONTS) {
                            return this.ofCustomFonts ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.CUSTOM_COLORS) {
                            return this.ofCustomColors ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.CUSTOM_SKY) {
                            return this.ofCustomSky ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.SHOW_CAPES) {
                            return this.ofShowCapes ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.CUSTOM_ITEMS) {
                            return this.ofCustomItems ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.NATURAL_TEXTURES) {
                            return this.ofNaturalTextures ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.EMISSIVE_TEXTURES) {
                            return this.ofEmissiveTextures ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.FAST_MATH) {
                            return this.ofFastMath ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.FAST_RENDER) {
                            return this.ofFastRender ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.LAZY_CHUNK_LOADING) {
                            return this.ofLazyChunkLoading ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.RENDER_REGIONS) {
                            return this.ofRenderRegions ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.SMART_ANIMATIONS) {
                            return this.ofSmartAnimations ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.ALTERNATE_BLOCKS) {
                            return this.ofAlternateBlocks ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.DYNAMIC_LIGHTS) {
                            int l = indexOf(this.ofDynamicLights, OF_DYNAMIC_LIGHTS);
                            return s1 + getTranslation(KEYS_DYNAMIC_LIGHTS, l);
                        }

                        if (par1EnumOptions == Option.SCREENSHOT_SIZE) {
                            return this.ofScreenshotSize <= 1 ? s1 + Lang.getDefault() : s1 + this.ofScreenshotSize + "x";
                        }

                        if (par1EnumOptions == Option.CUSTOM_ENTITY_MODELS) {
                            return this.ofCustomEntityModels ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.CUSTOM_GUIS) {
                            return this.ofCustomGuis ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.SHOW_GL_ERRORS) {
                            return this.ofShowGlErrors ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.HELD_ITEM_TOOLTIPS) {
                            return this.ofHeldItemTooltips ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.ADVANCED_TOOLTIPS) {
                            return this.advancedItemTooltips ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        }

                        if (par1EnumOptions == Option.CHAT_BACKGROUND) {
                            if (this.ofChatBackground == 3) {
                                return s1 + Lang.getOff();
                            } else {
                                return this.ofChatBackground == 5 ? s1 + Lang.get("of.general.compact") : s1 + Lang.getDefault();
                            }
                        } else if (par1EnumOptions == Option.CHAT_SHADOW) {
                            return this.ofChatShadow ? s1 + Lang.getOn() : s1 + Lang.getOff();
                        } else if (par1EnumOptions == Option.TELEMETRY) {
                            int k = indexOf(this.ofTelemetry, OF_TELEMETRY);
                            return s1 + getTranslation(KEYS_TELEMETRY, k);
                        } else if (par1EnumOptions.isProgressOption()) {
                            double d0 = (Double)par1EnumOptions.get();
                            return d0 == 0.0 ? s1 + I18n.get("options.off") : s1 + (int)(d0 * 100.0) + "%";
                        } else {
                            return null;
                        }
                    }
                }
            }
        }
    }

    public void loadOfOptions() {
        try {
            File file1 = this.optionsFileOF;
            if (!file1.exists()) {
                file1 = this.optionsFile;
            }

            if (!file1.exists()) {
                return;
            }

            List<IPersitentOption> list = this.getPersistentOptions();
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream(file1), StandardCharsets.UTF_8));
            String s = "";

            while ((s = bufferedreader.readLine()) != null) {
                try {
                    String[] astring = s.split(":");
                    if (astring[0].equals("ofRenderDistanceChunks") && astring.length >= 2) {
                        this.renderDistance.set(Integer.valueOf(astring[1]));
                        this.renderDistance.set(Config.limit(this.renderDistance.get(), 2, 1024));
                    }

                    if (astring[0].equals("ofFogType") && astring.length >= 2) {
                        this.ofFogType = Integer.valueOf(astring[1]);
                        this.ofFogType = Config.limit(this.ofFogType, 2, 3);
                    }

                    if (astring[0].equals("ofFogStart") && astring.length >= 2) {
                        this.ofFogStart = Float.valueOf(astring[1]);
                        if (this.ofFogStart < 0.2F) {
                            this.ofFogStart = 0.2F;
                        }

                        if (this.ofFogStart > 0.81F) {
                            this.ofFogStart = 0.8F;
                        }
                    }

                    if (astring[0].equals("ofMipmapType") && astring.length >= 2) {
                        this.ofMipmapType = Integer.valueOf(astring[1]);
                        this.ofMipmapType = Config.limit(this.ofMipmapType, 0, 3);
                    }

                    if (astring[0].equals("ofOcclusionFancy") && astring.length >= 2) {
                        this.ofOcclusionFancy = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofSmoothFps") && astring.length >= 2) {
                        this.ofSmoothFps = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofSmoothWorld") && astring.length >= 2) {
                        this.ofSmoothWorld = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAoLevel") && astring.length >= 2) {
                        this.ofAoLevel = Float.valueOf(astring[1]).floatValue();
                        this.ofAoLevel = Config.limit(this.ofAoLevel, 0.0, 1.0);
                    }

                    if (astring[0].equals("ofCloudsHeight") && astring.length >= 2) {
                        this.ofCloudsHeight = Float.valueOf(astring[1]).floatValue();
                        this.ofCloudsHeight = Config.limit(this.ofCloudsHeight, 0.0, 1.0);
                    }

                    if (astring[0].equals("ofTrees") && astring.length >= 2) {
                        this.ofTrees = Integer.valueOf(astring[1]);
                        this.ofTrees = limit(this.ofTrees, OF_TREES_VALUES);
                    }

                    if (astring[0].equals("ofAnimatedWater") && astring.length >= 2) {
                        this.ofAnimatedWater = Integer.valueOf(astring[1]);
                        this.ofAnimatedWater = Config.limit(this.ofAnimatedWater, 0, 2);
                    }

                    if (astring[0].equals("ofAnimatedLava") && astring.length >= 2) {
                        this.ofAnimatedLava = Integer.valueOf(astring[1]);
                        this.ofAnimatedLava = Config.limit(this.ofAnimatedLava, 0, 2);
                    }

                    if (astring[0].equals("ofAnimatedFire") && astring.length >= 2) {
                        this.ofAnimatedFire = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAnimatedPortal") && astring.length >= 2) {
                        this.ofAnimatedPortal = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAnimatedRedstone") && astring.length >= 2) {
                        this.ofAnimatedRedstone = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAnimatedExplosion") && astring.length >= 2) {
                        this.ofAnimatedExplosion = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAnimatedFlame") && astring.length >= 2) {
                        this.ofAnimatedFlame = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAnimatedSmoke") && astring.length >= 2) {
                        this.ofAnimatedSmoke = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofVoidParticles") && astring.length >= 2) {
                        this.ofVoidParticles = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofWaterParticles") && astring.length >= 2) {
                        this.ofWaterParticles = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofPortalParticles") && astring.length >= 2) {
                        this.ofPortalParticles = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofPotionParticles") && astring.length >= 2) {
                        this.ofPotionParticles = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofFireworkParticles") && astring.length >= 2) {
                        this.ofFireworkParticles = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofDrippingWaterLava") && astring.length >= 2) {
                        this.ofDrippingWaterLava = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAnimatedTerrain") && astring.length >= 2) {
                        this.ofAnimatedTerrain = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAnimatedTextures") && astring.length >= 2) {
                        this.ofAnimatedTextures = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofRainSplash") && astring.length >= 2) {
                        this.ofRainSplash = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofLagometer") && astring.length >= 2) {
                        this.ofLagometer = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAutoSaveTicks") && astring.length >= 2) {
                        this.ofAutoSaveTicks = Integer.valueOf(astring[1]);
                        this.ofAutoSaveTicks = Config.limit(this.ofAutoSaveTicks, 40, 40000);
                    }

                    if (astring[0].equals("ofBetterGrass") && astring.length >= 2) {
                        this.ofBetterGrass = Integer.valueOf(astring[1]);
                        this.ofBetterGrass = Config.limit(this.ofBetterGrass, 1, 3);
                    }

                    if (astring[0].equals("ofConnectedTextures") && astring.length >= 2) {
                        this.ofConnectedTextures = Integer.valueOf(astring[1]);
                        this.ofConnectedTextures = Config.limit(this.ofConnectedTextures, 1, 3);
                    }

                    if (astring[0].equals("ofWeather") && astring.length >= 2) {
                        this.ofWeather = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofSky") && astring.length >= 2) {
                        this.ofSky = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofStars") && astring.length >= 2) {
                        this.ofStars = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofSunMoon") && astring.length >= 2) {
                        this.ofSunMoon = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofChunkUpdates") && astring.length >= 2) {
                        this.ofChunkUpdates = Integer.valueOf(astring[1]);
                        this.ofChunkUpdates = Config.limit(this.ofChunkUpdates, 1, 5);
                    }

                    if (astring[0].equals("ofChunkUpdatesDynamic") && astring.length >= 2) {
                        this.ofChunkUpdatesDynamic = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofTime") && astring.length >= 2) {
                        this.ofTime = Integer.valueOf(astring[1]);
                        this.ofTime = Config.limit(this.ofTime, 0, 2);
                    }

                    if (astring[0].equals("ofAaLevel") && astring.length >= 2) {
                        this.ofAaLevel = Integer.valueOf(astring[1]);
                        this.ofAaLevel = Config.limit(this.ofAaLevel, 0, 16);
                    }

                    if (astring[0].equals("ofAfLevel") && astring.length >= 2) {
                        this.ofAfLevel = Integer.valueOf(astring[1]);
                        this.ofAfLevel = Config.limit(this.ofAfLevel, 1, 16);
                    }

                    if (astring[0].equals("ofProfiler") && astring.length >= 2) {
                        this.ofProfiler = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofBetterSnow") && astring.length >= 2) {
                        this.ofBetterSnow = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofSwampColors") && astring.length >= 2) {
                        this.ofSwampColors = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofRandomEntities") && astring.length >= 2) {
                        this.ofRandomEntities = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofCustomFonts") && astring.length >= 2) {
                        this.ofCustomFonts = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofCustomColors") && astring.length >= 2) {
                        this.ofCustomColors = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofCustomItems") && astring.length >= 2) {
                        this.ofCustomItems = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofCustomSky") && astring.length >= 2) {
                        this.ofCustomSky = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofShowCapes") && astring.length >= 2) {
                        this.ofShowCapes = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofNaturalTextures") && astring.length >= 2) {
                        this.ofNaturalTextures = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofEmissiveTextures") && astring.length >= 2) {
                        this.ofEmissiveTextures = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofLazyChunkLoading") && astring.length >= 2) {
                        this.ofLazyChunkLoading = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofRenderRegions") && astring.length >= 2) {
                        this.ofRenderRegions = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofSmartAnimations") && astring.length >= 2) {
                        this.ofSmartAnimations = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofAlternateBlocks") && astring.length >= 2) {
                        this.ofAlternateBlocks = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofDynamicLights") && astring.length >= 2) {
                        this.ofDynamicLights = Integer.valueOf(astring[1]);
                        this.ofDynamicLights = limit(this.ofDynamicLights, OF_DYNAMIC_LIGHTS);
                    }

                    if (astring[0].equals("ofScreenshotSize") && astring.length >= 2) {
                        this.ofScreenshotSize = Integer.valueOf(astring[1]);
                        this.ofScreenshotSize = Config.limit(this.ofScreenshotSize, 1, 4);
                    }

                    if (astring[0].equals("ofCustomEntityModels") && astring.length >= 2) {
                        this.ofCustomEntityModels = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofCustomGuis") && astring.length >= 2) {
                        this.ofCustomGuis = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofShowGlErrors") && astring.length >= 2) {
                        this.ofShowGlErrors = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofFastMath") && astring.length >= 2) {
                        this.ofFastMath = Boolean.valueOf(astring[1]);
                        Mth.fastMath = this.ofFastMath;
                    }

                    if (astring[0].equals("ofFastRender") && astring.length >= 2) {
                        this.ofFastRender = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofChatBackground") && astring.length >= 2) {
                        this.ofChatBackground = Integer.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofChatShadow") && astring.length >= 2) {
                        this.ofChatShadow = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("ofTelemetry") && astring.length >= 2) {
                        this.ofTelemetry = Integer.valueOf(astring[1]);
                        this.ofTelemetry = limit(this.ofTelemetry, OF_TELEMETRY);
                    }

                    if (astring[0].equals("ofHeldItemTooltips") && astring.length >= 2) {
                        this.ofHeldItemTooltips = Boolean.valueOf(astring[1]);
                    }

                    if (astring[0].equals("key_" + this.ofKeyBindZoom.getName())) {
                        this.ofKeyBindZoom.setKey(InputConstants.getKey(astring[1]));
                    }

                    String s1 = astring[0];
                    String s2 = astring[1];

                    for (IPersitentOption ipersitentoption : list) {
                        if (Config.equals(s1, ipersitentoption.getSaveKey())) {
                            ipersitentoption.loadValue(this, s2);
                        }
                    }
                } catch (Exception exception) {
                    Config.dbg("Skipping bad option: " + s);
                    exception.printStackTrace();
                }
            }

            KeyUtils.fixKeyConflicts(this.keyMappings, new KeyMapping[]{this.ofKeyBindZoom});
            KeyMapping.resetMapping();
            bufferedreader.close();
        } catch (Exception exception1) {
            Config.warn("Failed to load options");
            exception1.printStackTrace();
        }
    }

    private void postLoadOptions() {
        if (this.enableVsync.get()) {
            this.framerateLimit.set(0);
        }

        this.enableVsync.set(this.framerateLimit.get() == 0);
    }

    private void postApplyOptions() {
        if (!this.checkOptionValue(this.improvedTransparency, this.improvedTransparency.get())) {
            this.improvedTransparency.set(false);
        }

        if (!this.checkOptionValue(this.textureFiltering, this.textureFiltering.get())) {
            this.textureFiltering.set(TextureFilteringMethod.NONE);
        }
    }

    public boolean checkOptionValue(OptionInstance optionIn, Object valueIn) {
        if (optionIn == this.improvedTransparency) {
            boolean flag = (Boolean)valueIn;
            if (flag && Config.isShaders()) {
                this.showGuiMessage(Lang.get("of.message.it.sh1"), Lang.get("of.message.it.sh2"), "Improved Transparency is not compatible with Shaders");
                this.graphicsPreset.set(GraphicsPreset.CUSTOM);
                return false;
            }

            if (flag && Config.isAntialiasing()) {
                this.showGuiMessage(Lang.get("of.message.it.aa1"), Lang.get("of.message.it.aa2"), "Improved Transparency is not compatible with Antialiasing");
                this.graphicsPreset.set(GraphicsPreset.CUSTOM);
                return false;
            }
        }

        if (optionIn == this.textureFiltering) {
            TextureFilteringMethod texturefilteringmethod = (TextureFilteringMethod)valueIn;
            if (texturefilteringmethod != TextureFilteringMethod.NONE && Config.isAntialiasing()) {
                this.showGuiMessage(Lang.get("of.message.tf.aa1"), Lang.get("of.message.tf.aa2"), "Texture Filtering is not compatible with Antialiasing");
                this.graphicsPreset.set(GraphicsPreset.CUSTOM);
                return false;
            }

            if (texturefilteringmethod != TextureFilteringMethod.NONE && Config.isAnisotropicFiltering()) {
                this.showGuiMessage(
                    Lang.get("of.message.tf.af1"), Lang.get("of.message.tf.af2"), "Texture Filtering is not compatible with Anisotropic Filtering"
                );
                this.graphicsPreset.set(GraphicsPreset.CUSTOM);
                return false;
            }
        }

        return true;
    }

    private void showGuiMessage(String line1, String line2, String logMessage) {
        if (Minecraft.getInstance().screen == null) {
            Log.warn(logMessage);
        } else {
            Config.showGuiMessage(line1, line2);
        }
    }

    private List<IPersitentOption> getPersistentOptions() {
        List<IPersitentOption> list = new ArrayList<>();

        for (OptionInstance optioninstance : OptionInstance.OPTIONS_BY_KEY.values()) {
            if (optioninstance instanceof IPersitentOption ipersitentoption) {
                list.add(ipersitentoption);
            }
        }

        return list;
    }

    public void saveOfOptions() {
        try {
            PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.optionsFileOF), StandardCharsets.UTF_8));
            printwriter.println("ofFogType:" + this.ofFogType);
            printwriter.println("ofFogStart:" + this.ofFogStart);
            printwriter.println("ofMipmapType:" + this.ofMipmapType);
            printwriter.println("ofOcclusionFancy:" + this.ofOcclusionFancy);
            printwriter.println("ofSmoothFps:" + this.ofSmoothFps);
            printwriter.println("ofSmoothWorld:" + this.ofSmoothWorld);
            printwriter.println("ofAoLevel:" + this.ofAoLevel);
            printwriter.println("ofCloudsHeight:" + this.ofCloudsHeight);
            printwriter.println("ofTrees:" + this.ofTrees);
            printwriter.println("ofAnimatedWater:" + this.ofAnimatedWater);
            printwriter.println("ofAnimatedLava:" + this.ofAnimatedLava);
            printwriter.println("ofAnimatedFire:" + this.ofAnimatedFire);
            printwriter.println("ofAnimatedPortal:" + this.ofAnimatedPortal);
            printwriter.println("ofAnimatedRedstone:" + this.ofAnimatedRedstone);
            printwriter.println("ofAnimatedExplosion:" + this.ofAnimatedExplosion);
            printwriter.println("ofAnimatedFlame:" + this.ofAnimatedFlame);
            printwriter.println("ofAnimatedSmoke:" + this.ofAnimatedSmoke);
            printwriter.println("ofVoidParticles:" + this.ofVoidParticles);
            printwriter.println("ofWaterParticles:" + this.ofWaterParticles);
            printwriter.println("ofPortalParticles:" + this.ofPortalParticles);
            printwriter.println("ofPotionParticles:" + this.ofPotionParticles);
            printwriter.println("ofFireworkParticles:" + this.ofFireworkParticles);
            printwriter.println("ofDrippingWaterLava:" + this.ofDrippingWaterLava);
            printwriter.println("ofAnimatedTerrain:" + this.ofAnimatedTerrain);
            printwriter.println("ofAnimatedTextures:" + this.ofAnimatedTextures);
            printwriter.println("ofRainSplash:" + this.ofRainSplash);
            printwriter.println("ofLagometer:" + this.ofLagometer);
            printwriter.println("ofAutoSaveTicks:" + this.ofAutoSaveTicks);
            printwriter.println("ofBetterGrass:" + this.ofBetterGrass);
            printwriter.println("ofConnectedTextures:" + this.ofConnectedTextures);
            printwriter.println("ofWeather:" + this.ofWeather);
            printwriter.println("ofSky:" + this.ofSky);
            printwriter.println("ofStars:" + this.ofStars);
            printwriter.println("ofSunMoon:" + this.ofSunMoon);
            printwriter.println("ofChunkUpdates:" + this.ofChunkUpdates);
            printwriter.println("ofChunkUpdatesDynamic:" + this.ofChunkUpdatesDynamic);
            printwriter.println("ofTime:" + this.ofTime);
            printwriter.println("ofAaLevel:" + this.ofAaLevel);
            printwriter.println("ofAfLevel:" + this.ofAfLevel);
            printwriter.println("ofProfiler:" + this.ofProfiler);
            printwriter.println("ofBetterSnow:" + this.ofBetterSnow);
            printwriter.println("ofSwampColors:" + this.ofSwampColors);
            printwriter.println("ofRandomEntities:" + this.ofRandomEntities);
            printwriter.println("ofCustomFonts:" + this.ofCustomFonts);
            printwriter.println("ofCustomColors:" + this.ofCustomColors);
            printwriter.println("ofCustomItems:" + this.ofCustomItems);
            printwriter.println("ofCustomSky:" + this.ofCustomSky);
            printwriter.println("ofShowCapes:" + this.ofShowCapes);
            printwriter.println("ofNaturalTextures:" + this.ofNaturalTextures);
            printwriter.println("ofEmissiveTextures:" + this.ofEmissiveTextures);
            printwriter.println("ofLazyChunkLoading:" + this.ofLazyChunkLoading);
            printwriter.println("ofRenderRegions:" + this.ofRenderRegions);
            printwriter.println("ofSmartAnimations:" + this.ofSmartAnimations);
            printwriter.println("ofAlternateBlocks:" + this.ofAlternateBlocks);
            printwriter.println("ofDynamicLights:" + this.ofDynamicLights);
            printwriter.println("ofScreenshotSize:" + this.ofScreenshotSize);
            printwriter.println("ofCustomEntityModels:" + this.ofCustomEntityModels);
            printwriter.println("ofCustomGuis:" + this.ofCustomGuis);
            printwriter.println("ofShowGlErrors:" + this.ofShowGlErrors);
            printwriter.println("ofFastMath:" + this.ofFastMath);
            printwriter.println("ofFastRender:" + this.ofFastRender);
            printwriter.println("ofChatBackground:" + this.ofChatBackground);
            printwriter.println("ofChatShadow:" + this.ofChatShadow);
            printwriter.println("ofTelemetry:" + this.ofTelemetry);
            printwriter.println("ofHeldItemTooltips:" + this.ofHeldItemTooltips);
            printwriter.println("key_" + this.ofKeyBindZoom.getName() + ":" + this.ofKeyBindZoom.saveString());

            for (IPersitentOption ipersitentoption : this.getPersistentOptions()) {
                printwriter.println(ipersitentoption.getSaveKey() + ":" + ipersitentoption.getSaveText(this));
            }

            printwriter.close();
        } catch (Exception exception) {
            Config.warn("Failed to save options");
            exception.printStackTrace();
        }
    }

    public void resetSettings() {
        this.renderDistance.set(8);
        this.simulationDistance.set(8);
        this.entityDistanceScaling.set(1.0);
        this.bobView.set(true);
        this.framerateLimit.set(this.framerateLimit.getMaxValue());
        this.enableVsync.set(false);
        this.updateVSync();
        this.mipmapLevels.set(4);
        this.graphicsPreset.set(GraphicsPreset.FANCY);
        this.ambientOcclusion.set(true);
        this.cloudStatus.set(CloudStatus.FANCY);
        this.fov.set(70);
        this.gamma.set(0.0);
        this.guiScale.set(0);
        this.particles.set(ParticleStatus.ALL);
        this.ofHeldItemTooltips = true;
        this.forceUnicodeFont.set(false);
        this.prioritizeChunkUpdates.set(PrioritizeChunkUpdates.NONE);
        this.ofFogType = 2;
        this.ofFogStart = 0.8F;
        this.ofMipmapType = 0;
        this.ofOcclusionFancy = false;
        this.ofSmartAnimations = false;
        this.ofSmoothFps = false;
        Config.updateAvailableProcessors();
        this.ofSmoothWorld = Config.isSingleProcessor();
        this.ofLazyChunkLoading = false;
        this.ofRenderRegions = false;
        this.ofFastMath = false;
        this.ofFastRender = false;
        this.ofAlternateBlocks = true;
        this.ofDynamicLights = 3;
        this.ofScreenshotSize = 1;
        this.ofCustomEntityModels = true;
        this.ofCustomGuis = true;
        this.ofShowGlErrors = true;
        this.ofChatBackground = 0;
        this.ofChatShadow = true;
        this.ofTelemetry = 0;
        this.ofAoLevel = 1.0;
        this.ofAaLevel = 0;
        this.ofAfLevel = 1;
        this.cloudStatus.set(CloudStatus.FANCY);
        this.ofCloudsHeight = 0.0;
        this.ofTrees = 2;
        this.weatherRadius.set(10);
        this.ofBetterGrass = 3;
        this.ofAutoSaveTicks = 4000;
        this.ofLagometer = false;
        this.ofProfiler = false;
        this.ofWeather = true;
        this.ofSky = true;
        this.ofStars = true;
        this.ofSunMoon = true;
        this.vignette.set(true);
        this.ofChunkUpdates = 1;
        this.ofChunkUpdatesDynamic = false;
        this.ofTime = 0;
        this.ofBetterSnow = false;
        this.ofSwampColors = true;
        this.ofRandomEntities = true;
        this.biomeBlendRadius.set(2);
        this.ofCustomFonts = true;
        this.ofCustomColors = true;
        this.ofCustomItems = true;
        this.ofCustomSky = true;
        this.ofShowCapes = true;
        this.ofConnectedTextures = 2;
        this.ofNaturalTextures = false;
        this.ofEmissiveTextures = true;
        this.ofAnimatedWater = 0;
        this.ofAnimatedLava = 0;
        this.ofAnimatedFire = true;
        this.ofAnimatedPortal = true;
        this.ofAnimatedRedstone = true;
        this.ofAnimatedExplosion = true;
        this.ofAnimatedFlame = true;
        this.ofAnimatedSmoke = true;
        this.ofVoidParticles = true;
        this.ofWaterParticles = true;
        this.ofRainSplash = true;
        this.ofPortalParticles = true;
        this.ofPotionParticles = true;
        this.ofFireworkParticles = true;
        this.ofDrippingWaterLava = true;
        this.ofAnimatedTerrain = true;
        this.ofAnimatedTextures = true;
        this.ofQuickInfo = false;
        this.ofQuickInfoFps = Option.FULL.getValue();
        this.ofQuickInfoChunks = true;
        this.ofQuickInfoEntities = true;
        this.ofQuickInfoParticles = false;
        this.ofQuickInfoUpdates = true;
        this.ofQuickInfoGpu = false;
        this.ofQuickInfoPos = Option.COMPACT.getValue();
        this.ofQuickInfoFacing = Option.OFF.getValue();
        this.ofQuickInfoBiome = false;
        this.ofQuickInfoLight = false;
        this.ofQuickInfoMemory = Option.OFF.getValue();
        this.ofQuickInfoNativeMemory = Option.OFF.getValue();
        this.ofQuickInfoTargetBlock = Option.OFF.getValue();
        this.ofQuickInfoTargetFluid = Option.OFF.getValue();
        this.ofQuickInfoTargetEntity = Option.OFF.getValue();
        this.ofQuickInfoLabels = Option.COMPACT.getValue();
        this.ofQuickInfoBackground = false;
        Shaders.setShaderPack("OFF");
        Shaders.configAntialiasingLevel = 0;
        Shaders.uninit();
        Shaders.storeConfig();
        this.minecraft.delayTextureReload();
        this.save();
    }

    public void updateVSync() {
        if (this.minecraft.getWindow() != null) {
            this.minecraft.getWindow().updateVsync(this.enableVsync.get());
        }
    }

    public void updateMipmaps() {
        if (this.minecraft.screen != null) {
            this.minecraft.updateMaxMipLevel(this.mipmapLevels.get());
            this.minecraft.delayTextureReload();
        }
    }

    public void setAllAnimations(boolean flag) {
        int i = flag ? 0 : 2;
        this.ofAnimatedWater = i;
        this.ofAnimatedLava = i;
        this.ofAnimatedFire = flag;
        this.ofAnimatedPortal = flag;
        this.ofAnimatedRedstone = flag;
        this.ofAnimatedExplosion = flag;
        this.ofAnimatedFlame = flag;
        this.ofAnimatedSmoke = flag;
        this.ofVoidParticles = flag;
        this.ofWaterParticles = flag;
        this.ofRainSplash = flag;
        this.ofPortalParticles = flag;
        this.ofPotionParticles = flag;
        this.ofFireworkParticles = flag;
        this.particles.set(flag ? ParticleStatus.ALL : ParticleStatus.MINIMAL);
        this.ofDrippingWaterLava = flag;
        this.ofAnimatedTerrain = flag;
        this.ofAnimatedTextures = flag;
    }

    public void setAllQuickInfos(boolean flag) {
        if (flag) {
            this.ofQuickInfoFps = Option.FULL.getValue();
            this.ofQuickInfoChunks = true;
            this.ofQuickInfoEntities = true;
            this.ofQuickInfoParticles = true;
            this.ofQuickInfoUpdates = true;
            this.ofQuickInfoGpu = true;
            this.ofQuickInfoPos = Option.FULL.getValue();
            this.ofQuickInfoFacing = Option.FULL.getValue();
            this.ofQuickInfoBiome = true;
            this.ofQuickInfoLight = true;
            this.ofQuickInfoMemory = Option.FULL.getValue();
            this.ofQuickInfoNativeMemory = Option.FULL.getValue();
            this.ofQuickInfoTargetBlock = Option.FULL.getValue();
            this.ofQuickInfoTargetFluid = Option.FULL.getValue();
            this.ofQuickInfoTargetEntity = Option.FULL.getValue();
        } else {
            this.ofQuickInfoFps = Option.OFF.getValue();
            this.ofQuickInfoChunks = false;
            this.ofQuickInfoEntities = false;
            this.ofQuickInfoParticles = false;
            this.ofQuickInfoUpdates = false;
            this.ofQuickInfoGpu = false;
            this.ofQuickInfoPos = Option.OFF.getValue();
            this.ofQuickInfoFacing = Option.OFF.getValue();
            this.ofQuickInfoBiome = false;
            this.ofQuickInfoLight = false;
            this.ofQuickInfoMemory = Option.OFF.getValue();
            this.ofQuickInfoNativeMemory = Option.OFF.getValue();
            this.ofQuickInfoTargetBlock = Option.OFF.getValue();
            this.ofQuickInfoTargetFluid = Option.OFF.getValue();
            this.ofQuickInfoTargetEntity = Option.OFF.getValue();
        }
    }

    private static int nextValue(int val, int[] vals) {
        int i = indexOf(val, vals);
        if (i < 0) {
            return vals[0];
        }

        if (++i >= vals.length) {
            i = 0;
        }

        return vals[i];
    }

    private static int limit(int val, int[] vals) {
        int i = indexOf(val, vals);
        return i < 0 ? vals[0] : val;
    }

    public static int indexOf(int val, int[] vals) {
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == val) {
                return i;
            }
        }

        return -1;
    }

    public static int indexOf(double val, double[] vals) {
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == val) {
                return i;
            }
        }

        return -1;
    }

    private static String getTranslation(String[] strArray, int index) {
        if (index < 0 || index >= strArray.length) {
            index = 0;
        }

        return I18n.get(strArray[index]);
    }

    public static Component genericValueLabel(String keyIn, int valueIn) {
        return genericValueLabel(Component.translatable(keyIn), Component.literal(Integer.toString(valueIn)));
    }

    public static Component genericValueLabel(String keyIn, String valueKeyIn) {
        return genericValueLabel(Component.translatable(keyIn), Component.translatable(valueKeyIn));
    }

    public static Component genericValueLabel(String keyIn, String valueKeyIn, int valueIn) {
        return genericValueLabel(Component.translatable(keyIn), Component.translatable(valueKeyIn, Integer.toString(valueIn)));
    }

    private void setForgeKeybindProperties() {
        if (Reflector.KeyConflictContext_IN_GAME.exists()) {
            if (Reflector.ForgeKeyBinding_setKeyConflictContext.exists()) {
                Object object = Reflector.getFieldValue(Reflector.KeyConflictContext_IN_GAME);
                Reflector.call(this.keyUp, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyLeft, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyDown, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyRight, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyJump, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyShift, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keySprint, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyAttack, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyChat, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyPlayerList, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyCommand, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keyTogglePerspective, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
                Reflector.call(this.keySmoothCamera, Reflector.ForgeKeyBinding_setKeyConflictContext, object);
            }
        }
    }

    public void loadSelectedResourcePacks(PackRepository p_92146_) {
        Set<String> set = Sets.newLinkedHashSet();
        Iterator<String> iterator = this.resourcePacks.iterator();

        while (iterator.hasNext()) {
            String s = iterator.next();
            Pack pack = p_92146_.getPack(s);
            if (pack == null && !s.startsWith("file/")) {
                pack = p_92146_.getPack("file/" + s);
            }

            if (pack == null) {
                LOGGER.warn("Removed resource pack {} from options because it doesn't seem to exist anymore", s);
                iterator.remove();
            } else if (!pack.getCompatibility().isCompatible() && !this.incompatibleResourcePacks.contains(s)) {
                LOGGER.warn("Removed resource pack {} from options because it is no longer compatible", s);
                iterator.remove();
            } else if (pack.getCompatibility().isCompatible() && this.incompatibleResourcePacks.contains(s)) {
                LOGGER.info("Removed resource pack {} from incompatibility list because it's now compatible", s);
                this.incompatibleResourcePacks.remove(s);
            } else {
                set.add(pack.getId());
            }
        }

        p_92146_.setSelected(set);
    }

    public CameraType getCameraType() {
        return this.cameraType;
    }

    public void setCameraType(CameraType p_92158_) {
        this.cameraType = p_92158_;
    }

    private static List<String> readListOfStrings(String p_298720_) {
        List<String> list = GsonHelper.fromNullableJson(GSON, p_298720_, LIST_OF_STRINGS_TYPE);
        return list != null ? list : Lists.newArrayList();
    }

    public File getFile() {
        return this.optionsFile;
    }

    public String dumpOptionsForReport() {
        final List<Pair<String, Object>> list = new ArrayList<>();
        this.processDumpedOptions(new Options.OptionAccess() {
            @Override
            public <T> void process(String p_328704_, OptionInstance<T> p_330356_) {
                list.add(Pair.of(p_328704_, p_330356_.get()));
            }
        });
        list.add(Pair.of("fullscreenResolution", String.valueOf(this.fullscreenVideoModeString)));
        list.add(Pair.of("glDebugVerbosity", this.glDebugVerbosity));
        list.add(Pair.of("overrideHeight", this.overrideHeight));
        list.add(Pair.of("overrideWidth", this.overrideWidth));
        list.add(Pair.of("syncChunkWrites", this.syncWrites));
        list.add(Pair.of("useNativeTransport", this.useNativeTransport));
        list.add(Pair.of("resourcePacks", this.resourcePacks));
        return list.stream()
            .sorted(Comparator.comparing(Pair::getFirst))
            .map(pairIn -> pairIn.getFirst() + ": " + pairIn.getSecond())
            .collect(Collectors.joining(System.lineSeparator()));
    }

    public void setServerRenderDistance(int p_193771_) {
        this.serverRenderDistance = p_193771_;
    }

    public int getEffectiveRenderDistance() {
        return this.serverRenderDistance > 0 ? Math.min(this.renderDistance.get(), this.serverRenderDistance) : this.renderDistance.get();
    }

    private static Component pixelValueLabel(Component p_231953_, int p_231954_) {
        return Component.translatable("options.pixel_value", p_231953_, p_231954_);
    }

    private static Component percentValueLabel(Component p_231898_, double p_231899_) {
        return Component.translatable("options.percent_value", p_231898_, (int)(p_231899_ * 100.0));
    }

    public static Component genericValueLabel(Component p_231922_, Component p_231923_) {
        return Component.translatable("options.generic_value", p_231922_, p_231923_);
    }

    public static Component genericValueLabel(Component p_231901_, int p_231902_) {
        return genericValueLabel(p_231901_, Component.literal(Integer.toString(p_231902_)));
    }

    public static Component genericValueOrOffLabel(Component p_345288_, int p_344826_) {
        return p_344826_ == 0 ? genericValueLabel(p_345288_, CommonComponents.OPTION_OFF) : genericValueLabel(p_345288_, p_344826_);
    }

    private static Component percentValueOrOffLabel(Component p_335881_, double p_328979_) {
        return p_328979_ == 0.0 ? genericValueLabel(p_335881_, CommonComponents.OPTION_OFF) : percentValueLabel(p_335881_, p_328979_);
    }

    interface FieldAccess extends Options.OptionAccess {
        int process(String p_168523_, int p_168524_);

        boolean process(String p_168535_, boolean p_168536_);

        String process(String p_168533_, String p_168534_);

        float process(String p_168521_, float p_168522_);

        <T> T process(String p_168525_, T p_168526_, Function<String, T> p_168527_, Function<T, String> p_168528_);
    }

    interface OptionAccess {
        <T> void process(String p_330128_, OptionInstance<T> p_329013_);
    }
}
