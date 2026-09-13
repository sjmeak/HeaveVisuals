package rtx.heave.api.drags.components;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import rtx.heave.api.drags.DragSystem;
import rtx.heave.api.drags.Draggable;
import rtx.heave.api.drags.Position;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.ArmorModule;
import rtx.heave.api.ui.settings.Setting;
import rtx.heave.api.ui.settings.SettingsFactory;
import rtx.heave.utils.animations.Easings;
import rtx.heave.utils.animations.SmoothAnimation;
import rtx.heave.utils.color.ColorUtil;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.others.RectUtil;
import rtx.heave.utils.render.render2d.Render2D;
import rtx.heave.utils.render.render2d.Render2DCoordinateSpace;

public final class ArmorComp
extends Draggable {
    private static final Identifier HOTBAR_TEXTURE = Identifier.ofVanilla("hud/hotbar");
    private static final float ITEM = 16.0f;
    private static final float PAD = 2.0f;
    private static final float RADIUS = 4.0f;
    private static final float EDGE_ENTER = 16.0f;
    private static final float EDGE_EXIT = 26.0f;
    private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final SmoothAnimation visibility = new SmoothAnimation();
    private int side;
    private int previewSide;
    private int sideBeforeDrag;
    private boolean wasDragging;
    private boolean sideResolved;
    private float width = 20.0f;
    private float height = 20.0f;
    private float previewWidth = this.width;
    private float previewHeight = this.height;
    private List<ItemStack> shownItems = List.of();

    public ArmorComp() {
        super("armor", 166.0f, 5.0f);
        this.visibility.set(0.0);
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    @Override
    public float overlayHeight() {
        return this.previewHeight;
    }

    @Override
    public boolean isInteractive() {
        ArmorModule armorModule = ModuleManager.get().get(ArmorModule.class);
        if (armorModule != null && armorModule.style.is(ArmorModule.STYLE_UKU)) {
            return false;
        }
        return ArmorComp.componentEnabled();
    }

    @Override
    public float overlayWidth() {
        return this.previewWidth;
    }

    @Override
    protected List<Setting> buildHudSettings() {
        List<Setting> list = super.buildHudSettings();
        ArmorModule armorModule = ModuleManager.get().get(ArmorModule.class);
        if (armorModule != null) {
            for (rtx.heave.api.modules.settings.Setting s : armorModule.getSettings().all()) {
                Setting uiSetting = SettingsFactory.create(s);
                if (uiSetting != null) {
                    list.add(uiSetting);
                }
            }
        }
        return list;
    }

    private static boolean componentEnabled() {
        ArmorModule armorModule = ModuleManager.get().get(ArmorModule.class);
        return armorModule != null && armorModule.isEnabled();
    }

    private static int dockFromPosition(float f, float f2, float f3) {
        if (f >= f3 - f2 - 5.0f - 1.0f) {
            return 1;
        }
        if (f <= 6.0f) {
            return -1;
        }
        return 0;
    }

    private static float orientWidth(int n, float f, float f2) {
        return n != 0 ? f2 : f;
    }

    private static List<ItemStack> previewItems(ArmorModule mod) {
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>(4);
        ItemStack helm = Items.DIAMOND_HELMET.getDefaultStack();
        ItemStack chest = Items.DIAMOND_CHESTPLATE.getDefaultStack();
        chest.setDamage(40);
        ItemStack legs = Items.DIAMOND_LEGGINGS.getDefaultStack();
        legs.setDamage(120);
        ItemStack boots = Items.DIAMOND_BOOTS.getDefaultStack();
        boots.setDamage(260);
        arrayList.add(helm);
        arrayList.add(chest);
        arrayList.add(legs);
        arrayList.add(boots);
        return arrayList;
    }

    private static float orientHeight(int n, float f, float f2) {
        return n != 0 ? f : f2;
    }

    private static List<ItemStack> collectItems(ArmorModule mod) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        ArrayList<ItemStack> arrayList = new ArrayList<ItemStack>(4);
        if (minecraftClient.player != null) {
            for (EquipmentSlot equipmentSlot : ARMOR) {
                ItemStack itemStack = minecraftClient.player.getEquippedStack(equipmentSlot);
                if (itemStack.isEmpty()) continue;
                arrayList.add(itemStack);
            }
        }
        return arrayList;
    }

    @Override
    protected void render(DrawContext drawContext) {
        ArmorModule armorModule = ModuleManager.get().get(ArmorModule.class);
        List<ItemStack> list;
        boolean bl = ArmorComp.componentEnabled();
        boolean bl2 = DragSystem.get().isDragModeActive();
        List<ItemStack> list2 = list = bl ? ArmorComp.collectItems(armorModule) : List.of();
        if (list.isEmpty() && bl && bl2) {
            list = ArmorComp.previewItems(armorModule);
        }
        if (!list.isEmpty()) {
            this.shownItems = list;
        }
        boolean bl3 = bl && !list.isEmpty();
        this.visibility.run(bl3 ? 1.0 : 0.0, bl3 ? 0.18 : 0.12, Easings.CUBIC_OUT, true);
        this.visibility.update();
        float f = this.visibility.get();
        if (f <= 0.01f || this.shownItems.isEmpty()) {
            return;
        }
        if (armorModule != null && armorModule.style.is(ArmorModule.STYLE_UKU)) {
            this.renderUku(drawContext, armorModule, this.shownItems, f);
            return;
        }
        String orient = (armorModule != null) ? armorModule.orientation.getSelected() : ArmorModule.ORIENT_AUTO;
        String durMode = (armorModule != null) ? armorModule.durabilityMode.getSelected() : ArmorModule.DUR_PERCENT;
        String sideMode = (armorModule != null) ? armorModule.side.getSelected() : ArmorModule.SIDE_AUTO;
        boolean showDurText = !ArmorModule.DUR_HIDDEN.equals(durMode);
        boolean showBg = (armorModule == null || armorModule.background.getValue());

        boolean isVert;
        if (ArmorModule.ORIENT_HORIZ.equals(orient)) {
            isVert = false;
        } else if (ArmorModule.ORIENT_VERT.equals(orient)) {
            isVert = true;
        } else {
            isVert = this.side != 0;
        }

        boolean useVanillaFont = (armorModule != null && armorModule.vanillaFont.getValue());

        List<ItemStack> list3 = this.shownItems;
        // Tight spacing: items close to each other (16px icon + minimal gap)
        float slotStep = isVert ? (showDurText ? 16.0f : 14.0f) : (showDurText ? 16.5f : 14.5f);
        float f2 = 4.0f + (float)list3.size() * slotStep;
        float f3 = isVert ? (showDurText ? 38.0f : 18.0f) : (showDurText ? 26.0f : 18.0f);
        float f4 = Math.max(1.0f, Position.screenWidth());
        boolean bl4 = f2 <= f4 - 10.0f;
        this.updateSide(this.getDrag().isDragging(), bl4, f3, f4);

        // Apply side override from settings
        if (!this.getDrag().isDragging()) {
            if (ArmorModule.SIDE_LEFT.equals(sideMode)) {
                this.getDrag().setTargetX(5.0f);
            } else if (ArmorModule.SIDE_RIGHT.equals(sideMode)) {
                this.getDrag().setTargetX(f4 - f3 - 5.0f);
            }
        }

        this.width = isVert ? f3 : f2;
        this.height = isVert ? f2 : f3;
        this.previewWidth = this.width;
        this.previewHeight = this.height;
        float f5 = this.getX();
        float f6 = this.getY();
        float f7 = 0.96f + f * 0.04f;
        float f8 = f5 + this.width * 0.5f;
        float f9 = f6 + this.height * 0.5f;
        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(f8, f9);
        drawContext.getMatrices().scale(f7);
        drawContext.getMatrices().translate(-8.0f, -8.0f);
        drawContext.getMatrices().translate(-f8 + 8.0f, -f9 + 8.0f);

        if (showBg) {
            Render2D.beginFrame(drawContext);
            RectUtil.drawClientRect(f5, f6, this.width, this.height, 4.0f, f);
            Render2D.flush();
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        float f10 = 8.0f;
        float f11 = 1.0f * Math.max(0.0f, Math.min(1.0f, f));
        float f12 = 0.0f;
        for (ItemStack itemStack : list3) {
            float f13 = f5 + 2.0f + (isVert ? 0.0f : f12);
            float f14 = f6 + 2.0f + (isVert ? f12 : 0.0f);
            drawContext.getMatrices().pushMatrix();
            Render2DCoordinateSpace.applyGuiScaleIndependence((Matrix3x2f)drawContext.getMatrices());
            drawContext.getMatrices().translate(f13 + f10, f14 + f10);
            drawContext.getMatrices().scale(f11, f11);
            drawContext.getMatrices().translate(-8.0f, -8.0f);
            drawContext.drawItem(itemStack, 0, 0);
            drawContext.drawStackOverlay(textRenderer, itemStack, 0, 0);
            drawContext.getMatrices().popMatrix();

            if (showDurText && itemStack.isDamageable()) {
                int maxDamage = itemStack.getMaxDamage();
                int damage = itemStack.getDamage();
                int curDurability = Math.max(0, maxDamage - damage);
                String durText;
                if (ArmorModule.DUR_PERCENT.equals(durMode)) {
                    int percent = Math.round(((float) curDurability / (float) maxDamage) * 100.0f);
                    durText = percent + "%";
                } else {
                    durText = String.valueOf(curDurability);
                }

                int durColor = (curDurability < maxDamage * 0.25f) ? 0xFFFF5555 : (curDurability < maxDamage * 0.5f ? 0xFFFFAA00 : 0xFFFFFFFF);
                if (useVanillaFont) {
                    if (isVert) {
                        drawContext.drawText(textRenderer, durText, (int)(f13 + 19.0f), (int)(f14 + 4.5f), durColor, true);
                    } else {
                        int textW = textRenderer.getWidth(durText);
                        drawContext.drawText(textRenderer, durText, (int)(f13 + (16.0f - textW) / 2.0f), (int)(f14 + 17.0f), durColor, true);
                    }
                } else {
                    Render2D.beginFrame(drawContext);
                    if (isVert) {
                        Fonts.SF.draw(durText, f13 + 19.0f, f14 + 5.0f, 7.5f, durColor);
                    } else {
                        float textW = Fonts.SF.width(durText, 7.0f);
                        Fonts.SF.draw(durText, f13 + (16.0f - textW) / 2.0f, f14 + 17.0f, 7.0f, durColor);
                    }
                    Render2D.flush();
                }
            }
            f12 += slotStep;
        }
        drawContext.getMatrices().popMatrix();
    }

    private static int edgeSide(float f, float f2, int n) {
        if (n > 0) {
            return f < f2 - 26.0f ? 0 : 1;
        }
        if (n < 0) {
            return f > 26.0f ? 0 : -1;
        }
        if (f >= f2 - 16.0f) {
            return 1;
        }
        if (f <= 16.0f) {
            return -1;
        }
        return 0;
    }

    private void updateSide(boolean bl, boolean bl2, float f, float f2) {
        boolean bl3 = !this.wasDragging && bl;
        boolean bl4 = this.wasDragging && !bl;
        this.wasDragging = bl;
        if (bl3) {
            this.sideBeforeDrag = this.side;
        }
        if (bl) {
            int n = bl2 ? ArmorComp.edgeSide(Position.mouseX(), f2, this.previewSide) : 0;
            if (n != 0 != (this.previewSide != 0)) {
                this.getDrag().swapGrabOffset();
            }
            this.previewSide = n;
            this.sideResolved = true;
            return;
        }
        if (bl4) {
            this.side = this.getDrag().wasCancelled() ? this.sideBeforeDrag : this.previewSide;
        } else if (!this.sideResolved) {
            this.side = bl2 ? ArmorComp.dockFromPosition(this.getDrag().getTargetX(), f, f2) : 0;
            this.sideResolved = true;
        }
        this.previewSide = this.side;
        if (this.side > 0) {
            this.getDrag().setTargetX(f2 - f - 5.0f);
        } else if (this.side < 0) {
            this.getDrag().setTargetX(5.0f);
        }
    }

    private void renderUku(DrawContext drawContext, ArmorModule armorModule, List<ItemStack> list, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean dragMode = DragSystem.get().isDragModeActive();
        boolean showDamage = armorModule.ukuShowDamage.getValue();
        boolean showBg = armorModule.ukuBackground.getValue();
        String durFormat = armorModule.ukuDamageFormat.getSelected();
        String colorScheme = armorModule.ukuDamageColor.getSelected();

        // Exact 1.21.8 layout: 4 slots hotbar segment (82px wide), fixed next to hotbar
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        int hotbarStartX = screenW / 2 - 91;
        int hotbarEndX = screenW / 2 + 91;

        float x;
        if (ArmorModule.UKU_SIDE_LEFT.equals(armorModule.ukuSide.getSelected())) {
            boolean hasOffhand = mc.player != null && !mc.player.getOffHandStack().isEmpty();
            x = hotbarStartX - 82.0f - (hasOffhand ? 35.0f : 9.0f);
        } else {
            x = hotbarEndX + 9.0f;
        }
        float y = screenH - 22.0f;

        this.width = 82.0f;
        this.height = 22.0f;
        this.previewWidth = this.width;
        this.previewHeight = this.height;

        this.getDrag().setTargetX(x);
        this.getDrag().setTargetY(y);
        this.getDrag().syncToTarget();

        float slotsY = y;

        int alphaInt = Math.max(0, Math.min(255, Math.round(255.0f * alpha)));

        if (showBg) {
            try {
                drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, 182, 22, 0, 0, (int)x, (int)slotsY, 81, 22);
                drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, 182, 22, 181, 0, (int)(x + 81), (int)slotsY, 1, 22);
            } catch (Throwable t) {
                Render2D.beginFrame(drawContext);
                Render2D.rect(x, slotsY, 82.0f, 22.0f, 4.0f, 0x80000000);
                Render2D.flush();
            }
        }

        // 4 armor slots: HEAD, CHEST, LEGS, FEET
        for (int i = 0; i < 4; i++) {
            ItemStack stack = ItemStack.EMPTY;
            if (mc.player != null) {
                stack = mc.player.getEquippedStack(ARMOR[i]);
            }
            if (stack.isEmpty() && dragMode) {
                stack = previewItems(armorModule).get(i);
            }
            if (stack.isEmpty()) continue;

            int slotX = (int)x + 3 + i * 20;
            int slotY = (int)slotsY + 3;

            // Draw item and overlay
            drawContext.drawItem(stack, slotX, slotY);
            drawContext.drawStackOverlay(mc.textRenderer, stack, slotX, slotY, null);

            // Draw numerical durability above slot exactly as in 1.21.8
            if (showDamage && stack.isDamageable()) {
                int max = stack.getMaxDamage();
                int cur = Math.max(0, max - stack.getDamage());
                float pct = (float)cur / (float)max;

                String durText = "Проценты".equals(durFormat) ? (Math.round(pct * 100.0f) + "%") : String.valueOf(cur);

                int color;
                if ("Белый".equals(colorScheme)) {
                    color = 0xFFFFFFFF;
                } else if ("Цвет бара".equals(colorScheme)) {
                    color = 0xFF000000 | (stack.getItemBarColor() & 0xFFFFFF);
                } else {
                    // Exact 1.21.8 3-stage durability color
                    if (pct > 0.55f) {
                        color = 0xFF55FF55;
                    } else if (pct > 0.25f) {
                        color = 0xFFFFAA00;
                    } else {
                        color = 0xFFFF5555;
                    }
                }
                color = (alphaInt << 24) | (color & 0xFFFFFF);

                float durScale = 0.88f;
                drawContext.getMatrices().pushMatrix();
                drawContext.getMatrices().scale(durScale, durScale);
                int textW = mc.textRenderer.getWidth(durText);
                int drawX = Math.round(((float)slotX + 8.0f) / durScale - (float)textW / 2.0f);
                int drawY = Math.round(((float)slotsY - 5.5f) / durScale);
                drawContext.drawText(mc.textRenderer, durText, drawX, drawY, color, true);
                drawContext.getMatrices().popMatrix();
            }
        }
    }
}

