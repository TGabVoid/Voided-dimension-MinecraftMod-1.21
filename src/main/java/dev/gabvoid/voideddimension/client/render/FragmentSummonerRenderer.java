package dev.gabvoid.voideddimension.client.render;

import dev.gabvoid.voideddimension.entity.FragmentSummonerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FragmentSummonerRenderer extends GeoEntityRenderer<FragmentSummonerEntity> {
    public FragmentSummonerRenderer(EntityRendererFactory.Context context) {
        super(context, new FragmentSummonerModel());
        this.shadowRadius = 0.35f;
    }
}
