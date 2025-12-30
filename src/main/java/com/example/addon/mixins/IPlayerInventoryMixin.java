package com.example.addon.mixins;

import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInventory.class)
public interface IPlayerInventoryMixin {
    @Accessor("selectedSlot")
    int getSelectedSlot();

    @Accessor("selectedSlot")
    void setSelectedSlot(int slot);
}
