package dev.no.name.bdadvance.mixin;

import dev.no.name.afriends.compat.Compat;
import dev.no.name.bdadvance.battlepass.BattlePassHook;
import dev.no.name.bdadvance.modes.ModeLauncher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ScreenOpenMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void bdadvance$hideServerMenu(Screen screen, CallbackInfo callback) {
        if (ModeLauncher.hideScreen(screen)) {
            callback.cancel();
            return;
        }
        Screen replacement = BattlePassHook.replace(screen);
        if (replacement != screen) {
            callback.cancel();
            if (replacement != null) {
                Compat.setScreen(replacement);
            }
        }
    }
}
