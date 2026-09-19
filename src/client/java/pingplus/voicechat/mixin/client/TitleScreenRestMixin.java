package pingplus.voicechat.mixin.client;


import com.mojang.realmsclient.gui.screens.RealmsNotificationsScreen;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.gui.components.SplashRenderer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.security.auth.callback.Callback;

@Mixin(TitleScreen.class)
public abstract class TitleScreenRestMixin extends Screen {
    protected TitleScreenRestMixin(Component title){
        super(title);
    }
    @Shadow private SplashRenderer splash;
    @Shadow private FriendsButton friends;
    @Shadow private RealmsNotificationsScreen realmsNotificationsScreen;
    @Shadow @Final private static Component COPYRIGHT_TEXT;

    @Shadow private boolean realmsNotificationsEnabled() { return false; }
    @Shadow private int createDemoMenuOptions(int topPos, int spacing) { return 0; }
    @Shadow private int createNormalMenuOptions(int topPos, int spacing) { return 0; }
    @Shadow private int getHorizontalPosition(int currentButton, int numberOfButtons, int buttonWidth) { return 0; }
    @Inject(
            method = "init",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onInit(CallbackInfo ci){

        if (this.splash == null) {
            this.splash = this.minecraft.gui.splashManager().getSplash();
        }

        int copyrightWidth = this.font.width(COPYRIGHT_TEXT);
        int copyrightX = this.width - copyrightWidth - 2;
        int spacing = 24;
        int topPos = this.height / 4 + 48;
        if (this.minecraft.isDemo()) {
            topPos = this.createDemoMenuOptions(topPos, 24);
        } else {
            topPos = this.createNormalMenuOptions(topPos, 24);
        }

        int numberOfButtons = 3;
        int currentButton = 0;
        topPos += 24;

        this.friends = (FriendsButton)this.addRenderableWidget(CommonButtons.friends(20, (var1) -> OnlineOptionsScreen.confirmFriendsListEnabled(this.minecraft, () -> this.minecraft.gui.setScreen(new FriendsOverlayScreen(this)), this), !this.minecraft.isDemo()));
        ++currentButton;
        this.friends.setPosition(this.getHorizontalPosition(currentButton, 3, 20), topPos);
        SpriteIconButton language = (SpriteIconButton)this.addRenderableWidget(CommonButtons.language(20, (var1) -> this.minecraft.gui.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), true));
        ++currentButton;
        language.setPosition(this.getHorizontalPosition(currentButton, 3, 20), topPos);
        SpriteIconButton accessibility = (SpriteIconButton)this.addRenderableWidget(CommonButtons.accessibility(20, (var1) -> this.minecraft.gui.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), true));
        ++currentButton;
        accessibility.setPosition(this.getHorizontalPosition(currentButton, 3, 20), topPos);
        Button.Builder var10001 = Button.builder(Component.translatable("menu.options"), (var1) -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options, false)));
        int var10002 = this.width / 2 - 100;
        topPos += 24;
        this.addRenderableWidget(var10001.bounds(var10002, topPos, 98, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("menu.quit"), (var1) -> this.minecraft.stop()).bounds(this.width / 2 + 2, topPos, 98, 20).build());
        this.addRenderableWidget(new PlainTextButton(copyrightX, this.height - 10, copyrightWidth, 10, COPYRIGHT_TEXT, (var1) -> this.minecraft.gui.setScreen(new CreditsAndAttributionScreen(this)), this.font));
        if (this.realmsNotificationsScreen == null) {
            this.realmsNotificationsScreen = new RealmsNotificationsScreen();
        }

        if (this.realmsNotificationsEnabled()) {
            this.realmsNotificationsScreen.init(this.width, this.height);
        }
        ci.cancel();

    }

}


