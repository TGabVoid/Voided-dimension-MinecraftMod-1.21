package dev.gabvoid.voideddimension.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;

public final class VoidedPanoramaSkyRenderer implements DimensionRenderingRegistry.SkyRenderer {
    private static final Identifier PANORAMA = Identifier.of("voideddimension", "textures/sky/inactiveeffect1.png");
    private static final int LAT_STEPS = 32;
    private static final int LON_STEPS = 64;
    private static final float RADIUS = 16.0f;
    private static boolean loggedOnce = false;

    @Override
    public void render(WorldRenderContext context) {
        if (context.world() == null || context.camera() == null || context.matrixStack() == null) {
            return;
        }

        if (!loggedOnce) {
            System.out.println("[VoidedDimension] Custom sky renderer active with texture: " + PANORAMA);
            loggedOnce = true;
        }

        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.multiply(context.camera().getRotation());

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, PANORAMA);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();

        for (int i = 0; i < LAT_STEPS; i++) {
            float v0 = 1.0f - (float) i / LAT_STEPS;
            float v1 = 1.0f - (float) (i + 1) / LAT_STEPS;
            float lat0 = (float) (Math.PI * (-0.5 + (double) i / LAT_STEPS));
            float lat1 = (float) (Math.PI * (-0.5 + (double) (i + 1) / LAT_STEPS));
            float y0 = (float) Math.sin(lat0);
            float y1 = (float) Math.sin(lat1);
            float r0 = (float) Math.cos(lat0);
            float r1 = (float) Math.cos(lat1);

            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE);
            for (int j = 0; j <= LON_STEPS; j++) {
                float u = 1.0f - (float) j / LON_STEPS;
                float lon = (float) (Math.PI * 2.0 * ((double) j / LON_STEPS));
                float x0 = (float) Math.cos(lon) * r0;
                float z0 = (float) Math.sin(lon) * r0;
                float x1 = (float) Math.cos(lon) * r1;
                float z1 = (float) Math.sin(lon) * r1;

                buffer.vertex(matrix, x0 * RADIUS, y0 * RADIUS, z0 * RADIUS).texture(u, v0);
                buffer.vertex(matrix, x1 * RADIUS, y1 * RADIUS, z1 * RADIUS).texture(u, v1);
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        matrices.pop();
    }
}
