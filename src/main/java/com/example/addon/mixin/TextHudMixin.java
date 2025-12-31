package com.example.addon.mixin;

import com.example.addon.modules.CoordinateSpoofer;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Made by Libalpm64 
// This is for meteor's stateless starscript meteor hud is a pain to intercept.
// Spoofs the hud coordinates by intercepting the render method.
@Mixin(value = TextHud.class, remap = false)
public class TextHudMixin {
    @Unique
    private boolean skipping = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(HudRenderer renderer, CallbackInfo ci) {
        skipping = false;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/systems/hud/HudRenderer;text(Ljava/lang/String;DDLmeteordevelopment/meteorclient/utils/render/color/Color;ZD)D"))
    private double onRenderText(HudRenderer renderer, String text, double x, double y, Color color, boolean shadow, double scale) {
        CoordinateSpoofer module = Modules.get().get(CoordinateSpoofer.class);
        if (module == null || !module.isActive()) return renderer.text(text, x, y, color, shadow, scale);

        if (skipping) {
            // End of line or coordinate block if we are skipping we want to skip until the end of this element's section list.
            // This is to prevent the hud from taking back control.
            return x;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return renderer.text(text, x, y, color, shadow, scale);

        double spoofX = module.x.get();
        double spoofY = module.y.get();
        double spoofZ = module.z.get();

        if (text.startsWith("Pos: ")) {
            String primarySpoof = String.format("Pos: %.0f, %.0f, %.0f", spoofX, spoofY, spoofZ);
            skipping = true;
            return renderer.text(primarySpoof, x, y, color, shadow, scale);
        }

        String dimPath = mc.world.getRegistryKey().getValue().getPath();
        String targetPrefix = "";
        double factor = 1.0;

        if (dimPath.equals("overworld")) {
            targetPrefix = "Nether: ";
            factor = 0.125;
        } else if (dimPath.equals("the_nether")) {
            targetPrefix = "Overworld: ";
            factor = 8.0;
        }

        if (!targetPrefix.isEmpty() && text.startsWith(targetPrefix)) {
            String oppositeSpoof = String.format("%s%.0f, %.0f, %.0f", targetPrefix, spoofX * factor, spoofY, spoofZ * factor);
            skipping = true;
            return renderer.text(oppositeSpoof, x, y, color, shadow, scale);
        }

        return renderer.text(text, x, y, color, shadow, scale);
    }
}
