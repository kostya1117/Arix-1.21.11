package ru.arixcompany.features.file;

import lombok.experimental.UtilityClass;
import ru.arixcompany.Arix;
import ru.arixcompany.utils.IMinecraft;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class Directories implements IMinecraft {
    public static String directoryPathConfig = Arix.gameDirectoryPath + "/configs";
    public static String filesDirectoryPath = Arix.gameDirectoryPath + "/files";

    public static File directory = new File(Arix.gameDirectoryPath);
    public static File configDirectory = new File(directoryPathConfig);
    public static File filesDirectory = new File(filesDirectoryPath);

    public void createDirectories(File... files) {
        List<String> createdDirectories = new ArrayList<>();

        Arrays.stream(files)
                .filter(file -> !file.exists())
                .forEach(file -> {
                    if (file.mkdirs()) {
                        createdDirectories.add(file.getName());
                    }
                });
    }
}
