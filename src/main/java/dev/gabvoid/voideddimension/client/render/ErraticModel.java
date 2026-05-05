package dev.gabvoid.voideddimension.client.render;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.entity.ErraticEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class ErraticModel extends DefaultedEntityGeoModel<ErraticEntity> {
    public ErraticModel() {
        super(Identifier.of(VoidedDimension.MOD_ID, "erratic"));
    }
}

