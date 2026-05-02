package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.SortedSet;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.debug.GameTestBlockHighlightRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.ParticlesRenderState;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.BlockPosM;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.DynamicLights;
import net.optifine.EmissiveTextures;
import net.optifine.Lagometer;
import net.optifine.SmartAnimations;
import net.optifine.entity.model.CustomEntityModels;
import net.optifine.reflect.Reflector;
import net.optifine.render.ChunkVisibility;
import net.optifine.render.RegionLayerRenderer;
import net.optifine.render.RenderEnv;
import net.optifine.render.RenderStateManager;
import net.optifine.render.RenderUtils;
import net.optifine.shaders.FrustumDummy;
import net.optifine.shaders.RenderStage;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import net.optifine.util.BiomeUtils;
import net.optifine.util.GpuMemory;
import net.optifine.util.RandomUtils;
import net.optifine.util.RenderChunkUtils;
import net.optifine.util.SectionUtils;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
    private static final Identifier TRANSPARENCY_POST_CHAIN_ID = Identifier.withDefaultNamespace("transparency");
    private static final Identifier ENTITY_OUTLINE_POST_CHAIN_ID = Identifier.withDefaultNamespace("entity_outline");
    public static final int SECTION_SIZE = 16;
    public static final int HALF_SECTION_SIZE = 8;
    public static final int NEARBY_SECTION_DISTANCE_IN_BLOCKS = 32;
    private static final int MINIMUM_TRANSPARENT_SORT_COUNT = 15;
    private static final float CHUNK_VISIBILITY_THRESHOLD = 0.3F;
    private final Minecraft minecraft;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final RenderBuffers renderBuffers;
    private  SkyRenderer skyRenderer;
    private final CloudRenderer cloudRenderer = new CloudRenderer();
    private final WorldBorderRenderer worldBorderRenderer = new WorldBorderRenderer();
    private WeatherEffectRenderer weatherEffectRenderer = new WeatherEffectRenderer();
    private final ParticlesRenderState particlesRenderState = new ParticlesRenderState();
    public final DebugRenderer debugRenderer = new DebugRenderer();
    public final GameTestBlockHighlightRenderer gameTestBlockHighlightRenderer = new GameTestBlockHighlightRenderer();
    protected  ClientLevel level;
    private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections = new ObjectArrayList<>(50);
    private  ViewArea viewArea;
    private int ticks;
    private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap<>();
    private  RenderTarget entityOutlineTarget;
    private final LevelTargetBundle targets = new LevelTargetBundle();
    private int lastCameraSectionX = Integer.MIN_VALUE;
    private int lastCameraSectionY = Integer.MIN_VALUE;
    private int lastCameraSectionZ = Integer.MIN_VALUE;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    private  SectionRenderDispatcher sectionRenderDispatcher;
    private int lastViewDistance = -1;
    private boolean captureFrustum;
    private  Frustum capturedFrustum;
    private  BlockPos lastTranslucentSortBlockPos;
    private int translucencyResortIterationIndex;
    private final LevelRenderState levelRenderState;
    private final SubmitNodeStorage submitNodeStorage;
    private final FeatureRenderDispatcher featureRenderDispatcher;
    private  GpuSampler chunkLayerSampler;
    private final SimpleGizmoCollector collectedGizmos = new SimpleGizmoCollector();
    private LevelRenderer.FinalizedGizmos finalizedGizmos = new LevelRenderer.FinalizedGizmos(new DrawableGizmoPrimitives(), new DrawableGizmoPrimitives());
    private Set<SectionRenderDispatcher.RenderSection> chunksToResortTransparency = new LinkedHashSet<>();
    private int countChunksToUpdate = 0;
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> renderInfosTerrain = new ObjectArrayList<>(1024);
    private LongOpenHashSet renderInfosEntities = new LongOpenHashSet(1024);
    private List<SectionRenderDispatcher.RenderSection> renderInfosTileEntities = new ArrayList<>(1024);
    private ObjectArrayList renderInfosTerrainNormal = new ObjectArrayList(1024);
    private LongOpenHashSet renderInfosEntitiesNormal = new LongOpenHashSet(1024);
    private List renderInfosTileEntitiesNormal = new ArrayList(1024);
    private ObjectArrayList renderInfosTerrainShadow = new ObjectArrayList(1024);
    private LongOpenHashSet renderInfosEntitiesShadow = new LongOpenHashSet(1024);
    private List renderInfosTileEntitiesShadow = new ArrayList(1024);
    protected int renderDistance = 0;
    protected int renderDistanceSq = 0;
    protected int renderDistanceXZSq = 0;
    private RenderEnv renderEnv = new RenderEnv(Blocks.AIR.defaultBlockState(), new BlockPos(0, 0, 0));
    private boolean firstWorldLoad = false;
    private static int renderEntitiesCounter = 0;
    public int loadVisibleChunksCounter = -1;
    public static MessageSignature loadVisibleChunksMessageId = new MessageSignature(RandomUtils.getRandomBytes(256));
    private static boolean ambientOcclusion = false;
    private RegionLayerRenderer regionLayerRenderer = new RegionLayerRenderer();
    private int frameId;
    private boolean debugFixTerrainFrustumShadow;
    private EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> enumMapLayerDraws = new EnumMap<>(ChunkSectionLayer.class);

    public LevelRenderer(
        Minecraft p_422329_,
        EntityRenderDispatcher p_425871_,
        BlockEntityRenderDispatcher p_425970_,
        RenderBuffers p_430941_,
        LevelRenderState p_429507_,
        FeatureRenderDispatcher p_427160_
    ) {
        this.minecraft = p_422329_;
        this.entityRenderDispatcher = p_425871_;
        this.blockEntityRenderDispatcher = p_425970_;
        this.renderBuffers = p_430941_;
        this.submitNodeStorage = p_427160_.getSubmitNodeStorage();
        this.levelRenderState = p_429507_;
        this.featureRenderDispatcher = p_427160_;
        Reflector.ForgeEventFactoryClient_onInitLevelRenderer.call();
    }

    @Override
    public void close() {
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.destroyBuffers();
        }

        if (this.skyRenderer != null) {
            this.skyRenderer.close();
        }

        if (this.chunkLayerSampler != null) {
            this.chunkLayerSampler.close();
        }

        this.cloudRenderer.close();
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_109513_) {
        this.initOutline();
        if (this.skyRenderer != null) {
            this.skyRenderer.close();
        }

        this.skyRenderer = new SkyRenderer(this.minecraft.getTextureManager(), this.minecraft.getAtlasManager());
    }

    public void initOutline() {
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.destroyBuffers();
        }

        this.entityOutlineTarget = new TextureTarget("Entity Outline", this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), true);
    }

    private  PostChain getTransparencyChain() {
        if (!Minecraft.useShaderTransparency()) {
            return null;
        }

        PostChain postchain = this.minecraft.getShaderManager().getPostChain(TRANSPARENCY_POST_CHAIN_ID, LevelTargetBundle.SORTING_TARGETS);
        if (postchain == null) {
            this.minecraft.options.improvedTransparency().set(false);
            this.minecraft.options.save();
        }

        return postchain;
    }

    public void doEntityOutline() {
        if (this.shouldShowEntityOutlines()) {
            this.entityOutlineTarget.blitAndBlendToTexture(this.minecraft.getMainRenderTarget().getColorTextureView());
        }
    }

    public boolean shouldShowEntityOutlines() {
        return !Config.isShaders() && Config.isMainFramebufferEnabled()
            ? !this.minecraft.gameRenderer.isPanoramicMode() && this.entityOutlineTarget != null && this.minecraft.player != null
            : false;
    }

    public void setLevel( ClientLevel p_109702_) {
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;
        this.level = p_109702_;
        if (Config.isDynamicLights()) {
            DynamicLights.clear();
        }

        ChunkVisibility.reset();
        this.renderEnv.reset(null, null);
        BiomeUtils.onWorldChanged(this.level);
        Shaders.checkWorldChanged(this.level);
        if (p_109702_ != null) {
            this.allChanged();
        } else {
            this.entityRenderDispatcher.resetCamera();
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
                this.viewArea = null;
            }

            if (this.sectionRenderDispatcher != null) {
                this.sectionRenderDispatcher.dispose();
            }

            this.sectionRenderDispatcher = null;
            this.sectionOcclusionGraph.waitAndReset(null);
            this.clearVisibleSections();
            this.clearRenderInfos();
        }

        this.gameTestBlockHighlightRenderer.clear();
    }

    private void clearVisibleSections() {
        this.visibleSections.clear();
        this.nearbyVisibleSections.clear();
    }

    public void allChanged() {
        if (this.level != null) {
            this.level.clearTintCaches();
            if (this.sectionRenderDispatcher == null) {
                this.sectionRenderDispatcher = new SectionRenderDispatcher(
                    this.level, this, Util.backgroundExecutor(), this.renderBuffers, this.minecraft.getBlockRenderer(), this.minecraft.getBlockEntityRenderDispatcher()
                );
            } else {
                this.sectionRenderDispatcher.setLevel(this.level);
            }

            this.cloudRenderer.markForRebuild();
            ItemBlockRenderTypes.setCutoutLeaves(Config.isTreesFancy());
            LeavesBlock.setCutoutLeaves(Config.isTreesFancy());
            ModelBlockRenderer.updateAoLightValue();
            if (Config.isDynamicLights()) {
                DynamicLights.clear();
            }

            SmartAnimations.update();
            ambientOcclusion = Minecraft.useAmbientOcclusion();
            this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
            this.renderDistance = this.lastViewDistance * 16;
            this.renderDistanceSq = this.renderDistance * this.renderDistance;
            double d0 = (this.lastViewDistance + 1) * 16;
            this.renderDistanceXZSq = (int)(d0 * d0);
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
            }

            GpuMemory.bufferFreed(GpuMemory.getBufferAllocated());
            this.sectionRenderDispatcher.clearCompileQueue();
            this.viewArea = new ViewArea(this.sectionRenderDispatcher, this.level, this.minecraft.options.getEffectiveRenderDistance(), this);
            this.sectionOcclusionGraph.waitAndReset(this.viewArea);
            this.clearVisibleSections();
            this.clearRenderInfos();
            this.killFrustum();
            Camera camera = this.minecraft.gameRenderer.getMainCamera();
            this.viewArea.repositionCamera(SectionPos.of(camera.position()));
        }
    }

    public void resize(int p_109488_, int p_109489_) {
        this.needsUpdate();
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.resize(p_109488_, p_109489_);
        }
    }

    public  String getSectionStatistics() {
        if (this.viewArea == null) {
            return null;
        }

        int i = this.viewArea.sections.length;
        int j = this.countRenderedSections();
        return String.format(
            Locale.ROOT,
            "C: %d/%d %sD: %d, %s",
            j,
            i,
            this.minecraft.smartCull ? "(s) " : "",
            this.lastViewDistance,
            this.sectionRenderDispatcher == null ? "null" : this.sectionRenderDispatcher.getStats()
        );
    }

    public  SectionRenderDispatcher getSectionRenderDispatcher() {
        return this.sectionRenderDispatcher;
    }

    public double getTotalSections() {
        return this.viewArea == null ? 0.0 : this.viewArea.sections.length;
    }

    public double getLastViewDistance() {
        return this.lastViewDistance;
    }

    public int countRenderedSections() {
        return this.renderInfosTerrain.size();
    }

    public void resetSampler() {
        if (this.chunkLayerSampler != null) {
            this.chunkLayerSampler.close();
        }

        this.chunkLayerSampler = null;
    }

    public  String getEntityStatistics() {
        return this.level == null
            ? null
            : "E: " + this.levelRenderState.entityRenderStates.size() + "/" + this.level.getEntityCount() + ", SD: " + this.level.getServerSimulationDistance();
    }

    private void cullTerrain(Camera p_428507_, Frustum p_426703_, boolean p_430453_) {
        Vec3 vec3 = p_428507_.position();
        if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
            this.allChanged();
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("repositionCamera");
        int i = SectionPos.posToSectionCoord(vec3.x());
        int j = SectionPos.posToSectionCoord(vec3.y());
        int k = SectionPos.posToSectionCoord(vec3.z());
        if (this.lastCameraSectionX != i || this.lastCameraSectionY != j || this.lastCameraSectionZ != k) {
            this.lastCameraSectionX = i;
            this.lastCameraSectionY = j;
            this.lastCameraSectionZ = k;
            this.viewArea.repositionCamera(SectionPos.of(vec3));
            this.worldBorderRenderer.invalidate();
        }

        if (Config.isDynamicLights()) {
            DynamicLights.update(this);
        }

        this.sectionRenderDispatcher.setCameraPosition(vec3);
        double d0 = Math.floor(vec3.x / 8.0);
        double d1 = Math.floor(vec3.y / 8.0);
        double d2 = Math.floor(vec3.z / 8.0);
        if (d0 != this.prevCamX || d1 != this.prevCamY || d2 != this.prevCamZ) {
            this.sectionOcclusionGraph.invalidate();
        }

        this.prevCamX = d0;
        this.prevCamY = d1;
        this.prevCamZ = d2;
        profilerfiller.pop();
        Lagometer.timerVisibility.start();
        if (this.capturedFrustum == null) {
            boolean flag = this.minecraft.smartCull;
            if (p_430453_ && this.level.getBlockState(p_428507_.blockPosition()).isSolidRender()) {
                flag = false;
            }

            profilerfiller.push("updateSOG");
            this.sectionOcclusionGraph.update(flag, p_428507_, p_426703_, this.visibleSections, this.level.getChunkSource().getLoadedEmptySections());
            profilerfiller.pop();
            double d3 = Math.floor(p_428507_.xRot() / 2.0F);
            double d4 = Math.floor(p_428507_.yRot() / 2.0F);
            boolean flag1 = false;
            if (this.sectionOcclusionGraph.consumeFrustumUpdate() || d3 != this.prevCamRotX || d4 != this.prevCamRotY) {
                profilerfiller.push("applyFrustum");
                this.applyFrustum(offsetFrustum(p_426703_));
                profilerfiller.pop();
                this.prevCamRotX = d3;
                this.prevCamRotY = d4;
                flag1 = true;
                ShadersRender.frustumTerrainShadowChanged = true;
            }

            if (this.level.getSectionStorage().resetUpdated() || flag1) {
                this.applyFrustumEntities(p_426703_, -1);
                ShadersRender.frustumEntitiesShadowChanged = true;
            }
        }

        Lagometer.timerVisibility.end();
    }

    public static Frustum offsetFrustum(Frustum p_298803_) {
        return new Frustum(p_298803_).offsetToFullyIncludeCameraCube(8);
    }

    private void applyFrustum(Frustum p_194355_) {
        this.applyFrustum(p_194355_, true, -1);
    }

    public void applyFrustum(Frustum frustumIn, boolean updateRenderInfos, int maxChunkDistance) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        }

        if (updateRenderInfos) {
            this.clearVisibleSections();
        }

        this.clearRenderInfosTerrain();
        this.sectionOcclusionGraph.addSectionsInFrustum(frustumIn, this.visibleSections, this.nearbyVisibleSections, updateRenderInfos, maxChunkDistance);
    }

    public void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection p_301248_) {
        this.sectionOcclusionGraph.schedulePropagationFrom(p_301248_);
    }

    private Frustum prepareCullFrustum(Matrix4f p_254341_, Matrix4f p_332544_, Vec3 p_253766_) {
        Frustum frustum;
        if (this.capturedFrustum != null && !this.captureFrustum) {
            frustum = this.capturedFrustum;
        } else {
            frustum = new Frustum(p_254341_, p_332544_);
            frustum.prepare(p_253766_.x(), p_253766_.y(), p_253766_.z());
        }

        if (this.captureFrustum) {
            this.capturedFrustum = frustum;
            this.captureFrustum = false;
            frustum = this.capturedFrustum;
            frustum.disabled = Config.isShaders() && !Shaders.isFrustumCulling();
            frustum.prepare(p_253766_.x, p_253766_.y, p_253766_.z);
            this.applyFrustum(frustum, false, -1);
            this.applyFrustumEntities(frustum, -1);
        }

        if (this.debugFixTerrainFrustumShadow) {
            boolean flag = this.capturedFrustum != null;
            this.capturedFrustum = flag ? new Frustum(p_254341_, p_332544_) : frustum;
            this.capturedFrustum.prepare(p_253766_.x, p_253766_.y, p_253766_.z);
            this.debugFixTerrainFrustumShadow = false;
            frustum = this.capturedFrustum;
            frustum.prepare(p_253766_.x, p_253766_.y, p_253766_.z);
            ShadersRender.frustumTerrainShadowChanged = true;
            ShadersRender.frustumEntitiesShadowChanged = true;
            ShadersRender.applyFrustumShadow(this, frustum);
        }

        if (Config.isShaders() && !Shaders.isFrustumCulling()) {
            frustum.disabled = true;
        }

        return frustum;
    }

    public void renderLevel(
        GraphicsResourceAllocator p_367325_,
        DeltaTracker p_342180_,
        boolean p_109603_,
        Camera p_109604_,
        Matrix4f p_254120_,
        Matrix4f p_330527_,
        Matrix4f p_429784_,
        GpuBufferSlice p_407881_,
        Vector4f p_410175_,
        boolean p_407316_
    ) {
        float f = p_342180_.getGameTimeDeltaPartialTick(false);
        float f1 = f;
        this.levelRenderState.gameTime = this.level.getGameTime();
        this.blockEntityRenderDispatcher.prepare(p_109604_);
        this.entityRenderDispatcher.prepare(p_109604_, this.minecraft.crosshairPickEntity);
        final ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("populateLightUpdates");
        this.level.pollLightUpdates();
        profilerfiller.popPush("runLightUpdates");
        this.level.getChunkSource().getLightEngine().runLightUpdates();
        profilerfiller.popPush("prepareCullFrustum");
        Vec3 vec3 = p_109604_.position();
        Frustum frustum = this.prepareCullFrustum(p_254120_, p_429784_, vec3);
        if (Config.isShaders()) {
            Shaders.setViewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        } else {
            GlStateManager._viewport(0, 0, this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
        }

        boolean flag = Config.isShaders();
        this.checkLoadVisibleChunks(p_109604_, frustum, this.minecraft.player.isSpectator());
        this.frameId++;
        profilerfiller.popPush("cullTerrain");
        this.cullTerrain(p_109604_, frustum, this.minecraft.player.isSpectator());
        profilerfiller.popPush("compileSections");
        this.compileSections(p_109604_);
        profilerfiller.popPush("extract");
        profilerfiller.push("entities");
        this.extractVisibleEntities(p_109604_, frustum, p_342180_, this.levelRenderState);
        profilerfiller.popPush("blockEntities");
        this.extractVisibleBlockEntities(p_109604_, f, this.levelRenderState, frustum);
        profilerfiller.popPush("blockOutline");
        this.extractBlockOutline(p_109604_, this.levelRenderState, p_109603_);
        if (Reflector.ForgeHooksClient_onExtractBlockOutline.exists()) {
            p_109603_ = this.levelRenderState.blockOutlineRenderState != null;
        }

        profilerfiller.popPush("blockBreaking");
        this.extractBlockDestroyAnimation(p_109604_, this.levelRenderState);
        profilerfiller.popPush("weather");
        this.weatherEffectRenderer.extractRenderState(this.level, this.ticks, f, vec3, this.levelRenderState.weatherRenderState);
        profilerfiller.popPush("sky");
        this.skyRenderer.extractRenderState(this.level, f, p_109604_, this.levelRenderState.skyRenderState);
        profilerfiller.popPush("border");
        this.worldBorderRenderer.extract(this.level.getWorldBorder(), f, vec3, this.minecraft.options.getEffectiveRenderDistance() * 16, this.levelRenderState.worldBorderRenderState);
        profilerfiller.pop();
        profilerfiller.popPush("debug");
        this.debugRenderer.emitGizmos(frustum, vec3.x, vec3.y, vec3.z, p_342180_.getGameTimeDeltaPartialTick(false));
        this.gameTestBlockHighlightRenderer.emitGizmos();
        profilerfiller.popPush("setupFrameGraph");
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(p_254120_);
        FrameGraphBuilder framegraphbuilder = new FrameGraphBuilder();
        this.targets.main = framegraphbuilder.importExternal("main", this.minecraft.getMainRenderTarget());
        int i = this.minecraft.getMainRenderTarget().width;
        int j = this.minecraft.getMainRenderTarget().height;
        RenderTargetDescriptor rendertargetdescriptor = new RenderTargetDescriptor(i, j, true, 0);
        PostChain postchain = this.getTransparencyChain();
        if (postchain != null) {
            this.targets.translucent = framegraphbuilder.createInternal("translucent", rendertargetdescriptor);
            this.targets.itemEntity = framegraphbuilder.createInternal("item_entity", rendertargetdescriptor);
            this.targets.particles = framegraphbuilder.createInternal("particles", rendertargetdescriptor);
            this.targets.weather = framegraphbuilder.createInternal("weather", rendertargetdescriptor);
            this.targets.clouds = framegraphbuilder.createInternal("clouds", rendertargetdescriptor);
        }

        if (this.entityOutlineTarget != null) {
            this.targets.entityOutline = framegraphbuilder.importExternal("entity_outline", this.entityOutlineTarget);
        }

        FramePass framepass = framegraphbuilder.addPass("clear");
        this.targets.main = framepass.readsAndWrites(this.targets.main);
        framepass.executes(
            () -> {
                RenderTarget rendertarget = this.minecraft.getMainRenderTarget();
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorAndDepthTextures(
                        rendertarget.getColorTexture(), ARGB.colorFromFloat(0.0F, p_410175_.x, p_410175_.y, p_410175_.z), rendertarget.getDepthTexture(), 1.0
                    );
                if (flag) {
                    Shaders.clearRenderBuffer();
                    Shaders.setCamera(p_254120_, p_330527_, p_109604_, f1);
                    Shaders.renderPrepare();
                }
            }
        );
        if (p_407316_) {
            this.skyRenderer.setRenderParameters(this.level, f1);
            if ((Config.isSkyEnabled() || Config.isSunMoonEnabled() || Config.isStarsEnabled()) && !Shaders.isShadowPass) {
                this.addSkyPass(framegraphbuilder, p_109604_, p_407881_);
            } else {
                GlStateManager._disableBlend();
            }
        }

        this.addMainPass(framegraphbuilder, frustum, p_254120_, p_407881_, p_109603_, this.levelRenderState, p_342180_, profilerfiller);
        PostChain postchain1 = this.minecraft.getShaderManager().getPostChain(ENTITY_OUTLINE_POST_CHAIN_ID, LevelTargetBundle.OUTLINE_TARGETS);
        if (this.levelRenderState.haveGlowingEntities && postchain1 != null) {
            postchain1.addToFrame(framegraphbuilder, i, j, this.targets);
        }

        this.minecraft.particleEngine.extract(this.particlesRenderState, new Frustum(frustum).offset(-3.0F), p_109604_, f);
        if (!Shaders.isParticlesBeforeDeferred()) {
            this.addParticlesPass(framegraphbuilder, p_407881_);
        }

        CloudStatus cloudstatus = this.minecraft.options.getCloudsType();
        if (cloudstatus != CloudStatus.OFF && !Config.isCloudsOff()) {
            int k = p_109604_.attributeProbe().getValue(EnvironmentAttributes.CLOUD_COLOR, f);
            if (ARGB.alpha(k) > 0) {
                float f2 = p_109604_.attributeProbe().getValue(EnvironmentAttributes.CLOUD_HEIGHT, f);
                f2 += (float)this.minecraft.options.ofCloudsHeight * 128.0F;
                this.addCloudsPass(framegraphbuilder, cloudstatus, this.levelRenderState.cameraRenderState.pos, this.levelRenderState.gameTime, f, k, f2);
            }
        }

        this.addWeatherPass(framegraphbuilder, p_407881_);
        if (postchain != null) {
            postchain.addToFrame(framegraphbuilder, i, j, this.targets);
        }

        this.addLateDebugPass(framegraphbuilder, this.levelRenderState.cameraRenderState, p_407881_, p_254120_);
        if (Reflector.FramePassManager_insertForgePasses.exists()) {
            Reflector.FramePassManager_insertForgePasses.call(framegraphbuilder, this.targets, this.levelRenderState);
        }

        profilerfiller.popPush("executeFrameGraph");
        framegraphbuilder.execute(p_367325_, new FrameGraphBuilder.Inspector() {
            @Override
            public void beforeExecutePass(String p_367748_) {
                profilerfiller.push(p_367748_);
            }

            @Override
            public void afterExecutePass(String p_367757_) {
                profilerfiller.pop();
            }
        });
        this.targets.clear();
        matrix4fstack.popMatrix();
        profilerfiller.pop();
        this.levelRenderState.reset();
    }

    private void addMainPass(
        FrameGraphBuilder p_365119_,
        Frustum p_363733_,
        Matrix4f p_361439_,
        GpuBufferSlice p_407574_,
        boolean p_362593_,
        LevelRenderState p_425668_,
        DeltaTracker p_365046_,
        ProfilerFiller p_369478_
    ) {
        FramePass framepass = p_365119_.addPass("main");
        this.targets.main = framepass.readsAndWrites(this.targets.main);
        if (this.targets.translucent != null) {
            this.targets.translucent = framepass.readsAndWrites(this.targets.translucent);
        }

        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = framepass.readsAndWrites(this.targets.itemEntity);
        }

        if (this.targets.weather != null) {
            this.targets.weather = framepass.readsAndWrites(this.targets.weather);
        }

        if (p_425668_.haveGlowingEntities && this.targets.entityOutline != null) {
            this.targets.entityOutline = framepass.readsAndWrites(this.targets.entityOutline);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.main;
        ResourceHandle<RenderTarget> resourcehandle1 = this.targets.translucent;
        ResourceHandle<RenderTarget> resourcehandle2 = this.targets.itemEntity;
        ResourceHandle<RenderTarget> resourcehandle3 = this.targets.entityOutline;
        framepass.executes(
            () -> {
                RenderSystem.setShaderFog(p_407574_);
                Vec3 vec3 = p_425668_.cameraRenderState.pos;
                double d0 = vec3.x();
                double d1 = vec3.y();
                double d2 = vec3.z();
                p_369478_.push("terrain");
                Lagometer.timerTerrain.start();
                if (this.minecraft.options.ofSmoothFps) {
                    p_369478_.popPush("finish");
                    GL11.glFinish();
                    p_369478_.popPush("terrain");
                }

                boolean flag = Config.isShaders();
                Camera camera = this.minecraft.gameRenderer.getMainCamera();
                if (this.chunkLayerSampler == null) {
                    int i = this.minecraft.options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC ? this.minecraft.options.maxAnisotropyValue() : 1;
                    FilterMode filtermode = Config.isShaders() ? FilterMode.NEAREST : FilterMode.LINEAR;
                    this.chunkLayerSampler = RenderSystem.getDevice()
                        .createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filtermode, filtermode, i, OptionalDouble.empty());
                }

                if (flag) {
                    ShadersRender.beginTerrainSolid();
                }

                ChunkSectionsToRender chunksectionstorender = this.prepareChunkRenders(p_361439_, d0, d1, d2);
                chunksectionstorender.renderGroup(ChunkSectionLayerGroup.OPAQUE, this.chunkLayerSampler);
                if (flag) {
                    ShadersRender.endTerrain();
                }

                this.minecraft.gameRenderer.getLighting().setupFor(Lighting.Entry.LEVEL);
                if (resourcehandle2 != null) {
                    resourcehandle2.get().copyDepthFrom(this.minecraft.getMainRenderTarget());
                }

                if (this.shouldShowEntityOutlines() && resourcehandle3 != null) {
                    RenderTarget rendertarget = resourcehandle3.get();
                    RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(rendertarget.getColorTexture(), 0, rendertarget.getDepthTexture(), 1.0);
                }

                ItemFrameRenderer.updateItemRenderDistance();
                SignRenderer.updateTextRenderDistance();
                renderEntitiesCounter++;
                PoseStack posestack = new PoseStack();
                MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
                MultiBufferSource.BufferSource multibuffersource$buffersource1 = this.renderBuffers.crumblingBufferSource();
                if (Config.isFastRender()) {
                    RenderStateManager.enableCache();
                    multibuffersource$buffersource.enableCache();
                }

                if (flag) {
                    Shaders.beginEntities();
                }

                p_369478_.popPush("submitEntities");
                this.submitEntities(posestack, p_425668_, this.submitNodeStorage);
                if (flag) {
                    p_369478_.popPush("renderEntities");
                    this.featureRenderDispatcher.renderAllFeatures();
                    multibuffersource$buffersource.endLastBatch();
                    Shaders.endEntities();
                }

                if (flag) {
                    Shaders.beginBlockEntities();
                }

                p_369478_.popPush("submitBlockEntities");
                this.submitBlockEntities(posestack, p_425668_, this.submitNodeStorage);
                p_369478_.popPush("renderFeatures");
                this.featureRenderDispatcher.renderAllFeatures();
                multibuffersource$buffersource.endLastBatch();
                if (flag) {
                    Shaders.endBlockEntities();
                }

                this.checkPoseStack(posestack);
                if (Config.isFastRender()) {
                    multibuffersource$buffersource.flushCache();
                    RenderStateManager.flushCache();
                }

                multibuffersource$buffersource.endBatch(RenderTypes.solidMovingBlock());
                multibuffersource$buffersource.endBatch(RenderTypes.endPortal());
                multibuffersource$buffersource.endBatch(RenderTypes.endGateway());
                multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
                multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
                multibuffersource$buffersource.endBatch(Sheets.bedSheet());
                multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
                multibuffersource$buffersource.endBatch(Sheets.signSheet());
                multibuffersource$buffersource.endBatch(Sheets.hangingSignSheet());
                multibuffersource$buffersource.endBatch(Sheets.chestSheet());
                this.renderBuffers.outlineBufferSource().endOutlineBatch();
                if (Config.isFastRender()) {
                    multibuffersource$buffersource.disableCache();
                    RenderStateManager.disableCache();
                }

                Lagometer.timerTerrain.end();
                if (p_362593_) {
                    this.renderBlockOutline(multibuffersource$buffersource, posestack, false, p_425668_);
                }

                if (flag) {
                    RenderUtils.finishRenderBuffers();
                    ShadersRender.beginDebug();
                }

                p_369478_.pop();
                this.finalizeGizmoCollection();
                this.finalizedGizmos.standardPrimitives().render(posestack, multibuffersource$buffersource, p_425668_.cameraRenderState, p_361439_);
                multibuffersource$buffersource.endLastBatch();
                this.checkPoseStack(posestack);
                multibuffersource$buffersource.endBatch(Sheets.translucentItemSheet());
                multibuffersource$buffersource.endBatch(Sheets.bannerSheet());
                multibuffersource$buffersource.endBatch(Sheets.shieldSheet());
                multibuffersource$buffersource.endBatch(RenderTypes.armorEntityGlint());
                multibuffersource$buffersource.endBatch(RenderTypes.glint());
                multibuffersource$buffersource.endBatch(RenderTypes.glintTranslucent());
                multibuffersource$buffersource.endBatch(RenderTypes.entityGlint());
                p_369478_.push("destroyProgress");
                this.renderBlockDestroyAnimation(posestack, multibuffersource$buffersource1, p_425668_);
                multibuffersource$buffersource1.endBatch();
                p_369478_.pop();
                this.checkPoseStack(posestack);
                multibuffersource$buffersource.endBatch(RenderTypes.waterMask());
                multibuffersource$buffersource.endBatch();
                RenderUtils.flushRenderBuffers();
                renderEntitiesCounter--;
                if (resourcehandle1 != null) {
                    resourcehandle1.get().copyDepthFrom(resourcehandle.get());
                }

                if (flag) {
                    multibuffersource$buffersource.endBatch();
                    ShadersRender.endDebug();
                    Shaders.preRenderHand();
                    RenderSystem.backupProjectionMatrix();
                    float f = p_365046_.getGameTimeDeltaPartialTick(true);
                    boolean flag1 = this.minecraft.getCameraEntity() instanceof LivingEntity && ((LivingEntity)this.minecraft.getCameraEntity()).isSleeping();
                    RenderSystem.setProjectionMatrix(
                        this.minecraft
                            .gameRenderer
                            .hud3dProjectionMatrixBuffer
                            .getBuffer(
                                this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), this.minecraft.gameRenderer.getFov(camera, f, false)
                            ),
                        ProjectionType.PERSPECTIVE
                    );
                    ShadersRender.renderHandSolid(this.minecraft.gameRenderer, p_361439_, camera, f, flag1);
                    RenderSystem.restoreProjectionMatrix();
                    Shaders.preWater();
                }

                if (Shaders.isParticlesBeforeDeferred()) {
                    p_369478_.popPush("particles");
                    RenderSystem.setShaderFog(p_407574_);
                    this.particlesRenderState.submit(this.submitNodeStorage, this.levelRenderState.cameraRenderState);
                    this.featureRenderDispatcher.renderAllFeatures();
                    this.particlesRenderState.reset();
                }

                p_369478_.push("translucent");
                Lagometer.timerTerrain.start();
                if (flag) {
                    Shaders.beginWater();
                }

                chunksectionstorender.renderGroup(ChunkSectionLayerGroup.TRANSLUCENT, this.chunkLayerSampler);
                if (flag) {
                    Shaders.endWater();
                }

                Lagometer.timerTerrain.end();
                p_369478_.popPush("string");
                chunksectionstorender.renderGroup(ChunkSectionLayerGroup.TRIPWIRE, this.chunkLayerSampler);
                if (p_362593_) {
                    this.renderBlockOutline(multibuffersource$buffersource, posestack, true, p_425668_);
                }

                multibuffersource$buffersource.endBatch();
                p_369478_.pop();
            }
        );
    }

    private void addParticlesPass(FrameGraphBuilder p_366471_, GpuBufferSlice p_405857_) {
        FramePass framepass = p_366471_.addPass("particles");
        if (this.targets.particles != null) {
            this.targets.particles = framepass.readsAndWrites(this.targets.particles);
            framepass.reads(this.targets.main);
        } else {
            this.targets.main = framepass.readsAndWrites(this.targets.main);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.main;
        ResourceHandle<RenderTarget> resourcehandle1 = this.targets.particles;
        framepass.executes(() -> {
            RenderSystem.setShaderFog(p_405857_);
            if (resourcehandle1 != null) {
                resourcehandle1.get().copyDepthFrom(resourcehandle.get());
            }

            this.particlesRenderState.submit(this.submitNodeStorage, this.levelRenderState.cameraRenderState);
            this.featureRenderDispatcher.renderAllFeatures();
            this.particlesRenderState.reset();
        });
    }

    private void addCloudsPass(FrameGraphBuilder p_364518_, CloudStatus p_368512_, Vec3 p_364075_, long p_457289_, float p_369524_, int p_369495_, float p_366207_) {
        this.addCloudsPass(p_364518_, p_368512_, p_364075_, p_457289_, p_369524_, p_369495_, p_366207_, null);
    }

    private void addCloudsPass(
        FrameGraphBuilder builderIn,
        CloudStatus statusIn,
        Vec3 posIn,
        long ticksIn,
        float partialTicks,
        int colorIn,
        float cloudHeightIn,
        GpuBufferSlice fogBufferIn
    ) {
        FramePass framepass = builderIn.addPass("clouds");
        if (this.targets.clouds != null) {
            this.targets.clouds = framepass.readsAndWrites(this.targets.clouds);
        } else {
            this.targets.main = framepass.readsAndWrites(this.targets.main);
        }

        framepass.executes(() -> this.cloudRenderer.render(colorIn, statusIn, cloudHeightIn, posIn, ticksIn, partialTicks, fogBufferIn));
    }

    private void addWeatherPass(FrameGraphBuilder p_362650_, GpuBufferSlice p_408677_) {
        int i = this.minecraft.options.getEffectiveRenderDistance() * 16;
        float f = this.minecraft.gameRenderer.getDepthFar();
        FramePass framepass = p_362650_.addPass("weather");
        if (this.targets.weather != null) {
            this.targets.weather = framepass.readsAndWrites(this.targets.weather);
        } else {
            this.targets.main = framepass.readsAndWrites(this.targets.main);
        }

        framepass.executes(() -> {
            RenderSystem.setShaderFog(p_408677_);
            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
            CameraRenderState camerarenderstate = this.levelRenderState.cameraRenderState;
            if (Config.isShaders()) {
                Shaders.beginWeather();
            }

            this.weatherEffectRenderer.render(multibuffersource$buffersource, camerarenderstate.pos, this.levelRenderState.weatherRenderState);
            if (Config.isShaders()) {
                Shaders.endWeather();
            }

            this.worldBorderRenderer.render(this.levelRenderState.worldBorderRenderState, camerarenderstate.pos, i, f);
            multibuffersource$buffersource.endBatch();
        });
    }

    private void addLateDebugPass(FrameGraphBuilder p_369572_, CameraRenderState p_453048_, GpuBufferSlice p_408435_, Matrix4f p_452705_) {
        FramePass framepass = p_369572_.addPass("late_debug");
        this.targets.main = framepass.readsAndWrites(this.targets.main);
        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = framepass.readsAndWrites(this.targets.itemEntity);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.main;
        framepass.executes(() -> {
            RenderSystem.setShaderFog(p_408435_);
            PoseStack posestack = new PoseStack();
            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
            RenderSystem.outputColorTextureOverride = resourcehandle.get().getColorTextureView();
            RenderSystem.outputDepthTextureOverride = resourcehandle.get().getDepthTextureView();
            if (!this.finalizedGizmos.alwaysOnTopPrimitives().isEmpty()) {
                RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
                RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(rendertarget.getDepthTexture(), 1.0);
                this.finalizedGizmos.alwaysOnTopPrimitives().render(posestack, multibuffersource$buffersource, p_453048_, p_452705_);
                multibuffersource$buffersource.endLastBatch();
            }

            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            this.checkPoseStack(posestack);
        });
    }

    public void extractVisibleEntities(Camera p_427759_, Frustum p_430259_, DeltaTracker p_428460_, LevelRenderState p_424777_) {
        Vec3 vec3 = p_427759_.position();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
        boolean flag = this.shouldShowEntityOutlines();
        Entity.setViewScale(Mth.clamp(this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get());
        int i = this.level.getMinY();
        int j = this.level.getMaxY();
        boolean flag1 = Shaders.isShadowPass && !this.minecraft.player.isSpectator();

        for (Entity entity : this.level.entitiesForRendering()) {
            if (this.shouldRenderEntity(entity, i, j) && (this.entityRenderDispatcher.shouldRender(entity, p_430259_, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player))) {
                BlockPos blockpos = entity.blockPosition();
                if ((this.level.isOutsideBuildHeight(blockpos.getY()) || this.isSectionCompiledAndVisible(blockpos))
                    && (
                        entity != p_427759_.entity()
                            || flag1
                            || p_427759_.isDetached()
                            || p_427759_.entity() instanceof LivingEntity && ((LivingEntity)p_427759_.entity()).isSleeping()
                    )
                    && (!(entity instanceof LocalPlayer) || p_427759_.entity() == entity)) {
                    if (entity.tickCount == 0) {
                        entity.xOld = entity.getX();
                        entity.yOld = entity.getY();
                        entity.zOld = entity.getZ();
                    }

                    float f = p_428460_.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(entity));
                    EntityRenderState entityrenderstate = this.extractEntity(entity, f);
                    p_424777_.entityRenderStates.add(entityrenderstate);
                    if (entityrenderstate.appearsGlowing() && flag) {
                        p_424777_.haveGlowingEntities = true;
                    }
                }
            }
        }
    }

    public void submitEntities(PoseStack p_429655_, LevelRenderState p_424795_, SubmitNodeCollector p_423961_) {
        Vec3 vec3 = p_424795_.cameraRenderState.pos;
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();

        for (EntityRenderState entityrenderstate : p_424795_.entityRenderStates) {
            if (!p_424795_.haveGlowingEntities) {
                entityrenderstate.outlineColor = 0;
            }

            this.entityRenderDispatcher
                .submit(
                    entityrenderstate,
                    p_424795_.cameraRenderState,
                    entityrenderstate.x - d0,
                    entityrenderstate.y - d1,
                    entityrenderstate.z - d2,
                    p_429655_,
                    p_423961_
                );
        }
    }

    private void extractVisibleBlockEntities(Camera p_427254_, float p_426770_, LevelRenderState p_428920_) {
        this.extractVisibleBlockEntities(p_427254_, p_426770_, p_428920_, FrustumDummy.INSTANCE);
    }

    public void extractVisibleBlockEntities(Camera cameraIn, float partialTicks, LevelRenderState stateIn, Frustum frustumIn) {
        Vec3 vec3 = cameraIn.position();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        PoseStack posestack = new PoseStack();
        boolean flag = Reflector.IForgeBlockEntity_getRenderBoundingBox.exists();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.renderInfosTileEntities) {
            List<BlockEntity> list = sectionrenderdispatcher$rendersection.getSectionMesh().getRenderableBlockEntities();
            if (!list.isEmpty() && !(sectionrenderdispatcher$rendersection.getVisibility(Util.getMillis()) < 0.3F)) {
                for (BlockEntity blockentity : list) {
                    if (flag) {
                        AABB aabb = (AABB)Reflector.call(blockentity, Reflector.IForgeBlockEntity_getRenderBoundingBox);
                        if (aabb != null && !frustumIn.isVisible(aabb)) {
                            continue;
                        }
                    }

                    BlockPos blockpos = blockentity.getBlockPos();
                    SortedSet<BlockDestructionProgress> sortedset = this.destructionProgress.get(blockpos.asLong());
                    ModelFeatureRenderer.CrumblingOverlay modelfeaturerenderer$crumblingoverlay;
                    if (sortedset != null && !sortedset.isEmpty()) {
                        posestack.pushPose();
                        posestack.translate(blockpos.getX() - d0, blockpos.getY() - d1, blockpos.getZ() - d2);
                        modelfeaturerenderer$crumblingoverlay = new ModelFeatureRenderer.CrumblingOverlay(sortedset.last().getProgress(), posestack.last());
                        posestack.popPose();
                    } else {
                        modelfeaturerenderer$crumblingoverlay = null;
                    }

                    BlockEntityRenderState blockentityrenderstate = this.blockEntityRenderDispatcher.tryExtractRenderState(blockentity, partialTicks, modelfeaturerenderer$crumblingoverlay);
                    if (blockentityrenderstate != null) {
                        stateIn.blockEntityRenderStates.add(blockentityrenderstate);
                    }
                }
            }
        }

        Iterator<BlockEntity> iterator = this.level.getGloballyRenderedBlockEntities().iterator();

        while (iterator.hasNext()) {
            BlockEntity blockentity1 = iterator.next();
            if (blockentity1.isRemoved()) {
                iterator.remove();
            } else {
                if (flag) {
                    AABB aabb1 = (AABB)Reflector.call(blockentity1, Reflector.IForgeBlockEntity_getRenderBoundingBox);
                    if (aabb1 != null && !frustumIn.isVisible(aabb1)) {
                        continue;
                    }
                }

                BlockEntityRenderState blockentityrenderstate1 = this.blockEntityRenderDispatcher.tryExtractRenderState(blockentity1, partialTicks, null);
                if (blockentityrenderstate1 != null) {
                    stateIn.blockEntityRenderStates.add(blockentityrenderstate1);
                }
            }
        }
    }

    public void submitBlockEntities(PoseStack p_426748_, LevelRenderState p_428747_, SubmitNodeStorage p_423080_) {
        Vec3 vec3 = p_428747_.cameraRenderState.pos;
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();

        for (BlockEntityRenderState blockentityrenderstate : p_428747_.blockEntityRenderStates) {
            BlockPos blockpos = blockentityrenderstate.blockPos;
            p_426748_.pushPose();
            p_426748_.translate(blockpos.getX() - d0, blockpos.getY() - d1, blockpos.getZ() - d2);
            this.blockEntityRenderDispatcher.submit(blockentityrenderstate, p_426748_, p_423080_, p_428747_.cameraRenderState);
            p_426748_.popPose();
        }
    }

    private void extractBlockDestroyAnimation(Camera p_424426_, LevelRenderState p_426580_) {
        Vec3 vec3 = p_424426_.position();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        p_426580_.blockBreakingRenderStates.clear();

        for (Entry<SortedSet<BlockDestructionProgress>> entry : this.destructionProgress.long2ObjectEntrySet()) {
            BlockPos blockpos = BlockPos.of(entry.getLongKey());
            if (!(blockpos.distToCenterSqr(d0, d1, d2) > 1024.0)) {
                SortedSet<BlockDestructionProgress> sortedset = entry.getValue();
                if (sortedset != null && !sortedset.isEmpty()) {
                    int i = sortedset.last().getProgress();
                    p_426580_.blockBreakingRenderStates.add(new BlockBreakingRenderState(this.level, blockpos, i));
                }
            }
        }
    }

    private void renderBlockDestroyAnimation(PoseStack p_366956_, MultiBufferSource.BufferSource p_365998_, LevelRenderState p_428141_) {
        Vec3 vec3 = p_428141_.cameraRenderState.pos;
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();

        for (BlockBreakingRenderState blockbreakingrenderstate : p_428141_.blockBreakingRenderStates) {
            p_366956_.pushPose();
            BlockPos blockpos = blockbreakingrenderstate.blockPos;
            p_366956_.translate(blockpos.getX() - d0, blockpos.getY() - d1, blockpos.getZ() - d2);
            PoseStack.Pose posestack$pose = p_366956_.last();
            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(
                p_365998_.getBuffer(ModelBakery.DESTROY_TYPES.get(blockbreakingrenderstate.progress)), posestack$pose, 1.0F
            );
            ModelData modeldata = this.level.getModelDataManager().getAtOrEmpty(blockpos);
            this.minecraft
                .getBlockRenderer()
                .renderBreakingTexture(blockbreakingrenderstate.blockState, blockpos, blockbreakingrenderstate, p_366956_, vertexconsumer, modeldata);
            p_366956_.popPose();
        }
    }

    private void extractBlockOutline(Camera p_428965_, LevelRenderState p_424419_) {
        this.extractBlockOutline(p_428965_, p_424419_, true);
    }

    private void extractBlockOutline(Camera cameraIn, LevelRenderState stateIn, boolean shouldRender) {
        stateIn.blockOutlineRenderState = null;
        if (Reflector.ForgeHooksClient_onExtractBlockOutline.exists()) {
            RenderHighlightEvent.Callback renderhighlightevent$callback = (RenderHighlightEvent.Callback)Reflector.ForgeHooksClient_onExtractBlockOutline
                .call(this, cameraIn, stateIn, this.minecraft.hitResult);
            if (renderhighlightevent$callback != null) {
                stateIn.blockOutlineRenderState = new BlockOutlineRenderState(
                    BlockPos.ZERO, false, false, Shapes.empty(), null, null, null, renderhighlightevent$callback
                );
                return;
            }

            if (!shouldRender) {
                return;
            }
        }

        if (this.minecraft.hitResult instanceof BlockHitResult blockhitresult && blockhitresult.getType() != HitResult.Type.MISS) {
            BlockPos blockpos = blockhitresult.getBlockPos();
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (!blockstate.isAir() && this.level.getWorldBorder().isWithinBounds(blockpos)) {
                boolean flag = ItemBlockRenderTypes.getChunkRenderType(blockstate).sortOnUpload();
                boolean flag1 = this.minecraft.options.highContrastBlockOutline().get();
                CollisionContext collisioncontext = CollisionContext.of(cameraIn.entity());
                VoxelShape voxelshape = blockstate.getShape(this.level, blockpos, collisioncontext);
                if (SharedConstants.DEBUG_SHAPES) {
                    VoxelShape voxelshape1 = blockstate.getCollisionShape(this.level, blockpos, collisioncontext);
                    VoxelShape voxelshape2 = blockstate.getOcclusionShape();
                    VoxelShape voxelshape3 = blockstate.getInteractionShape(this.level, blockpos);
                    stateIn.blockOutlineRenderState = new BlockOutlineRenderState(blockpos, flag, flag1, voxelshape, voxelshape1, voxelshape2, voxelshape3);
                } else {
                    stateIn.blockOutlineRenderState = new BlockOutlineRenderState(blockpos, flag, flag1, voxelshape, blockstate);
                }
            }
        }
    }

    private void renderBlockOutline(MultiBufferSource.BufferSource p_367206_, PoseStack p_365062_, boolean p_368189_, LevelRenderState p_422597_) {
        BlockOutlineRenderState blockoutlinerenderstate = p_422597_.blockOutlineRenderState;
        if (blockoutlinerenderstate != null) {
            if (blockoutlinerenderstate.customRenderer() != null) {
                blockoutlinerenderstate.customRenderer().render(p_367206_, p_365062_, p_368189_, p_422597_);
                return;
            }

            if (blockoutlinerenderstate.isTranslucent() == p_368189_) {
                if (Config.isShaders()) {
                    ShadersRender.beginOutline();
                }

                Vec3 vec3 = p_422597_.cameraRenderState.pos;
                if (blockoutlinerenderstate.highContrast()) {
                    VertexConsumer vertexconsumer = p_367206_.getBuffer(RenderTypes.secondaryBlockOutline());
                    this.renderHitOutline(p_365062_, vertexconsumer, vec3.x, vec3.y, vec3.z, blockoutlinerenderstate, -16777216, 7.0F);
                }

                VertexConsumer vertexconsumer1 = p_367206_.getBuffer(RenderTypes.lines());
                int i = blockoutlinerenderstate.highContrast() ? -11010079 : ARGB.black(102);
                this.renderHitOutline(
                    p_365062_, vertexconsumer1, vec3.x, vec3.y, vec3.z, blockoutlinerenderstate, i, this.minecraft.getWindow().getAppropriateLineWidth()
                );
                p_367206_.endLastBatch();
                if (Config.isShaders()) {
                    p_367206_.endBatch(RenderTypes.lines());
                    ShadersRender.endOutline();
                }
            }
        }
    }

    public void checkPoseStack(PoseStack p_109589_) {
        if (!p_109589_.isEmpty()) {
            throw new IllegalStateException("Pose stack not empty");
        }
    }

    public EntityRenderState extractEntity(Entity p_427314_, float p_423106_) {
        return this.entityRenderDispatcher.extractEntity(p_427314_, p_423106_);
    }

    private void scheduleTranslucentSectionResort(Vec3 p_362155_) {
        if (!this.visibleSections.isEmpty()) {
            BlockPos blockpos = BlockPos.containing(p_362155_);
            boolean flag = !blockpos.equals(this.lastTranslucentSortBlockPos);
            TranslucencyPointOfView translucencypointofview = new TranslucencyPointOfView();

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.nearbyVisibleSections) {
                this.scheduleResort(sectionrenderdispatcher$rendersection, translucencypointofview, p_362155_, flag, true);
            }

            this.translucencyResortIterationIndex = this.translucencyResortIterationIndex % this.visibleSections.size();
            int i = Math.max(this.visibleSections.size() / 8, 15);

            while (i-- > 0) {
                int j = this.translucencyResortIterationIndex++ % this.visibleSections.size();
                this.scheduleResort(this.visibleSections.get(j), translucencypointofview, p_362155_, flag, false);
            }

            this.lastTranslucentSortBlockPos = blockpos;
        }
    }

    private void scheduleResort(
        SectionRenderDispatcher.RenderSection p_363545_, TranslucencyPointOfView p_409581_, Vec3 p_364217_, boolean p_363419_, boolean p_368916_
    ) {
        p_409581_.set(p_364217_, p_363545_.getSectionNode());
        boolean flag = p_363545_.getSectionMesh().isDifferentPointOfView(p_409581_);
        boolean flag1 = p_363419_ && (p_409581_.isAxisAligned() || p_368916_);
        if ((flag1 || flag) && !p_363545_.transparencyResortingScheduled() && p_363545_.hasTranslucentGeometry()) {
            p_363545_.resortTransparency(this.sectionRenderDispatcher);
        }
    }

    public ChunkSectionsToRender prepareChunkRenders(Matrix4fc p_407733_, double p_409433_, double p_409487_, double p_408168_) {
        ObjectListIterator<SectionRenderDispatcher.RenderSection> objectlistiterator = this.renderInfosTerrain.listIterator(0);
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> enummap = this.enumMapLayerDraws;
        int i = 0;

        for (ChunkSectionLayer chunksectionlayer : ChunkSectionLayer.values()) {
            List list = enummap.get(chunksectionlayer);
            if (list != null) {
                list.clear();
            } else {
                enummap.put(chunksectionlayer, new ArrayList<>());
            }
        }

        List<DynamicUniforms.ChunkSectionInfo> list1 = new ArrayList<>();
        GpuTextureView gputextureview = this.minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int i1 = gputextureview.getWidth(0);
        int j1 = gputextureview.getHeight(0);
        boolean flag1 = Config.isRenderRegions();
        if (flag1) {
            this.regionLayerRenderer.reset();
        }

        boolean flag = SmartAnimations.isActive();

        while (objectlistiterator.hasNext()) {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = objectlistiterator.next();
            SectionMesh sectionmesh = sectionrenderdispatcher$rendersection.getSectionMesh();
            BlockPos blockpos = sectionrenderdispatcher$rendersection.getRenderOrigin();
            long j = Util.getMillis();
            int k = -1;

            for (ChunkSectionLayer chunksectionlayer1 : ChunkSectionLayer.VALUES) {
                SectionBuffers sectionbuffers = sectionmesh.getBuffers(chunksectionlayer1);
                if (sectionbuffers != null) {
                    if (flag) {
                        BitSet bitset = sectionmesh.getAnimatedSprites(chunksectionlayer1);
                        if (bitset != null) {
                            SmartAnimations.spritesRendered(bitset);
                        }
                    }

                    if (flag1 && sectionbuffers.getVboRegion() != null) {
                        this.regionLayerRenderer
                            .addSection(
                                chunksectionlayer1,
                                sectionrenderdispatcher$rendersection.regionX,
                                sectionrenderdispatcher$rendersection.regionZ,
                                sectionbuffers
                            );
                    } else {
                        if (k == -1) {
                            k = list1.size();
                            list1.add(
                                new DynamicUniforms.ChunkSectionInfo(
                                    new Matrix4f(p_407733_),
                                    blockpos.getX(),
                                    blockpos.getY(),
                                    blockpos.getZ(),
                                    sectionrenderdispatcher$rendersection.getVisibility(j),
                                    i1,
                                    j1,
                                    p_409433_,
                                    p_409487_,
                                    p_408168_
                                )
                            );
                        }

                        VertexFormat.IndexType vertexformat$indextype;
                        GpuBuffer gpubuffer;
                        if (sectionbuffers.getIndexBuffer() == null) {
                            if (sectionbuffers.getIndexCount() > i) {
                                i = sectionbuffers.getIndexCount();
                            }

                            gpubuffer = null;
                            vertexformat$indextype = null;
                        } else {
                            gpubuffer = sectionbuffers.getIndexBuffer();
                            vertexformat$indextype = sectionbuffers.getIndexType();
                        }

                        int l = k;
                        enummap.get(chunksectionlayer1)
                            .add(
                                new RenderPass.Draw<>(
                                    0,
                                    sectionbuffers.getVertexBuffer(),
                                    gpubuffer,
                                    vertexformat$indextype,
                                    0,
                                    sectionbuffers.getIndexCount(),
                                    (buffers2In, uploader2In) -> uploader2In.upload("ChunkSection", buffers2In[l]),
                                    null,
                                    sectionbuffers.getMultiTextureData()
                                )
                            );
                    }
                }
            }
        }

        if (flag1) {
            this.regionLayerRenderer.finishPrepare(p_407733_, p_409433_, p_409487_, p_408168_, list1, enummap);
        }

        GpuBufferSlice[] agpubufferslice = RenderSystem.getDynamicUniforms().writeChunkSections(list1.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        return new ChunkSectionsToRender(gputextureview, enummap, i, agpubufferslice);
    }

    public void endFrame() {
        this.cloudRenderer.endFrame();
    }

    public void captureFrustum() {
        this.captureFrustum = true;
    }

    public void killFrustum() {
        this.capturedFrustum = null;
    }

    public void tick(Camera p_426600_) {
        if (this.level.tickRateManager().runsNormally()) {
            this.ticks++;
        }

        this.weatherEffectRenderer
            .tickRainParticles(
                this.level, p_426600_, this.ticks, this.minecraft.options.particles().get(), this.minecraft.options.weatherRadius().get()
            );
        this.removeBlockBreakingProgress();
    }

    private void removeBlockBreakingProgress() {
        if (this.ticks % 20 == 0) {
            Iterator<BlockDestructionProgress> iterator = this.destroyingBlocks.values().iterator();

            while (iterator.hasNext()) {
                BlockDestructionProgress blockdestructionprogress = iterator.next();
                int i = blockdestructionprogress.getUpdatedRenderTick();
                if (this.ticks - i > 400) {
                    iterator.remove();
                    this.removeProgress(blockdestructionprogress);
                }
            }
        }

        if (Config.isRenderRegions() && this.ticks % 60 == 0) {
            this.regionLayerRenderer.clear();
        }
    }

    private void removeProgress(BlockDestructionProgress p_109766_) {
        long i = p_109766_.getPos().asLong();
        Set<BlockDestructionProgress> set = this.destructionProgress.get(i);
        set.remove(p_109766_);
        if (set.isEmpty()) {
            this.destructionProgress.remove(i);
        }
    }

    private void addSkyPass(FrameGraphBuilder p_362462_, Camera p_369183_, GpuBufferSlice p_408470_) {
        FogType fogtype = p_369183_.getFluidInCamera();
        if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !this.doesMobEffectBlockSky(p_369183_)) {
            SkyRenderState skyrenderstate = this.levelRenderState.skyRenderState;
            if (skyrenderstate.skybox != DimensionType.Skybox.NONE) {
                SkyRenderer skyrenderer = this.skyRenderer;
                if (skyrenderer != null) {
                    FramePass framepass = p_362462_.addPass("sky");
                    this.targets.main = framepass.readsAndWrites(this.targets.main);
                    framepass.executes(
                        () -> {
                            boolean flag = Config.isShaders();
                            if (flag) {
                                Shaders.beginSky();
                            }

                            RenderSystem.setShaderFog(p_408470_);
                            if (skyrenderstate.skybox == DimensionType.Skybox.END) {
                                skyrenderer.renderEndSky();
                                if (skyrenderstate.endFlashIntensity > 1.0E-5F) {
                                    PoseStack posestack = new PoseStack();
                                    skyrenderer.renderEndFlash(posestack, skyrenderstate.endFlashIntensity, skyrenderstate.endFlashXAngle, skyrenderstate.endFlashYAngle);
                                }
                            } else {
                                PoseStack posestack1 = new PoseStack();
                                if (flag) {
                                    Shaders.disableTexture2D();
                                }

                                skyrenderstate.skyColor = CustomColors.getSkyColor(
                                    skyrenderstate.skyColor,
                                    this.minecraft.level,
                                    this.minecraft.getCameraEntity().getX(),
                                    this.minecraft.getCameraEntity().getY() + 1.0,
                                    this.minecraft.getCameraEntity().getZ()
                                );
                                if (flag) {
                                    Shaders.setSkyColor(skyrenderstate.skyColor);
                                    RenderSystem.setColorToAttribute(true);
                                    Shaders.enableFog();
                                    Shaders.preSkyList(posestack1);
                                }

                                if (Config.isSkyEnabled()) {
                                    skyrenderer.renderSkyDisc(skyrenderstate.skyColor);
                                }

                                if (flag) {
                                    Shaders.disableFog();
                                }

                                if (Config.isSunMoonEnabled()) {
                                    if (flag) {
                                        Shaders.disableTexture2D();
                                    }

                                    if (flag) {
                                        Shaders.setRenderStage(RenderStage.SUNSET);
                                    }

                                    skyrenderer.renderSunriseAndSunset(posestack1, skyrenderstate.sunAngle, skyrenderstate.sunriseAndSunsetColor);
                                    if (flag) {
                                        Shaders.enableTexture2D();
                                    }

                                    skyrenderer.renderSunMoonAndStars(
                                        posestack1,
                                        skyrenderstate.sunAngle,
                                        skyrenderstate.moonAngle,
                                        skyrenderstate.starAngle,
                                        skyrenderstate.moonPhase,
                                        skyrenderstate.rainBrightness,
                                        skyrenderstate.starBrightness
                                    );
                                    if (flag) {
                                        Shaders.disableTexture2D();
                                    }
                                }

                                if (skyrenderstate.shouldRenderDarkDisc) {
                                    if (flag) {
                                        Shaders.setRenderStage(RenderStage.VOID);
                                    }

                                    skyrenderer.renderDarkDisc();
                                }

                                if (flag) {
                                    RenderSystem.setColorToAttribute(false);
                                }
                            }

                            if (flag) {
                                Shaders.endSky();
                            }
                        }
                    );
                }
            }
        }
    }

    private boolean doesMobEffectBlockSky(Camera p_234311_) {
        return p_234311_.entity() instanceof LivingEntity livingentity
            ? livingentity.hasEffect(MobEffects.BLINDNESS) || livingentity.hasEffect(MobEffects.DARKNESS)
            : false;
    }

    private void compileSections(Camera p_194371_) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("populateSectionsToCompile");
        RenderRegionCache renderregioncache = new RenderRegionCache();
        renderregioncache.compileStarted();
        BlockPos blockpos = p_194371_.blockPosition();
        List<SectionRenderDispatcher.RenderSection> list = Lists.newArrayList();
        long i = Mth.floor(this.minecraft.options.chunkSectionFadeInTime().get() * 1000.0);
        Lagometer.timerChunkUpdate.start();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.visibleSections) {
            if (sectionrenderdispatcher$rendersection.isDirty()
                && (sectionrenderdispatcher$rendersection.getSectionMesh() != CompiledSectionMesh.UNCOMPILED || sectionrenderdispatcher$rendersection.hasAllNeighbors())) {
                if (sectionrenderdispatcher$rendersection.needsBackgroundPriorityUpdate()) {
                    list.add(sectionrenderdispatcher$rendersection);
                } else {
                    BlockPos blockpos1 = SectionPos.of(sectionrenderdispatcher$rendersection.getSectionNode()).center();
                    double d0 = blockpos1.distSqr(blockpos);
                    boolean flag = d0 < 768.0;
                    boolean flag1 = false;
                    if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NEARBY) {
                        flag1 = flag || sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                    } else if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                        flag1 = sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                    }

                    if (!flag && !sectionrenderdispatcher$rendersection.wasPreviouslyEmpty()) {
                        sectionrenderdispatcher$rendersection.setFadeDuration(i);
                    } else {
                        sectionrenderdispatcher$rendersection.setFadeDuration(0L);
                    }

                    sectionrenderdispatcher$rendersection.setWasPreviouslyEmpty(false);
                    if (flag1) {
                        profilerfiller.push("compileSectionSynchronously");
                        this.sectionRenderDispatcher.rebuildSectionSync(sectionrenderdispatcher$rendersection, renderregioncache);
                        sectionrenderdispatcher$rendersection.setNotDirty();
                        profilerfiller.pop();
                    } else {
                        list.add(sectionrenderdispatcher$rendersection);
                    }
                }
            }
        }

        Lagometer.timerChunkUpdate.end();
        Lagometer.timerChunkUpload.start();
        profilerfiller.popPush("uploadSectionMeshes");
        this.sectionRenderDispatcher.uploadAllPendingUploads();
        profilerfiller.popPush("scheduleAsyncCompile");
        if (this.chunksToResortTransparency.size() > 0) {
            Iterator<SectionRenderDispatcher.RenderSection> iterator = this.chunksToResortTransparency.iterator();
            if (iterator.hasNext()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = iterator.next();
                if (this.sectionRenderDispatcher.updateTransparencyLater(sectionrenderdispatcher$rendersection1)) {
                    iterator.remove();
                }
            }
        }

        double d2 = 0.0;
        int j = Config.getUpdatesPerFrame();
        this.countChunksToUpdate = list.size();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 : SectionUtils.getSortedDist(list, this.minecraft.player)) {
            boolean flag3 = sectionrenderdispatcher$rendersection2.isChunkRegionEmpty();
            boolean flag2 = sectionrenderdispatcher$rendersection2.needsBackgroundPriorityUpdate();
            if (sectionrenderdispatcher$rendersection2.isDirty()) {
                sectionrenderdispatcher$rendersection2.rebuildSectionAsync(renderregioncache);
                sectionrenderdispatcher$rendersection2.setNotDirty();
                if (!flag3 && !flag2) {
                    double d1 = 2.0 * RenderChunkUtils.getRelativeBufferSize(sectionrenderdispatcher$rendersection2);
                    d2 += d1;
                    if (d2 > j) {
                        break;
                    }
                }
            }
        }

        renderregioncache.compileFinished();
        Lagometer.timerChunkUpload.end();
        profilerfiller.popPush("scheduleTranslucentResort");
        this.scheduleTranslucentSectionResort(p_194371_.position());
        profilerfiller.pop();
    }

    private void renderHitOutline(
        PoseStack p_109638_,
        VertexConsumer p_109639_,
        double p_109641_,
        double p_109642_,
        double p_109643_,
        BlockOutlineRenderState p_422577_,
        int p_362600_,
        float p_459634_
    ) {
        if (!Config.isCustomEntityModels() || !CustomEntityModels.isCustomModel(p_422577_.blockState())) {
            BlockPos blockpos = p_422577_.pos();
            if (SharedConstants.DEBUG_SHAPES) {
                ShapeRenderer.renderShape(
                    p_109638_,
                    p_109639_,
                    p_422577_.shape(),
                    blockpos.getX() - p_109641_,
                    blockpos.getY() - p_109642_,
                    blockpos.getZ() - p_109643_,
                    ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F),
                    p_459634_
                );
                if (p_422577_.collisionShape() != null) {
                    ShapeRenderer.renderShape(
                        p_109638_,
                        p_109639_,
                        p_422577_.collisionShape(),
                        blockpos.getX() - p_109641_,
                        blockpos.getY() - p_109642_,
                        blockpos.getZ() - p_109643_,
                        ARGB.colorFromFloat(0.4F, 0.0F, 0.0F, 0.0F),
                        p_459634_
                    );
                }

                if (p_422577_.occlusionShape() != null) {
                    ShapeRenderer.renderShape(
                        p_109638_,
                        p_109639_,
                        p_422577_.occlusionShape(),
                        blockpos.getX() - p_109641_,
                        blockpos.getY() - p_109642_,
                        blockpos.getZ() - p_109643_,
                        ARGB.colorFromFloat(0.4F, 0.0F, 1.0F, 0.0F),
                        p_459634_
                    );
                }

                if (p_422577_.interactionShape() != null) {
                    ShapeRenderer.renderShape(
                        p_109638_,
                        p_109639_,
                        p_422577_.interactionShape(),
                        blockpos.getX() - p_109641_,
                        blockpos.getY() - p_109642_,
                        blockpos.getZ() - p_109643_,
                        ARGB.colorFromFloat(0.4F, 0.0F, 0.0F, 1.0F),
                        p_459634_
                    );
                }
            } else {
                ShapeRenderer.renderShape(
                    p_109638_,
                    p_109639_,
                    p_422577_.shape(),
                    blockpos.getX() - p_109641_,
                    blockpos.getY() - p_109642_,
                    blockpos.getZ() - p_109643_,
                    p_362600_,
                    p_459634_
                );
            }
        }
    }

    public void blockChanged(BlockGetter p_109545_, BlockPos p_109546_, BlockState p_109547_, BlockState p_109548_, @Block.UpdateFlags int p_109549_) {
        this.setBlockDirty(p_109546_, (p_109549_ & 8) != 0);
    }

    private void setBlockDirty(BlockPos p_109733_, boolean p_109734_) {
        for (int i = p_109733_.getZ() - 1; i <= p_109733_.getZ() + 1; i++) {
            for (int j = p_109733_.getX() - 1; j <= p_109733_.getX() + 1; j++) {
                for (int k = p_109733_.getY() - 1; k <= p_109733_.getY() + 1; k++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i), p_109734_);
                }
            }
        }
    }

    public void setBlocksDirty(int p_109495_, int p_109496_, int p_109497_, int p_109498_, int p_109499_, int p_109500_) {
        for (int i = p_109497_ - 1; i <= p_109500_ + 1; i++) {
            for (int j = p_109495_ - 1; j <= p_109498_ + 1; j++) {
                for (int k = p_109496_ - 1; k <= p_109499_ + 1; k++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i));
                }
            }
        }
    }

    public void setBlockDirty(BlockPos p_109722_, BlockState p_109723_, BlockState p_109724_) {
        if (this.minecraft.getModelManager().requiresRender(p_109723_, p_109724_)) {
            this.setBlocksDirty(
                p_109722_.getX(), p_109722_.getY(), p_109722_.getZ(), p_109722_.getX(), p_109722_.getY(), p_109722_.getZ()
            );
        }
    }

    public void setSectionDirtyWithNeighbors(int p_109491_, int p_109492_, int p_109493_) {
        this.setSectionRangeDirty(p_109491_ - 1, p_109492_ - 1, p_109493_ - 1, p_109491_ + 1, p_109492_ + 1, p_109493_ + 1);
    }

    public void setSectionRangeDirty(int p_368495_, int p_365381_, int p_365979_, int p_367380_, int p_368841_, int p_363880_) {
        for (int i = p_365979_; i <= p_363880_; i++) {
            for (int j = p_368495_; j <= p_367380_; j++) {
                for (int k = p_365381_; k <= p_368841_; k++) {
                    this.setSectionDirty(j, k, i);
                }
            }
        }
    }

    public void setSectionDirty(int p_109771_, int p_109772_, int p_109773_) {
        this.setSectionDirty(p_109771_, p_109772_, p_109773_, false);
    }

    private void setSectionDirty(int p_109502_, int p_109503_, int p_109504_, boolean p_109505_) {
        this.viewArea.setDirty(p_109502_, p_109503_, p_109504_, p_109505_);
    }

    public void onSectionBecomingNonEmpty(long p_366966_) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSection(p_366966_);
        if (sectionrenderdispatcher$rendersection != null) {
            this.sectionOcclusionGraph.schedulePropagationFrom(sectionrenderdispatcher$rendersection);
            sectionrenderdispatcher$rendersection.setWasPreviouslyEmpty(true);
        }
    }

    public void destroyBlockProgress(int p_109775_, BlockPos p_109776_, int p_109777_) {
        if (p_109777_ >= 0 && p_109777_ < 10) {
            BlockDestructionProgress blockdestructionprogress1 = this.destroyingBlocks.get(p_109775_);
            if (blockdestructionprogress1 != null) {
                this.removeProgress(blockdestructionprogress1);
            }

            if (blockdestructionprogress1 == null
                || blockdestructionprogress1.getPos().getX() != p_109776_.getX()
                || blockdestructionprogress1.getPos().getY() != p_109776_.getY()
                || blockdestructionprogress1.getPos().getZ() != p_109776_.getZ()) {
                blockdestructionprogress1 = new BlockDestructionProgress(p_109775_, p_109776_);
                this.destroyingBlocks.put(p_109775_, blockdestructionprogress1);
            }

            blockdestructionprogress1.setProgress(p_109777_);
            blockdestructionprogress1.updateTick(this.ticks);
            this.destructionProgress.computeIfAbsent(blockdestructionprogress1.getPos().asLong(), keyIn -> Sets.newTreeSet()).add(blockdestructionprogress1);
        } else {
            BlockDestructionProgress blockdestructionprogress = this.destroyingBlocks.remove(p_109775_);
            if (blockdestructionprogress != null) {
                this.removeProgress(blockdestructionprogress);
            }
        }
    }

    public boolean hasRenderedAllSections() {
        return this.sectionRenderDispatcher.isQueueEmpty();
    }

    public void onChunkReadyToRender(ChunkPos p_376082_) {
        this.sectionOcclusionGraph.onChunkReadyToRender(p_376082_);
    }

    public void needsUpdate() {
        this.sectionOcclusionGraph.invalidate();
        this.cloudRenderer.markForRebuild();
    }

    public int getCountRenderers() {
        return this.viewArea.sections.length;
    }

    public int getCountEntitiesRendered() {
        return this.levelRenderState.entityRenderStates.size();
    }

    public int getCountTileEntitiesRendered() {
        return this.levelRenderState.blockEntityRenderStates.size();
    }

    public int getCountLoadedChunks() {
        if (this.level == null) {
            return 0;
        }

        ClientChunkCache clientchunkcache = this.level.getChunkSource();
        return clientchunkcache == null ? 0 : clientchunkcache.getLoadedChunksCount();
    }

    public int getCountChunksToUpdate() {
        return this.countChunksToUpdate;
    }

    public SectionRenderDispatcher.RenderSection getRenderChunk(BlockPos pos) {
        return this.viewArea.getRenderSectionAt(pos);
    }

    public SectionRenderDispatcher.RenderSection getRenderChunk(long pos) {
        return this.viewArea.getRenderSection(pos);
    }

    public ClientLevel getWorld() {
        return this.level;
    }

    private void clearRenderInfos() {
        this.clearRenderInfosTerrain();
        this.clearRenderInfosEntities();
    }

    private void clearRenderInfosTerrain() {
        if (renderEntitiesCounter > 0) {
            this.renderInfosTerrain = new ObjectArrayList<>(this.renderInfosTerrain.size() + 16);
            this.renderInfosTileEntities = new ArrayList<>(this.renderInfosTileEntities.size() + 16);
        } else {
            this.renderInfosTerrain.clear();
            this.renderInfosTileEntities.clear();
        }
    }

    private void clearRenderInfosEntities() {
        if (renderEntitiesCounter > 0) {
            this.renderInfosEntities = new LongOpenHashSet(this.renderInfosEntities.size() + 16);
        } else {
            this.renderInfosEntities.clear();
        }
    }

    public void onPlayerPositionSet() {
        if (this.firstWorldLoad) {
            this.allChanged();
            this.firstWorldLoad = false;
        }
    }

    public void pauseChunkUpdates() {
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.pauseChunkUpdates();
        }
    }

    public void resumeChunkUpdates() {
        if (this.sectionRenderDispatcher != null) {
            this.sectionRenderDispatcher.resumeChunkUpdates();
        }
    }

    public int getFrameCount() {
        return this.frameId;
    }

    public RenderBuffers getRenderTypeTextures() {
        return this.renderBuffers;
    }

    public LongOpenHashSet getRenderChunksEntities() {
        return this.renderInfosEntities;
    }

    private void addEntitySection(LongOpenHashSet set, EntitySectionStorage storage, BlockPos pos) {
        long i = SectionPos.asLong(pos);
        EntitySection entitysection = storage.getSection(i);
        if (entitysection != null) {
            set.add(i);
        }
    }

    private boolean hasEntitySection(EntitySectionStorage storage, BlockPos pos) {
        long i = SectionPos.asLong(pos);
        EntitySection entitysection = storage.getSection(i);
        return entitysection != null;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfos() {
        return this.visibleSections;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfosTerrain() {
        return this.renderInfosTerrain;
    }

    public List<SectionRenderDispatcher.RenderSection> getRenderInfosTileEntities() {
        return this.renderInfosTileEntities;
    }

    private void checkLoadVisibleChunks(Camera activeRenderInfo, Frustum icamera, boolean spectator) {
        if (this.loadVisibleChunksCounter == 0) {
            this.loadAllVisibleChunks(activeRenderInfo, icamera, spectator);
            this.minecraft.gui.getChat().deleteMessage(loadVisibleChunksMessageId);
        }

        if (this.loadVisibleChunksCounter >= 0) {
            this.loadVisibleChunksCounter--;
        }
    }

    private void loadAllVisibleChunks(Camera activeRenderInfo, Frustum icamera, boolean spectator) {
        int i = this.minecraft.options.ofChunkUpdates;
        boolean flag = this.minecraft.options.ofLazyChunkLoading;

        try {
            this.minecraft.options.ofChunkUpdates = 1000;
            this.minecraft.options.ofLazyChunkLoading = false;
            LevelRenderer levelrenderer = Config.getRenderGlobal();
            int j = levelrenderer.getCountLoadedChunks();
            long k = System.currentTimeMillis();
            Config.dbg("Loading visible chunks");
            long l = System.currentTimeMillis() + 5000L;
            int i1 = 0;
            boolean flag1 = false;

            do {
                flag1 = false;

                for (int j1 = 0; j1 < 100; j1++) {
                    levelrenderer.needsUpdate();
                    levelrenderer.cullTerrain(activeRenderInfo, icamera, spectator);
                    Config.sleep(1L);
                    this.compileSections(activeRenderInfo);
                    if (levelrenderer.getCountChunksToUpdate() > 0) {
                        flag1 = true;
                    }

                    if (!levelrenderer.hasRenderedAllSections()) {
                        flag1 = true;
                    }

                    i1 += levelrenderer.getCountChunksToUpdate();

                    while (!levelrenderer.hasRenderedAllSections()) {
                        int k1 = levelrenderer.getCountChunksToUpdate();
                        this.compileSections(activeRenderInfo);
                        if (k1 == levelrenderer.getCountChunksToUpdate()) {
                            break;
                        }
                    }

                    i1 -= levelrenderer.getCountChunksToUpdate();
                    if (!flag1) {
                        break;
                    }
                }

                if (levelrenderer.getCountLoadedChunks() != j) {
                    flag1 = true;
                    j = levelrenderer.getCountLoadedChunks();
                }

                if (System.currentTimeMillis() > l) {
                    Config.log("Chunks loaded: " + i1);
                    l = System.currentTimeMillis() + 5000L;
                }
            } while (flag1);

            Config.log("Chunks loaded: " + i1);
            Config.log("Finished loading visible chunks");
            SectionRenderDispatcher.renderChunksUpdated = 0;
        } finally {
            this.minecraft.options.ofChunkUpdates = i;
            this.minecraft.options.ofLazyChunkLoading = flag;
        }
    }

    public void applyFrustumEntities(Frustum camera, int maxChunkDistance) {
        this.renderInfosEntities.clear();
        int i = (int)camera.getCameraX() >> 4 << 4;
        int j = (int)camera.getCameraY() >> 4 << 4;
        int k = (int)camera.getCameraZ() >> 4 << 4;
        int l = maxChunkDistance * maxChunkDistance;
        EntitySectionStorage<?> entitysectionstorage = this.level.getSectionStorage();
        BlockPosM blockposm = new BlockPosM();
        LongSet longset = entitysectionstorage.getSectionKeys();
        LongIterator longiterator = longset.iterator();

        while (longiterator.hasNext()) {
            long i1 = longiterator.nextLong();
            blockposm.setXyz(
                SectionPos.sectionToBlockCoord(SectionPos.x(i1)), SectionPos.sectionToBlockCoord(SectionPos.y(i1)), SectionPos.sectionToBlockCoord(SectionPos.z(i1))
            );
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(blockposm);
            if (sectionrenderdispatcher$rendersection != null && camera.isVisible(sectionrenderdispatcher$rendersection.getBoundingBox())) {
                if (maxChunkDistance > 0) {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getPosition();
                    int j1 = i - blockpos.getX();
                    int k1 = j - blockpos.getY();
                    int l1 = k - blockpos.getZ();
                    int i2 = j1 * j1 + k1 * k1 + l1 * l1;
                    if (i2 > l) {
                        continue;
                    }
                }

                this.renderInfosEntities.add(i1);
            }
        }
    }

    public void setShadowRenderInfos(boolean shadowInfos) {
        if (shadowInfos) {
            this.renderInfosTerrain = this.renderInfosTerrainShadow;
            this.renderInfosEntities = this.renderInfosEntitiesShadow;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesShadow;
        } else {
            this.renderInfosTerrain = this.renderInfosTerrainNormal;
            this.renderInfosEntities = this.renderInfosEntitiesNormal;
            this.renderInfosTileEntities = this.renderInfosTileEntitiesNormal;
        }
    }

    public int getRenderedChunksShadow() {
        return !Config.isShadersShadows() ? -1 : this.renderInfosTerrainShadow.size();
    }

    public int getCountEntitiesRenderedShadow() {
        return !Config.isShadersShadows() ? -1 : ShadersRender.countEntitiesRenderedShadow;
    }

    public int getCountTileEntitiesRenderedShadow() {
        if (!Config.isShaders()) {
            return -1;
        } else {
            return !Shaders.hasShadowMap ? -1 : ShadersRender.countTileEntitiesRenderedShadow;
        }
    }

    public void captureFrustumShadow() {
        this.debugFixTerrainFrustumShadow = true;
    }

    public boolean isDebugFrustum() {
        return this.capturedFrustum != null;
    }

    public void onChunkRenderNeedsUpdate(SectionRenderDispatcher.RenderSection renderChunk) {
        if (!renderChunk.getSectionMesh().hasTerrainBlockEntities()) {
            ;
        }
    }

    public boolean needsFrustumUpdate() {
        return this.sectionOcclusionGraph.needsFrustumUpdate();
    }

    public boolean shouldRenderEntity(Entity entity, int minWorldY, int maxWorldY) {
        if (entity instanceof Display) {
            return true;
        } else if (entity == this.minecraft.getCameraEntity()) {
            return true;
        } else if (entity instanceof LivingEntity livingentity && livingentity.getScale() > 1.0F) {
            return true;
        } else {
            BlockPos blockpos = entity.blockPosition();
            return this.renderInfosEntities.contains(SectionPos.asLong(blockpos)) || blockpos.getY() <= minWorldY || blockpos.getY() >= maxWorldY;
        }
    }

    public LevelRenderState getLevelRenderState() {
        return this.levelRenderState;
    }

    public FeatureRenderDispatcher getFeatureRenderDispatcher() {
        return this.featureRenderDispatcher;
    }

    public SubmitNodeStorage getSubmitNodeStorage() {
        return this.submitNodeStorage;
    }

    public GpuSampler getChunkLayerSampler() {
        return this.chunkLayerSampler;
    }

    public int getTicks() {
        return this.ticks;
    }

    public WeatherEffectRenderer getWeatherEffects() {
        return this.weatherEffectRenderer;
    }

    public void setWeatherEffects(WeatherEffectRenderer value) {
        this.weatherEffectRenderer = value;
    }

    public static int getLightColor(BlockAndTintGetter p_109542_, BlockPos p_109543_) {
        return getLightColor(LevelRenderer.BrightnessGetter.DEFAULT, p_109542_, p_109542_.getBlockState(p_109543_), p_109543_);
    }

    public static int getLightColor(LevelRenderer.BrightnessGetter p_398213_, BlockAndTintGetter p_109538_, BlockState p_109539_, BlockPos p_109540_) {
        return getPackedLightmapCoords(p_398213_, p_109538_, p_109539_, p_109540_, ambientOcclusion);
    }

    public static int getPackedLightmapCoords(
        LevelRenderer.BrightnessGetter lightGetterIn,
        BlockAndTintGetter blockGetterIn,
        BlockState blockStateIn,
        BlockPos blockPosIn,
        boolean ambientOcclusionIn
    ) {
        if (EmissiveTextures.isRenderEmissive() && Config.isMinecraftThread()) {
            return LightTexture.MAX_BRIGHTNESS;
        }

        if (blockStateIn.emissiveRendering(blockGetterIn, blockPosIn)) {
            return 15794417;
        }

        int i = lightGetterIn.packedBrightness(blockGetterIn, blockPosIn);
        int j = LightTexture.block(i);
        int k = blockStateIn.getLightValue(blockGetterIn, blockPosIn);
        if (j < k) {
            int l = LightTexture.sky(i);
            i = LightTexture.pack(k, l);
        }

        if (Config.isDynamicLights() && (!ambientOcclusionIn || !blockStateIn.isSolidRender())) {
            i = DynamicLights.getCombinedLight(blockPosIn, i);
        }

        return i;
    }

    public boolean isSectionCompiledAndVisible(BlockPos p_452552_) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(p_452552_);
        return sectionrenderdispatcher$rendersection != null && sectionrenderdispatcher$rendersection.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED
            ? sectionrenderdispatcher$rendersection.getVisibility(Util.getMillis()) >= 0.3F
            : false;
    }

    public  RenderTarget entityOutlineTarget() {
        return this.targets.entityOutline != null ? this.targets.entityOutline.get() : null;
    }

    public  RenderTarget getTranslucentTarget() {
        return this.targets.translucent != null ? this.targets.translucent.get() : null;
    }

    public  RenderTarget getItemEntityTarget() {
        return this.targets.itemEntity != null ? this.targets.itemEntity.get() : null;
    }

    public  RenderTarget getParticlesTarget() {
        return this.targets.particles != null ? this.targets.particles.get() : null;
    }

    public  RenderTarget getWeatherTarget() {
        return this.targets.weather != null ? this.targets.weather.get() : null;
    }

    public  RenderTarget getCloudsTarget() {
        return this.targets.clouds != null ? this.targets.clouds.get() : null;
    }

    @VisibleForDebug
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> getVisibleSections() {
        return this.visibleSections;
    }

    @VisibleForDebug
    public SectionOcclusionGraph getSectionOcclusionGraph() {
        return this.sectionOcclusionGraph;
    }

    public  Frustum getCapturedFrustum() {
        return this.capturedFrustum;
    }

    public CloudRenderer getCloudRenderer() {
        return this.cloudRenderer;
    }

    public Gizmos.TemporaryCollection collectPerFrameGizmos() {
        return Gizmos.withCollector(this.collectedGizmos);
    }

    private void finalizeGizmoCollection() {
        DrawableGizmoPrimitives drawablegizmoprimitives = new DrawableGizmoPrimitives();
        DrawableGizmoPrimitives drawablegizmoprimitives1 = new DrawableGizmoPrimitives();
        this.collectedGizmos.addTemporaryGizmos(this.minecraft.getPerTickGizmos());
        IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
        if (integratedserver != null) {
            this.collectedGizmos.addTemporaryGizmos(integratedserver.getPerTickGizmos());
        }

        long i = Util.getMillis();

        for (SimpleGizmoCollector.GizmoInstance simplegizmocollector$gizmoinstance : this.collectedGizmos.drainGizmos()) {
            simplegizmocollector$gizmoinstance.gizmo()
                .emit(
                    simplegizmocollector$gizmoinstance.isAlwaysOnTop() ? drawablegizmoprimitives1 : drawablegizmoprimitives,
                    simplegizmocollector$gizmoinstance.getAlphaMultiplier(i)
                );
        }

        this.finalizedGizmos = new LevelRenderer.FinalizedGizmos(drawablegizmoprimitives, drawablegizmoprimitives1);
    }

    @FunctionalInterface
    public interface BrightnessGetter {
        LevelRenderer.BrightnessGetter DEFAULT = (blockGetterIn, blockPosIn) -> {
            int i = blockGetterIn.getBrightness(LightLayer.SKY, blockPosIn);
            int j = blockGetterIn.getBrightness(LightLayer.BLOCK, blockPosIn);
            return Brightness.pack(j, i);
        };

        int packedBrightness(BlockAndTintGetter p_398222_, BlockPos p_398220_);
    }

    record FinalizedGizmos(DrawableGizmoPrimitives standardPrimitives, DrawableGizmoPrimitives alwaysOnTopPrimitives) {
    }
}
