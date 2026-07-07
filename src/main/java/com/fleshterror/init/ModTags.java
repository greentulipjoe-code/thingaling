package com.fleshterror.init;

import com.fleshterror.FleshTerrorMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModTags {
    // Any block in this tag can be torn out of the world by the monster's tentacles.
    // See src/main/resources/data/fleshterror/tags/blocks/grabbable_structure_blocks.json
    public static final TagKey<Block> GRABBABLE_STRUCTURE_BLOCKS = TagKey.create(
            Registries.BLOCK, new ResourceLocation(FleshTerrorMod.MOD_ID, "grabbable_structure_blocks"));
}
