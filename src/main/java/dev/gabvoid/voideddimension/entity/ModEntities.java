package dev.gabvoid.voideddimension.entity;

import dev.gabvoid.voideddimension.VoidedDimension;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<PuppetmanEntity> PUPPETMAN = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(VoidedDimension.MOD_ID, "puppetman"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, PuppetmanEntity::new)
                    .dimensions(EntityDimensions.fixed(3.0f, 9.75f))
                    .build()
    );

    public static final EntityType<WanderingFragmentEntity> WANDERING_FRAGMENT = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(VoidedDimension.MOD_ID, "wandering_fragment"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WanderingFragmentEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8f, 0.6f))
                    .build()
    );

    public static final EntityType<FragmentSummonerEntity> FRAGMENT_SUMMONER = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(VoidedDimension.MOD_ID, "fragment_summoner"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, FragmentSummonerEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0f, 1.1f))
                    .build()
    );

    public static final EntityType<ErraticEntity> ERRATIC = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(VoidedDimension.MOD_ID, "erratic"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ErraticEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0f, 1.0f))
                    .build()
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(PUPPETMAN, PuppetmanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WANDERING_FRAGMENT, WanderingFragmentEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(FRAGMENT_SUMMONER, FragmentSummonerEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ERRATIC, ErraticEntity.createAttributes());
        VoidedDimension.LOGGER.info("Entidades registradas: puppetman, wandering_fragment, fragment_summoner, erratic");
    }
}
