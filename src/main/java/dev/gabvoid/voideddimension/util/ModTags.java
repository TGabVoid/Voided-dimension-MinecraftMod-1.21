package dev.gabvoid.voideddimension.util;

import dev.gabvoid.voideddimension.VoidedDimension;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class ModTags {
    public static class Blocks {
        private static TagKey<Block> createTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, 
                Identifier.of(VoidedDimension.MOD_ID, name));
        }

        public static final TagKey<Block> VOID_ORES = TagKey.of(
                RegistryKeys.BLOCK,
                Identifier.of("voideddimension", "void_ores")
        );
    }

    public static class Items {
        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM,
                Identifier.of(VoidedDimension.MOD_ID, name));
        }
        public static final TagKey<Item> CONSUMABLE_ITEMS = createTag("consumable_items");
    }


}
