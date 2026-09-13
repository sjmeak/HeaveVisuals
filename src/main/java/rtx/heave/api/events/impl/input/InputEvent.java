package rtx.heave.api.events.impl.input;
import net.minecraft.util.PlayerInput;
import rtx.heave.api.events.Event;

public final class InputEvent
extends Event {
    private PlayerInput input;

    public InputEvent(PlayerInput playerInput) {
        this.input = playerInput;
    }

    public PlayerInput getInput() {
        return this.input;
    }
}

