package com.example.addon.modules;

import com.example.addon.Main_Addon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.DonkeyEntity;
import net.minecraft.entity.passive.MuleEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;

/**
 * 2bfr dupe. Maybe patched idk havent checked.
 */
public class LlamaDupe2bfr extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay between between each consecutive dupe in ticks (2 ticks ~= 1 second (scaled; so for 15 seconds it should be at 30 ticks)")
        .defaultValue(30)
        .sliderMin(10)
        .sliderMax(200)
        .build()
    );
    private final Setting<Boolean> fastMode = sgGeneral.add(new BoolSetting.Builder()
        .name("fast-mode")
        .description("Interact every tick for instant/rapid duping.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> includeDonkeys = sgGeneral.add(new BoolSetting.Builder()
        .name("include-donkeys")
        .description("Also target donkeys/mules in addition to llamas.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> enforceDonkeyDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("enforce-donkey-distance")
        .description("Only interact with donkey/mule when beyond required distance (server-safe).")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> donkeyMinDistance = sgGeneral.add(new IntSetting.Builder()
        .name("donkey-min-distance")
        .description("Minimum distance from donkey/mule before interacting (matches server checks).")
        .defaultValue(128)
        .min(1)
        .sliderMax(256)
        .visible(enforceDonkeyDistance::get)
        .build()
    );
    private final Setting<Boolean> dismountOnLlama = sgGeneral.add(new BoolSetting.Builder()
        .name("dismount-on-llama")
        .description("Auto-dismount if riding a llama before interacting.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> dismountOnDonkey = sgGeneral.add(new BoolSetting.Builder()
        .name("dismount-on-donkey")
        .description("Auto-dismount if riding a donkey/mule before interacting.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> autoSneak = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-sneak-on-donkey")
        .description("Temporarily sneak when interacting with donkey/mule to open chest instantly.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("debug")
        .description("Sends debug messages to indicate what is happening or if any error occurred")
        .defaultValue(false)
        .build()
    );
    private int timer = 0;


    public LlamaDupe2bfr() {
        super(Main_Addon.CATEGORY,"LlamaDupe2b2fr","Performs the llama dupe for 2b2fr.org");
    }

    @Override
    public void onActivate() {
        timer = delay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!Utils.canUpdate() || mc.interactionManager == null) return;

        if(mc.player.getVehicle() instanceof LlamaEntity && dismountOnLlama.get()){
            mc.player.dismountVehicle();
            ChatUtils.sendPlayerMsg(".dismount");

            if(debug.get()){
               ChatUtils.sendMsg(Text.of("Dismounted"));
            }
        } else if ((mc.player.getVehicle() instanceof DonkeyEntity || mc.player.getVehicle() instanceof MuleEntity) && dismountOnDonkey.get()) {
            mc.player.dismountVehicle();
            ChatUtils.sendPlayerMsg(".dismount");
            if (debug.get()) ChatUtils.sendMsg(Text.of("Dismounted (donkey/mule)"));
        }

        if (!fastMode.get()) {
            if (timer > 0) {
                timer--;
                return;
            } else {
                timer = delay.get();
                if(debug.get()){
                    ChatUtils.sendMsg(Text.of("Next interaction in ticks: " + timer));
                }
            }
        }

        Entity target = getNearestTargetEntity();

        if(target == null){
            ChatUtils.sendMsg(Text.of("Could not find a target chested animal within interaction range... Toggling"));
            toggle();
            return;
        }
        if (enforceDonkeyDistance.get() && (target instanceof DonkeyEntity || target instanceof MuleEntity)) {
            float dist = target.distanceTo(mc.player);
            if (dist < donkeyMinDistance.get()) {
                if (debug.get()) ChatUtils.sendMsg(Text.of("Too close to donkey/mule (" + dist + "), waiting until >= " + donkeyMinDistance.get()));
                return;
            }
        }

        boolean shouldSneak = autoSneak.get() && (target instanceof DonkeyEntity || target instanceof MuleEntity);
        if (shouldSneak) setPressed(mc.options.sneakKey, true);

        ActionResult actionResult =  mc.interactionManager.interactEntity(mc.player, target, Hand.MAIN_HAND);

        if (shouldSneak) setPressed(mc.options.sneakKey, false);

        if(debug.get()){
            ChatUtils.sendMsg(Text.of((target instanceof LlamaEntity ? "Llama" : (target instanceof DonkeyEntity ? "Donkey" : "Mule")) +
                " interaction status: " + actionResult));
        }
    }

    private Entity getNearestTargetEntity() {
        return mc.world.getOtherEntities(
                mc.player,
                new Box(mc.player.getBlockPos()).expand(mc.player.getEntityInteractionRange()),
                entity -> entity instanceof LlamaEntity || (includeDonkeys.get() && (entity instanceof DonkeyEntity || entity instanceof MuleEntity))
            )
            .stream()
            .min((entity1, entity2) -> Float.compare(entity1.distanceTo(mc.player), entity2.distanceTo(mc.player)))
            .orElse(null);
    }

    private void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

}
