package rtx.heave.api.modules.impl.Visuals;

import java.util.Locale;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rtx.heave.api.events.EventHandler;
import rtx.heave.api.events.impl.player.AttackEntityEvent;
import rtx.heave.api.events.impl.render.HudRenderEvent;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.SliderSetting;

public final class HealthIndicator extends Module {
    private static HealthIndicator instance;
    private final BooleanSetting nametag = this.register(new BooleanSetting("Nametag", "Отображает здоровье в неймтеге игрока.", true));
    private final BooleanSetting crosshair = this.register(new BooleanSetting("Crosshair", "Отображает здоровье под прицелом.", true));
    private final SliderSetting yOffset = this.register(
        new SliderSetting("Смещение по Y", "Смещение отображения здоровья по вертикали.")
            .range(-100.0f, 100.0f)
            .increment(1.0f)
            .setValue(0.0f)
            .visible(() -> this.crosshair.getValue())
    );
    private static final long SHOW_TIME_MS = 10000L;
    private PlayerEntity lastTarget;
    private long lastAttackMs;

    public HealthIndicator() {
        super("Health Indicator", "Отображает здоровье игроков под прицелом и в неймтегах.", Category.VISUALS);
        instance = this;
    }

    public static HealthIndicator getInstance() {
        return instance != null ? instance : ModuleManager.get().get(HealthIndicator.class);
    }

    public Text decorate(Text name, LivingEntity entity) {
        if (name == null || !this.isEnabled() || !this.nametag.getValue()) {
            return name;
        }
        if (!(entity instanceof PlayerEntity)) {
            return name;
        }
        float health = entity.getHealth();
        String string = String.format(Locale.ROOT, "%.1f", health);
        return Text.empty().append(name).append(Text.literal(" " + string).formatted(this.getFormatting(health)));
    }

    @EventHandler
    public void onAttack(AttackEntityEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Entity target = event.getTarget();
        if (target instanceof PlayerEntity player) {
            this.lastTarget = player;
            this.lastAttackMs = System.currentTimeMillis();
        } else {
            this.lastAttackMs = 0L;
        }
    }

    @EventHandler
    public void onRender2D(HudRenderEvent event) {
        if (!this.isEnabled() || !this.crosshair.getValue()) {
            return;
        }
        if (this.lastTarget == null || System.currentTimeMillis() - this.lastAttackMs > SHOW_TIME_MS) {
            return;
        }
        DrawContext context = event.getGraphics();
        if (context == null || this.mc.textRenderer == null) {
            return;
        }
        int health = (int) this.lastTarget.getHealth();
        MutableText text = Text.literal(Integer.toString(health)).formatted(this.getFormatting(health));
        int textWidth = this.mc.textRenderer.getWidth(text);
        int centerX = this.mc.getWindow().getScaledWidth() / 2;
        int centerY = this.mc.getWindow().getScaledHeight() / 2;
        int x = centerX - (textWidth / 2);
        int y = centerY + 10 + (int) this.yOffset.getValue();

        int color = this.getHealthColor(health);
        context.drawTextWithShadow(this.mc.textRenderer, text, x, y, color);
    }

    @Override
    protected void onDisable() {
        this.lastTarget = null;
        this.lastAttackMs = 0L;
    }

    private int getHealthColor(float health) {
        if (health <= 5.0f) {
            return 0xFFFF3333;
        }
        if (health <= 10.0f) {
            return 0xFFFF5555;
        }
        if (health <= 15.0f) {
            return 0xFFFFAA00;
        }
        if (health <= 20.0f) {
            return 0xFF55FF55;
        }
        return 0xFF00AA00;
    }

    private Formatting getFormatting(float health) {
        if (health <= 5.0f) {
            return Formatting.DARK_RED;
        }
        if (health <= 10.0f) {
            return Formatting.RED;
        }
        if (health <= 15.0f) {
            return Formatting.GOLD;
        }
        if (health <= 20.0f) {
            return Formatting.GREEN;
        }
        return Formatting.DARK_GREEN;
    }
}
