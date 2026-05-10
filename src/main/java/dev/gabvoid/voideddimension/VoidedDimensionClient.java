package dev.gabvoid.voideddimension;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.client.render.RenderLayer;
import dev.gabvoid.voideddimension.entity.ModEntities;
import dev.gabvoid.voideddimension.client.render.PuppetmanRenderer;
import dev.gabvoid.voideddimension.client.render.WanderingFragmentRenderer;
import dev.gabvoid.voideddimension.client.render.FragmentSummonerRenderer;
import dev.gabvoid.voideddimension.client.render.ErraticRenderer;
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
        EntityRendererRegistry.register(ModEntities.WANDERING_FRAGMENT, WanderingFragmentRenderer::new);
        EntityRendererRegistry.register(ModEntities.FRAGMENT_SUMMONER, FragmentSummonerRenderer::new);
        EntityRendererRegistry.register(ModEntities.ERRATIC, ErraticRenderer::new);
        FragileBedrockOverlayRenderer.register();
        ModKeyBindings.register();
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ROSE_PETALS, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BONY_RACIM, RenderLayer.getCutout());
        // Bloques non-opaque para espectador
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FUSTE_CARCASA, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FRACTURED_STONE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FRACTURED_COBBLESTONE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.STRESS_CRACK, RenderLayer.getCutout());
        DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.VOIDED_DIMENSION_KEY, new VoidedPanoramaSkyRenderer());
        // Cloud renderer no-op: evita que se dibujen nubes en la dimensión.
        DimensionRenderingRegistry.registerCloudRenderer(ModDimensions.VOIDED_DIMENSION_KEY, context -> {
        });
    }
}
