package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AbyssalFusteFeature extends Feature<DefaultFeatureConfig> {
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public AbyssalFusteFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        return generateAt(context.getWorld(), context.getRandom(), context.getOrigin());
    }

    public static boolean generateAt(StructureWorldAccess world, Random random, BlockPos origin) {
        final int originChunkX = origin.getX() >> 4;
        final int originChunkZ = origin.getZ() >> 4;

        int height = random.nextBetween(40, 64);
        int baseRadius = random.nextBetween(3, 5);

        Set<BlockPos> shape = new HashSet<>();
        int axisX = 0;
        int axisZ = 0;
        int nextShift = random.nextBetween(3, 4);

        for (int y = 0; y < height; y++) {
            if (y == nextShift) {
                axisX = clamp(axisX + random.nextBetween(-1, 1), -1, 1);
                axisZ = clamp(axisZ + random.nextBetween(-1, 1), -1, 1);
                nextShift += random.nextBetween(3, 4);
            }

            float t = y / (float) Math.max(1, height - 1);
            float radiusFloat = lerp(baseRadius, 1.0f, t) + random.nextFloat() * 0.5f - 0.25f;
            int radius = Math.max(1, Math.round(radiusFloat));

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > radius + (random.nextFloat() * 0.6f - 0.3f)) {
                        continue;
                    }
                    // 5% pequeños huecos
                    if (random.nextFloat() < 0.05f) {
                        continue;
                    }

                    BlockPos pos = origin.add(axisX + dx, y, axisZ + dz);
                    shape.add(pos.toImmutable());
                }
            }
        }

        if (shape.size() < 24) {
            return false;
        }

        // Si la forma sale del chunk actual, se cancela completa para evitar "cortes" verticales.
        for (BlockPos p : shape) {
            if ((p.getX() >> 4) != originChunkX || (p.getZ() >> 4) != originChunkZ) {
                return false;
            }
        }

        pruneFloating(shape);

        Map<BlockPos, BlockState> blocks = new HashMap<>();
        List<BlockPos> shell = new ArrayList<>();
        List<BlockPos> core = new ArrayList<>();

        for (BlockPos p : shape) {
            blocks.put(p, withAxis(ModBlocks.ABYSAL_FUSTE.getDefaultState(), chooseAxisForPos(p, shape, random)));
            if (isShell(p, shape)) {
                shell.add(p);
            } else {
                core.add(p);
            }
        }

        // Carcasa prioriza superficie para forma orgánica irregular.
        int targetCarcasa = Math.max(1, Math.round(shape.size() * 0.25f));
        Collections.shuffle(shell, new java.util.Random(random.nextLong()));
        for (BlockPos p : shell) {
            if (targetCarcasa <= 0) {
                break;
            }
            if (random.nextFloat() < 0.60f) {
                blocks.put(p, withAxis(ModBlocks.FUSTE_CARCASA.getDefaultState(), chooseAxisForPos(p, shape, random)));
                targetCarcasa--;
            }
        }

        // Venas conectadas, largas y visibles: cobertura 8%..12% con sesgo vertical y ramificaciones.
        float veinCoverage = 0.08f + random.nextFloat() * 0.04f;
        int targetVein = Math.max(1, Math.round(shape.size() * veinCoverage));
        int placedVein = 0;
        int surfaceVein = 0;
        Set<BlockPos> veinPlaced = new HashSet<>();

        List<BlockPos> veinSeeds = new ArrayList<>();
        int minStartY = origin.getY() + Math.max(1, (int) (height * 0.12f));
        int maxStartY = origin.getY() + Math.max(2, (int) (height * 0.72f));
        for (BlockPos p : shape) {
            if (p.getY() >= minStartY && p.getY() <= maxStartY) {
                veinSeeds.add(p);
            }
        }
        if (veinSeeds.isEmpty()) {
            veinSeeds.addAll(shape);
        }
        Collections.shuffle(veinSeeds, new java.util.Random(random.nextLong()));

        int lineAttempts = 0;
        int seedIndex = 0;
        while (placedVein < targetVein && lineAttempts < targetVein * 6 && !veinSeeds.isEmpty()) {
            lineAttempts++;
            BlockPos seed = veinSeeds.get(seedIndex % veinSeeds.size());
            seedIndex++;

            int lineMin = 6;
            int lineMax = 16 + random.nextBetween(4, 8);
            int desiredLen = random.nextBetween(lineMin, lineMax);
            int linePlaced = 0;
            List<BlockPos> linePath = new ArrayList<>();

            int dirX = random.nextBetween(-1, 1);
            int dirZ = random.nextBetween(-1, 1);
            if (dirX == 0 && dirZ == 0) dirX = random.nextBoolean() ? 1 : -1;

            BlockPos current = seed;
            for (int i = 0; i < desiredLen && placedVein < targetVein; i++) {
                if (!shape.contains(current)) {
                    current = findNearbyInShape(current, shape);
                    if (current == null) break;
                }

                boolean shellPos = isShell(current, shape);
                if (!veinPlaced.contains(current)
                        && localVeinDensity(current, veinPlaced) <= 4
                        && allowBySurfaceRatio(shellPos, placedVein, surfaceVein)) {
                    int nextX = random.nextFloat() < 0.82f ? dirX : random.nextBetween(-1, 1);
                    int nextZ = random.nextFloat() < 0.82f ? dirZ : random.nextBetween(-1, 1);
                    int nextY = random.nextFloat() < 0.78f ? 1 : 0;
                    Direction.Axis veinAxis = axisFromStep(nextX, nextY, nextZ);
                    blocks.put(current, withAxis(ModBlocks.ABYSS_VEIN.getDefaultState(), veinAxis));
                    veinPlaced.add(current);
                    linePath.add(current);
                    placedVein++;
                    linePlaced++;
                    if (shellPos) surfaceVein++;

                    // Grosor irregular ocasional sin romper continuidad.
                    if (placedVein < targetVein && random.nextFloat() < 0.08f) {
                        Direction side = HORIZONTAL[random.nextInt(HORIZONTAL.length)];
                        BlockPos thick = current.offset(side);
                        boolean shellThick = shape.contains(thick) && isShell(thick, shape);
                        if (shape.contains(thick)
                                && !veinPlaced.contains(thick)
                                && localVeinDensity(thick, veinPlaced) <= 8
                                && allowBySurfaceRatio(shellThick, placedVein, surfaceVein)) {
                            blocks.put(thick, withAxis(ModBlocks.ABYSS_VEIN.getDefaultState(), axisFromStep(side.getOffsetX(), 0, side.getOffsetZ())));
                            veinPlaced.add(thick);
                            placedVein++;
                            if (shellThick) surfaceVein++;
                        }
                    }
                }

                if (random.nextFloat() < 0.16f) {
                    dirX = clamp(dirX + random.nextBetween(-1, 1), -1, 1);
                    dirZ = clamp(dirZ + random.nextBetween(-1, 1), -1, 1);
                    if (dirX == 0 && dirZ == 0) dirZ = random.nextBoolean() ? 1 : -1;
                }

                int stepX = random.nextFloat() < 0.88f ? dirX : random.nextBetween(-1, 1);
                int stepY = random.nextFloat() < 0.78f ? 1 : 0;
                int stepZ = random.nextFloat() < 0.88f ? dirZ : random.nextBetween(-1, 1);
                current = current.add(stepX, stepY, stepZ).toImmutable();
            }

            if (linePlaced < 4) {
                for (BlockPos p : linePath) {
                    if (veinPlaced.remove(p)) {
                        placedVein--;
                        if (isShell(p, shape)) surfaceVein--;
                        blocks.put(p, withAxis(ModBlocks.ABYSAL_FUSTE.getDefaultState(), chooseAxisForPos(p, shape, random)));
                    }
                }
                continue;
            }

            // 30-40% de probabilidad de una rama corta conectada.
            if (placedVein < targetVein && !linePath.isEmpty() && random.nextFloat() < (0.30f + random.nextFloat() * 0.10f)) {
                BlockPos branchStart = linePath.get(random.nextInt(linePath.size()));
                int bLen = random.nextBetween(2, 4);
                int bdx = random.nextBetween(-1, 1);
                int bdz = random.nextBetween(-1, 1);
                if (bdx == 0 && bdz == 0) bdx = random.nextBoolean() ? 1 : -1;
                BlockPos b = branchStart;
                for (int bi = 0; bi < bLen && placedVein < targetVein; bi++) {
                    int by = random.nextFloat() < 0.62f ? 1 : 0;
                    b = b.add(bdx, by, bdz).toImmutable();
                    if (!shape.contains(b) || veinPlaced.contains(b) || localVeinDensity(b, veinPlaced) > 8) {
                        continue;
                    }
                    boolean shellB = isShell(b, shape);
                    if (!allowBySurfaceRatio(shellB, placedVein, surfaceVein)) {
                        continue;
                    }
                    blocks.put(b, withAxis(ModBlocks.ABYSS_VEIN.getDefaultState(), axisFromStep(bdx, by, bdz)));
                    veinPlaced.add(b);
                    placedVein++;
                    if (shellB) surfaceVein++;
                    if (random.nextFloat() < 0.33f) {
                        bdx = clamp(bdx + random.nextBetween(-1, 1), -1, 1);
                        bdz = clamp(bdz + random.nextBetween(-1, 1), -1, 1);
                    }
                }
            }
        }

        // Validación previa: si alguna posición no es accesible (chunk no disponible),
        // abortamos antes de colocar para evitar estructuras partidas a la mitad.
        for (BlockPos pos : blocks.keySet()) {
            if (!isPositionAccessible(world, pos)) {
                return false;
            }
        }

        // Colocar masa principal con prioridad alta: sobrescribe casi todo salvo bedrock.
        int placedMain = 0;
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState current = world.getBlockState(pos);
            if (!current.isOf(Blocks.BEDROCK)) {
                world.setBlockState(pos, entry.getValue(), Block.NOTIFY_LISTENERS);
                placedMain++;
            }
        }

        if (placedMain == 0) {
            return false;
        }

        // Racim en caras externas, más frecuente en mitad superior.
        List<BlockPos> shellTop = new ArrayList<>();
        int halfY = origin.getY() + (height / 2);
        for (BlockPos p : shell) {
            if (p.getY() >= halfY) {
                shellTop.add(p);
            }
        }
        Collections.shuffle(shellTop, new java.util.Random(random.nextLong()));

        int targetCore = random.nextBetween(90, 180);
        int placedCore = 0;
        int placedRacim = 0;

        List<BlockPos> shellAll = new ArrayList<>(shell);
        Collections.shuffle(shellAll, new java.util.Random(random.nextLong()));

        for (BlockPos anchor : shellAll) {
            if (placedCore >= targetCore) {
                break;
            }
            float yNorm = Math.max(0.0f, Math.min(1.0f, (anchor.getY() - origin.getY()) / (float) Math.max(1, height)));
            float coreChance = 0.20f + (yNorm * 0.65f); // toda la estructura, más fuerte en la punta
            if (random.nextFloat() > coreChance) {
                continue;
            }

            BlockPos corePos = anchor.up();
            if (!world.getBlockState(corePos).isAir() || world.getBlockState(corePos.down()).isAir()) {
                continue;
            }

            world.setBlockState(corePos, ModBlocks.BONY_RACIM_BLOCK.getDefaultState(), Block.NOTIFY_LISTENERS);
            placedCore++;

            // Tipo amatista: racimos alrededor del core siempre que sea posible.
            Direction[] clusterDirs = {
                    Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
            };
            for (Direction d : clusterDirs) {
                if (d != Direction.UP && random.nextFloat() > 0.92f) {
                    continue;
                }
                if (d == Direction.UP && random.nextFloat() > 0.985f) {
                    continue;
                }

                BlockPos racimPos = corePos.offset(d);
                if (!world.getBlockState(racimPos).isAir()) {
                    continue;
                }

                BlockState racim = ModBlocks.BONY_RACIM.getDefaultState()
                        .with(Properties.FACING, d)
                        .with(Properties.WATERLOGGED, false);
                if (racim.canPlaceAt(world, racimPos)) {
                    world.setBlockState(racimPos, racim, Block.NOTIFY_LISTENERS);
                    placedRacim++;
                }
            }
        }

        return true;
    }

    private static boolean canReplaceForTree(BlockState state) {
        return state.isAir() || state.isOf(Blocks.CAVE_AIR) || state.isOf(Blocks.VOID_AIR) || state.isReplaceable();
    }

    private static boolean isPositionAccessible(StructureWorldAccess world, BlockPos pos) {
        try {
            world.getBlockState(pos);
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    private static void pruneFloating(Set<BlockPos> shape) {
        boolean changed = true;
        while (changed) {
            changed = false;
            List<BlockPos> toRemove = new ArrayList<>();
            for (BlockPos p : shape) {
                boolean hasDown = shape.contains(p.down());
                int horizontalNeighbors = 0;
                for (Direction d : HORIZONTAL) {
                    if (shape.contains(p.offset(d))) {
                        horizontalNeighbors++;
                    }
                }
                boolean hasVertical = shape.contains(p.up()) || hasDown;
                if (!hasDown && horizontalNeighbors == 0 && hasVertical) {
                    continue;
                }
                if (!hasDown && horizontalNeighbors <= 1 && !shape.contains(p.up())) {
                    toRemove.add(p);
                }
            }
            if (!toRemove.isEmpty()) {
                changed = true;
                shape.removeAll(toRemove);
            }
        }
    }

    private static Direction.Axis chooseAxisForPos(BlockPos p, Set<BlockPos> shape, Random random) {
        int xScore = (shape.contains(p.east()) ? 1 : 0) + (shape.contains(p.west()) ? 1 : 0);
        int yScore = (shape.contains(p.up()) ? 1 : 0) + (shape.contains(p.down()) ? 1 : 0);
        int zScore = (shape.contains(p.north()) ? 1 : 0) + (shape.contains(p.south()) ? 1 : 0);

        if (xScore > yScore && xScore >= zScore) return Direction.Axis.X;
        if (zScore > yScore && zScore >= xScore) return Direction.Axis.Z;
        if (yScore > 0) return Direction.Axis.Y;

        int pick = random.nextInt(3);
        return pick == 0 ? Direction.Axis.X : (pick == 1 ? Direction.Axis.Y : Direction.Axis.Z);
    }

    private static Direction.Axis axisFromStep(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int az = Math.abs(dz);
        if (ax >= ay && ax >= az) return Direction.Axis.X;
        if (az >= ay && az >= ax) return Direction.Axis.Z;
        return Direction.Axis.Y;
    }

    private static BlockState withAxis(BlockState state, Direction.Axis axis) {
        if (state.contains(Properties.AXIS)) {
            return state.with(Properties.AXIS, axis);
        }
        return state;
    }

    private static boolean isShell(BlockPos pos, Set<BlockPos> shape) {
        for (Direction d : HORIZONTAL) {
            if (!shape.contains(pos.offset(d))) {
                return true;
            }
        }
        return !shape.contains(pos.up()) || !shape.contains(pos.down());
    }

    private static BlockPos findNearbyInShape(BlockPos center, Set<BlockPos> shape) {
        if (shape.contains(center)) return center;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos test = center.add(dx, dy, dz);
                    if (shape.contains(test)) {
                        return test;
                    }
                }
            }
        }
        return null;
    }

    private static int localVeinDensity(BlockPos center, Set<BlockPos> veins) {
        int c = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (veins.contains(center.add(dx, dy, dz))) c++;
                }
            }
        }
        return c;
    }

    private static boolean allowBySurfaceRatio(boolean shellPos, int placedVein, int surfaceVein) {
        if (placedVein < 8) return true;
        int nextTotal = placedVein + 1;
        int nextSurface = surfaceVein + (shellPos ? 1 : 0);
        return ((float) nextSurface / (float) nextTotal) >= 0.40f || shellPos;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}


