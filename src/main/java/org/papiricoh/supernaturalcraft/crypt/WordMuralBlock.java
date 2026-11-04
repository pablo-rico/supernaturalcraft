package org.papiricoh.supernaturalcraft.crypt;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.supernaturalcraft.prophecy.Prophecies;
import org.papiricoh.supernaturalcraft.prophecy.Tablet;
import org.papiricoh.supernaturalcraft.prophecy.WordCipher;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * A mural of the Word in the crypt's library (v0.36): studied, it gives away {@link #TEACHES} letters of the Demon Tablet's
 * cipher (which ones follow from where it stands), and a prophet sees one more. Studying it twice teaches nothing new.
 */
public class WordMuralBlock extends HorizontalDirectionalBlock {

    public static final com.mojang.serialization.MapCodec<WordMuralBlock> CODEC = simpleCodec(WordMuralBlock::new);
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);
    public static final int TEACHES = 2;

    public WordMuralBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VARIANT, 0));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VARIANT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        int count = TEACHES + (Prophecies.prophet(sp) ? 1 : 0);
        int before = Prophecies.get(sp).known(Tablet.DEMON);
        int taught = WordCipher.lettersFrom(Mth.getSeed(pos), count);
        int after = Prophecies.update(sp, p -> p.withKnown(Tablet.DEMON, p.known(Tablet.DEMON) | taught)).known(Tablet.DEMON);
        int learnt = Integer.bitCount(after & ~before);
        sp.displayClientMessage(learnt > 0
                ? Component.translatable("message.supernaturalcraft.word_mural.learnt", learnt, WordCipher.letters(after & ~before)).withStyle(ChatFormatting.GOLD)
                : Component.translatable("message.supernaturalcraft.word_mural.known").withStyle(ChatFormatting.GRAY), true);
        if (learnt > 0) level.playSound(null, pos, AllSounds.TABLET_WHISPER.get(), SoundSource.BLOCKS, 0.7f, 1.2f);
        return InteractionResult.CONSUME;
    }
}
