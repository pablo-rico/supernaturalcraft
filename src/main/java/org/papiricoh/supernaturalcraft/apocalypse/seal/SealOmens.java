package org.papiricoh.supernaturalcraft.apocalypse.seal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the broken seals do to the world (v0.40), softer than an eclipse and growing with the stage
 * ({@link SealWorld#effectiveStage}): more demons at night near the hunters (by day too in the last stage), now and then a
 * villager possessed (once a day near each hunter at most), and the plagues of the stage (blood on the water, a withered field;
 * all in the ledger, all turned back). The sky's omens are the client's ({@code SealSky}); the angels' war opens with the second
 * stage ({@link SealWorld#openAngels}). Nothing in an echo but the sky, faintly.
 */
public final class SealOmens {

    /** How often (ticks) the omens are rolled for each hunter. */
    public static final int EVERY = 200;
    /** Chance per roll and stage of a demon near a hunter; the cap of demons round them (plus the stage). */
    public static final float DEMON_CHANCE = 0.05f;
    public static final int DEMON_CAP = 2;
    /** Chance per roll of a possession (from the second stage), and of a plague (from the second stage). */
    public static final float POSSESSION_CHANCE = 0.015f, PLAGUE_CHANCE = 0.02f;

    /** The day each hunter last saw a villager possessed near them. */
    private static final Map<UUID, Long> POSSESSED = new ConcurrentHashMap<>();

    private SealOmens() {
    }

    static void forget(UUID id) {
        POSSESSED.remove(id);
    }

    static void tick(ServerLevel level, SealSavedData data) {
        if (data.echo() || !data.started()) return;
        int stage = SealRules.stage(data.broken(), data.risen());
        if (stage <= 0 || level.getDifficulty() == Difficulty.PEACEFUL) return;
        boolean spawning = level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        RandomSource r = level.random;
        long day = SealRules.dayOf(WorldClock.day(level.getServer()));
        for (ServerPlayer p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || ArenaSavedData.get(level).at(p.position()) != null) continue;
            if (spawning && r.nextFloat() < DEMON_CHANCE * stage) demon(level, p, stage, r);
            if (stage >= 2 && r.nextFloat() < POSSESSION_CHANCE && !Long.valueOf(day).equals(POSSESSED.get(p.getUUID()))) {
                if (possess(level, p)) POSSESSED.put(p.getUUID(), day);
            }
            if (stage >= 2 && r.nextFloat() < PLAGUE_CHANCE && !data.ledger().full()) {
                double a = r.nextDouble() * Math.PI * 2, d = 24 + r.nextDouble() * 16;
                BlockPos at = SealStaging.ground(level, (int) (p.getX() + Math.cos(a) * d), (int) (p.getZ() + Math.sin(a) * d));
                if (at != null) SealPlagues.ambient(level, at, r);
            }
        }
    }

    /** A demon out of the dark 16-28 blocks from {@code p} (in daylight only in the last stage), up to a cap round them. */
    static void demon(ServerLevel level, ServerPlayer p, int stage, RandomSource r) {
        int near = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getType().is(AllTags.Entities.DEMONS)).size();
        if (near >= DEMON_CAP + stage) return;
        double a = r.nextDouble() * Math.PI * 2, d = 16 + r.nextDouble() * 12;
        BlockPos at = SealStaging.ground(level, (int) (p.getX() + Math.cos(a) * d), (int) (p.getZ() + Math.sin(a) * d));
        if (at == null || !level.isLoaded(at) || Math.abs(at.getY() - p.getY()) > 16) return;
        if (ArenaSavedData.get(level).at(Vec3.atCenterOf(at)) != null) return;
        if (org.papiricoh.supernaturalcraft.coven.church.Sanctuaries.get(level).inside(Vec3.atCenterOf(at))) return;
        boolean dark = level.getBrightness(LightLayer.SKY, at) - level.getSkyDarken() < 8 && level.getBrightness(LightLayer.BLOCK, at) < 8;
        if (!dark && stage < SealRules.DAYLIGHT_DEMON_STAGE) return;
        EntityType<? extends Mob> type = r.nextInt(5) == 0 ? AllEntities.DEMON_OCCULTIST.get() : AllEntities.BLACK_EYED_DEMON.get();
        type.spawn(level, at, MobSpawnType.EVENT);
    }

    /** A villager within 32 blocks of {@code p} (no name, no seal's, not bound) is taken by a demon. @return whether one was */
    static boolean possess(ServerLevel level, ServerPlayer p) {
        for (Villager v : level.getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(32),
                v -> v.isAlive() && !v.hasCustomName() && !v.isNoAi() && !v.isBaby() && !v.getPersistentData().contains(SealEvent.TAG))) {
            Mob demon = AllEntities.BLACK_EYED_DEMON.get().create(level);
            if (demon == null) return false;
            demon.moveTo(v.getX(), v.getY(), v.getZ(), v.getYRot(), v.getXRot());
            demon.finalizeSpawn(level, level.getCurrentDifficultyAt(v.blockPosition()), MobSpawnType.CONVERSION, null);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, v.getX(), v.getY() + 1, v.getZ(), 30, 0.3, 0.8, 0.3, 0.04);
            v.discard();
            level.addFreshEntity(demon);
            return true;
        }
        return false;
    }
}
