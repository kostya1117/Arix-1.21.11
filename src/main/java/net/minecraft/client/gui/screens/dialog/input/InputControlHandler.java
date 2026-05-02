package net.minecraft.client.gui.screens.dialog.input;

import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface

public interface InputControlHandler<T extends InputControl> {
    void addControl(T p_409632_, Screen p_410085_, InputControlHandler.Output p_406349_);

    @FunctionalInterface
    
    interface Output {
        void accept(LayoutElement p_409024_, Action.ValueGetter p_409247_);
    }
}
