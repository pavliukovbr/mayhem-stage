package dev.pavliukovbr.mayhem;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

/**
 * Materiais do palco. Sem BlockItem de proposito: estes blocos existem para
 * serem colocados por function do datapack, nao para inventario.
 */
public final class StageBlocks {
    public static Block IVORY_PLAIN;
    public static Block IVORY_CARVED;
    public static Block IVORY_FLUTED;
    public static Block IVORY_STAIRS;
    public static Block IVORY_SLAB;
    public static Block IVORY_WALL;
    public static Block GLOSS_BLACK;
    public static Block VELVET_BLACK;
    public static Block LED_WHITE;
    public static Block LED_RED;
    public static Block GOLD_LEAF;

    private static Block reg(String name, Function<BlockBehaviour.Properties, Block> factory,
                             BlockBehaviour.Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(MayhemShow.MOD_ID, name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(props.setId(key)));
    }

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().strength(1.5f, 6.0f).sound(SoundType.STONE);
    }

    public static void init() {
        IVORY_PLAIN  = reg("ivory_plain", Block::new, stone());
        IVORY_CARVED = reg("ivory_carved", Block::new, stone());
        IVORY_FLUTED = reg("ivory_fluted", RotatedPillarBlock::new, stone());
        IVORY_STAIRS = reg("ivory_stairs",
                p -> new StairBlock(IVORY_PLAIN.defaultBlockState(), p), stone());
        IVORY_SLAB   = reg("ivory_slab", SlabBlock::new, stone());
        IVORY_WALL   = reg("ivory_wall", WallBlock::new, stone());
        GLOSS_BLACK  = reg("gloss_black", Block::new, stone());
        VELVET_BLACK = reg("velvet_black", Block::new, stone());
        LED_WHITE    = reg("led_white", Block::new, stone().lightLevel(s -> 15));
        LED_RED      = reg("led_red", Block::new, stone().lightLevel(s -> 15));
        GOLD_LEAF    = reg("gold_leaf", Block::new, stone());
    }

    private StageBlocks() {}
}
