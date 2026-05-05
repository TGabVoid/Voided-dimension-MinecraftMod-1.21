package dev.gabvoid.voideddimension.client.render;

import dev.gabvoid.voideddimension.entity.ErraticEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ErraticRenderer extends GeoEntityRenderer<ErraticEntity> {
    public ErraticRenderer(EntityRendererFactory.Context context) {
        super(context, new ErraticModel());
        this.shadowRadius = 0.45f;
    }
}

