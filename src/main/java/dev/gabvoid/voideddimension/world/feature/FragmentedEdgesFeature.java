package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.AmethystClusterBlock;
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
    private static final BlockState SMOOTH_BASALT = Blocks.SMOOTH_BASALT.getDefaultState();
    private static final BlockState DARK_OAK_FENCE = Blocks.DARK_OAK_FENCE.getDefaultState();
    private static final BlockState RED_SAND = Blocks.RED_SAND.getDefaultState();
    private static final BlockState DEAD_BUSH = Blocks.DEAD_BUSH.getDefaultState();
    private static final BlockState LIGHT_GRAY_GLASS = Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState();
    private static final BlockState SMALL_AMETHYST_BUD = Blocks.SMALL_AMETHYST_BUD.getDefaultState()
            .with(AmethystClusterBlock.FACING, Direction.UP);
    private static final BlockState[] DEEPSLATE_FRAGMENTS = {
            Blocks.COBBLED_DEEPSLATE.getDefaultState(),
            Blocks.DEEPSLATE_BRICKS.getDefaultState(),
            Blocks.POLISHED_DEEPSLATE.getDefaultState()
    };
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
    private static final double FLOATING_PLATFORM_CHANCE = 0.35;
    private static final double ROOT_CHANCE = 0.5;
    private static final double CRYSTAL_CHANCE = 0.4;

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
        placed |= maybePlaceCollapsedPlatform(world, base, airSide, random);
        placed |= maybePlaceRoots(world, base, airSide, random);
        placed |= maybePlaceCrystalPillar(world, base, airSide, random);
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

    private boolean maybePlaceCollapsedPlatform(StructureWorldAccess world, BlockPos base, Direction airSide, Random random) {
        if (random.nextDouble() > FLOATING_PLATFORM_CHANCE) {
            return false;
        }
        boolean placed = false;
        int length = random.nextBetween(2, 4);
        int halfWidth = random.nextBetween(1, 2);
        int verticalShift = random.nextBetween(-1, 1);
        Direction lateral = airSide.rotateYClockwise();
        BlockPos start = base.up(verticalShift).offset(airSide);
        for (int l = 0; l < length; l++) {
            for (int w = -halfWidth; w <= halfWidth; w++) {
                if (random.nextFloat() > 0.8f) continue;
                BlockPos target = start.offset(airSide, l).offset(lateral, w);
                if (!world.isAir(target)) continue;
                world.setBlockState(target, randomPlatformState(random), Block.NOTIFY_ALL);
                if (random.nextFloat() < 0.25f) {
                    BlockPos fragment = target.down();
                    if (world.isAir(fragment)) {
                        world.setBlockState(fragment, randomFragmentState(random), Block.NOTIFY_ALL);
                    }
                }
                placed = true;
            }
        }
        return placed;
    }

    private boolean maybePlaceRoots(StructureWorldAccess world, BlockPos base, Direction airSide, Random random) {
        if (random.nextDouble() > ROOT_CHANCE) {
            return false;
        }
        BlockPos fencePos = base.offset(airSide).up(random.nextBetween(-1, 1));
        if (!world.isAir(fencePos)) {
            return false;
        }
        world.setBlockState(fencePos, DARK_OAK_FENCE, Block.NOTIFY_ALL);
        Direction side = random.nextBoolean() ? airSide.rotateYClockwise() : airSide.rotateYCounterclockwise();
        BlockPos bushBase = fencePos.offset(side);
        if (!world.isAir(bushBase)) {
            return true;
        }
        world.setBlockState(bushBase, RED_SAND, Block.NOTIFY_ALL);
        BlockPos bush = bushBase.up();
        if (world.isAir(bush)) {
            world.setBlockState(bush, DEAD_BUSH, Block.NOTIFY_ALL);
        }
        return true;
    }

    private boolean maybePlaceCrystalPillar(StructureWorldAccess world, BlockPos base, Direction airSide, Random random) {
        if (random.nextDouble() > CRYSTAL_CHANCE) {
            return false;
        }
        boolean placed = false;
        Direction lateral = random.nextBoolean() ? airSide.rotateYClockwise() : airSide.rotateYCounterclockwise();
        BlockPos pillarBase = base.offset(airSide).offset(lateral, random.nextBetween(0, 1)).up(random.nextBetween(-1, 1));
        if (!world.isAir(pillarBase)) {
            return false;
        }
        int height = random.nextBetween(2, 4);
        for (int i = 0; i < height; i++) {
            BlockPos layerPos = pillarBase.up(i);
            if (!world.isAir(layerPos)) {
                break;
            }
            world.setBlockState(layerPos, LIGHT_GRAY_GLASS, Block.NOTIFY_ALL);
            placed = true;
        }
        if (placed) {
            BlockPos budPos = pillarBase.up(height);
            if (!world.isAir(budPos)) {
                budPos = budPos.down();
            }
            if (world.isAir(budPos)) {
                world.setBlockState(budPos, SMALL_AMETHYST_BUD, Block.NOTIFY_ALL);
            }
        }
        return placed;
    }

    private BlockState randomPlatformState(Random random) {
        return random.nextFloat() < 0.5f ? SMOOTH_BASALT : randomFragmentState(random);
    }

    private BlockState randomFragmentState(Random random) {
        return DEEPSLATE_FRAGMENTS[random.nextInt(DEEPSLATE_FRAGMENTS.length)];
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

