package com.example.addon.modules;

import com.example.addon.Main_Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Sneak;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HorseScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DonkeyInstantDupe extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<String> donkeyHome = sg.add(new StringSetting.Builder()
        .name("donkey-home")
        .description("Home where donkey is located")
        .defaultValue("Donkey")
        .build()
    );

    private final Setting<String> trackHome = sg.add(new StringSetting.Builder()
        .name("track-home")
        .description("Home 128+ blocks away with minecart")
        .defaultValue("DonkeyTrack")
        .build()
    );

    private final Setting<Boolean> debug = sg.add(new BoolSetting.Builder()
        .name("debug")
        .description("Show debug messages")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> continuousMode = sg.add(new BoolSetting.Builder()
        .name("continuous-mode")
        .description("Keep cycling through donkeys continuously")
        .defaultValue(true)
        .build()
    );

    private enum Phase { GO_DONKEY, SNEAK_CLICK, WAIT_DELAY, GO_TRACK, MOUNT_CART, WAIT_INVENTORY, DISMOUNT_RETURN }
    private Phase phase = Phase.GO_DONKEY;
    private int timer = 0;
    private Set<UUID> processedDonkeys = new HashSet<>();
    private Entity currentTargetDonkey = null;

    public DonkeyInstantDupe() {
        super(Main_Addon.CATEGORY, "DonkeyInstantDupe", "Simple donkey dupe with sneak+click");
    }

    @Override
    public void onActivate() {
        phase = Phase.GO_DONKEY;
        timer = 0;
        processedDonkeys.clear();
        currentTargetDonkey = null;
    }

    @Override
    public void onDeactivate() {
        Sneak sneakModule = Modules.get().get(Sneak.class);
        if (sneakModule.isActive()) {
            sneakModule.toggle();
            if (debug.get()) ChatUtils.info("Turned off sneak on module disable");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || mc.player == null) return;

        switch (phase) {
            case GO_DONKEY -> {
                ChatUtils.sendPlayerMsg("/home " + donkeyHome.get());
                phase = Phase.SNEAK_CLICK;
                timer = 0;
                if (debug.get()) ChatUtils.info("Teleporting to donkey home...");
            }

            case SNEAK_CLICK -> {
                Entity donkey = findNextUnprocessedDonkey();
                if (donkey == null) return;

                currentTargetDonkey = donkey;

                Sneak sneakModule = Modules.get().get(Sneak.class);
                if (!mc.player.isSneaking()) {
                    if (!sneakModule.isActive()) sneakModule.toggle();
                    return;
                }

                float distance = mc.player.distanceTo(donkey);
                if (distance > 6.0) return;

                tryInteractWithDonkey(donkey);
                processedDonkeys.add(donkey.getUuid());
                if (debug.get()) ChatUtils.info("Sneak+click interaction done with donkey, total processed: " + processedDonkeys.size());

                phase = Phase.WAIT_DELAY;
                timer = 15;
            }

            case WAIT_DELAY -> {
                if (timer > 0) { timer--; return; }
                ChatUtils.sendPlayerMsg("/home " + trackHome.get());
                phase = Phase.MOUNT_CART;
                timer = 5;
                if (debug.get()) ChatUtils.info("Teleporting to track to mount minecart...");
            }

            case MOUNT_CART -> {
                if (timer > 0) { timer--; return; }

                Sneak sneakModule = Modules.get().get(Sneak.class);
                if (sneakModule.isActive()) sneakModule.toggle();

                MinecartEntity cart = findMinecart();
                if (cart != null) {
                    ActionResult mountResult = mc.interactionManager.interactEntity(mc.player, cart, Hand.MAIN_HAND);
                    if (debug.get()) ChatUtils.info("Mount attempt: " + mountResult + " (hasVehicle: " + mc.player.hasVehicle() + ")");
                    if (mc.player.hasVehicle()) {
                        phase = Phase.WAIT_INVENTORY;
                        timer = 100;
                    } else {
                        timer = 10;
                    }
                } else {
                    if (debug.get()) ChatUtils.error("No minecart found at track!");
                    phase = Phase.GO_DONKEY;
                    timer = 100;
                }
            }

            case WAIT_INVENTORY -> {
                if (timer > 0) { timer--; return; }

                if (mc.currentScreen instanceof HorseScreen) {
                    timer = 20;
                    return;
                }

                if (mc.player.hasVehicle()) {
                    mc.player.setSneaking(true);
                    mc.player.setSneaking(false);
                    timer = 10;
                    return;
                }

                phase = Phase.DISMOUNT_RETURN;
                timer = 10;
            }

            case DISMOUNT_RETURN -> {
                if (timer > 0) { timer--; return; }

                if (mc.player.hasVehicle()) {
                    mc.player.setSneaking(true);
                    mc.player.setSneaking(false);
                    timer = 10;
                    return;
                }

                ChatUtils.sendPlayerMsg("/home " + donkeyHome.get());

                if (continuousMode.get()) {
                    phase = Phase.GO_DONKEY;
                    timer = 200;
                    processedDonkeys.clear();
                    if (debug.get()) ChatUtils.info("Continuous mode: starting next cycle...");
                } else {
                    this.toggle();
                }
                currentTargetDonkey = null;
            }
        }
    }

    private java.util.List<Entity> findDonkeys() {
        return mc.world.getOtherEntities(mc.player,
            new Box(mc.player.getBlockPos()).expand(8.0),
            e -> (e instanceof DonkeyEntity donkey && donkey.hasChest()) || (e instanceof MuleEntity mule && mule.hasChest())
        );
    }

    private Entity findNextUnprocessedDonkey() {
        return findDonkeys().stream()
            .filter(d -> !processedDonkeys.contains(d.getUuid()))
            .min((a, b) -> Float.compare(a.distanceTo(mc.player), b.distanceTo(mc.player)))
            .orElse(null);
    }

    private MinecartEntity findMinecart() {
        return (MinecartEntity) mc.world.getOtherEntities(mc.player,
            new Box(mc.player.getBlockPos()).expand(5.0),
            e -> e instanceof MinecartEntity
        ).stream().findFirst().orElse(null);
    }

    private void tryInteractWithDonkey(Entity donkey) {
        ActionResult result = mc.interactionManager.interactEntity(mc.player, donkey, Hand.MAIN_HAND);
        if (debug.get()) ChatUtils.info("Interaction result: " + result);
    }

    @Override
    public String getInfoString() {
        return phase.name() + " (" + timer + ") [" + processedDonkeys.size() + " processed]";
    }
}
