package net.minecraft.client.gui.screens.options;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.stream.Stream;

import com.viaversion.viafabricplus.features.mouse_sensitivity.MouseSensitivity1_13_2;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public class MouseSettingsScreen extends OptionsSubScreen {
    private static final Component TITLE = Component.translatable("options.mouse_settings.title");

    private static OptionInstance<?>[] options(Options p_344227_) {
        return new OptionInstance[]{
            p_344227_.sensitivity(),
            p_344227_.touchscreen(),
            p_344227_.mouseWheelSensitivity(),
            p_344227_.discreteMouseScroll(),
            p_344227_.invertMouseX(),
            p_344227_.invertMouseY(),
            p_344227_.allowCursorChanges()
        };
    }
    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2) && this.list.findOption(this.options.sensitivity()).isHovered()) {
            context.setTooltipForNextFrame(font, Component.nullToEmpty("<=1.13.2 Sensitivity: " + MouseSensitivity1_13_2.get1_13SliderValue(this.options.sensitivity().get().floatValue()).valueInt() + "%"), mouseX, mouseY);
        }
    }

    public MouseSettingsScreen(Screen p_342435_, Options p_344636_) {
        super(p_342435_, p_344636_, TITLE);
    }

    @Override
    protected void addOptions() {
        if (InputConstants.isRawMouseInputSupported()) {
            this.list
                .addSmall(Stream.concat(Arrays.stream(options(this.options)), Stream.of(this.options.rawMouseInput())).toArray(OptionInstance[]::new));
        } else {
            this.list.addSmall(options(this.options));
        }
    }
}
