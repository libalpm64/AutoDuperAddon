package com.example.addon.modules;

import com.example.addon.Main_Addon;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class CoordinateSpoofer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> x = sgGeneral.add(new IntSetting.Builder()
        .name("x")
        .description("The spoofed X coordinate.")
        .defaultValue(0)
        .build()
    );

    public final Setting<Integer> y = sgGeneral.add(new IntSetting.Builder()
        .name("y")
        .description("The spoofed Y coordinate.")
        .defaultValue(0)
        .build()
    );

    public final Setting<Integer> z = sgGeneral.add(new IntSetting.Builder()
        .name("z")
        .description("The spoofed Z coordinate.")
        .defaultValue(0)
        .build()
    );

    public CoordinateSpoofer() {
        super(Main_Addon.CATEGORY, "coordinate-spoofer", "Spoofs your coordinates in the Debug HUD.");
    }
}
