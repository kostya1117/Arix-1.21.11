package ru.arixcompany;

import de.florianmichael.viamcp.ViaMCP;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.clickgui.Gui;
import ru.arixcompany.event.EventHandler;
import ru.arixcompany.event.EventRepo;
import ru.arixcompany.event.player.EventKey;
import ru.arixcompany.module.ModuleRepo;
import ru.arixcompany.module.Theme;
import ru.arixcompany.utils.IMinecraft;
import ru.arixcompany.utils.render.RoundRectShader;
import ru.arixcompany.utils.render.font.FontManager;
import ru.vidtu.ias.IAS;
import ru.vidtu.ias.IASMinecraft;

import java.io.File;

@Getter
public class Arix implements IMinecraft {
    @Getter
    public static Arix instance;
    public static final File gameDirectory = new File(Minecraft.getInstance().gameDirectory, "arix");
    public boolean initialized = false;
    @Setter
    @Getter
    private Theme currentTheme = Theme.PURPLE;

    @NonFinal
    ModuleRepo moduleRepo;

    public Arix(){
        instance = this;
        ViaMCP.create();
        ViaMCP.INSTANCE.initAsyncSlider();
        FontManager.init();
        RoundRectShader.init();
        IASMinecraft.init();
        moduleRepo = new ModuleRepo();

        EventRepo.register(this);
        initialized = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::onExit));
    }

    void onExit() {
        IAS.close();
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (e.getAction() == 1 && e.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT && mc.screen == null) {
            mc.setScreen(new Gui());
        }
    }
}
