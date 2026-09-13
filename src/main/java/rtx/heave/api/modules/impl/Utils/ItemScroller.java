package rtx.heave.api.modules.impl.Utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import rtx.heave.api.modules.Category;
import rtx.heave.api.modules.Module;
import rtx.heave.api.modules.ModuleManager;
import rtx.heave.api.modules.settings.impl.BooleanSetting;
import rtx.heave.api.modules.settings.impl.SeparatorSetting;

public final class ItemScroller extends Module {
    private static ItemScroller instance;

    private final SeparatorSetting scrollGroup = this.register(new SeparatorSetting("Прокрутка"));
    public final BooleanSetting scrollSingle = this.register(
        new BooleanSetting("По одному предмету", "Перемещение по одному предмету при обычном скролле.", true)
    );
    public final BooleanSetting scrollStacks = this.register(
        new BooleanSetting("Стаки (Shift)", "Перемещение стака при скролле с зажатым Shift.", true)
    );
    public final BooleanSetting scrollMatching = this.register(
        new BooleanSetting("Одинаковые (Ctrl)", "Перемещение всех одинаковых предметов при скролле с зажатым Ctrl.", true)
    );
    public final BooleanSetting scrollAll = this.register(
        new BooleanSetting("Все предметы (Ctrl+Shift)", "Перемещение абсолютно всех предметов при скролле с Ctrl+Shift.", true)
    );
    public final BooleanSetting reverseScroll = this.register(
        new BooleanSetting("Инверсия скролла", "Инвертировать направление прокрутки колесика.", false)
    );

    private final SeparatorSetting dragGroup = this.register(new SeparatorSetting("Перетаскивание"));
    public final BooleanSetting dragMove = this.register(
        new BooleanSetting("Перемещение (Shift+ЛКМ)", "Быстрое перемещение стаков при зажатых Shift и ЛКМ при движении курсора.", true)
    );
    public final BooleanSetting dropDrag = this.register(
        new BooleanSetting("Выбрасывание (Q+курсор)", "Выбрасывание предметов из слотов при зажатой клавише Q и наведении.", true)
    );

    private static Slot lastHoveredSlot = null;

    public ItemScroller() {
        super("Item Scroller", "Удобное управление инвентарем через колесико мыши и быстрое перемещение (Masa ItemScroller).", Category.UTILS);
        instance = this;
    }

    public static ItemScroller getInstance() {
        return instance;
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    public static boolean hasShiftDown() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    public static boolean hasControlDown() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    public static boolean isKeyDown(int key) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return false;
        long handle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }

    public static boolean onMouseScrolled(HandledScreen<?> screen, Slot slot, double horizontalAmount, double verticalAmount) {
        ItemScroller scroller = ModuleManager.get().get(ItemScroller.class);
        if (scroller == null || !scroller.isEnabled() || slot == null) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) {
            return false;
        }
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            return false;
        }

        boolean up = verticalAmount > 0;
        if (scroller.reverseScroll.getValue()) {
            up = !up;
        }

        boolean shift = hasShiftDown();
        boolean ctrl = hasControlDown();

        if (ctrl && shift) {
            if (!scroller.scrollAll.getValue()) return false;
            if (up) {
                moveAllStacks(mc, screen, slot, false);
            } else {
                moveAllStacks(mc, screen, slot, true);
            }
            return true;
        }

        if (ctrl) {
            if (!scroller.scrollMatching.getValue()) return false;
            if (!slot.hasStack()) return false;
            if (up) {
                moveMatchingStacks(mc, screen, slot, false);
            } else {
                moveMatchingStacks(mc, screen, slot, true);
            }
            return true;
        }

        if (shift) {
            if (!scroller.scrollStacks.getValue()) return false;
            if (up) {
                pullOneMatchingStack(mc, screen, slot);
            } else {
                if (slot.hasStack()) {
                    mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                }
            }
            return true;
        }

        if (!scroller.scrollSingle.getValue()) return false;
        if (up) {
            pullSingleItemFromOther(mc, screen, slot);
        } else {
            moveSingleItemToOther(mc, screen, slot);
        }
        return true;
    }

    public static void onSlotHovered(HandledScreen<?> screen, Slot slot) {
        ItemScroller scroller = ModuleManager.get().get(ItemScroller.class);
        if (scroller == null || !scroller.isEnabled() || slot == null) {
            lastHoveredSlot = slot;
            return;
        }
        if (slot == lastHoveredSlot) {
            return;
        }
        lastHoveredSlot = slot;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;
        if (mc.getWindow() == null) return;
        long window = mc.getWindow().getHandle();

        // 1. Drop Drag: 'Q' key held
        if (scroller.dropDrag.getValue() && isKeyDown(GLFW.GLFW_KEY_Q) && slot.hasStack()) {
            int button = hasControlDown() ? 1 : 0;
            mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, button, SlotActionType.THROW, mc.player);
            return;
        }

        // 2. Drag Move: Shift + Left Mouse Button held
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (scroller.dragMove.getValue() && hasShiftDown() && leftDown && slot.hasStack()) {
            if (hasControlDown()) {
                moveMatchingStacks(mc, screen, slot, true);
            } else {
                mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
        }
    }

    public static boolean areInSameInventory(Slot slot1, Slot slot2) {
        if (slot1 == null || slot2 == null) return false;
        if (slot1.inventory != slot2.inventory) {
            return false;
        }
        if (slot1.inventory instanceof net.minecraft.entity.player.PlayerInventory) {
            int idx1 = slot1.getIndex();
            int idx2 = slot2.getIndex();
            if (idx1 < 9 && idx2 >= 9) return false;
            if (idx1 >= 9 && idx2 < 9) return false;
        }
        return true;
    }

    public static boolean moveSingleItemToOther(MinecraftClient mc, HandledScreen<?> screen, Slot slot) {
        if (slot == null || !slot.hasStack() || mc.interactionManager == null || mc.player == null) {
            return false;
        }
        ScreenHandler handler = screen.getScreenHandler();
        int syncId = handler.syncId;
        int slotId = slot.id;
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) return false;

        if (stack.getCount() > 1) {
            mc.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, slotId, 1, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
            mc.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.PICKUP, mc.player);
        } else {
            mc.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
        return true;
    }

    public static boolean pullSingleItemFromOther(MinecraftClient mc, HandledScreen<?> screen, Slot targetSlot) {
        if (targetSlot == null || mc.interactionManager == null || mc.player == null) {
            return false;
        }
        ScreenHandler handler = screen.getScreenHandler();
        int syncId = handler.syncId;
        ItemStack targetStack = targetSlot.getStack();
        if (targetStack.isEmpty()) {
            return false;
        }
        if (targetStack.getCount() >= targetSlot.getMaxItemCount(targetStack)) {
            return false;
        }

        for (int i = handler.slots.size() - 1; i >= 0; i--) {
            Slot srcSlot = handler.slots.get(i);
            if (!areInSameInventory(srcSlot, targetSlot) && srcSlot.hasStack()) {
                ItemStack srcStack = srcSlot.getStack();
                if (ItemStack.areItemsAndComponentsEqual(srcStack, targetStack)) {
                    int srcId = srcSlot.id;
                    int targetId = targetSlot.id;
                    if (srcStack.getCount() > 1) {
                        mc.interactionManager.clickSlot(syncId, srcId, 1, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(syncId, targetId, 1, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(syncId, srcId, 0, SlotActionType.PICKUP, mc.player);
                    } else {
                        mc.interactionManager.clickSlot(syncId, srcId, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(syncId, targetId, 0, SlotActionType.PICKUP, mc.player);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static void pullOneMatchingStack(MinecraftClient mc, HandledScreen<?> screen, Slot targetSlot) {
        if (targetSlot == null || mc.interactionManager == null || mc.player == null) {
            return;
        }
        ScreenHandler handler = screen.getScreenHandler();
        int syncId = handler.syncId;
        ItemStack targetStack = targetSlot.getStack();
        if (targetStack.isEmpty()) return;

        for (int i = handler.slots.size() - 1; i >= 0; i--) {
            Slot slot = handler.slots.get(i);
            if (!areInSameInventory(slot, targetSlot) && slot.hasStack()) {
                if (ItemStack.areItemsAndComponentsEqual(slot.getStack(), targetStack)) {
                    mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                    return;
                }
            }
        }
    }

    public static void moveMatchingStacks(MinecraftClient mc, HandledScreen<?> screen, Slot targetSlot, boolean toOther) {
        if (targetSlot == null || !targetSlot.hasStack() || mc.interactionManager == null || mc.player == null) {
            return;
        }
        ScreenHandler handler = screen.getScreenHandler();
        int syncId = handler.syncId;
        ItemStack targetStack = targetSlot.getStack();

        for (int i = handler.slots.size() - 1; i >= 0; i--) {
            Slot slot = handler.slots.get(i);
            if (slot.id != targetSlot.id && areInSameInventory(slot, targetSlot) == toOther && slot.hasStack()) {
                if (ItemStack.areItemsAndComponentsEqual(slot.getStack(), targetStack)) {
                    mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
                }
            }
        }
        if (toOther && targetSlot.hasStack()) {
            mc.interactionManager.clickSlot(syncId, targetSlot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }

    public static void moveAllStacks(MinecraftClient mc, HandledScreen<?> screen, Slot targetSlot, boolean toOther) {
        if (targetSlot == null || mc.interactionManager == null || mc.player == null) {
            return;
        }
        ScreenHandler handler = screen.getScreenHandler();
        int syncId = handler.syncId;

        for (int i = handler.slots.size() - 1; i >= 0; i--) {
            Slot slot = handler.slots.get(i);
            if (slot.id != targetSlot.id && areInSameInventory(slot, targetSlot) == toOther && slot.hasStack()) {
                mc.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
        }
        if (toOther && targetSlot.hasStack()) {
            mc.interactionManager.clickSlot(syncId, targetSlot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        }
    }
}
