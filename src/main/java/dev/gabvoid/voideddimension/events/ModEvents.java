package dev.gabvoid.voideddimension.events;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.items.ModItems;
import dev.gabvoid.voideddimension.items.custom.AgonizingGlowItem;
import dev.gabvoid.voideddimension.world.ModDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.chunk.WorldChunk;

public class ModEvents
{
    // Se resetea en cada ejecución del cliente; sirve para test rápido.
    private static boolean didInitialBedrockHoleTeleport = false;
    private static boolean fragilePatchEnabled = false;
    private static boolean didAutoOp = false;
    private static final int FRAGILE_BEDROCK_BOTTOM_Y = -64;
    private static final int FRAGILE_BEDROCK_TOP_Y = -55;
    private static final int FRAGILE_PATCH_PER_TICK = 6;
    private static final Set<UUID> VOID_PASS_INVULN = new HashSet<>();
    private static final Set<UUID> VOID_PASS_PENDING_BREAK = new HashSet<>();
    private static final Set<Long> FRAGILE_QUEUED = new HashSet<>();
    private static final Deque<ChunkPos> FRAGILE_QUEUE = new ArrayDeque<>();

    private static void drainFragileBedrockQueue(ServerWorld world) {
        for (int i = 0; i < FRAGILE_PATCH_PER_TICK; i++) {
            ChunkPos chunkPos = FRAGILE_QUEUE.poll();
            if (chunkPos == null) return;
            FRAGILE_QUEUED.remove(chunkPos.toLong());
            applyFragileBedrockPatch(world, chunkPos);
        }
    }

    public static void registerEvents() {

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            VoidedDimension.LOGGER.info("[voideddimension] Server starting. Worlds will log on load.");
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            VoidedDimension.LOGGER.info("[voideddimension] World loaded: key={}, seed={}, minY={}, height={}, topY={}",
                    world.getRegistryKey().getValue(),
                    world.getSeed(),
                    world.getBottomY(),
                    world.getHeight(),
                    world.getTopY());
        });

        ServerTickEvents.END_SERVER_TICK.register(server ->
        {
            if (!fragilePatchEnabled && !server.getPlayerManager().getPlayerList().isEmpty()) {
                fragilePatchEnabled = true;
            }
            if (!didAutoOp && server.isSingleplayer()) {
                ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayerList().isEmpty()
                        ? null
                        : server.getPlayerManager().getPlayerList().get(0);
                if (serverPlayer != null) {
                    server.getPlayerManager().addToOperators(serverPlayer.getGameProfile());
                    didAutoOp = true;
                }
            }

            for (PlayerEntity player : server.getPlayerManager().getPlayerList())
            {
                if (player.getMainHandStack().getItem() instanceof AgonizingGlowItem)
                {
                    ((AgonizingGlowItem) player.getMainHandStack().getItem()).tick(player.getWorld(), player);
                }

                handleVoidFallTeleport(server, player);
                handleInitialBedrockHoleTeleport(server, player);
                handleVoidPassLanding(server, player);
            }

            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            if (overworld != null && fragilePatchEnabled) {
                drainFragileBedrockQueue(overworld);
            }
        });

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world.getRegistryKey() != World.OVERWORLD) return;
            if (!fragilePatchEnabled) return;
            ChunkPos chunkPos = chunk.getPos();
            long key = chunkPos.toLong();
            if (FRAGILE_QUEUED.add(key)) {
                FRAGILE_QUEUE.add(chunkPos);
            }
        });
    }

    private static void handleInitialBedrockHoleTeleport(MinecraftServer server, PlayerEntity player) {
        if (didInitialBedrockHoleTeleport) return;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) return;
        if (serverPlayer.getWorld().getRegistryKey() != World.OVERWORLD) return;

        // Llevarlo al fondo cerca del spawn.
        BlockPos spawn = overworld.getSpawnPos();
        double x = spawn.getX() + 0.5;
        double z = spawn.getZ() + 0.5;
        // Ojo: si lo ponemos en -64 puede spawnear dentro de bedrock.
        // Para testear el hueco, lo ponemos un poco arriba.
        double y = -58.0;

        serverPlayer.teleport(overworld, x, y, z, serverPlayer.getYaw(), serverPlayer.getPitch());

        BlockPos testPos = new BlockPos((int) x, -63, (int) z);
        overworld.setBlockState(testPos, ModBlocks.FRAGILE_BEDROCK.getDefaultState(), 2);

        serverPlayer.sendMessage(Text.literal("[voideddimension] Test: te llevé cerca de la capa de bedrock (Y≈-64). Se genera bedrock fragil en esa capa; explora unos chunks alrededor."), false);
        VoidedDimension.LOGGER.info("[voideddimension] Test teleport to bedrock layer at x={}, y={}, z={} for player {}", x, y, z, serverPlayer.getName().getString());

        didInitialBedrockHoleTeleport = true;
    }

    private static void handleVoidPassLanding(MinecraftServer server, PlayerEntity player) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
        if (player.getWorld().getRegistryKey() != ModDimensions.VOIDED_DIMENSION_KEY) return;

        boolean hasPass = player.getInventory().containsAny(item -> item.isOf(ModItems.VOID_PASS));
        if (hasPass || VOID_PASS_PENDING_BREAK.contains(player.getUuid())) {
            // Evita daño por caída mientras tenga el pass o esté pendiente de romperse.
            serverPlayer.fallDistance = 0;
        }

        if (!serverPlayer.isOnGround()) return;
        if (!VOID_PASS_PENDING_BREAK.remove(player.getUuid())) return;

        // Romper y consumir el pass al aterrizar.
        player.getInventory().remove(itemStack -> itemStack.isOf(ModItems.VOID_PASS), 1, player.getInventory());
        serverPlayer.getWorld().playSound(null, serverPlayer.getBlockPos(), SoundEvents.ENTITY_ITEM_BREAK,
                SoundCategory.PLAYERS, 0.8f, 1.0f);
    }

    private static void handleVoidFallTeleport(MinecraftServer server, PlayerEntity player) {
        World world = player.getWorld();
        if (world.getRegistryKey() != World.OVERWORLD) return;

        boolean hasPass = player.getInventory().containsAny(item -> item.isOf(ModItems.VOID_PASS));
        boolean shouldProtect = hasPass && player.getY() < -64 && !player.isCreative() && !player.isSpectator();
        UUID playerId = player.getUuid();
        if (shouldProtect && !VOID_PASS_INVULN.contains(playerId)) {
            VOID_PASS_INVULN.add(playerId);
            player.setInvulnerable(true);
        } else if (!shouldProtect && VOID_PASS_INVULN.contains(playerId)) {
            VOID_PASS_INVULN.remove(playerId);
            player.setInvulnerable(false);
        }

        double y = player.getY();
        // If player is far below bedrock (e.g., -250), either die or transfer
        if (y > -450) return;

        if (!hasPass) {
            // Lethal fall into void without pass
            player.damage(world.getDamageSources().outOfWorld(), Float.MAX_VALUE);
            return;
        }

        // Marca para romper el pass al aterrizar en la dimensión.
        VOID_PASS_PENDING_BREAK.add(playerId);

        // Teleport to voided dimension at same X/Z, high Y (e.g., 400) to fall in
        ServerWorld targetWorld = server.getWorld(ModDimensions.VOIDED_DIMENSION_KEY);
        if (targetWorld == null) return;

        double targetX = player.getX();
        double targetZ = player.getZ();
        double targetY = 400.0;

        if (!(player instanceof ServerPlayerEntity serverPlayer)) return;

        // Clear fall distance and velocity to avoid immediate death
        serverPlayer.fallDistance = 0;
        serverPlayer.setVelocity(0, 0, 0);
        VOID_PASS_INVULN.remove(playerId);
        serverPlayer.setInvulnerable(false);
        serverPlayer.teleport(targetWorld, targetX, targetY, targetZ, serverPlayer.getYaw(), serverPlayer.getPitch());
    }

    private static void applyFragileBedrockPatch(ServerWorld world, ChunkPos chunkPos) {
        if (world.getRegistryKey() != World.OVERWORLD) return;

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        BlockPos.Mutable pos = new BlockPos.Mutable();
        int heightSpan = Math.max(1, FRAGILE_BEDROCK_TOP_Y - FRAGILE_BEDROCK_BOTTOM_Y);
        for (int y = FRAGILE_BEDROCK_BOTTOM_Y; y <= FRAGILE_BEDROCK_TOP_Y; y++) {
            double yFactor = (y - FRAGILE_BEDROCK_BOTTOM_Y) / (double) heightSpan;
            double threshold = 0.52 + (yFactor * 0.12);
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    pos.set(startX + dx, y, startZ + dz);
                    if (!world.getBlockState(pos).isOf(Blocks.BEDROCK)) continue;
                    double density = fragileDensity(world, pos.getX(), pos.getZ());
                    if (density >= threshold) {
                        world.setBlockState(pos, ModBlocks.FRAGILE_BEDROCK.getDefaultState(), 0);
                    }
                }
            }
        }
    }

    private static double fragileDensity(ServerWorld world, int x, int z) {
        long seed = world.getSeed() ^ 0x9E3779B97F4A7C15L;
        double base = fbm(seed, x, z, 0.025, 4);
        double detail = fbm(seed ^ 0xC6A4A7935BD1E995L, x, z, 0.09, 2);
        double combined = (base * 0.75) + (detail * 0.25);
        return (combined + 1.0) * 0.5;
    }

    private static double fbm(long seed, int x, int z, double baseFreq, int octaves) {
        double amp = 1.0;
        double freq = baseFreq;
        double sum = 0.0;
        double max = 0.0;
        for (int i = 0; i < octaves; i++) {
            sum += amp * smoothNoise(seed + (i * 0x632BE59BD9B4E019L), x * freq, z * freq);
            max += amp;
            amp *= 0.5;
            freq *= 2.0;
        }
        return sum / max;
    }

    private static double smoothNoise(long seed, double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double tx = x - x0;
        double tz = z - z0;
        double sx = tx * tx * (3.0 - 2.0 * tx);
        double sz = tz * tz * (3.0 - 2.0 * tz);

        double v00 = hashToUnit(seed, x0, z0);
        double v10 = hashToUnit(seed, x1, z0);
        double v01 = hashToUnit(seed, x0, z1);
        double v11 = hashToUnit(seed, x1, z1);

        double ix0 = lerp(v00, v10, sx);
        double ix1 = lerp(v01, v11, sx);
        return lerp(ix0, ix1, sz) * 2.0 - 1.0;
    }

    private static double hashToUnit(long seed, int x, int z) {
        long h = seed;
        h ^= (long) x * 341873128712L;
        h ^= (long) z * 132897987541L;
        h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
        h = (h ^ (h >>> 33)) * 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);
        return (h & 0xFFFFFF) / (double) 0xFFFFFF;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
