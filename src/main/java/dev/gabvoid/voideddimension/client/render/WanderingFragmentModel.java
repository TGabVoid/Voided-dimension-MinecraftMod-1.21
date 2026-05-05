package dev.gabvoid.voideddimension.client.render;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.entity.WanderingFragmentEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class WanderingFragmentModel extends DefaultedEntityGeoModel<WanderingFragmentEntity> {
    public WanderingFragmentModel() {
        super(Identifier.of(VoidedDimension.MOD_ID, "wandering_fragment"));
    }
}
