package rtx.heave.utils.render.post.guilayerblur;

public final class GuiCapture {
    private static GuiCapture.Source currentSource;

    private GuiCapture() {
    }

    public static float scale() {
        return currentSource != null ? currentSource.captureScale() : 1.0f;
    }

    public static boolean isBound(GuiCapture.Source s) {
        return currentSource != null && currentSource == s;
    }

    public static void bind(GuiCapture.Source s) {
        currentSource = s;
    }

    public static boolean active() {
        return currentSource != null && currentSource.captureActive();
    }

    public static float blurRadius() {
        return currentSource != null ? currentSource.captureBlurRadius() : 0.0f;
    }

    public static float shatterProgress() {
        return currentSource != null ? currentSource.shatterProgress() : 0.0f;
    }

    public static boolean emitPanelBoundary() {
        return currentSource == null || currentSource.emitPanelBoundary();
    }

    public static interface Source {
        public float shatterProgress();
    
        public float captureScale();
    
        public boolean captureActive();
    
        public float captureBlurRadius();
    
        default public boolean emitPanelBoundary() {
            return true;
        }
    }
}
