package rtx.heave.api.crosshair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class CrosshairConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("heave").resolve("crosshair.json");

    private static CrosshairConfig INSTANCE;

    public boolean enabled = true;
    public String style = "CUSTOM"; // CROSS, DOT, CUSTOM
    public String color = "#FFFFFF";
    public String entityColor = "#FF4444";
    public String containerColor = "#4488FF";
    public boolean applyBlend = true;
    public double canvasScale = 1.0;
    public int[] canvasPixels = new int[15 * 15];
    @SerializedName(value = "entityCanvasPixels", alternate = {"playerCanvasPixels"})
    public int[] entityCanvasPixels = new int[15 * 15];

    public CrosshairConfig() {
        initDefaultNormalPixels();
        initDefaultEntityPixels();
    }

    public void initDefaultNormalPixels() {
        int white = 0xFFFFFFFF;
        canvasPixels[7 * 15 + 7] = white;
        canvasPixels[7 * 15 + 5] = white;
        canvasPixels[7 * 15 + 6] = white;
        canvasPixels[7 * 15 + 8] = white;
        canvasPixels[7 * 15 + 9] = white;
        canvasPixels[5 * 15 + 7] = white;
        canvasPixels[6 * 15 + 7] = white;
        canvasPixels[8 * 15 + 7] = white;
        canvasPixels[9 * 15 + 7] = white;
    }

    public void initDefaultEntityPixels() {
        int red = 0xFFFF4444;
        entityCanvasPixels[7 * 15 + 7] = red;
        entityCanvasPixels[7 * 15 + 5] = red;
        entityCanvasPixels[7 * 15 + 6] = red;
        entityCanvasPixels[7 * 15 + 8] = red;
        entityCanvasPixels[7 * 15 + 9] = red;
        entityCanvasPixels[5 * 15 + 7] = red;
        entityCanvasPixels[6 * 15 + 7] = red;
        entityCanvasPixels[8 * 15 + 7] = red;
        entityCanvasPixels[9 * 15 + 7] = red;
    }

    public int getColorInt() {
        return parseColor(color);
    }

    public int getEntityColorInt() {
        return parseColor(entityColor);
    }

    public int getContainerColorInt() {
        return parseColor(containerColor);
    }

    public static int parseColor(Object obj) {
        if (obj instanceof Number num) {
            return num.intValue();
        }
        if (obj instanceof String hex) {
            try {
                if (hex.startsWith("#")) {
                    long val = Long.parseLong(hex.substring(1), 16);
                    if (hex.length() == 7) {
                        return (int) (0xFF000000 | val);
                    } else if (hex.length() == 9) {
                        return (int) val;
                    }
                } else {
                    return (int) Long.parseLong(hex, 16);
                }
            } catch (Exception ignored) {}
        }
        return 0xFFFFFFFF;
    }

    public static CrosshairConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                CrosshairConfig loaded = GSON.fromJson(json, CrosshairConfig.class);
                INSTANCE = (loaded != null) ? loaded : new CrosshairConfig();
                if (INSTANCE.canvasPixels == null || INSTANCE.canvasPixels.length != 15 * 15) {
                    INSTANCE.canvasPixels = new int[15 * 15];
                    INSTANCE.initDefaultNormalPixels();
                }
                if (INSTANCE.entityCanvasPixels == null || INSTANCE.entityCanvasPixels.length != 15 * 15) {
                    INSTANCE.entityCanvasPixels = new int[15 * 15];
                    INSTANCE.initDefaultEntityPixels();
                }
            } else {
                INSTANCE = new CrosshairConfig();
                save();
            }
        } catch (Exception ignored) {
            INSTANCE = new CrosshairConfig();
        }
    }

    public static void save() {
        try {
            if (INSTANCE == null) return;
            if (!Files.exists(FILE.getParent())) {
                Files.createDirectories(FILE.getParent());
            }
            Files.writeString(FILE, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
}
