package rtx.heave.api.holyworld;

import java.util.Locale;
import net.minecraft.util.math.Vec3d;

public final class HolyWorldEvent {
    public enum Type {
        CARGO("Ценный груз", "a", 0xFF00E5FF, 15 * 60 * 1000L),
        GOLD_RUSH("Золотая лихорадка", "j", 0xFFFFB300, 15 * 60 * 1000L),
        QUARRY("Шахта", "i", 0xFF4ADE80, 20 * 60 * 1000L),
        CUBE("Кубик", "d", 0xFFA855F7, 10 * 60 * 1000L),
        OTHER("Событие сервера", "g", 0xFF38BDF8, 15 * 60 * 1000L);

        public final String defaultName;
        public final String iconChar;
        public final int color;
        public final long defaultDurationMs;

        Type(String defaultName, String iconChar, int color, long defaultDurationMs) {
            this.defaultName = defaultName;
            this.iconChar = iconChar;
            this.color = color;
            this.defaultDurationMs = defaultDurationMs;
        }
    }

    private final String name;
    private final Type type;
    private final String serverId;
    private final String serverName;
    private final int anarchyNumber;
    private final long startEpochMs;
    private final long endEpochMs;
    private final Vec3d coords;
    private final String rarity;
    private final boolean chatVerified;

    public HolyWorldEvent(String name, Type type, String serverId, String serverName, int anarchyNumber,
                           long startEpochMs, long endEpochMs, Vec3d coords, String rarity, boolean chatVerified) {
        this.name = name != null && !name.isBlank() ? name : type.defaultName;
        this.type = type;
        this.serverId = serverId != null ? serverId : "";
        this.serverName = serverName != null && !serverName.isBlank() ? serverName : ("Анархия #" + anarchyNumber);
        this.anarchyNumber = anarchyNumber;
        this.startEpochMs = startEpochMs;
        this.endEpochMs = endEpochMs;
        this.coords = coords;
        this.rarity = rarity != null ? rarity : "";
        this.chatVerified = chatVerified;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerName() {
        return serverName;
    }

    public int getAnarchyNumber() {
        return anarchyNumber;
    }

    public long getStartEpochMs() {
        return startEpochMs;
    }

    public long getEndEpochMs() {
        return endEpochMs;
    }

    public Vec3d getCoords() {
        return coords;
    }

    public String getRarity() {
        return rarity;
    }

    public boolean isChatVerified() {
        return chatVerified;
    }

    public boolean isLive() {
        long now = System.currentTimeMillis();
        return now >= startEpochMs && now < endEpochMs;
    }

    public boolean isUpcoming() {
        return System.currentTimeMillis() < startEpochMs;
    }

    public boolean isFinished() {
        return System.currentTimeMillis() >= endEpochMs;
    }

    public long remainingSeconds() {
        long now = System.currentTimeMillis();
        if (isLive()) {
            return Math.max(0L, (endEpochMs - now) / 1000L);
        }
        if (isUpcoming()) {
            return Math.max(0L, (startEpochMs - now) / 1000L);
        }
        return 0L;
    }

    public String formatRemaining() {
        long sec = remainingSeconds();
        if (sec <= 0L && !isLive()) {
            return "00:00";
        }
        long hours = sec / 3600L;
        long minutes = (sec % 3600L) / 60L;
        long seconds = sec % 60L;

        if (hours > 0) {
            return String.format(Locale.ROOT, "%dч %02dм", hours, minutes);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    public String getIconChar() {
        return type.iconChar;
    }

    public int getColor() {
        return type.color;
    }
}
