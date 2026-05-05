package dev.gabvoid.voideddimension.network;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.world.ModDimensions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModNetworking {
    private static final int RTP_RANGE = 60000;

    private ModNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(RtpVoidedPayload.ID, RtpVoidedPayload.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(RtpVoidedPayload.ID, (payload, context) -> {
            var player = context.player();
            var server = player.getServer();
            if (server == null) {
                return;
            }

            server.execute(() -> {
                ServerWorld targetWorld = server.getWorld(ModDimensions.VOIDED_DIMENSION_KEY);
                if (targetWorld == null) {
                    player.sendMessage(Text.literal("[voideddimension] RTP failed: target dimension not found."), false);
                    return;
                }

                int x = player.getRandom().nextBetween(-RTP_RANGE, RTP_RANGE);
                int z = player.getRandom().nextBetween(-RTP_RANGE, RTP_RANGE);
                double y = 300.0;

                player.fallDistance = 0;
                player.setVelocity(0.0, 0.0, 0.0);
                player.teleport(targetWorld, x + 0.5, y, z + 0.5, player.getYaw(), player.getPitch());
                player.sendMessage(Text.literal("[voideddimension] RTP -> x=" + x + " y=300 z=" + z), false);
            });
        });
    }

    public record RtpVoidedPayload(int nonce) implements CustomPayload {
        public static final CustomPayload.Id<RtpVoidedPayload> ID =
                new CustomPayload.Id<>(Identifier.of(VoidedDimension.MOD_ID, "rtp_voided"));

        public static final PacketCodec<RegistryByteBuf, RtpVoidedPayload> CODEC =
                PacketCodec.tuple(PacketCodecs.INTEGER, RtpVoidedPayload::nonce, RtpVoidedPayload::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
