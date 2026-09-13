package rtx.heave.utils.render.targetesp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class ProjectionUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Vector2f worldToScreen(Vec3d worldPos) {
        if (mc == null || mc.gameRenderer == null || mc.gameRenderer.getCamera() == null || mc.getWindow() == null) {
            return null;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        Vec3d delta = worldPos.subtract(camPos);

        Quaternionf camRot = new Quaternionf(camera.getRotation()).conjugate();
        Vector3f rotated = delta.toVector3f();
        rotated.rotate(camRot);

        if (rotated.z < 0.0f) {
            float halfW = (float) mc.getWindow().getScaledWidth() / 2.0f;
            float halfH = (float) mc.getWindow().getScaledHeight() / 2.0f;
            float fov = (float) mc.options.getFov().getValue().intValue();
            float f4 = (float) ((double) halfH / ((double) -rotated.z * Math.tan(Math.toRadians((double) (fov / 2.0f)))));
            float screenX = halfW + rotated.x * f4;
            float screenY = halfH - rotated.y * f4;

            if (screenX >= -64.0f && screenX <= (float) mc.getWindow().getScaledWidth() + 64.0f
                && screenY >= -64.0f && screenY <= (float) mc.getWindow().getScaledHeight() + 64.0f) {
                return new Vector2f(screenX, screenY);
            }
        }
        return null;
    }

    public static float getScaledSize(Vec3d worldPos) {
        if (mc == null || mc.gameRenderer == null || mc.gameRenderer.getCamera() == null) {
            return 0.0f;
        }
        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        double dist = camPos.distanceTo(worldPos);
        if (dist > 0.01) {
            double fov = (double) mc.options.getFov().getValue().intValue();
            double base = Math.max(10.0, 1000.0 / dist) * 0.5;
            double fovFactor = (fov != 70.0) ? (fov / 70.0) : 1.0;
            return (float) (base / fovFactor);
        }
        return 0.0f;
    }
}
