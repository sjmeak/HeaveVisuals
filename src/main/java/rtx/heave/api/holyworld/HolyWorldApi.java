package rtx.heave.api.holyworld;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import rtx.heave.utils.sounds.SoundManager;

public final class HolyWorldApi {
    private static final String BASE_URL = "https://api.holyworld.me/v1";
    private static HolyWorldApi instance;

    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;

    public record ServerInfo(String id, String name, int anarchyNumber) {}
    public record CoinsTradeInfo(double buyPrice, double sellPrice, double volume, long lastTradeTime) {}

    private final List<ServerInfo> servers = new CopyOnWriteArrayList<>();
    private final Map<String, ServerInfo> serverMap = new ConcurrentHashMap<>();
    private volatile CoinsTradeInfo coinsTrade = new CoinsTradeInfo(0.0, 0.0, 0.0, 0L);
    private final List<HolyWorldEvent> apiEvents = new CopyOnWriteArrayList<>();
    private final List<HolyWorldEvent> chatEvents = new CopyOnWriteArrayList<>();
    private final Map<String, Long> eventFirstSeen = new ConcurrentHashMap<>();

    private volatile boolean isAvailable = false;
    private volatile long lastUpdateTime = 0L;

    private HolyWorldApi() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HolyWorld-Api-Worker");
            t.setDaemon(true);
            return t;
        });

        // Generate initial fallback servers so GUI is never empty before network finishes
        populateDefaultServers();

        // Immediate background refresh & scheduled poll every 20 seconds
        this.scheduler.scheduleAtFixedRate(this::refreshAll, 0, 20, TimeUnit.SECONDS);
    }

    public static synchronized HolyWorldApi getInstance() {
        if (instance == null) {
            instance = new HolyWorldApi();
        }
        return instance;
    }

    public void shutdown() {
        if (this.scheduler != null) {
            this.scheduler.shutdownNow();
        }
    }

    private void populateDefaultServers() {
        if (!servers.isEmpty()) return;
        List<ServerInfo> list = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            list.add(new ServerInfo("LITE_ANARCHY_" + i, "СолоЛайт #" + i, i));
        }
        for (int i = 13; i <= 26; i++) {
            list.add(new ServerInfo("LITE_ANARCHY_" + i, "ДуоЛайт #" + i, i));
        }
        for (int i = 27; i <= 39; i++) {
            list.add(new ServerInfo("LITE_ANARCHY_" + i, "ТриоЛайт #" + i, i));
        }
        for (int i = 40; i <= 51; i++) {
            list.add(new ServerInfo("LITE_ANARCHY_" + i, "КланЛайт #" + i, i));
        }
        list.add(new ServerInfo("LITE_NEW_ANARCHY_1", "Лайт (1.20) #1", 101));
        list.add(new ServerInfo("LITE_NEW_ANARCHY_2", "Лайт (1.20) #2", 102));
        list.add(new ServerInfo("LITE_NEW_ANARCHY_3", "Лайт (1.20) #3", 103));

        servers.addAll(list);
        for (ServerInfo s : list) {
            serverMap.put(s.id(), s);
        }
    }

    public void refreshAll() {
        try {
            fetchServers();
            fetchEvents();
            fetchCoinsTrades();
            this.isAvailable = true;
            this.lastUpdateTime = System.currentTimeMillis();
        } catch (Throwable t) {
            // Keep previous data on network failure
        }
    }

    private void fetchServers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/servers"))
                .header("User-Agent", "HeaveVisuals/1.0")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonElement root = JsonParser.parseString(response.body());
                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    List<ServerInfo> list = new ArrayList<>();
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        String id = entry.getKey();
                        String displayName = entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : id;
                        int number = parseAnarchyNumber(id, displayName);
                        list.add(new ServerInfo(id, displayName, number));
                    }
                    list.sort(Comparator.comparingInt(ServerInfo::anarchyNumber));
                    if (!list.isEmpty()) {
                        this.servers.clear();
                        this.servers.addAll(list);
                        this.serverMap.clear();
                        for (ServerInfo s : list) {
                            this.serverMap.put(s.id(), s);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void fetchEvents() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/events"))
                .header("User-Agent", "HeaveVisuals/1.0")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200) {
                JsonElement root = JsonParser.parseString(response.body());
                List<HolyWorldEvent> list = new ArrayList<>();
                long now = System.currentTimeMillis();
                Set<String> activeInstances = new HashSet<>();

                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                        String serverId = entry.getKey();
                        ServerInfo server = serverMap.get(serverId);
                        int anarchyNum = server != null ? server.anarchyNumber() : parseAnarchyNumber(serverId, "");
                        String serverName = server != null ? server.name() : ("Анархия #" + (anarchyNum > 0 ? anarchyNum : serverId));

                        if (entry.getValue().isJsonArray()) {
                            JsonArray arr = entry.getValue().getAsJsonArray();
                            for (JsonElement itemEl : arr) {
                                if (!itemEl.isJsonObject()) continue;
                                JsonObject itemObj = itemEl.getAsJsonObject();
                                String instanceId = itemObj.has("instanceId") ? itemObj.get("instanceId").getAsString() : (serverId + "_" + itemObj.hashCode());
                                activeInstances.add(instanceId);

                                String rawId = itemObj.has("id") ? itemObj.get("id").getAsString() : "EVENT";
                                String displayName = "";
                                String rarity = "";

                                if (itemObj.has("metadata") && itemObj.get("metadata").isJsonObject()) {
                                    JsonObject meta = itemObj.getAsJsonObject("metadata");
                                    if (meta.has("displayName")) {
                                        displayName = meta.get("displayName").getAsString();
                                    }
                                    if (meta.has("rare")) {
                                        rarity = meta.get("rare").getAsString();
                                    }
                                }

                                HolyWorldEvent.Type type = mapApiEventType(rawId, displayName);
                                if (displayName.isBlank() || displayName.equals(rawId)) {
                                    displayName = type.defaultName;
                                }

                                long firstSeen = eventFirstSeen.computeIfAbsent(instanceId, k -> now);
                                long endEpoch = firstSeen + type.defaultDurationMs;

                                list.add(new HolyWorldEvent(
                                    displayName,
                                    type,
                                    serverId,
                                    serverName,
                                    anarchyNum,
                                    firstSeen,
                                    endEpoch,
                                    null,
                                    cleanRarity(rarity),
                                    false
                                ));
                            }
                        }
                    }
                }
                eventFirstSeen.keySet().retainAll(activeInstances);
                this.apiEvents.clear();
                this.apiEvents.addAll(list);
            }
        } catch (Throwable ignored) {}
    }

    private void fetchCoinsTrades() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/coins-trades"))
                .header("User-Agent", "HeaveVisuals/1.0")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonElement root = JsonParser.parseString(response.body());
                if (root.isJsonObject()) {
                    JsonObject obj = root.getAsJsonObject();
                    double buy = obj.has("buy") ? obj.get("buy").getAsDouble() : 0.0;
                    double sell = obj.has("sell") ? obj.get("sell").getAsDouble() : 0.0;
                    double volume = obj.has("volume") ? obj.get("volume").getAsDouble() : 0.0;
                    long time = obj.has("time") ? obj.get("time").getAsLong() : System.currentTimeMillis();
                    this.coinsTrade = new CoinsTradeInfo(buy, sell, volume, time);
                }
            }
        } catch (Throwable ignored) {}
    }

    public void notifyLiveChatEvent(String eventName, Vec3d coords, long durationMs) {
        long now = System.currentTimeMillis();
        long start = (durationMs > 0) ? (now + durationMs) : now;
        long end = start + 15 * 60 * 1000L;

        int currentAnarchy = detectCurrentAnarchy();
        ServerInfo sInfo = findServerByAnarchy(currentAnarchy);
        String sName = sInfo != null ? sInfo.name() : ("Анархия #" + currentAnarchy);
        String sId = sInfo != null ? sInfo.id() : ("LITE_ANARCHY_" + currentAnarchy);

        HolyWorldEvent.Type type = mapApiEventType(eventName, eventName);

        // Remove old chat events on this server for the same type
        chatEvents.removeIf(e -> e.getAnarchyNumber() == currentAnarchy && e.getType() == type);

        HolyWorldEvent event = new HolyWorldEvent(
            eventName,
            type,
            sId,
            sName,
            currentAnarchy,
            start,
            end,
            coords,
            "Чат",
            true
        );
        chatEvents.add(0, event);

        // Limit cached chat events
        if (chatEvents.size() > 20) {
            chatEvents.remove(chatEvents.size() - 1);
        }
    }

    public List<HolyWorldEvent> getTrackedEvents(String filterType, String search) {
        long now = System.currentTimeMillis();
        chatEvents.removeIf(HolyWorldEvent::isFinished);

        List<HolyWorldEvent> result = new ArrayList<>();

        // 1. Add chat verified live events first
        result.addAll(chatEvents);

        // 2. Add API events from HolyWorld /events
        result.addAll(apiEvents);

        // Apply type filter
        if (filterType != null && !filterType.equalsIgnoreCase("ALL")) {
            result.removeIf(e -> !e.getType().name().equalsIgnoreCase(filterType));
        }

        // Apply search query (filter by anarchy number or server name or event name)
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase(Locale.ROOT);
            result.removeIf(e -> {
                String numStr = String.valueOf(e.getAnarchyNumber());
                return !e.getName().toLowerCase(Locale.ROOT).contains(q)
                    && !e.getServerName().toLowerCase(Locale.ROOT).contains(q)
                    && !numStr.contains(q);
            });
        }

        // Sort: Active events first (sorted by remaining duration), then upcoming events by start time
        result.sort((a, b) -> {
            boolean aLive = a.isLive();
            boolean bLive = b.isLive();
            if (aLive != bLive) {
                return aLive ? -1 : 1;
            }
            if (a.isChatVerified() != b.isChatVerified()) {
                return a.isChatVerified() ? -1 : 1;
            }
            return Long.compare(a.remainingSeconds(), b.remainingSeconds());
        });

        return result;
    }

    public static void joinAnarchy(int anarchyNumber) {
        if (anarchyNumber <= 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        String cmd = "an " + anarchyNumber;

        if (mc.player != null && mc.player.networkHandler != null) {
            mc.player.networkHandler.sendChatCommand(cmd);
            mc.player.sendMessage(Text.literal("§6[HolyWorld] §fПодключение к §e" + cmd + "§f..."), false);
        }

        if (mc.keyboard != null) {
            mc.keyboard.setClipboard("/" + cmd);
        }

        if (mc.getSoundManager() != null) {
            SoundManager.playSound(SoundManager.NOTIFICATION, 1.0f, 1.0f);
        }
    }

    private static HolyWorldEvent.Type mapApiEventType(String rawId, String displayName) {
        String s = ((rawId != null ? rawId : "") + " " + (displayName != null ? displayName : "")).toLowerCase(Locale.ROOT);
        if (s.contains("cargo") || s.contains("груз") || s.contains("посылк")) {
            return HolyWorldEvent.Type.CARGO;
        }
        if (s.contains("gold") || s.contains("fortress") || s.contains("лихорадк") || s.contains("золот") || s.contains("крепост")) {
            return HolyWorldEvent.Type.GOLD_RUSH;
        }
        if (s.contains("quarry") || s.contains("mine") || s.contains("шахт") || s.contains("полян") || s.contains("снежн") || s.contains("адск")) {
            return HolyWorldEvent.Type.QUARRY;
        }
        if (s.contains("cube") || s.contains("куб")) {
            return HolyWorldEvent.Type.CUBE;
        }
        return HolyWorldEvent.Type.OTHER;
    }

    private static String cleanRarity(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("default")) {
            return "";
        }
        String l = raw.toLowerCase(Locale.ROOT);
        if (l.contains("peaceful") || l.contains("мирн")) return "Мирный";
        if (l.contains("mythic") || l.contains("мифич")) return "Мифический";
        if (l.contains("legend") || l.contains("легенд")) return "Легендарный";
        if (l.contains("epic") || l.contains("эпич")) return "Эпический";
        if (l.contains("rare") || l.contains("редк")) return "Редкий";
        if (l.contains("deadly") || l.contains("смертел")) return "Смертельный";
        if (l.contains("explosive") || l.contains("взрыв")) return "Взрывной";
        return raw;
    }

    private static int parseAnarchyNumber(String id, String displayName) {
        String combined = (displayName != null ? displayName : "") + " " + (id != null ? id : "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < combined.length(); i++) {
            char c = combined.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (!sb.isEmpty()) {
                break;
            }
        }
        try {
            return !sb.isEmpty() ? Integer.parseInt(sb.toString()) : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private int detectCurrentAnarchy() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.world != null) {
            try {
                var sb = mc.world.getScoreboard();
                if (sb != null) {
                    var objective = sb.getObjectiveForSlot(net.minecraft.scoreboard.ScoreboardDisplaySlot.SIDEBAR);
                    if (objective != null && objective.getDisplayName() != null) {
                        String title = objective.getDisplayName().getString();
                        int n = parseAnarchyNumber(title, title);
                        if (n > 0) return n;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return 1;
    }

    private ServerInfo findServerByAnarchy(int num) {
        for (ServerInfo s : servers) {
            if (s.anarchyNumber() == num) return s;
        }
        return null;
    }

    public List<ServerInfo> getServers() {
        return Collections.unmodifiableList(servers);
    }

    public CoinsTradeInfo getCoinsTrade() {
        return this.coinsTrade;
    }

    public boolean isAvailable() {
        return this.isAvailable;
    }

    public long getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    public int getTotalServersCount() {
        return this.servers.size();
    }
}
