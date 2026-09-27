package pingplus.voicechat.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * Hold-to-look feature: while the key is held the camera switches to third
 * person and orbits the player freely; the player itself does not turn.
 */
public final class ShoulderCamFeature {
    private static boolean active;
    private static CameraType savedType = CameraType.FIRST_PERSON;
    private static float cameraYaw;
    private static float cameraPitch;

    public static void tick(Minecraft client, boolean down) {
        boolean valid = PlayerSettings.shoulderCam && down && client.player != null && client.gui.screen() == null;
        if (valid && !active) {
            savedType = client.options.getCameraType();
            cameraYaw = client.player.getYRot();
            cameraPitch = client.player.getXRot();
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!valid && active) {
            client.options.setCameraType(savedType);
        }
        active = valid;
    }

    public static boolean active() { return active; }

    public static float cameraYaw() { return cameraYaw; }

    public static float cameraPitch() { return cameraPitch; }

    public static void rotate(double cursorDeltaX, double cursorDeltaY) {
        cameraYaw += (float) (cursorDeltaX * 0.15);
        cameraPitch = Mth.clamp(cameraPitch + (float) (cursorDeltaY * 0.15), -90.0f, 90.0f);
    }

    private ShoulderCamFeature() {}
}
