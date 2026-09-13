package dev.redstones.mediaplayerinfo.impl.win;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;

public class WindowsMediaPlayerInfo {
    public static final WindowsMediaPlayerInfo INSTANCE = new WindowsMediaPlayerInfo();
    private static boolean loaded = false;

    static {
        try {
            File dllFile = Files.createTempDirectory("mediaplayerinfo-").resolve("MediaPlayerInfo.dll").toFile();
            dllFile.deleteOnExit();
            try (InputStream in = WindowsMediaPlayerInfo.class.getResourceAsStream("/mediaplayerinfo/natives/win/MediaPlayerInfo.dll")) {
                if (in != null) {
                    Files.write(dllFile.toPath(), in.readAllBytes());
                    System.load(dllFile.getAbsolutePath());
                    loaded = true;
                }
            }
        } catch (Throwable t) {
            System.err.println("[Heave] Failed to load MediaPlayerInfo.dll: " + t.getMessage());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public native List<WindowsMediaSession> getMediaSessions();
}
