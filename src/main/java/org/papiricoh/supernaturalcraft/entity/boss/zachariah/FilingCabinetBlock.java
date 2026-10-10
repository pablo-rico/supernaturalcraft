package org.papiricoh.supernaturalcraft.entity.boss.zachariah;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * A filing cabinet of Zachariah's office, numbered I-IV ({@link #NUMBER}, its front shows the numeral; {@link #FACING} where the
 * drawers face). Use a Heavenly Form on the cabinet its number names and the form's holder is Approved
 * ({@link ZachariahEntity#file}); the wrong cabinet only rattles. Anyone may file anyone's form: carrying a friend's paperwork is
 * allowed (it is still filed for whoever it was issued to). No block entity (the office's arena writes and gives these back).
 * <p>Builders: sneak-use with an empty hand in creative to change the number.
 */
public class FilingCabinetBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<FilingCabinetBlock> CODEC = simpleCodec(FilingCabinetBlock::new);
    /** Which cabinet it is (1-4, shown as I-IV on its front). */
    public static final IntegerProperty NUMBER = IntegerProperty.create("number", 1, FormRules.CABINETS);

    public FilingCabinetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(NUMBER, 1));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NUMBER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(AllItems.HEAVENLY_FORM.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        fileAt((ServerLevel) level, pos, state, player, stack);
        return ItemInteractionResult.CONSUME;
    }

    /**
     * {@code player} files {@code form} in the cabinet at {@code pos}. @return what came of it ({@code NOTHING_TO_FILE} also when
     * no fight holds the office)
     */
    public static ZachariahEntity.Filing fileAt(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack form) {
        HeavenlyForm f = form.get(AllDataComponents.HEAVENLY_FORM.get());
        ZachariahEntity boss = ZachariahEntity.holding(level, Vec3.atCenterOf(pos));
        int number = state.getValue(NUMBER);
        ZachariahEntity.Filing result = boss == null || f == null ? ZachariahEntity.Filing.NOTHING_TO_FILE
                : boss.file(f.owner(), number, false);
        switch (result) {
            case FILED -> {
                form.shrink(1);
                if (player instanceof ServerPlayer sp && !sp.getUUID().equals(f.owner())) {
                    sp.displayClientMessage(Component.translatable("message.supernaturalcraft.zachariah.filed_for_another")
                            .withStyle(ChatFormatting.GOLD), true);
                }
            }
            case WRONG_CABINET -> {
                level.playSound(null, pos, AllSounds.heaven("zachariah.denied"), SoundSource.BLOCKS, 1.0f, 1.0f);
                player.displayClientMessage(Component.translatable("message.supernaturalcraft.zachariah.wrong_cabinet",
                        FormRules.roman(number), FormRules.roman(f.number())).withStyle(ChatFormatting.RED), true);
            }
            case NOTHING_TO_FILE -> {
                level.playSound(null, pos, SoundEvents.CHISELED_BOOKSHELF_INSERT, SoundSource.BLOCKS, 1.0f, 0.8f);
                player.displayClientMessage(Component.translatable("message.supernaturalcraft.zachariah.office_closed")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
        return result;
    }

    /** A drawer rattles; a builder in creative, sneaking, changes its number. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isCreative() && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int next = state.getValue(NUMBER) % FormRules.CABINETS + 1;
                level.setBlock(pos, state.setValue(NUMBER, next), Block.UPDATE_ALL);
                player.displayClientMessage(Component.literal(FormRules.roman(next)), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) level.playSound(null, pos, AllSounds.heaven("zachariah.file"), SoundSource.BLOCKS, 0.8f, 1.0f);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
