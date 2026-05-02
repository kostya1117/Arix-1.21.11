package net.minecraft.client.gui.components;

import com.google.common.base.Strings;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.DataFixUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debugchart.BandwidthDebugChart;
import net.minecraft.client.gui.components.debugchart.FpsDebugChart;
import net.minecraft.client.gui.components.debugchart.PingDebugChart;
import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.client.gui.components.debugchart.TpsDebugChart;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.optifine.Lagometer;
import net.optifine.QuickInfo;
import net.optifine.render.GuiRenderCache;
import net.optifine.util.Mutable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public class DebugScreenOverlay {
    private static final float CROSSHAIR_SCALE = 0.01F;
    private static final int CROSSHAIR_INDEX_COUNT = 36;
    private static final int MARGIN_RIGHT = 2;
    private static final int MARGIN_LEFT = 2;
    private static final int MARGIN_TOP = 2;
    private final Minecraft minecraft;
    private final Font font;
    private final GpuBuffer crosshairBuffer;
    private final RenderSystem.AutoStorageIndexBuffer crosshairIndicies = RenderSystem.getSequentialBuffer(VertexFormat.Mode.LINES);
    private @Nullable ChunkPos lastPos;
    private @Nullable LevelChunk clientChunk;
    private @Nullable CompletableFuture<LevelChunk> serverChunk;
    private boolean renderProfilerChart;
    private boolean renderFpsCharts;
    private boolean renderNetworkCharts;
    private final LocalSampleLogger frameTimeLogger = new LocalSampleLogger(1);
    private final LocalSampleLogger tickTimeLogger = new LocalSampleLogger(TpsDebugDimensions.values().length);
    private final LocalSampleLogger pingLogger = new LocalSampleLogger(1);
    private final LocalSampleLogger bandwidthLogger = new LocalSampleLogger(1);
    private final Map<RemoteDebugSampleType, LocalSampleLogger> remoteSupportingLoggers = Map.of(RemoteDebugSampleType.TICK_TIME, this.tickTimeLogger);
    private final FpsDebugChart fpsChart;
    private final TpsDebugChart tpsChart;
    private final PingDebugChart pingChart;
    private final BandwidthDebugChart bandwidthChart;
    private final ProfilerPieChart profilerPieChart;
    private GuiRenderCache renderCache;

    public DebugScreenOverlay(Minecraft p_94039_) {
        this.minecraft = p_94039_;
        this.font = p_94039_.font;
        this.fpsChart = new FpsDebugChart(this.font, this.frameTimeLogger);
        this.tpsChart = new TpsDebugChart(this.font, this.tickTimeLogger, () -> p_94039_.level == null ? 0.0F : p_94039_.level.tickRateManager().millisecondsPerTick());
        this.pingChart = new PingDebugChart(this.font, this.pingLogger);
        this.bandwidthChart = new BandwidthDebugChart(this.font, this.bandwidthLogger);
        this.profilerPieChart = new ProfilerPieChart(this.font);

        try (ByteBufferBuilder bytebufferbuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH.getVertexSize() * 12 * 2)) {
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(4.0F);
            bufferbuilder.addVertex(1.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(4.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(4.0F);
            bufferbuilder.addVertex(0.0F, 1.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(4.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(4.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 1.0F).setColor(-16777216).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(4.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(2.0F);
            bufferbuilder.addVertex(1.0F, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F).setLineWidth(2.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(2.0F);
            bufferbuilder.addVertex(0.0F, 1.0F, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(2.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(2.0F);
            bufferbuilder.addVertex(0.0F, 0.0F, 1.0F).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F).setLineWidth(2.0F);

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                this.crosshairBuffer = RenderSystem.getDevice().createBuffer(() -> "Crosshair vertex buffer", 32, meshdata.vertexBuffer());
            }
        }

        this.renderCache = new GuiRenderCache(p_94039_.gameRenderer.getGuiRenderer(), 100L);
    }

    public void clearChunkCache() {
        this.serverChunk = null;
        this.clientChunk = null;
    }

    public void render(GuiGraphics p_281427_) {
        Options options = this.minecraft.options;
        if (this.minecraft.isGameLoadFinished() && (!options.hideGui || this.minecraft.screen != null)) {
            Collection<Identifier> collection = this.minecraft.debugEntries.getCurrentlyEnabled();
            boolean flag = false;
            if (!collection.isEmpty()) {
                p_281427_.nextStratum();
                ProfilerFiller profilerfiller = Profiler.get();
                profilerfiller.push("debug");
                ChunkPos chunkpos;
                if (this.minecraft.getCameraEntity() != null && this.minecraft.level != null) {
                    BlockPos blockpos = this.minecraft.getCameraEntity().blockPosition();
                    chunkpos = new ChunkPos(blockpos);
                } else {
                    chunkpos = null;
                }

                if (!Objects.equals(this.lastPos, chunkpos)) {
                    this.lastPos = chunkpos;
                    this.clearChunkCache();
                }

                List<String> list3 = new ArrayList<>();
                List<String> list = new ArrayList<>();
                final List<String> list1 = list3;
                final List<String> list2 = list;
                final Mutable<Identifier> mutable = new Mutable<>();
                DebugScreenDisplayer debugscreendisplayer = new DebugScreenDisplayer() {
                    private int indexFps = -1;

                    @Override
                    public void addPriorityLine(String p_427095_) {
                        this.addLine(p_427095_);
                    }

                    @Override
                    public void addLine(String p_427386_) {
                        if (DebugScreenEntries.isFpsLine(mutable.get())) {
                            if (this.indexFps >= 0) {
                                String s6 = list1.get(this.indexFps);
                                s6 = s6 + " " + p_427386_;
                                list1.set(this.indexFps, s6);
                                return;
                            }

                            this.indexFps = list1.size();
                        }

                        List<String> list4 = DebugScreenEntries.isLineLeft(mutable.get()) ? list1 : list2;
                        list4.add(p_427386_);
                    }

                    @Override
                    public void addToGroup(Identifier p_456752_, Collection<String> p_426939_) {
                        List<String> list4 = DebugScreenEntries.isLineLeft(mutable.get()) ? list1 : list2;
                        if (DebugScreenEntries.isNewLine(mutable.get())) {
                            list4.add("");
                        }

                        list4.addAll(p_426939_);
                    }

                    @Override
                    public void addToGroup(Identifier p_452831_, String p_426337_) {
                        List<String> list4 = DebugScreenEntries.isLineLeft(mutable.get()) ? list1 : list2;
                        if (DebugScreenEntries.isNewLine(mutable.get())) {
                            list4.add("");
                        }

                        list4.add(p_426337_);
                    }
                };
                Level level = this.getLevel();

                for (Identifier identifier : collection) {
                    mutable.set(identifier);
                    DebugScreenEntry debugscreenentry = DebugScreenEntries.getEntry(identifier);
                    if (debugscreenentry != null) {
                        debugscreenentry.display(debugscreendisplayer, level, this.getClientChunk(), this.getServerChunk());
                    }
                }

                if (this.minecraft.debugEntries.isOverlayVisible()) {
                    list3.add("");
                    boolean flag1 = this.minecraft.getSingleplayerServer() != null;
                    KeyMapping keymapping = options.keyDebugModifier;
                    String s5 = keymapping.getTranslatedKeyMessage().getString();
                    String s = "[" + (keymapping.isUnbound() ? "" : s5 + "+");
                    String s1 = s + options.keyDebugPofilingChart.getTranslatedKeyMessage().getString() + "]";
                    String s2 = s + options.keyDebugFpsCharts.getTranslatedKeyMessage().getString() + "]";
                    String s3 = s + options.keyDebugNetworkCharts.getTranslatedKeyMessage().getString() + "]";
                    list3.add(
                        "Debug charts: "
                            + s1
                            + " Profiler "
                            + (this.renderProfilerChart ? "visible" : "hidden")
                            + "; "
                            + s2
                            + " "
                            + (flag1 ? "FPS + TPS " : "FPS ")
                            + (this.renderFpsCharts ? "visible" : "hidden")
                            + "; "
                            + s3
                            + " "
                            + (!this.minecraft.isLocalServer() ? "Bandwidth + Ping" : "Ping")
                            + (this.renderNetworkCharts ? " visible" : " hidden")
                    );
                    String s4 = s + options.keyDebugDebugOptions.getTranslatedKeyMessage().getString() + "]";
                    list3.add("To edit: press " + s4);
                }

                this.renderLines(p_281427_, list3, true);
                this.renderLines(p_281427_, list, false);
                flag = !list3.isEmpty() || !list.isEmpty();
                p_281427_.nextStratum();
                this.profilerPieChart.setBottomOffset(10);
                if (this.showFpsCharts()) {
                    int i = p_281427_.guiWidth();
                    int k = i / 2;
                    if (Boolean.TRUE) {
                        Lagometer.renderLagometer(p_281427_, this.minecraft.getWindow().getGuiScale());
                    } else {
                        this.fpsChart.drawChart(p_281427_, 0, this.fpsChart.getWidth(k));
                    }

                    if (this.tickTimeLogger.size() > 0) {
                        int i1 = this.tpsChart.getWidth(k);
                        this.tpsChart.drawChart(p_281427_, i - i1, i1);
                    }

                    this.profilerPieChart.setBottomOffset(this.tpsChart.getFullHeight());
                }

                if (this.showNetworkCharts() && this.minecraft.getConnection() != null) {
                    int j = p_281427_.guiWidth();
                    int l = j / 2;
                    if (!this.minecraft.isLocalServer()) {
                        this.bandwidthChart.drawChart(p_281427_, 0, this.bandwidthChart.getWidth(l));
                    }

                    int j1 = this.pingChart.getWidth(l);
                    this.pingChart.drawChart(p_281427_, j - j1, j1);
                    this.profilerPieChart.setBottomOffset(this.pingChart.getFullHeight());
                }

                if (this.minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.VISUALIZE_CHUNKS_ON_SERVER)) {
                    IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
                    if (integratedserver != null && this.minecraft.player != null) {
                        ChunkLoadStatusView chunkloadstatusview = integratedserver.createChunkLoadStatusView(16 + ChunkLevel.RADIUS_AROUND_FULL_CHUNK);
                        chunkloadstatusview.moveTo(this.minecraft.player.level().dimension(), this.minecraft.player.chunkPosition());
                        LevelLoadingScreen.renderChunks(p_281427_, p_281427_.guiWidth() / 2, p_281427_.guiHeight() / 2, 4, 1, chunkloadstatusview);
                    }
                }

                try (Zone zone = profilerfiller.zone("profilerPie")) {
                    this.profilerPieChart.render(p_281427_);
                }

                profilerfiller.pop();
            }

            if (!flag && this.minecraft.options.ofQuickInfo) {
                p_281427_.nextStratum();
                QuickInfo.render(p_281427_);
            }
        }
    }

    private void renderLines(GuiGraphics p_286519_, List<String> p_286665_, boolean p_286644_) {
        int i = 9;

        for (int j = 0; j < p_286665_.size(); j++) {
            String s = p_286665_.get(j);
            if (!Strings.isNullOrEmpty(s)) {
                int k = this.font.width(s);
                int l = p_286644_ ? 2 : p_286519_.guiWidth() - 2 - k;
                int i1 = 2 + i * j;
                p_286519_.fill(l - 1, i1 - 1, l + k + 1, i1 + i - 1, -1873784752);
            }
        }

        for (int j1 = 0; j1 < p_286665_.size(); j1++) {
            String s1 = p_286665_.get(j1);
            if (!Strings.isNullOrEmpty(s1)) {
                int k1 = this.font.width(s1);
                int l1 = p_286644_ ? 2 : p_286519_.guiWidth() - 2 - k1;
                int i2 = 2 + i * j1;
                p_286519_.drawString(this.font, s1, l1, i2, -2039584, false);
            }
        }
    }

    private @Nullable ServerLevel getServerLevel() {
        if (this.minecraft.level == null) {
            return null;
        }

        IntegratedServer integratedserver = this.minecraft.getSingleplayerServer();
        return integratedserver != null ? integratedserver.getLevel(this.minecraft.level.dimension()) : null;
    }

    private @Nullable Level getLevel() {
        return this.minecraft.level == null
            ? null
            : DataFixUtils.orElse(
                Optional.ofNullable(this.minecraft.getSingleplayerServer()).flatMap(server2In -> Optional.ofNullable(server2In.getLevel(this.minecraft.level.dimension()))),
                this.minecraft.level
            );
    }

    private @Nullable LevelChunk getServerChunk() {
        if (this.minecraft.level != null && this.lastPos != null) {
            if (this.serverChunk == null) {
                ServerLevel serverlevel = this.getServerLevel();
                if (serverlevel == null) {
                    return null;
                }

                this.serverChunk = serverlevel.getChunkSource()
                    .getChunkFuture(this.lastPos.x, this.lastPos.z, ChunkStatus.FULL, false)
                    .thenApply(chunkIn -> (LevelChunk)chunkIn.orElse(null));
            }

            return this.serverChunk.getNow(null);
        } else {
            return null;
        }
    }

    private @Nullable LevelChunk getClientChunk() {
        if (this.minecraft.level != null && this.lastPos != null) {
            if (this.clientChunk == null) {
                this.clientChunk = this.minecraft.level.getChunk(this.lastPos.x, this.lastPos.z);
            }

            return this.clientChunk;
        } else {
            return null;
        }
    }

    public boolean showDebugScreen() {
        DebugScreenEntryList debugscreenentrylist = this.minecraft.debugEntries;
        return (debugscreenentrylist.isOverlayVisible() || !debugscreenentrylist.getCurrentlyEnabled().isEmpty())
            && (!this.minecraft.options.hideGui || this.minecraft.screen != null);
    }

    public boolean showProfilerChart() {
        return this.minecraft.debugEntries.isOverlayVisible() && this.renderProfilerChart;
    }

    public boolean showNetworkCharts() {
        return this.minecraft.debugEntries.isOverlayVisible() && this.renderNetworkCharts;
    }

    public boolean showFpsCharts() {
        return this.minecraft.debugEntries.isOverlayVisible() && this.renderFpsCharts;
    }

    public void toggleNetworkCharts() {
        this.renderNetworkCharts = !this.minecraft.debugEntries.isOverlayVisible() || !this.renderNetworkCharts;
        if (this.renderNetworkCharts) {
            this.minecraft.debugEntries.setOverlayVisible(true);
            this.renderFpsCharts = false;
        }
    }

    public void toggleFpsCharts() {
        this.renderFpsCharts = !this.minecraft.debugEntries.isOverlayVisible() || !this.renderFpsCharts;
        if (this.renderFpsCharts) {
            this.minecraft.debugEntries.setOverlayVisible(true);
            this.renderNetworkCharts = false;
        }
    }

    public void toggleProfilerChart() {
        this.renderProfilerChart = !this.minecraft.debugEntries.isOverlayVisible() || !this.renderProfilerChart;
        if (this.renderProfilerChart) {
            this.minecraft.debugEntries.setOverlayVisible(true);
        }
    }

    public void logFrameDuration(long p_300948_) {
        this.frameTimeLogger.logSample(p_300948_);
    }

    public LocalSampleLogger getTickTimeLogger() {
        return this.tickTimeLogger;
    }

    public LocalSampleLogger getPingLogger() {
        return this.pingLogger;
    }

    public LocalSampleLogger getBandwidthLogger() {
        return this.bandwidthLogger;
    }

    public ProfilerPieChart getProfilerPieChart() {
        return this.profilerPieChart;
    }

    public void logRemoteSample(long[] p_333428_, RemoteDebugSampleType p_333591_) {
        LocalSampleLogger localsamplelogger = this.remoteSupportingLoggers.get(p_333591_);
        if (localsamplelogger != null) {
            localsamplelogger.logFullSample(p_333428_);
        }
    }

    public void reset() {
        this.tickTimeLogger.reset();
        this.pingLogger.reset();
        this.bandwidthLogger.reset();
    }

    public LocalSampleLogger getFrameTimeLogger() {
        return this.frameTimeLogger;
    }

    public boolean isRenderFpsCharts() {
        return this.renderFpsCharts;
    }

    public boolean isRenderProfilerChart() {
        return this.renderProfilerChart;
    }

    public void render3dCrosshair(Camera p_407572_) {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate(0.0F, 0.0F, -1.0F);
        matrix4fstack.rotateX(p_407572_.xRot() * (float) (Math.PI / 180.0));
        matrix4fstack.rotateY(p_407572_.yRot() * (float) (Math.PI / 180.0));
        float f = 0.01F * this.minecraft.getWindow().getGuiScale();
        matrix4fstack.scale(-f, f, -f);
        RenderPipeline renderpipeline = RenderPipelines.LINES;
        RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
        GpuTextureView gputextureview = rendertarget.getColorTextureView();
        GpuTextureView gputextureview1 = rendertarget.getDepthTextureView();
        GpuBuffer gpubuffer = this.crosshairIndicies.getBuffer(36);
        GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
            .writeTransform(matrix4fstack, new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "3d crosshair", gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
            renderpass.setPipeline(renderpipeline);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.setVertexBuffer(0, this.crosshairBuffer);
            renderpass.setIndexBuffer(gpubuffer, this.crosshairIndicies.type());
            renderpass.setUniform("DynamicTransforms", gpubufferslice);
            renderpass.drawIndexed(0, 0, 36, 1);
        }

        matrix4fstack.popMatrix();
    }
}
