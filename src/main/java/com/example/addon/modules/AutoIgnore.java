package com.example.addon.modules;

import com.example.addon.Main_Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoIgnore extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> commandPrefix = sgGeneral.add(new StringSetting.Builder()
        .name("command-prefix")
        .description("Command prefix to ignore players (e.g., /ignore).")
        .defaultValue("/ignore")
        .build()
    );

    private final Setting<List<String>> targetWords = sgGeneral.add(new StringListSetting.Builder()
        .name("target-words")
        .description("Words in player names to trigger ignore command.")
        .defaultValue(new ArrayList<>())
        .build()
    );

    private final Setting<List<String>> exceptionPlayers = sgGeneral.add(new StringListSetting.Builder()
        .name("exception-players")
        .description("Players who should not be ignored")
        .defaultValue(new ArrayList<>())
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Delay between commands in ticks.")
        .defaultValue(20)
        .min(0)
        .sliderMax(100)
        .build()
    );

    private int timer;
    private List<String> playersToIgnore;
    private final Set<String> ignoredPlayers = new HashSet<>();

    public AutoIgnore() {
        super(Main_Addon.CATEGORY, "AutoIgnore", "Automatically ignores spam bots with names containing specified words, excluding exceptions.");
    }

    @Override
    public void onActivate() {
        timer = 0;
        playersToIgnore = new ArrayList<>();
        ignoredPlayers.clear();
    }

    @Override
    public void onDeactivate() {
        playersToIgnore.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.getNetworkHandler() == null) return;

        if (timer <= 0) {
            if (playersToIgnore.isEmpty()) {
                updatePlayersToIgnore();
            }

            if (!playersToIgnore.isEmpty()) {
                String playerName = playersToIgnore.remove(0);
                String command = commandPrefix.get().trim() + " " + playerName;
                ChatUtils.sendPlayerMsg(command);
                ignoredPlayers.add(playerName);
                timer = delay.get();
            }
        } else {
            timer--;
        }
    }

    private void updatePlayersToIgnore() {
        playersToIgnore.clear();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().name();
            if (ignoredPlayers.contains(name)) continue;

            boolean isException = false;
            for (String exception : exceptionPlayers.get()) {
                if (name.equalsIgnoreCase(exception)) {
                    isException = true;
                    break;
                }
            }
            if (isException) continue;

            for (String word : targetWords.get()) {
                if (name.toLowerCase().contains(word.toLowerCase())) {
                    playersToIgnore.add(name);
                    break;
                }
            }
        }
    }
}
