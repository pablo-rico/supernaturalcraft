package org.papiricoh.supernaturalcraft.hunter.wall;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.allegiance.Expulsion;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.magic.fx.MagicFx;
import org.papiricoh.supernaturalcraft.magic.school.School;
import org.papiricoh.supernaturalcraft.network.MagicFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * The angel banishing sigil (v0.36), painted in its maker's own blood ({@link WallSigilItem}). A palm slammed on it sends
 * every angel within {@link #RADIUS} away — wild ones back to Heaven, angel players flung far off — friend or foe, the one
 * who struck it too if they are an angel. Never a boss. It is spent in the blast.
 */
public class BanishingSigilBlock extends WallSigilBlock {

    public static final int RADIUS = 16;

    public BanishingSigilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) blast(server, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Sets the sigil off: banishes the angels round it and wipes it. @return how many were sent away */
    public static int blast(ServerLevel level, BlockPos pos, Player by) {
        Vec3 c = Vec3.atCenterOf(pos);
        int sent = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RADIUS),
                e -> e.isAlive() && Kin.isAngel(e) && !e.getType().is(AllTags.Entities.BOSSES) && e.distanceToSqr(c) <= RADIUS * RADIUS)) {
            if (Expulsion.banish(e, by)) sent++;
        }
        MagicFx.ward(level, School.ENOCHIAN, null, c, RADIUS, MagicFxPayload.WARD_BROKEN, 30);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        level.playSound(null, pos, AllSounds.BANISH_BLAST.get(), SoundSource.BLOCKS, 2f, 1f);
        level.removeBlock(pos, false);
        return sent;
    }
}
