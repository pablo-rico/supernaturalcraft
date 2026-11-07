package org.papiricoh.supernaturalcraft.apocalypse.ruby;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.MessengerSchedule;
import org.papiricoh.supernaturalcraft.allegiance.power.Crossings;
import org.papiricoh.supernaturalcraft.arena.ArenaController;
import org.papiricoh.supernaturalcraft.arena.ArenaSavedData;
import org.papiricoh.supernaturalcraft.arena.ArenaTheme;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.entity.allegiance.MessengerEntity;
import org.papiricoh.supernaturalcraft.entity.boss.lucifer.LuciferSummoning;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.util.Players;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.UUID;

/**
 * Ruby's road on the server (v0.40): her visits (at dawn after Azazel; again once Alastair is dead), the reveal in Lilith's
 * arena as the last seal breaks, and, for a hunter who still owes her the fight (they lost or fled it, or beat Lilith before
 * v0.40 without her), a night ambush wherever she finds them, in a small arena of its own (not exclusive).
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class RubyFight {

    /** How often (ticks) her visits and ambushes are considered; the ambush's arena; midnight's window. */
    public static final int PERIOD = 100, AMBUSH_RADIUS = 16;
    public static final long NIGHT_START = 14_000, NIGHT_END = 22_000;

    private RubyFight() {
    }

    /** Whether anyone who fought in {@code arena} is still owed the reveal. */
    public static boolean owed(ServerLevel level, ArenaController arena) {
        for (UUID id : arena.participants()) {
            if (Players.byUuid(level, id) instanceof ServerPlayer p && RubyRules.owedReveal(Rubys.step(p))) return true;
        }
        return false;
    }

    /** Lilith has fallen in {@code arena}: Ruby steps out where she stood, and the arena is hers now. */
    public static @Nullable RubyEntity reveal(ServerLevel level, LivingEntity lilith, ArenaController arena) {
        int hunters = 0;
        for (UUID id : arena.participants()) {
            if (Players.byUuid(level, id) instanceof ServerPlayer p) {
                hunters++;
                if (RubyRules.owedReveal(Rubys.step(p))) Rubys.advance(p, RubyRules.Step.BETRAYAL_PENDING);
            }
        }
        Vec3 at = lilith.position();
        RubyEntity ruby = spawn(level, at, Math.max(1, hunters));
        if (ruby == null) {
            arena.beginRestore(true);
            return null;
        }
        ruby.bindArena(arena);
        return ruby;
    }

    public static @Nullable RubyEntity spawn(ServerLevel level, Vec3 at, int hunters) {
        RubyEntity ruby = AllEntities.RUBY.get().create(level);
        if (ruby == null) return null;
        ruby.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360, 0);
        ruby.tune(hunters);
        level.addFreshEntity(ruby);
        level.sendParticles(org.papiricoh.supernaturalcraft.registry.AllParticles.DEMON_SMOKE.get(), at.x, at.y + 1, at.z, 40, 0.4, 0.8, 0.4, 0.04);
        return ruby;
    }

    /** She comes for {@code p} by night: a small arena round them (others' fights may go on). */
    public static @Nullable RubyEntity ambush(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ArenaController arena = LuciferSummoning.openArena(level, p.blockPosition(), AMBUSH_RADIUS, ArenaTheme.SEAL, false);
        if (arena == null) return null;
        Vec3 ahead = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(6));
        RubyEntity ruby = spawn(level, ahead, 1);
        if (ruby == null) {
            arena.beginRestore(false);
            return null;
        }
        ruby.bindArena(arena);
        arena.join(p);
        ruby.setTarget(p);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.ruby.ambush").withStyle(ChatFormatting.DARK_RED), false);
        return ruby;
    }

    /** Whether a Ruby is about anywhere near {@code p} already. */
    static boolean near(ServerPlayer p) {
        return !p.serverLevel().getEntitiesOfClass(RubyEntity.class, p.getBoundingBox().inflate(64)).isEmpty();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        if (overworld.getGameTime() % PERIOD != 53) return;
        long clock = WorldClock.day(event.getServer());
        long day = clock / 24000L, time = Math.floorMod(clock, 24000L);
        for (ServerPlayer p : overworld.players()) {
            if (!p.isAlive() || p.isSpectator() || Crossings.inBattle(p)) continue;
            visits(p, day, time);
        }
    }

    static boolean free(ServerPlayer p) {
        return !RubyNpcEntity.visiting(p) && !MessengerEntity.visiting(p) && !org.papiricoh.supernaturalcraft.pact.CastielEntity.visiting(p)
                && !org.papiricoh.supernaturalcraft.pact.CrowleyKingEntity.visiting(p);
    }

    static void visits(ServerPlayer p, long day, long time) {
        if (!free(p)) return;
        RubyData d = Rubys.get(p);
        RubyRules.Step step = RubyRules.Step.of(d.step());
        if (step == RubyRules.Step.BETRAYAL_PENDING) {
            if (time >= NIGHT_START && time < NIGHT_END && RubyRules.visitDue(day, d.lastVisit()) && !near(p)) {
                Rubys.update(p, x -> x.withVisit(day));
                ambush(p);
            }
            return;
        }
        if (!MessengerSchedule.isDawn(time) || !RubyRules.visitDue(day, d.lastVisit())) return;
        boolean azazel = AuthorWorld.done(p, "main/yellow_eyed"), alastair = AuthorWorld.done(p, "main/on_the_head_of_a_pin"),
                lilith = AuthorWorld.done(p, "main/lucifer_rising");
        if (RubyRules.opens(step, Rubys.side(p), azazel, alastair, lilith) == null) return;
        Rubys.update(p, x -> x.withVisit(day));
        RubyNpcEntity.visit(p);
    }

    /** For a test or a command: the dawn has come and she is due ({@code ignoreRest}). */
    public static @Nullable RubyNpcEntity visitNow(ServerPlayer p) {
        return RubyNpcEntity.visit(p);
    }

    /** Unused by the game; the position of a warehouse-less ambush for previews. */
    static BlockPos ahead(ServerPlayer p) {
        return p.blockPosition().relative(p.getDirection(), 6);
    }
}
