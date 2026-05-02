package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.CrashReport;
import net.minecraft.TracingExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.optifine.Config;
import net.optifine.override.ChunkCacheOF;
import net.optifine.render.AabbFrame;
import net.optifine.render.ICamera;
import net.optifine.render.VboRegion;
import net.optifine.util.ChunkUtils;
import org.jspecify.annotations.Nullable;

public class SectionRenderDispatcher {
    private final CompileTaskDynamicQueue compileQueue = new CompileTaskDynamicQueue();
    private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
    final Executor mainThreadUploadExecutor = this.toUpload::add;
    final Queue<SectionMesh> toClose = Queues.newConcurrentLinkedQueue();
    final SectionBufferBuilderPack fixedBuffers;
    private final SectionBufferBuilderPool bufferPool;
    volatile boolean closed;
    private final ConsecutiveExecutor consecutiveExecutor;
    private final TracingExecutor executor;
    ClientLevel level;
    final LevelRenderer renderer;
    Vec3 cameraPosition = Vec3.ZERO;
    final SectionCompiler sectionCompiler;
    private int countRenderBuilders;
    private List<SectionBufferBuilderPack> listPausedBuilders = new ArrayList<>();
    public static final ChunkSectionLayer[] BLOCK_RENDER_LAYERS = ChunkSectionLayer.VALUES;
    public static int renderChunksUpdated;

    public SectionRenderDispatcher(
        ClientLevel p_299878_,
        LevelRenderer p_299032_,
        TracingExecutor p_364436_,
        RenderBuffers p_310401_,
        BlockRenderDispatcher p_343142_,
        BlockEntityRenderDispatcher p_344654_
    ) {
        this.level = p_299878_;
        this.renderer = p_299032_;
        this.fixedBuffers = p_310401_.fixedBufferPack();
        this.bufferPool = p_310401_.sectionBufferPool();
        this.countRenderBuilders = this.bufferPool.getFreeBufferCount();
        this.executor = p_364436_;
        this.consecutiveExecutor = new ConsecutiveExecutor(p_364436_, "Section Renderer");
        this.consecutiveExecutor.schedule(this::runTask);
        this.sectionCompiler = new SectionCompiler(p_343142_, p_344654_);
        this.sectionCompiler.sectionRenderDispatcher = this;
    }

    public void setLevel(ClientLevel p_298968_) {
        this.level = p_298968_;
    }

    private void runTask() {
        if (!this.closed && !this.bufferPool.isEmpty()) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.compileQueue.poll(this.cameraPosition);
            if (sectionrenderdispatcher$rendersection$compiletask != null) {
                SectionBufferBuilderPack sectionbufferbuilderpack = Objects.requireNonNull(this.bufferPool.acquire());
                if (sectionbufferbuilderpack == null) {
                    this.compileQueue.add(sectionrenderdispatcher$rendersection$compiletask);
                    return;
                }

                CompletableFuture.<CompletableFuture<SectionRenderDispatcher.SectionTaskResult>>supplyAsync(
                        () -> sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack),
                        this.executor.forName(sectionrenderdispatcher$rendersection$compiletask.name())
                    )
                    .thenCompose(resultIn -> (CompletionStage<SectionRenderDispatcher.SectionTaskResult>)resultIn)
                    .whenComplete((taskResultIn, throwableIn) -> {
                        if (throwableIn != null) {
                            Minecraft.getInstance().delayCrash(CrashReport.forThrowable(throwableIn, "Batching sections"));
                        } else {
                            sectionrenderdispatcher$rendersection$compiletask.isCompleted.set(true);
                            this.consecutiveExecutor.schedule(() -> {
                                if (taskResultIn == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                                    sectionbufferbuilderpack.clearAll();
                                } else {
                                    sectionbufferbuilderpack.discardAll();
                                }

                                this.bufferPool.release(sectionbufferbuilderpack);
                                this.runTask();
                            });
                        }
                    });
            }
        }
    }

    public void setCameraPosition(Vec3 p_407405_) {
        this.cameraPosition = p_407405_;
    }

    public void uploadAllPendingUploads() {
        Runnable runnable;
        while ((runnable = this.toUpload.poll()) != null) {
            runnable.run();
        }

        SectionMesh sectionmesh;
        while ((sectionmesh = this.toClose.poll()) != null) {
            sectionmesh.close();
        }
    }

    public void rebuildSectionSync(SectionRenderDispatcher.RenderSection p_299640_, RenderRegionCache p_297835_) {
        p_299640_.compileSync(p_297835_);
    }

    public void schedule(SectionRenderDispatcher.RenderSection.CompileTask p_297747_) {
        if (!this.closed) {
            this.consecutiveExecutor.schedule(() -> {
                if (!this.closed) {
                    this.compileQueue.add(p_297747_);
                    this.runTask();
                }
            });
        }
    }

    public void clearCompileQueue() {
        this.compileQueue.clear();
    }

    public boolean isQueueEmpty() {
        return this.compileQueue.size() == 0 && this.toUpload.isEmpty();
    }

    public void dispose() {
        this.closed = true;
        this.clearCompileQueue();
        this.uploadAllPendingUploads();
    }

    @VisibleForDebug
    public String getStats() {
        return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.compileQueue.size(), this.toUpload.size(), this.bufferPool.getFreeBufferCount());
    }

    @VisibleForDebug
    public int getCompileQueueSize() {
        return this.compileQueue.size();
    }

    @VisibleForDebug
    public int getToUpload() {
        return this.toUpload.size();
    }

    @VisibleForDebug
    public int getFreeBufferCount() {
        return this.bufferPool.getFreeBufferCount();
    }

    public void pauseChunkUpdates() {
        long i = System.currentTimeMillis();
        if (this.listPausedBuilders.size() <= 0) {
            while (this.listPausedBuilders.size() != this.countRenderBuilders) {
                this.uploadAllPendingUploads();
                SectionBufferBuilderPack sectionbufferbuilderpack = this.bufferPool.acquire();
                if (sectionbufferbuilderpack != null) {
                    this.listPausedBuilders.add(sectionbufferbuilderpack);
                }

                if (System.currentTimeMillis() > i + 1000L) {
                    break;
                }
            }
        }
    }

    public void resumeChunkUpdates() {
        for (SectionBufferBuilderPack sectionbufferbuilderpack : this.listPausedBuilders) {
            this.bufferPool.release(sectionbufferbuilderpack);
        }

        this.listPausedBuilders.clear();
    }

    public boolean updateChunkNow(SectionRenderDispatcher.RenderSection renderChunk, RenderRegionCache regionCacheIn) {
        this.rebuildSectionSync(renderChunk, regionCacheIn);
        return true;
    }

    public boolean updateChunkLater(SectionRenderDispatcher.RenderSection renderChunk, RenderRegionCache regionCacheIn) {
        if (this.bufferPool.isEmpty()) {
            return false;
        }

        renderChunk.rebuildSectionAsync(regionCacheIn);
        return true;
    }

    public boolean updateTransparencyLater(SectionRenderDispatcher.RenderSection renderChunk) {
        if (this.bufferPool.isEmpty()) {
            return false;
        }

        renderChunk.resortTransparency(this);
        return true;
    }

    public void addUploadTask(Runnable r) {
        if (r != null) {
            this.toUpload.add(r);
        }
    }

    public class RenderSection {
        public static final int SIZE = 16;
        public final int index;
        public final AtomicReference<SectionMesh> sectionMesh = new AtomicReference<>(CompiledSectionMesh.UNCOMPILED);
        private SectionRenderDispatcher.RenderSection.@Nullable RebuildTask lastRebuildTask;
        private SectionRenderDispatcher.RenderSection.@Nullable ResortTransparencyTask lastResortTransparencyTask;
        private AABB bb;
        private boolean dirty = true;
        volatile long sectionNode = SectionPos.asLong(-1, -1, -1);
        final BlockPos.MutableBlockPos renderOrigin = new BlockPos.MutableBlockPos(-1, -1, -1);
        private boolean playerChanged;
        private long uploadedTime;
        private long fadeDuration;
        private boolean wasPreviouslyEmpty;
        private boolean playerUpdate = false;
        private boolean needsBackgroundPriorityUpdate;
        private boolean renderRegions = Config.isRenderRegions();
        public int regionX;
        public int regionZ;
        public int regionDX;
        public int regionDY;
        public int regionDZ;
        private VboRegion[] vboRegions;
        private final SectionRenderDispatcher.RenderSection[] renderChunksOfset16 = new SectionRenderDispatcher.RenderSection[6];
        private boolean renderChunksOffset16Updated = false;
        private LevelChunk chunk;
        private SectionOcclusionGraph.Node renderInfo = new SectionOcclusionGraph.Node(this, null, 0);
        public AabbFrame boundingBoxParent;
        private SectionPos sectionPosition;

        public RenderSection(final int p_299358_, final long p_366281_) {
            this.index = p_299358_;
            this.setSectionNode(p_366281_);
        }

        public float getVisibility(long p_451612_) {
            long i = p_451612_ - this.uploadedTime;
            return i >= this.fadeDuration ? 1.0F : (float)i / (float)this.fadeDuration;
        }

        public void setFadeDuration(long p_460134_) {
            this.fadeDuration = p_460134_;
        }

        public void setWasPreviouslyEmpty(boolean p_453036_) {
            this.wasPreviouslyEmpty = p_453036_;
        }

        public boolean wasPreviouslyEmpty() {
            return this.wasPreviouslyEmpty;
        }

        private boolean doesChunkExistAt(long p_366776_) {
            ChunkAccess chunkaccess = SectionRenderDispatcher.this.level
                .getChunk(SectionPos.x(p_366776_), SectionPos.z(p_366776_), ChunkStatus.FULL, false);
            return chunkaccess != null && SectionRenderDispatcher.this.level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(p_366776_));
        }

        public boolean hasAllNeighbors() {
            return this.doesChunkExistAt(this.sectionNode);
        }

        public AABB getBoundingBox() {
            return this.bb;
        }

        public CompletableFuture<Void> upload(Map<ChunkSectionLayer, MeshData> p_409621_, CompiledSectionMesh p_409834_) {
            if (SectionRenderDispatcher.this.closed) {
                p_409621_.values().forEach(MeshData::close);
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.runAsync(() -> p_409621_.forEach((layerIn, dataIn) -> {
                    try (Zone zone = Profiler.get().zone("Upload Section Layer")) {
                        p_409834_.uploadMeshLayer(layerIn, dataIn, this.sectionNode);
                        dataIn.close();
                    }

                    if (this.uploadedTime == 0L) {
                        this.uploadedTime = Util.getMillis();
                    }
                }), SectionRenderDispatcher.this.mainThreadUploadExecutor);
            }
        }

        public CompletableFuture<Void> uploadSectionIndexBuffer(CompiledSectionMesh p_410735_, ByteBufferBuilder.Result p_393953_, ChunkSectionLayer p_406199_) {
            if (SectionRenderDispatcher.this.closed) {
                p_393953_.close();
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.runAsync(() -> {
                    try (Zone zone = Profiler.get().zone("Upload Section Indices")) {
                        p_410735_.uploadLayerIndexBuffer(p_406199_, p_393953_, this.sectionNode);
                        p_393953_.close();
                    }
                }, SectionRenderDispatcher.this.mainThreadUploadExecutor);
            }
        }

        public void setSectionNode(long p_360921_) {
            this.reset();
            this.sectionNode = p_360921_;
            int i = SectionPos.sectionToBlockCoord(SectionPos.x(p_360921_));
            int j = SectionPos.sectionToBlockCoord(SectionPos.y(p_360921_));
            int k = SectionPos.sectionToBlockCoord(SectionPos.z(p_360921_));
            this.renderOrigin.set(i, j, k);
            this.bb = new AABB(i, j, k, i + 16, j + 16, k + 16);
            int l = i;
            int i1 = j;
            int j1 = k;
            this.sectionPosition = SectionPos.of(this.getPosition());
            if (this.renderRegions) {
                int k1 = 8;
                this.regionX = l >> k1 << k1;
                this.regionZ = j1 >> k1 << k1;
                this.regionDX = l - this.regionX;
                this.regionDY = i1;
                this.regionDZ = j1 - this.regionZ;
            }

            this.renderChunksOffset16Updated = false;
            this.chunk = null;
            this.boundingBoxParent = null;
        }

        public SectionMesh getSectionMesh() {
            return this.sectionMesh.get();
        }

        public void reset() {
            this.cancelTasks();
            this.sectionMesh.getAndSet(CompiledSectionMesh.UNCOMPILED).close();
            this.dirty = true;
            this.uploadedTime = 0L;
            this.wasPreviouslyEmpty = false;
        }

        public BlockPos getRenderOrigin() {
            return this.renderOrigin;
        }

        public BlockPos getPosition() {
            return this.renderOrigin;
        }

        public long getSectionNode() {
            return this.sectionNode;
        }

        public void setDirty(boolean p_298731_) {
            boolean flag = this.dirty;
            this.dirty = true;
            this.playerChanged = p_298731_ | (flag && this.playerChanged);
            if (this.isWorldPlayerUpdate()) {
                this.playerUpdate = true;
            }

            if (!flag) {
                SectionRenderDispatcher.this.renderer.onChunkRenderNeedsUpdate(this);
            }
        }

        public void setNotDirty() {
            this.dirty = false;
            this.playerChanged = false;
            this.playerUpdate = false;
            this.needsBackgroundPriorityUpdate = false;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public boolean isDirtyFromPlayer() {
            return this.dirty && this.playerChanged;
        }

        public long getNeighborSectionNode(Direction p_362694_) {
            return SectionPos.offset(this.sectionNode, p_362694_);
        }

        public void resortTransparency(SectionRenderDispatcher p_298196_) {
            if (this.getSectionMesh() instanceof CompiledSectionMesh compiledsectionmesh) {
                this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(compiledsectionmesh);
                p_298196_.schedule(this.lastResortTransparencyTask);
            }
        }

        public boolean hasTranslucentGeometry() {
            return this.getSectionMesh().hasTranslucentGeometry();
        }

        public boolean transparencyResortingScheduled() {
            return this.lastResortTransparencyTask != null && !this.lastResortTransparencyTask.isCompleted.get();
        }

        protected void cancelTasks() {
            if (this.lastRebuildTask != null) {
                this.lastRebuildTask.cancel();
                this.lastRebuildTask = null;
            }

            if (this.lastResortTransparencyTask != null) {
                this.lastResortTransparencyTask.cancel();
                this.lastResortTransparencyTask = null;
            }
        }

        public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache p_300037_) {
            this.cancelTasks();
            RenderSectionRegion rendersectionregion = p_300037_.createRegion(SectionRenderDispatcher.this.level, this.sectionNode);
            boolean flag = this.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED;
            this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(rendersectionregion, flag, p_300037_);
            return this.lastRebuildTask;
        }

        public void rebuildSectionAsync(RenderRegionCache p_297331_) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(p_297331_);
            SectionRenderDispatcher.this.schedule(sectionrenderdispatcher$rendersection$compiletask);
        }

        public void compileSync(RenderRegionCache p_298605_) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(p_298605_);
            sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
        }

        void setSectionMesh(SectionMesh p_408721_) {
            SectionMesh sectionmesh = this.sectionMesh.getAndSet(p_408721_);
            SectionRenderDispatcher.this.toClose.add(sectionmesh);
            SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
        }

        VertexSorting createVertexSorting(SectionPos p_393405_) {
            Vec3 vec3 = SectionRenderDispatcher.this.cameraPosition;
            return VertexSorting.byDistance(
                (float)(vec3.x - p_393405_.minBlockX()), (float)(vec3.y - p_393405_.minBlockY()), (float)(vec3.z - p_393405_.minBlockZ())
            );
        }

        private boolean isWorldPlayerUpdate() {
            if (SectionRenderDispatcher.this.level instanceof ClientLevel) {
                ClientLevel clientlevel = SectionRenderDispatcher.this.level;
                return clientlevel.isPlayerUpdate();
            } else {
                return false;
            }
        }

        public boolean isPlayerUpdate() {
            return this.playerUpdate;
        }

        public void setNeedsBackgroundPriorityUpdate(boolean needsBackgroundPriorityUpdate) {
            this.needsBackgroundPriorityUpdate = needsBackgroundPriorityUpdate;
        }

        public boolean needsBackgroundPriorityUpdate() {
            return this.needsBackgroundPriorityUpdate;
        }

        public LevelChunk getChunk() {
            return this.getChunk(this.getPosition());
        }

        private LevelChunk getChunk(BlockPos posIn) {
            LevelChunk levelchunk = this.chunk;
            if (levelchunk != null && ChunkUtils.isLoaded(levelchunk)) {
                return levelchunk;
            }

            levelchunk = SectionRenderDispatcher.this.level.getChunkAt(posIn);
            this.chunk = levelchunk;
            return levelchunk;
        }

        public boolean isChunkRegionEmpty() {
            return this.isChunkRegionEmpty(this.getPosition());
        }

        private boolean isChunkRegionEmpty(BlockPos posIn) {
            int i = posIn.getY();
            int j = i + 15;
            return this.getChunk(posIn).isYSpaceEmpty(i, j);
        }

        public SectionOcclusionGraph.Node getRenderInfo() {
            return this.renderInfo;
        }

        public SectionOcclusionGraph.Node getRenderInfo(Direction dirIn, int counterIn) {
            this.renderInfo.initialize(dirIn, counterIn);
            return this.renderInfo;
        }

        public boolean isBoundingBoxInFrustum(ICamera camera, int frameCount) {
            return this.getBoundingBoxParent().isBoundingBoxInFrustumFully(camera, frameCount) ? true : camera.isBoundingBoxInFrustum(this.bb);
        }

        public AabbFrame getBoundingBoxParent() {
            if (this.boundingBoxParent == null) {
                BlockPos blockpos = this.getPosition();
                int i = blockpos.getX();
                int j = blockpos.getY();
                int k = blockpos.getZ();
                int l = 5;
                int i1 = i >> l << l;
                int j1 = j >> l << l;
                int k1 = k >> l << l;
                if (i1 != i || j1 != j || k1 != k) {
                    AabbFrame aabbframe = SectionRenderDispatcher.this.renderer.getRenderChunk(new BlockPos(i1, j1, k1)).getBoundingBoxParent();
                    if (aabbframe != null && aabbframe.minX == i1 && aabbframe.minY == j1 && aabbframe.minZ == k1) {
                        this.boundingBoxParent = aabbframe;
                    }
                }

                if (this.boundingBoxParent == null) {
                    int l1 = 1 << l;
                    this.boundingBoxParent = new AabbFrame(i1, j1, k1, i1 + l1, j1 + l1, k1 + l1);
                }
            }

            return this.boundingBoxParent;
        }

        public ClientLevel getWorld() {
            return SectionRenderDispatcher.this.level;
        }

        public SectionPos getSectionPosition() {
            return this.sectionPosition;
        }

        public void setVboRegions(VboRegion[] vboRegions) {
            this.vboRegions = vboRegions;
        }

        @Override
        public String toString() {
            return "pos: " + this.getPosition();
        }

        public abstract class CompileTask {
            protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
            protected final AtomicBoolean isCompleted = new AtomicBoolean(false);
            protected final boolean isRecompile;

            public CompileTask(final boolean p_299251_) {
                this.isRecompile = p_299251_;
            }

            public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_300298_);

            public abstract void cancel();

            protected abstract String name();

            public boolean isRecompile() {
                return this.isRecompile;
            }

            public BlockPos getRenderOrigin() {
                return RenderSection.this.renderOrigin;
            }
        }

        class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            protected final RenderSectionRegion region;
            private RenderRegionCache renderRegionCache;

            public RebuildTask(final RenderSectionRegion p_410538_, final boolean p_299891_) {
                this(p_410538_, p_299891_, null);
            }

            public RebuildTask(final RenderSectionRegion renderCacheIn, final boolean highPriorityIn, RenderRegionCache renderRegionCacheIn) {
                super(highPriorityIn);
                this.region = renderCacheIn;
                this.renderRegionCache = renderRegionCacheIn;
                if (this.renderRegionCache != null) {
                    this.renderRegionCache.compileStarted();
                }
            }

            @Override
            protected String name() {
                return "rend_chk_rebuild";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_299595_) {
                try {
                    if (this.isCancelled.get()) {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }

                    long i = RenderSection.this.sectionNode;
                    SectionPos sectionpos = SectionPos.of(i);
                    if (this.isCancelled.get()) {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }

                    SectionCompiler.Results sectioncompiler$results;
                    try (Zone zone = Profiler.get().zone("Compile Section")) {
                        ChunkCacheOF chunkcacheof = this.region.makeChunkCacheOF();
                        sectioncompiler$results = SectionRenderDispatcher.this.sectionCompiler
                            .compile(
                                sectionpos,
                                chunkcacheof,
                                RenderSection.this.createVertexSorting(sectionpos),
                                p_299595_,
                                RenderSection.this.regionDX,
                                RenderSection.this.regionDY,
                                RenderSection.this.regionDZ
                            );
                    }

                    TranslucencyPointOfView translucencypointofview = TranslucencyPointOfView.of(SectionRenderDispatcher.this.cameraPosition, i);
                    if (this.isCancelled.get()) {
                        sectioncompiler$results.release();
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    } else {
                        CompiledSectionMesh compiledsectionmesh = new CompiledSectionMesh(translucencypointofview, sectioncompiler$results);
                        compiledsectionmesh.setVboRegions(RenderSection.this.vboRegions);
                        compiledsectionmesh.setAnimatedSprites(sectioncompiler$results.animatedSprites);
                        CompletableFuture<Void> completablefuture = RenderSection.this.upload(sectioncompiler$results.renderedLayers, compiledsectionmesh);
                        return completablefuture.handle((voidIn, excIn) -> {
                            if (excIn != null && !(excIn instanceof CancellationException) && !(excIn instanceof InterruptedException)) {
                                Minecraft.getInstance().delayCrash(CrashReport.forThrowable(excIn, "Rendering section"));
                            }

                            if (!this.isCancelled.get() && !SectionRenderDispatcher.this.closed) {
                                RenderSection.this.setSectionMesh(compiledsectionmesh);
                                return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                            } else {
                                SectionRenderDispatcher.this.toClose.add(compiledsectionmesh);
                                return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                            }
                        });
                    }
                } finally {
                    if (this.renderRegionCache != null) {
                        this.renderRegionCache.compileFinished();
                    }
                }
            }

            @Override
            public void cancel() {
                if (this.isCancelled.compareAndSet(false, true)) {
                    RenderSection.this.setDirty(false);
                }
            }
        }

        class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            private final CompiledSectionMesh compiledSectionMesh;

            public ResortTransparencyTask(final CompiledSectionMesh p_407319_) {
                super(true);
                this.compiledSectionMesh = p_407319_;
            }

            @Override
            protected String name() {
                return "rend_chk_sort";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_297366_) {
                if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                }

                MeshData.SortState meshdata$sortstate = this.compiledSectionMesh.getTransparencyState();
                if (meshdata$sortstate != null && !this.compiledSectionMesh.isEmpty(ChunkSectionLayer.TRANSLUCENT)) {
                    long i = RenderSection.this.sectionNode;
                    VertexSorting vertexsorting = RenderSection.this.createVertexSorting(SectionPos.of(i));
                    TranslucencyPointOfView translucencypointofview = TranslucencyPointOfView.of(SectionRenderDispatcher.this.cameraPosition, i);
                    if (!this.compiledSectionMesh.isDifferentPointOfView(translucencypointofview) && !translucencypointofview.isAxisAligned()) {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    } else {
                        ByteBufferBuilder.Result bytebufferbuilder$result = meshdata$sortstate.buildSortedIndexBuffer(
                            p_297366_.buffer(ChunkSectionLayer.TRANSLUCENT), vertexsorting
                        );
                        if (bytebufferbuilder$result == null) {
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else if (this.isCancelled.get()) {
                            bytebufferbuilder$result.close();
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else {
                            CompletableFuture<Void> completablefuture = RenderSection.this.uploadSectionIndexBuffer(
                                this.compiledSectionMesh, bytebufferbuilder$result, ChunkSectionLayer.TRANSLUCENT
                            );
                            return completablefuture.handle((voidIn, excIn) -> {
                                if (excIn != null && !(excIn instanceof CancellationException) && !(excIn instanceof InterruptedException)) {
                                    Minecraft.getInstance().delayCrash(CrashReport.forThrowable(excIn, "Rendering section"));
                                }

                                if (this.isCancelled.get()) {
                                    return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                                }

                                this.compiledSectionMesh.setTranslucencyPointOfView(translucencypointofview);
                                return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                            });
                        }
                    }
                } else {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                }
            }

            @Override
            public void cancel() {
                this.isCancelled.set(true);
            }
        }
    }

    enum SectionTaskResult {
        SUCCESSFUL,
        CANCELLED;
    }
}
