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

    public static void register() {
        FabricDefaultAttributeRegistry.register(PUPPETMAN, PuppetmanEntity.createAttributes());
        VoidedDimension.LOGGER.info("Entidades registradas: puppetman");
    }
}
