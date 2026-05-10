package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.world.ModDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * VoidBlockInactiveFeature: Genera void_block encima de curse_stone en INACTIVE_THRESHOLD.
 * Comportamiento tipo grass/dirt: void_block siempre encima, curse_stone debajo.
 * Variación ocasional con cobblestone y diferentes orientaciones.
 */
public class VoidBlockInactiveFeature extends Feature<DefaultFeatureConfig> {
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 256;
    private static final double VOID_BLOCK_PROBABILITY = 0.85; // 85% del suelo tendrá void_block

    public VoidBlockInactiveFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        var random = context.getRandom();
        boolean changed = false;

        // Solo generar en INACTIVE_THRESHOLD
        BlockPos checkPos = origin.add(8, 0, 8);
        if (!world.getBiome(checkPos).matchesKey(ModDimensions.INACTIVE_THRESHOLD_KEY)) {
            return false;
        }

        // Procesar superficie del chunk
        for (int x = origin.getX(); x < origin.getX() + 16; x++) {
            for (int z = origin.getZ(); z < origin.getZ() + 16; z++) {
                BlockPos surfacePos = findSurface(world, x, z);
                if (surfacePos == null) continue;

                BlockState topState = world.getBlockState(surfacePos);
                if (topState.isOf(ModBlocks.CURSE_STONE_BLOCK)) {
                    if (random.nextDouble() < VOID_BLOCK_PROBABILITY) {
                        // Generar void_block encima de curse_stone
                        BlockPos abovePos = surfacePos.up();
                        if (world.isAir(abovePos)) {
                            Direction randomFacing = getRandomFacing(random);
                            BlockState voidState = ModBlocks.VOID_BLOCK.getDefaultState()
                                    .with(Properties.HORIZONTAL_FACING, randomFacing);
                            world.setBlockState(abovePos, voidState, Block.NOTIFY_ALL);
                            changed = true;
                        }
                    } else {
                        // Variación ocasional: generar cobblestone o dejar curse_stone con variantes
                        if (random.nextBoolean()) {
                            BlockPos abovePos = surfacePos.up();
                            if (world.isAir(abovePos)) {
                                BlockState cobbleState = ModBlocks.CURSE_COBBLESTONE_BLOCK.getDefaultState();
                                world.setBlockState(abovePos, cobbleState, Block.NOTIFY_ALL);
                                changed = true;
                            }
                        }
                    }
                }
            }
        }

        return changed;
    }

    private BlockPos findSurface(StructureWorldAccess world, int x, int z) {
        // Buscar el bloque sólido más alto
        for (int y = MAX_Y; y >= MIN_Y; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && !state.isOf(Blocks.BEDROCK)) {
                return pos;
            }
        }
        return null;
    }

    private Direction getRandomFacing(net.minecraft.util.math.random.Random random) {
        Direction[] directions = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
        return directions[random.nextInt(directions.length)];
    }
}

