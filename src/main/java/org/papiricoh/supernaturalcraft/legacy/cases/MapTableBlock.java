package org.papiricoh.supernaturalcraft.legacy.cases;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.Legacy;
import org.papiricoh.supernaturalcraft.legacy.bunker.BunkerProtection;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TableGrid;
import org.papiricoh.supernaturalcraft.legacy.cases.table.TablePart;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTable;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableBlockEntity;
import org.papiricoh.supernaturalcraft.legacy.cases.table.WarTableServerHandlers;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

/**
 * The war room's lit map table. A loose piece (v0.17) still works alone: a member who uses it with no case open is handed the
 * next one ({@link CaseOffice#issue}); with one open, it shows where. Nine pieces set down by hand in a 3×3 fuse into a war
 * table (v0.19, {@link TablePart}): a hologram of the land around it, its cases and pins, opened on use. Breaking any piece
 * breaks the table up again. Only members can break it.
 */
public class MapTableBlock extends Block implements EntityBlock {

    public static final EnumProperty<TablePart> PART = EnumProperty.create("part", TablePart.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);
    /** A formed piece ends at the glass, so a look at the table lands on the hologram. */
    private static final VoxelShape FORMED = Block.box(0, 0, 0, 16, 13, 16);

    public MapTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, TablePart.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART).formed() ? FORMED : SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == TablePart.CENTER ? new WarTableBlockEntity(pos, state) : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || state.getValue(PART).formed()) return;
        BlockPos centre = WarTable.tryForm(level, pos);
        if (centre == null) return;
        level.playSound(null, centre, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.7f, 1.6f);
        level.playSound(null, centre, AllSounds.LEGACY_MAP_TABLE.get(), SoundSource.BLOCKS, 0.8f, 1.2f);
        if (placer instanceof Player p) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.war_table.formed").withStyle(ChatFormatting.AQUA), true);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        BlockPos centre = WarTable.centre(pos, state);
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (centre != null && !newState.is(this)) WarTable.unform(level, centre);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        level.playSound(null, pos, AllSounds.LEGACY_MAP_TABLE.get(), SoundSource.BLOCKS, 0.8f, 1.0f);
        Legacy legacy = Legacies.get(sp);
        if (!legacy.member()) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.map_table.outsider").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResult.CONSUME;
        }
        BlockPos centre = WarTable.centre(pos, state);
        if (centre != null) {
            WarTableServerHandlers.open(sp, centre);
            return InteractionResult.CONSUME;
        }
        CaseFile open = CaseOffice.open(legacy);
        if (open != null) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.map_table.open",
                    Component.translatable("legacy.supernaturalcraft.case.scenario." + open.scenario()), open.site().getX(), open.site().getZ())
                    .withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.CONSUME;
        }
        CaseOffice.issue(sp, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        TablePart p = state.getValue(PART);
        if (!p.formed()) return state;
        int quarter = switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
        };
        return state.setValue(PART, TablePart.of(TableGrid.rotate(p.part(), quarter)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        TablePart p = state.getValue(PART);
        if (!p.formed() || mirror == Mirror.NONE) return state;
        return state.setValue(PART, TablePart.of(TableGrid.mirror(p.part(), mirror == Mirror.FRONT_BACK)));
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return BunkerProtection.mayBreak(player) ? super.getDestroyProgress(state, player, level, pos) : 0f;
    }
}
