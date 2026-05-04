package ru.arixcompany.features.file;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import ru.arixcompany.features.file.files.FriendFile;
import ru.arixcompany.features.file.files.ModuleFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileRepo {
    List<ClientFile> clientFiles = new ArrayList<>();


    public void setup() {
        register(new FriendFile(),
                new ModuleFile());
    }

    public void register(ClientFile... clientFIle) {
        clientFiles.addAll(List.of(clientFIle));
    }
}