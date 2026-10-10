package org.papiricoh.supernaturalcraft.legacy.cases;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.author.CabinBuilder;
import org.papiricoh.supernaturalcraft.entity.legacy.ShapeshifterEntity;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.legacy.artifact.Artifacts;
import org.papiricoh.supernaturalcraft.legacy.research.FieldNotesItem;
import org.papiricoh.supernaturalcraft.network.LegacyFxPayload;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.List;
import java.util.UUID;

/**
 * Cases out in the world (v0.17). An open case comes alive when its hunter gets within {@link #REACH} blocks of the site: the
 * scenario's set piece is written ({@link CaseLayout}, through a private {@link ArenaController} so it can be put back) and the
 * creatures are put out. Killing every target solves it (field notes, sometimes a cursed artifact, the case filed as solved);
 * a hostage's death, or {@link #LOST_AFTER} ticks without the hunter near, loses it. Either way the set piece goes and the
 * creatures left vanish. The hunter's record is updated when they are online (at once, or as soon as they log in).
 */
public final class CaseSites {

    /** The site comes alive this close; the hunter counts as "near" this close. */
    public static final double REACH = 48, NEAR = 64;
    /** Ticks an active case survives without its hunter near: ten minutes. */
    public static final long LOST_AFTER = 12000;
    public static final int PIECE_RADIUS = 12;
    /** Persistent-data tags on a case's creatures (value = the site key) and on its hostage. */
    public static final String TAG = "sn_case", HOSTAGE_TAG = "sn_case_hostage";
    /** Chance a solved case also turns up a cursed artifact. */
    public static final double ARTIFACT_CHANCE = 0.30;

    private CaseSites() {
    }

    // --- every second ----------------------------------------------------------------------------------------------------

    /** Once a second in the overworld; {@code players} explicit so tests can pass their own. */
    public static void tick(ServerLevel level, List<? extends ServerPlayer> players) {
        CaseSavedData data = CaseSavedData.get(level);
        long now = level.getGameTime();
        for (ServerPlayer p : players) {
            if (p.level() != level || !p.isAlive() || p.isSpectator()) continue;
            CaseFile open = CaseOffice.open(Legacies.get(p));
            if (open != null && open.state() == CaseFile.OPEN && horizontal(p, open.site()) <= REACH
                    && level.hasChunkAt(open.site())) {
                materialize(level, p, open);
            }
        }
        for (CaseSavedData.Site s : data.snapshot()) {
            ServerPlayer owner = owner(level, s.owner);
            if (s.outcome != 0) {
                if (owner != null) settle(owner, s, data);
                continue;
            }
            if (owner != null && owner.level() == level && horizontal(owner, s.centre) <= NEAR) {
                s.lastNear = now;
                data.setDirty();
            }
            if (now - s.lastNear > LOST_AFTER) end(level, s, CaseFile.LOST);
        }
    }

    // --- coming alive ----------------------------------------------------------------------------------------------------

    /** Writes the case's set piece at its site and puts its creatures out; the case becomes ACTIVE. */
    public static @Nullable CaseSavedData.Site materialize(ServerLevel level, ServerPlayer owner, CaseFile file) {
        CaseSavedData data = CaseSavedData.get(level);
        if (data.get(owner.getUUID(), file.index()) != null) return data.get(owner.getUUID(), file.index());
        int x = file.site().getX(), z = file.site().getZ();
        level.getChunk(x >> 4, z >> 4);
        BlockPos centre = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        ArenaController piece = new ArenaController(UUID.randomUUID(), centre, PIECE_RADIUS);
        CaseSavedData.Site site = new CaseSavedData.Site(owner.getUUID(), file.index(), centre, piece);
        site.lastNear = level.getGameTime();
        data.put(site);
        for (CaseLayout.Cell c : CaseLayout.piece(file.scenario())) {
            piece.mutate(level, centre.offset(c.x(), c.y(), c.z()), CabinBuilder.state(c.state()), 0);
        }
        CaseGenerator.Plan plan = CaseGenerator.roll(level.getSeed(), owner.getUUID().getMostSignificantBits(),
                owner.getUUID().getLeastSignificantBits(), file.index(), file.tier(), 0, 1);
        String monster = file.monster().toString();
        String leader = !file.twist().equals("named_leader") ? ""
                : !plan.leader().isEmpty() ? plan.leader() : CaseGenerator.LEADERS.get(Math.floorMod(file.index(), CaseGenerator.LEADERS.size()));
        String extra = "";
        if (file.twist().equals("second_monster")) {
            extra = !plan.extra().isEmpty() && !plan.extra().equals(monster) ? plan.extra()
                    : CaseGenerator.MONSTERS.stream().map(CaseGenerator.Monster::id).filter(id -> !id.equals(monster)).findFirst().orElse("");
        }
        int count = CaseGenerator.count(monster, file.tier());
        if (!extra.isEmpty()) count = Math.min(count, CaseGenerator.MAX_COUNT - 1);
        int[][] lairs = CaseLayout.lairs(file.scenario());
        int n = 0;
        for (int i = 0; i < count; i++) {
            Mob m = spawn(level, file.monster(), centre.offset(lairs[n % lairs.length][0], 0, lairs[n % lairs.length][2]), site, file.tier(), owner);
            n++;
            if (m == null) continue;
            if (i == 0 && !leader.isEmpty()) {
                m.setCustomName(Component.literal(leader));
                m.setCustomNameVisible(true);
                scale(m, 1.6);
            }
            site.targets.add(m.getUUID());
        }
        if (!extra.isEmpty()) {
            Mob m = spawn(level, ResourceLocation.parse(extra), centre.offset(lairs[n % lairs.length][0], 0, lairs[n % lairs.length][2]), site, file.tier(), owner);
            if (m != null) site.targets.add(m.getUUID());
        }
        if (file.twist().equals("hostage")) {
            Villager v = EntityType.VILLAGER.create(level);
            if (v != null) {
                v.moveTo(centre.getX() + 0.5 - 2, centre.getY(), centre.getZ() + 0.5 + 3, 0, 0);
                v.setNoAi(true);
                v.setPersistenceRequired();
                v.setCustomName(Component.translatable("legacy.supernaturalcraft.case.hostage"));
                v.getPersistentData().putString(HOSTAGE_TAG, site.key());
                level.addFreshEntity(v);
                site.hostage = v.getUUID();
            }
        }
        if (site.targets.isEmpty()) {
            // Nothing could be put out (a monster that no longer exists): the case solves itself rather than hang.
            end(level, site, CaseFile.SOLVED);
            return site;
        }
        data.setDirty();
        BlockPos at = centre;
        Legacies.update(owner, l -> l.withCases(list -> {
            list.replaceAll(c -> c.index() == file.index() ? new CaseFile(c.index(), at, c.scenario(), c.monster(), c.twist(), c.tier(),
                    c.issued(), CaseFile.ACTIVE) : c);
            return list;
        }));
        owner.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.case.arrived").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        return site;
    }

    private static @Nullable Mob spawn(ServerLevel level, ResourceLocation id, BlockPos at, CaseSavedData.Site site, int tier, ServerPlayer owner) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null || !(type.create(level) instanceof Mob m)) return null;
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        m.setPersistenceRequired();
        m.getPersistentData().putString(TAG, site.key());
        scale(m, 1 + 0.35 * (tier - 1));
        if (m instanceof ShapeshifterEntity s) s.caseDisguise(owner, level.random);
        level.addFreshEntity(m);
        return m;
    }

    private static void scale(Mob m, double health) {
        var h = m.getAttribute(Attributes.MAX_HEALTH);
        if (h != null) {
            h.setBaseValue(h.getBaseValue() * health);
            m.setHealth(m.getMaxHealth());
        }
        var d = m.getAttribute(Attributes.ATTACK_DAMAGE);
        if (d != null) d.setBaseValue(d.getBaseValue() * (1 + (health - 1) * 0.5));
    }

    // --- deaths ----------------------------------------------------------------------------------------------------------

    /** A creature (or hostage) of a case died. */
    public static void onDeath(LivingEntity dead) {
        if (!(dead.level() instanceof ServerLevel level)) return;
        String key = dead.getPersistentData().getString(TAG);
        String hostage = dead.getPersistentData().getString(HOSTAGE_TAG);
        CaseSavedData data = CaseSavedData.get(level);
        if (!hostage.isEmpty()) {
            CaseSavedData.Site s = data.get(hostage);
            if (s != null && s.outcome == 0) end(level, s, CaseFile.LOST);
            return;
        }
        if (key.isEmpty()) return;
        CaseSavedData.Site s = data.get(key);
        if (s == null || s.outcome != 0) return;
        s.targets.remove(dead.getUUID());
        data.setDirty();
        if (s.targets.isEmpty()) end(level, s, CaseFile.SOLVED);
    }

    /** Whether a case creature (or hostage) belongs to a site still out: one whose case is over vanishes when it loads. */
    public static boolean orphan(Entity e) {
        if (!(e.level() instanceof ServerLevel level)) return false;
        String key = e.getPersistentData().getString(TAG);
        if (key.isEmpty()) key = e.getPersistentData().getString(HOSTAGE_TAG);
        if (key.isEmpty()) return false;
        CaseSavedData.Site s = CaseSavedData.get(level).get(key);
        return s == null || s.outcome != 0;
    }

    // --- the end ---------------------------------------------------------------------------------------------------------

    /** Ends a site: the set piece goes back, what is left of its creatures vanishes; the hunter hears of it when online. */
    public static void end(ServerLevel level, CaseSavedData.Site s, int outcome) {
        if (s.outcome != 0) return;
        s.outcome = outcome;
        s.piece.restoreNow(level);
        for (UUID u : s.targets) discard(level, u);
        if (s.hostage != null && outcome == CaseFile.SOLVED && level.getEntity(s.hostage) instanceof Villager v) {
            v.setNoAi(false);
            v.getPersistentData().remove(HOSTAGE_TAG);
            v.setCustomName(null);
        } else if (s.hostage != null) {
            discard(level, s.hostage);
        }
        CaseSavedData data = CaseSavedData.get(level);
        data.setDirty();
        ServerPlayer owner = owner(level, s.owner);
        if (owner != null) settle(owner, s, data);
    }

    /** A site's hunter if online (in the player list, or at least in the level: tests' players are only there). */
    static @Nullable ServerPlayer owner(ServerLevel level, UUID id) {
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
        if (p == null && level.getPlayerByUUID(id) instanceof ServerPlayer sp) p = sp;
        return p;
    }

    private static void discard(ServerLevel level, UUID id) {
        Entity e = level.getEntity(id);
        if (e != null) e.discard();
    }

    /** The hunter's side of an ended case: the record, the rewards, the news. */
    static void settle(ServerPlayer owner, CaseSavedData.Site s, CaseSavedData data) {
        data.remove(s);
        CaseFile file = CaseOffice.find(Legacies.get(owner), s.index);
        if (file == null || file.closed()) return;
        boolean solved = s.outcome == CaseFile.SOLVED;
        org.papiricoh.supernaturalcraft.memory.MemoryHooks.caseClosed(owner, file.index(), file.monster().toString(), file.scenario(), solved);
        Legacies.update(owner, l -> {
            var next = l.withCases(list -> {
                list.replaceAll(c -> c.index() == s.index ? c.withState(s.outcome) : c);
                return list;
            });
            return solved ? next.solvedOne() : next;
        });
        ServerLevel level = owner.serverLevel();
        if (solved) {
            reward(owner, file);
            level.playSound(null, owner.blockPosition(), AllSounds.LEGACY_RESEARCH_DONE.get(), SoundSource.PLAYERS, 1.0f, 1.1f);
        }
        owner.displayClientMessage(Component.translatable("message.supernaturalcraft.legacy.case." + (solved ? "solved" : "lost"),
                Component.translatable("legacy.supernaturalcraft.case.scenario." + file.scenario()))
                .withStyle(solved ? ChatFormatting.GOLD : ChatFormatting.GRAY), false);
        if (owner.connection != null) {
            PacketDistributor.sendToPlayer(owner, new LegacyFxPayload(LegacyFxPayload.CASE_CLOSED, owner.getId(), s.index, solved ? "solved" : "lost"));
        }
    }

    /** Field notes on the monster (and the place), experience, and now and then a cursed artifact. */
    public static void reward(ServerPlayer owner, CaseFile file) {
        give(owner, FieldNotesItem.stack(FieldNotesItem.creature(file.monster()), 2 + file.tier()));
        give(owner, FieldNotesItem.stack(FieldNotesItem.PLACE, 1));
        owner.giveExperiencePoints(10 * file.tier());
        long seed = owner.serverLevel().getSeed() ^ owner.getUUID().getLeastSignificantBits() * 31 ^ (long) file.index() * 0x9E3779B97F4A7C15L;
        if (new java.util.Random(seed).nextDouble() < ARTIFACT_CHANCE) give(owner, Artifacts.roll(seed, Math.max(0, Math.min(3, file.tier() - 1))));
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack) && !stack.isEmpty()) p.drop(stack, false);
    }

    private static double horizontal(ServerPlayer p, BlockPos at) {
        return Math.hypot(p.getX() - (at.getX() + 0.5), p.getZ() - (at.getZ() + 0.5));
    }
}
