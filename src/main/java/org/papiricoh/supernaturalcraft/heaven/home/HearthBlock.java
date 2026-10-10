package org.papiricoh.supernaturalcraft.heaven.home;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenPassage;
import org.papiricoh.supernaturalcraft.heaven.passage.HeavenStanding;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlot;
import org.papiricoh.supernaturalcraft.heaven.plot.HeavenPlots;
import org.papiricoh.supernaturalcraft.hell.Torment;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.List;

/**
 * The hearth of a hunter's home in Heaven (v0.18): rest by it (use it) to be healed whole and fed, rid of every harmful effect
 * and of Hell's Torment, with a minute of Regeneration; then it needs {@code SNConfig.HEAVEN_HEARTH_COOLDOWN} ticks before the
 * next rest. The first rest earns {@code home_sweet_heaven}. In a plot only its owner, the hunters they trust and operators may
 * rest by it; a hearth anywhere else serves anyone.
 * <p>State: {@link #FACING} (its front, where the fire shows).
 */
public class HearthBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<HearthBlock> CODEC = simpleCodec(HearthBlock::new);
    /** Regeneration after a rest. */
    public static final int REGENERATION_TICKS = 1200;

    public enum Rest { RESTED, COOLDOWN, NOT_YOURS }

    public HearthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            Rest r = rest(sl, pos, sp);
            switch (r) {
                case COOLDOWN -> {
                    long left = HeavenPassage.get(sp).lastRest() + SNConfig.HEAVEN_HEARTH_COOLDOWN.get() - sl.getGameTime();
                    sp.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.hearth_cooling",
                            Math.max(1, (left + 1199) / 1200)).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                }
                case NOT_YOURS -> sp.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.hearth_not_yours")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
                case RESTED -> sp.displayClientMessage(Component.translatable("message.supernaturalcraft.heaven.hearth_rested")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** {@code player} rests by the hearth at {@code pos}, if they may and it is time. */
    public static Rest rest(ServerLevel level, BlockPos pos, ServerPlayer player) {
        HeavenPlot plot = HeavenPlots.plotAt(level, pos);
        if (plot != null && !plot.hub() && !player.getUUID().equals(plot.owner) && !plot.trusted.contains(player.getUUID())
                && !player.hasPermissions(2)) {
            return Rest.NOT_YOURS;
        }
        HeavenStanding s = HeavenPassage.get(player);
        long now = level.getGameTime();
        if (s.lastRest() > 0 && now >= s.lastRest() && now - s.lastRest() < SNConfig.HEAVEN_HEARTH_COOLDOWN.get()) return Rest.COOLDOWN;
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20f);
        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance e : player.getActiveEffects()) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) harmful.add(e);
        }
        harmful.forEach(e -> player.removeEffect(e.getEffect()));
        player.clearFire();
        Torment.set(player, 0f);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, 0));
        HeavenPassage.set(player, s.withLastRest(Math.max(1, now)));
        ChorusRewards.award(player, "main/home_sweet_heaven");
        level.playSound(null, pos, AllSounds.heaven("heaven.hearth_rest"), SoundSource.BLOCKS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 24, 0.4, 0.8, 0.4, 0.02);
        return Rest.RESTED;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        var front = state.getValue(FACING);
        double x = pos.getX() + 0.5 + front.getStepX() * 0.52, z = pos.getZ() + 0.5 + front.getStepZ() * 0.52;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x + (random.nextDouble() - 0.5) * 0.4,
                pos.getY() + 0.2 + random.nextDouble() * 0.3, z + (random.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
        if (random.nextInt(24) == 0) {
            level.playLocalSound(x, pos.getY() + 0.3, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 1.0f, false);
        }
    }
}
