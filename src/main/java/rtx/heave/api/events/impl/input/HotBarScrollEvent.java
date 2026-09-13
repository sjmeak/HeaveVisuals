package rtx.heave.api.events.impl.input;
import rtx.heave.api.events.CancellableEvent;

public final class HotBarScrollEvent
extends CancellableEvent {
    private double vertical;

    public HotBarScrollEvent(double d) {
        this.vertical = d;
    }

    public double getVertical() {
        return this.vertical;
    }

    public void setVertical(double d) {
        this.vertical = d;
    }
}

