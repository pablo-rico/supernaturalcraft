package org.papiricoh.supernaturalcraft.entity.alpha;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.entity.legacy.WerewolfEntity;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.pact.Pacts;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * Where and when the Alphas come (v0.39, {@link AlphaRules}): at night in the Overworld, now and then, for a hunter on Crowley's
 * errand ({@link Pacts#huntingAlpha}), one at a time, a little way off in the dark, with a few of its kind.
 */
public final class AlphaSpawns {

    private AlphaSpawns() {
    }

    /** Every {@link AlphaRules#CHECK_TICKS}: each hunter on the errand may be found by one of the Alphas still out there. */
    public static void tick(MinecraftServer server) {
        ServerLevel level = server.overworld();
        if (level.getGameTime() % AlphaRules.CHECK_TICKS != 11) return;
        boolean night = !level.isDay();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            List<PactRules.Alpha> hunting = hunting(p);
            if (hunting.isEmpty()) continue;
            if (!AlphaRules.comes(level.random.nextDouble(), night, alive(level, p))) continue;
            PactRules.Alpha a = AlphaRules.pick(hunting, level.random.nextDouble());
            Vec3 at = spot(level, p);
            if (a != null && at != null) come(level, a, at, p);
        }
    }

    /** The Alphas that may still come for {@code p}. */
    public static List<PactRules.Alpha> hunting(ServerPlayer p) {
        List<PactRules.Alpha> out = new ArrayList<>();
        for (PactRules.Alpha a : PactRules.Alpha.values()) if (Pacts.huntingAlpha(p, a)) out.add(a);
        return out;
    }

    /** How many Alphas are alive for {@code p} (in this level). */
    public static int alive(ServerLevel level, ServerPlayer p) {
        int n = 0;
        for (var e : level.getAllEntities()) {
            if (e instanceof Alpha a && e.isAlive() && p.getUUID().equals(a.quarry())) n++;
        }
        return n;
    }

    public static EntityType<? extends Monster> type(PactRules.Alpha a) {
        return switch (a) {
            case VAMPIRE -> AllEntities.ALPHA_VAMPIRE.get();
            case WEREWOLF -> AllEntities.ALPHA_WEREWOLF.get();
            case SHAPESHIFTER -> AllEntities.ALPHA_SHAPESHIFTER.get();
        };
    }

    private static EntityType<? extends Monster> kin(PactRules.Alpha a) {
        return switch (a) {
            case VAMPIRE -> AllEntities.VAMPIRE.get();
            case WEREWOLF -> AllEntities.WEREWOLF.get();
            case SHAPESHIFTER -> AllEntities.SHAPESHIFTER.get();
        };
    }

    /** A spot in the dark between {@link AlphaRules#MIN_DISTANCE} and {@link AlphaRules#MAX_DISTANCE} of {@code p}, or null. */
    public static @Nullable Vec3 spot(ServerLevel level, ServerPlayer p) {
        for (int i = 0; i < 8; i++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double d = AlphaRules.MIN_DISTANCE + level.random.nextDouble() * (AlphaRules.MAX_DISTANCE - AlphaRules.MIN_DISTANCE);
            Vec3 at = ground(level, p.position().add(Math.cos(a) * d, 0, Math.sin(a) * d));
            if (at != null) return at;
        }
        return null;
    }

    /**
     * Standing ground in {@code near}'s column if its chunk is loaded, else null: first near {@code near}'s own height (under
     * the trees, in a cave, under a roof), then the top of the column.
     */
    public static @Nullable Vec3 ground(ServerLevel level, Vec3 near) {
        BlockPos col = BlockPos.containing(near);
        if (!level.hasChunkAt(col)) return null;
        for (int dy = 6; dy >= -10; dy--) {
            BlockPos q = col.above(dy);
            if (standable(level, q)) return Vec3.atBottomCenterOf(q);
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
        if (!level.getBlockState(top.below()).isFaceSturdy(level, top.below(), Direction.UP) || !level.getFluidState(top.below()).isEmpty()) {
            // Under the canopy or over water: try the ground under where it stands.
            BlockPos at = col.atY(Math.min(top.getY(), (int) Math.floor(near.y) + 4));
            for (int dy = 0; dy < 10; dy++) {
                BlockPos q = at.below(dy);
                if (level.getBlockState(q.below()).isFaceSturdy(level, q.below(), Direction.UP) && level.getBlockState(q).isAir()
                        && level.getBlockState(q.above()).isAir() && level.getFluidState(q.below()).isEmpty()) {
                    return Vec3.atBottomCenterOf(q);
                }
            }
            return null;
        }
        return Vec3.atBottomCenterOf(top);
    }

    private static boolean standable(ServerLevel level, BlockPos q) {
        return level.getBlockState(q.below()).isFaceSturdy(level, q.below(), Direction.UP) && level.getFluidState(q.below()).isEmpty()
                && !level.getBlockState(q.below()).is(net.minecraft.world.level.block.Blocks.BARRIER)
                && level.getBlockState(q).getCollisionShape(level, q).isEmpty() && level.getFluidState(q).isEmpty()
                && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty();
    }

    /** Alpha {@code a} comes out of the dark at {@code at} for {@code quarry}, with a few of its kind. @return the Alpha */
    public static @Nullable Mob come(ServerLevel level, PactRules.Alpha a, Vec3 at, @Nullable ServerPlayer quarry) {
        Mob alpha = type(a).create(level);
        if (alpha == null) return null;
        alpha.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        alpha.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        alpha.setPersistenceRequired();
        if (alpha instanceof Alpha x && quarry != null) x.core().setQuarry(quarry.getUUID());
        if (quarry != null) alpha.setTarget(quarry);
        level.addFreshEntity(alpha);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 30, 0.4, 1.0, 0.4, 0.02);
        int pack = AlphaRules.pack(a, level.random.nextDouble());
        for (int i = 0; i < pack; i++) {
            Mob m = kin(a).create(level);
            if (m == null) continue;
            double ang = Math.PI * 2 * i / pack, r = 2.5 + level.random.nextDouble() * 1.5;
            Vec3 p = ground(level, at.add(Math.cos(ang) * r, 0, Math.sin(ang) * r));
            if (p == null) p = at;
            m.moveTo(p.x, p.y, p.z, level.random.nextFloat() * 360, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(p)), MobSpawnType.EVENT, null);
            if (m instanceof WerewolfEntity w) w.forceForm(true);
            if (m instanceof org.papiricoh.supernaturalcraft.entity.legacy.ShapeshifterEntity s) s.setDisguise("");
            if (quarry != null) m.setTarget(quarry);
            level.addFreshEntity(m);
        }
        if (quarry != null) AlphaCore.herald(quarry, a);
        return alpha;
    }

    /** Whether {@code level} is where the Alphas walk (the Overworld). */
    public static boolean walksIn(Level level) {
        return level.dimension().equals(Level.OVERWORLD);
    }
}
