package com.example.addon;

import com.example.addon.modules.AutoIgnore;
import com.example.addon.modules.CoordinateSpoofer;
import com.example.addon.modules.DonkeyInstantDupe;
import com.example.addon.modules.DonkeyRider;
import com.example.addon.modules.ItemFrameDupe;
import com.example.addon.modules.LlamaDupe2bfr;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class Main_Addon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("Duper");

    @Override
    public void onInitialize() {
        LOG.info("Initializing 8b8t/6b6t AutoDuper");

        // Modules
        Modules.get().add(new DonkeyRider());
        Modules.get().add(new ItemFrameDupe());
        Modules.get().add(new LlamaDupe2bfr());
        Modules.get().add(new DonkeyInstantDupe());
        Modules.get().add(new AutoIgnore());
        Modules.get().add(new CoordinateSpoofer());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
