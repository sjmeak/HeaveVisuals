package rtx.heave.api.modules.impl.Visuals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.client.MinecraftClient;
import rtx.heave.Heave;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.ModeSetting;

public final class CustomSwords extends Module {

    private static final String NONE = "Обычный";

    private static final String[] FOLDER_ALIASES = {
        "CustomSwordsHeave",
        "CustomHeaveSword",
        "CustomHeaveSwords"
    };

    private static final String[] ALL_SWORD_TYPES = {
        "netherite_sword",
        "diamond_sword",
        "iron_sword",
        "golden_sword",
        "stone_sword",
        "wooden_sword"
    };

    private static CustomSwords instance;

    private final BooleanSetting selfOnly = this.register(
        new BooleanSetting("Только свой", "Заменять модель меча только у своего игрока.", true)
    );
    private final ModeSetting weapon = this.register(
        new ModeSetting("Меч", "Выбранный меч.", NONE, NONE)
    );

    private final List<String> folderSwordNames = new ArrayList<>();
    private long lastScanTime = 0;

    public CustomSwords() {
        super("Custom Swords", "Заменяет модели ванильных мечей.", Category.VISUALS);
        instance = this;
        this.rescanFolder();
    }

    public static CustomSwords getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        this.rescanFolder();
        super.onEnable();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (!event.isPre() || !this.isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - this.lastScanTime > 5000) {
            this.lastScanTime = now;
            this.rescanFolder();
        }
    }

    public boolean isSelfOnly() {
        return this.selfOnly.getValue();
    }

    /**
     * Returns the CustomModelData string to set on the sword stack,
     * or null if "None" is selected.
     */
    public String getSelectedCustomModelKey() {
        String sel = this.weapon.getSelected();
        if (sel == null || NONE.equals(sel)) return null;
        return sel;
    }

    public synchronized void rescanFolder() {
        MinecraftClient mc = MinecraftClient.getInstance();
        File runDir = (mc != null && mc.runDirectory != null) ? mc.runDirectory : new File(".");

        File defaultDir = new File(runDir, "CustomSwordsHeave");
        extractDefaultSwordsIfMissing(defaultDir);

        String userHome = System.getProperty("user.home", "");
        String appData = System.getenv("APPDATA");

        List<File> candidateDirs = new ArrayList<>();
        candidateDirs.add(defaultDir);
        candidateDirs.add(new File(runDir, "CustomHeaveSword"));
        candidateDirs.add(new File(runDir, "CustomHeaveSwords"));

        List<File> baseDirs = new ArrayList<>();
        if (appData != null && !appData.isEmpty()) {
            baseDirs.add(new File(appData, ".minecraft"));
        }
        if (!userHome.isEmpty()) {
            baseDirs.add(new File(userHome, ".minecraft"));
            baseDirs.add(new File(userHome, ".lunarclient"));
            baseDirs.add(new File(userHome, ".lunarclient/offline/multiver"));
            baseDirs.add(new File(userHome, ".lunarclient/profiles/1.21"));
        }

        for (File base : baseDirs) {
            for (String alias : FOLDER_ALIASES) {
                File d = new File(base, alias);
                if (d.exists() && d.isDirectory() && !candidateDirs.contains(d)) {
                    candidateDirs.add(d);
                }
            }
        }

        if (!userHome.isEmpty()) {
            File dotMc = new File(userHome, ".minecraft/CustomSwordsHeave");
            if (dotMc.getParentFile().exists()) extractDefaultSwordsIfMissing(dotMc);
        }

        Map<String, File> swordsMap = new LinkedHashMap<>();
        for (File d : candidateDirs) {
            File[] sub = d.listFiles(File::isDirectory);
            if (sub != null) {
                for (File s : sub) {
                    swordsMap.putIfAbsent(s.getName(), s);
                }
            }
        }

        List<String> newFolderNames = new ArrayList<>(swordsMap.keySet());
        this.folderSwordNames.clear();
        this.folderSwordNames.addAll(newFolderNames);

        // Rebuild ModeSetting options
        List<String> opts = new ArrayList<>();
        opts.add(NONE);
        opts.addAll(newFolderNames);

        List<String> currentOpts = this.weapon.getOptions();
        if (!currentOpts.equals(opts)) {
            String prev = this.weapon.getSelected();
            this.weapon.value(opts.toArray(new String[0]));
            if (opts.contains(prev)) {
                this.weapon.setSelected(prev);
            }
        }

        boolean packModified = this.buildRuntimePack(runDir, swordsMap.values());
        if (packModified && this.lastScanTime > 0 && mc != null && mc.world != null) {
            try {
                mc.execute(mc::reloadResources);
            } catch (Throwable t) {
                Heave.LOGGER.error("[CustomSwords] Failed to reload resources", t);
            }
        }
    }

    private static void extractDefaultSwordsIfMissing(File targetDir) {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        try (InputStream is = CustomSwords.class.getResourceAsStream("/assets/heave/default_swords.zip")) {
            if (is == null) {
                Heave.LOGGER.debug("[CustomSwords] default_swords.zip not found in resources");
                return;
            }
            try (ZipInputStream zis = new ZipInputStream(is, StandardCharsets.UTF_8)) {
                ZipEntry entry;
                byte[] buffer = new byte[8192];
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        File dir = new File(targetDir, entry.getName());
                        if (!dir.exists()) dir.mkdirs();
                    } else {
                        File outFile = new File(targetDir, entry.getName());
                        if (!outFile.exists()) {
                            File parent = outFile.getParentFile();
                            if (parent != null && !parent.exists()) parent.mkdirs();
                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                int len;
                                while ((len = zis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
        } catch (Exception e) {
            Heave.LOGGER.error("[CustomSwords] Failed to extract default swords", e);
        }
    }

    private boolean buildRuntimePack(File runDir, Collection<File> subDirs) {
        List<File> packDirs = new ArrayList<>();
        packDirs.add(new File(new File(runDir, "resourcepacks"), "heave_custom_swords"));

        String userHome = System.getProperty("user.home", "");
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isEmpty()) {
            File dotMcPacks = new File(appData, ".minecraft/resourcepacks/heave_custom_swords");
            if (!packDirs.contains(dotMcPacks)) packDirs.add(dotMcPacks);
        }
        if (!userHome.isEmpty()) {
            File userMcPacks = new File(userHome, ".minecraft/resourcepacks/heave_custom_swords");
            if (!packDirs.contains(userMcPacks)) packDirs.add(userMcPacks);
            File lunarMultiver = new File(userHome, ".lunarclient/offline/multiver/resourcepacks/heave_custom_swords");
            if (lunarMultiver.getParentFile().exists() && !packDirs.contains(lunarMultiver)) {
                packDirs.add(lunarMultiver);
            }
        }

        boolean anyModified = false;

        for (File packDir : packDirs) {
            File modelsDir = new File(packDir, "assets/minecraft/models/item");
            File texturesDir = new File(packDir, "assets/minecraft/textures/item");
            File itemsDir = new File(packDir, "assets/minecraft/items");
            modelsDir.mkdirs();
            texturesDir.mkdirs();
            itemsDir.mkdirs();

            boolean m1 = writeIfDifferent(new File(packDir, "pack.mcmeta"),
                ("{\n  \"pack\": {\n    \"pack_format\": " + net.minecraft.SharedConstants.RESOURCE_PACK_VERSION + ",\n    \"description\": \"Heave Custom Swords\"\n  }\n}")
                .getBytes(StandardCharsets.UTF_8));
            if (m1) anyModified = true;

            // Copy folder sword assets
            for (File dir : subDirs) {
                String name = dir.getName();
                String safe = safeName(name);
                File[] files = dir.listFiles();
                if (files == null) continue;

                File pngFile = findBestPng(files);
                File jsonFile = findBestJson(files);

                if (pngFile != null) {
                    boolean m2 = copyIfDifferent(pngFile, new File(texturesDir, safe + ".png"));
                    if (m2) anyModified = true;
                }

                if (jsonFile != null) {
                    try {
                        String content = new String(readBytes(jsonFile), StandardCharsets.UTF_8);
                        content = content.replace("\"items/netherite_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"item/netherite_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"minecraft:items/netherite_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"minecraft:item/netherite_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"items/diamond_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"item/diamond_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"minecraft:items/diamond_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"minecraft:item/diamond_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"items/iron_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"item/iron_sword\"", "\"item/" + safe + "\"")
                                         .replace("\"items/sword\"", "\"item/" + safe + "\"")
                                         .replace("\"item/sword\"", "\"item/" + safe + "\"")
                                         .replace("\"items/", "\"item/");
                        boolean m3 = writeIfDifferent(new File(modelsDir, safe + ".json"), content.getBytes(StandardCharsets.UTF_8));
                        if (m3) anyModified = true;
                    } catch (Exception e) {
                        Heave.LOGGER.error("[CustomSwords] Failed to process json for {}", name, e);
                    }
                } else {
                    String flat = "{\n  \"parent\": \"minecraft:item/handheld\",\n  \"textures\": {\n    \"layer0\": \"item/" + safe + "\"\n  }\n}";
                    boolean m4 = writeIfDifferent(new File(modelsDir, safe + ".json"), flat.getBytes(StandardCharsets.UTF_8));
                    if (m4) anyModified = true;
                }
            }

            // Generate items/<sword>.json for ALL swords with their respective fallbacks
            for (String swordType : ALL_SWORD_TYPES) {
                String itemJson = buildItemJson(subDirs, swordType);
                boolean m5 = writeIfDifferent(new File(itemsDir, swordType + ".json"), itemJson.getBytes(StandardCharsets.UTF_8));
                if (m5) anyModified = true;
            }
        }

        return anyModified;
    }

    private static File findBestPng(File[] files) {
        File best = null;
        for (File f : files) {
            String lower = f.getName().toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".png")) continue;
            if (lower.equals("pack.png") || lower.equals("icon.png")) continue;
            if (lower.contains("sword") || lower.contains("blade") || lower.contains("item")) {
                return f;
            }
            if (best == null) {
                best = f;
            }
        }
        if (best != null) return best;
        for (File f : files) {
            if (f.getName().toLowerCase(Locale.ROOT).endsWith(".png")) return f;
        }
        return null;
    }

    private static File findBestJson(File[] files) {
        File best = null;
        for (File f : files) {
            String lower = f.getName().toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".json")) continue;
            if (lower.equals("pack.mcmeta")) continue;
            if (lower.contains("sword") || lower.contains("blade") || lower.contains("item") || lower.contains("model")) {
                return f;
            }
            if (best == null) {
                best = f;
            }
        }
        if (best != null) return best;
        for (File f : files) {
            String lower = f.getName().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".json") && !lower.equals("pack.mcmeta")) return f;
        }
        return null;
    }

    private String buildItemJson(Collection<File> subDirs, String fallbackItem) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"model\": {\n    \"type\": \"minecraft:select\",\n    \"property\": \"minecraft:custom_model_data\",\n    \"index\": 0,\n    \"cases\": [\n");

        boolean first = true;
        for (File dir : subDirs) {
            String name = dir.getName();
            String modelPath = "minecraft:item/" + safeName(name);
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            sb.append("      { \"when\": \"").append(jsonEscape(name))
              .append("\", \"model\": { \"type\": \"minecraft:model\", \"model\": \"")
              .append(modelPath).append("\" } }");
        }

        sb.append("\n    ],\n    \"fallback\": { \"type\": \"minecraft:model\", \"model\": \"minecraft:item/")
          .append(fallbackItem).append("\" }\n  }\n}");
        return sb.toString();
    }

    private static String safeName(String folderName) {
        String lower = folderName.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            switch (c) {
                case 'а': sb.append("a"); break;
                case 'б': sb.append("b"); break;
                case 'в': sb.append("v"); break;
                case 'г': sb.append("g"); break;
                case 'д': sb.append("d"); break;
                case 'е': case 'ё': sb.append("e"); break;
                case 'ж': sb.append("zh"); break;
                case 'з': sb.append("z"); break;
                case 'и': case 'й': sb.append(c == 'й' ? "y" : "i"); break;
                case 'к': sb.append("k"); break;
                case 'л': sb.append("l"); break;
                case 'м': sb.append("m"); break;
                case 'н': sb.append("n"); break;
                case 'о': sb.append("o"); break;
                case 'п': sb.append("p"); break;
                case 'р': sb.append("r"); break;
                case 'с': sb.append("s"); break;
                case 'т': sb.append("t"); break;
                case 'у': sb.append("u"); break;
                case 'ф': sb.append("f"); break;
                case 'х': sb.append("kh"); break;
                case 'ц': sb.append("ts"); break;
                case 'ч': sb.append("ch"); break;
                case 'ш': sb.append("sh"); break;
                case 'щ': sb.append("shch"); break;
                case 'ы': sb.append("y"); break;
                case 'э': sb.append("e"); break;
                case 'ю': sb.append("yu"); break;
                case 'я': sb.append("ya"); break;
                case 'ъ': case 'ь': break;
                default:
                    if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-') {
                        sb.append(c);
                    } else {
                        sb.append('_');
                    }
                    break;
            }
        }
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean copyIfDifferent(File src, File dst) {
        try {
            if (dst.exists() && dst.length() == src.length() && dst.lastModified() >= src.lastModified()) return false;
            File parent = dst.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            dst.setLastModified(src.lastModified());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean writeIfDifferent(File dst, byte[] data) {
        try {
            if (dst.exists() && dst.length() == data.length) {
                byte[] existing = readBytes(dst);
                if (java.util.Arrays.equals(existing, data)) return false;
            }
            File parent = dst.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileOutputStream fos = new FileOutputStream(dst)) {
                fos.write(data);
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static byte[] readBytes(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            return in.readAllBytes();
        }
    }
}
