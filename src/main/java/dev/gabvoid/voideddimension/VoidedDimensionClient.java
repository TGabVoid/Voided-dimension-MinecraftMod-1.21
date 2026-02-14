package dev.gabvoid.voideddimension;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import dev.gabvoid.voideddimension.entity.ModEntities;
import dev.gabvoid.voideddimension.client.render.PuppetmanRenderer;
import dev.gabvoid.voideddimension.client.render.FragileBedrockOverlayRenderer;

public class VoidedDimensionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Código de inicialización del cliente
        System.out.println("VoidedDimensionClient inicializado correctamente.");

        EntityRendererRegistry.register(ModEntities.PUPPETMAN, PuppetmanRenderer::new);
        FragileBedrockOverlayRenderer.register();
    }
}
