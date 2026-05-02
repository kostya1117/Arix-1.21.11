package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import net.optifine.BlockPosM;
import net.optifine.Vec3M;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SectionOcclusionGraph {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
    private static final int MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE = SectionPos.blockToSectionCoord(60);
    private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
    private boolean needsFullUpdate = true;
    private  Future<?> fullUpdateTask;
    private  ViewArea viewArea;
    private final AtomicReference<SectionOcclusionGraph. GraphState> currentGraph = new AtomicReference<>();
    private final AtomicReference<SectionOcclusionGraph. GraphEvents> nextGraphEvents = new AtomicReference<>();
    private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);
    private LevelRenderer levelRenderer;

    public void waitAndReset( ViewArea p_298923_) {
        if (this.fullUpdateTask != null) {
            try {
                this.fullUpdateTask.get();
                this.fullUpdateTask = null;
            } catch (Exception exception) {
                LOGGER.warn("Full update failed", exception);
            }
        }

        this.viewArea = p_298923_;
        this.levelRenderer = Minecraft.getInstance().levelRenderer;
        if (p_298923_ != null) {
            this.currentGraph.set(new SectionOcclusionGraph.GraphState(p_298923_));
            this.invalidate();
        } else {
            this.currentGraph.set(null);
        }
    }

    public void invalidate() {
        this.needsFullUpdate = true;
    }

    public void addSectionsInFrustum(Frustum p_299761_, List<SectionRenderDispatcher.RenderSection> p_301346_, List<SectionRenderDispatcher.RenderSection> p_365911_) {
        this.addSectionsInFrustum(p_299761_, p_301346_, p_365911_, true, -1);
    }

    public void addSectionsInFrustum(
        Frustum frustumIn,
        List<SectionRenderDispatcher.RenderSection> sectionsIn,
        List<SectionRenderDispatcher.RenderSection> sectionsNearIn,
        boolean updateSections,
        int maxChunkDistance
    ) {
        List<SectionRenderDispatcher.RenderSection> list = this.levelRenderer.getRenderInfosTerrain();
        List<SectionRenderDispatcher.RenderSection> list1 = this.levelRenderer.getRenderInfosTileEntities();
        int i = (int)frustumIn.getCameraX() >> 4 << 4;
        int j = (int)frustumIn.getCameraY() >> 4 << 4;
        int k = (int)frustumIn.getCameraZ() >> 4 << 4;
        int l = maxChunkDistance * maxChunkDistance;
        this.currentGraph.get().storage().sectionTree.visitNodes((nodeIn, skipFrustumIn, levelIn, nearIn) -> {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = nodeIn.getSection();
            if (sectionrenderdispatcher$rendersection != null) {
                if (maxChunkDistance > 0) {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getPosition();
                    int i1 = i - blockpos.getX();
                    int j1 = j - blockpos.getY();
                    int k1 = k - blockpos.getZ();
                    int l1 = i1 * i1 + j1 * j1 + k1 * k1;
                    if (l1 > l) {
                        return;
                    }
                }

                if (updateSections) {
                    sectionsIn.add(sectionrenderdispatcher$rendersection);
                }

                if (nearIn) {
                    sectionsNearIn.add(sectionrenderdispatcher$rendersection);
                }

                SectionMesh sectionmesh = sectionrenderdispatcher$rendersection.getSectionMesh();
                if (!sectionmesh.isEmpty()) {
                    list.add(sectionrenderdispatcher$rendersection);
                }

                if (!sectionmesh.getRenderableBlockEntities().isEmpty()) {
                    list1.add(sectionrenderdispatcher$rendersection);
                }
            }
        }, frustumIn, 32);
    }

    public boolean consumeFrustumUpdate() {
        return this.needsFrustumUpdate.compareAndSet(true, false);
    }

    public void onChunkReadyToRender(ChunkPos p_299612_) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            this.addNeighbors(sectionocclusiongraph$graphevents, p_299612_);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            this.addNeighbors(sectionocclusiongraph$graphevents1, p_299612_);
        }
    }

    public void schedulePropagationFrom(SectionRenderDispatcher.RenderSection p_301377_) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            sectionocclusiongraph$graphevents.sectionsToPropagateFrom.add(p_301377_);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            sectionocclusiongraph$graphevents1.sectionsToPropagateFrom.add(p_301377_);
        }

        if (p_301377_.getSectionMesh().hasTerrainBlockEntities()) {
            this.needsFrustumUpdate.set(true);
        }
    }

    public void update(
        boolean p_301275_, Camera p_298972_, Frustum p_298939_, List<SectionRenderDispatcher.RenderSection> p_300432_, LongOpenHashSet p_365816_
    ) {
        Vec3 vec3 = p_298972_.position();
        if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
            this.scheduleFullUpdate(p_301275_, p_298972_, vec3, p_365816_);
        }

        this.runPartialUpdate(p_301275_, p_298939_, p_300432_, vec3, p_365816_);
    }

    private void scheduleFullUpdate(boolean p_298569_, Camera p_299582_, Vec3 p_297830_, LongOpenHashSet p_370191_) {
        this.needsFullUpdate = false;
        LongOpenHashSet longopenhashset = p_370191_.clone();
        this.fullUpdateTask = CompletableFuture.runAsync(() -> {
            SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = new SectionOcclusionGraph.GraphState(this.viewArea);
            this.nextGraphEvents.set(sectionocclusiongraph$graphstate.events);
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();
            this.initializeQueueForFullUpdate(p_299582_, queue);
            queue.forEach(nodeIn -> sectionocclusiongraph$graphstate.storage.sectionToNodeMap.put(nodeIn.section, nodeIn));
            this.runUpdates(sectionocclusiongraph$graphstate.storage, p_297830_, queue, p_298569_, sectionIn -> {}, longopenhashset);
            this.currentGraph.set(sectionocclusiongraph$graphstate);
            this.nextGraphEvents.set(null);
            this.needsFrustumUpdate.set(true);
        }, Util.backgroundExecutor());
    }

    private void runPartialUpdate(
        boolean p_298388_, Frustum p_299940_, List<SectionRenderDispatcher.RenderSection> p_297967_, Vec3 p_299094_, LongOpenHashSet p_363554_
    ) {
        SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = this.currentGraph.get();
        this.queueSectionsWithNewNeighbors(sectionocclusiongraph$graphstate);
        if (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();

            while (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.poll();
                SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph$graphstate.storage
                    .sectionToNodeMap
                    .get(sectionrenderdispatcher$rendersection);
                if (sectionocclusiongraph$node != null && sectionocclusiongraph$node.section == sectionrenderdispatcher$rendersection) {
                    queue.add(sectionocclusiongraph$node);
                }
            }

            List<SectionRenderDispatcher.RenderSection> list1 = this.levelRenderer.getRenderInfos();
            List<SectionRenderDispatcher.RenderSection> list2 = this.levelRenderer.getRenderInfosTerrain();
            List<SectionRenderDispatcher.RenderSection> list = this.levelRenderer.getRenderInfosTileEntities();
            Frustum frustum = LevelRenderer.offsetFrustum(p_299940_);
            Consumer<SectionRenderDispatcher.RenderSection> consumer = sectionIn -> {
                if (frustum.isVisible(sectionIn.getBoundingBox())) {
                    this.needsFrustumUpdate.set(true);
                    if (sectionIn == list1) {
                        SectionMesh sectionmesh = sectionIn.sectionMesh.get();
                        if (!sectionmesh.isEmpty()) {
                            list2.add(sectionIn);
                        }

                        if (!sectionmesh.getRenderableBlockEntities().isEmpty()) {
                            list.add(sectionIn);
                        }
                    }
                }
            };
            this.runUpdates(sectionocclusiongraph$graphstate.storage, p_299094_, queue, p_298388_, consumer, p_363554_);
        }
    }

    private void queueSectionsWithNewNeighbors(SectionOcclusionGraph.GraphState p_298801_) {
        LongIterator longiterator = p_298801_.events.chunksWhichReceivedNeighbors.iterator();

        while (longiterator.hasNext()) {
            long i = longiterator.nextLong();
            List<SectionRenderDispatcher.RenderSection> list = p_298801_.storage.chunksWaitingForNeighbors.get(i);
            if (list != null && list.get(0).hasAllNeighbors()) {
                p_298801_.events.sectionsToPropagateFrom.addAll(list);
                p_298801_.storage.chunksWaitingForNeighbors.remove(i);
            }
        }

        p_298801_.events.chunksWhichReceivedNeighbors.clear();
    }

    private void addNeighbors(SectionOcclusionGraph.GraphEvents p_300825_, ChunkPos p_297758_) {
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x - 1, p_297758_.z));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x, p_297758_.z - 1));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x + 1, p_297758_.z));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x, p_297758_.z + 1));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x - 1, p_297758_.z - 1));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x - 1, p_297758_.z + 1));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x + 1, p_297758_.z - 1));
        p_300825_.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(p_297758_.x + 1, p_297758_.z + 1));
    }

    private void initializeQueueForFullUpdate(Camera p_298889_, Queue<SectionOcclusionGraph.Node> p_297605_) {
        BlockPos blockpos = p_298889_.blockPosition();
        long i = SectionPos.asLong(blockpos);
        int j = SectionPos.y(i);
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSection(i);
        if (sectionrenderdispatcher$rendersection == null) {
            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
            boolean flag = j < levelheightaccessor.getMinSectionY();
            int k = flag ? levelheightaccessor.getMinSectionY() : levelheightaccessor.getMaxSectionY();
            int l = this.viewArea.getViewDistance();
            List<SectionOcclusionGraph.Node> list = Lists.newArrayList();
            int i1 = SectionPos.x(i);
            int j1 = SectionPos.z(i);

            for (int k1 = -l; k1 <= l; k1++) {
                for (int l1 = -l; l1 <= l; l1++) {
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.viewArea
                        .getRenderSection(SectionPos.asLong(k1 + i1, k, l1 + j1));
                    if (sectionrenderdispatcher$rendersection1 != null && this.isInViewDistance(i, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                        Direction direction = flag ? Direction.UP : Direction.DOWN;
                        SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionrenderdispatcher$rendersection1.getRenderInfo(direction, 0);
                        sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (k1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.EAST);
                        } else if (k1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.WEST);
                        }

                        if (l1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.SOUTH);
                        } else if (l1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.NORTH);
                        }

                        list.add(sectionocclusiongraph$node);
                    }
                }
            }

            list.sort(Comparator.comparingDouble(nodeIn -> blockpos.distSqr(SectionPos.of(nodeIn.section.getSectionNode()).center())));
            p_297605_.addAll(list);
        } else {
            p_297605_.add(sectionrenderdispatcher$rendersection.getRenderInfo(null, 0));
        }
    }

    private void runUpdates(
        SectionOcclusionGraph.GraphStorage p_299200_,
        Vec3 p_300018_,
        Queue<SectionOcclusionGraph.Node> p_300570_,
        boolean p_300892_,
        Consumer<SectionRenderDispatcher.RenderSection> p_298647_,
        LongOpenHashSet p_362895_
    ) {
        SectionPos sectionpos = SectionPos.of(p_300018_);
        long i = sectionpos.asLong();
        BlockPos blockpos = sectionpos.center();

        while (!p_300570_.isEmpty()) {
            SectionOcclusionGraph.Node sectionocclusiongraph$node = p_300570_.poll();
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$node.section;
            if (!p_362895_.contains(sectionocclusiongraph$node.section.getSectionNode())) {
                if (p_299200_.sectionTree.add(sectionocclusiongraph$node.section)) {
                    p_298647_.accept(sectionocclusiongraph$node.section);
                }
            } else {
                sectionocclusiongraph$node.section.sectionMesh.compareAndSet(CompiledSectionMesh.UNCOMPILED, CompiledSectionMesh.EMPTY);
            }

            long j = sectionrenderdispatcher$rendersection.getSectionNode();
            boolean flag = Math.abs(SectionPos.x(j) - sectionpos.x()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
                || Math.abs(SectionPos.y(j) - sectionpos.y()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
                || Math.abs(SectionPos.z(j) - sectionpos.z()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE;

            for (Direction direction : DIRECTIONS) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.getRelativeFrom(
                    i, sectionrenderdispatcher$rendersection, direction
                );
                if (sectionrenderdispatcher$rendersection1 != null && (!p_300892_ || !sectionocclusiongraph$node.hasDirection(direction.getOpposite()))) {
                    if (p_300892_ && sectionocclusiongraph$node.hasSourceDirections()) {
                        SectionMesh sectionmesh = sectionrenderdispatcher$rendersection.getSectionMesh();
                        boolean flag1 = false;

                        for (int k = 0; k < DIRECTIONS.length; k++) {
                            if (sectionocclusiongraph$node.hasSourceDirection(k) && sectionmesh.facesCanSeeEachother(DIRECTIONS[k].getOpposite(), direction)) {
                                flag1 = true;
                                break;
                            }
                        }

                        if (!flag1) {
                            continue;
                        }
                    }

                    if (p_300892_ && flag) {
                        int l = SectionPos.sectionToBlockCoord(SectionPos.x(j));
                        int i1 = SectionPos.sectionToBlockCoord(SectionPos.y(j));
                        int j1 = SectionPos.sectionToBlockCoord(SectionPos.z(j));
                        boolean flag2 = direction.getAxis() == Direction.Axis.X ? blockpos.getX() > l : blockpos.getX() < l;
                        boolean flag3 = direction.getAxis() == Direction.Axis.Y ? blockpos.getY() > i1 : blockpos.getY() < i1;
                        boolean flag4 = direction.getAxis() == Direction.Axis.Z ? blockpos.getZ() > j1 : blockpos.getZ() < j1;
                        Vec3M vec3m = p_299200_.vec3M1.set(l + (flag2 ? 16 : 0), i1 + (flag3 ? 16 : 0), j1 + (flag4 ? 16 : 0));
                        Vec3M vec3m1 = p_299200_.vec3M2.set(p_300018_.x, p_300018_.y, p_300018_.z).sub(vec3m).normalize().mul(CEILED_SECTION_DIAGONAL);
                        boolean flag5 = true;

                        while (vec3m.distanceSquared(p_300018_.x, p_300018_.y, p_300018_.z) > 3600.0) {
                            vec3m.add(vec3m1);
                            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
                            if (vec3m.y > levelheightaccessor.getMaxY() || vec3m.y < levelheightaccessor.getMinY()) {
                                break;
                            }

                            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = this.viewArea
                                .getRenderSectionAt(p_299200_.blockPosM1.setXyz(vec3m.x, vec3m.y, vec3m.z));
                            if (sectionrenderdispatcher$rendersection2 == null || p_299200_.sectionToNodeMap.get(sectionrenderdispatcher$rendersection2) == null
                                )
                             {
                                flag5 = false;
                                break;
                            }
                        }

                        if (!flag5) {
                            continue;
                        }
                    }

                    SectionOcclusionGraph.Node sectionocclusiongraph$node1 = p_299200_.sectionToNodeMap.get(sectionrenderdispatcher$rendersection1);
                    if (sectionocclusiongraph$node1 != null) {
                        sectionocclusiongraph$node1.addSourceDirection(direction);
                    } else {
                        SectionOcclusionGraph.Node sectionocclusiongraph$node2 = sectionrenderdispatcher$rendersection1.getRenderInfo(
                            direction, sectionocclusiongraph$node.step + 1
                        );
                        sectionocclusiongraph$node2.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (sectionrenderdispatcher$rendersection1.hasAllNeighbors()) {
                            p_300570_.add(sectionocclusiongraph$node2);
                            p_299200_.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                        } else if (this.isInViewDistance(i, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                            p_299200_.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                            long k1 = SectionPos.sectionToChunk(sectionrenderdispatcher$rendersection1.getSectionNode());
                            p_299200_.chunksWaitingForNeighbors.computeIfAbsent(k1, posLongIn -> new ArrayList<>()).add(sectionrenderdispatcher$rendersection1);
                        }
                    }
                }
            }
        }
    }

    private boolean isInViewDistance(long p_363726_, long p_370059_) {
        return ChunkTrackingView.isInViewDistance(
            SectionPos.x(p_363726_),
            SectionPos.z(p_363726_),
            this.viewArea.getViewDistance(),
            SectionPos.x(p_370059_),
            SectionPos.z(p_370059_)
        );
    }

    private SectionRenderDispatcher. RenderSection getRelativeFrom(long p_369239_, SectionRenderDispatcher.RenderSection p_299737_, Direction p_301139_) {
        long i = p_299737_.getNeighborSectionNode(p_301139_);
        int j = SectionPos.sectionToBlockCoord(SectionPos.y(p_369239_));
        int k = SectionPos.sectionToBlockCoord(SectionPos.y(i));
        ClientLevel clientlevel = this.levelRenderer.level;
        if (k >= clientlevel.getMinY() && k < clientlevel.getMaxY()) {
            if (Mth.abs(j - k) > this.levelRenderer.renderDistance) {
                return null;
            }

            int l = SectionPos.sectionToBlockCoord(SectionPos.x(p_369239_));
            int i1 = SectionPos.sectionToBlockCoord(SectionPos.z(p_369239_));
            int j1 = SectionPos.sectionToBlockCoord(SectionPos.x(i));
            int k1 = SectionPos.sectionToBlockCoord(SectionPos.z(i));
            int l1 = l - j1;
            int i2 = i1 - k1;
            int j2 = l1 * l1 + i2 * i2;
            return j2 > this.levelRenderer.renderDistanceXZSq ? null : this.viewArea.getRenderSection(i);
        } else {
            return null;
        }
    }

    @VisibleForDebug
    public SectionOcclusionGraph. Node getNode(SectionRenderDispatcher.RenderSection p_299335_) {
        return this.currentGraph.get().storage.sectionToNodeMap.get(p_299335_);
    }

    public Octree getOctree() {
        return this.currentGraph.get().storage.sectionTree;
    }

    public boolean needsFrustumUpdate() {
        return this.needsFrustumUpdate.get();
    }

    public void setNeedsFrustumUpdate(boolean val) {
        this.needsFrustumUpdate.set(val);
    }

    record GraphEvents(LongSet chunksWhichReceivedNeighbors, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
        GraphEvents() {
            this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
        }
    }

    record GraphState(SectionOcclusionGraph.GraphStorage storage, SectionOcclusionGraph.GraphEvents events) {
        GraphState(ViewArea p_367222_) {
            this(new SectionOcclusionGraph.GraphStorage(p_367222_), new SectionOcclusionGraph.GraphEvents());
        }
    }

    static class GraphStorage {
        public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
        public final Octree sectionTree;
        public final Long2ObjectMap<List<SectionRenderDispatcher.RenderSection>> chunksWaitingForNeighbors;
        public final Vec3M vec3M1 = new Vec3M(0.0, 0.0, 0.0);
        public final Vec3M vec3M2 = new Vec3M(0.0, 0.0, 0.0);
        public final Vec3M vec3M3 = new Vec3M(0.0, 0.0, 0.0);
        public final BlockPosM blockPosM1 = new BlockPosM();

        public GraphStorage(ViewArea p_364979_) {
            this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(p_364979_.sections.length);
            this.sectionTree = new Octree(p_364979_.getCameraSectionPos(), p_364979_.getViewDistance(), p_364979_.sectionGridSizeY, p_364979_.level.getMinY());
            this.chunksWaitingForNeighbors = new Long2ObjectOpenHashMap<>();
        }

        @Override
        public String toString() {
            return "sectionToNode: " + this.sectionToNodeMap + ", sectionTree: " + this.sectionTree + ", sectionsWaiting: " + this.chunksWaitingForNeighbors;
        }
    }

    @VisibleForDebug
    public static class Node {
        @VisibleForDebug
        public final SectionRenderDispatcher.RenderSection section;
        private int sourceDirections;
        int directions;
        @VisibleForDebug
        public int step;

        public Node(SectionRenderDispatcher.RenderSection p_299649_,  Direction p_299325_, int p_298364_) {
            this.section = p_299649_;
            if (p_299325_ != null) {
                this.addSourceDirection(p_299325_);
            }

            this.step = p_298364_;
        }

        void setDirections(int directionsIn, Direction directionIn) {
            this.directions = this.directions | directionsIn | 1 << directionIn.ordinal();
        }

        public void initialize(Direction facingIn, int counter) {
            this.sourceDirections = facingIn != null ? 1 << facingIn.ordinal() : 0;
            this.directions = 0;
            this.step = counter;
        }

        @Override
        public String toString() {
            return this.section.getPosition() + "";
        }

        boolean hasDirection(Direction p_299145_) {
            return (this.directions & 1 << p_299145_.ordinal()) > 0;
        }

        void addSourceDirection(Direction p_299877_) {
            this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << p_299877_.ordinal());
        }

        @VisibleForDebug
        public boolean hasSourceDirection(int p_301075_) {
            return (this.sourceDirections & 1 << p_301075_) > 0;
        }

        boolean hasSourceDirections() {
            return this.sourceDirections != 0;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.section.getSectionNode());
        }

        @Override
        public boolean equals(Object p_300561_) {
            return p_300561_ instanceof SectionOcclusionGraph.Node sectionocclusiongraph$node
                ? this.section.getSectionNode() == sectionocclusiongraph$node.section.getSectionNode()
                : false;
        }
    }

    static class SectionToNodeMap {
        private final SectionOcclusionGraph.Node[] nodes;

        SectionToNodeMap(int p_298573_) {
            this.nodes = new SectionOcclusionGraph.Node[p_298573_];
        }

        public void put(SectionRenderDispatcher.RenderSection p_297513_, SectionOcclusionGraph.Node p_298532_) {
            this.nodes[p_297513_.index] = p_298532_;
        }

        public SectionOcclusionGraph. Node get(SectionRenderDispatcher.RenderSection p_297749_) {
            int i = p_297749_.index;
            return i >= 0 && i < this.nodes.length ? this.nodes[i] : null;
        }

        @Override
        public String toString() {
            StringBuilder stringbuilder = new StringBuilder();
            int i = 0;

            for (int j = 0; j < this.nodes.length; j++) {
                SectionOcclusionGraph.Node sectionocclusiongraph$node = this.nodes[j];
                if (sectionocclusiongraph$node != null) {
                    if (!stringbuilder.isEmpty()) {
                        stringbuilder.append(", ");
                    }

                    stringbuilder.append(j + ":" + sectionocclusiongraph$node);
                    if (i++ > 100) {
                        stringbuilder.append(", ...");
                        break;
                    }
                }
            }

            return "[" + stringbuilder.toString() + "]";
        }
    }
}
