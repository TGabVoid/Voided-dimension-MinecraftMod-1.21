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

public class GiantVoidFeature extends Feature<DefaultFeatureConfig> {
    private static final int REGION_SIZE_CHUNKS = 12;
    private static final double REGION_SPAWN_CHANCE = 0.85;
    private static final int CEILING_Y = 300;

    public GiantVoidFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        if (!world.toServerWorld().getRegistryKey().equals(ModDimensions.VOIDED_DIMENSION_KEY)) {
            return false;
        }

        BlockPos origin = context.getOrigin();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        long seed = world.getSeed();

        int regionX = Math.floorDiv(chunkX, REGION_SIZE_CHUNKS);
        int regionZ = Math.floorDiv(chunkZ, REGION_SIZE_CHUNKS);

        boolean changed = false;
        // Evalua también regiones vecinas para evitar cortes en bordes de region.
        for (int rz = -1; rz <= 1; rz++) {
            for (int rx = -1; rx <= 1; rx++) {
                changed |= carveRegionVoidInChunk(world, chunkX, chunkZ, seed, regionX + rx, regionZ + rz);
            }
        }

        return changed;
    }

    private boolean carveRegionVoidInChunk(StructureWorldAccess world, int chunkX, int chunkZ, long seed, int regionX, int regionZ) {
        if (toUnit(hash(regionX, regionZ, seed, 7)) >= REGION_SPAWN_CHANCE) {
            return false;
        }

        int centerChunkX = (regionX * REGION_SIZE_CHUNKS) + positiveMod((int) hash(regionX, regionZ, seed, 11), REGION_SIZE_CHUNKS);
        int centerChunkZ = (regionZ * REGION_SIZE_CHUNKS) + positiveMod((int) hash(regionX, regionZ, seed, 13), REGION_SIZE_CHUNKS);
        int centerX = (centerChunkX << 4) + 8;
        int centerZ = (centerChunkZ << 4) + 8;

        int radiusX = 42 + positiveMod((int) hash(regionX, regionZ, seed, 17), 22);
        int radiusZ = 42 + positiveMod((int) hash(regionX, regionZ, seed, 19), 22);
        int radiusY = 34 + positiveMod((int) hash(regionX, regionZ, seed, 21), 30);
        int topY = CEILING_Y + positiveMod((int) hash(regionX, regionZ, seed, 23), 11);
        int bottomY = Math.max(8, topY - (radiusY * 2) - positiveMod((int) hash(regionX, regionZ, seed, 29), 46));
        int shapeType = positiveMod((int) hash(regionX, regionZ, seed, 31), 3);

        int minX = (chunkX << 4);
        int maxX = minX + 15;
        int minZ = (chunkZ << 4);
        int maxZ = minZ + 15;

        // Culling rapido: si chunk esta demasiado lejos del centro, no iterar.
        double maxReachX = radiusX * 1.9;
        double maxReachZ = radiusZ * 1.9;
        if ((maxX < centerX - maxReachX) || (minX > centerX + maxReachX) || (maxZ < centerZ - maxReachZ) || (minZ > centerZ + maxReachZ)) {
            return false;
        }

        boolean changed = false;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                double nx = (x - centerX) / (double) radiusX;
                double nz = (z - centerZ) / (double) radiusZ;
                double shell2d = (nx * nx) + (nz * nz);
                if (shell2d > 2.80) {
                    continue;
                }

                int yMin = Math.max(1, bottomY);
                int yMax = Math.min(511, topY);
                int centerY = (topY + bottomY) / 2;
                double halfSpanY = Math.max(10.0, (topY - bottomY) * 0.5);
                for (int y = yMin; y <= yMax; y++) {
                    double ny = (y - centerY) / halfSpanY;

                    double warpX = (fbmPerlin3(x * 0.011, y * 0.010, z * 0.011, seed, 89, 2) - 0.5) * 0.42;
                    double warpY = (fbmPerlin3(x * 0.010, y * 0.009, z * 0.010, seed, 97, 2) - 0.5) * 0.30;
                    double warpZ = (fbmPerlin3(x * 0.011, y * 0.010, z * 0.011, seed, 101, 2) - 0.5) * 0.42;

                    double wx = nx + warpX;
                    double wy = ny + warpY;
                    double wz = nz + warpZ;

                    double field;
                    if (shapeType == 0) {
                        field = (wx * wx) + (wy * wy) + (wz * wz);
                    } else if (shapeType == 1) {
                        field = Math.pow(Math.abs(wx), 2.45) + Math.pow(Math.abs(wy), 2.15) + Math.pow(Math.abs(wz), 2.45);
                    } else {
                        field = Math.pow(Math.abs(wx), 2.9) + Math.pow(Math.abs(wy), 2.35) + Math.pow(Math.abs(wz), 2.9);
                    }

                    double noiseLo = fbmPerlin3(x * 0.018, y * 0.016, z * 0.018, seed, 41, 3);
                    double noiseHi = fbmPerlin3(x * 0.054, y * 0.048, z * 0.054, seed, 47, 2);
                    double doublePerlin = (noiseLo * 0.68) + (noiseHi * 0.32);

                    double erosion = (doublePerlin - 0.5) * 0.88;
                    double lateralFalloff = Math.pow(Math.max(0.0, shell2d - 0.72), 1.35);
                    double verticalFalloff = Math.pow(Math.max(0.0, Math.abs(ny) - 0.82), 1.60);
                    double threshold = 1.04 + erosion - (lateralFalloff * 0.18) - (verticalFalloff * 0.11);
                    double signed = field - threshold;
                    if (signed > 0.28) {
                        continue;
                    }

                    if (signed > -0.22) {
                        double tRaw = Math.max(0.0, Math.min(1.0, (signed + 0.22) / 0.50));
                        double t = tRaw * tRaw * (3.0 - (2.0 * tRaw));
                        double edgeNoiseA = fbmPerlin3(x * 0.070, y * 0.050, z * 0.070, seed, 71, 2);
                        double edgeNoiseB = fbmPerlin3(x * 0.028, y * 0.026, z * 0.028, seed, 73, 2);
                        double edgeNoiseC = fbmPerlin3(x * 0.012, y * 0.010, z * 0.012, seed, 79, 2);
                        double edgeNoise = (edgeNoiseA * 0.50) + (edgeNoiseB * 0.30) + (edgeNoiseC * 0.20);
                        if (edgeNoise > (1.0 - t)) {
                            continue;
                        }
                    }

                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.BEDROCK) || state.isAir()) {
                        continue;
                    }
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    changed = true;
                }
            }
        }

        return changed;
    }

    private static long hash(int x, int z, long seed, int salt) {
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
        return toUnit(h);
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

    private static int positiveMod(int value, int mod) {
        int m = value % mod;
        return m < 0 ? m + mod : m;
    }

    private static double toUnit(long h) {
        long mantissa = (h >>> 11) & ((1L << 53) - 1);
        return mantissa / (double) (1L << 53);
    }
}

