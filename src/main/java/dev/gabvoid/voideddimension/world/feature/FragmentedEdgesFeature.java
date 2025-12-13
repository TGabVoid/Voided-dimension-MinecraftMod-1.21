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

public class FragmentedEdgesFeature extends Feature<DefaultFeatureConfig> {
    private static final Direction[] HORIZONTALS = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
    private static final BlockState GLOWSTONE = Blocks.GLOWSTONE.getDefaultState();
    private static final int MIN_Y = 290;
    private static final int MAX_Y = 355;
    private static final int CHUNK_ATTEMPTS = 18;
    private static final int LOCAL_SEARCH_TRIES = 16;
    private static final int HORIZONTAL_SEARCH = 6;
    private static final int COLUMN_SCAN_RANGE = 18;
    private static final int MIN_AIR_DEPTH = 6;
    private static final int VERTICAL_CLUSTER_RADIUS = 3;
    private static final int LATERAL_CLUSTER_STEPS = 2;
    private static final double SEA_LANTERN_CHANCE = 0.18;

    public FragmentedEdgesFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();
        boolean placedAny = false;

        for (int attempt = 0; attempt < CHUNK_ATTEMPTS; attempt++) {
            int startX = origin.getX() + random.nextInt(16);
            int startZ = origin.getZ() + random.nextInt(16);
            int startY = random.nextBetween(MIN_Y, MAX_Y);
            CrackEdge edge = locateCrackEdge(world, new BlockPos(startX, startY, startZ), random);
            if (edge == null) continue;
            if (placeCluster(world, edge, random)) {
                placedAny = true;
            }
        }
        return placedAny;
    }

    private CrackEdge locateCrackEdge(StructureWorldAccess world, BlockPos start, Random random) {
        BlockPos.Mutable columnPos = start.mutableCopy();
        for (int attempt = 0; attempt < LOCAL_SEARCH_TRIES; attempt++) {
            int sampleX = start.getX() + randomOffset(random, HORIZONTAL_SEARCH);
            int sampleY = MathHelper.clamp(start.getY() + randomOffset(random, COLUMN_SCAN_RANGE), MIN_Y, MAX_Y);
            int sampleZ = start.getZ() + randomOffset(random, HORIZONTAL_SEARCH);
            columnPos.set(sampleX, sampleY, sampleZ);
            CrackEdge edge = scanColumnForEdge(world, columnPos);
            if (edge != null) {
                return edge;
            }
        }
        return null;
    }

    private CrackEdge scanColumnForEdge(StructureWorldAccess world, BlockPos.Mutable columnPos) {
        BlockPos.Mutable airPos = new BlockPos.Mutable();
        BlockPos.Mutable hostPos = new BlockPos.Mutable();
        int x = columnPos.getX();
        int z = columnPos.getZ();
        int consecutiveAir = 0;

        for (int y = columnPos.getY(); y >= MIN_Y; y--) {
            airPos.set(x, y, z);
            BlockState state = world.getBlockState(airPos);
            if (state.isAir()) {
                consecutiveAir++;
                if (consecutiveAir < MIN_AIR_DEPTH) continue;
                for (Direction dir : HORIZONTALS) {
                    hostPos.set(x + dir.getOffsetX(), y, z + dir.getOffsetZ());
                    BlockState hostState = world.getBlockState(hostPos);
                    Direction airSide = dir.getOpposite();
                    if (isCrackHost(hostState) && world.isAir(hostPos.offset(airSide))) {
                        return new CrackEdge(hostPos.toImmutable(), airSide);
                    }
                }
            } else {
                consecutiveAir = 0;
            }
        }
        return null;
    }

    private boolean placeCluster(StructureWorldAccess world, CrackEdge edge, Random random) {
        boolean placed = false;
        BlockPos base = edge.hostPos();
        Direction airSide = edge.airSide();

        placed |= placeGlowstone(world, base, airSide);
        for (int dy = -VERTICAL_CLUSTER_RADIUS; dy <= VERTICAL_CLUSTER_RADIUS; dy++) {
            if (dy == 0 || random.nextFloat() > 0.7f) continue;
            placed |= placeGlowstone(world, base.up(dy), airSide);
        }

        if (random.nextFloat() < SEA_LANTERN_CHANCE) {
            BlockPos lanternPos = findLanternSpot(world, base, airSide);
            if (lanternPos != null) {
                world.setBlockState(lanternPos, Blocks.SEA_LANTERN.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        Direction clockwise = airSide.rotateYClockwise();
        Direction counter = airSide.rotateYCounterclockwise();
        placed |= decorateSide(world, base, airSide, clockwise, random);
        placed |= decorateSide(world, base, airSide, counter, random);
        return placed;
    }

    private boolean decorateSide(StructureWorldAccess world, BlockPos base, Direction airSide, Direction lateral, Random random) {
        boolean placed = false;
        BlockPos current = base.offset(lateral);
        for (int step = 0; step < LATERAL_CLUSTER_STEPS; step++) {
            if (random.nextFloat() > 0.55f) break;
            placed |= placeGlowstone(world, current, airSide);
            if (random.nextBoolean()) {
                placed |= placeGlowstone(world, current.up(), airSide);
            }
            if (random.nextFloat() < 0.35f) {
                placed |= placeGlowstone(world, current.down(), airSide);
            }
            current = current.offset(lateral);
        }
        return placed;
    }

    private boolean placeGlowstone(StructureWorldAccess world, BlockPos pos, Direction airSide) {
        if (pos.getY() < MIN_Y || pos.getY() > MAX_Y) {
            return false;
        }
        BlockState current = world.getBlockState(pos);
        if (!isCrackHost(current)) {
            return false;
        }
        if (!world.isAir(pos.offset(airSide))) {
            return false;
        }
        if (current.isOf(Blocks.GLOWSTONE)) {
            return false;
        }
        world.setBlockState(pos, GLOWSTONE, Block.NOTIFY_ALL);
        return true;
    }

    private boolean isCrackHost(BlockState state) {
        return state.isOf(Blocks.STONE) || state.isOf(Blocks.COBBLESTONE);
    }

    private int randomOffset(Random random, int range) {
        if (range <= 0) {
            return 0;
        }
        return random.nextBetween(-range, range);
    }

    private BlockPos findLanternSpot(StructureWorldAccess world, BlockPos base, Direction airSide) {
        BlockPos.Mutable cursor = base.mutableCopy();
        while (cursor.getY() > MIN_Y && world.isAir(cursor.offset(airSide))) {
            cursor.move(Direction.DOWN);
        }
        BlockPos candidate = cursor.down();
        return candidate.getY() >= MIN_Y ? candidate : null;
    }

    private static final class CrackEdge {
        private final BlockPos hostPos;
        private final Direction airSide;

        private CrackEdge(BlockPos hostPos, Direction airSide) {
            this.hostPos = hostPos;
            this.airSide = airSide;
        }

        public BlockPos hostPos() {
            return hostPos;
        }

        public Direction airSide() {
            return airSide;
        }
    }
}