package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class PillarFeature extends Feature<DefaultFeatureConfig> {
    private static final int ROOF_UNDERSIDE_Y = 449;
    private static final int MIN_SURFACE_Y = 200;
    // Base spacing target around ~200 blocks with jitter; effective separation ~192-208
    private static final int GRID_CHUNKS_BASE = 12; // 12*16=192; sometimes 13*16=208

    public PillarFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        // Enforce sparse distribution with jittered grid (~200 blocks separation average)
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        int h = hash2i(chunkX, chunkZ);
        int cell = GRID_CHUNKS_BASE + (h & 1); // 12 or 13
        int offX = Math.floorMod((h >>> 1), cell);
        int offZ = Math.floorMod((h >>> 9), cell);
        if (Math.floorMod(chunkX - offX, cell) != 0 || Math.floorMod(chunkZ - offZ, cell) != 0) {
            return false;
        }

        // Start from the roof underside and scan downward until first solid surface (non-air except bedrock)
        int startY = ROOF_UNDERSIDE_Y;
        int surfaceY = -1;
        for (int y = startY; y >= MIN_SURFACE_Y; y--) {
            BlockState state = world.getBlockState(origin.withY(y));
            if (!state.isAir() && state.getBlock() != Blocks.BEDROCK) {
                surfaceY = y;
                break;
            }
        }
        if (surfaceY < 0) return false;

        BlockState material = Blocks.STONE.getDefaultState();

    // Per-pillar radius variation (diameter 9..18 => radius 4.5..9.0)
    float radiusMin = 4.5f;
    float radiusMax = 9.0f;
    float radius = radiusMin + random.nextFloat() * (radiusMax - radiusMin);
        float rSq = radius * radius;

        // Precompute ground contact per radial column (so every column can touch its own ground)
    int r = (int) Math.ceil(radius) + 2;
        int size = r * 2 + 1;
        int[][] groundY = new int[size][size]; // store highest solid y for each column inside cylinder
        int maxGround = MIN_SURFACE_Y; // track max ground among columns
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                float distSq = dx * dx + dz * dz;
                int ix = dx + r, iz = dz + r;
                groundY[ix][iz] = MIN_SURFACE_Y - 1; // default none found
                if (distSq > rSq) continue;
                // scan downward for this column
                for (int y = startY; y >= MIN_SURFACE_Y; y--) {
                    BlockState s = world.getBlockState(new BlockPos(origin.getX() + dx, y, origin.getZ() + dz));
                    if (!s.isAir() && s.getBlock() != Blocks.BEDROCK) {
                        groundY[ix][iz] = y; // first solid
                        if (y > maxGround) maxGround = y;
                        break;
                    }
                }
            }
        }

        // Always full pillars: from roof underside down to the ground per column
        int globalMinY = MIN_SURFACE_Y;
        int globalMaxY = startY;

        // Crack profile distribution: 60% diagonal-only (more linear/continuous), 40% rings + optional diagonals
        boolean diagonalOnly = random.nextFloat() < 0.60f;
        int ringPeriod = diagonalOnly ? 0 : (6 + random.nextInt(5)); // 6..10, or disabled
        int ringThickness = diagonalOnly ? 0 : (1 + random.nextInt(2)); // 1..2, or disabled
        int diagonalCount = diagonalOnly ? (2 + random.nextInt(2)) : (random.nextInt(2)); // diag-only: 2..3; else 0..1
        float[] diagAngles = new float[diagonalCount];
        int[] diagStride = new int[diagonalCount];
        for (int i = 0; i < diagonalCount; i++) {
            diagAngles[i] = (float) (random.nextFloat() * Math.PI * 2f); // direction of diagonal
            // Diagonal-only pillars: slightly tighter spacing for visible linear cracks; else a bit wider
            diagStride[i] = (diagonalOnly ? (6 + random.nextInt(5)) : (8 + random.nextInt(6))); // 6..10 or 8..13
        }

        // Stepped banding on radius: slight inset/outset per band to break smoothness subtly
        int stepEvery = 5 + random.nextInt(4); // 5..8
        float stepDepth = 0.6f; // one block-ish influence on the rim

        // Build the pillar
        for (int y = globalMaxY; y >= globalMinY; y--) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    float distSq = dx * dx + dz * dz;
                    // Apply stepped banding to local radius threshold
                    int bandIndex = Math.floorDiv(y, stepEvery);
                    float localRadius = radius - ((bandIndex & 1) == 0 ? 0f : stepDepth);
                    float localRSq = localRadius * localRadius;
                    if (distSq > localRSq) continue; // banded cylinder boundary

                    int ix = dx + r, iz = dz + r;
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;

                    // Full pillars: per-column stop at ground

                    // Cracks: horizontal ring segments and some diagonals
                    boolean inCrack = false;
                    // Horizontal rings: every ringPeriod with given thickness, carve outer rim segments (if enabled)
                    if (ringPeriod > 0 && ringThickness > 0) {
                        int yMod = Math.floorMod(y, ringPeriod);
                        if (yMod < ringThickness) {
                            float dist = (float) Math.sqrt(distSq);
                            float rim = localRadius - dist;
                            if (rim < 1.2f) {
                                // break ring into segments using hash so it’s not continuous
                                int segHash = hash2i(origin.getX() + dx, origin.getZ() + dz) ^ (y * 0x9E3779B1);
                                if ((segHash & 7) == 0) inCrack = true; // ~1/8 of rim cells become crack
                            }
                        }
                    }
                    // Diagonals: carve along slanted directions based on projected coordinate
                    if (!inCrack && diagonalCount > 0) {
                        for (int i = 0; i < diagonalCount; i++) {
                            float ang = diagAngles[i];
                            float px = (float) (dx * Math.cos(ang) + dz * Math.sin(ang));
                            int stripe = Math.floorMod(Math.round(px) + y, diagStride[i]);
                            if (stripe == 0) {
                                float dist = (float) Math.sqrt(distSq);
                                float rim = localRadius - dist;
                                if (rim < 1.2f) {
                                    if (diagonalOnly) {
                                        // Make diagonals continuous and read as lineal cracks
                                        inCrack = true; break;
                                    } else {
                                        // Keep some randomness when rings are present to avoid over-noise
                                        int dHash = (hash2i(origin.getX() + dx, origin.getZ() + dz) ^ (y * 0x85EBCA77)) & 3;
                                        if (dHash == 0) { inCrack = true; break; } // ~1/4 density along stripe
                                    }
                                }
                            }
                        }
                    }
                    // prevent cracks within the last 2 layers above ground to force full contact
                    int gy = groundY[ix][iz];
                    if (y <= gy + 2) inCrack = false;

                    // Rim micro-chipping (keep very subtle)
                    float dist = (float) Math.sqrt(distSq);
                    float rim = localRadius - dist;
                    boolean skipRim = rim < 0.6f && random.nextFloat() < (0.6f - rim) * 0.08f; // very subtle
                    if (y <= gy + 2) skipRim = false; // never chip at contact

                    // Decide if we should place a block at (x,y,z)
                    boolean place;
                    // Fill down until touching per-column ground; include the contact block above ground
                    int columnStopY = groundY[ix][iz] + 1;
                    place = y >= columnStopY;

                    if (place && !inCrack && !skipRim) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState cur = world.getBlockState(pos);
                        if (cur.isAir()) {
                            world.setBlockState(pos, material, Block.NOTIFY_ALL);
                        }
                    }
                }
            }
        }
        return true;
    }

    private static int hash2i(int x, int z) {
        int h = x * 0x9E3779B1 ^ z * 0x85EBCA77;
        h ^= (h >>> 16);
        h *= 0x27D4EB2D;
        h ^= (h >>> 15);
        return h;
    }
}
