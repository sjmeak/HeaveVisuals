package rtx.heave.mixinplugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class HeaveMixinPlugin implements IMixinConfigPlugin {
    private static final Set<String> SKIPPED = new HashSet<String>();

    private static Boolean lunarDetected = null;
    private static Boolean sodiumDetected = null;
    private static Boolean irisDetected = null;

    @Override
    public void onLoad(String mixinPackage) {
        if (isLunarClient()) {
            System.out.println("[Heave] Lunar Client environment detected! Active compatibility overrides enabled.");
        }
        if (isSodium()) {
            System.out.println("[Heave] Sodium/Embeddium detected! Enabling Sodium compatibility mode.");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Handle Lunar Client specific conflicts first
        if (isLunarClient()) {
            if (mixinClassName.endsWith("WindowTitleMixin")) {
                if (!Boolean.getBoolean("heave.debug.skip_lunar_compat")) {
                    System.out.println("[Heave] Skipping " + mixinClassName + " (Lunar Client custom menu compatibility)");
                }
                return false;
            }
            if (mixinClassName.endsWith("GameRendererMixin")) {
                if (!Boolean.getBoolean("heave.debug.skip_lunar_compat")) {
                    System.out.println("[Heave] Skipping " + mixinClassName + " (Lunar Client GameRenderer compatibility)");
                }
                return false;
            }
            if (mixinClassName.endsWith("CrashHookMixin") || mixinClassName.endsWith("SystemReportMixin")) {
                if (SKIPPED.add(mixinClassName)) {
                    System.out.println("[Heave] Skipping " + mixinClassName + " (Lunar Client reporter compatibility)");
                }
                return false;
            }
            if (mixinClassName.endsWith("LevelRendererChunkFadeMixin")) {
                if (SKIPPED.add(mixinClassName)) {
                    System.out.println("[Heave] Skipping " + mixinClassName + " (Lunar Sodium chunk compatibility)");
                }
                return false;
            }
        }

        // Handle Sodium conflicts
        if (isSodium()) {
            if (mixinClassName.endsWith("LevelRendererChunkFadeMixin")) {
                if (SKIPPED.add(mixinClassName)) {
                    System.out.println("[Heave] Skipping " + mixinClassName + " (Sodium chunk compatibility)");
                }
                return false;
            }
        }

        // Handle Iris requirement
        if (mixinClassName.contains(".compat.Iris") && !isIris()) {
            return false;
        }

        return true;
    }

    public static boolean isLunarClient() {
        if (lunarDetected == null) {
            lunarDetected = checkLunar();
        }
        return lunarDetected;
    }

    private static boolean checkLunar() {
        // 1. Thread stack trace inspection (Genesis is always on the call stack)
        try {
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                String s = ste.getClassName().toLowerCase();
                if (s.contains("lunar") || s.contains("genesis") || s.contains("ichor") || s.contains("moonsworth")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        // 2. ClassLoader hierarchy inspection
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            while (cl != null) {
                String name = cl.getClass().getName().toLowerCase();
                if (name.contains("lunar") || name.contains("genesis") || name.contains("ichor") || name.contains("moonsworth")) {
                    return true;
                }
                cl = cl.getParent();
            }
        } catch (Throwable ignored) {}

        try {
            String name = HeaveMixinPlugin.class.getClassLoader().getClass().getName().toLowerCase();
            if (name.contains("lunar") || name.contains("genesis") || name.contains("ichor") || name.contains("moonsworth")) {
                return true;
            }
        } catch (Throwable ignored) {}

        // 3. System properties inspection
        try {
            for (Object key : System.getProperties().keySet()) {
                String k = String.valueOf(key).toLowerCase();
                String v = String.valueOf(System.getProperty(k)).toLowerCase();
                if (k.contains("lunar") || k.contains("genesis") || k.contains("ichor")
                        || v.contains("lunar") || v.contains("genesis") || v.contains("ichor")
                        || v.contains("multiver")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        // 4. Classpath and launch command inspection
        String cp = System.getProperty("java.class.path", "").toLowerCase();
        if (cp.contains("lunar") || cp.contains("genesis") || cp.contains("ichor") || cp.contains("multiver")) {
            return true;
        }

        String cmd = System.getProperty("sun.java.command", "").toLowerCase();
        if (cmd.contains("lunar") || cmd.contains("genesis") || cmd.contains("ichor") || cmd.contains("multiver")) {
            return true;
        }

        String userDir = System.getProperty("user.dir", "").toLowerCase();
        if (userDir.contains(".lunarclient") || userDir.contains("lunar")) {
            return true;
        }

        if (System.getenv("LUNAR_CLIENT") != null) {
            return true;
        }

        // 5. Game directory via FabricLoader
        try {
            if (FabricLoader.getInstance().getGameDir().toString().toLowerCase().contains("lunar")) {
                return true;
            }
        } catch (Throwable ignored) {}

        if (isLoaded("lunarclient") || isLoaded("lunar-client") || isLoaded("lunar") || isLoaded("genesis")) {
            return true;
        }

        // 6. Direct class checking
        return isClassPresent("com.moonsworth.lunar.genesis.Genesis")
            || isClassPresent("com.moonsworth.lunar.client.LunarClient")
            || isClassPresent("com.moonsworth.lunar.patch.v1_21.LunarPatch");
    }

    public static boolean isSodium() {
        if (sodiumDetected == null) {
            sodiumDetected = isLoaded("sodium")
                || isLoaded("rubidium")
                || isLoaded("embeddium")
                || isClassPresent("net.caffeinemc.mods.sodium.client.SodiumClientMod")
                || isClassPresent("me.jellysquid.mods.sodium.client.SodiumClientMod");
        }
        return sodiumDetected;
    }

    public static boolean isIris() {
        if (irisDetected == null) {
            irisDetected = isLoaded("iris")
                || isLoaded("oculus")
                || isClassPresent("net.irisshaders.iris.Iris");
        }
        return irisDetected;
    }

    private static boolean isLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, HeaveMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable throwable) {
            try {
                Class.forName(className, false, Thread.currentThread().getContextClassLoader());
                return true;
            } catch (Throwable t2) {
                return false;
            }
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
