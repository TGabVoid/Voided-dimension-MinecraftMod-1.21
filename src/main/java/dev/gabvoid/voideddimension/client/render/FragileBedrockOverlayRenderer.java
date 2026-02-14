package dev.gabvoid.voideddimension.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class FragileBedrockOverlayRenderer {
    private static final int RANGE = 24;
    private static final int MIN_Y = -64;
    private static final int MAX_Y = -55;

    private FragileBedrockOverlayRenderer() {
    }

    public static void register() {
        WorldRenderEvents.LAST.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world == null || client.player == null) return;

            World world = client.world;
            BlockPos center = client.player.getBlockPos();

            int minX = center.getX() - RANGE;
            int maxX = center.getX() + RANGE;
            int minZ = center.getZ() - RANGE;
            int maxZ = center.getZ() + RANGE;

            MatrixStack matrices = context.matrixStack();
            Vec3d cameraPos = context.camera().getPos();

            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
            VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

            for (int y = MIN_Y; y <= MAX_Y; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!world.getBlockState(pos).isOf(ModBlocks.FRAGILE_BEDROCK)) continue;
                        Box box = new Box(pos).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z).expand(0.002);
                        WorldRenderer.drawBox(matrices, lines, box, 0.35f, 0.9f, 1.0f, 0.75f);
                    }
                }
            }

            consumers.draw();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
        });
    }
}

