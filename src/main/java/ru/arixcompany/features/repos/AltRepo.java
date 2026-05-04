package ru.arixcompany.features.repos;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class AltRepo {

    @Getter
    private static final List<Alt> alts = new ArrayList<>();
    @Getter
    @Setter
    private static String lastAlt;

    public static void add(Alt alt) {
        alts.add(alt);
    }

    public static void remove(Alt alt) {
        alts.remove(alt);
    }

    public static void clear() {
        alts.clear();
    }

    @Getter
    @RequiredArgsConstructor
    public static class Alt {
        private final String name;
    }
}