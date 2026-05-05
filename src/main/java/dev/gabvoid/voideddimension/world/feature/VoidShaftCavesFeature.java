package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import dev.gabvoid.voideddimension.world.ModDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class VoidShaftCavesFeature extends Feature<DefaultFeatureConfig> {
    private static final int START_Y = 350;
    private static final int END_Y_MIN = 8;
    private static final int END_Y_MAX = 20;
    private static final int REGION_SIZE_CHUNKS = 3;
    private static final double REGION_SPAWN_CHANCE = 0.28;
    private static final int REGION_SCAN_RADIUS = 2;
    private static final int HORIZONTAL_LEN_MIN = 100;
    private static final int HORIZONTAL_LEN_MAX = 220;
    private static final int VERTICAL_LEN_MIN = 100;
    private static final int VERTICAL_LEN_MAX = 280;
    private static final float MACRO_CHAMBER_CHANCE = 0.12f;
    private static final float HORIZONTAL_BRANCH_CHANCE = 0.50f;
    private static final float VERTICAL_SPLICE_CHANCE = 0.18f;
    private static final float HORIZONTAL_SPLICE_CHANCE = 0.14f;
    private static final double SHAPE_NOISE_FREQ = 0.12;
    private static final double FLOOR_BREAK_NOISE_FREQ = 0.22;

    public VoidShaftCavesFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();

        // Solo dentro de la dimension voided.
        if (!world.toServerWorld().getRegistryKey().equals(ModDimensions.VOIDED_DIMENSION_KEY)) {
            return false;
        }

        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;

        int regionX = Math.floorDiv(chunkX, REGION_SIZE_CHUNKS);
        int regionZ = Math.floorDiv(chunkZ, REGION_SIZE_CHUNKS);
        long seed = world.getSeed();
        boolean changed = false;
        int chunkCenterX = (chunkX << 4) + 8;
        int chunkCenterZ = (chunkZ << 4) + 8;

        for (int rz = -REGION_SCAN_RADIUS; rz <= REGION_SCAN_RADIUS; rz++) {
            for (int rx = -REGION_SCAN_RADIUS; rx <= REGION_SCAN_RADIUS; rx++) {
                int rX = regionX + rx;
                int rZ = regionZ + rz;
                if (!regionEnabled(rX, rZ, seed)) {
                    continue;
                }

                int vX = regionBlockX(rX, rZ, seed, 23);
                int vZ = regionBlockZ(rX, rZ, seed, 37);
                int hX = regionBlockX(rX, rZ, seed, 43);
                int hZ = regionBlockZ(rX, rZ, seed, 47);

                int maxDist = 170;
                if (Math.abs(vX - chunkCenterX) > maxDist && Math.abs(hX - chunkCenterX) > maxDist) {
                    continue;
                }
                if (Math.abs(vZ - chunkCenterZ) > maxDist && Math.abs(hZ - chunkCenterZ) > maxDist) {
                    continue;
                }

                int verticalLen = verticalLengthForRegion(rX, rZ, seed);
                int verticalBottom = Math.max(END_Y_MIN, START_Y - verticalLen);
                int verticalRadius = 4 + positiveMod((int) hashRegion(rX, rZ, seed, 53), 5);
                changed |= carveConnector(world, new BlockPos(vX, START_Y, vZ), new BlockPos(vX, verticalBottom, vZ), verticalRadius, chunkX, chunkZ);

                int horizontalLen = horizontalLengthForRegion(rX, rZ, seed);
                int horizontalY = Math.max(END_Y_MIN + 8, START_Y - (35 + positiveMod((int) hashRegion(rX, rZ, seed, 59), 120)));
                double angle = toUnitDouble(hashRegion(rX, rZ, seed, 61)) * (Math.PI * 2.0);
                int half = horizontalLen / 2;
                int hx1 = hX - (int) Math.round(Math.cos(angle) * half);
                int hz1 = hZ - (int) Math.round(Math.sin(angle) * half);
                int hx2 = hX + (int) Math.round(Math.cos(angle) * half);
                int hz2 = hZ + (int) Math.round(Math.sin(angle) * half);
                int horizontalRadius = 3 + positiveMod((int) hashRegion(rX, rZ, seed, 67), 4);
                changed |= carveConnector(world, new BlockPos(hx1, horizontalY, hz1), new BlockPos(hx2, horizontalY + randomYOffset(seed, rX, rZ, 71, 5), hz2), horizontalRadius, chunkX, chunkZ);

                // Nodo principal: conecta vertical y horizontal dentro de la misma region.
                int joinY = Math.max(verticalBottom + 4, Math.min(START_Y - 4, horizontalY + randomYOffset(seed, rX, rZ, 73, 12)));
                changed |= carveConnector(world, new BlockPos(vX, joinY, vZ), new BlockPos(hX, horizontalY, hZ), Math.max(3, horizontalRadius), chunkX, chunkZ);

                // Nodo secundario opcional: enlaza con region vecina para red no siempre perfecta.
                if (toUnitDouble(hashRegion(rX, rZ, seed, 79)) < 0.42) {
                    int dir = positiveMod((int) hashRegion(rX, rZ, seed, 83), 4);
                    int nrX = rX + (dir == 0 ? 1 : dir == 1 ? -1 : 0);
                    int nrZ = rZ + (dir == 2 ? 1 : dir == 3 ? -1 : 0);
                    if (regionEnabled(nrX, nrZ, seed)) {
                        int nvX = regionBlockX(nrX, nrZ, seed, 23);
                        int nvZ = regionBlockZ(nrX, nrZ, seed, 37);
                        int nBottom = Math.max(END_Y_MIN, START_Y - verticalLengthForRegion(nrX, nrZ, seed));
                        int ny = Math.max(nBottom + 4, Math.min(joinY + randomYOffset(seed, rX, rZ, 89, 8), START_Y - 4));
                        changed |= carveConnector(world, new BlockPos(hX, horizontalY, hZ), new BlockPos(nvX, ny, nvZ), Math.max(2, horizontalRadius - 1), chunkX, chunkZ);
                    }
                }
            }
        }

        return changed;
    }

    private boolean carveSphere(StructureWorldAccess world, int cx, int cy, int cz, int r, int chunkX, int chunkZ) {
        boolean changed = false;
        long seed = world.getSeed();

        // Elipsoide anisotropico por llamada para que cada tramo sea distinto.
        float rx = Math.max(1.2f, r * (0.85f + (float) (hash3(cx, cy, cz, seed, 101) * 0.8f)));
        float ry = Math.max(1.0f, r * (0.55f + (float) (hash3(cx, cy, cz, seed, 103) * 0.45f)));
        float rz = Math.max(1.2f, r * (0.85f + (float) (hash3(cx, cy, cz, seed, 107) * 0.8f)));
        int irx = Math.max(1, (int) Math.ceil(rx));
        int iry = Math.max(1, (int) Math.ceil(ry));
        int irz = Math.max(1, (int) Math.ceil(rz));

        for (int dx = -irx; dx <= irx; dx++) {
            for (int dy = -iry; dy <= iry; dy++) {
                for (int dz = -irz; dz <= irz; dz++) {
                    double nx = Math.abs(dx / rx);
                    double ny = Math.abs(dy / ry);
                    double nz = Math.abs(dz / rz);

                    // Perfil menos liso: mezcla superelipse + componente angular.
                    double superEllipse = Math.pow(nx, 2.35) + Math.pow(ny, 2.0) + Math.pow(nz, 2.35);
                    double angular = Math.max(nx, nz) * 0.32;
                    double base = superEllipse + angular;

                    int x = cx + dx;
                    int y = cy + dy;
                    int z = cz + dz;

                    // Filtro temprano para no gastar ruido en voxeles fuera del chunk/altura.
                    if ((x >> 4) != chunkX || (z >> 4) != chunkZ) {
                        continue;
                    }
                    if (y <= 0 || y >= 511) {
                        continue;
                    }

                    // Ruido tipo Perlin para destruir en patron organico (no hueco cuadrado/liso).
                    double shapeNoise = fbmPerlin3(
                            x * SHAPE_NOISE_FREQ,
                            y * SHAPE_NOISE_FREQ,
                            z * SHAPE_NOISE_FREQ,
                            seed,
                            131,
                            3
                    );
                    double threshold = 1.0 + ((shapeNoise - 0.5) * 1.20);

                    // Ruptura del "suelo" del hueco con Perlin 2D para que se vea quebrado.
                    if (dy <= 1) {
                        double floorNoise = fbmPerlin2(x * FLOOR_BREAK_NOISE_FREQ, z * FLOOR_BREAK_NOISE_FREQ, seed, 197, 3);
                        double floorDepth = Math.min(1.0, Math.abs(dy) / Math.max(1.0, iry));
                        threshold += ((floorNoise - 0.5) * 1.35) * floorDepth;
                    }

                    if (base > threshold) {
                        continue;
                    }

                    // Borde suavizado: evita zonas excesivamente lisas o cortes duros.
                    double softness = threshold - base;
                    if (softness < 0.16) {
                        double edgeNoise = fbmPerlin3(x * (SHAPE_NOISE_FREQ * 1.8), y * (SHAPE_NOISE_FREQ * 1.4), z * (SHAPE_NOISE_FREQ * 1.8), seed, 211, 2);
                        if (edgeNoise > (softness / 0.16)) {
                            continue;
                        }
                    }

                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.BEDROCK)) {
                        continue;
                    }

                    if (!state.isAir()) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    private boolean carveConnector(StructureWorldAccess world, BlockPos from, BlockPos to, int radius, int chunkX, int chunkZ) {
        boolean changed = false;
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        int steps = Math.max(4, Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))));

        if (!segmentMayAffectChunk(from, to, chunkX, chunkZ, radius + 3)) {
            return false;
        }

        int stepStride = Math.max(1, steps / 64);

        for (int i = 0; i <= steps; i += stepStride) {
            float t = i / (float) steps;
            int x = Math.round(from.getX() + (dx * t));
            int y = Math.round(from.getY() + (dy * t));
            int z = Math.round(from.getZ() + (dz * t));
            int rr = Math.max(1, radius + (i % 3 == 0 ? 1 : 0));

            // Evita tallado costoso fuera del entorno del chunk actual.
            if (!isNearChunk(x, z, chunkX, chunkZ, rr + 3)) {
                continue;
            }
            changed |= carveSphere(world, x, y, z, rr, chunkX, chunkZ);
        }

        // Garantiza cerrar el conector en el punto final.
        changed |= carveSphere(world, to.getX(), to.getY(), to.getZ(), Math.max(1, radius), chunkX, chunkZ);

        return changed;
    }

    private boolean segmentMayAffectChunk(BlockPos from, BlockPos to, int chunkX, int chunkZ, int margin) {
        int minSegX = Math.min(from.getX(), to.getX()) - margin;
        int maxSegX = Math.max(from.getX(), to.getX()) + margin;
        int minSegZ = Math.min(from.getZ(), to.getZ()) - margin;
        int maxSegZ = Math.max(from.getZ(), to.getZ()) + margin;

        int chunkMinX = chunkX << 4;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunkZ << 4;
        int chunkMaxZ = chunkMinZ + 15;

        return !(maxSegX < chunkMinX || minSegX > chunkMaxX || maxSegZ < chunkMinZ || minSegZ > chunkMaxZ);
    }

    private boolean isNearChunk(int x, int z, int chunkX, int chunkZ, int margin) {
        int minX = (chunkX << 4) - margin;
        int maxX = (chunkX << 4) + 15 + margin;
        int minZ = (chunkZ << 4) - margin;
        int maxZ = (chunkZ << 4) + 15 + margin;
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    private boolean regionEnabled(int regionX, int regionZ, long seed) {
        return toUnitDouble(hashRegion(regionX, regionZ, seed, 11)) < REGION_SPAWN_CHANCE;
    }

    private int regionBlockX(int regionX, int regionZ, long seed, int salt) {
        int localChunkX = positiveMod((int) hashRegion(regionX, regionZ, seed, salt), REGION_SIZE_CHUNKS);
        return (((regionX * REGION_SIZE_CHUNKS) + localChunkX) << 4) + 8;
    }

    private int regionBlockZ(int regionX, int regionZ, long seed, int salt) {
        int localChunkZ = positiveMod((int) hashRegion(regionX, regionZ, seed, salt), REGION_SIZE_CHUNKS);
        return (((regionZ * REGION_SIZE_CHUNKS) + localChunkZ) << 4) + 8;
    }

    private int verticalLengthForRegion(int regionX, int regionZ, long seed) {
        double tier = toUnitDouble(hashRegion(regionX, regionZ, seed, 97));
        if (tier < 0.22) {
            return START_Y - END_Y_MIN;
        }
        return VERTICAL_LEN_MIN + positiveMod((int) hashRegion(regionX, regionZ, seed, 101), (VERTICAL_LEN_MAX - VERTICAL_LEN_MIN) + 1);
    }

    private int horizontalLengthForRegion(int regionX, int regionZ, long seed) {
        return HORIZONTAL_LEN_MIN + positiveMod((int) hashRegion(regionX, regionZ, seed, 103), (HORIZONTAL_LEN_MAX - HORIZONTAL_LEN_MIN) + 1);
    }

    private int randomYOffset(long seed, int regionX, int regionZ, int salt, int amplitude) {
        return positiveMod((int) hashRegion(regionX, regionZ, seed, salt), (amplitude * 2) + 1) - amplitude;
    }

    private static float lerp(float a, float b, float t) {
        return a + ((b - a) * t);
    }

    private static long hashRegion(int x, int z, long seed, int salt) {
        long h = seed;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        h ^= (long) salt * 0x165667B19E3779F9L;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }

    private static int positiveMod(int value, int mod) {
        int m = value % mod;
        return m < 0 ? m + mod : m;
    }

    private static double toUnitDouble(long h) {
        long mantissa = (h >>> 11) & ((1L << 53) - 1);
        return mantissa / (double) (1L << 53);
    }

    private static double hash3(int x, int y, int z, long seed, int salt) {
        long h = seed;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) y * 0xC2B2AE3D27D4EB4FL;
        h ^= (long) z * 0x165667B19E3779F9L;
        h ^= (long) salt * 0x94D049BB133111EBL;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return toUnitDouble(h);
    }

    private static double fbmPerlin2(double x, double z, long seed, int salt, int octaves) {
        double value = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double max = 0.0;

        for (int i = 0; i < octaves; i++) {
            value += perlin2(x * frequency, z * frequency, seed, salt + (i * 17)) * amplitude;
            max += amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }

        return max <= 0.0 ? 0.5 : (value / max);
    }

    private static double fbmPerlin3(double x, double y, double z, long seed, int salt, int octaves) {
        double value = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double max = 0.0;

        for (int i = 0; i < octaves; i++) {
            value += perlin3(x * frequency, y * frequency, z * frequency, seed, salt + (i * 31)) * amplitude;
            max += amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }

        return max <= 0.0 ? 0.5 : (value / max);
    }

    private static double perlin2(double x, double z, long seed, int salt) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double tx = x - x0;
        double tz = z - z0;
        double u = fade(tx);
        double v = fade(tz);

        double n00 = grad2(hashLattice(x0, 0, z0, seed, salt), tx, tz);
        double n10 = grad2(hashLattice(x1, 0, z0, seed, salt), tx - 1.0, tz);
        double n01 = grad2(hashLattice(x0, 0, z1, seed, salt), tx, tz - 1.0);
        double n11 = grad2(hashLattice(x1, 0, z1, seed, salt), tx - 1.0, tz - 1.0);

        double ix0 = lerpD(n00, n10, u);
        double ix1 = lerpD(n01, n11, u);
        return (lerpD(ix0, ix1, v) * 0.5) + 0.5;
    }

    private static double perlin3(double x, double y, double z, long seed, int salt) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int z1 = z0 + 1;

        double tx = x - x0;
        double ty = y - y0;
        double tz = z - z0;
        double u = fade(tx);
        double v = fade(ty);
        double w = fade(tz);

        double n000 = grad3(hashLattice(x0, y0, z0, seed, salt), tx, ty, tz);
        double n100 = grad3(hashLattice(x1, y0, z0, seed, salt), tx - 1.0, ty, tz);
        double n010 = grad3(hashLattice(x0, y1, z0, seed, salt), tx, ty - 1.0, tz);
        double n110 = grad3(hashLattice(x1, y1, z0, seed, salt), tx - 1.0, ty - 1.0, tz);
        double n001 = grad3(hashLattice(x0, y0, z1, seed, salt), tx, ty, tz - 1.0);
        double n101 = grad3(hashLattice(x1, y0, z1, seed, salt), tx - 1.0, ty, tz - 1.0);
        double n011 = grad3(hashLattice(x0, y1, z1, seed, salt), tx, ty - 1.0, tz - 1.0);
        double n111 = grad3(hashLattice(x1, y1, z1, seed, salt), tx - 1.0, ty - 1.0, tz - 1.0);

        double nx00 = lerpD(n000, n100, u);
        double nx10 = lerpD(n010, n110, u);
        double nx01 = lerpD(n001, n101, u);
        double nx11 = lerpD(n011, n111, u);
        double nxy0 = lerpD(nx00, nx10, v);
        double nxy1 = lerpD(nx01, nx11, v);
        return (lerpD(nxy0, nxy1, w) * 0.5) + 0.5;
    }

    private static long hashLattice(int x, int y, int z, long seed, int salt) {
        long h = seed;
        h ^= (long) x * 0x9E3779B97F4A7C15L;
        h ^= (long) y * 0xC2B2AE3D27D4EB4FL;
        h ^= (long) z * 0x165667B19E3779F9L;
        h ^= (long) salt * 0x94D049BB133111EBL;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return h;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double grad2(long hash, double x, double z) {
        switch ((int) (hash & 7L)) {
            case 0: return x + z;
            case 1: return -x + z;
            case 2: return x - z;
            case 3: return -x - z;
            case 4: return x;
            case 5: return -x;
            case 6: return z;
            default: return -z;
        }
    }

    private static double grad3(long hash, double x, double y, double z) {
        switch ((int) (hash & 15L)) {
            case 0: return x + y;
            case 1: return -x + y;
            case 2: return x - y;
            case 3: return -x - y;
            case 4: return x + z;
            case 5: return -x + z;
            case 6: return x - z;
            case 7: return -x - z;
            case 8: return y + z;
            case 9: return -y + z;
            case 10: return y - z;
            case 11: return -y - z;
            case 12: return x + y;
            case 13: return -x + y;
            case 14: return y - z;
            default: return -y - z;
        }
    }

    private static int fastFloor(double v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }

    private static double lerpD(double a, double b, double t) {
        return a + ((b - a) * t);
    }

}






