package dev.gabvoid.voideddimension.world;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.world.feature.PillarFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

public class ModFeatures {
    // Register the custom Feature at class-load time so it's available before datapack registry loading
    public static final Feature<DefaultFeatureConfig> PILLAR_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "pillar"),
            new PillarFeature(DefaultFeatureConfig.CODEC)
    );

    public static void register() {
        var checkId = Identifier.of(VoidedDimension.MOD_ID, "pillar");
        var present = Registries.FEATURE.get(checkId);
        VoidedDimension.LOGGER.info("[voideddimension] Feature '{}' registered at init? {}", checkId, present != null);
        System.out.println("[ModFeatures] Feature present in registry at init: " + (present != null));

        // Hook the placed feature (defined via datapack JSON) into all biomes at LOCAL_MODIFICATIONS
        RegistryKey<PlacedFeature> placedKey = RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                Identifier.of(VoidedDimension.MOD_ID, "gap_pillars"));
        BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Feature.LOCAL_MODIFICATIONS, placedKey);
        VoidedDimension.LOGGER.info("[voideddimension] Added placed feature {} to all biomes at {}", placedKey.getValue(), GenerationStep.Feature.LOCAL_MODIFICATIONS);
        System.out.println("[ModFeatures] Added placed feature 'gap_pillars' to all biomes");
    }
}
