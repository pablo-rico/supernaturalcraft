package org.papiricoh.supernaturalcraft.entity.boss.chuck.arena;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

/** Solid ink: the lines the Author draws his arenas with (the ruled lines of the page, the text of the giant book). */
public class InkBlock extends Block {

    public static final MapCodec<InkBlock> CODEC = simpleCodec(InkBlock::new);

    public InkBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
