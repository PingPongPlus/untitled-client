package pingplus.voicechat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;

public final class HandSwapFeature {
    private static final int INTERVAL_TICKS = 1;

    private boolean wasEnabled;
    private int ticks;
    private HumanoidArm savedMainArm = HumanoidArm.RIGHT;

    public void tick(Minecraft client) {
        if (client.player == null) {
            wasEnabled = false;
            ticks = 0;
            return;
        }

        if (PlayerSettings.handSwap) {
            if (!wasEnabled) {
                savedMainArm = client.player.getMainArm();
                ticks = 0;
                client.player.setMainArm(savedMainArm.getOpposite());
            } else {
                ticks++;
                if (ticks >= INTERVAL_TICKS) {
                    ticks = 0;
                    client.player.setMainArm(client.player.getMainArm().getOpposite());
                }
            }
        } else if (wasEnabled) {
            client.player.setMainArm(savedMainArm);
            ticks = 0;
        }
        wasEnabled = PlayerSettings.handSwap;
    }
}
