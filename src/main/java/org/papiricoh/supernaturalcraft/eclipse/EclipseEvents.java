package org.papiricoh.supernaturalcraft.eclipse;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.papiricoh.supernaturalcraft.SNConfig;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.weapon.Holy;

import java.util.List;

/** What changes under an eclipsed sun: more demons, the dead do not burn, holy strikes bite deeper. */
public final class EclipseEvents {

    public static final float HOLY_BONUS = 1.1f;
    /** Ids of undead that were not burning when their tick began. */
    private static final IntSet UNLIT = new IntOpenHashSet();

    private EclipseEvents() {
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Eclipses.tick(level);
        if (Eclipses.active(level) && level.getGameTime() % SNConfig.ECLIPSE_SPAWN_INTERVAL.get() == 0) spawnDemons(level);
    }

    /** Demons walk under the eclipse even by day: one near each player now and then, up to a cap. */
    static void spawnDemons(ServerLevel level) {
        if (!level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) return;
        RandomSource r = level.random;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) continue;
            int near = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32),
                    m -> m.getType().is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.DEMONS)).size();
            if (near >= SNConfig.ECLIPSE_DEMON_CAP.get()) continue;
            double angle = r.nextDouble() * Math.PI * 2, dist = 16 + r.nextDouble() * 12;
            int x = (int) (p.getX() + Math.cos(angle) * dist), z = (int) (p.getZ() + Math.sin(angle) * dist);
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.isLoaded(at) || Math.abs(at.getY() - p.getY()) > 16) continue;
            EntityType<? extends Mob> type = r.nextInt(4) == 0 ? AllEntities.DEMON_OCCULTIST.get() : AllEntities.BLACK_EYED_DEMON.get();
            type.spawn(level, at, MobSpawnType.EVENT);
        }
    }

    /** Natural night spawns lean three times harder towards demons. */
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.MONSTER || !Eclipses.active((net.minecraft.world.level.Level) event.getLevel())) return;
        List<MobSpawnSettings.SpawnerData> demons = event.getSpawnerDataList().stream()
                .filter(d -> d.type.is(org.papiricoh.supernaturalcraft.registry.AllTags.Entities.DEMONS)).toList();
        for (MobSpawnSettings.SpawnerData d : demons) {
            event.addSpawnerData(d);
            event.addSpawnerData(d);
        }
    }

    // Sunlight ignites the undead inside their own tick; whatever caught fire this tick out of a
    // clear sky, with no fire or lava about, was the sun, and the sun is out.
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide && mob.getType().is(EntityTypeTags.UNDEAD)
                && !mob.isOnFire() && Eclipses.active(mob.level())) {
            UNLIT.add(mob.getId());
        }
    }

    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide || !UNLIT.remove(mob.getId())) return;
        if (mob.isOnFire() && !mob.isInLava() && !mob.isInWall() && mob.level().getBlockStates(mob.getBoundingBox())
                .noneMatch(s -> s.is(net.minecraft.tags.BlockTags.FIRE) || s.is(net.minecraft.world.level.block.Blocks.LAVA))) {
            mob.clearFire();
        }
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (Holy.isHoly(event.getSource()) && !org.papiricoh.supernaturalcraft.entity.boss.BossDamage.isExact(event.getSource())
                && Eclipses.active(event.getEntity().level())) event.setAmount(event.getAmount() * HOLY_BONUS);
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Eclipses.sync(p);
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Eclipses.sync(p);
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Eclipses.sync(p);
    }
}
