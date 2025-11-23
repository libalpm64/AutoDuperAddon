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
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Simple donkey dupe: Go to donkey -> sneak+click -> wait 1 second -> TP to track -> mount minecart
 */
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
                timer = 100; // Wait 5 seconds after TP for stability
                if (debug.get()) ChatUtils.info("Going to donkey home - waiting 5 seconds");
            }
            
            case SNEAK_CLICK -> {
                if (timer > 0) { timer--; return; }
                
                Entity donkey = findNextUnprocessedDonkey();
                if (donkey != null) {
                    currentTargetDonkey = donkey;
                    
                    Sneak sneakModule = Modules.get().get(Sneak.class);
                    if (!sneakModule.isActive()) {
                        sneakModule.toggle();
                        if (debug.get()) ChatUtils.info("Activated sneak module - waiting for it to fully activate");
                        timer = 20;
                        return;
                    }
                    
                    if (!mc.player.isSneaking()) {
                        if (debug.get()) ChatUtils.info("Waiting for sneak to fully activate... (currently: " + mc.player.isSneaking() + ")");
                        timer = 20;
                        return;
                    }
                    
                    float distance = mc.player.distanceTo(donkey);
                    if (debug.get()) ChatUtils.info("Ready to interact! Distance: " + String.format("%.2f", distance) + " (sneaking: " + mc.player.isSneaking() + ")");
                    
                    if (distance > 6.0) {
                        if (debug.get()) ChatUtils.info("Too far from donkey, getting closer...");
                        timer = 10;
                        return;
                    }
                    
                    tryInteractWithDonkey(donkey);
                    
                    processedDonkeys.add(donkey.getUuid());
                    if (debug.get()) ChatUtils.info("Completed sneak+interaction with donkey! Processed " + processedDonkeys.size() + " donkeys total.");
                    
                    phase = Phase.WAIT_DELAY;
                    timer = 15; // TP after 15 ticks - gives us 25 ticks to get there and mount
                } else {
                    if (debug.get()) ChatUtils.info("No more unprocessed donkeys found nearby! Total processed: " + processedDonkeys.size());
                    if (findDonkeys().size() > processedDonkeys.size()) {
                        if (debug.get()) ChatUtils.info("Some donkeys might be out of range, waiting and retrying...");
                        timer = 40;
                    } else {
                        if (debug.get()) ChatUtils.info("All nearby donkeys processed, continuing with dupe cycle...");
                        phase = Phase.WAIT_DELAY;
                        timer = 15;
                    }
                }
            }
            
            case WAIT_DELAY -> {
                if (timer > 0) { timer--; return; }
                
                ChatUtils.sendPlayerMsg("/home " + trackHome.get());
                phase = Phase.MOUNT_CART;
                timer = 5;
                
                if (debug.get()) ChatUtils.info("TP to track EARLY - need to mount fast!");
            }
            
            case MOUNT_CART -> {
                if (timer > 0) { timer--; return; }
                
                Sneak sneakModule = Modules.get().get(Sneak.class);
                if (sneakModule.isActive()) {
                    sneakModule.toggle();
                    if (debug.get()) ChatUtils.info("Turned off sneak for mounting");
                }
                
                MinecartEntity cart = findMinecart();
                if (cart != null) {
                    ActionResult mountResult = mc.interactionManager.interactEntity(mc.player, cart, Hand.MAIN_HAND);
                    
                    if (debug.get()) ChatUtils.info("Mount attempt: " + mountResult + " (hasVehicle: " + mc.player.hasVehicle() + ")");
                    
                    if (mc.player.hasVehicle()) {
                        if (debug.get()) ChatUtils.info("Successfully mounted - server should create fake inventory");
                        
                        phase = Phase.WAIT_INVENTORY;
                        timer = 100; // Wait 5 seconds for inventory interaction
                    } else {
                        if (debug.get()) ChatUtils.info("Mount failed, retrying...");
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
                
                boolean inventoryOpen = mc.currentScreen instanceof HorseScreen;
                
                if (inventoryOpen) {
                    if (debug.get()) ChatUtils.info("Donkey inventory is open, waiting for it to close...");
                    timer = 20;
                    return;
                }
                
                if (mc.player.hasVehicle()) {
                    if (debug.get()) ChatUtils.info("Still mounted but inventory closed, forcing dismount...");
                    mc.player.setSneaking(true);
                    mc.player.setSneaking(false);
                    timer = 10; // Wait a bit after dismount
                    return;
                }
                
                if (debug.get()) ChatUtils.info("Inventory closed and dismounted, returning to original home...");
                phase = Phase.DISMOUNT_RETURN;
                timer = 10; // Short delay before teleporting
            }
            
            case DISMOUNT_RETURN -> {
                if (timer > 0) { timer--; return; }
                
                if (mc.player.hasVehicle()) {
                    mc.player.setSneaking(true);
                    mc.player.setSneaking(false);
                    if (debug.get()) ChatUtils.info("Forcing dismount...");
                    timer = 10;
                    return;
                }
                
                ChatUtils.sendPlayerMsg("/home " + donkeyHome.get());
                if (debug.get()) ChatUtils.info("Teleported back to original home!");
                
                if (continuousMode.get()) {
                    phase = Phase.GO_DONKEY;
                    timer = 200; // Wait 10 seconds before next cycle
                    processedDonkeys.clear();
                    if (debug.get()) ChatUtils.info("Continuous mode: Starting next cycle...");
                } else {
                    if (debug.get()) ChatUtils.info("Single cycle mode: Completed all donkeys, disabling module...");
                    this.toggle();
                    return;
                }
                currentTargetDonkey = null;
            }
        }
    }

    private java.util.List<Entity> findDonkeys() {
        return mc.world.getOtherEntities(mc.player, 
            new Box(mc.player.getBlockPos()).expand(8.0),
            e -> {
                if (e instanceof DonkeyEntity donkey) {
                    return donkey.hasChest();
                }
                if (e instanceof MuleEntity mule) {
                    return mule.hasChest();
                }
                return false;
            }
        );
    }
    
    private Entity findNextUnprocessedDonkey() {
        return findDonkeys().stream()
            .filter(donkey -> !processedDonkeys.contains(donkey.getUuid()))
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
        
        if (debug.get()) ChatUtils.info("Single interaction result: " + result);
    }

    @Override
    public String getInfoString() {
        return phase.name() + " (" + timer + ") [" + processedDonkeys.size() + " processed]";
    }
}