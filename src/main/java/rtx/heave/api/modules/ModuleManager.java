package rtx.heave.api.modules;

import rtx.heave.api.modules.impl.Visuals.TimeChanger;
import rtx.heave.api.modules.impl.Visuals.FullBright;
import rtx.heave.api.modules.impl.Visuals.CustomFog;
import rtx.heave.api.modules.impl.Visuals.HitWaves;
import rtx.heave.api.modules.impl.Interface.ScoreboardModule;
import rtx.heave.api.modules.impl.Movement.AutoSprint;
import rtx.heave.api.modules.impl.Visuals.PlayerPing;
import rtx.heave.api.modules.impl.Visuals.NameTags;

import rtx.heave.api.events.EventHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import rtx.heave.api.events.EventBus;
import rtx.heave.api.events.impl.game.TickEvent;
import rtx.heave.api.events.impl.input.HotBarScrollEvent;
import rtx.heave.api.events.impl.input.KeyPressEvent;
import rtx.heave.api.events.impl.input.KeyPressEvent.Action;
import rtx.heave.api.events.impl.input.MouseButtonEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.Module.BindMode;
import rtx.heave.api.modules.impl.Interface.ArmorModule;
import rtx.heave.api.modules.impl.Interface.ClickGui;
import rtx.heave.api.modules.impl.Interface.CooldownsModule;
import rtx.heave.api.modules.impl.Interface.CustomGui;
import rtx.heave.api.modules.impl.Interface.InfoModule;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.modules.impl.Interface.InventoryModule;
import rtx.heave.api.modules.impl.Interface.NotificationsModule;
import rtx.heave.api.modules.impl.Interface.PotionsModule;
import rtx.heave.api.modules.impl.Interface.TargetHudModule;
import rtx.heave.api.modules.impl.Interface.WatermarkModule;
import rtx.heave.api.modules.impl.Utils.CameraSettings;
import rtx.heave.api.modules.impl.Utils.ClientSounds;
import rtx.heave.api.modules.impl.Utils.EventMarkers;
import rtx.heave.api.modules.impl.Utils.Globals;
import rtx.heave.api.modules.impl.Utils.HandSwap;
import rtx.heave.api.modules.impl.Utils.HitSound;
import rtx.heave.api.modules.impl.Utils.HolyWorldApiModule;
import rtx.heave.api.modules.impl.Utils.HolyWorldHelper;
import rtx.heave.api.modules.impl.Utils.Optimization;
import rtx.heave.api.modules.impl.Utils.Party;
import rtx.heave.api.modules.impl.Utils.Profiler;
import rtx.heave.api.modules.impl.Utils.ShulkerPreview;
import rtx.heave.api.modules.impl.Utils.ItemScroller;
import rtx.heave.api.modules.impl.Utils.StreamerMode;
import rtx.heave.api.modules.impl.Visuals.AspectRatio;
import rtx.heave.api.modules.impl.Visuals.BetterHud;
import rtx.heave.api.modules.impl.Visuals.Blink;
import rtx.heave.api.modules.impl.Visuals.BlockOverlay;
import rtx.heave.api.modules.impl.Visuals.ChinaHat;
import rtx.heave.api.modules.impl.Visuals.Crosshair;
import rtx.heave.api.modules.impl.Visuals.FogBlur;
import rtx.heave.api.modules.impl.Visuals.HitColor;
import rtx.heave.api.modules.impl.Visuals.HitParticles;
import rtx.heave.api.modules.impl.Visuals.Hitboxes;
import rtx.heave.api.modules.impl.Visuals.ItemHighlight;
import rtx.heave.api.modules.impl.Visuals.JumpCircle;
import rtx.heave.api.modules.impl.Visuals.KillEffect;
import rtx.heave.api.modules.impl.Visuals.NoRender;
import rtx.heave.api.modules.impl.Visuals.SelfTag;
import rtx.heave.api.modules.impl.Visuals.ShaderHands;
import rtx.heave.api.modules.impl.Visuals.TargetESP;
import rtx.heave.api.modules.impl.Visuals.Trails;
import rtx.heave.api.modules.impl.Visuals.ViewModel;
import rtx.heave.api.modules.impl.Visuals.WorldParticles;
import rtx.heave.api.modules.restrict.Server;
import rtx.heave.api.modules.restrict.ServerRestrictions;
import rtx.heave.utils.key.KeyBind;

public final class ModuleManager {
    private static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<Module>();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private ModuleManager() {
    }

    public <T extends Module> T get(Class<T> clazz) {
        for (Module module : this.modules) {
            if (!clazz.isInstance(module)) continue;
            return (T)module;
        }
        return null;
    }

    public static ModuleManager get() {
        return INSTANCE;
    }

    private void register(Module ... moduleArray) {
        for (Module module : moduleArray) {
            this.modules.add(module);
        }
    }

    public void init() {
        this.register(new HitSound(), new TimeChanger(), new FullBright(), new CustomFog(), new HitWaves(), new ScoreboardModule(), new AutoSprint(), new PlayerPing(), new NameTags(), new BetterHud(), new Blink(), new AspectRatio(), new ClickGui(), new CustomGui(), new FogBlur(), new HitColor(), new Hitboxes(), new SelfTag(), new InterfaceModule(), new NotificationsModule(), new WatermarkModule(), new Profiler(), new TargetHudModule(), new PotionsModule(), new CooldownsModule(), new InfoModule(), new ArmorModule(), new InventoryModule(), new ViewModel(), new HandSwap(), new CameraSettings(), new JumpCircle(), new Crosshair(), new KillEffect(), new rtx.heave.api.modules.impl.Visuals.TotemCounter(), new rtx.heave.api.modules.impl.Visuals.HealthIndicator(), new rtx.heave.api.modules.impl.Visuals.CustomSwords(), new ChinaHat(), new BlockOverlay(), new ShaderHands(), new ItemHighlight(), new NoRender(), new WorldParticles(), new HitParticles(), new TargetESP(), new Trails(), new ShulkerPreview(), new ClientSounds(), new HolyWorldApiModule(), new HolyWorldHelper(), new EventMarkers(), new rtx.heave.api.modules.impl.Visuals.SaturationModule(), new StreamerMode(), new Optimization(), new Party(), new Globals(), new ItemScroller());
        EventBus.get().subscribe(this);
    }

    public List<Module> getAll() {
        return Collections.unmodifiableList(this.modules);
    }

    @EventHandler
    public void onKey(KeyPressEvent keyPressEvent) {
        if (this.shouldIgnoreBinds()) {
            return;
        }
        if (keyPressEvent.action == KeyPressEvent.Action.PRESS) {
            this.applyKeyBound(keyPressEvent.keyCode, true);
        } else if (keyPressEvent.action == KeyPressEvent.Action.RELEASE) {
            this.applyKeyBound(keyPressEvent.keyCode, false);
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre()) {
            return;
        }
        EnumSet<Server> enumSet = ServerRestrictions.current();
        for (Module module : this.modules) {
            if (!module.isEnabled()) continue;
            if (ServerRestrictions.isHiddenBy(module, enumSet)) {
                module.disable();
                continue;
            }
            String string = ServerRestrictions.blockReason(module, enumSet);
            if (string == null) continue;
            module.disable();
            ServerRestrictions.notify(string);
        }
    }

    public static boolean isKeybindToggling = false;

    private boolean applyBind(Module module, boolean bl) {
        if (module.getBindMode() == Module.BindMode.HOLD) {
            try {
                isKeybindToggling = true;
                module.setEnabled(bl);
            } finally {
                isKeybindToggling = false;
            }
            return true;
        }
        if (bl) {
            try {
                isKeybindToggling = true;
                module.toggle();
            } finally {
                isKeybindToggling = false;
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onMouse(MouseButtonEvent mouseButtonEvent) {
        if (this.shouldIgnoreBinds()) {
            return;
        }
        if (mouseButtonEvent.action == MouseButtonEvent.Action.PRESS) {
            this.applyMouseBound(mouseButtonEvent.button, true);
        } else if (mouseButtonEvent.action == MouseButtonEvent.Action.RELEASE) {
            this.applyMouseBound(mouseButtonEvent.button, false);
        }
    }

    public List<Module> getEnabled() {
        return this.modules.stream().filter(Module::isEnabled).toList();
    }

    @EventHandler
    public void onScroll(HotBarScrollEvent hotBarScrollEvent) {
        if (this.shouldIgnoreBinds()) {
            return;
        }
        int n = hotBarScrollEvent.getVertical() > 0.0 ? 1000 : (hotBarScrollEvent.getVertical() < 0.0 ? 1001 : -1);
        if (n != -1 && this.toggleBound(n)) {
            hotBarScrollEvent.cancel();
        }
    }

    public List<Module> getByCategory(Category category) {
        return this.modules.stream().filter(module -> module.getCategory() == category).toList();
    }

    private boolean toggleBound(int n) {
        boolean bl = false;
        for (Module module : this.modules) {
            KeyBind keyBind;
            if (module instanceof ClickGui || !(keyBind = module.getBind()).isBound() || keyBind.getCode() != n) continue;
            try {
                isKeybindToggling = true;
                module.toggle();
            } finally {
                isKeybindToggling = false;
            }
            bl = true;
        }
        return bl;
    }

    private boolean applyMouseBound(int n, boolean bl) {
        boolean bl2 = false;
        for (Module module : this.modules) {
            KeyBind keyBind = module.getBind();
            if (!keyBind.isBound() || !keyBind.matchesMouseButton(n)) continue;
            bl2 |= this.applyBind(module, bl);
        }
        return bl2;
    }

    private boolean applyKeyBound(int n, boolean bl) {
        boolean bl2 = false;
        for (Module module : this.modules) {
            KeyBind keyBind;
            if (module instanceof ClickGui || !(keyBind = module.getBind()).isBound() || keyBind.getCode() != n) continue;
            bl2 |= this.applyBind(module, bl);
        }
        return bl2;
    }

    private boolean shouldIgnoreBinds() {
        return this.mc == null || this.mc.currentScreen != null;
    }

    public Module findByName(String string) {
        for (Module module : this.modules) {
            if (!module.getName().equalsIgnoreCase(string)) continue;
            return module;
        }
        return null;
    }
}

