package ru.arixcompany.features.module.modules.misc;

import ru.arixcompany.features.event.EventHandler;
import ru.arixcompany.features.event.player.EventAttack;
import ru.arixcompany.features.module.Category;
import ru.arixcompany.features.module.Module;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.features.module.setting.implement.ValueSetting;
import ru.arixcompany.features.repos.SoundRepo;

import java.util.concurrent.ThreadLocalRandom;

public class ClientSounds extends Module {

    public ValueSetting volumeSetting = new ValueSetting("Громкость")
            .range(0.0f, 1.0f)
            .setValue(1.0f);

    public ValueSetting pitchSetting = new ValueSetting("Тон")
            .range(0.5f, 2.0f)
            .setValue(1.0f);

    public SelectSetting hitSound = new SelectSetting("Звуки ударов")
            .value("Без","Металл","Пузырь","Стоны");

    public ClientSounds() {
        super("ClientSounds", Category.Misc);
        setup(volumeSetting, pitchSetting, hitSound);
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        if (hitSound.isSelected("Без")) return;
        String sound = getSound();

        float pitch = getPitch();
        float stonp;

        if (hitSound.isSelected("Стоны")) {
            stonp = ThreadLocalRandom.current()
                    .nextFloat(1f, 1.3F);

            SoundRepo.play(sound, getVolume(), stonp);
        }

        if (!hitSound.isSelected("Стоны")) {
            SoundRepo.play(sound, getVolume(), pitch);
        }
    }

    public String getSound() {
        return switch (hitSound.getSelected()) {
            case "Металл" -> "hit/metallic.wav";
            case "Пузырь" -> "hit/buble.wav";
            case "Стоны" -> "hit/ston.wav";
            default -> "";
        };
    }

    public float getVolume() {
        return volumeSetting.getValue();
    }

    public float getPitch() {
        return pitchSetting.getValue();
    }
}