package org.papiricoh.supernaturalcraft.light;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.papiricoh.supernaturalcraft.entity.magic.SigilBolt;
import org.papiricoh.supernaturalcraft.registry.AllParticles;

/**
 * A brazier of holy fire that the Darkness's summoning raises at the four cardinal points of her
 * arena. Each one burning weakens her; she puts them out, and you light them again with flint and
 * steel, a fire charge, a torch or a spell. Unbreakable, and gone when the arena is restored.
 */
public class LightWellBlock extends Block {

    public static final MapCodec<LightWellBlock> CODEC = simpleCodec(LightWellBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LightWellBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, true));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    public static boolean kindles(ItemStack stack) {
        return stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE) || stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(LIT) || !kindles(stack)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            relight(level, pos, state);
            if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            else if (stack.is(Items.FIRE_CHARGE) && !player.getAbilities().instabuild) stack.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && !state.getValue(LIT) && (projectile instanceof SigilBolt || projectile.isOnFire())) {
            relight(level, hit.getBlockPos(), state);
        }
    }

    public static void relight(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 1.4f);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        for (int i = 0; i < 2; i++) {
            level.addParticle(AllParticles.GRACE.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.05 + random.nextDouble() * 0.05, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0.02, 0);
        }
    }
}
