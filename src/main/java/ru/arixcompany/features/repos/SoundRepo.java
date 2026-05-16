package ru.arixcompany.features.repos;

import ru.arixcompany.Arix;
import ru.arixcompany.features.module.modules.misc.ClientSounds;
import ru.arixcompany.utils.MessageSender;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public final class SoundRepo {
    private static final String SOUND_PATH = "assets/arix/sounds/";

    public static void playOn() {
        play("enable.wav");
    }
    public static void playOff() {
        play("disable.wav");
    }
    public static void playButton() {
        play("button.wav");
    }

    public static void play(String fileName) {
        play(fileName, getClientVolume(), getClientPitch());
    }

    public static void play(String fileName, float volume, float pitch) {
        if (volume <= 0.0f) return;
        if (pitch <= 0.0f) pitch = 1.0f;

        float finalPitch = pitch;

        new Thread(() -> playInternal(fileName, volume, finalPitch), "Sound-" + fileName).start();
    }

    private static void playInternal(String fileName, float volume, float pitch) {
        String path = SOUND_PATH + fileName;

        try (InputStream resource = SoundRepo.class.getClassLoader().getResourceAsStream(path)) {
            if (resource == null) {
                MessageSender.error("Звук не найден: " + path);
                return;
            }

            try (BufferedInputStream buffered = new BufferedInputStream(resource);
                 AudioInputStream audioStream = AudioSystem.getAudioInputStream(buffered)) {

                AudioFormat baseFormat = audioStream.getFormat();

                byte[] pcmData = audioStream.readAllBytes();

                AudioFormat playbackFormat = new AudioFormat(
                        baseFormat.getEncoding(),
                        baseFormat.getSampleRate() * pitch,
                        baseFormat.getSampleSizeInBits(),
                        baseFormat.getChannels(),
                        baseFormat.getFrameSize(),
                        baseFormat.getFrameRate() * pitch,
                        baseFormat.isBigEndian()
                );

                SourceDataLine line = null;

                try {
                    line = AudioSystem.getSourceDataLine(playbackFormat);
                    line.open(playbackFormat);
                } catch (Exception e) {
                    if (line != null) {
                        line.close();
                    }
                    line = AudioSystem.getSourceDataLine(baseFormat);
                    line.open(baseFormat);
                }

                try {
                    applyVolume(line, volume);
                    line.start();
                    line.write(pcmData, 0, pcmData.length);
                    line.drain();
                    line.stop();
                } finally {
                    line.close();
                }
            }
        } catch (Exception e) {
            MessageSender.error("Ошибка при воспроизведении звука : " + fileName);
            e.printStackTrace();
        }
    }

    private static void applyVolume(SourceDataLine line, float volume) {
        volume = Math.max(0.0f, Math.min(1.0f, volume));

        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);

                if (volume <= 0.0f) {
                    gain.setValue(gain.getMinimum());
                    return;
                }

                float db = (float) (20.0 * Math.log10(volume));
                db = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db));
                gain.setValue(db);
            }
        } catch (Exception ignored) {
        }
    }

    public static float getClientVolume() {
        ClientSounds clientSounds = Arix.getInstance().getModuleRepo().getModule(ClientSounds.class);
        if (clientSounds == null || !clientSounds.isState()) {
            return 0.0f;
        }
        return clientSounds.getVolume();
    }

    public static float getClientPitch() {
        ClientSounds clientSounds = Arix.getInstance().getModuleRepo().getModule(ClientSounds.class);
        if (clientSounds == null || !clientSounds.isState()) {
            return 1.0f;
        }
        return clientSounds.getPitch();
    }
}