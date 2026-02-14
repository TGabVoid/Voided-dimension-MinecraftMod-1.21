package dev.gabvoid.voideddimension.world;

import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.registry.Registries;

public class ModDimensions
{
    public static final RegistryKey<World> VOIDED_DIMENSION_KEY =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of("voideddimension", "voided_dimension"));
    public static final RegistryKey<Biome> INACTIVE_THRESHOLD_KEY =
            RegistryKey.of(RegistryKeys.BIOME, Identifier.of("voideddimension", "inactive_threshold"));
    public static final RegistryKey<Biome> FRAGMENTED_PLAINS_KEY =
            RegistryKey.of(RegistryKeys.BIOME, Identifier.of("voideddimension", "fragmented_plains"));
    public static final RegistryKey<Biome> ROSE_DESERT_KEY =
            RegistryKey.of(RegistryKeys.BIOME, Identifier.of("voideddimension", "rose_desert"));
    public static final RegistryKey<Biome> SHATTER_REEF_KEY =
            RegistryKey.of(RegistryKeys.BIOME, Identifier.of("voideddimension", "shatter_reef"));

    // No registrar ni crear el bioma aquí. Defínelo solo en los archivos JSON del datapack.
}