package rtx.heave.api.drags.components;
import rtx.heave.api.events.EventHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import rtx.heave.api.drags.DragSystem;
import rtx.heave.api.drags.components.ListHudComp;
import rtx.heave.api.drags.components.ListHudComp.Row;
import rtx.heave.api.events.EventBus;
import rtx.heave.api.events.impl.network.PacketEvent;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.CooldownsModule;
import rtx.heave.api.modules.impl.Utils.HolyWorldHelper;
import rtx.heave.utils.render.render2d.Render2DCoordinateSpace;

public final class CooldownsComp
extends ListHudComp {
    private static final Item[] PREVIEW_ITEMS = new Item[]{Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.MACE};
    private final Map<Identifier, Cooldown> cooldowns = new ConcurrentHashMap<Identifier, Cooldown>();
    private long previewSwitchMs;
    private int previewIndex;
    private static final float ITEM_SCALE = 0.5f;

    private static CooldownsComp instance;

    public CooldownsComp() {
        super("cooldowns", "Cooldowns", CooldownsModule.class, 118.0f, 82.0f);
        instance = this;
        EventBus.get().subscribe(this);
    }

    public static CooldownsComp getInstance() {
        return instance;
    }

    public static long getCooldownRemainingMs(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return 0;
        ItemCooldownManager cooldownManager = mc.player.getItemCooldownManager();
        if (!cooldownManager.isCoolingDown(stack)) {
            return 0;
        }
        if (instance != null) {
            Identifier group = cooldownManager.getGroup(stack);
            Cooldown cd = instance.cooldowns.get(group);
            if (cd != null) {
                long rem = cd.endMs - System.currentTimeMillis();
                if (rem > 0) return rem;
            }
        }
        float progress = cooldownManager.getCooldownProgress(stack, 0.0f);
        if (progress > 0.0f) {
            return (long)(progress * 1000.0f);
        }
        return 0;
    }

    @Override
    protected String headerIconGlyph() {
        return "i";
    }

    @Override
    protected List<ListHudComp.Row> collectRows() {
        long l = System.currentTimeMillis();
        if (MinecraftClient.getInstance().world == null) {
            this.cooldowns.clear();
        }
        CooldownsModule mod = ModuleManager.get().get(CooldownsModule.class);
        if (mod != null && mod.isEnabled() && mod.displayMode.is("Хотбар") && !DragSystem.get().isDragModeActive()) {
            return java.util.Collections.emptyList();
        }
        ArrayList<ListHudComp.Row> arrayList = new ArrayList<ListHudComp.Row>();
        Iterator<Cooldown> iterator = this.cooldowns.values().iterator();
        while (iterator.hasNext()) {
            Cooldown cooldown = iterator.next();
            long l2 = cooldown.endMs - l;
            if (l2 <= 0L) {
                iterator.remove();
                continue;
            }
            ItemStack itemStack = CooldownsComp.resolveCooldownStack(cooldown.group);
            if (itemStack.isEmpty()) continue;
            ItemStack itemStack2 = itemStack;
            String string = CooldownsComp.cleanName(itemStack.getName().getString());
            ListHudComp.IconDrawer iconDrawer = (drawContext, f, f2, f3, f4) -> CooldownsComp.drawItemIcon(drawContext, itemStack2, f, f2, f3, f4);
            arrayList.add(new ListHudComp.Row((Object)cooldown.group, string, CooldownsComp.formatRemaining(l2), iconDrawer));
        }

        HolyWorldHelper hw = ModuleManager.get().get(HolyWorldHelper.class);
        if (hw != null && hw.isEnabled()) {
            for (HolyWorldHelper.ActiveZone zone : hw.getActiveZones()) {
                double remainingSec = zone.getRemainingSeconds();
                if (remainingSec > 0.0) {
                    ItemStack iconStack = new ItemStack(zone.type.fallbackItem);
                    arrayList.add(new ListHudComp.Row(
                        (Object)("hw_" + zone.type.nbtName),
                        zone.type.displayName,
                        CooldownsComp.formatRemaining((long)(remainingSec * 1000.0)),
                        (drawContext, f, f2, f3, f4) -> CooldownsComp.drawItemIcon(drawContext, iconStack, f, f2, f3, f4)
                    ));
                }
            }
        }


        if (arrayList.isEmpty() && DragSystem.get().isDragModeActive()) {
            arrayList.add(this.previewRow(l));
        }
        return arrayList;
    }

    private static String formatRemaining(long l) {
        float f = (float)l / 1000.0f;
        if (f >= 60.0f) {
            int n = Math.round(f);
            return n / 60 + ":" + String.format(Locale.ROOT, "%02d", n % 60);
        }
        return String.format(Locale.ROOT, "%.1fs", Float.valueOf(f));
    }

    private static ItemStack resolveCooldownStack(Identifier identifier) {
        ClientPlayerEntity clientPlayerEntity = MinecraftClient.getInstance().player;
        if (clientPlayerEntity != null) {
            ItemCooldownManager itemCooldownManager = clientPlayerEntity.getItemCooldownManager();
            PlayerInventory playerInventory = clientPlayerEntity.getInventory();
            int n = playerInventory.size();
            for (int i = 0; i < n; ++i) {
                ItemStack itemStack = playerInventory.getStack(i);
                if (itemStack.isEmpty() || !identifier.equals((Object)itemCooldownManager.getGroup(itemStack))) continue;
                return itemStack;
            }
        }
        Item item = Registries.ITEM.get(identifier);
        if (item != null && item != Items.AIR) {
            return item.getDefaultStack();
        }
        return ItemStack.EMPTY;
    }

    private static void drawItemIcon(DrawContext drawContext, ItemStack itemStack, float f, float f2, float f3, float f4) {
        float f5 = Math.max(0.0f, Math.min(1.0f, f4));
        if (itemStack == null || itemStack.isEmpty() || f5 <= 0.01f) {
            return;
        }
        float f6 = 0.5f * f5;
        float f7 = f + f3 * 0.5f;
        float f8 = f2 + f3 * 0.5f - 0.5f;
        drawContext.getMatrices().pushMatrix();
        Render2DCoordinateSpace.applyGuiScaleIndependence((Matrix3x2f)drawContext.getMatrices());
        drawContext.getMatrices().translate(f7, f8);
        drawContext.getMatrices().scale(f6, f6);
        drawContext.getMatrices().translate(-8.0f, -8.0f);
        drawContext.drawItem(itemStack, 0, 0);
        drawContext.getMatrices().popMatrix();
    }

    @EventHandler
    private void onPacket(PacketEvent packetEvent) {
        if (!packetEvent.isReceive() || !(packetEvent.getPacket() instanceof CooldownUpdateS2CPacket cooldownUpdateS2CPacket)) {
            return;
        }
        Identifier identifier = cooldownUpdateS2CPacket.cooldownGroup();
        if (identifier == null) {
            return;
        }
        if (cooldownUpdateS2CPacket.cooldown() <= 0) {
            this.cooldowns.remove(identifier);
            return;
        }
        long l = System.currentTimeMillis() + (long)cooldownUpdateS2CPacket.cooldown() * 50L;
        this.cooldowns.put(identifier, new Cooldown(identifier, l));
    }

    private static String cleanName(String string) {
        if (string == null) {
            return "";
        }
        return string.replaceAll("\\[[^\\[\\]]*\\]", " ").replaceAll("<[^<>]*>", " ").replaceAll("\\{[^{}]*\\}", " ").replaceAll("\\s+", " ").trim();
    }

    private ListHudComp.Row previewRow(long l) {
        if (l - this.previewSwitchMs >= 1000L) {
            this.previewIndex = (this.previewIndex + 1) % PREVIEW_ITEMS.length;
            this.previewSwitchMs = l;
        }
        ItemStack itemStack = PREVIEW_ITEMS[this.previewIndex].getDefaultStack();
        return new ListHudComp.Row((Object)"preview", "Example cooldown", "**:**", (drawContext, f, f2, f3, f4) -> CooldownsComp.drawItemIcon(drawContext, itemStack, f, f2, f3, f4), null);
    }

    private record Cooldown(Identifier group, long endMs) {}
}

