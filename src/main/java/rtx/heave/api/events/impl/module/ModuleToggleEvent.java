package rtx.heave.api.events.impl.module;
import rtx.heave.api.events.Event;
import rtx.heave.api.modules.Module;

public final class ModuleToggleEvent
extends Event {
    private final Module module;
    private final boolean enabled;

    public ModuleToggleEvent(Module module, boolean bl) {
        this.module = module;
        this.enabled = bl;
    }

    public Module getModule() {
        return this.module;
    }

    public boolean isEnabled() {
        return this.enabled;
    }
}

