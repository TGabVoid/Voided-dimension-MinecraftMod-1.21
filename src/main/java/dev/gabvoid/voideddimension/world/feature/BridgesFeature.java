package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class BridgesFeature extends Feature<DefaultFeatureConfig> {
    private static final Direction[] HORIZONTALS = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
    private static final BlockState BRIDGE_BLOCK = Blocks.QUARTZ_BLOCK.getDefaultState();

    public BridgesFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        boolean placedAny = false;
        for (int attempt = 0; attempt < 10; attempt++) {
            int startX = origin.getX() + random.nextInt(16);
            int startZ = origin.getZ() + random.nextInt(16);
            int startY = origin.getY() + random.nextBetween(-15, 15);
            startY = MathHelper.clamp(startY, 240, 360);

            BlockPos pos = new BlockPos(startX, startY, startZ);
            BlockPos host = findHostExtremity(world, pos, random);
            if (host != null) {
                placedAny = buildBridge(world, host, random);
                if (placedAny) {
                    break;
                }
            }
        }
        return placedAny;
    }

    private BlockPos findHostExtremity(StructureWorldAccess world, BlockPos center, Random random) {
        for (int i = 0; i < 20; i++) {
            BlockPos p = center.add(random.nextBetween(-10, 10), random.nextBetween(-5, 5), random.nextBetween(-10, 10));
            // Find an edge block (air adjacent to opaque ground)
            if (world.getBlockState(p).isAir() && !world.getBlockState(p.down()).isAir() && world.getBlockState(p.down()).isOpaque()) {
                return p;
            }
        }
        return null;
    }
    
    private boolean isExposed(StructureWorldAccess world, BlockPos pos) {
        for (Direction dir : HORIZONTALS) {
            if (world.isAir(pos.offset(dir)) && world.isAir(pos.offset(dir).down())) {
                return true;
            }
        }
        return false;
    }

    private boolean buildBridge(StructureWorldAccess world, BlockPos start, Random random) {
        Direction dir = HORIZONTALS[random.nextInt(HORIZONTALS.length)];
        int maxLength = random.nextBetween(20, 60);

        BlockPos current = start;
        BlockPos end = null;

        // Find endpoint
        for (int i = 5; i < maxLength; i++) {
            BlockPos test = current.offset(dir, i);
            boolean foundLanding = false;
            // check up/down a bit
            for (int dy = -5; dy <= 5; dy++) {
                BlockPos p = test.up(dy);
                if (world.getBlockState(p).isAir() && !world.getBlockState(p.down()).isAir() && world.getBlockState(p.down()).isOpaque()) {
                    end = p;
                    foundLanding = true;
                    break;
                }
            }
            if (foundLanding) {
                break;
            }
        }

        if (end == null || Math.abs(end.getX() - start.getX()) + Math.abs(end.getZ() - start.getZ()) < 6) return false;

        boolean placedAny = false;
        int dist = Math.max(1, Math.abs(end.getX() - start.getX()) + Math.abs(end.getZ() - start.getZ()));
        double heightDiff = end.getY() - start.getY();

        // Hyperbolic/parabolic droop
        double droop = random.nextDouble() * 3.0 + 1.0;

        for (int step = 0; step <= dist; step++) {
            double t = (double) step / dist;
            // Linear interpolate x, z
            int x = (int) Math.round(start.getX() * (1 - t) + end.getX() * t);
            int z = (int) Math.round(start.getZ() * (1 - t) + end.getZ() * t);

            // Parabola for droop: 4 * t * (1 - t) peaks at 1 in the middle
            double parabola = 4 * t * (1 - t);
            int y = (int) Math.round(start.getY() + heightDiff * t - parabola * droop);

            BlockPos pathPos = new BlockPos(x, y, z);

            // Width variation
            int width = random.nextFloat() < 0.2f ? 2 : 1;
            if (t < 0.1 || t > 0.9) width = 2; // wider at ends

            for (int dx = -width; dx <= width; dx++) {
                for (int dz = -width; dz <= width; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > width + 1) continue;
                    BlockPos placePos = pathPos.add(dx, 0, dz);
                    if (world.isAir(placePos)) {
                        world.setBlockState(placePos, BRIDGE_BLOCK, Block.NOTIFY_ALL);
                        placedAny = true;
                    }
                    if (random.nextFloat() < 0.5f && world.isAir(placePos.down())) {
                        world.setBlockState(placePos.down(), BRIDGE_BLOCK, Block.NOTIFY_ALL); // small thickness
                    }
                }
            }
        }
        return placedAny;
    }
}
