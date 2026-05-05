package dev.gabvoid.voideddimension.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FeatureDebugCommand {
    private static final List<String> FEATURE_TYPES = List.of(
            "abyssal_fuste_feature",
            "dry_cracks",
            "fragmented_edges",
            "giant_void",
            "void_shafts",
            "gap_pillars",
            "bedrock_fragment_hole",
            "rose_petals"
    );

    private static final SuggestionProvider<ServerCommandSource> FEATURE_SUGGESTIONS =
            (context, builder) -> {
                for (String type : FEATURE_TYPES) {
                    builder.suggest(type);
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        var root = literal("feature")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("list")
                        .executes(ctx -> listFeatures(ctx.getSource())))
                .then(literal("create")
                        .then(argument("type", StringArgumentType.word())
                                .suggests(FEATURE_SUGGESTIONS)
                                .executes(ctx -> {
                                    ServerCommandSource source = ctx.getSource();
                                    BlockPos pos = BlockPos.ofFloored(source.getPosition());
                                    String type = StringArgumentType.getString(ctx, "type");
                                    return placeFeature(source, type, pos.getX(), pos.getY(), pos.getZ());
                                })
                                .then(argument("pos", BlockPosArgumentType.blockPos())
                                        .executes(ctx -> {
                                            BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                                            return placeFeature(
                                                    ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "type"),
                                                    pos.getX(),
                                                    pos.getY(),
                                                    pos.getZ()
                                            );
                                        }))
                                .then(argument("x", IntegerArgumentType.integer())
                                        .then(argument("y", IntegerArgumentType.integer())
                                                .then(argument("z", IntegerArgumentType.integer())
                                                        .executes(ctx -> placeFeature(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "type"),
                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                IntegerArgumentType.getInteger(ctx, "y"),
                                                                IntegerArgumentType.getInteger(ctx, "z")
                                                        )))))))
                .then(literal("scatter")
                        .then(argument("type", StringArgumentType.word())
                                .suggests(FEATURE_SUGGESTIONS)
                                .then(argument("count", IntegerArgumentType.integer(1, 128))
                                        .then(argument("radius", IntegerArgumentType.integer(1, 1024))
                                                .executes(ctx -> scatterFeatures(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "type"),
                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                        IntegerArgumentType.getInteger(ctx, "radius")
                                                ))))))
                .then(literal("range")
                        .then(argument("type", StringArgumentType.word())
                                .suggests(FEATURE_SUGGESTIONS)
                                .then(argument("count", IntegerArgumentType.integer(1, 128))
                                        .then(argument("radius", IntegerArgumentType.integer(1, 1024))
                                                .then(argument("minY", IntegerArgumentType.integer(-64, 1024))
                                                        .then(argument("maxY", IntegerArgumentType.integer(-64, 1024))
                                                                .executes(ctx -> scatterFeaturesWithYRange(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "type"),
                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                        IntegerArgumentType.getInteger(ctx, "radius"),
                                                                        IntegerArgumentType.getInteger(ctx, "minY"),
                                                                        IntegerArgumentType.getInteger(ctx, "maxY")
                                                                ))))))));

        dispatcher.register(root);
    }

    private static int listFeatures(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("[feature] types: " + String.join(", ", FEATURE_TYPES)), false);
        source.sendFeedback(() -> Text.literal("Use: /feature create <type> [x y z]"), false);
        source.sendFeedback(() -> Text.literal("Use: /feature scatter <type> <count> <radius>"), false);
        source.sendFeedback(() -> Text.literal("Use: /feature range <type> <count> <radius> <minY> <maxY>"), false);
        return 1;
    }

    private static int placeFeature(ServerCommandSource source, String rawType, int x, int y, int z) {
        String type = normalizeType(rawType);
        if (!FEATURE_TYPES.contains(type)) {
            source.sendError(Text.literal("Unknown feature type: " + rawType));
            return 0;
        }

        ServerWorld world = source.getWorld();
        BlockPos target = remapToFeatureAnchorIfNeeded(world, type, new BlockPos(x, y, z));
        if (!world.isChunkLoaded(target)) {
            source.sendError(Text.literal("[feature] target chunk is not loaded at " + target.getX() + " " + target.getY() + " " + target.getZ()));
            return 0;
        }

        int result;
        if ("giant_void".equals(type)) {
            result = placeGiantVoidCluster(source, target);
        } else {
            String cmd = "place feature voideddimension:" + type + " " + target.getX() + " " + target.getY() + " " + target.getZ();
            result = runAsCommand(source, cmd, false);
        }
        if (result > 0) {
            source.sendFeedback(() -> Text.literal("[feature] placed " + type + " near " + target.getX() + " " + target.getY() + " " + target.getZ() + " -> " + result), true);
        } else {
            source.sendError(Text.literal("[feature] no se pudo colocar " + type + " en " + target.getX() + " " + target.getY() + " " + target.getZ()));
        }
        return result;
    }

    private static int scatterFeatures(ServerCommandSource source, String rawType, int count, int radius) {
        BlockPos center = BlockPos.ofFloored(source.getPosition());
        return scatter(source, rawType, count, radius, center.getY(), center.getY());
    }

    private static int scatterFeaturesWithYRange(ServerCommandSource source, String rawType, int count, int radius, int minY, int maxY) {
        int low = Math.min(minY, maxY);
        int high = Math.max(minY, maxY);
        return scatter(source, rawType, count, radius, low, high);
    }

    private static int scatter(ServerCommandSource source, String rawType, int count, int radius, int minY, int maxY) {
        String type = normalizeType(rawType);
        if (!FEATURE_TYPES.contains(type)) {
            source.sendError(Text.literal("Unknown feature type: " + rawType));
            return 0;
        }

        ServerWorld world = source.getWorld();
        BlockPos center = BlockPos.ofFloored(source.getPosition());

        int success = 0;
        int unloaded = 0;
        int failed = 0;
        for (int i = 0; i < count; i++) {
            BlockPos target = null;
            for (int attempt = 0; attempt < 10; attempt++) {
                int x = center.getX() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
                int z = center.getZ() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
                int y = ThreadLocalRandom.current().nextInt(minY, maxY + 1);
                y = Math.max(world.getBottomY() + 1, Math.min(world.getTopY() - 1, y));

                BlockPos candidate = remapToFeatureAnchorIfNeeded(world, type, new BlockPos(x, y, z));
                if (world.isChunkLoaded(candidate)) {
                    target = candidate;
                    break;
                }
            }
            if (target == null) {
                unloaded++;
                continue;
            }

            int result;
            if ("giant_void".equals(type)) {
                result = placeGiantVoidCluster(source, target);
            } else {
                String cmd = "place feature voideddimension:" + type + " " + target.getX() + " " + target.getY() + " " + target.getZ();
                result = runAsCommand(source, cmd, false);
            }
            if (result > 0) {
                success++;
            } else {
                failed++;
            }
        }

        int finalSuccess = success;
        int finalUnloaded = unloaded;
        int finalFailed = failed;
        source.sendFeedback(() -> Text.literal("[feature] placed " + finalSuccess + "/" + count
                + " of " + type + " (radius=" + radius + ", y=" + minY + ".." + maxY + ", unloaded=" + finalUnloaded + ", failed=" + finalFailed + ")"), true);
        return success;
    }

    private static String normalizeType(String raw) {
        if (raw.startsWith("voideddimension:")) {
            return raw.substring("voideddimension:".length());
        }
        return raw;
    }

    private static int runAsCommand(ServerCommandSource source, String command, boolean notifyOnFailure) {
        try {
            return source.getServer().getCommandManager().getDispatcher().execute(command, source.withSilent());
        } catch (CommandSyntaxException e) {
            if (notifyOnFailure) {
                source.sendError(Text.literal("Command failed: " + e.getMessage()));
            }
            return 0;
        }
    }

    private static BlockPos remapToFeatureAnchorIfNeeded(ServerWorld world, String type, BlockPos desired) {
        if ("giant_void".equals(type)) {
            return findNearestAnchor(world, desired, 12, 0.85, 7, 11, 13, 16);
        }
        if ("void_shafts".equals(type)) {
            return findNearestAnchor(world, desired, 3, 0.35, 11, 23, 37, 12);
        }
        return desired;
    }

    private static int placeGiantVoidCluster(ServerCommandSource source, BlockPos center) {
        ServerWorld world = source.getWorld();
        int placed = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x = center.getX() + (dx * 16);
                int z = center.getZ() + (dz * 16);
                BlockPos p = new BlockPos(x, center.getY(), z);
                if (!world.isChunkLoaded(p)) {
                    continue;
                }
                String cmd = "place feature voideddimension:giant_void " + x + " " + center.getY() + " " + z;
                if (runAsCommand(source, cmd, false) > 0) {
                    placed++;
                }
            }
        }
        return placed;
    }

    private static BlockPos findNearestAnchor(ServerWorld world, BlockPos desired, int regionSizeChunks, double spawnChance,
                                              int rollSalt, int anchorXSalt, int anchorZSalt, int searchRadiusRegions) {
        int desiredChunkX = desired.getX() >> 4;
        int desiredChunkZ = desired.getZ() >> 4;
        int startRegionX = Math.floorDiv(desiredChunkX, regionSizeChunks);
        int startRegionZ = Math.floorDiv(desiredChunkZ, regionSizeChunks);
        long seed = world.getSeed();

        BlockPos best = desired;
        long bestDistSq = Long.MAX_VALUE;

        for (int dz = -searchRadiusRegions; dz <= searchRadiusRegions; dz++) {
            for (int dx = -searchRadiusRegions; dx <= searchRadiusRegions; dx++) {
                int regionX = startRegionX + dx;
                int regionZ = startRegionZ + dz;

                long regionHash = hashRegion(regionX, regionZ, seed, rollSalt);
                if (toUnitDouble(regionHash) >= spawnChance) {
                    continue;
                }

                int anchorLocalX = positiveMod((int) hashRegion(regionX, regionZ, seed, anchorXSalt), regionSizeChunks);
                int anchorLocalZ = positiveMod((int) hashRegion(regionX, regionZ, seed, anchorZSalt), regionSizeChunks);
                int chunkX = (regionX * regionSizeChunks) + anchorLocalX;
                int chunkZ = (regionZ * regionSizeChunks) + anchorLocalZ;
                int x = (chunkX << 4) + 8;
                int z = (chunkZ << 4) + 8;
                BlockPos candidate = new BlockPos(x, desired.getY(), z);

                long ddx = candidate.getX() - desired.getX();
                long ddz = candidate.getZ() - desired.getZ();
                long distSq = (ddx * ddx) + (ddz * ddz);
                if (!world.isChunkLoaded(candidate)) {
                    continue;
                }

                if (distSq < bestDistSq) {
                    best = candidate;
                    bestDistSq = distSq;
                }
            }
        }

        return best;
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
}



