package org.papiricoh.supernaturalcraft.entity.boss.ketch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.british.BritishRegistry;
import org.papiricoh.supernaturalcraft.british.BritishRules;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * Calling Arthur Ketch (v0.41). He hunts a hunter who turned Hess down (or was written off) by night, wherever they stand
 * ({@link #ambush}: a small arena round them, not exclusive). Beaten once, he comes back only by the rite ({@code summon_ketch},
 * anywhere, at night). By command, anywhere ({@link #summon}).
 */
public final class KetchSummoning {

    /** His ambush's arena; how far from the hunter he steps out. */
    public static final int AMBUSH_RADIUS = 18;
    public static final double STEP_OUT = 7;

    private KetchSummoning() {
    }

    /** Whether a Ketch is about anywhere near {@code p} already. */
    public static boolean near(ServerPlayer p) {
        return !p.serverLevel().getEntitiesOfClass(KetchEntity.class, p.getBoundingBox().inflate(64)).isEmpty();
    }

    /** He comes for {@code p}: a small arena round them (others' fights may go on). Null if he could not. */
    public static @Nullable KetchEntity ambush(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ArenaController arena = LuciferSummoning.openArena(level, p.blockPosition(), AMBUSH_RADIUS, ArenaTheme.AMBUSH, false);
        if (arena == null) return null;
        Vec3 behind = p.position().subtract(Vec3.directionFromRotation(0, p.getYRot()).scale(STEP_OUT));
        KetchEntity k = spawn(level, behind, p.position());
        if (k == null) {
            arena.beginRestore(false);
            return null;
        }
        k.bindArena(arena);
        arena.join(p);
        k.beginEmergence();
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.ketch.ambush").withStyle(ChatFormatting.DARK_RED), false);
        return k;
    }

    static @Nullable KetchEntity spawn(ServerLevel level, Vec3 at, Vec3 face) {
        KetchEntity k = BritishRegistry.KETCH.get().create(level);
        if (k == null) return null;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(at));
        double y = Math.abs(top.getY() - at.y) <= 3 ? top.getY() : at.y;
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - at.x), face.z - at.z));
        k.moveTo(at.x, y, at.z, yaw, 0);
        k.setYHeadRot(yaw);
        k.setYBodyRot(yaw);
        level.addFreshEntity(k);
        return k;
    }

    /** The rematch rite: anywhere in the Overworld, by night. @return whether he answers */
    public static boolean rite(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = riteRefusal(level, altar);
        if (why != null) {
            if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        return summon(level, altar.above(), ritualist) != null;
    }

    /** Why the rite fails here and now, or null. */
    public static @Nullable Component riteRefusal(ServerLevel level, BlockPos altar) {
        if (level.dimension() != Level.OVERWORLD || !BritishRules.night(WorldClock.day(level.getServer()))) {
            return Component.translatable("message.supernaturalcraft.ketch.rite.day");
        }
        return null;
    }

    /** Opens the fight at {@code at} for {@code player}; null if another fight holds the spot (or it cannot start). */
    public static @Nullable Entity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer player) {
        ArenaController arena = LuciferSummoning.openArena(level, at, AMBUSH_RADIUS, ArenaTheme.AMBUSH, false);
        if (arena == null) {
            if (player != null) {
                Component why = LuciferSummoning.refusal(level);
                player.displayClientMessage(why != null ? why : Component.translatable("message.supernaturalcraft.ketch.busy").withStyle(ChatFormatting.GRAY), true);
            }
            return null;
        }
        Vec3 c = Vec3.atBottomCenterOf(at);
        KetchEntity k = spawn(level, c.add(0, 0, 5), player != null ? player.position() : c);
        if (k == null) {
            arena.beginRestore(false);
            return null;
        }
        k.bindArena(arena);
        if (player != null) arena.join(player);
        k.beginEmergence();
        return k;
    }
}
