package ru.arixcompany.features.file;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.arixcompany.features.file.exception.*;

import java.io.File;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class ClientFile {
    String name;

    public abstract void saveToFile(File path) throws FileSaveException;

    public abstract void loadFromFile(File path) throws FileLoadException;

    public void saveToFile(File path, String fileName) throws FileSaveException {
    }

    public void loadFromFile(File path, String fileName) throws FileLoadException {
    }
}