package rtx.heave.utils.sounds;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.OggAudioStream;
import net.minecraft.sound.SoundCategory;
import rtx.heave.Heave;

public final class DirectSoundPlayer {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Heave-SoundThread");
        t.setDaemon(true);
        return t;
    });

    private static class DecodedSound {
        final AudioFormat format;
        final byte[] pcmData;

        DecodedSound(AudioFormat format, byte[] pcmData) {
            this.format = format;
            this.pcmData = pcmData;
        }
    }

    private static final Map<String, DecodedSound> CACHE = new ConcurrentHashMap<>();

    private DirectSoundPlayer() {}

    public static void play(String resourcePath, float volume) {
        if (resourcePath == null || resourcePath.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        float masterVolume = 1.0f;
        if (mc != null && mc.options != null) {
            masterVolume = mc.options.getSoundVolume(SoundCategory.MASTER);
        }
        float effectiveVolume = volume * masterVolume;
        if (effectiveVolume <= 0.001f) return;

        EXECUTOR.execute(() -> {
            try {
                DecodedSound sound = getOrLoad(resourcePath);
                if (sound == null || sound.pcmData.length == 0) return;

                AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(sound.pcmData),
                    sound.format,
                    sound.pcmData.length / sound.format.getFrameSize()
                );

                Clip clip = AudioSystem.getClip();
                clip.open(ais);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float safeVol = Math.max(0.0001f, Math.min(1.0f, effectiveVolume));
                    float dB = (float) (20.0 * Math.log10(safeVol));
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                }

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });

                clip.start();
            } catch (Throwable t) {
                Heave.LOGGER.debug("[DirectSoundPlayer] Failed to play {}: {}", resourcePath, t.getMessage());
            }
        });
    }

    private static DecodedSound getOrLoad(String resourcePath) {
        return CACHE.computeIfAbsent(resourcePath, path -> {
            String fullPath = path.startsWith("/") ? path : "/" + path;
            try (InputStream is = DirectSoundPlayer.class.getResourceAsStream(fullPath)) {
                if (is == null) {
                    Heave.LOGGER.warn("[DirectSoundPlayer] Resource not found: {}", fullPath);
                    return null;
                }
                try (OggAudioStream oas = new OggAudioStream(is)) {
                    AudioFormat format = oas.getFormat();
                    ByteBuffer buffer = oas.readAll();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    return new DecodedSound(format, data);
                }
            } catch (Exception e) {
                Heave.LOGGER.error("[DirectSoundPlayer] Error decoding OGG {}: {}", fullPath, e.getMessage());
                return null;
            }
        });
    }
}