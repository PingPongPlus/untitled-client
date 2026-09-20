package pingplus.voicechat.mixin.client;


import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pingplus.voicechat.client.ServerIPLookup;
import net.minecraft.client.gui.screens.ConfirmScreen;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


// Button to lookup the real IP of a server

@Mixin(JoinMultiplayerScreen.class)
public class JoinMultiplayerScreenMixin extends Screen {
    protected JoinMultiplayerScreenMixin(Component title){
        super(title);
    }
    @Shadow
    protected ServerSelectionList serverSelectionList;

    @Inject(method = "init", at = @At("TAIL"))
        private void nsLookupButton(CallbackInfo ci) {
        this.addRenderableWidget(
                Button.builder(Component.literal("nslookup"), button -> {
                            var selected = this.serverSelectionList.getSelected();

                            if (!(selected instanceof ServerSelectionList.OnlineServerEntry entry)) {
                                button.setMessage(Component.literal("Select a server"));
                                return;
                            }

                            var server = entry.getServerData();
                            var client = this.minecraft;

                            button.active = false;
                            button.setMessage(Component.literal("Looking up..."));

                            CompletableFuture
                                    .supplyAsync(() -> new ServerIPLookup().returnIp(server))
                                    .whenComplete((ips, error) -> client.execute(() -> {
                                        button.active = true;
                                        button.setMessage(Component.literal("nslookup"));

                                        if (client.gui.screen() != this || !this.children().contains(button)) {
                                            return;
                                        }

                                        boolean success =
                                                error == null && ips != null && !ips.isEmpty();

                                        String result = success
                                                ? ips.stream()
                                                        .map(ServerIPLookup.ServerInfo::displayText)
                                                        .collect(Collectors.joining("\n\n"))
                                                : "Could not resolve the server address.";

                                        client.gui.setScreen(new ConfirmScreen(
                                                copy -> {
                                                    if (copy && success) {
                                                        client.keyboardHandler.setClipboard(ips.stream()
                                                                .map(ServerIPLookup.ServerInfo::address)
                                                                .collect(Collectors.joining("\n")));
                                                    }

                                                    client.gui.setScreen(this);
                                                },
                                                Component.literal("Server address"),
                                                Component.literal(result),
                                                Component.literal(success ? "Copy" : "OK"),
                                                Component.literal("Done")
                                        ));
                                    }));
                        })
                        .bounds(5, 5, 100, 20)
                        .build()
        );
    }
}
