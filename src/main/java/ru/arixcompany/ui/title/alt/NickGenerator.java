package ru.arixcompany.ui.title.alt;

import java.util.Random;

public class NickGenerator {

    private static final Random random = new Random();

    private static final String[] prefixes = {
            "Neo","Zay","Vex","Kiro","Blaze","Raze","Cry","Xeno","Kyro","Jax",
            "Aero","Nix","Zoro","Lynx","Kane","Mori","Zane","Rey","Noir","Axel",
            "Dark","Light","Shadow","Storm","Flame","Frost","Knight","Hunter",
            "Nova","Drift","Clutch","Venom","Strike","Pulse","Rogue","Snare","Viper"
    };

    private static final String[] cores = {
            "Shadow","Ghost","Storm","Flame","Frost","Knight","Hunter","Nova",
            "Drift","Clutch","Venom","Strike","Pulse","Rogue","Snare","Viper",
            "Blaze","Raze","Cry","Xeno","Kyro","Jax","Aero","Nix","Zoro","Lynx"
    };

    private static final String[] endings = {
            "x","z","y","ix","ex","er","or","on","en","yy","ez"
    };

    private static final String[] numbers = {
            "7","9","11","17","21","24","47","77","99",
            "01","02","05","67","52","777","999","1337","2024","2025","2007","2015",
            "1488","228"
    };

    private static final String[] suffixes = {
            "pro","gg","lol","yt","tv","xd","fps","god","btw","prod"
    };

    private static final String vowels = "aeiouy";
    private static final String consonants = "bcdfghjklmnpqrstvwxz";

    private static final String[][] bigrams = {
            {"a","n"},{"a","r"},{"a","x"},{"a","z"},
            {"e","x"},{"e","r"},{"e","n"},{"e","z"},
            {"o","n"},{"o","r"},{"o","x"},
            {"i","x"},{"i","z"},{"i","n"},
            {"y","n"},{"y","x"},
            {"v","e"},{"v","o"},{"v","a"},
            {"k","a"},{"k","e"},{"k","i"},
            {"z","e"},{"z","o"},{"z","y"},
            {"r","a"},{"r","e"},{"r","o"},
            {"x","o"},{"x","e"},{"x","a"}
    };


    private static String generateAINick() {
        int len = 5 + random.nextInt(6);
        StringBuilder sb = new StringBuilder();
        boolean vowelNext = random.nextBoolean();

        for (int i = 0; i < len; i++) {
            if (vowelNext) {
                sb.append(vowels.charAt(random.nextInt(vowels.length())));
            } else {
                sb.append(consonants.charAt(random.nextInt(consonants.length())));
            }
            if (random.nextDouble() > 0.3) {
                vowelNext = !vowelNext;
            }
        }
        return sb.toString();
    }

    private static String generateNeuroNick() {
        int length = 5 + random.nextInt(6);
        StringBuilder sb = new StringBuilder();

        String current = bigrams[random.nextInt(bigrams.length)][0];
        sb.append(current);

        for (int i = 1; i < length; i++) {
            String next = null;

            for (String[] pair : bigrams) {
                if (pair[0].equals(current) && random.nextDouble() < 0.35) {
                    next = pair[1];
                    break;
                }
            }

            if (next == null) {
                next = String.valueOf(consonants.charAt(random.nextInt(consonants.length())));
            }

            sb.append(next);
            current = next;
        }

        return sb.toString();
    }

    private static String humanize(String nick) {

        if (random.nextDouble() < 0.4) {
            nick = nick.substring(0,1).toUpperCase() + nick.substring(1);
        }

        if (random.nextDouble() < 0.35) {
            nick += numbers[random.nextInt(numbers.length)];
        }

        if (random.nextDouble() < 0.15) {
            nick = "x" + nick;
        }

        if (random.nextDouble() < 0.15) {
            nick += nick.charAt(nick.length()-1);
        }

        if (random.nextDouble() < 0.2) {
            nick = nick.replace("o","0")
                    .replace("e","3");
        }

        return nick;
    }

    private static String esportStyle(String input) {
        input = input.replace("o","0")
                .replace("e","3")
                .replace("a","4");

        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (random.nextDouble() < 0.3) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String random(String[] arr) {
        return arr[random.nextInt(arr.length)];
    }

    private static String trim(String nick) {
        if (nick.length() > 16) {
            return nick.substring(0, 16);
        }
        if (nick.length() < 3) {
            return nick + random(endings);
        }
        return nick;
    }

    public static String generateRandomName() {

        int pattern = random.nextInt(18);
        String nick;

        switch (pattern) {

            case 0:
                nick = random(prefixes) + random(cores);
                break;

            case 1:
                nick = random(prefixes).toLowerCase() + random(endings);
                break;

            case 2:
                nick = random(prefixes) + random(numbers);
                break;

            case 3:
                nick = random(cores).toLowerCase() + random(endings) + random(numbers);
                break;

            case 4:
                nick = "x" + random(prefixes);
                break;

            case 5:
                nick = random(prefixes) + "_" + random(cores);
                break;

            case 6:
                nick = random(prefixes).toLowerCase() + random(endings) + random(suffixes);
                break;

            case 7:
                nick = random(prefixes) + random(cores) + random(numbers);
                break;

            case 8:
                nick = generateAINick();
                break;

            case 9:
                nick = generateAINick() + random(numbers);
                break;

            case 10:
                nick = "x" + generateAINick();
                break;

            case 11:
                nick = esportStyle(generateAINick());
                break;

            case 12:
                nick = humanize(generateNeuroNick());
                break;

            case 13:
                nick = humanize(random(prefixes) + generateNeuroNick());
                break;

            case 14:
                nick = generateNeuroNick();
                break;

            case 15:
                nick = generateNeuroNick() + random(suffixes);
                break;

            case 16:
                nick = esportStyle(random(prefixes) + random(endings));
                break;

            default:
                nick = humanize(generateAINick());
                break;
        }

        return trim(nick);
    }
}