package dev.gabvoid.voideddimension;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.client.render.RenderLayer;
import dev.gabvoid.voideddimension.entity.ModEntities;
import dev.gabvoid.voideddimension.client.render.PuppetmanRenderer;
import dev.gabvoid.voideddimension.client.render.FragileBedrockOverlayRenderer;
import dev.gabvoid.voideddimension.client.input.ModKeyBindings;
import dev.gabvoid.voideddimension.blocks.ModBlocks;
import dev.gabvoid.voideddimension.client.render.VoidedPanoramaSkyRenderer;
import dev.gabvoid.voideddimension.world.ModDimensions;

public class VoidedDimensionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Código de inicialización del cliente
        System.out.println("VoidedDimensionClient inicializado correctamente.");

        EntityRendererRegistry.register(ModEntities.PUPPETMAN, PuppetmanRenderer::new);
        FragileBedrockOverlayRenderer.register();
        ModKeyBindings.register();
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ROSE_PETALS, RenderLayer.getCutout());
        DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.VOIDED_DIMENSION_KEY, new VoidedPanoramaSkyRenderer());
        // Cloud renderer no-op: evita que se dibujen nubes en la dimensión.
        DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.VOIDED_DIMENSION_KEY, context -> {
        });
    }
}
