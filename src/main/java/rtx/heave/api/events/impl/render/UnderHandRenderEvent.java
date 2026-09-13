package rtx.heave.api.events.impl.render;
import rtx.heave.api.events.Event;
import rtx.heave.utils.render.underhand.UnderHand2D;

public final class UnderHandRenderEvent
extends Event {
    private final UnderHand2D ctx;

    public UnderHandRenderEvent(UnderHand2D underHand2D) {
        this.ctx = underHand2D;
    }

    public UnderHand2D ctx() {
        return this.ctx;
    }
}

