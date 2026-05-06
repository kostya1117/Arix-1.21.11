package ru.arixcompany.features.file.files;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.arixcompany.features.file.ClientFile;
import ru.arixcompany.features.file.exception.FileLoadException;
import ru.arixcompany.features.file.exception.FileSaveException;
import ru.arixcompany.features.repos.AltRepo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AltFile extends ClientFile {

    public AltFile() {
        super("alts");
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {

        if (path == null)
            throw new FileSaveException("Path is null");

        if (!path.exists() && !path.mkdirs())
            throw new FileSaveException("Failed to create directory");

        File file = new File(path, getName() + ".json");

        try {
            List<String> lines = new ArrayList<>();

            if (AltRepo.getLastAlt() != null && !AltRepo.getLastAlt().isBlank()) {
                lines.add("last=" + AltRepo.getLastAlt().trim());
            }

            List<String> names = AltRepo.getAlts().stream()
                    .map(AltRepo.Alt::getName)
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());

            lines.addAll(names);

            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new FileSaveException("Failed to save alts", e);
        }
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        if (path == null) return;

        File file = new File(path, getName() + ".json");

        if (!file.exists()) return;
        try {
            List<String> raw = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            AltRepo.clear();
            String lastAlt = null;

            for (String line : raw) {
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("last=")) {
                    lastAlt = line.substring(5).trim();
                    continue;
                }
                AltRepo.add(new AltRepo.Alt(line));
            }

            AltRepo.setLastAlt(lastAlt);
        } catch (IOException e) {
            throw new FileLoadException("Failed to load alts", e);
        }
    }
}