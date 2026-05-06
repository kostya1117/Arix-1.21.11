package ru.arixcompany.features.repos.sound;

public record SoundId(String namespace, String path) {

    public SoundId {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace is empty");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path is empty");
        }
    }

    public static SoundId of(String namespace, String path) {
        return new SoundId(namespace, path);
    }

    public static SoundId parse(String full) {
        int index = full.indexOf(':');
        if (index == -1) {
            throw new IllegalArgumentException("Invalid id: " + full);
        }
        return new SoundId(full.substring(0, index), full.substring(index + 1));
    }

    public String resourcePath() {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        return "assets/" + namespace + "/" + normalized;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}