package com.example.addon.mixin;

import com.example.addon.modules.CoordinateSpoofer;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.List;
import java.util.Locale;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudLines;
import net.minecraft.client.gui.hud.debug.PlayerPositionDebugHudEntry;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Minecraft Debug Hud Mixin
// Made by Libalpm64
// This is the Java Debug Hud Mixin
// We can intercept it with the new @AT("HEAD") method introduced in 1.21.11 alongside the deobfuscated source code from Minecraft.

@Mixin(PlayerPositionDebugHudEntry.class)
public class PlayerPositionDebugHudEntryMixin {
    @Shadow @Final public static Identifier SECTION_ID;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(DebugHudLines lines, @Nullable World world, @Nullable WorldChunk clientChunk, @Nullable WorldChunk chunk, CallbackInfo ci) {
        CoordinateSpoofer module = Modules.get().get(CoordinateSpoofer.class);
        if (module == null || !module.isActive()) return;

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Entity entity = minecraftClient.getCameraEntity();
        if (entity != null) {
            double x = module.x.get();
            double y = module.y.get();
            double z = module.z.get();
            
            BlockPos blockPos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            ChunkPos chunkPos = new ChunkPos(blockPos);
            Direction direction = entity.getHorizontalFacing();

            String directionText = switch (direction) {
                case NORTH -> "Towards negative Z";
                case SOUTH -> "Towards positive Z";
                case WEST -> "Towards negative X";
                case EAST -> "Towards positive X";
                default -> "Invalid";
            };
            
            LongSet longSet = (LongSet)(world instanceof ServerWorld ? ((ServerWorld)world).getForcedChunks() : LongSets.EMPTY_SET);
            
            lines.addLinesToSection(
                SECTION_ID,
                List.of(
                    String.format(
                        Locale.ROOT,
                        "XYZ: %.3f / %.5f / %.3f",
                        x, y, z
                    ),
                    String.format(Locale.ROOT, "Block: %d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ()),
                    String.format(
                        Locale.ROOT,
                        "Chunk: %d %d %d [%d %d in r.%d.%d.mca]",
                        chunkPos.x,
                        ChunkSectionPos.getSectionCoord(blockPos.getY()),
                        chunkPos.z,
                        chunkPos.getRegionRelativeX(),
                        chunkPos.getRegionRelativeZ(),
                        chunkPos.getRegionX(),
                        chunkPos.getRegionZ()
                    ),
                    String.format(
                        Locale.ROOT, "Facing: %s (%s) (%.1f / %.1f)", direction, directionText, MathHelper.wrapDegrees(entity.getYaw()), MathHelper.wrapDegrees(entity.getPitch())
                    ),
                    minecraftClient.world.getRegistryKey().getValue() + " FC: " + longSet.size()
                )
            );
            ci.cancel();
        }
    }
}
