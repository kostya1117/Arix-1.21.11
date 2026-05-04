package ru.arixcompany.features.file.files;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import ru.arixcompany.features.file.ClientFile;
import ru.arixcompany.features.file.exception.FileLoadException;
import ru.arixcompany.features.file.exception.FileSaveException;
import ru.arixcompany.features.repos.FriendRepo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FriendFile extends ClientFile {

    public FriendFile() {
        super("friends");
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        if (path != null && !path.exists() && !path.mkdirs()) {
            throw new FileSaveException("Failed to create directory for " + getName());
        }

        File file = new File(path, getName() + ".json");

        try {
            List<String> friendNames = FriendRepo.getFriends().stream()
                    .map(FriendRepo.Friend::getName)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());

            Files.write(file.toPath(), friendNames, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileSaveException(String.format("Failed to save %s to file", getName()), e);
        }
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        File file = new File(path, getName() + ".json");

        if (!file.exists()) {
            return;
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);

            List<FriendRepo.Friend> loadedFriends = content.lines()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .distinct()
                    .map(FriendRepo.Friend::new)
                    .collect(Collectors.toList());

            FriendRepo.clear();
            FriendRepo.getFriends().addAll(loadedFriends);
        } catch (IOException e) {
            throw new FileLoadException(String.format("Failed to load %s from file", getName()), e);
        }
    }
}