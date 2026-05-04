package ru.arixcompany;

import de.florianmichael.viamcp.ViaMCP;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import ru.arixcompany.clickgui.Gui;
import ru.arixcompany.features.command.CommandRepo;
import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.EventRepo;
import ru.arixcompany.features.event.player.EventKey;
import ru.arixcompany.features.file.Directories;
import ru.arixcompany.features.file.FileController;
import ru.arixcompany.features.file.FileRepo;
import ru.arixcompany.features.file.exception.FileProcessingException;
import ru.arixcompany.features.module.ModuleRepo;
import ru.arixcompany.features.module.Theme;
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
    public static final String gameDirectoryPath = new File(Minecraft.getInstance().gameDirectory, "arix").getAbsolutePath();
    public boolean initialized = false;
    @Setter
    @Getter
    private Theme currentTheme = Theme.PURPLE;

    @NonFinal
    ModuleRepo moduleRepo;
    @NonFinal
    FileController fileController;
    @NonFinal
    FileRepo fileRepo;
    @NonFinal
    CommandRepo commandRepo;

    public Arix(){
        instance = this;
        ViaMCP.create();
        ViaMCP.INSTANCE.initAsyncSlider();
        FontManager.init();
        RoundRectShader.init();
        IASMinecraft.init();
        moduleRepo = new ModuleRepo();
        commandRepo = new CommandRepo();
        commandRepo.setup();

        initFileManager();

        EventRepo.register(this);
        initialized = true;
        Runtime.getRuntime().addShutdownHook(new Thread(this::onExit));
    }

    void onExit() {
        IAS.close();

        if (isInitialized()) {
            try {
                fileController.saveFiles();
            } catch (FileProcessingException ignored) {
            } finally {
                fileController.stopAutoSave();
            }
        }
    }

    public void initFileManager() {
        Directories.createDirectories(Directories.directory, Directories.filesDirectory,
                Directories.configDirectory);

        fileRepo = new FileRepo();
        fileRepo.setup();

        fileController = new FileController(fileRepo.getClientFiles(), Directories.filesDirectory,
                Directories.configDirectory);
        try {
            fileController.loadFiles();
        } catch (FileProcessingException ignored) {
        }
    }

    @EventHandler
    public void onKey(EventKey e) {
        if (e.getAction() == 1 && e.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT && mc.screen == null) {
            mc.setScreen(new Gui());
        }
    }
}
