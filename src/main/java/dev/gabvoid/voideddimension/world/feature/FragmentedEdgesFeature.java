package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
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
    
    // Bloques mazicos que encajan de manera limpia
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.getDefaultState();
    private static final BlockState GLOWSTONE = Blocks.GLOWSTONE.getDefaultState();
    private static final BlockState SMOOTH_BASALT = Blocks.SMOOTH_BASALT.getDefaultState();
    
    private static final int MIN_Y = 240; // Restringido para que se alinee con las capas del bioma y no estorbe (240 - 360) cambiada la altura min
    private static final int MAX_Y = 360;
    private static final int CHUNK_ATTEMPTS = 8; // Disminuido 60% la cantidad de intentos por chunk
    private static final int LOCAL_SEARCH_TRIES = 24;
    private static final int HORIZONTAL_SEARCH = 12;
    private static final int COLUMN_SCAN_RANGE = 30;
    private static final int MIN_AIR_DEPTH = 3; 

    public FragmentedEdgesFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();
        boolean placedAny = false;
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;

        // --- 1. Generación de Obsidianas x3 ---
        int obsidianAttempts = CHUNK_ATTEMPTS * 3; 
        for (int attempt = 0; attempt < obsidianAttempts; attempt++) {
            int startX = origin.getX() + random.nextInt(16);
            int startZ = origin.getZ() + random.nextInt(16);
            int startY = origin.getY() + random.nextBetween(-20, 20); 
            
            CrackEdge edge = locateCrackEdge(world, new BlockPos(startX, startY, startZ), random);
            if (edge == null) continue;
            
            if (buildSolidPillarSupport(world, edge.hostPos(), edge.airSide(), random, originChunkX, originChunkZ)) {
                placedAny = true;
            }
        }

        // --- 2. Generación normal de Glowstone y Plataformas Mixtas ---
        for (int attempt = 0; attempt < CHUNK_ATTEMPTS; attempt++) {
            int startX = origin.getX() + random.nextInt(16);
            int startZ = origin.getZ() + random.nextInt(16);
            int startY = origin.getY() + random.nextBetween(-20, 20); 
            
            CrackEdge edge = locateCrackEdge(world, new BlockPos(startX, startY, startZ), random);
            if (edge == null) continue;
            
            if (placeMassiveDecoration(world, edge, random, originChunkX, originChunkZ)) {
                placedAny = true;
            }
        }
        return placedAny;
    }

    private CrackEdge locateCrackEdge(StructureWorldAccess world, BlockPos start, Random random) {
        BlockPos.Mutable columnPos = start.mutableCopy();
        for (int attempt = 0; attempt < LOCAL_SEARCH_TRIES; attempt++) {
            int sampleX = start.getX() + randomOffset(random, HORIZONTAL_SEARCH);
            int sampleY = MathHelper.clamp(start.getY() + randomOffset(random, COLUMN_SCAN_RANGE), 1, MAX_Y);
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

    private boolean placeMassiveDecoration(StructureWorldAccess world, CrackEdge edge, Random random, int chunkX, int chunkZ) {
        BlockPos base = edge.hostPos();
        Direction airSide = edge.airSide();

        // Decidimos quÃ© tipo de estructura maciza generar en la pared de la grieta
        double r = random.nextDouble();

        if (r < 0.15) { // Glowstone
            return carveGlowstoneNodule(world, base, airSide, random, chunkX, chunkZ);
        } else { // El espacio restante es para las plataformas mixtas de Basalto y Crumby Abyss
            return buildMixedPlatformStub(world, base, airSide, random, chunkX, chunkZ);
        }
    }

    // Un puente macizo roto de basalto saliendo de la pared (sin flotar ni ser disperso), con bloques mixtos
    private boolean buildMixedPlatformStub(StructureWorldAccess world, BlockPos base, Direction airSide, Random random, int chunkX, int chunkZ) {
        boolean placed = false;
        // Reducir la longitud del basalto en un 40% (aprox 4 a 7 en lugar de 6 a 12)
        int length = random.nextBetween(4, 7);
        // Hacerlo un poco mÃ¡s ancho o variable para que acompaÃ±e el nivel de masividad
        int width = random.nextBetween(1, 2);
        // Generar un grosor de 2 a 3 bloques
        int thickness = random.nextBetween(2, 3);
        
        BlockState crumbly = ModBlocks.CRUMBLY_ABYSS.getDefaultState();

        // Determinar focos (clusters) para los bloques quebradizos, asÃ­ estarÃ¡n juntos
        int numClusters = random.nextFloat() < 0.7f ? 1 : 2;
        float[][] clusters = new float[numClusters][3];
        for (int i = 0; i < numClusters; i++) {
            clusters[i][0] = random.nextBetween(1, length - 1); // step
            clusters[i][1] = random.nextBetween(-width, width); // lat
            clusters[i][2] = random.nextFloat() * 1.5f + 1.0f; // radius
        }

        for (int step = 0; step < length; step++) {
            for (int lat = -width; lateralStep(lat, width); lat++) {
                // Iterar tambiÃ©n el grosor (hacia abajo)
                for (int dy = 0; dy > -thickness; dy--) {
                    BlockPos target = base.offset(airSide, step + 1).offset(airSide.rotateYClockwise(), lat).up(dy);
                    if (!isInChunk(target, chunkX, chunkZ)) continue;
                    if (!world.isAir(target)) continue;

                    // Darle forma curva o de arco roto (los laterales pueden ser mas cortos)
                    if (step > length / 2 && Math.abs(lat) == width && random.nextFloat() < 0.6f) continue;
                    if (step > length - 2 && random.nextFloat() < 0.7f) continue;
                    // Reducir grosor en la punta para que se vea mas orgÃ¡nico el quiebre
                    if (dy < 0 && step > length - 3 && random.nextBoolean()) continue;
                    
                    // Verificar si estamos dentro del cluster quebradizo
                    boolean isCrumbly = false;
                    for (int i = 0; i < numClusters; i++) {
                        float ds = step - clusters[i][0];
                        float dl = lat - clusters[i][1];
                        // le damos menos peso a dy para que afecte columnas enteras
                        float distSq = ds * ds + dl * dl + (dy * dy * 0.25f);
                        if (distSq <= clusters[i][2] * clusters[i][2]) {
                            isCrumbly = true;
                            break;
                        }
                    }

                    // AÃ±adir ligera imperfecciÃ³n a los bordes del cluster
                    if (isCrumbly && random.nextFloat() < 0.1f) isCrumbly = false;
                    
                    BlockState blockToPlace = isCrumbly ? crumbly : SMOOTH_BASALT;

                    world.setBlockState(target, blockToPlace, Block.NOTIFY_ALL);
                    placed = true;
                }
            }
        }
        return placed;
    }

    // Un refuerzo vertical macizo y limpio en la pared de la grieta
    private boolean buildSolidPillarSupport(StructureWorldAccess world, BlockPos base, Direction airSide, Random random, int chunkX, int chunkZ) {
        boolean placed = false;
        // Que parezca que estÃ¡ cayendo: se extiende mÃ¡s hacia abajo, promedio 8 a 11
        int height = random.nextBetween(8, 11);
        BlockPos current = base.offset(airSide);

        for (int dy = 0; dy > -height; dy--) {
            BlockPos target = current.up(dy);
            if (!isInChunk(target, chunkX, chunkZ)) continue;
            if (!world.isAir(target)) break;

            world.setBlockState(target, OBSIDIAN, Block.NOTIFY_ALL);
            placed = true;
        }
        return placed;
    }

    // IncrustaciÃ³n de Glowstone limpia y concentrada
    private boolean carveGlowstoneNodule(StructureWorldAccess world, BlockPos base, Direction airSide, Random random, int chunkX, int chunkZ) {
        boolean placed = false;
        int radius = random.nextBetween(1, 2);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx * dx + dy * dy > radius * radius + random.nextFloat()) continue;

                BlockPos target = base.up(dy).offset(airSide.rotateYClockwise(), dx);
                if (!isInChunk(target, chunkX, chunkZ)) continue;
                if (isCrackHost(world.getBlockState(target))) {
                    world.setBlockState(target, GLOWSTONE, Block.NOTIFY_ALL);
                    placed = true;
                }
            }
        }
        return placed;
    }

    private boolean isInChunk(BlockPos pos, int chunkX, int chunkZ) {
        return (pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ;
    }

    private boolean lateralStep(int lat, int width) {
        return lat <= width;
    }

    private boolean isCrackHost(BlockState state) {
        return state.isOf(Blocks.STONE) || state.isOf(Blocks.COBBLESTONE) || state.isOf(Blocks.ANDESITE) 
                || state.isOf(Blocks.CRACKED_STONE_BRICKS) || state.isOf(ModBlocks.CURSE_STONE_BLOCK)
                || state.isOf(ModBlocks.CURSE_COBBLESTONE_BLOCK) || state.isOf(ModBlocks.ASHE) 
                || state.isOf(ModBlocks.DIRT_ASHE) || state.isOf(ModBlocks.SAND_ASHE)
                || state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.DEEPSLATE);
    }

    private int randomOffset(Random random, int range) {
        if (range <= 0) return 0;
        return random.nextBetween(-range, range);
    }

    private static final class CrackEdge {
        private final BlockPos hostPos;
        private final Direction airSide;

        private CrackEdge(BlockPos hostPos, Direction airSide) {
            this.hostPos = hostPos;
            this.airSide = airSide;
        }
        public BlockPos hostPos() { return hostPos; }
        public Direction airSide() { return airSide; }
    }
}


