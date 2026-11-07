package org.papiricoh.supernaturalcraft.apocalypse;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.apocalypse.seal.SealWorld;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.BossShares;
import org.papiricoh.supernaturalcraft.cinematic.CinematicLocks;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferEntity;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.util.Players;

import java.util.UUID;

/**
 * Swan Song (v0.40): season 5's finale. The first Lucifer beaten, the four rings (forged into the Key) open the Cage under him
 * and he falls in. Everyone who fought him earns {@code main/swan_song}; the first time in a world, the Cage closes on the
 * Apocalypse ({@link SealWorld#cageClosed}: the omens fade to an echo). His spoils (the Shackle and the rings given back) are
 * his loot table's, shared per hunter by {@link BossShares}.
 */
public final class SwanSong {

    /** The cinematic ({@code cinematics/swan_song.json}, anchored on Lucifer), its length and reach. */
    public static final int TICKS = 120;
    public static final double RANGE = 48;

    private SwanSong() {
    }

    public static void play(LuciferEntity lucifer, ServerLevel level, ArenaController arena) {
        Vec3 at = lucifer.position();
        CinematicLocks.play(lucifer, SupernaturalCraft.asResource(ApocalypseAssets.SWAN_SONG), TICKS, RANGE);
        level.playSound(null, lucifer.blockPosition(), AllSounds.apocalypse("apocalypse.cage_open"), SoundSource.HOSTILE, 4.0f, 0.8f);
        level.playSound(null, lucifer.blockPosition(), AllSounds.apocalypse("apocalypse.cage_fall"), SoundSource.HOSTILE, 3.0f, 0.7f);
        // The four rings, one at each quarter, and the light between them.
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 2 * i;
            level.sendParticles(AllParticles.RING_LIGHT.get(), at.x + Math.cos(a) * 3, at.y + 0.2, at.z + Math.sin(a) * 3, 12, 0.2, 0.1, 0.2, 0.01);
        }
        level.sendParticles(AllParticles.RING_LIGHT.get(), at.x, at.y + 0.5, at.z, 60, 1.6, 0.3, 1.6, 0.02);
        for (UUID id : BossShares.fighters(level, lucifer, RANGE)) {
            if (Players.byUuid(level, id) instanceof ServerPlayer p) {
                ChorusRewards.award(p, "main/swan_song");
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.swan_song").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
            }
        }
        if (level.getServer() != null && !SealWorld.echo(level.getServer())) {
            SealWorld.cageClosed(level.getServer());
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.swan_song.world").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
            }
        }
    }
}
