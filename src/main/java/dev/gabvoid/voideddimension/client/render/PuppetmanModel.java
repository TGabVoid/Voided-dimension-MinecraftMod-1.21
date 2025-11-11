package dev.gabvoid.voideddimension.client.render;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.entity.PuppetmanEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class PuppetmanModel extends DefaultedEntityGeoModel<PuppetmanEntity> {
    public PuppetmanModel() {
        super(Identifier.of(VoidedDimension.MOD_ID, "puppetman"));
    }
}
