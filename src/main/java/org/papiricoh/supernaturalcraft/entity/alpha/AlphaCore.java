package org.papiricoh.supernaturalcraft.entity.alpha;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.network.EveFxPayload;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.pact.Pacts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the three Alphas share (v0.39), kept by each one's own entity class: a plain vanilla boss bar (they are no great enemies:
 * not in {@code #bosses}, no health scaling), the hunter each came for, its signature moves' cooldowns, going back into the dark by
 * day, and Crowley's errand when it falls ({@link Pacts#alphaSlain} for its killer and every hunter near who was after it).
 */
public final class AlphaCore {

    private final PactRules.Alpha alpha;
    private final ServerBossEvent bar;
    private final Map<String, Long> ready = new HashMap<>();
    private final List<ServerPlayer> tracked = new ArrayList<>();
    private @Nullable UUID quarry;
    private int lonely;
    private boolean credited;

    public AlphaCore(PactRules.Alpha alpha) {
        this.alpha = alpha;
        this.bar = new ServerBossEvent(Component.translatable("entity.supernaturalcraft." + alpha.entity()), color(alpha),
                BossEvent.BossBarOverlay.NOTCHED_6);
        bar.setDarkenScreen(false);
    }

    private static BossEvent.BossBarColor color(PactRules.Alpha a) {
        return switch (a) {
            case VAMPIRE -> BossEvent.BossBarColor.RED;
            case WEREWOLF -> BossEvent.BossBarColor.WHITE;
            case SHAPESHIFTER -> BossEvent.BossBarColor.PINK;
        };
    }

    public PactRules.Alpha alpha() {
        return alpha;
    }

    public @Nullable UUID quarry() {
        return quarry;
    }

    public void setQuarry(@Nullable UUID quarry) {
        this.quarry = quarry;
    }

    public ServerBossEvent bar() {
        return bar;
    }

    /** Whether {@code move} is off cooldown at {@code now}. */
    public boolean ready(String move, long now) {
        return now >= ready.getOrDefault(move, 0L);
    }

    /** {@code move} is used at {@code now}: ready again after {@code cooldown}. */
    public void use(String move, long now, int cooldown) {
        ready.put(move, now + cooldown);
    }

    /**
     * A blow as it lands on the Alpha: scaled by {@link AlphaRules#damageScale} (an ascended blade hits for hundreds), save what
     * nothing escapes ({@code /kill}, the void).
     */
    public float scaled(DamageSource source, float amount) {
        if (org.papiricoh.supernaturalcraft.entity.legacy.LegacyWeapons.absolute(source)) return amount;
        return amount * AlphaRules.damageScale(alpha);
    }

    /** Test hook: {@code p} counts as near when it falls (fake players are not in the level's list). */
    public void track(ServerPlayer p) {
        if (!tracked.contains(p)) tracked.add(p);
    }

    /** Each server tick: its bar, and by day, back into the dark if its quarry is gone. */
    public void tick(Monster self) {
        if (!(self.level() instanceof ServerLevel level)) return;
        bar.setProgress(Math.max(0, self.getHealth() / self.getMaxHealth()));
        if (self.tickCount % 20 == 0) {
            for (ServerPlayer p : List.copyOf(bar.getPlayers())) if (!p.isAlive() || p.distanceTo(self) > AlphaRules.BAR_REACH) bar.removePlayer(p);
            for (ServerPlayer p : level.players()) if (p.distanceTo(self) <= AlphaRules.BAR_REACH && !p.isSpectator()) bar.addPlayer(p);
            ServerPlayer q = quarry == null ? null : level.getServer().getPlayerList().getPlayer(quarry);
            boolean near = q != null && q.level() == level && q.distanceTo(self) < AlphaRules.LEAVE_REACH;
            lonely = level.isDay() && !near && self.getTarget() == null ? lonely + 20 : 0;
            if (lonely >= AlphaRules.LEAVE_TICKS) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, self.getX(), self.getY() + 1, self.getZ(), 40, 0.4, 0.8, 0.4, 0.03);
                bar.removeAllPlayers();
                self.discard();
            }
        }
    }

    /** It fell for good: Crowley's errand moves on for whoever brought it down or was near and after it. */
    public void slain(Monster self, DamageSource source) {
        if (credited || !(self.level() instanceof ServerLevel level)) return;
        credited = true;
        bar.removeAllPlayers();
        List<ServerPlayer> near = new ArrayList<>();
        if (source.getEntity() instanceof ServerPlayer killer) near.add(killer);
        for (ServerPlayer p : level.players()) if (!near.contains(p) && p.distanceTo(self) <= AlphaRules.SLAIN_REACH) near.add(p);
        for (ServerPlayer p : tracked) if (!near.contains(p) && p.isAlive()) near.add(p);
        for (ServerPlayer p : near) {
            if (p == source.getEntity() || Pacts.huntingAlpha(p, alpha)) Pacts.alphaSlain(p, alpha);
        }
        level.sendParticles(ParticleTypes.SOUL, self.getX(), self.getY() + 1, self.getZ(), 30, 0.4, 0.8, 0.4, 0.04);
    }

    /** One of its moves, shown to everyone near. */
    public static void fx(Monster self, int move, @Nullable LivingEntity victim, int duration) {
        if (self.level() instanceof ServerLevel level) {
            EveFxPayload.send(level, self.position(), new EveFxPayload(self.getId(), EveFxPayload.ALPHA, move, victim != null ? victim.getId() : -1,
                    self.position(), duration));
        }
    }

    /** Tells its quarry it is coming. */
    public static void herald(ServerPlayer quarry, PactRules.Alpha a) {
        quarry.sendSystemMessage(Component.translatable("message.supernaturalcraft.alpha.coming." + a.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    public void save(CompoundTag tag) {
        if (quarry != null) tag.putUUID("Quarry", quarry);
        tag.putBoolean("Credited", credited);
    }

    public void load(CompoundTag tag) {
        quarry = tag.hasUUID("Quarry") ? tag.getUUID("Quarry") : null;
        credited = tag.getBoolean("Credited");
    }
}
