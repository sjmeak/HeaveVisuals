package rtx.heave.api.crosshair;

import java.util.Arrays;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import rtx.heave.api.ui.theme.ThemeManager;
import rtx.heave.utils.render.fonts.Fonts;
import rtx.heave.utils.render.render2d.Render2D;

public final class CrosshairEditorScreen extends Screen {
    private static final int GRID = 15;
    private static final int CELL_SIZE = 10;
    private static final int CANVAS_W = GRID * CELL_SIZE; // 150 px
    private static final int WIN_W = 460;
    private static final int WIN_H = 226;

    private static final int[] PALETTE = new int[] {
        0xFFFFFFFF,
        0xFFFF3333,
        0xFF33FF33,
        0xFF3388FF,
        0xFFFFAA00,
        0xFFFFFF33,
        0xFFFF33FF,
        0xFF33FFFF
    };

    private final Screen parent;
    private final int[] normalPixels = new int[GRID * GRID];
    private final int[] entityPixels = new int[GRID * GRID];

    private int brushColor = 0xFFFFFFFF;
    private int selectedGrid = 1; // 1 = Normal, 2 = Entity
    private int activeDrawTarget = 0; // 0 = None, 1 = Normal, 2 = Entity
    private boolean isDrawing = false;
    private boolean isErasing = false;

    public CrosshairEditorScreen(Screen parent) {
        super(Text.literal("Редактор прицела"));
        this.parent = parent;
        CrosshairConfig cfg = CrosshairConfig.get();
        if (cfg.canvasPixels != null && cfg.canvasPixels.length == GRID * GRID) {
            System.arraycopy(cfg.canvasPixels, 0, this.normalPixels, 0, GRID * GRID);
        } else {
            initDefaultNormal(this.normalPixels);
        }
        if (cfg.entityCanvasPixels != null && cfg.entityCanvasPixels.length == GRID * GRID) {
            System.arraycopy(cfg.entityCanvasPixels, 0, this.entityPixels, 0, GRID * GRID);
        } else {
            initDefaultEntity(this.entityPixels);
        }
    }

    private static void initDefaultNormal(int[] arr) {
        Arrays.fill(arr, 0);
        int white = 0xFFFFFFFF;
        arr[7 * 15 + 7] = white;
        arr[7 * 15 + 5] = white;
        arr[7 * 15 + 6] = white;
        arr[7 * 15 + 8] = white;
        arr[7 * 15 + 9] = white;
        arr[5 * 15 + 7] = white;
        arr[6 * 15 + 7] = white;
        arr[8 * 15 + 7] = white;
        arr[9 * 15 + 7] = white;
    }

    private static void initDefaultEntity(int[] arr) {
        Arrays.fill(arr, 0);
        int red = 0xFFFF4444;
        arr[7 * 15 + 7] = red;
        arr[7 * 15 + 5] = red;
        arr[7 * 15 + 6] = red;
        arr[7 * 15 + 8] = red;
        arr[7 * 15 + 9] = red;
        arr[5 * 15 + 7] = red;
        arr[6 * 15 + 7] = red;
        arr[8 * 15 + 7] = red;
        arr[9 * 15 + 7] = red;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float sw = (float) this.width;
        float sh = (float) this.height;

        Render2D.beginFrame(context);

        // Dim background
        Render2D.rect(0, 0, sw, sh, 0.0f, 0x88000000);

        float winX = (sw - WIN_W) / 2.0f;
        float winY = (sh - WIN_H) / 2.0f;

        // Main window backdrop
        Render2D.rect(winX, winY, WIN_W, WIN_H, 8.0f, 0xF5111116);
        Render2D.outline(winX, winY, WIN_W, WIN_H, 8.0f, 1.0f, ThemeManager.accent(80));

        // Header
        Fonts.SF_BOLD.draw("Редактор прицела (15x15)", winX + 14.0f, winY + 9.0f, 9.5f, 0xFFFFFFFF);
        Fonts.SF_MEDIUM.draw("Двойная сетка: Обычный и Entity", winX + 14.0f + Fonts.SF_BOLD.width("Редактор прицела (15x15)", 9.5f) + 8.0f, winY + 10.5f, 7.5f, 0x88FFFFFF);

        float grid1X = winX + 14.0f;
        float grid1Y = winY + 34.0f;
        float grid2X = grid1X + CANVAS_W + 14.0f;
        float grid2Y = grid1Y;

        // Labels above grids
        boolean sel1 = (selectedGrid == 1);
        Fonts.SF_BOLD.draw("Обычный прицел", grid1X + 2.0f, grid1Y - 11.0f, 7.5f, sel1 ? ThemeManager.accent(255) : 0xCCFFFFFF);
        if (sel1) {
            Render2D.rect(grid1X + 2.0f, grid1Y - 2.5f, 40.0f, 1.5f, 0.5f, ThemeManager.accent(255));
        }

        boolean sel2 = (selectedGrid == 2);
        Fonts.SF_BOLD.draw("На сущности (Entity)", grid2X + 2.0f, grid2Y - 11.0f, 7.5f, sel2 ? 0xFFFF4444 : 0xCCFF7777);
        if (sel2) {
            Render2D.rect(grid2X + 2.0f, grid2Y - 2.5f, 45.0f, 1.5f, 0.5f, 0xFFFF4444);
        }

        // Draw Grid 1 (Normal)
        renderGrid(grid1X, grid1Y, this.normalPixels, sel1 ? ThemeManager.accent(160) : 0x26FFFFFF);

        // Draw Grid 2 (Entity)
        renderGrid(grid2X, grid2Y, this.entityPixels, sel2 ? 0xAAFF4444 : 0x26FFFFFF);

        // Right controls
        float rightX = grid2X + CANVAS_W + 14.0f;
        float rightW = winX + WIN_W - rightX - 14.0f;

        // Dual mini-preview box
        float prevH = 46.0f;
        Render2D.rect(rightX, grid1Y, rightW, prevH, 4.0f, 0xFF09090D);
        Render2D.outline(rightX, grid1Y, rightW, prevH, 4.0f, 0.5f, 0x22FFFFFF);

        // Preview Normal (left)
        float prev1CenterX = rightX + (rightW / 4.0f);
        float prevCenterY = grid1Y + prevH / 2.0f - 3.0f;
        renderMiniPreview(this.normalPixels, prev1CenterX, prevCenterY);
        Fonts.SF.draw("Обычный", prev1CenterX - Fonts.SF.width("Обычный", 5.5f) / 2.0f, grid1Y + prevH - 8.0f, 5.5f, 0x88FFFFFF);

        // Divider in preview
        Render2D.rect(rightX + rightW / 2.0f, grid1Y + 4.0f, 1.0f, prevH - 8.0f, 0.0f, 0x22FFFFFF);

        // Preview Entity (right)
        float prev2CenterX = rightX + (rightW * 3.0f / 4.0f);
        renderMiniPreview(this.entityPixels, prev2CenterX, prevCenterY);
        Fonts.SF.draw("Entity", prev2CenterX - Fonts.SF.width("Entity", 5.5f) / 2.0f, grid1Y + prevH - 8.0f, 5.5f, 0xFFFF6666);

        // Palette
        float paletteY = grid1Y + prevH + 9.0f;
        Fonts.SF_MEDIUM.draw("Цвет кисти:", rightX, paletteY, 7.0f, 0xAAFFFFFF);

        float swatchY = paletteY + 11.0f;
        float swatchSize = 10.5f;
        float swatchGap = 3.0f;
        for (int i = 0; i < PALETTE.length; i++) {
            float sx = rightX + (i % 4) * (swatchSize + swatchGap);
            float sy = swatchY + (i / 4) * (swatchSize + swatchGap);
            int col = PALETTE[i];
            Render2D.rect(sx, sy, swatchSize, swatchSize, 2.5f, col);
            if (this.brushColor == col) {
                Render2D.outline(sx - 0.5f, sy - 0.5f, swatchSize + 1.0f, swatchSize + 1.0f, 3.0f, 1.0f, 0xFFFFFFFF);
            }
        }

        // Action Buttons
        float btnY = swatchY + 28.0f;
        float btnH = 13.0f;

        // Button: Copy Normal -> Entity
        drawButton(rightX, btnY, rightW, btnH, "В Entity ➔", mouseX, mouseY);

        float halfBtnW = (rightW - 2.0f) / 2.0f;
        drawButton(rightX, btnY + 16.0f, halfBtnW, btnH, "Крест", mouseX, mouseY);
        drawButton(rightX + halfBtnW + 2.0f, btnY + 16.0f, halfBtnW, btnH, "Точка", mouseX, mouseY);

        // Clear active grid button
        drawButton(rightX, btnY + 32.0f, rightW, btnH, selectedGrid == 1 ? "Очистить (Обычный)" : "Очистить (Entity)", mouseX, mouseY);

        // Save & Close button
        float saveY = winY + WIN_H - 22.0f;
        drawAccentButton(rightX, saveY, rightW, 15.0f, "Сохранить", mouseX, mouseY);

        // Bottom hints
        Fonts.SF.draw("ЛКМ - рисовать  |  ПКМ - стирать  |  Клик по сетке выбирает активную", grid1X, winY + WIN_H - 12.0f, 6.5f, 0x66FFFFFF);

        Render2D.flush();

        // Handle continuous drawing when dragging
        if (this.isDrawing || this.isErasing) {
            if (this.activeDrawTarget == 1) {
                paintCell(1, mouseX, mouseY, grid1X, grid1Y);
            } else if (this.activeDrawTarget == 2) {
                paintCell(2, mouseX, mouseY, grid2X, grid2Y);
            }
        }
    }

    private void renderGrid(float gx, float gy, int[] pixels, int borderCol) {
        Render2D.rect(gx - 2.0f, gy - 2.0f, CANVAS_W + 4.0f, CANVAS_W + 4.0f, 3.0f, 0xFF08080C);
        Render2D.outline(gx - 2.0f, gy - 2.0f, CANVAS_W + 4.0f, CANVAS_W + 4.0f, 3.0f, 0.8f, borderCol);

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                float cx = gx + x * CELL_SIZE;
                float cy = gy + y * CELL_SIZE;
                int pixel = pixels[y * GRID + x];

                boolean isCenter = (x == 7 && y == 7);
                int cellBg = (pixel != 0) ? pixel : (isCenter ? 0x28FFFFFF : 0x0CFFFFFF);

                Render2D.rect(cx, cy, CELL_SIZE - 1.0f, CELL_SIZE - 1.0f, 1.0f, cellBg);
                if (isCenter) {
                    Render2D.outline(cx, cy, CELL_SIZE - 1.0f, CELL_SIZE - 1.0f, 1.0f, 0.6f, 0x66FFFFFF);
                }
            }
        }
    }

    private void renderMiniPreview(int[] pixels, float cx, float cy) {
        float startX = cx - (GRID * 1.5f) / 2.0f;
        float startY = cy - (GRID * 1.5f) / 2.0f;
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int p = pixels[y * GRID + x];
                if (p != 0) {
                    Render2D.rect(startX + x * 1.5f, startY + y * 1.5f, 1.5f, 1.5f, 0.0f, p);
                }
            }
        }
    }

    private void drawButton(float x, float y, float w, float h, String label, int mx, int my) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        Render2D.rect(x, y, w, h, 3.0f, hov ? 0xFF2A2A36 : 0xFF1B1B22);
        Render2D.outline(x, y, w, h, 3.0f, 0.5f, 0x33FFFFFF);
        float tw = Fonts.SF_MEDIUM.width(label, 7.0f);
        Fonts.SF_MEDIUM.draw(label, x + (w - tw) / 2.0f, y + (h - 7.0f) / 2.0f + 0.5f, 7.0f, hov ? 0xFFFFFFFF : 0xCCFFFFFF);
    }

    private void drawAccentButton(float x, float y, float w, float h, String label, int mx, int my) {
        boolean hov = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int accent = ThemeManager.accent(hov ? 255 : 220);
        Render2D.rect(x, y, w, h, 3.0f, accent);
        float tw = Fonts.SF_BOLD.width(label, 7.5f);
        Fonts.SF_BOLD.draw(label, x + (w - tw) / 2.0f, y + (h - 7.5f) / 2.0f + 0.5f, 7.5f, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        float mouseX = (float) click.x();
        float mouseY = (float) click.y();
        int button = click.button();

        float sw = (float) this.width;
        float sh = (float) this.height;
        float winX = (sw - WIN_W) / 2.0f;
        float winY = (sh - WIN_H) / 2.0f;
        float grid1X = winX + 14.0f;
        float grid1Y = winY + 34.0f;
        float grid2X = grid1X + CANVAS_W + 14.0f;
        float grid2Y = grid1Y;

        // Check Grid 1 (Normal)
        if (mouseX >= grid1X && mouseX < grid1X + CANVAS_W && mouseY >= grid1Y && mouseY < grid1Y + CANVAS_W) {
            this.selectedGrid = 1;
            this.activeDrawTarget = 1;
            this.isDrawing = (button == 0);
            this.isErasing = (button == 1);
            paintCell(1, mouseX, mouseY, grid1X, grid1Y);
            return true;
        }

        // Check Grid 2 (Entity)
        if (mouseX >= grid2X && mouseX < grid2X + CANVAS_W && mouseY >= grid2Y && mouseY < grid2Y + CANVAS_W) {
            this.selectedGrid = 2;
            this.activeDrawTarget = 2;
            this.isDrawing = (button == 0);
            this.isErasing = (button == 1);
            paintCell(2, mouseX, mouseY, grid2X, grid2Y);
            return true;
        }

        float rightX = grid2X + CANVAS_W + 14.0f;
        float rightW = winX + WIN_W - rightX - 14.0f;
        float prevH = 46.0f;
        float paletteY = grid1Y + prevH + 9.0f;
        float swatchY = paletteY + 11.0f;
        float swatchSize = 10.5f;
        float swatchGap = 3.0f;

        // Swatches
        for (int i = 0; i < PALETTE.length; i++) {
            float sx = rightX + (i % 4) * (swatchSize + swatchGap);
            float sy = swatchY + (i / 4) * (swatchSize + swatchGap);
            if (mouseX >= sx && mouseX <= sx + swatchSize && mouseY >= sy && mouseY <= sy + swatchSize) {
                this.brushColor = PALETTE[i];
                return true;
            }
        }

        // Buttons
        float btnY = swatchY + 28.0f;
        float btnH = 13.0f;

        // Copy Normal -> Entity
        if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= btnY && mouseY <= btnY + btnH) {
            System.arraycopy(this.normalPixels, 0, this.entityPixels, 0, GRID * GRID);
            this.selectedGrid = 2;
            return true;
        }

        // Presets: Cross & Dot
        float halfBtnW = (rightW - 2.0f) / 2.0f;
        if (mouseX >= rightX && mouseX <= rightX + halfBtnW && mouseY >= btnY + 16.0f && mouseY <= btnY + 16.0f + btnH) {
            applyPresetCross(this.selectedGrid == 1 ? this.normalPixels : this.entityPixels);
            return true;
        }
        if (mouseX >= rightX + halfBtnW + 2.0f && mouseX <= rightX + rightW && mouseY >= btnY + 16.0f && mouseY <= btnY + 16.0f + btnH) {
            applyPresetDot(this.selectedGrid == 1 ? this.normalPixels : this.entityPixels);
            return true;
        }

        // Clear active grid
        if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= btnY + 32.0f && mouseY <= btnY + 32.0f + btnH) {
            if (this.selectedGrid == 1) {
                Arrays.fill(this.normalPixels, 0);
            } else {
                Arrays.fill(this.entityPixels, 0);
            }
            return true;
        }

        // Save & Close button
        float saveY = winY + WIN_H - 22.0f;
        if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= saveY && mouseY <= saveY + 15.0f) {
            saveAndClose();
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    private void paintCell(int target, float mx, float my, float gx, float gy) {
        int cellX = (int) ((mx - gx) / CELL_SIZE);
        int cellY = (int) ((my - gy) / CELL_SIZE);
        if (cellX >= 0 && cellX < GRID && cellY >= 0 && cellY < GRID) {
            int idx = cellY * GRID + cellX;
            int[] arr = (target == 1) ? this.normalPixels : this.entityPixels;
            if (this.isDrawing) {
                arr[idx] = this.brushColor;
            } else if (this.isErasing) {
                arr[idx] = 0;
            }
        }
    }

    private void applyPresetCross(int[] arr) {
        Arrays.fill(arr, 0);
        int col = this.brushColor != 0 ? this.brushColor : 0xFFFFFFFF;
        arr[7 * 15 + 7] = col;
        arr[7 * 15 + 5] = col;
        arr[7 * 15 + 6] = col;
        arr[7 * 15 + 8] = col;
        arr[7 * 15 + 9] = col;
        arr[5 * 15 + 7] = col;
        arr[6 * 15 + 7] = col;
        arr[8 * 15 + 7] = col;
        arr[9 * 15 + 7] = col;
    }

    private void applyPresetDot(int[] arr) {
        Arrays.fill(arr, 0);
        int col = this.brushColor != 0 ? this.brushColor : 0xFFFFFFFF;
        for (int dy = 6; dy <= 8; dy++) {
            for (int dx = 6; dx <= 8; dx++) {
                arr[dy * 15 + dx] = col;
            }
        }
    }

    private void saveAndClose() {
        CrosshairConfig cfg = CrosshairConfig.get();
        System.arraycopy(this.normalPixels, 0, cfg.canvasPixels, 0, GRID * GRID);
        System.arraycopy(this.entityPixels, 0, cfg.entityCanvasPixels, 0, GRID * GRID);
        CrosshairConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        this.isDrawing = false;
        this.isErasing = false;
        this.activeDrawTarget = 0;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.saveAndClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
