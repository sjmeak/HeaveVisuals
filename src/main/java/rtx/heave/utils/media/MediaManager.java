package rtx.heave.utils.media;

import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.impl.win.WindowsMediaPlayerInfo;
import dev.redstones.mediaplayerinfo.impl.win.WindowsMediaSession;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MediaManager {
    private static final MediaManager INSTANCE = new MediaManager();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Heave-MediaManager");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean updating = new AtomicBoolean(false);

    private volatile String trackTitle = "";
    private volatile String artist = "";
    private volatile long positionMs = 0;
    private volatile long durationMs = 0;
    private volatile boolean playing = false;
    private volatile boolean active = false;
    private volatile BufferedImage artwork = null;
    private volatile WindowsMediaSession currentSession = null;
    private volatile long lastPollTime = 0;

    private MediaManager() {
        this.startWorker();
    }

    public static MediaManager get() {
        return INSTANCE;
    }

    private void startWorker() {
        Thread pollThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(300);
                    this.poll();
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
        }, "Heave-Media-Poller");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    public void poll() {
        if (!WindowsMediaPlayerInfo.isLoaded()) {
            return;
        }
        if (!this.updating.compareAndSet(false, true)) {
            return;
        }
        this.executor.execute(() -> {
            try {
                List<WindowsMediaSession> sessions = WindowsMediaPlayerInfo.INSTANCE.getMediaSessions();
                WindowsMediaSession activeSession = null;
                if (sessions != null && !sessions.isEmpty()) {
                    for (WindowsMediaSession session : sessions) {
                        if (session == null) continue;
                        MediaInfo info = session.getMedia();
                        if (info != null && info.getPlaying()) {
                            activeSession = session;
                            break;
                        }
                    }
                    if (activeSession == null) {
                        activeSession = sessions.get(0);
                    }
                }

                if (activeSession != null && activeSession.getMedia() != null) {
                    MediaInfo info = activeSession.getMedia();
                    String t = info.getTitle();
                    String a = info.getArtist();
                    if ((t != null && !t.isBlank()) || (a != null && !a.isBlank())) {
                        this.trackTitle = t != null ? t.trim() : "Неизвестный трек";
                        this.artist = a != null ? a.trim() : "Неизвестный исполнитель";
                        this.positionMs = Math.max(0, info.getPosition());
                        this.durationMs = Math.max(0, info.getDuration());
                        this.playing = info.getPlaying();
                        this.active = true;
                        this.currentSession = activeSession;
                        this.artwork = info.getArtwork();
                        this.lastPollTime = System.currentTimeMillis();
                        return;
                    }
                }

                this.active = false;
                this.playing = false;
                this.currentSession = null;
            } catch (Throwable t) {
                this.active = false;
                this.currentSession = null;
            } finally {
                this.updating.set(false);
            }
        });
    }

    public boolean isActive() {
        return this.active;
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public String getTrackTitle() {
        return this.active ? this.trackTitle : "Трек не воспроизводится";
    }

    public String getArtist() {
        return this.active ? this.artist : "Ожидание медиаплеера...";
    }

    public long getPositionMs() {
        if (!this.active) return 0;
        if (this.playing && this.durationMs > 0) {
            long elapsed = System.currentTimeMillis() - this.lastPollTime;
            return Math.min(this.durationMs, this.positionMs + elapsed);
        }
        return this.positionMs;
    }

    public long getDurationMs() {
        return this.active ? this.durationMs : 0;
    }

    public float getProgress() {
        long d = this.getDurationMs();
        if (d <= 0) return 0.0f;
        return Math.min(1.0f, Math.max(0.0f, (float) this.getPositionMs() / (float) d));
    }

    public String getPositionText() {
        return formatTime(this.getPositionMs());
    }

    public String getDurationText() {
        return formatTime(this.getDurationMs());
    }

    public BufferedImage getArtwork() {
        return this.artwork;
    }

    public void playPause() {
        WindowsMediaSession session = this.currentSession;
        if (session != null) {
            this.executor.execute(() -> {
                try {
                    session.playPause();
                } catch (Throwable ignored) {}
            });
        }
    }

    public void next() {
        WindowsMediaSession session = this.currentSession;
        if (session != null) {
            this.executor.execute(() -> {
                try {
                    session.next();
                } catch (Throwable ignored) {}
            });
        }
    }

    public void previous() {
        WindowsMediaSession session = this.currentSession;
        if (session != null) {
            this.executor.execute(() -> {
                try {
                    session.previous();
                } catch (Throwable ignored) {}
            });
        }
    }

    private static String formatTime(long ms) {
        long totalSec = Math.max(0, ms / 1000);
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }
}
