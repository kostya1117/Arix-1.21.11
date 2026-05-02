package net.minecraft.client.gui.components;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public interface TabOrderedElement {
    default int getTabOrderGroup() {
        return 0;
    }
}
