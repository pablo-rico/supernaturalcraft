package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.papiricoh.supernaturalcraft.allegiance.power.Passives;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The fire of holy oil (v0.13): burns {@link #LIFETIME} ticks without spreading; an angel inside a ring of it is held
 * ({@code TRAPPED}, refreshed while the ring burns) and one who walks through it burns. Harmless to anyone else.
 * A ring is "around" an angel when fire lies in at least {@link #ENCLOSED} of the eight directions within
 * {@link #ENCLOSE_REACH} blocks ({@link #enclosed}).
 */
public class HolyOilFireBlock extends Block {

    public static final int LIFETIME = 600, CHECK_TICKS = 10, HOLD_TICKS = 30;
    public static final int ENCLOSED = 7;
    public static final double ENCLOSE_REACH = 5;
    public static final float CROSSING_DAMAGE = 3f;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);
    /** When each fire goes out (transient: a reloaded ring burns one more lifetime). */
    private static final Map<GlobalPos, Long> EXPIRY = new ConcurrentHashMap<>();

    public HolyOilFireBlock(Properties properties) {
        super(properties);
    }

    /** Lights holy oil at {@code pos} (air above solid ground). @return whether it caught */
    public static boolean place(ServerLevel level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        if (!(here.isAir() || here.canBeReplaced()) || !here.getFluidState().isEmpty()) return false;
        BlockState fire = AllBlocks.HOLY_OIL_FIRE.get().defaultBlockState();
        if (!fire.canSurvive(level, pos)) return false;
        EXPIRY.put(GlobalPos.of(level.dimension(), pos.immutable()), level.getGameTime() + LIFETIME);
        level.setBlockAndUpdate(pos, fire);
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (!level.isClientSide) level.scheduleTick(pos, this, CHECK_TICKS);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) EXPIRY.remove(GlobalPos.of(level.dimension(), pos));
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos.immutable());
        long now = level.getGameTime();
        long until = EXPIRY.computeIfAbsent(key, k -> now + LIFETIME);
        if (now >= until) {
            level.removeBlock(pos, false);
            return;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(ENCLOSE_REACH),
                e -> e.isAlive() && Kin.isAngel(e))) {
            hold(level, e);
        }
        level.scheduleTick(pos, this, CHECK_TICKS);
    }

    /** Holds {@code angel} if a ring of this fire is around it. @return whether it is held */
    public static boolean hold(ServerLevel level, LivingEntity angel) {
        if (!enclosed(level, angel.position())) return false;
        boolean fresh = !angel.hasEffect(AllMobEffects.TRAPPED);
        angel.addEffect(new MobEffectInstance(AllMobEffects.TRAPPED, HOLD_TICKS, 0, false, true, true));
        if (fresh && angel instanceof ServerPlayer p) {
            AllegianceFx.toSelf(p, AllegianceFxPayload.WHISPER, Passives.WHISPER_TRAPPED_OIL, 0, p.position(), 60);
        }
        return true;
    }

    /** Whether fire lies in at least {@link #ENCLOSED} of the eight directions round {@code at}. */
    public static boolean enclosed(Level level, Vec3 at) {
        int hits = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4;
            double dx = Math.cos(a), dz = Math.sin(a);
            boolean hit = false;
            for (double d = 0.5; d <= ENCLOSE_REACH && !hit; d += 0.5) {
                for (int dy = -1; dy <= 1 && !hit; dy++) {
                    m.set(Math.floor(at.x + dx * d), Math.floor(at.y + 0.1) + dy, Math.floor(at.z + dz * d));
                    hit = level.getBlockState(m).getBlock() instanceof HolyOilFireBlock;
                }
            }
            if (hit) hits++;
        }
        return hits >= ENCLOSED;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || !Kin.isAngel(living)) return;
        living.igniteForSeconds(2);
        living.hurt(AllDamageTypes.source(level, AllDamageTypes.SMITE, null), CROSSING_DAMAGE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + random.nextDouble(), pos.getY() + 0.1, pos.getZ() + random.nextDouble(), 0, 0.03, 0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(AllParticles.GRACE.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.4, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }
}
