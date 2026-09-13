package rtx.heave.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.heave.api.drags.components.CooldownsComp;
import rtx.heave.api.events.EventBus;
import rtx.heave.api.events.impl.inventory.HandledScreenEvent;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.impl.Interface.CooldownsModule;
import rtx.heave.api.modules.impl.Visuals.BetterHud;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    @Nullable
    protected Slot focusedSlot;
    @Unique
    private boolean heave_panelAnimated;

    @Inject(method="init", at={@At(value="TAIL")}, require = 0)
    private void heave_trackInventoryOpen(CallbackInfo ci) {
        BetterHud.markInventoryOpen();
    }

    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    private void heave_animatePanel(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        this.heave_panelAnimated = BetterHud.inventoryAnimationEnabled();
        if (this.heave_panelAnimated) {
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(0.0f, BetterHud.inventorySlideOffset());
        }
    }

    @Inject(method="render", at={@At(value="TAIL")}, require = 0)
    private void heave_endPanelAnimation(DrawContext graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.heave_panelAnimated) {
            this.heave_panelAnimated = false;
            graphics.getMatrices().popMatrix();
        }
        EventBus.get().post(new HandledScreenEvent(graphics, this.x, this.y, this.focusedSlot));
        if (this.focusedSlot != null) {
            rtx.heave.api.modules.impl.Utils.ItemScroller.onSlotHovered((HandledScreen<?>)(Object)this, this.focusedSlot);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 0)
    private void heave_itemScrollerScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (rtx.heave.api.modules.impl.Utils.ItemScroller.onMouseScrolled((HandledScreen<?>)(Object)this, this.focusedSlot, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), require = 0)
    private void heave_itemScrollerDrag(net.minecraft.client.gui.Click click, double offsetX, double offsetY, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (this.focusedSlot != null) {
            rtx.heave.api.modules.impl.Utils.ItemScroller.onSlotHovered((HandledScreen<?>)(Object)this, this.focusedSlot);
        }
    }

    @Inject(method = "drawSlot", at = @At("TAIL"), require = 0)
    private void heave_drawSlotCooldown(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (slot == null || !slot.hasStack()) return;
        CooldownsModule mod = ModuleManager.get().get(CooldownsModule.class);
        if (mod == null || !mod.isEnabled() || !mod.showInInventory.getValue()) return;

        long remainingMs = CooldownsComp.getCooldownRemainingMs(slot.getStack());
        if (remainingMs > 0) {
            float sec = (float) remainingMs / 1000.0f;
            String text = String.valueOf((int) Math.ceil(sec));
            int sx = slot.x;
            int sy = slot.y;
            context.getMatrices().pushMatrix();
            Render2D.beginFrame(context);
            float tw = Fonts.SF.width(text, 6.5f);
            float tx = sx + 16.0f - tw - 1.0f;
            float ty = sy + 1.0f;
            int color = mod.cooldownColor.getColor();
            Fonts.SF.draw(text, tx, ty, 6.5f, color);
            Render2D.flush();
            context.getMatrices().popMatrix();
        }
    }
}
