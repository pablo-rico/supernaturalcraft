package org.papiricoh.supernaturalcraft.prophecy.trials;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.prophecy.Prophecies;
import org.papiricoh.supernaturalcraft.prophecy.Prophecy;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllParticles;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.List;

/**
 * The Trials on the server (v0.36; numbers and order in {@link TrialRules}). The Demon Tablet's last chapter opens them;
 * {@code prophecy/WordEvents} feeds them the hellhound's blood, the soul brought out of Hell and the injections, and keeps
 * their toll on the hunter's hearts ({@link #applyToll}) and the weakness while one is under way.
 */
public final class Trials {

    public static final String DIALOGUE = "trials_end";
    public static final List<String> OPTIONS = List.of("complete", "stop");
    public static final ResourceLocation TOLL = SupernaturalCraft.asResource("trials_toll");
    /** On a demon being cured: injections given, and when the last one went in. */
    static final String CURE_COUNT = "SNTrialCure", CURE_TIME = "SNTrialCureAt";

    private Trials() {
    }

    public static void registerDialogue() {
        AllegianceDialogue.register(DIALOGUE, Trials::answer);
    }

    /** The Demon Tablet read to its end: the first trial begins (again, if they were given up). */
    public static void offer(ServerPlayer sp) {
        int trial = Prophecies.get(sp).trial();
        if (TrialRules.underway(trial) || trial == TrialRules.DONE) return;
        begin(sp, TrialRules.HOUND);
    }

    static void begin(ServerPlayer sp, int trial) {
        Prophecies.update(sp, p -> p.withTrial(trial, 0, WorldClock.day(sp.server)));
        applyToll(sp);
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.trials.begin." + trial).withStyle(ChatFormatting.DARK_RED), false);
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.4f, 0.6f);
    }

    /** The trial under way is done: the next one begins. */
    public static void advance(ServerPlayer sp, int done) {
        if (Prophecies.get(sp).trial() != done) return;
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.trials.done." + done).withStyle(ChatFormatting.GOLD), false);
        begin(sp, TrialRules.next(done));
    }

    /** Keeps the hearts the Trials take, and the weakness while one is under way. Called now and then. */
    public static void applyToll(ServerPlayer sp) {
        Prophecy p = Prophecies.get(sp);
        if (p.trial() == TrialRules.STOPPED) {
            long today = WorldClock.day(sp.server);
            int owed = TrialRules.owedAfter(p.trialStep(), today - p.trialDay());
            if (owed != p.trialStep()) p = Prophecies.update(sp, q -> q.withTrial(TrialRules.STOPPED, owed, today));
        }
        int hearts = TrialRules.toll(p.trial(), p.trialStep());
        AttributeInstance max = sp.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) {
            AttributeModifier current = max.getModifier(TOLL);
            double want = -2.0 * hearts;
            if (hearts == 0) {
                if (current != null) max.removeModifier(TOLL);
            } else if (current == null || current.amount() != want) {
                max.addOrUpdateTransientModifier(new AttributeModifier(TOLL, want, AttributeModifier.Operation.ADD_VALUE));
                if (sp.getHealth() > sp.getMaxHealth()) sp.setHealth(sp.getMaxHealth());
            }
        }
        if (TrialRules.underway(p.trial())) sp.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false, true));
    }

    /** An injection of purified blood into a trapped demon, on the third trial. @return whether it was given */
    public static boolean inject(ServerPlayer sp, Mob demon, ItemStack blood) {
        if (Prophecies.get(sp).trial() != TrialRules.CURE || !Kin.isDemon(demon) || demon.getType().is(AllTags.Entities.BOSSES)) return false;
        if (!demon.hasEffect(AllMobEffects.TRAPPED)) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.trials.cure.hold").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        var data = demon.getPersistentData();
        int count = data.getInt(CURE_COUNT);
        long now = sp.serverLevel().getGameTime();
        if (!TrialRules.mayInject(count, now - data.getLong(CURE_TIME))) {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.trials.cure.wait").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        demon.setPersistenceRequired();
        count++;
        data.putInt(CURE_COUNT, count);
        data.putLong(CURE_TIME, now);
        if (!sp.getAbilities().instabuild) blood.shrink(1);
        sp.serverLevel().sendParticles(AllParticles.GRACE.get(), demon.getX(), demon.getY() + 1, demon.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        sp.serverLevel().playSound(null, demon.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 1f, 0.7f);
        if (TrialRules.last(count)) {
            AllegianceDialogue.ask(sp, demon, DIALOGUE, "choice", OPTIONS, 600);
        } else {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.trials.cure.step", count, TrialRules.INJECTIONS)
                    .withStyle(ChatFormatting.GOLD), true);
        }
        return true;
    }

    private static void answer(ServerPlayer sp, @Nullable Entity speaker, String node, String option) {
        if (Prophecies.get(sp).trial() != TrialRules.CURE) return;
        if (!"complete".equals(option)) {
            int owed = TrialRules.owedOnStop(TrialRules.CURE);
            Prophecies.update(sp, p -> p.withTrial(TrialRules.STOPPED, owed, WorldClock.day(sp.server)));
            applyToll(sp);
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.trials.stopped").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        Prophecies.update(sp, p -> p.withTrial(TrialRules.DONE, 0, WorldClock.day(sp.server)));
        applyToll(sp);
        ServerLevel level = sp.serverLevel();
        // The cured demon is a man again.
        if (speaker instanceof Mob demon && demon.isAlive()) {
            Villager man = EntityType.VILLAGER.create(level);
            if (man != null) {
                man.moveTo(demon.getX(), demon.getY(), demon.getZ(), demon.getYRot(), 0);
                man.setPersistenceRequired();
                level.addFreshEntity(man);
            }
            demon.discard();
        }
        seal(level, sp);
        ChorusRewards.award(sp, "main/gates_sealed");
        sp.displayClientMessage(Component.translatable("message.supernaturalcraft.trials.sealed", TrialRules.SEAL_RADIUS)
                .withStyle(ChatFormatting.GOLD), false);
    }

    /** The seal where {@code by} stands: every demon on its ground (never a boss) is sent back below. */
    static void seal(ServerLevel level, ServerPlayer by) {
        GateSeals.get(level).add(by.blockPosition());
        int r = TrialRules.SEAL_RADIUS;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(by.blockPosition()).inflate(r, 64, r),
                e -> e instanceof Mob && Kin.isDemon(e) && !e.getType().is(AllTags.Entities.BOSSES)
                        && TrialRules.sealed(e.getX() - by.getX(), e.getZ() - by.getZ()))) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 1, e.getZ(), 30, 0.3, 0.8, 0.3, 0.05);
            e.discard();
        }
        level.sendParticles(AllParticles.GRACE.get(), by.getX(), by.getY() + 1, by.getZ(), 200, 3, 2, 3, 0.2);
        level.playSound(null, by.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1f, 1.2f);
    }
}
