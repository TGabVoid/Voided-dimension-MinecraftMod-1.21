package dev.gabvoid.voideddimension.client.render;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.entity.FragmentSummonerEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class FragmentSummonerModel extends DefaultedEntityGeoModel<FragmentSummonerEntity> {
    public FragmentSummonerModel() {
        super(Identifier.of(VoidedDimension.MOD_ID, "fragment_summoner"));
    }
}
