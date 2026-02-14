package dev.gabvoid.voideddimension.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class BedrockFragmentHoleFeature extends Feature<DefaultFeatureConfig> {
    public BedrockFragmentHoleFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) return false;
        if (!serverWorld.getRegistryKey().equals(World.OVERWORLD)) return false;

        BlockPos origin = context.getOrigin();

        // El hueco SIEMPRE empieza en -64 (pediste que sea fijo), independientemente del origin Y.
        int bottomY = -64;
        int topY = -55;

        // Usamos las coordenadas X/Z del origin, pero fijamos la Y.
        BlockPos base = new BlockPos(origin.getX(), bottomY, origin.getZ());

        for (int y = bottomY; y <= topY; y++) {
            int dy = y - bottomY;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos pos = base.add(dx, dy, dz);
                    // 2 = sin updates costosos. Queremos que SIEMPRE quede aire.
                    serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                }
            }
        }

        return true;
    }
}