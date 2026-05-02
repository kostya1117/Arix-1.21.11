package ru.arixcompany;

import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.vidtu.ias.IAS;
import ru.vidtu.ias.IASMinecraft;

import java.io.File;

public class Arix {

    public static final File gameDirectory = new File(Minecraft.getInstance().gameDirectory, "arix");

    public Arix(){
        init();
        Runtime.getRuntime().addShutdownHook(new Thread(this::onExit));
    }

    void onExit() {
        IAS.close();
    }

    void init(){
        IASMinecraft.init();
        System.out.print("IAS init");
    }
}
