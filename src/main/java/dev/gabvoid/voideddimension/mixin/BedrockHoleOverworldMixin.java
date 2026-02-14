package dev.gabvoid.voideddimension.mixin;

import dev.gabvoid.voideddimension.VoidedDimension;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Huecos de bedrock en Overworld sin depender de JSON vanilla.
 *
 * Idea: usar RNG determinista por chunk (basado en seed+mundo+chunkPos) para:
 * - decidir raramente si hay hueco
 * - variar un poco la forma (irregular) y el offset dentro del chunk
 *
 * Siempre fuerza AIR en Y=-64..-55 para atravesar bedrock.
 */
@Mixin(ChunkGenerator.class)
public abstract class BedrockHoleOverworldMixin {

    // Ajustes de testeo
    private static final int BOTTOM_Y = -64;
    private static final int TOP_Y = -55;

    // Para testeo: 1 de cada ~10 chunks (≈80% de la frecuencia anterior)
    private static final int RARITY = 10;

    // Hook principal: por nombre (sin descriptor) y NO-crítico.
    // Si Mojang/Yarn cambian la firma, esto no debe crashear el juego.
    @Dynamic
    @Inject(method = "buildBedrock", at = @At("TAIL"), require = 0, remap = false)
    private void voideddimension$afterBuildBedrock(ChunkRegion region, Chunk chunk, GenerationShapeConfig shapeConfig, CallbackInfo ci) {
        carve(region, chunk);
    }

    // Hook alternativo: si el método fue renombrado/cambiado, intentamos otro nombre común.
    // (Si no existe, require=0 evita crash.)
    @Dynamic
    @Inject(method = "buildBedrockAndCeiling", at = @At("TAIL"), require = 0, remap = false)
    private void voideddimension$afterBuildBedrockAlt(ChunkRegion region, Chunk chunk, GenerationShapeConfig shapeConfig, CallbackInfo ci) {
        carve(region, chunk);
    }

    private void carve(ChunkRegion region, Chunk chunk) {
        // Solo overworld
        // ChunkRegion en 1.21.1 no expone getWorld(); usamos toServerWorld() para chequear la dimensión.
        if (!region.toServerWorld().getRegistryKey().equals(World.OVERWORLD)) return;

        ChunkPos chunkPos = chunk.getPos();

        // RNG determinista por chunk
        long seed = region.getSeed();
        ChunkRandom rand = new ChunkRandom(Random.create(seed));
        rand.setCarverSeed(seed, chunkPos.x, chunkPos.z);

        // Decide si este chunk tiene hueco
        if (rand.nextInt(RARITY) != 0) return;

        // Centro-ish del chunk con pequeño offset natural
        int centerX = chunkPos.getStartX() + 8 + rand.nextInt(5) - 2;
        int centerZ = chunkPos.getStartZ() + 8 + rand.nextInt(5) - 2;

        // Ancho variable: 3..5, pero irregular por 'jitter'
        int radius = rand.nextBoolean() ? 1 : 2; // 1 => 3x3, 2 => 5x5

        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int y = BOTTOM_Y; y <= TOP_Y; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Irregularidad: recorta esquinas a veces (forma más orgánica)
                    int manhattan = Math.abs(dx) + Math.abs(dz);
                    if (radius == 2 && manhattan == 4 && rand.nextInt(3) != 0) continue;

                    m.set(centerX + dx, y, centerZ + dz);
                    region.setBlockState(m, Blocks.AIR.getDefaultState(), 0);
                }
            }
        }

        // Log poco frecuente para confirmar que está funcionando
        if (chunkPos.x == 0 && chunkPos.z == 0) {
            VoidedDimension.LOGGER.info("[voideddimension] Carved overworld bedrock hole at chunk {},{} (x={}, z={}, radius={})", chunkPos.x, chunkPos.z, centerX, centerZ, radius);
        }
    }
}
