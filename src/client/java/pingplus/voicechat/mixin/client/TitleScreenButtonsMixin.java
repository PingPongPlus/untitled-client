package pingplus.voicechat.mixin.client;


import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pingplus.voicechat.client.gui.TextOnlyButton;

// Mixin for changing Minecraft starter menu Buttons
// including singleplayer, multiplayer and Realms
// Other stuff like options friends etc are not considered here
@Mixin(TitleScreen.class)
public abstract class TitleScreenButtonsMixin extends Screen {


    protected TitleScreenButtonsMixin(Component title) {
        super(title);
    }

    @Shadow
    private Component getMultiplayerDisabledReason() {
        throw new AssertionError("Mixin shadow");
    }

    @Inject(
            method = "createNormalMenuOptions(II)I",
            at = @At("HEAD"),
            cancellable = true
    )
    private void createCustomMenuOptions(
    int topPos,
    int spacing,
    CallbackInfoReturnable<Integer> cir
) {



        Button singleplayerButton = addRenderableWidget(new TextOnlyButton(
                width / 2 - 100, topPos, 200, 20,
                Component.translatable("menu.singleplayer"),
                button -> minecraft.gui.setScreen(new SelectWorldScreen(this))
        ));
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            addRenderableWidget(new TextOnlyButton(
                    singleplayerButton.getX() + singleplayerButton.getWidth() + 2,
                    topPos, 20, 20,
                    Component.literal("TW"),
                    button -> CreateWorldScreen.testWorld(minecraft, () -> minecraft.gui.setScreen(this))
            ));
        }

        Component multiplayerDisabledReason = this.getMultiplayerDisabledReason();
        boolean multiplayerAllowed = multiplayerDisabledReason == null;
        Tooltip tooltip = multiplayerDisabledReason != null ? Tooltip.create(multiplayerDisabledReason) : null;
        int multiplayerY = topPos + spacing;
        Button multiplayerButton =
                addRenderableWidget(new TextOnlyButton(
                width / 2 - 100, multiplayerY, 200, 20,
                Component.translatable("menu.multiplayer"), button -> {


                    Screen screen;
                    /*
                        This is the original Minecraft Multiplayer warning source i ll remove this cus
                        its annoying af lol

                    if (minecraft.options.skipMultiplayerWarning) {
                        screen = new JoinMultiplayerScreen(this);
                    } else {
                        screen = new SafetyScreen(this);
                    }

                     */
                    screen = new JoinMultiplayerScreen(this);
                    minecraft.gui.setScreen(screen);
                }



        ));
        multiplayerButton.setTooltip(tooltip);
        multiplayerButton.active = multiplayerAllowed;
        topPos = multiplayerY + spacing;
        Button realmsButton = addRenderableWidget(new TextOnlyButton(
                width / 2 - 100, topPos, 200, 20,
                Component.translatable("menu.online"),
                button -> minecraft.gui.setScreen(new RealmsMainScreen(this))
        ));
        realmsButton.setTooltip(tooltip);
        realmsButton.active = multiplayerAllowed;


        cir.setReturnValue(topPos);
        // Add your custom buttons here.

        // Skip the original method and return the last button's Y position.
    }
}
