package org.papiricoh.supernaturalcraft.hunter;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;

/**
 * The devil's trap: a 3×3 painted sigil. Each block is one ninth of the drawing ({@link #PART}
 * 0–8, row-major, 4 is the centre). Any demon standing on any part is held in place.
 *
 * <p>v0.36: a trap may be painted on a ceiling ({@link #FACE}); it then holds whatever stands in the 3×3 column below it, down
 * to the first solid block ({@link #CEILING_REACH} at most), checked every {@link #CEILING_TICKS} by its centre (never a boss).
 * A floor trap may be hidden under a carpet ({@link #COVER}): it looks like the rug and still holds; breaking it takes the
 * rug off, not the paint.
 */
public class DevilsTrapBlock extends Block {

    public static final MapCodec<DevilsTrapBlock> CODEC = simpleCodec(DevilsTrapBlock::new);
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    public static final int CENTER = 4;
    public static final EnumProperty<Face> FACE = EnumProperty.create("face", Face.class);
    public static final EnumProperty<Cover> COVER = EnumProperty.create("cover", Cover.class);
    /** How far below a ceiling trap it reaches (blocks), and how often its centre looks. */
    public static final int CEILING_REACH = 8, CEILING_TICKS = 10;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 0.5, 16);
    private static final VoxelShape CEILING_SHAPE = Block.box(0, 15.5, 0, 16, 16, 16);
    private static final VoxelShape COVERED_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    /** Where the drawing is painted. */
    public enum Face implements StringRepresentable {
        FLOOR, CEILING;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** The rug hiding a floor trap, if any. */
    public enum Cover implements StringRepresentable {
        NONE(null), WHITE(DyeColor.WHITE), ORANGE(DyeColor.ORANGE), MAGENTA(DyeColor.MAGENTA), LIGHT_BLUE(DyeColor.LIGHT_BLUE),
        YELLOW(DyeColor.YELLOW), LIME(DyeColor.LIME), PINK(DyeColor.PINK), GRAY(DyeColor.GRAY), LIGHT_GRAY(DyeColor.LIGHT_GRAY),
        CYAN(DyeColor.CYAN), PURPLE(DyeColor.PURPLE), BLUE(DyeColor.BLUE), BROWN(DyeColor.BROWN), GREEN(DyeColor.GREEN),
        RED(DyeColor.RED), BLACK(DyeColor.BLACK);

        @Nullable
        public final DyeColor dye;

        Cover(@Nullable DyeColor dye) {
            this.dye = dye;
        }

        public static Cover of(DyeColor dye) {
            for (Cover c : values()) if (c.dye == dye) return c;
            return NONE;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public DevilsTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, CENTER).setValue(FACE, Face.FLOOR).setValue(COVER, Cover.NONE));
    }

    public static boolean onCeiling(BlockState state) {
        return state.getValue(FACE) == Face.CEILING;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACE, ctx.getClickedFace() == Direction.DOWN ? Face.CEILING : Face.FLOOR);
    }

    @Override
    protected MapCodec<? extends DevilsTrapBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACE, COVER);
    }

    /** Offset of {@code part} from the centre block, as (dx, dz). */
    public static int dx(int part) {
        return part % 3 - 1;
    }

    public static int dz(int part) {
        return part / 3 - 1;
    }

    public static BlockPos center(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        return pos.offset(-dx(part), 0, -dz(part));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (onCeiling(state)) return CEILING_SHAPE;
        return state.getValue(COVER) == Cover.NONE ? SHAPE : COVERED_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction toward = support(state);
        BlockPos on = pos.relative(toward);
        return level.getBlockState(on).isFaceSturdy(level, on, toward.getOpposite());
    }

    /** The side the drawing is painted on. */
    private static Direction support(BlockState state) {
        return onCeiling(state) ? Direction.UP : Direction.DOWN;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (dir == support(state) && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        // A trap with a missing ninth is a broken seal: the whole drawing fades.
        if (dir.getAxis().isHorizontal() && !neighbor.is(this) && expectsPartAt(state, dir)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    private static boolean expectsPartAt(BlockState state, Direction dir) {
        int part = state.getValue(PART);
        int nx = dx(part) + dir.getStepX();
        int nz = dz(part) + dir.getStepZ();
        return nx >= -1 && nx <= 1 && nz >= -1 && nz <= 1;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Only the centre drops the item, so breaking any part breaks the centre too.
        BlockPos center = center(pos, state);
        if (!center.equals(pos) && level.getBlockState(center).is(this)) {
            level.destroyBlock(center, !player.isCreative(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!onCeiling(state) && entity instanceof LivingEntity living) hold(state, level, pos, living);
    }

    /** Holds {@code living} if it is a demon: the trap at {@code pos} flares as it catches and as what it holds strains. */
    public static void hold(BlockState state, Level level, BlockPos pos, LivingEntity living) {
        if (!level.isClientSide && org.papiricoh.supernaturalcraft.allegiance.Kin.isDemon(living)) {
            boolean fresh = !living.hasEffect(AllMobEffects.TRAPPED);
            living.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, 40, 0, false, true, true));
            // v0.21: the trap flares as it catches, and now and then as what it holds strains against it.
            if (level instanceof ServerLevel server) {
                Vec3 c = flareAt(center(pos, state), state);
                if (fresh) {
                    MagicFx.trapFlare(server, c, living, TRAP_RADIUS, MagicFxPayload.TRAP_CAUGHT, FLARE_TICKS);
                } else if ((server.getGameTime() + living.getId()) % STRAIN_INTERVAL == 0) {
                    MagicFx.trapFlare(server, c, living, TRAP_RADIUS, MagicFxPayload.TRAP_STRAIN, FLARE_TICKS);
                }
            }
        }
    }

    /** The drawing's half-width (blocks), how long a flare plays, ticks between two strains of a held demon. */
    public static final float TRAP_RADIUS = 1.5f;
    public static final int FLARE_TICKS = 20, STRAIN_INTERVAL = 60;

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        // The broken seal flares once, from its centre.
        if (!newState.is(this) && state.getValue(PART) == CENTER && level instanceof ServerLevel server) {
            MagicFx.trapFlare(server, flareAt(pos, state), null, TRAP_RADIUS, MagicFxPayload.TRAP_BROKEN, FLARE_TICKS);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    private static Vec3 flareAt(BlockPos center, BlockState state) {
        return onCeiling(state) ? Vec3.atBottomCenterOf(center).add(0, 0.95, 0) : Vec3.atBottomCenterOf(center).add(0, 0.05, 0);
    }

    // --- v0.36: on the ceiling ------------------------------------------------------------------------------------------

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide && onCeiling(state) && state.getValue(PART) == CENTER) level.scheduleTick(pos, this, CEILING_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!onCeiling(state) || state.getValue(PART) != CENTER) return;
        int depth = reach(level, pos);
        AABB column = new AABB(pos.getX() - 1, pos.getY() + 1 - depth, pos.getZ() - 1, pos.getX() + 2, pos.getY() + 1, pos.getZ() + 2);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, column,
                e -> e.isAlive() && !e.getType().is(AllTags.Entities.BOSSES))) {
            hold(state, level, pos, e);
        }
        level.scheduleTick(pos, this, CEILING_TICKS);
    }

    /** How many blocks a ceiling trap centred at {@code center} reaches down: to the first solid block, at most {@link #CEILING_REACH}. */
    public static int reach(BlockGetter level, BlockPos center) {
        int depth = 1;
        BlockPos.MutableBlockPos m = center.mutable();
        while (depth < CEILING_REACH) {
            m.move(Direction.DOWN);
            if (level.getBlockState(m).isFaceSturdy(level, m, Direction.UP)) break;
            depth++;
        }
        return depth;
    }

    // --- v0.36: under a rug ---------------------------------------------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (onCeiling(state) || state.getValue(COVER) != Cover.NONE || !stack.is(ItemTags.WOOL_CARPETS)
                || !(stack.getItem() instanceof BlockItem bi) || !(bi.getBlock() instanceof WoolCarpetBlock carpet)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            setCover(level, center(pos, state), Cover.of(carpet.getColor()));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1f, 1f);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Lays (or, with {@link Cover#NONE}, lifts) a rug over every ninth of the trap centred at {@code center}. */
    public static void setCover(Level level, BlockPos center, Cover cover) {
        for (int part = 0; part < 9; part++) {
            BlockPos p = center.offset(dx(part), 0, dz(part));
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof DevilsTrapBlock && s.getValue(PART) == part) {
                level.setBlock(p, s.setValue(COVER, cover), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        Cover cover = state.getValue(COVER);
        if (cover == Cover.NONE) return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        // Breaking a hidden trap lifts the rug: the paint is still there.
        if (!level.isClientSide) {
            setCover(level, center(pos, state), Cover.NONE);
            if (!player.getAbilities().instabuild && cover.dye != null) {
                popResource(level, pos, new ItemStack(carpetItem(cover.dye)));
            }
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 1f, 1f);
        }
        return false;
    }

    private static net.minecraft.world.item.Item carpetItem(DyeColor dye) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace(dye.getName() + "_carpet"));
    }
}
