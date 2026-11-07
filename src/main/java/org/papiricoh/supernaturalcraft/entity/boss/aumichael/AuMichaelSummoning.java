package org.papiricoh.supernaturalcraft.entity.boss.aumichael;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.apocworld.ApocWorldRegistry;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressSite;
import org.papiricoh.supernaturalcraft.apocworld.fortress.FortressWorld;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;

/**
 * v0.41: the Michael of the Apocalypse World at his fortress. The fight begins in place ({@link #begin}) when a hunter on the road
 * that far ({@code IN_AU} or beyond) who has not seen him flee walks up to his throne. Once he has fled from them, he comes back
 * only by the rite ({@code summon_au_michael}, by his throne). By command, anywhere ({@link #summon}).
 */
public final class AuMichaelSummoning {

    /** The arena round a free summoning. */
    public static final int FREE_RADIUS = 22;

    private AuMichaelSummoning() {
    }

    /** Whether a fight with him is on in {@code level}. */
    public static boolean fighting(ServerLevel level) {
        for (ArenaController a : ArenaSavedData.get(level).all()) if (a.isActive() && a.theme() == ArenaTheme.FORTRESS) return true;
        return false;
    }

    /** Whether {@code p} may start the fight at his throne: on the road that far, and not yet beaten him. */
    public static boolean awaits(ServerPlayer p) {
        return Nephilims.step(p).atLeast(NephilimRules.Step.IN_AU) && !AuMichaelSpoils.beaten(p);
    }

    /** Once a second: a hunter he waits for, at his throne, starts the fight. */
    public static void tick(MinecraftServer server) {
        if (!FortressWorld.built(server)) return;
        FortressSite site = FortressWorld.site(server);
        ServerLevel level = ApocalypseWorld.level(server);
        if (site == null || fighting(level)) return;
        Vec3 throne = Vec3.atBottomCenterOf(site.throne());
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative() || p.position().distanceToSqr(throne) > AuMichaelBalance.TRIGGER * AuMichaelBalance.TRIGGER) continue;
            if (!awaits(p)) continue;
            begin(level, p, site);
            return;
        }
    }

    /** The fight at his fortress: the court is the arena, its gate barred, he rises from his throne. */
    public static @Nullable AuMichaelEntity begin(ServerLevel level, @Nullable ServerPlayer hunter, FortressSite site) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (hunter != null) hunter.displayClientMessage(busy, true);
            return null;
        }
        ArenaController arena = LuciferSummoning.openArena(level, site.centre(), AuMichaelBalance.ARENA_RADIUS, ArenaTheme.FORTRESS);
        if (arena == null) return null;
        AuMichaelEntity boss = ApocWorldRegistry.AU_MICHAEL.get().create(level);
        if (boss == null) return null;
        Vec3 at = Vec3.atBottomCenterOf(site.throne());
        float yaw = site.throneYaw();
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindFortress(site.origin(), site.q());
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        for (BlockPos p : site.gate()) {
            if (level.getBlockState(p).canBeReplaced()) arena.mutate(level, p, Blocks.IRON_BARS.defaultBlockState(), 0);
        }
        boss.beginEmergence();
        if (hunter != null) {
            hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.au_michael.found").withStyle(ChatFormatting.GOLD), false);
        }
        return boss;
    }

    /** The rematch rite, by his throne. @return whether he answers */
    public static boolean rite(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = riteRefusal(level, altar);
        if (why != null) {
            if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        FortressSite site = FortressWorld.site(level.getServer());
        return site != null && begin(level, ritualist, site) != null;
    }

    /** Why the rite fails here and now, or null: only at his fortress, by the throne. */
    public static @Nullable Component riteRefusal(ServerLevel level, BlockPos altar) {
        FortressSite site = FortressWorld.site(level.getServer());
        if (site == null || !ApocalypseWorld.is(level, altar) || level != ApocalypseWorld.level(level.getServer())
                || site.throne().distSqr(altar) > AuMichaelBalance.RITE_REACH * AuMichaelBalance.RITE_REACH) {
            return Component.translatable("message.supernaturalcraft.au_michael.rite.elsewhere");
        }
        return LuciferSummoning.refusal(level);
    }

    /** Opens the fight at {@code at} for {@code player}; null if another fight holds the world (or it cannot start). */
    public static @Nullable Entity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer player) {
        ArenaController arena = LuciferSummoning.openArena(level, at, FREE_RADIUS, ArenaTheme.FORTRESS);
        if (arena == null) {
            if (player != null) {
                Component why = LuciferSummoning.refusal(level);
                player.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.au_michael.busy")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        AuMichaelEntity boss = ApocWorldRegistry.AU_MICHAEL.get().create(level);
        if (boss == null) return null;
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 180, 0);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
