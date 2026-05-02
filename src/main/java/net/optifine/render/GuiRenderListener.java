package net.optifine.render;

import net.minecraft.client.gui.render.GuiRenderer;

public interface GuiRenderListener {
    void addToMeshBegin();

    void traverseStratumBegin(int var1);

    void meshToDraw(GuiRenderer.MeshToDraw var1);

    void traverseStratumEnd(int var1);

    void addToMeshEnd();
}
