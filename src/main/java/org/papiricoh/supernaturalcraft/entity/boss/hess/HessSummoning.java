package org.papiricoh.supernaturalcraft.entity.boss.hess;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.british.BritishOrder;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.BritishRules;
import org.papiricoh.supernaturalcraft.british.BritishServer;
import org.papiricoh.supernaturalcraft.british.compound.CompoundAnchors;
import org.papiricoh.supernaturalcraft.british.site.BritishSites;
import org.papiricoh.supernaturalcraft.british.site.PlacedSite;
import org.papiricoh.supernaturalcraft.british.site.SiteBuilder;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;

/**
 * Calling Dr. Hess (v0.41). At the British compound the fight begins in place ({@link #begin}): a hunter who has had her
 * ultimatum and has not beaten her walks into the war room, and she is waiting with two of her agents. Beaten once, she comes
 * back only by the rite ({@code summon_hess}), at the compound. By command, anywhere ({@link #summon}).
 */
public final class HessSummoning {

    /** The arena round a free summoning; how close to her post a hunter starts the fight; the rite's reach from it. */
    public static final int FREE_RADIUS = 20;
    public static final double TRIGGER = 10, RITE_REACH = 14;
    /** Agents waiting with her when the fight begins. */
    public static final int GUARDS = 2;

    private HessSummoning() {
    }

    /** Whether a Hess fight is on (only one in the world). */
    public static boolean fighting(ServerLevel level) {
        for (ArenaController a : ArenaSavedData.get(level).all()) if (a.isActive() && a.theme() == ArenaTheme.BRITISH_HQ) return true;
        return false;
    }

    /** Whether {@code p} walking into the war room starts the fight. */
    public static boolean due(ServerPlayer p) {
        return !p.isSpectator() && !p.isCreative() && BritishOrder.choice(p) != BritishRules.Choice.NONE && !BritishOrder.hessBeaten(p);
    }

    /** Once a second: a hunter who has not beaten her, in the war room, starts the fight. */
    public static void tick(ServerLevel overworld) {
        if (!SiteBuilder.built(overworld.getServer(), BritishSites.COMPOUND) || fighting(overworld)) return;
        PlacedSite site = BritishSites.compound(overworld.getServer());
        if (site == null) return;
        Vec3 post = site.floor(CompoundAnchors.HESS);
        for (ServerPlayer p : overworld.players()) {
            if (p.position().distanceToSqr(post) > TRIGGER * TRIGGER || !due(p)) continue;
            begin(overworld, p, site, true);
            return;
        }
    }

    /** The fight at the compound: {@code first} the first one (her agents with her; a hunter still with London is written off). */
    public static @Nullable HessEntity begin(ServerLevel level, @Nullable ServerPlayer hunter, PlacedSite site, boolean first) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (hunter != null) hunter.displayClientMessage(busy, true);
            return null;
        }
        BlockPos centre = site.point(CompoundAnchors.ARENA_CENTRE);
        ArenaController arena = LuciferSummoning.openArena(level, centre != null ? centre : site.origin(), CompoundAnchors.ARENA_RADIUS, ArenaTheme.BRITISH_HQ);
        if (arena == null) return null;
        HessEntity boss = BritishRegistry.HESS.get().create(level);
        if (boss == null) return null;
        Vec3 at = site.floor(CompoundAnchors.HESS);
        Vec3 face = hunter != null ? hunter.position() : site.floor(CompoundAnchors.ENTRY);
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - at.x), face.z - at.z));
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindCompound(site.origin(), site.q());
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        if (hunter != null) {
            arena.join(hunter);
            if (BritishOrder.choice(hunter) == BritishRules.Choice.JOINED) BritishServer.betray(hunter);
        }
        if (first) {
            int n = 0;
            for (Vec3 s : boss.agentSpots()) {
                if (n++ >= boss.minionCount(GUARDS)) break;
                boss.callAgent(level, s);
            }
        }
        // The front door barred for the fight.
        for (BlockPos p : site.box(CompoundAnchors.GATE_FRONT)) {
            if (level.getBlockState(p).isAir() || level.getBlockState(p).canBeReplaced()) arena.mutate(level, p, Blocks.IRON_BARS.defaultBlockState(), 0);
        }
        boss.beginEmergence();
        if (hunter != null && first) {
            hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.hess.found").withStyle(ChatFormatting.DARK_AQUA), false);
        }
        return boss;
    }

    /** The rematch rite, at the compound. @return whether she answers */
    public static boolean rite(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = riteRefusal(level, altar);
        if (why != null) {
            if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        PlacedSite site = BritishSites.compound(level.getServer());
        return site != null && begin(level, ritualist, site, false) != null;
    }

    /** Why the rite fails here and now, or null: only at the compound, by the war room or its forecourt. */
    public static @Nullable Component riteRefusal(ServerLevel level, BlockPos altar) {
        PlacedSite site = BritishSites.compound(level.getServer());
        if (site == null || level.dimension() != Level.OVERWORLD || !nearPost(site, altar)) {
            return Component.translatable("message.supernaturalcraft.hess.rite.elsewhere");
        }
        return LuciferSummoning.refusal(level);
    }

    static boolean nearPost(PlacedSite site, BlockPos altar) {
        for (String n : new String[]{CompoundAnchors.HESS, CompoundAnchors.RITE}) {
            BlockPos p = site.point(n);
            if (p != null && p.distSqr(altar) <= RITE_REACH * RITE_REACH) return true;
        }
        return false;
    }

    /** Opens the fight at {@code at} for {@code player}; null if another fight holds the world (or it cannot start). */
    public static @Nullable Entity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer player) {
        ArenaController arena = LuciferSummoning.openArena(level, at, FREE_RADIUS, ArenaTheme.BRITISH_HQ);
        if (arena == null) {
            if (player != null) {
                Component why = LuciferSummoning.refusal(level);
                player.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.hess.busy").withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        HessEntity boss = BritishRegistry.HESS.get().create(level);
        if (boss == null) return null;
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 180, 0);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        if (player != null) arena.join(player);
        boss.beginEmergence();
        return boss;
    }
}
