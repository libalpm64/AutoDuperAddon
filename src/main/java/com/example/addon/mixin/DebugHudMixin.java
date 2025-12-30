package com.example.addon.mixin;

import com.example.addon.modules.CoordinateSpoofer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ListIterator;

@Mixin(DebugHud.class)
public class DebugHudMixin {
    @Inject(method = "getLeftText", at = @At("RETURN"))
    private void onGetLeftText(CallbackInfoReturnable<List<String>> info) {
        CoordinateSpoofer module = Modules.get().get(CoordinateSpoofer.class);
        if (module == null || !module.isActive()) return;

        List<String> list = info.getReturnValue();
        ListIterator<String> it = list.listIterator();

        while (it.hasNext()) {
            String s = it.next();
            if (s.startsWith("XYZ: ")) {
                it.set(String.format("XYZ: %d.000 / %d.00000 / %d.000", module.x.get(), module.y.get(), module.z.get()));
            } else if (s.startsWith("Block: ")) {
                it.set(String.format("Block: %d %d %d", module.x.get(), module.y.get(), module.z.get()));
            } else if (s.startsWith("Chunk: ")) {
                it.set(String.format("Chunk: %d %d %d in %d %d %d", module.x.get() >> 4, module.y.get() >> 4, module.z.get() >> 4, module.x.get() & 15, module.y.get(), module.z.get() & 15));
            }
        }
    }
}
