package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.pact.LabSite;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.pact.PactSavedData;
import org.papiricoh.supernaturalcraft.pact.PactWorld;
import org.papiricoh.supernaturalcraft.pact.Pacts;
import org.papiricoh.supernaturalcraft.pact.layout.LabAnchors;
import org.papiricoh.supernaturalcraft.registry.AllEntities;

/**
 * How Godstiel's fight begins (v0.39).
 * <ul>
 *   <li><b>The door</b> ({@link #doorRefusal}, {@link DoorScene}): the rite {@code open_purgatory_door} at the altar in the
 *   middle of Crowley's lab, by a hunter whose Alphas are down. The door opens, Castiel takes the souls and turns on Crowley,
 *   and the fight begins ({@link #begin}).</li>
 *   <li><b>A rematch</b> ({@link #rite}), by the rite {@code summon_godstiel} in the same hall, once he has fallen: the same
 *   fight with no talk at the end.</li>
 *   <li><b>Anywhere</b> ({@link #summon}): commands and tests, a plain arena round the spot.</li>
 * </ul>
 */
public final class GodstielSummoning {

    /** The arena's radius round a free summoning. */
    public static final int FREE_RADIUS = 22;
    /** How close to the lab's altar spot the rites must be worked. */
    public static final int RITE_REACH = 3;

    private GodstielSummoning() {
    }

    /** Whether a Godstiel fight (or the door's scene) is on anywhere. */
    public static boolean fighting(MinecraftServer server) {
        if (DoorScene.active()) return true;
        for (ArenaController a : ArenaSavedData.get(server.overworld()).all()) if (a.isActive() && a.theme() == ArenaTheme.LAB) return true;
        return false;
    }

    /** Why the door's rite fails here and now, or null: not the lab's altar, the Alphas not down, already opened, a fight on. */
    public static @Nullable Component doorRefusal(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        LabSite site = PactWorld.site(level.getServer());
        if (site == null || !PactSavedData.get(level.getServer()).labBuilt() || level.dimension() != Level.OVERWORLD || !site.atRite(altar, RITE_REACH)) {
            return Component.translatable("message.supernaturalcraft.pact.door.elsewhere");
        }
        if (ritualist != null) {
            PactRules.Step step = Pacts.step(ritualist);
            if (step.atLeast(PactRules.Step.BETRAYED)) return Component.translatable("message.supernaturalcraft.pact.door.opened");
            if (step != PactRules.Step.RITE_READY) return Component.translatable("message.supernaturalcraft.pact.door.not_ready");
        }
        if (fighting(level.getServer())) return Component.translatable("message.supernaturalcraft.pact.door.busy");
        return LuciferSummoning.refusal(level);
    }

    /** The door's rite: the scene starts (the fight follows it). @return whether it began */
    public static boolean door(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = doorRefusal(level, altar, ritualist);
        if (why != null) {
            if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        LabSite site = PactWorld.site(level.getServer());
        return site != null && DoorScene.start(level, site, ritualist);
    }

    /** The fight at the lab: {@code opener} worked the rite, {@code at} where he stands up from (the rite's spot, or the hall). */
    public static @Nullable GodstielEntity begin(ServerLevel level, @Nullable ServerPlayer opener, LabSite site, Vec3 at, boolean rematch) {
        Component busy = LuciferSummoning.refusal(level);
        if (busy != null) {
            if (opener != null) opener.displayClientMessage(busy, true);
            return null;
        }
        BlockPos centre = site.point(LabAnchors.ARENA_CENTRE);
        ArenaController arena = LuciferSummoning.openArena(level, centre != null ? centre : site.origin(), LabAnchors.ARENA_RADIUS, ArenaTheme.LAB);
        if (arena == null) return null;
        GodstielEntity boss = AllEntities.GODSTIEL.get().create(level);
        if (boss == null) return null;
        Vec3 face = opener != null ? opener.position() : site.floor(LabAnchors.ENTRY);
        float yaw = (float) Math.toDegrees(Math.atan2(-(face.x - at.x), face.z - at.z));
        boss.moveTo(at.x, at.y, at.z, yaw, 0);
        boss.setYHeadRot(yaw);
        boss.setYBodyRot(yaw);
        boss.bindLab(site);
        boss.opener(opener != null ? opener.getUUID() : null);
        boss.rematch(rematch);
        boss.bindArena(arena);
        if (rematch) PactSavedData.get(level.getServer()).rematched();
        level.addFreshEntity(boss);
        boss.barDoors(level, arena);
        boss.beginEmergence();
        return boss;
    }

    /** The rematch rite, in the lab's hall. @return whether he answers */
    public static boolean rite(ServerLevel level, BlockPos altar, @Nullable ServerPlayer ritualist) {
        Component why = riteRefusal(level, altar);
        if (why != null) {
            if (ritualist != null) ritualist.displayClientMessage(why.copy().withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        LabSite site = PactWorld.site(level.getServer());
        return site != null && begin(level, ritualist, site, site.floor(LabAnchors.HALL), true) != null;
    }

    /** Why the rematch fails here and now, or null: not at the lab's altar spot, or a fight on. */
    public static @Nullable Component riteRefusal(ServerLevel level, BlockPos altar) {
        LabSite site = PactWorld.site(level.getServer());
        if (site == null || level.dimension() != Level.OVERWORLD || !site.atRite(altar, RITE_REACH)) {
            return Component.translatable("message.supernaturalcraft.godstiel.rite.elsewhere");
        }
        if (fighting(level.getServer())) return Component.translatable("message.supernaturalcraft.pact.door.busy");
        return LuciferSummoning.refusal(level);
    }

    /** Anywhere (commands, tests): a plain arena round {@code at}. */
    public static @Nullable GodstielEntity summon(ServerLevel level, BlockPos at, @Nullable ServerPlayer facing) {
        ArenaController arena = LuciferSummoning.openArena(level, at, FREE_RADIUS, ArenaTheme.LAB);
        if (arena == null) {
            if (facing != null) {
                Component why = LuciferSummoning.refusal(level);
                if (why != null) facing.displayClientMessage(why, true);
            }
            return null;
        }
        GodstielEntity boss = AllEntities.GODSTIEL.get().create(level);
        if (boss == null) return null;
        boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        boss.bindArena(arena);
        boss.opener(facing != null ? facing.getUUID() : null);
        // Summoned by hand by someone who has beaten him: a rematch (no talk).
        boss.rematch(facing != null && AuthorWorld.done(facing, BossProgression.Boss.GODSTIEL.advancement));
        level.addFreshEntity(boss);
        boss.beginEmergence();
        return boss;
    }
}
