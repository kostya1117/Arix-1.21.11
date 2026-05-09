package ru.arixcompany.features.module.modules.misc.funtime.autobuy.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

public final class ItemsRegistry {

    private ItemsRegistry() {}

    public static void register(Map<String, ItemTarget> targets) {
        item(targets, "golden_apple", "Золотое яблоко", Items.GOLDEN_APPLE, "гэпл");
        item(targets, "enchanted_golden_apple", "Зач. яблоко", Items.ENCHANTED_GOLDEN_APPLE, "чарка");
        ItemTarget elytra = new ItemTarget(
                "elytra",
                "Элитры",
                new net.minecraft.world.item.ItemStack(Items.ELYTRA),
                null,
                0,
                false,
                true,
                "элитры"
        );

        elytra.setCheckDurability(true);
        elytra.setMinDurabilityPercent(70);
        targets.put("elytra", elytra);

        item(targets, "netherite_ingot", "Незерит слиток", Items.NETHERITE_INGOT, "незеритовый слиток");
        item(targets, "spawner", "Спавнер", Items.SPAWNER, "спавнер");
        item(targets, "diamond", "Алмаз", Items.DIAMOND, "алмаз");
        item(targets, "beacon", "Маяк", Items.BEACON, "маяк");
        item(targets, "sniffer_egg", "Яйцо нюхача", Items.SNIFFER_EGG, "яйцо нюхача");
        item(targets, "trial_key", "Ключ испытаний", Items.TRIAL_KEY, "ключ испытаний");
        item(targets, "dragon_head", "Голова дракона", Items.DRAGON_HEAD, "голова дракона");
        item(targets, "villager_spawn_egg", "Яйцо крестьянина", Items.VILLAGER_SPAWN_EGG, "яйцо жителя");

        // ===== LORE =====
        lore(targets, "dynamite_black", "Динамит BLACK", Items.TNT,
                List.of("Этот динамит взрывается", "в 10 раз сильнее обычного", "и способен взорвать обсидиан"),
                "блэк");

        lore(targets, "dynamite_white", "Динамит WHITE", Items.TNT,
                List.of("Этот динамит взрывается", "в 10 раз сильнее обычного"),
                "вайт");

        lore(targets, "silver", "Серебро", Items.IRON_NUGGET,
                List.of("Это валюта для покупки", "отмычек к тайникам", "у Знахаря (/warp stash)"),
                "серебро");

        lore(targets, "trapka", "Трапка", Items.NETHERITE_SCRAP,
                List.of("Нерушимая клетка"),
                "трапка");

        lore(targets, "sphere_beast", "Сфера Бестии", Items.PLAYER_HEAD,
                List.of("вериная дикая мощь", "Обостряет реакции", "Укрепляя ваше тело."),
                "сфера бестии");

        lore(targets, "sphere_satyr", "Сфера Сатира", Items.PLAYER_HEAD,
                List.of("Шёпот Сатира звучит", "Ускоряя расправу", "Но сковывая прыжок."),
                "сфера сатира");

        lore(targets, "sphere_chaos", "Сфера Хаоса", Items.PLAYER_HEAD,
                List.of("Хаос искажает реальность"),
                "сфера хаоса");

        lore(targets, "sphere_ares", "Сфера Ареса", Items.PLAYER_HEAD,
                List.of("Дух Ареса пылает внутри"),
                "сфера ареса");

        lore(targets, "sphere_hydra", "Сфера Гидры", Items.PLAYER_HEAD,
                List.of("Живучесть темных глубин"),
                "сфера гидры");

        lore(targets, "sphere_titan", "Сфера Титана", Items.PLAYER_HEAD,
                List.of("Мощь Титанов крепка"),
                "сфера титана");

        lore(targets, "talisman_demon", "Талисман Демона", Items.TOTEM_OF_UNDYING,
                List.of("Печать разжигает ярость", "Ускоряя удары сердца", "И силу каждой атаки."),
                "демона");

        lore(targets, "talisman_discord", "Талисман Раздора", Items.TOTEM_OF_UNDYING,
                List.of("Раздор жаждет хаоса", "Даруя безумный темп", "Но разрушая броню."),
                "раздор");

        lore(targets, "talisman_rage", "Талисман Ярости", Items.TOTEM_OF_UNDYING,
                List.of("Чистая, дикая агрессия"),
                "Ярости");

        lore(targets, "talisman_crusher", "Талисман Крушителя", Items.TOTEM_OF_UNDYING,
                List.of("Легендарный символ"),
                "талисман Крушителя");

        lore(targets, "talisman_vixr", "Талисман Вихря", Items.TOTEM_OF_UNDYING,
                List.of("Вихрь не знает покоя"),
                "Вихрь");

        lore(targets, "talisman_tiran", "Талисман Тирана", Items.TOTEM_OF_UNDYING,
                List.of("Тиран подавляет слабых"),
                "Тиран");

        lore(targets, "talisman_mraka", "Талисман Мрака", Items.TOTEM_OF_UNDYING,
                List.of("Мрак сгущается рядом"),
                "Мрака");

        // ===== NAME =====
        name(targets, "potion_assassin", "[★] Зелье Ассасина", Items.SPLASH_POTION, "зелье Ассасина");
        name(targets, "potion_holy_water", "[★] Святая вода", Items.SPLASH_POTION, "Святая вода");
        name(targets, "potion_paladin", "[★] Зелье Палладина", Items.SPLASH_POTION, "зелье Палладина");
        name(targets, "potion_sleeping", "[★] Снотворное", Items.SPLASH_POTION, "Снотворное");
        name(targets, "potion_clapper", "[★] Хлопушка", Items.SPLASH_POTION, "Хлопушка");
        name(targets, "potion_wrath", "[★] Зелье Гнева", Items.SPLASH_POTION, "зелье Гнева");
        name(targets, "potion_radiation", "[★] Зелье Радиации", Items.SPLASH_POTION, "зелье Радиации");
    }

    private static void item(Map<String, ItemTarget> targets,
                             String id, String name, Item item, String search) {

        targets.put(id,
                new ItemTarget(id, name,
                        new net.minecraft.world.item.ItemStack(item),
                        null,
                        0,
                        false,
                        true,
                        search));
    }

    private static void name(Map<String, ItemTarget> targets,
                             String id, String name, Item item, String search) {

        targets.put(id,
                new ItemTarget(id, name,
                        new net.minecraft.world.item.ItemStack(item),
                        null,
                        0,
                        true,
                        false,
                        search));
    }

    private static void lore(Map<String, ItemTarget> targets,
                             String id, String name, Item item,
                             List<String> lore, String search) {

        targets.put(id,
                new ItemTarget(id, name,
                        new net.minecraft.world.item.ItemStack(item),
                        lore,
                        0,
                        false,
                        false,
                        search));
    }
}