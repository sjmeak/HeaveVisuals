package rtx.heave.api.drags;

import rtx.heave.api.events.EventHandler;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import rtx.heave.utils.render.render2d.Render2DCoordinateSpace;
import rtx.heave.Heave;
import rtx.heave.api.config.ConfigManager;
import rtx.heave.api.drags.DragController;
import rtx.heave.api.drags.Draggable;
import rtx.heave.api.drags.HudSettingsPanel;
import rtx.heave.api.drags.MitosisController;
import rtx.heave.api.drags.MitosisController.SplitDraw;
import rtx.heave.api.drags.Position;
import rtx.heave.api.drags.SplitRectComp;
import rtx.heave.api.drags.components.ArmorComp;
import rtx.heave.api.drags.components.CooldownsComp;
import rtx.heave.api.drags.components.GpsHudComp;
import rtx.heave.api.drags.components.HolyWorldComp;
import rtx.heave.api.drags.components.InventoryComp;
import rtx.heave.api.drags.components.PotionsComp;
import rtx.heave.api.drags.components.TargetHudComp;
import rtx.heave.api.drags.components.TotemCounterComp;
import rtx.heave.api.drags.components.WatermarkComp;
import rtx.heave.api.drags.hud.InfoHud;
import rtx.heave.api.events.EventBus;
import rtx.heave.api.events.impl.game.CloseScreenEvent;
import rtx.heave.api.events.impl.input.MouseButtonEvent;
import rtx.heave.api.events.impl.input.MouseButtonEvent.Action;
import rtx.heave.api.events.impl.render.HudRenderEvent;
import rtx.heave.api.modules.impl.Interface.InterfaceModule;
import rtx.heave.api.ui.settings.Setting;
import rtx.heave.utils.render.others.LoadingVisualGuard;
import rtx.heave.utils.render.others.RectUtil;
import rtx.heave.utils.render.render2d.Render2D;

public final class DragSystem {
    private static final DragSystem INSTANCE = new DragSystem();
    private final List<Draggable> elements = new ArrayList<Draggable>();
    private final Set<String> renderFailures = new HashSet<String>();
    private Draggable activeDrag = null;

    private DragSystem() {
    }

    public static DragSystem get() {
        return INSTANCE;
    }

    public void register(Draggable draggable) {
        this.elements.add(draggable);
    }

    public void init() {
        this.register(new WatermarkComp());
        this.register(new TargetHudComp());
        this.register(new PotionsComp());
        this.register(new CooldownsComp());
        this.register(new ArmorComp());
        this.register(new InventoryComp());
        this.register(new TotemCounterComp());
        this.register(new HolyWorldComp());
        this.register(new GpsHudComp());
        new InfoHud();
        ConfigManager.applyActiveDrags();
        EventBus.get().subscribe(this);
    }

    public void unregister(Draggable draggable) {
        this.elements.remove(draggable);
        if (this.activeDrag == draggable) {
            this.activeDrag = null;
        }
    }

    @EventHandler
    public void onCloseScreen(CloseScreenEvent closeScreenEvent) {
        if (!(closeScreenEvent.getScreen() instanceof ChatScreen)) {
            return;
        }
        if (this.activeDrag != null) {
            this.activeDrag.getDrag().release();
            this.activeDrag = null;
            ConfigManager.markDirty();
        }
    }

    public void bringToFront(Draggable draggable) {
        int n = this.elements.indexOf(draggable);
        if (n < 0 || n == this.elements.size() - 1) {
            return;
        }
        this.elements.remove(n);
        this.elements.add(draggable);
    }

    public boolean isDragModeActive() {
        return MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
    }

    private void handleRenderFailure(Draggable draggable, Throwable throwable) {
        try {
            RectUtil.clearSplitOverride();
            Render2D.flush();
        }
        catch (Throwable throwable2) {
            // empty catch block
        }
        String string = draggable.getClass().getName();
        if (this.renderFailures.add(string)) {
            Heave.LOGGER.error("[DragSystem] Component {} failed to render and was isolated", (Object)string, (Object)throwable);
        }
    }

    private void releaseInterruptedDrag() {
        boolean bl = false;
        if (this.activeDrag != null) {
            this.activeDrag.getDrag().release();
            this.activeDrag = null;
            bl = true;
        }
        for (Draggable draggable : this.elements) {
            if (!draggable.getDrag().isDragging()) continue;
            draggable.getDrag().release();
            bl = true;
        }
        if (bl) {
            ConfigManager.markDirty();
        }
    }

    public void applyDragDistortion(Framebuffer framebuffer) {
    }

    public List<Draggable> getAll() {
        return Collections.unmodifiableList(this.elements);
    }

    @EventHandler
    public void onHud(HudRenderEvent hudRenderEvent) {
        if (LoadingVisualGuard.shouldSuppressHud(MinecraftClient.getInstance())) {
            return;
        }
        DrawContext drawContext = hudRenderEvent.getGraphics();
        boolean inChat = this.isDragModeActive();
        if (!inChat) {
            this.releaseInterruptedDrag();
        }
        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();

        if (inChat) {
            for (Draggable draggable : this.elements) {
                if (!draggable.isInteractive()) continue;
                draggable.getDrag().tick(draggable, mouseX, mouseY);
            }
        }

        for (Draggable draggable : this.elements) {
            if (!draggable.getDrag().isDragging()) {
                draggable.getDrag().applyScreenClamp(draggable.width(), draggable.height());
            }
        }

        for (Draggable draggable : this.elements) {
            if (!draggable.isVisible()) continue;
            try {
                draggable.renderNormal(drawContext);
            } catch (Throwable throwable) {
                this.handleRenderFailure(draggable, throwable);
            }
        }

        if (inChat) {
            HudAlignment.INSTANCE.render(drawContext);
        }
    }

    public JsonObject writeDrags() {
        JsonObject jsonObject = new JsonObject();
        for (Draggable draggable : this.elements) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("x", (Number)Float.valueOf(draggable.getDrag().getTargetX()));
            jsonObject2.addProperty("y", (Number)Float.valueOf(draggable.getDrag().getTargetY()));
            jsonObject.add(draggable.getId(), (JsonElement)jsonObject2);
        }
        return jsonObject;
    }

    public void applyDrags(JsonObject jsonObject) {
        if (jsonObject == null) {
            return;
        }
        for (Draggable draggable : this.elements) {
            JsonElement jsonElement = jsonObject.has(draggable.getId()) ? jsonObject.get(draggable.getId()) : null;
            if (jsonElement == null || !jsonElement.isJsonObject()) continue;
            JsonObject jsonObject2 = jsonElement.getAsJsonObject();
            try {
                if (jsonObject2.has("x")) {
                    draggable.getDrag().setTargetX(jsonObject2.get("x").getAsFloat());
                }
                if (jsonObject2.has("y")) {
                    draggable.getDrag().setTargetY(jsonObject2.get("y").getAsFloat());
                }
                draggable.getDrag().syncToTarget();
            }
            catch (Exception exception) {}
        }
    }

    public boolean isDragging() {
        return this.activeDrag != null;
    }

    public boolean onChatClick(Click click) {
        if (!this.isDragModeActive()) {
            return false;
        }
        if (this.activeDrag != null) {
            return true;
        }
        float mouseX = (float) (click.x() / (double) Render2DCoordinateSpace.guiIndependentScale());
        float mouseY = (float) (click.y() / (double) Render2DCoordinateSpace.guiIndependentScale());

        if (click.button() == 1 && this.activeDrag != null) {
            this.activeDrag.getDrag().cancel();
            this.activeDrag = null;
            ConfigManager.markDirty();
            return true;
        }

        if (click.button() == 0) {
            for (int i = this.elements.size() - 1; i >= 0; --i) {
                Draggable draggable = this.elements.get(i);
                if (!draggable.isInteractive()) continue;
                float grabW = Math.max(draggable.width(), draggable.overlayWidth());
                float grabH = Math.max(draggable.height(), draggable.overlayHeight());
                if (draggable.getDrag().tryGrab(mouseX, mouseY, grabW, grabH)) {
                    this.activeDrag = draggable;
                    this.bringToFront(draggable);
                    return true;
                }
            }
        }
        return false;
    }

    public void onCursorMove(float mouseX, float mouseY) {
        if (this.activeDrag != null) {
            this.activeDrag.getDrag().tick(this.activeDrag, mouseX, mouseY);
        }
    }

    public boolean onChatDragged(Click click) {
        if (!this.isDragModeActive() || this.activeDrag == null) {
            return false;
        }
        float mouseX = (float) (click.x() / (double) Render2DCoordinateSpace.guiIndependentScale());
        float mouseY = (float) (click.y() / (double) Render2DCoordinateSpace.guiIndependentScale());
        this.activeDrag.getDrag().tick(this.activeDrag, mouseX, mouseY);
        return true;
    }

    public boolean onChatRelease(Click click) {
        if (this.activeDrag != null) {
            this.activeDrag.getDrag().release();
            this.activeDrag = null;
            ConfigManager.markDirty();
            return true;
        }
        return false;
    }

    public void onChatClosed() {
        if (this.activeDrag != null) {
            this.activeDrag.getDrag().release();
            this.activeDrag = null;
            ConfigManager.markDirty();
        }
        this.releaseInterruptedDrag();
    }

    @EventHandler
    public void onMouseButton(MouseButtonEvent event) {
        if (!this.isDragModeActive()) {
            return;
        }
        float mouseX = Position.mouseX();
        float mouseY = Position.mouseY();

        if (event.action == MouseButtonEvent.Action.PRESS && event.button == 1 && this.activeDrag != null) {
            this.activeDrag.getDrag().cancel();
            this.activeDrag = null;
            ConfigManager.markDirty();
            event.cancel();
            return;
        }

        if (event.button == 0) {
            if (event.action == MouseButtonEvent.Action.PRESS) {
                for (int i = this.elements.size() - 1; i >= 0; --i) {
                    Draggable draggable = this.elements.get(i);
                    if (!draggable.isInteractive()) continue;
                    float grabW = Math.max(draggable.width(), draggable.overlayWidth());
                    float grabH = Math.max(draggable.height(), draggable.overlayHeight());
                    if (draggable.getDrag().tryGrab(mouseX, mouseY, grabW, grabH)) {
                        this.activeDrag = draggable;
                        this.bringToFront(draggable);
                        event.cancel();
                        return;
                    }
                }
            } else if (event.action == MouseButtonEvent.Action.RELEASE && this.activeDrag != null) {
                this.activeDrag.getDrag().release();
                this.activeDrag = null;
                ConfigManager.markDirty();
                event.cancel();
            }
        }
    }
}
