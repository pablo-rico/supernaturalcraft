package org.papiricoh.supernaturalcraft.entity.boss.dagon;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.british.BritishOrder;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.birth.BirthScene;
import org.papiricoh.supernaturalcraft.british.safehouse.SafehouseAnchors;
import org.papiricoh.supernaturalcraft.british.site.BritishSites;
import org.papiricoh.supernaturalcraft.british.site.PlacedSite;
import org.papiricoh.supernaturalcraft.british.site.SiteBuilder;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;

/**
 * Calling Dagon (v0.41). At Kelly Kline's safe house the fight begins in place ({@link #begin}): a hunter on the nephilim's
 * trail ({@link NephilimRules.Step#TRACING}) who comes near the cabin finds her standing guard before its door. Beaten once, she
 * comes back only by the rite ({@code summon_dagon}), at the safe house. By command, anywhere ({@link #summon}).
 */
public final class DagonSummoning {

    /** The arena round a free summoning; how close to her post a hunter starts the fight; the rite's reach from it. */
    public static final int FREE_RADIUS = 20;
    public static final double TRIGGER = 14, RITE_REACH = 16;

    private DagonSummoning() {
    }

    /** Whether a Dagon fight is on (only one in the world). */
    public static boolean fighting(ServerLevel level) {
        for (ArenaController a : ArenaSavedData.get(level).all()) if (a.isActive() && a.theme() == ArenaTheme.SAFEHOUSE) return true;
        return false;
    }

    /** Whether {@code p} coming near the safe house starts the fight: on the trail, and Dagon still stands for them. */
    public static boolean due(ServerPlayer p) {
        return !p.isSpectator() && !p.isCreative() && Nephilims.step(p) == NephilimRules.Step.TRACING && !BritishOrder.dagonBeaten(p);
    }

    /** Once a second: a hunter on the trail, at the safe house, starts the fight. */
    public static void tick(ServerLevel overworld) {
        if (!SiteBuilder.built(overworld.getServer(), BritishSites.SAFEHOUSE) || fighting(overworld) || BirthScene.active()) return;
        PlacedSite site = BritishSites.safehouse(overworld.getServer());
        if (site == null) return;
        Vec3 post = site.floor(SafehouseAnchors.GUARD);
        for (ServerPlayer p : overworld.players()) {
            if (p.position().distanceToSqr(post) > TRIGGER * TRIGGER || !due(p)) continue;
            begin(overworld, p, site);
            return;
        }
    }

    /** The fight at the safe house. */
    public static @Nullable DagonEntity begin(ServerLevel level, @Nullable ServerPlayer hunter, PlacedSite site) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (hunter != null) hunter.displayClientMessage(busy, true);
            return null;
        }
        BlockPos centre = site.point(SafehouseAnchors.ARENA_CENTRE);
        ArenaController arena = LuciferSummoning.openArena(level, centre != null ? centre : site.origin(), SafehouseAnchors.ARENA_RADIUS, ArenaTheme.SAFEHOUSE);
        if (arena == null) return null;
        DagonEntity boss = BritishRegistry.DAGON.get().create(level);
        if (boss == null) return null;
        Vec3 at = site.floor(SafehouseAnchors.GUARD);
        Vec3 face = hunter != null ? hunter.position() : site.floor(SafehouseAnchors.ENTRY);
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - at.x), face.z - at.z));
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindSafehouse(site.origin(), site.q());
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        if (hunter != null) {
            arena.join(hunter);
            boss.witness(hunter);
            hunter.displayClientMessage(Component.translatable("message.supernaturalcraft.dagon.found").withStyle(ChatFormatting.DARK_RED), false);
        }
        boss.beginEmergence();
        return boss;
    }

    /** The rematch rite, at the safe house. @return whether she answers */
    public static boolean rite(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = riteRefusal(level, altar);
        if (why != null) {
            if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        PlacedSite site = BritishSites.safehouse(level.getServer());
        return site != null && begin(level, ritualist, site) != null;
    }

    /** Why the rite fails here and now, or null: only at the safe house, in its clearing. */
    public static @Nullable Component riteRefusal(ServerLevel level, BlockPos altar) {
        PlacedSite site = BritishSites.safehouse(level.getServer());
        BlockPos post = site == null ? null : site.point(SafehouseAnchors.ARENA_CENTRE);
        if (post == null || level.dimension() != Level.OVERWORLD || post.distSqr(altar) > RITE_REACH * RITE_REACH) {
            return Component.translatable("message.supernaturalcraft.dagon.rite.elsewhere");
        }
        if (BirthScene.active()) return Component.translatable("message.supernaturalcraft.dagon.busy");
        return LuciferSummoning.refusal(level);
    }

    /** Opens the fight at {@code at} for {@code player}; null if another fight holds the world (or it cannot start). */
    public static @Nullable Entity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer player) {
        ArenaController arena = LuciferSummoning.openArena(level, at, FREE_RADIUS, ArenaTheme.SAFEHOUSE);
        if (arena == null) {
            if (player != null) {
                Component why = LuciferSummoning.refusal(level);
                player.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.dagon.busy").withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        DagonEntity boss = BritishRegistry.DAGON.get().create(level);
        if (boss == null) return null;
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 180, 0);
        boss.bindArena(arena);
        level.addFreshEntity(boss);
        if (player != null) {
            arena.join(player);
            boss.witness(player);
        }
        boss.beginEmergence();
        return boss;
    }
}
