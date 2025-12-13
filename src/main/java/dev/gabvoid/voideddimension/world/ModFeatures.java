package dev.gabvoid.voideddimension.world;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.world.feature.FragmentedEdgesFeature;
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
    private static final Feature<DefaultFeatureConfig> FRAGMENTED_EDGES_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "fragmented_edges"),
            new FragmentedEdgesFeature(DefaultFeatureConfig.CODEC)
    );

    public static void register() {
        var checkId = Identifier.of(VoidedDimension.MOD_ID, "pillar");
        var present = Registries.FEATURE.get(checkId);
        VoidedDimension.LOGGER.info("[voideddimension] Feature '{}' registered at init? {}", checkId, present != null);
        System.out.println("[ModFeatures] Feature present in registry at init: " + (present != null));

        RegistryKey<PlacedFeature> pillars = RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                Identifier.of(VoidedDimension.MOD_ID, "gap_pillars"));
        RegistryKey<PlacedFeature> edgesFeature = RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                Identifier.of(VoidedDimension.MOD_ID, "fragmented_edges"));
        BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Feature.LOCAL_MODIFICATIONS, pillars);
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(ModDimensions.FRAGMENTED_PLAINS_KEY), GenerationStep.Feature.UNDERGROUND_DECORATION, edgesFeature);
        VoidedDimension.LOGGER.info("[voideddimension] Added placed feature {} to all biomes at {}", pillars.getValue(), GenerationStep.Feature.LOCAL_MODIFICATIONS);
        VoidedDimension.LOGGER.info("[voideddimension] Added placed feature {} to all biomes at {}", edgesFeature.getValue(), GenerationStep.Feature.UNDERGROUND_DECORATION);
        System.out.println("[ModFeatures] Added placed feature 'gap_pillars' to all biomes");
        System.out.println("[ModFeatures] Added placed feature 'fragmented_edges' to fragmented plains");
    }
}