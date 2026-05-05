package dev.gabvoid.voideddimension.client.render;

import dev.gabvoid.voideddimension.entity.WanderingFragmentEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WanderingFragmentRenderer extends GeoEntityRenderer<WanderingFragmentEntity> {
    public WanderingFragmentRenderer(EntityRendererFactory.Context context) {
        super(context, new WanderingFragmentModel());
        this.shadowRadius = 0.6f;
    }
}
