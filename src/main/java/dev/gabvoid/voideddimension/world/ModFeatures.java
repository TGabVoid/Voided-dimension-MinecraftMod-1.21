package dev.gabvoid.voideddimension.world;

import dev.gabvoid.voideddimension.VoidedDimension;
import dev.gabvoid.voideddimension.world.feature.BedrockFragmentHoleFeature;
import dev.gabvoid.voideddimension.world.feature.BridgesFeature;
import dev.gabvoid.voideddimension.world.feature.DryCracksFeature;
import dev.gabvoid.voideddimension.world.feature.FragmentedEdgesFeature;
import dev.gabvoid.voideddimension.world.feature.AbyssalFusteFeature;
import dev.gabvoid.voideddimension.world.feature.GiantVoidFeature;
import dev.gabvoid.voideddimension.world.feature.PillarFeature;
import dev.gabvoid.voideddimension.world.feature.RosePetalsFeature;
import dev.gabvoid.voideddimension.world.feature.VoidShaftCavesFeature;
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

    private static final Feature<DefaultFeatureConfig> DRY_CRACKS_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "dry_cracks"),
            new DryCracksFeature(DefaultFeatureConfig.CODEC)
    );

    private static final Feature<DefaultFeatureConfig> BRIDGES_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "bridges"),
            new BridgesFeature(DefaultFeatureConfig.CODEC)
    );

    // Id "real" (usado por tus configured_feature/*.json)
    private static final Feature<DefaultFeatureConfig> BEDROCK_FRAGMENT_HOLE_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "bedrock_fragment_hole"),
            new BedrockFragmentHoleFeature(DefaultFeatureConfig.CODEC)
    );

    // Alias para que /locate feature voideddimension:bedrock_hole funcione
    private static final Feature<DefaultFeatureConfig> BEDROCK_HOLE_FEATURE_ALIAS = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "bedrock_hole"),
            new BedrockFragmentHoleFeature(DefaultFeatureConfig.CODEC)
    );

    private static final Feature<DefaultFeatureConfig> ROSE_PETALS_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "rose_petals"),
            new RosePetalsFeature(DefaultFeatureConfig.CODEC)
    );

    private static final Feature<DefaultFeatureConfig> ABYSSAL_FUSTE_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "abyssal_fuste_feature"),
            new AbyssalFusteFeature(DefaultFeatureConfig.CODEC)
    );

    private static final Feature<DefaultFeatureConfig> GIANT_VOID_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "giant_void"),
            new GiantVoidFeature(DefaultFeatureConfig.CODEC)
    );

    private static final Feature<DefaultFeatureConfig> VOID_SHAFTS_FEATURE = Registry.register(
            Registries.FEATURE,
            Identifier.of(VoidedDimension.MOD_ID, "void_shafts"),
            new VoidShaftCavesFeature(DefaultFeatureConfig.CODEC)
    );

    public static void register() {
        var checkId = Identifier.of(VoidedDimension.MOD_ID, "pillar");
        var present = Registries.FEATURE.get(checkId);
        VoidedDimension.LOGGER.info("[voideddimension] Feature '{}' registered at init? {}", checkId, present != null);
        System.out.println("[ModFeatures] Feature present in registry at init: " + (present != null));

        RegistryKey<PlacedFeature> pillars = RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                Identifier.of(VoidedDimension.MOD_ID, "gap_pillars"));
        RegistryKey<PlacedFeature> giantVoid = RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                Identifier.of(VoidedDimension.MOD_ID, "giant_void"));
        RegistryKey<PlacedFeature> voidShafts = RegistryKey.of(RegistryKeys.PLACED_FEATURE,
                Identifier.of(VoidedDimension.MOD_ID, "void_shafts"));
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        ModDimensions.INACTIVE_THRESHOLD_KEY,
                        ModDimensions.FRAGMENTED_PLAINS_KEY,
                        ModDimensions.ROSE_DESERT_KEY,
                        ModDimensions.SHATTER_REEF_KEY
                ),
                GenerationStep.Feature.LOCAL_MODIFICATIONS,
                pillars
        );
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        ModDimensions.INACTIVE_THRESHOLD_KEY,
                        ModDimensions.FRAGMENTED_PLAINS_KEY,
                        ModDimensions.ROSE_DESERT_KEY,
                        ModDimensions.SHATTER_REEF_KEY
                ),
                GenerationStep.Feature.LOCAL_MODIFICATIONS,
                giantVoid
        );
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        ModDimensions.INACTIVE_THRESHOLD_KEY,
                        ModDimensions.FRAGMENTED_PLAINS_KEY,
                        ModDimensions.ROSE_DESERT_KEY,
                        ModDimensions.SHATTER_REEF_KEY
                ),
                GenerationStep.Feature.LOCAL_MODIFICATIONS,
                voidShafts
        );
        // Overworld "bedrock holes" ahora se hacen por mixin (más fiable y sin JSON vanilla)
        VoidedDimension.LOGGER.info("[voideddimension] Added placed feature {} to voided biomes at {}", pillars.getValue(), GenerationStep.Feature.LOCAL_MODIFICATIONS);
        VoidedDimension.LOGGER.info("[voideddimension] Added placed feature {} to voided biomes at {}", giantVoid.getValue(), GenerationStep.Feature.LOCAL_MODIFICATIONS);
        VoidedDimension.LOGGER.info("[voideddimension] Added placed feature {} to voided biomes at {}", voidShafts.getValue(), GenerationStep.Feature.LOCAL_MODIFICATIONS);
        VoidedDimension.LOGGER.info("[voideddimension] Overworld bedrock holes handled by mixin (no placed_feature)");
        System.out.println("[ModFeatures] Added placed feature 'gap_pillars' to voided biomes");
        System.out.println("[ModFeatures] Added placed feature 'giant_void' to voided biomes");
        System.out.println("[ModFeatures] Added placed feature 'void_shafts' to voided biomes");
        System.out.println("[ModFeatures] Overworld bedrock holes handled by mixin (no placed_feature)");
    }
}