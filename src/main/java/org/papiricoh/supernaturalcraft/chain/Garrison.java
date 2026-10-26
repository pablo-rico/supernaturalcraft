package org.papiricoh.supernaturalcraft.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.chain.seat.SeatWorld;
import org.papiricoh.supernaturalcraft.chain.seat.dungeon.DungeonAnchors;

import java.util.ArrayList;
import java.util.List;

/**
 * A stronghold's unnamed garrison: one of its side's rank and file ({@link ChainRoster}) at each guard post of the plan,
 * raised once per Lord when a hunter first comes near ({@code Court.garrisoned == Court.lords}); half of it while the throne
 * stands vacant. The fallen keep no stronghold, so the Monastery is manned by the Host. Guards are ordinary persistent
 * mobs that never join the chain; whatever a hunter kills stays dead until a new Lord is crowned.
 */
public final class Garrison {

    public static final String TAG = "sn_chain_garrison";
    /** How close a hunter must be to the stronghold's origin for the garrison to muster. */
    public static final int MUSTER = 96;
    /** How far a guard strays from its post. */
    public static final int LEASH = 10;

    private Garrison() {
    }

    static void raise(MinecraftServer server, ChainFaction f, SeatWorld.Anchors anchors) {
        var data = ChainSavedData.get(server);
        var court = data.court(f);
        if (court.garrisoned == court.lords) return;
        ServerLevel level = server.overworld();
        BlockPos o = anchors.origin();
        if (level.players().stream().noneMatch(p -> !p.isSpectator() && p.distanceToSqr(Vec3.atCenterOf(o)) < MUSTER * MUSTER)) return;
        List<BlockPos> all = new ArrayList<>(anchors.guards().values()), posts = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) if (!court.vacant() || i % 2 == 0) posts.add(all.get(i));
        int r = DungeonAnchors.RADIUS + 8;
        int standing = level.getEntitiesOfClass(Mob.class, new AABB(o).inflate(r, DungeonAnchors.HEIGHT, r),
                m -> m.isAlive() && m.getPersistentData().getBoolean(TAG)).size();
        var rng = ChainRules.rng(level.getSeed(), -70 - f.ordinal(), court.lords);
        for (int i = standing; i < posts.size(); i++) muster(level, f, posts.get(i), ChainRoster.draw(ChainRoster.pool(f, ChainFaction.Branch.LOYAL), rng));
        court.garrisoned = court.lords;
        data.changed();
    }

    private static void muster(ServerLevel level, ChainFaction f, BlockPos post, String type) {
        Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(type)).map(t -> t.create(level)).orElse(null);
        if (!(entity instanceof Mob mob)) return;
        mob.moveTo(Vec3.atBottomCenterOf(post), level.random.nextFloat() * 360, 0);
        if (!level.noCollision(mob)) return;
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(post), MobSpawnType.EVENT, null);
        mob.getPersistentData().putBoolean(TAG, true);
        mob.getPersistentData().putBoolean("sn_chain_excluded", true);
        mob.setPersistenceRequired();
        if (mob instanceof PathfinderMob walker) walker.restrictTo(post, LEASH);
        level.addFreshEntity(mob);
    }
}
