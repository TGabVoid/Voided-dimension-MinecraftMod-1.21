package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.world.ModDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class DryCracksFeature extends Feature<DefaultFeatureConfig> {
    private static final boolean DEBUG_MARKERS = false; // Desactivar debug logs
    private static final int MIN_Y = 0;
    private static final int MAX_Y = 512;
    private static final int CRACK_SYSTEMS_MIN = 1;
    private static final int CRACK_SYSTEMS_MAX = 2;
    private static final int MAX_CRACK_DEPTH = 3;
    private static final int SAFE_EDGE_DEPTH = 4;

    private static final int FLAT_RADIUS = 2;
    private static final int MAX_FLAT_DELTA = 2;
    private static final int MIN_MASSIVE_WIDTH = 7;
    private static final int FULL_DEPTH_WIDTH = 10;
    private static final int MAX_SPAN_SCAN = 14;
    private static final int SITE_SEARCH_TRIES = 48;
    private static final int SURFACE_DESCENT_SCAN = 24;

    private static final int PATCH_RADIUS_MIN = 32;
    private static final int PATCH_RADIUS_MAX = 48;
    private static final int CELL_SIZE_MIN = 30;
    private static final int CELL_SIZE_MAX = 60;
    private static final double EDGE_THRESHOLD_MIN = 1.0;
    private static final double EDGE_THRESHOLD_MAX = 2.5;
    private static final Set<Long> NO_GEN_CHUNKS_LOGGED = ConcurrentHashMap.newKeySet();
    private static final Set<Long> ABYSSAL_FUSTE_CELLS_SPAWNED = ConcurrentHashMap.newKeySet();
    private static final double FUSTE_CENTER_CHANCE = 0.30; // 30% por centro de celda
    private static final double FUSTE_LOWER_MULTI_CHANCE = 0.055; // 50%/9 ~= 5.5% (bajito por chunk/celda)
    private static final int CHUNK_INTERIOR_MARGIN = 7; // margen agresivo para evitar cortes verticales por borde de chunk
    private static final BlockState FRACTURED_STONE_STATE = ModBlocks.FRACTURED_STONE.getDefaultState();
    private static final BlockState FRACTURED_COBBLESTONE_STATE = ModBlocks.FRACTURED_COBBLESTONE.getDefaultState();
    private static final BlockState STRESS_CRACK_STATE = ModBlocks.STRESS_CRACK.getDefaultState();

    public DryCracksFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        var random = context.getRandom();
        BlockPos origin = context.getOrigin();
        long chunkKey = ChunkPos.toLong(origin.getX() >> 4, origin.getZ() >> 4);
        boolean changed = false;

        BlockPos surfaceOrigin = groundSurfacePos(world, origin.getX(), origin.getZ());
        if (surfaceOrigin == null) {
            return false;
        }

        // Safety guard: even if referenced elsewhere by mistake, only run in fragmented_plains.
        if (!world.getBiome(surfaceOrigin).matchesKey(ModDimensions.FRAGMENTED_PLAINS_KEY)) {
            return false;
        }

        if (DEBUG_MARKERS) {
            VoidedDimension.LOGGER.info("[dry_cracks] generate() origin={} {} {}", origin.getX(), origin.getY(), origin.getZ());
        }

        int systems = random.nextBetween(CRACK_SYSTEMS_MIN, CRACK_SYSTEMS_MAX);
        for (int i = 0; i < systems; i++) {
            Site site = findValidSite(world, origin, random);
            if (site == null) {
                if (DEBUG_MARKERS) {
                    BlockPos topAtOrigin = groundSurfacePos(world, origin.getX() + random.nextInt(16), origin.getZ() + random.nextInt(16));
                    if (topAtOrigin != null) {
                        markDebug(world, topAtOrigin, Blocks.MAGENTA_CONCRETE.getDefaultState());
                    }
                }
                continue;
            }

            if (DEBUG_MARKERS) {
                markDebug(world, site.pos(), Blocks.LIME_CONCRETE.getDefaultState());
            }

            int patchRadius = random.nextBetween(PATCH_RADIUS_MIN, PATCH_RADIUS_MAX);
            
            // Para que las grietas se conecten perfectamente y formen un patrón global continuo,
            // DEBEMOS usar el mismo tamaño de red, grosor y semilla (del mundo) para todos los parches.
            // Reducimos el tamaño un ~25% para que hayan as células (de 45 a 34).
            int cellSize = 34;
            // Duplicamos grosor de grieta: al subir este umbral, mas puntos caen en borde negro.
            double edgeThreshold = 3.6;
            long seed = world.getSeed() + 87312L; 
            
            boolean generated = carveVoronoiPatch(world, site, patchRadius, cellSize, edgeThreshold, seed, random);
            if (generated && DEBUG_MARKERS) {
                notifyDebug(world, site.pos(), patchRadius, cellSize, edgeThreshold);
            }
            changed |= generated;
        }

        if (!changed && DEBUG_MARKERS) {
            BlockPos originTop = groundSurfacePos(world, origin.getX() + 8, origin.getZ() + 8);
            if (originTop != null) {
                markDebug(world, originTop, Blocks.YELLOW_CONCRETE.getDefaultState());
            }
        }

        return changed;
    }

    private boolean carveAtSurface(StructureWorldAccess world, int x, int z, int depth) {
        BlockPos top = groundSurfacePos(world, x, z);
        if (top == null) {
            return false;
        }
        if (!isDrySurface(world.getBlockState(top))) {
            return false;
        }
        boolean carved = carveDown(world, top, depth);
        if (carved) {
            paintRim(world, top);
        }
        return carved;
    }

    private boolean carveVoronoiPatch(
            StructureWorldAccess world,
            Site site,
            int radius,
            int cellSize,
            double edgeThreshold,
            long seed,
            net.minecraft.util.math.random.Random random
    ) {
        boolean changed = false;
        BlockPos center = site.pos();
        final int genChunkX = center.getX() >> 4;
        final int genChunkZ = center.getZ() >> 4;
        int radiusSq = radius * radius;
        Map<Long, CellCenterCandidate> cellCenters = new HashMap<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx * dx) + (dz * dz) > radiusSq) {
                    continue;
                }

                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                // Nunca escribir fuera del chunk en generación (evita far-chunk setBlock y chunks no disponibles).
                if ((x >> 4) != genChunkX || (z >> 4) != genChunkZ) {
                    continue;
                }
                BlockPos top = groundSurfacePos(world, x, z);
                if (top == null) {
                    continue;
                }
                
                if (Math.abs(top.getY() - center.getY()) > 1) {
                    continue;
                }

                BlockState topState = world.getBlockState(top);
                if (!isDrySurface(topState)) {
                    continue;
                }

                // Forma de grieta normal/limpia base para líneas
                double distortX = x; // ya no aplicamos el "wobble" sucio en las líneas
                double distortZ = z; // ya no aplicamos el "wobble" sucio en las líneas

                double edgeMetric = voronoiEdgeMetric(distortX, distortZ, cellSize, seed);

                CellSample sample = nearestCellSample(distortX, distortZ, cellSize, seed);
                long cellKey = toCellKey(sample.sx(), sample.sz());
                CellCenterCandidate prev = cellCenters.get(cellKey);
                if (prev == null || sample.edgeMetric() > prev.edgeMetric()) {
                    cellCenters.put(cellKey, new CellCenterCandidate(top.toImmutable(), sample.sx(), sample.sz(), sample.edgeMetric()));
                }

                if (edgeMetric < edgeThreshold) {
                    // Solo el borde Voronoi se abre como grieta de aire, con profundidad segura.
                    changed |= carveSafeEdgeCrack(world, top, SAFE_EDGE_DEPTH);
                } else if (edgeMetric >= edgeThreshold + 1.0 && edgeMetric <= edgeThreshold + 4.5) {
                    // Usar un "ruido" extra para determinar si este segmento de la celula permite spikes o stress cracks
                    double segmentNoise = hash01(x / 5, z / 5, seed, 42);
                    if (segmentNoise < 0.25) {
                        if (random.nextFloat() < 0.15f) {
                            generateSpike(world, top, random);
                            changed = true;
                        } else if (random.nextFloat() < 0.15f) {
                            world.setBlockState(top, random.nextBoolean() ? Blocks.BLACKSTONE.getDefaultState() : Blocks.OBSIDIAN.getDefaultState(), Block.NOTIFY_ALL);
                            changed = true;
                        }
                    }
                    if (segmentNoise < 0.35 && random.nextFloat() < 0.40f) {
                        generateHorizontalStressCrack(world, top, random);
                        changed = true;
                    }
                } else {
                    // Base de superficie: fractured_stone (como grass block)
                    world.setBlockState(top, FRACTURED_STONE_STATE, Block.NOTIFY_ALL);
                    // Un poco debajo: fractured_cobblestone
                    BlockPos below = top.down();
                    if (world.isAir(below) || !isDrySurface(world.getBlockState(below))) {
                        world.setBlockState(below, FRACTURED_COBBLESTONE_STATE, Block.NOTIFY_ALL);
                    }
                    changed = true;
                }
            }
        }

        // Spawn en centros: 30% para un solo fuste.
        // Variante baja: prob baja (~5.5%) para varios fustes en la parte baja del bioma.
        for (Map.Entry<Long, CellCenterCandidate> entry : cellCenters.entrySet()) {
            CellCenterCandidate c = entry.getValue();
            if (c.edgeMetric() < edgeThreshold + 1.2) {
                continue;
            }

            if (!ABYSSAL_FUSTE_CELLS_SPAWNED.add(entry.getKey())) {
                continue;
            }

            if (hash01(c.sx(), c.sz(), seed, 77) < FUSTE_CENTER_CHANCE) {
                BlockPos centerSpawn = clampToChunkInterior(c.pos(), genChunkX, genChunkZ, CHUNK_INTERIOR_MARGIN);
                try {
                    if (AbyssalFusteFeature.generateAt(world, random, centerSpawn)) {
                        changed = true;
                    }
                } catch (IllegalStateException ex) {
                    if (DEBUG_MARKERS) {
                        VoidedDimension.LOGGER.warn("[dry_cracks] skipped center abyssal_fuste_feature at {} {} {}: {}",
                                centerSpawn.getX(), centerSpawn.getY(), centerSpawn.getZ(), ex.getMessage());
                    }
                }
            }

            // En capas bajas: baja probabilidad para varios fustes locales (sin salir del chunk actual).
            if (hash01(c.sx(), c.sz(), seed, 88) < FUSTE_LOWER_MULTI_CHANCE) {
                int multiCount = 2 + random.nextInt(2); // 2..3
                BlockPos base = clampToChunkInterior(c.pos(), genChunkX, genChunkZ, CHUNK_INTERIOR_MARGIN);
                for (int m = 0; m < multiCount; m++) {
                    int sx = base.getX() + random.nextBetween(-2, 2);
                    int sz = base.getZ() + random.nextBetween(-2, 2);
                    if ((sx >> 4) != genChunkX || (sz >> 4) != genChunkZ) {
                        continue;
                    }
                    int startY = base.getY() - random.nextBetween(32, 180);
                    BlockPos lower = drySurfaceFromY(world, sx, sz, startY);
                    if (lower == null || lower.getY() >= base.getY() - 10) {
                        continue;
                    }
                    try {
                        if (AbyssalFusteFeature.generateAt(world, random, lower)) {
                            changed = true;
                        }
                    } catch (IllegalStateException ex) {
                        if (DEBUG_MARKERS) {
                            VoidedDimension.LOGGER.warn("[dry_cracks] skipped lower abyssal_fuste_feature at {} {} {}: {}",
                                    lower.getX(), lower.getY(), lower.getZ(), ex.getMessage());
                        }
                    }
                }
            }
        }

        return changed;
    }

    private BlockState getConcreteColor(int index) {
        BlockState[] colors = {
            Blocks.WHITE_CONCRETE.getDefaultState(),
            Blocks.ORANGE_CONCRETE.getDefaultState(),
            Blocks.MAGENTA_CONCRETE.getDefaultState(),
            Blocks.LIGHT_BLUE_CONCRETE.getDefaultState(),
            Blocks.YELLOW_CONCRETE.getDefaultState(),
            Blocks.LIME_CONCRETE.getDefaultState(),
            Blocks.PINK_CONCRETE.getDefaultState(),
            Blocks.GRAY_CONCRETE.getDefaultState(),
            Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(),
            Blocks.CYAN_CONCRETE.getDefaultState(),
            Blocks.PURPLE_CONCRETE.getDefaultState(),
            Blocks.BLUE_CONCRETE.getDefaultState(),
            Blocks.BROWN_CONCRETE.getDefaultState(),
            Blocks.GREEN_CONCRETE.getDefaultState(),
            Blocks.RED_CONCRETE.getDefaultState(),
            Blocks.BLACK_CONCRETE.getDefaultState()
        };
        return colors[Math.abs(index) % colors.length];
    }

    private void generateHorizontalStressCrack(StructureWorldAccess world, BlockPos groundPos, net.minecraft.util.math.random.Random random) {
        // Grietas horizontales difuminadas sobre el suelo
        // Grosor: 3-6 bloques de profundidad (hacia abajo)
        int thickness = 3 + random.nextInt(4); // 3-6
        
        // Largo de la grieta en una dirección horizontal (4-8 bloques)
        int length = 4 + random.nextInt(5);
        
        // Elegiir dirección dominante: N/S o E/W
        boolean isNorthSouth = random.nextBoolean();
        
        // Generar patrón horizontal difuminado
        BlockPos current = groundPos.down(); // Comenzar un bloque debajo del suelo
        
        for (int depth = 0; depth < thickness; depth++) {
            // Aumentar irregularidad conforme bajamos
            int lengthVariation = (depth > 1) ? random.nextBetween(-2, 2) : 0;
            int actualLength = Math.max(2, length + lengthVariation);
            
            for (int step = 0; step <= actualLength; step++) {
                BlockPos target;
                
                if (isNorthSouth) {
                    // Grieta que va N-S con desviación E-W
                    int deviation = random.nextFloat() < 0.7f ? 0 : (random.nextBoolean() ? 1 : -1);
                    target = groundPos.down(depth + 1).north(step).east(deviation);
                } else {
                    // Grieta que va E-W con desviación N-S
                    int deviation = random.nextFloat() < 0.7f ? 0 : (random.nextBoolean() ? 1 : -1);
                    target = groundPos.down(depth + 1).east(step).north(deviation);
                }
                
                // Difuminación: no todos los bloques se reemplazan
                if (random.nextFloat() < 0.85f) {
                    BlockState state = world.getBlockState(target);
                    if (isDrySurface(state)) {
                        world.setBlockState(target, STRESS_CRACK_STATE, Block.NOTIFY_ALL);
                    }
                }
            }
        }
    }

    private void generateSpike(StructureWorldAccess world, BlockPos groundPos, net.minecraft.util.math.random.Random random) {
        // Altura promedio 3 o 4 bloques, 10% de probabilidad de ser hasta 7 bloques
        int height = random.nextFloat() < 0.10f ? 5 + random.nextInt(3) : 3 + random.nextInt(2);
        boolean hasCrystal = random.nextFloat() < 0.25f; // 25% de los spikes tienen cristales

        BlockPos currentPos = groundPos.up();
        Direction[] directions = Direction.values();

        // Convertir bloque base (suelo) al mismo material del spike para dar sensacion de que brota de la tierra
        world.setBlockState(groundPos, Blocks.BLACKSTONE.getDefaultState(), Block.NOTIFY_ALL);

        for (int step = 1; step <= height; step++) {
            if (!world.isAir(currentPos)) break;

            BlockState state = Blocks.BLACKSTONE.getDefaultState();
            float r = random.nextFloat();

            if (step == height) {
                state = Blocks.OBSIDIAN.getDefaultState();
            } else {
                if (hasCrystal && r < 0.3f) {
                    state = Blocks.AMETHYST_BLOCK.getDefaultState(); // Cristal temporal
                } else if (r < 0.5f) {
                    state = Blocks.OBSIDIAN.getDefaultState();
                } else if (r < 0.7f) {
                    state = Blocks.CRYING_OBSIDIAN.getDefaultState();
                }
            }
            
            world.setBlockState(currentPos, state, Block.NOTIFY_ALL);

            // Agregar racimos de cristales alrededor si toca el bloque de cristal
            if (hasCrystal && state.isOf(Blocks.AMETHYST_BLOCK) && random.nextFloat() < 0.6f) {
                for (Direction dir : Direction.Type.HORIZONTAL) {
                    if (random.nextFloat() < 0.4f) {
                        BlockPos side = currentPos.offset(dir);
                        if (world.isAir(side)) {
                            world.setBlockState(side, Blocks.AMETHYST_CLUSTER.getDefaultState().with(net.minecraft.state.property.Properties.FACING, dir), Block.NOTIFY_ALL);
                        }
                    }
                }
            }

            // Distorsión del crecimiento: Moverse en una dirección aleatoria de forma que cause dobleces.
            // Para no quedar flotando en ángulos agudos o imposibles, forzamos un pilar auxiliar debajó si avanzamos horizontal.
            if (step < height && random.nextFloat() < 0.4f) {
                Direction moveDir = directions[random.nextInt(directions.length)];
                if (moveDir.getAxis() != Direction.Axis.Y) { // Solo si es horizontal
                    BlockPos nextPos = currentPos.offset(moveDir);
                    if (world.isAir(nextPos) && !world.isAir(nextPos.down())) {
                        currentPos = nextPos;
                    } else if (world.isAir(nextPos) && world.isAir(nextPos.down())) {
                         // Rellenar debajo para justificar el quiebre y que no flote
                         world.setBlockState(nextPos.down(), Blocks.BLACKSTONE.getDefaultState(), Block.NOTIFY_ALL);
                         currentPos = nextPos;
                    } else {
                        currentPos = currentPos.up(); // Avance normal hacia arriba
                    }
                } else if (moveDir == Direction.UP) {
                     currentPos = currentPos.up();
                } else {
                     currentPos = currentPos.up();
                }
            } else {
                 currentPos = currentPos.up(); // Avance normal vertical
            }
        }
    }

    private boolean carveSafeEdgeCrack(StructureWorldAccess world, BlockPos top, int maxDepth) {
        BlockState topState = world.getBlockState(top);
        if (!isDrySurface(topState)) {
            return false;
        }

        int carved = 0;
        BlockPos.Mutable mut = top.mutableCopy();
        while (carved < maxDepth && mut.getY() >= MIN_Y) {
            BlockState state = world.getBlockState(mut);
            if (!isDrySurface(state)) {
                break;
            }
            world.setBlockState(mut, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            carved++;
            mut.move(Direction.DOWN);
        }

        if (carved == 0) {
            return false;
        }

        BlockPos floorPos = top.down(carved);
        BlockState floor = world.getBlockState(floorPos);
        if (floor.isAir()) {
            // Si debajo hay vacío, colocamos fondo sólido para que no sea caída infinita.
            world.setBlockState(floorPos, safeFloorState(topState), Block.NOTIFY_ALL);
        }

        return true;
    }

    private BlockState safeFloorState(BlockState referenceTopState) {
        if (referenceTopState.isOf(ModBlocks.DIRT_ASHE)) {
            return ModBlocks.DIRT_ASHE.getDefaultState();
        }
        if (referenceTopState.isOf(ModBlocks.SAND_ASHE)) {
            return ModBlocks.SAND_ASHE.getDefaultState();
        }
        if (referenceTopState.isOf(ModBlocks.ASHE)) {
            return ModBlocks.ASHE.getDefaultState();
        }
        if (referenceTopState.isOf(ModBlocks.FRACTURED_COBBLESTONE)) {
            return ModBlocks.FRACTURED_COBBLESTONE.getDefaultState();
        }
        if (referenceTopState.isOf(ModBlocks.FRACTURED_STONE)) {
            return ModBlocks.FRACTURED_STONE.getDefaultState();
        }
        if (referenceTopState.isOf(Blocks.CRACKED_STONE_BRICKS)) {
            return Blocks.CRACKED_STONE_BRICKS.getDefaultState();
        }
        if (referenceTopState.isOf(Blocks.ANDESITE)) {
            return Blocks.ANDESITE.getDefaultState();
        }
        if (referenceTopState.isOf(Blocks.COBBLESTONE)) {
            return Blocks.COBBLESTONE.getDefaultState();
        }
        return Blocks.STONE.getDefaultState();
    }

    private int getCellColorIndex(double x, double z, int cellSize, long seed) {
        int cellX = Math.floorDiv((int)Math.floor(x), cellSize);
        int cellZ = Math.floorDiv((int)Math.floor(z), cellSize);
        double f1 = Double.MAX_VALUE;
        int bestHash = 0;

        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                int sx = cellX + ox;
                int sz = cellZ + oz;

                // Altera fuertemente la posición de los "puntos fuertes" (centros de celdas) Voronoi
                // Con esto distorsionamos la topología general de la celda haciendolas más diagonales/estiradas.
                double jx = 0.1d + (0.8d * hash01(sx, sz, seed, 0));
                double jz = 0.1d + (0.8d * hash01(sx, sz, seed, 1));
                
                // Variación de escala en vértices
                double px = (sx * cellSize) + (jx * cellSize);
                double pz = (sz * cellSize) + (jz * cellSize);

                double ddx = x - px;
                // Escalamos el eje Z localmente para forzar formas diagonales u oblongas
                double ddz = (z - pz) * 1.5;
                double distSq = (ddx * ddx) + (ddz * ddz);

                if (distSq < f1) {
                    f1 = distSq;
                    bestHash = sx * 31 + sz;
                }
            }
        }
        return bestHash;
    }

    private double voronoiEdgeMetric(double x, double z, int cellSize, long seed) {
        int cellX = Math.floorDiv((int)Math.floor(x), cellSize);
        int cellZ = Math.floorDiv((int)Math.floor(z), cellSize);
        double f1 = Double.MAX_VALUE;
        double f2 = Double.MAX_VALUE;

        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                int sx = cellX + ox;
                int sz = cellZ + oz;

                double jx = 0.1d + (0.8d * hash01(sx, sz, seed, 0));
                double jz = 0.1d + (0.8d * hash01(sx, sz, seed, 1));
                double px = (sx * cellSize) + (jx * cellSize);
                double pz = (sz * cellSize) + (jz * cellSize);

                double ddx = x - px;
                // Quitar deformación excesiva temporalmente para el debug
                double ddz = z - pz; // Quitar deformación excesiva temporalmente para el debug
                double distSq = (ddx * ddx) + (ddz * ddz);

                if (distSq < f1) {
                    f2 = f1;
                    f1 = distSq;
                } else if (distSq < f2) {
                    f2 = distSq;
                }
            }
        }

        return Math.sqrt(f2) - Math.sqrt(f1);
    }

    private double hash01(int x, int z, long seed, int salt) {
        long h = seed;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= (long) salt * 0x165667B19E3779F9L;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdl;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53l;
        h ^= (h >>> 33);

        long mantissa = (h >>> 11) & ((1L << 53) - 1);
        return mantissa / (double) (1L << 53);
    }

    private void paintRim(StructureWorldAccess world, BlockPos center) {
        // Deshabilitado temporalmente el pintado de borde blanco para que no se vea sobrecargado/exagerado
        /*
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos rim = center.offset(direction);
            BlockState rimState = world.getBlockState(rim);
            if (rimState.isAir() || rimState.isOf(Blocks.BEDROCK) || !world.isAir(rim.up())) {
                continue;
            }
            world.setBlockState(rim, Blocks.WHITE_CONCRETE.getDefaultState(), Block.NOTIFY_ALL);
        }
        */
    }

    private boolean carveDown(StructureWorldAccess world, BlockPos top, int depth) {
        boolean carved = false;
        for (int d = 0; d < depth && d < MAX_CRACK_DEPTH; d++) {
            BlockPos p = top.down(d);
            BlockState state = world.getBlockState(p);
            if (!isDrySurface(state)) {
                break;
            }
            world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            carved = true;
        }
        return carved;
    }

    private Site findValidSite(StructureWorldAccess world, BlockPos origin, net.minecraft.util.math.random.Random random) {
        for (int i = 0; i < SITE_SEARCH_TRIES; i++) {
            int x = origin.getX() + random.nextInt(16);
            int z = origin.getZ() + random.nextInt(16);
            BlockPos top = groundSurfacePos(world, x, z);
            if (top == null) {
                continue;
            }
            if (!isDrySurface(world.getBlockState(top))) {
                continue;
            }
            // Forzar que el origen sea un area mayormente horizontal
            if (!isLocallyFlat(world, top, 4, 1)) {
                continue;
            }
            int width = localMassiveWidth(world, top);
            if (width < MIN_MASSIVE_WIDTH) {
                continue;
            }
            return new Site(top, Math.max(3, width));
        }

        BlockPos fallback = groundSurfacePos(world, origin.getX() + 8, origin.getZ() + 8);
        if (fallback != null && isDrySurface(world.getBlockState(fallback)) && isLocallyFlat(world, fallback, 2, 1)) {
            return new Site(fallback, Math.max(3, localMassiveWidth(world, fallback)));
        }

        return null;
    }

    private void markDebug(StructureWorldAccess world, BlockPos ground, BlockState marker) {
        // Apagado para no generar bloques extraños fofos en el aire (concreto lima/magenta)
        /*
        BlockPos markerPos = ground.up();
        if (world.isAir(markerPos)) {
            world.setBlockState(markerPos, marker, Block.NOTIFY_ALL);
        }
        */
    }

    private void notifyDebug(StructureWorldAccess world, BlockPos pos, int radius, int cellSize, double edgeThreshold) {
        String msg = String.format(
                "[dry_cracks] generated at x=%d y=%d z=%d | chunk=%d,%d | radius=%d cell=%d edge<=%.2f",
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                pos.getX() >> 4,
                pos.getZ() >> 4,
                radius,
                cellSize,
                edgeThreshold
        );
        VoidedDimension.LOGGER.info(msg);
    }

    private BlockPos groundSurfacePos(StructureWorldAccess world, int x, int z) {
        BlockPos top = surfacePos(world, x, z);
        if (top == null) {
            return null;
        }

        BlockPos.Mutable cursor = top.mutableCopy();
        // Aumentamos scan de descenso
        int minY = Math.max(MIN_Y, top.getY() - 120);
        while (cursor.getY() >= minY && (world.getBlockState(cursor).isAir() || world.getBlockState(cursor).isOf(Blocks.BEDROCK))) {
            cursor.move(Direction.DOWN);
        }

        if (cursor.getY() < MIN_Y) {
            return null;
        }
        return cursor.toImmutable();
    }

    private int localMassiveWidth(StructureWorldAccess world, BlockPos center) {
        int xSpan = spanInAxis(world, center, 1, 0) + spanInAxis(world, center, -1, 0) + 1;
        int zSpan = spanInAxis(world, center, 0, 1) + spanInAxis(world, center, 0, -1) + 1;
        return Math.max(xSpan, zSpan);
    }

    private int spanInAxis(StructureWorldAccess world, BlockPos center, int dx, int dz) {
        int span = 0;
        int baseY = center.getY();
        for (int i = 1; i <= MAX_SPAN_SCAN; i++) {
            BlockPos p = new BlockPos(center.getX() + (dx * i), baseY, center.getZ() + (dz * i));
            BlockPos top = groundSurfacePos(world, p.getX(), p.getZ());
            if (top == null) {
                break;
            }
            if (Math.abs(top.getY() - baseY) > 1) {
                break;
            }
            if (!isDrySurface(world.getBlockState(top))) {
                break;
            }
            span++;
        }
        return span;
    }

    private int depthFromWidth(int width) {
        if (width >= FULL_DEPTH_WIDTH) {
            return 3;
        }
        if (width >= 9) {
            return 2;
        }
        if (width >= MIN_MASSIVE_WIDTH) {
            return 1;
        }
        return 1;
    }

    private boolean isLocallyFlat(StructureWorldAccess world, BlockPos center, int radius, int maxDelta) {
        int baseY = center.getY();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos p = groundSurfacePos(world, center.getX() + dx, center.getZ() + dz);
                if (p == null) {
                    return false;
                }
                if (Math.abs(p.getY() - baseY) > maxDelta) {
                    return false;
                }
            }
        }
        return true;
    }

    private BlockPos surfacePos(StructureWorldAccess world, int x, int z) {
        // En vez de Heightmap (q devuelve el techo), bajamos desde MAX_Y buscando el primer bloque solido.
        BlockPos.Mutable cursor = new BlockPos.Mutable(x, MAX_Y, z);
        while (cursor.getY() >= MIN_Y && (world.getBlockState(cursor).isAir() || world.getBlockState(cursor).isOf(Blocks.BEDROCK))) {
            cursor.move(Direction.DOWN);
        }
        
        if (cursor.getY() < MIN_Y) {
            return null;
        }

        return cursor.toImmutable();
    }

    private BlockPos drySurfaceFromY(StructureWorldAccess world, int x, int z, int startY) {
        BlockPos.Mutable cursor = new BlockPos.Mutable(x, Math.min(MAX_Y, Math.max(MIN_Y, startY)), z);
        while (cursor.getY() >= MIN_Y) {
            BlockState state = world.getBlockState(cursor);
            if (!state.isAir() && !state.isOf(Blocks.BEDROCK) && isDrySurface(state)) {
                return cursor.toImmutable();
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private CellSample nearestCellSample(double x, double z, int cellSize, long seed) {
        int cellX = Math.floorDiv((int) Math.floor(x), cellSize);
        int cellZ = Math.floorDiv((int) Math.floor(z), cellSize);
        double f1 = Double.MAX_VALUE;
        double f2 = Double.MAX_VALUE;
        int bestX = cellX;
        int bestZ = cellZ;

        for (int ox = -2; ox <= 2; ox++) {
            for (int oz = -2; oz <= 2; oz++) {
                int sx = cellX + ox;
                int sz = cellZ + oz;
                double jx = 0.1d + (0.8d * hash01(sx, sz, seed, 0));
                double jz = 0.1d + (0.8d * hash01(sx, sz, seed, 1));
                double px = (sx * cellSize) + (jx * cellSize);
                double pz = (sz * cellSize) + (jz * cellSize);
                double ddx = x - px;
                double ddz = z - pz;
                double distSq = (ddx * ddx) + (ddz * ddz);

                if (distSq < f1) {
                    f2 = f1;
                    f1 = distSq;
                    bestX = sx;
                    bestZ = sz;
                } else if (distSq < f2) {
                    f2 = distSq;
                }
            }
        }

        double edge = Math.sqrt(f2) - Math.sqrt(f1);
        return new CellSample(bestX, bestZ, edge);
    }

    private long toCellKey(int sx, int sz) {
        return (((long) sx) << 32) ^ (sz & 0xffffffffL);
    }

    private BlockPos clampToChunkInterior(BlockPos pos, int chunkX, int chunkZ, int margin) {
        int minX = (chunkX << 4) + margin;
        int maxX = (chunkX << 4) + (15 - margin);
        int minZ = (chunkZ << 4) + margin;
        int maxZ = (chunkZ << 4) + (15 - margin);
        int x = Math.max(minX, Math.min(maxX, pos.getX()));
        int z = Math.max(minZ, Math.min(maxZ, pos.getZ()));
        return new BlockPos(x, pos.getY(), z);
    }

    private boolean isDrySurface(BlockState state) {
        return state.isOf(Blocks.STONE)
                || state.isOf(Blocks.COBBLESTONE)
                || state.isOf(Blocks.ANDESITE)
                || state.isOf(Blocks.CRACKED_STONE_BRICKS)
                || state.isOf(Blocks.WHITE_CONCRETE)
                || state.isOf(ModBlocks.DIRT_ASHE)
                || state.isOf(ModBlocks.SAND_ASHE)
                || state.isOf(ModBlocks.ASHE)
                || state.isOf(ModBlocks.FRACTURED_STONE)
                || state.isOf(ModBlocks.FRACTURED_COBBLESTONE)
                || state.isOf(Blocks.BEDROCK);
    }

    private record Site(BlockPos pos, int width) { }
    private record CellSample(int sx, int sz, double edgeMetric) { }
    private record CellCenterCandidate(BlockPos pos, int sx, int sz, double edgeMetric) { }
}

