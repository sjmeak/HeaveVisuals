package rtx.heave.api.crosshair;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class CrosshairHud {
    private static CrosshairHud INSTANCE;
    public static final int CANVAS_SIZE = 15;
    private static final Identifier VANILLA_CROSSHAIR = Identifier.ofVanilla("hud/crosshair");
    private static final Identifier CUSTOM_CROSSHAIR_ID = Identifier.of("heave", "textures/dynamic_crosshair.png");
    private static final Identifier ENTITY_CROSSHAIR_ID = Identifier.of("heave", "textures/dynamic_crosshair_entity.png");

    private NativeImageBackedTexture dynamicTexture;
    private NativeImage dynamicImage;
    private final int[] lastUploadedPixels = new int[CANVAS_SIZE * CANVAS_SIZE];
    private String lastUploadedStyle = "";

    private NativeImageBackedTexture entityDynamicTexture;
    private NativeImage entityDynamicImage;
    private final int[] lastUploadedEntityPixels = new int[CANVAS_SIZE * CANVAS_SIZE];
    private String lastUploadedEntityStyle = "";

    private CrosshairHud() {}

    public static CrosshairHud get() {
        if (INSTANCE == null) INSTANCE = new CrosshairHud();
        return INSTANCE;
    }

    private CrosshairMode getMode(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit != null) {
            if (hit.getType() == HitResult.Type.ENTITY) {
                return CrosshairMode.ENTITY;
            }
            if (hit.getType() == HitResult.Type.BLOCK && client.world != null && hit instanceof BlockHitResult blockHit) {
                BlockPos pos = blockHit.getBlockPos();
                BlockState blockState = client.world.getBlockState(pos);
                Block block = blockState.getBlock();
                if (block instanceof BlockEntityProvider || client.world.getBlockEntity(pos) != null) {
                    return CrosshairMode.CONTAINER;
                }
            }
        }
        return CrosshairMode.DEFAULT;
    }

    private void ensureDynamicTexture(MinecraftClient client, String style, int[] pixels) {
        if (this.dynamicImage == null) {
            this.dynamicImage = new NativeImage(CANVAS_SIZE, CANVAS_SIZE, false);
            this.dynamicTexture = new NativeImageBackedTexture(() -> "heave_dynamic_crosshair", this.dynamicImage);
            client.getTextureManager().registerTexture(CUSTOM_CROSSHAIR_ID, this.dynamicTexture);
        }

        boolean needUpdate = false;
        if (!style.equals(this.lastUploadedStyle)) {
            needUpdate = true;
            this.lastUploadedStyle = style;
        }

        if ("CUSTOM".equalsIgnoreCase(style)) {
            if (pixels != null && !Arrays.equals(pixels, this.lastUploadedPixels)) {
                needUpdate = true;
                System.arraycopy(pixels, 0, this.lastUploadedPixels, 0, CANVAS_SIZE * CANVAS_SIZE);
            }
        }

        if (needUpdate) {
            uploadToImage(this.dynamicImage, style, pixels);
            this.dynamicTexture.upload();
        }
    }

    private void ensureEntityDynamicTexture(MinecraftClient client, String style, int[] pixels) {
        if (this.entityDynamicImage == null) {
            this.entityDynamicImage = new NativeImage(CANVAS_SIZE, CANVAS_SIZE, false);
            this.entityDynamicTexture = new NativeImageBackedTexture(() -> "heave_dynamic_crosshair_entity", this.entityDynamicImage);
            client.getTextureManager().registerTexture(ENTITY_CROSSHAIR_ID, this.entityDynamicTexture);
        }

        boolean needUpdate = false;
        if (!style.equals(this.lastUploadedEntityStyle)) {
            needUpdate = true;
            this.lastUploadedEntityStyle = style;
        }

        if ("CUSTOM".equalsIgnoreCase(style)) {
            if (pixels != null && !Arrays.equals(pixels, this.lastUploadedEntityPixels)) {
                needUpdate = true;
                System.arraycopy(pixels, 0, this.lastUploadedEntityPixels, 0, CANVAS_SIZE * CANVAS_SIZE);
            }
        }

        if (needUpdate) {
            uploadToImage(this.entityDynamicImage, style, pixels);
            this.entityDynamicTexture.upload();
        }
    }

    private void uploadToImage(NativeImage image, String style, int[] pixels) {
        for (int y = 0; y < CANVAS_SIZE; y++) {
            for (int x = 0; x < CANVAS_SIZE; x++) {
                image.setColor(x, y, 0);
            }
        }

        if ("DOT".equalsIgnoreCase(style)) {
            for (int dy = 6; dy <= 8; dy++) {
                for (int dx = 6; dx <= 8; dx++) {
                    image.setColor(dx, dy, 0xFFFFFFFF);
                }
            }
        } else if ("CUSTOM".equalsIgnoreCase(style) && pixels != null) {
            for (int y = 0; y < CANVAS_SIZE; y++) {
                for (int x = 0; x < CANVAS_SIZE; x++) {
                    int pixel = pixels[y * CANVAS_SIZE + x];
                    if (pixel != 0) {
                        int a = (pixel >> 24) & 0xFF;
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                        image.setColor(x, y, abgr);
                    }
                }
            }
        }
    }

    private static final Identifier ATTACK_INDICATOR_FULL = Identifier.ofVanilla("hud/crosshair_attack_indicator_full");
    private static final Identifier ATTACK_INDICATOR_BG = Identifier.ofVanilla("hud/crosshair_attack_indicator_background");
    private static final Identifier ATTACK_INDICATOR_PROGRESS = Identifier.ofVanilla("hud/crosshair_attack_indicator_progress");

    public void render(DrawContext ctx) {
        CrosshairConfig cfg = CrosshairConfig.get();
        if (!cfg.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int cx = (sw - 15) / 2;
        int cy = (sh - 15) / 2;
        float centerX = (float) cx + 7.5f;
        float centerY = (float) cy + 7.5f;

        double scale = cfg.canvasScale > 0.0 ? cfg.canvasScale : 1.0;
        String style = cfg.style != null ? cfg.style : "CUSTOM";

        CrosshairMode mode = getMode(client);
        int rawColor = switch (mode) {
            case ENTITY -> cfg.getEntityColorInt();
            case CONTAINER -> cfg.getContainerColorInt();
            default -> cfg.getColorInt();
        };

        rawColor = 0xFF000000 | (rawColor & 0x00FFFFFF);

        boolean blend = cfg.applyBlend && (rawColor == 0xFFFFFFFF);
        RenderPipeline pipeline = blend ? RenderPipelines.CROSSHAIR : RenderPipelines.GUI_TEXTURED;

        ctx.createNewRootLayer();
        ctx.getMatrices().pushMatrix();
        if (Math.abs(scale - 1.0) > 0.001) {
            ctx.getMatrices().translate(centerX, centerY);
            ctx.getMatrices().scale((float) scale, (float) scale);
            ctx.getMatrices().translate(-centerX, -centerY);
        }

        if ("CROSS".equalsIgnoreCase(style)) {
            if (blend) {
                ctx.drawGuiTexture(pipeline, VANILLA_CROSSHAIR, cx, cy, 15, 15);
            } else {
                ctx.drawGuiTexture(pipeline, VANILLA_CROSSHAIR, cx, cy, 15, 15, rawColor);
            }
        } else {
            Identifier texId = CUSTOM_CROSSHAIR_ID;
            if (mode == CrosshairMode.ENTITY) {
                ensureEntityDynamicTexture(client, style, cfg.entityCanvasPixels);
                texId = ENTITY_CROSSHAIR_ID;
            } else {
                ensureDynamicTexture(client, style, cfg.canvasPixels);
                texId = CUSTOM_CROSSHAIR_ID;
            }

            if (blend) {
                ctx.drawTexture(pipeline, texId, cx, cy, 0.0F, 0.0F, 15, 15, 15, 15);
            } else {
                ctx.drawTexture(pipeline, texId, cx, cy, 0.0F, 0.0F, 15, 15, 15, 15, rawColor);
            }
        }

        ctx.getMatrices().popMatrix();

        if (client.options != null && client.options.getAttackIndicator().getValue() == net.minecraft.client.option.AttackIndicator.CROSSHAIR && client.player != null) {
            float f = client.player.getAttackCooldownProgress(0.0F);
            boolean bl = false;
            if (client.targetedEntity instanceof net.minecraft.entity.LivingEntity && f >= 1.0F) {
                bl = client.player.getAttackCooldownProgressPerTick() > 5.0F;
                bl &= client.targetedEntity.isAlive();
                net.minecraft.component.type.AttackRangeComponent attackRange = client.player.getActiveOrMainHandStack().get(net.minecraft.component.DataComponentTypes.ATTACK_RANGE);
                bl &= (attackRange == null || attackRange.isWithinRange(client.player, client.crosshairTarget.getPos()));
            }

            int j = sh / 2 - 7 + 16;
            int k = sw / 2 - 8;
            if (bl) {
                ctx.drawGuiTexture(RenderPipelines.CROSSHAIR, ATTACK_INDICATOR_FULL, k, j, 16, 16);
            } else if (f < 1.0F) {
                int l = (int) (f * 17.0F);
                ctx.drawGuiTexture(RenderPipelines.CROSSHAIR, ATTACK_INDICATOR_BG, k, j, 16, 4);
                ctx.drawGuiTexture(RenderPipelines.CROSSHAIR, ATTACK_INDICATOR_PROGRESS, 16, 4, 0, 0, k, j, l, 4);
            }
        }

        ctx.createNewRootLayer();
    }

    private enum CrosshairMode {
        DEFAULT, ENTITY, CONTAINER
    }
}
