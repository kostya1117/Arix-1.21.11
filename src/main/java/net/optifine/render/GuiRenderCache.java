package net.optifine.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.optifine.util.HashMapMap;

public class GuiRenderCache implements GuiRenderListener {
    private GuiRenderer guiRenderer;
    private GuiRenderState renderState;
    private long cacheTimeMs;
    private long updateTimeMs;
    private int cacheStratum;
    private boolean renderCached;
    private boolean addToMesh;
    private int stratum;
    private List<GuiRenderer.MeshToDraw> meshes = new ArrayList<>();
    private static HashMapMap<VertexFormat.Mode, VertexFormat, BufferBuilder> mapBufferBuilders = new HashMapMap<>();

    public GuiRenderCache(GuiRenderer guiRenderer, long cacheTimeMs) {
        this.guiRenderer = guiRenderer;
        this.renderState = guiRenderer.getRenderState();
        this.cacheTimeMs = cacheTimeMs;
        this.cacheStratum = -1;
    }

    public boolean drawCached(GuiGraphics graphicsIn) {
        if (!this.checkGraphics(graphicsIn)) {
            return false;
        }

        this.cacheStratum = this.renderState.getStratum();
        this.renderState.addFrameListener(this);
        this.renderCached = System.currentTimeMillis() < this.updateTimeMs;
        return this.renderCached;
    }

    public void startRender(GuiGraphics graphicsIn) {
        if (this.checkGraphics(graphicsIn)) {
            for (GuiRenderer.MeshToDraw guirenderer$meshtodraw : this.meshes) {
                guirenderer$meshtodraw.mesh().setPersistent(false);
                guirenderer$meshtodraw.close();
            }

            this.meshes.clear();

            for (BufferBuilder bufferbuilder : mapBufferBuilders.values()) {
                bufferbuilder.clear();
            }
        }
    }

    public void stopRender(GuiGraphics graphicsIn) {
        if (this.checkGraphics(graphicsIn)) {
            this.updateTimeMs = System.currentTimeMillis() + this.cacheTimeMs;
        }
    }

    private boolean checkGraphics(GuiGraphics graphicsIn) {
        return graphicsIn.getGuiRenderState() == this.renderState;
    }

    @Override
    public void addToMeshBegin() {
        this.addToMesh = true;
    }

    @Override
    public void traverseStratumBegin(int stratumIn) {
        if (this.addToMesh) {
            if (stratumIn == this.cacheStratum) {
                this.guiRenderer.flushMesh();
            }

            this.stratum = stratumIn;
            if (this.stratum == this.cacheStratum) {
                if (this.renderCached) {
                    this.guiRenderer.addMeshesToDraw(this.meshes);
                }
            }
        }
    }

    @Override
    public void meshToDraw(GuiRenderer.MeshToDraw mesh) {
        if (this.addToMesh) {
            if (this.stratum == this.cacheStratum) {
                if (!this.renderCached) {
                    MeshData meshdata = mesh.mesh();
                    MeshData.DrawState meshdata$drawstate = meshdata.drawState();
                    ByteBuffer bytebuffer = meshdata.vertexBuffer();
                    BufferBuilder bufferbuilder = mapBufferBuilders.computeIfAbsent(
                        meshdata$drawstate.mode(),
                        meshdata$drawstate.format(),
                        (modeIn, formatIn) -> new BufferBuilder(new ByteBufferBuilder(10000), modeIn, formatIn)
                    );
                    bufferbuilder.putBulkData(bytebuffer);
                    bytebuffer.rewind();
                    MeshData meshdata1 = bufferbuilder.build();
                    meshdata1.setPersistent(true);
                    GuiRenderer.MeshToDraw guirenderer$meshtodraw = new GuiRenderer.MeshToDraw(meshdata1, mesh.pipeline(), mesh.textureSetup(), mesh.scissorArea());
                    this.meshes.add(guirenderer$meshtodraw);
                }
            }
        }
    }

    @Override
    public void traverseStratumEnd(int stratumIn) {
        if (this.addToMesh) {
            if (stratumIn == this.cacheStratum) {
                this.guiRenderer.flushMesh();
            }

            this.stratum = -1;
        }
    }

    @Override
    public void addToMeshEnd() {
        this.addToMesh = false;
    }
}
