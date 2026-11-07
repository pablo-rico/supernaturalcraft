package org.papiricoh.supernaturalcraft.apocworld.camp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.apocworld.ApocalypseWorld;
import org.papiricoh.supernaturalcraft.apocworld.AUStanding;
import org.papiricoh.supernaturalcraft.apocworld.army.AuArmyRules;
import org.papiricoh.supernaturalcraft.apocworld.army.AuPrisonerEntity;
import org.papiricoh.supernaturalcraft.apocworld.army.AuSoldierEntity;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Freeing Michael's prisoners (v0.41). A cage's lock holds while any guard of its camp stands ({@link #guarded}); broken, the
 * prisoners in that cage go free ({@link #unlocked}). When the last cage of a camp is open, the camp is freed: once per camp per
 * world, every hunter near shares it ({@link AUStanding#campsFreed}), and the first camp a hunter frees grants
 * {@value #WAR_OF_THE_WORLDS}.
 */
public final class Camps {

    public static final String WAR_OF_THE_WORLDS = "main/war_of_the_worlds";

    private Camps() {
    }

    /** The camp the cage at {@code lock} belongs to: the heart its prisoners name, or null (no caged prisoner by it). */
    public static @Nullable BlockPos campAt(Level level, BlockPos lock) {
        for (AuPrisonerEntity p : caged(level, Vec3.atCenterOf(lock), AuArmyRules.CAGE_RANGE)) {
            if (p.camp() != null) return p.camp();
        }
        return null;
    }

    private static List<AuPrisonerEntity> caged(Level level, Vec3 at, double range) {
        return level.getEntitiesOfClass(AuPrisonerEntity.class, new AABB(at, at).inflate(range), p -> p.isAlive() && !p.freed());
    }

    /** The guards of the camp at {@code camp} still standing. */
    public static List<AuSoldierEntity> guards(Level level, BlockPos camp) {
        Vec3 c = Vec3.atCenterOf(camp);
        return level.getEntitiesOfClass(AuSoldierEntity.class, new AABB(c, c).inflate(AuArmyRules.CAMP_GUARD_RANGE, 16, AuArmyRules.CAMP_GUARD_RANGE),
                s -> s.isAlive() && camp.equals(s.camp()));
    }

    /** Whether the lock at {@code lock} still holds: its camp's guards are up. */
    public static boolean guarded(Level level, BlockPos lock) {
        BlockPos camp = campAt(level, lock);
        return camp != null && !guards(level, camp).isEmpty();
    }

    /** The lock at {@code lock} has been broken (by {@code by}, or nobody): its cage's prisoners go free. @return how many */
    public static int unlocked(ServerLevel level, BlockPos lock, @Nullable ServerPlayer by) {
        BlockPos camp = campAt(level, lock);
        int n = 0;
        for (AuPrisonerEntity p : caged(level, Vec3.atCenterOf(lock), AuArmyRules.CAGE_RANGE)) {
            if (camp != null && !camp.equals(p.camp())) continue;
            p.free();
            n++;
        }
        if (n > 0 && by != null) {
            by.displayClientMessage(Component.translatable("message.supernaturalcraft.apocworld.cage_open").withStyle(ChatFormatting.GOLD), true);
        }
        if (camp != null && caged(level, Vec3.atCenterOf(camp), AuArmyRules.CAMP_RANGE).stream().noneMatch(p -> camp.equals(p.camp()))) {
            freed(level, camp, by);
        }
        return n;
    }

    /** Every cage of the camp at {@code camp} is open: it counts for the hunters near (once per camp per world). */
    public static void freed(ServerLevel level, BlockPos camp, @Nullable ServerPlayer by) {
        if (!Freed.get(level).add(camp)) return;
        Set<ServerPlayer> hunters = new LinkedHashSet<>();
        if (by != null) hunters.add(by);
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(Vec3.atCenterOf(camp)) < AuArmyRules.CREDIT_RANGE * AuArmyRules.CREDIT_RANGE) hunters.add(p);
        }
        for (ServerPlayer p : hunters) {
            ApocalypseWorld.update(p, AUStanding::withCampFreed);
            ChorusRewards.award(p, WAR_OF_THE_WORLDS);
            p.sendSystemMessage(Component.translatable("message.supernaturalcraft.apocworld.camp_freed").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Whether the camp at {@code camp} has been freed in {@code level}. */
    public static boolean isFreed(ServerLevel level, BlockPos camp) {
        return Freed.get(level).has(camp);
    }

    /** The camps freed in one dimension (each counts once). */
    public static final class Freed extends SavedData {
        private static final String NAME = "supernaturalcraft_au_camps";
        private final Set<Long> camps = new HashSet<>();

        public static Freed get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new Factory<>(Freed::new, Freed::load, null), NAME);
        }

        private static Freed load(CompoundTag tag, HolderLookup.Provider registries) {
            Freed f = new Freed();
            for (long l : tag.getLongArray("Camps")) f.camps.add(l);
            return f;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.put("Camps", new LongArrayTag(new ArrayList<>(camps)));
            return tag;
        }

        boolean add(BlockPos camp) {
            boolean added = camps.add(camp.asLong());
            if (added) setDirty();
            return added;
        }

        boolean has(BlockPos camp) {
            return camps.contains(camp.asLong());
        }

        /** Forgets every freed camp (tests). */
        public void clear() {
            camps.clear();
            setDirty();
        }
    }
}
