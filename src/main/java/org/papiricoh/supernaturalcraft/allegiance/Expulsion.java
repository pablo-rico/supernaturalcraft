package org.papiricoh.supernaturalcraft.allegiance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.network.AllegianceFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.AllTags;

/**
 * An exorcism or a banishing on a sworn player (v0.13). A demon mob is cast out (it dies); a demon player is
 * <em>expelled</em> instead: a heavy blow that never kills by itself (it leaves at least one health point), the Corruption
 * torn out ({@link EssenceRules#EXPELLED_KEEPS}) and the smoke shown ({@link AllegianceFxPayload#EXPELLED}, arg 0). The
 * Enochian banishing sigil does the same to an angel (arg 1, light) and flings them far away; a lesser angel (a mob) is
 * simply sent back to Heaven.
 */
public final class Expulsion {

    /** How far (blocks, horizontally) the banishing sigil throws an angel player. */
    public static final int BANISH_MIN = 96, BANISH_MAX = 192;
    public static final float BANISH_DAMAGE = 4f;

    private Expulsion() {
    }

    /** Expels a demon player: damage (never lethal by itself), Corruption gone. */
    public static boolean expel(ServerPlayer player, @Nullable Entity by, float damage) {
        if (!Allegiances.get(player).isDemon()) return false;
        ServerLevel level = player.serverLevel();
        tear(player, damage, by);
        level.sendParticles(AllParticles.DEMON_SMOKE.get(), player.getX(), player.getEyeY(), player.getZ(), 60, 0.2, 1.4, 0.2, 0.08);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getEyeY(), player.getZ(), 30, 0.2, 1.0, 0.2, 0.05);
        AllegianceFx.around(player, AllegianceFxPayload.EXPELLED, 0, 0, player.getEyePosition(), 40);
        AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.expelled", false, ChatFormatting.DARK_RED);
        return true;
    }

    /** The banishing sigil on {@code target}: an angel player flung far away, a lesser angel sent back. */
    public static boolean banish(Entity target, @Nullable Entity by) {
        if (!Kin.isAngel(target) || target.getType().is(AllTags.Entities.BOSSES)) return false;
        if (!(target.level() instanceof ServerLevel level)) return false;
        level.sendParticles(AllParticles.GRACE.get(), target.getX(), target.getY() + 1, target.getZ(), 50, 0.3, 1.0, 0.3, 0.15);
        level.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, target.blockPosition(), AllSounds.ALLEGIANCE_EXPEL.get(), SoundSource.PLAYERS, 1.5f, 1.4f);
        if (target instanceof ServerPlayer player) {
            tear(player, BANISH_DAMAGE, by);
            AllegianceFx.around(player, AllegianceFxPayload.EXPELLED, 1, 0, player.getEyePosition(), 40);
            double a = level.random.nextDouble() * Math.PI * 2;
            int d = BANISH_MIN + level.random.nextInt(BANISH_MAX - BANISH_MIN + 1);
            int x = (int) Math.round(player.getX() + Math.cos(a) * d), z = (int) Math.round(player.getZ() + Math.sin(a) * d);
            level.getChunk(x >> 4, z >> 4);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            player.teleportTo(level, x + 0.5, Math.max(top.getY(), level.getMinBuildHeight() + 1), z + 0.5, player.getYRot(), player.getXRot());
            AllegianceFx.tell(player, "message.supernaturalcraft.allegiance.banished", false, ChatFormatting.GOLD);
            return true;
        }
        target.discard();
        return true;
    }

    /** The blow and the essence torn out. */
    private static void tear(ServerPlayer player, float damage, @Nullable Entity by) {
        Allegiances.set(player, Allegiances.get(player).withEssence(EssenceRules.EXPELLED_KEEPS));
        float blow = Math.min(damage, Math.max(0, player.getHealth() - 1));
        // No attacker: a rite is not a duel, so it lands whatever the PvP rules.
        if (blow > 0) player.hurt(AllDamageTypes.source(player.level(), AllDamageTypes.SMITE, null), blow);
        player.serverLevel().playSound(null, player.blockPosition(), AllSounds.ALLEGIANCE_EXPEL.get(), SoundSource.PLAYERS, 1.5f, 1f);
    }

    /** Whether an exorcism should expel {@code e} rather than cast it out. */
    public static boolean expelsInstead(LivingEntity e) {
        return e instanceof ServerPlayer;
    }
}
