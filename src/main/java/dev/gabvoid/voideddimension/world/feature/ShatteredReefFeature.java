package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Placeholder de ShatteredReefFeature.
 * Genera formaciones de arrecifes rotos con bloques de placeholder.
 * Usa bloques simples de Minecraft para no depender de fragmentos.
 */
public class ShatteredReefFeature extends Feature<DefaultFeatureConfig> {
    private static final int REEF_ATTEMPTS = 6;
    private static final int REEF_RADIUS_MIN = 8;
    private static final int REEF_RADIUS_MAX = 16;
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.getDefaultState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.getDefaultState();
    private static final BlockState CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.getDefaultState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.getDefaultState();

    public ShatteredReefFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();
        boolean placedAny = false;

        for (int attempt = 0; attempt < REEF_ATTEMPTS; attempt++) {
            int x = origin.getX() + random.nextBetween(-32, 32);
            int z = origin.getZ() + random.nextBetween(-32, 32);
            BlockPos reefPos = findReefBase(world, x, z);
            if (reefPos == null) continue;

            int radius = random.nextBetween(REEF_RADIUS_MIN, REEF_RADIUS_MAX);
            if (generateReefFormation(world, reefPos, radius, random)) {
                placedAny = true;
            }
        }

        return placedAny;
    }

    private BlockPos findReefBase(StructureWorldAccess world, int x, int z) {
        // Buscar una posición sólida para construir el arrecife
        for (int y = 200; y >= 50; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state != Blocks.BEDROCK.getDefaultState()) {
                return pos.up();
            }
        }
        return null;
    }

    private boolean generateReefFormation(StructureWorldAccess world, BlockPos center, int radius, Random random) {
        boolean placed = false;
        int radiusSq = radius * radius;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) continue;

                for (int dy = -2; dy <= 8; dy++) {
                    BlockPos target = center.add(dx, dy, dz);
                    double distSq = dx * dx + dz * dz;
                    double noiseVal = Math.sin(distSq * 0.1) * Math.cos(dy * 0.5);

                    // Generar bloques según patrón de ruido
                    if (random.nextFloat() < (0.5 - distSq / (radiusSq * 2)) * 0.8) {
                        BlockState toPlace = chooseReefBlock(random, noiseVal);
                        if (world.isAir(target)) {
                            world.setBlockState(target, toPlace, Block.NOTIFY_ALL);
                            placed = true;
                        }
                    }
                }
            }
        }

        return placed;
    }

    private BlockState chooseReefBlock(Random random, double noise) {
        double r = random.nextDouble();
        if (r < 0.4) {
            return OBSIDIAN;
        } else if (r < 0.7) {
            return BLACKSTONE;
        } else if (r < 0.85) {
            return CRYING_OBSIDIAN;
        } else {
            return DEEPSLATE;
        }
    }
}

