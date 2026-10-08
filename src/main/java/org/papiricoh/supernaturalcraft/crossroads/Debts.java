package org.papiricoh.supernaturalcraft.crossroads;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal.State;
import org.papiricoh.supernaturalcraft.entity.demon.CrossroadsDemonEntity;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The debt: ticked every second per player. On the last day the hounds are heard; when it falls
 * due a pack comes for the debtor. Ways out: survive the hunt (kill the pack, or last
 * {@link DealTerms#SURVIVE_TICKS}), kill the demon before the due date (Break the Deal), or die
 * while the hounds are out (the soul is collected, and something is lost with it).
 */
public final class Debts {

    /** Checks between regroups of a pack whose hounds went missing (another dimension, unloaded). */
    private static final int REGROUP_CHECKS = 5;
    private static final Map<UUID, Long> LOGIN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_OMEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> MISSING = new ConcurrentHashMap<>();

    private Debts() {
    }

    public static CrossroadsDeal get(Player p) {
        return p.getData(AllAttachments.CROSSROADS_DEAL);
    }

    public static void set(Player p, CrossroadsDeal deal) {
        p.setData(AllAttachments.CROSSROADS_DEAL, deal);
    }

    /** The server's clock (game time of the overworld, shared by every dimension). */
    public static long now(Player p) {
        MinecraftServer server = p.getServer();
        return server != null ? server.overworld().getGameTime() : p.level().getGameTime();
    }

    // --- the clock --------------------------------------------------------------------------------

    public static void tick(ServerPlayer p) {
        CrossroadsDeal deal = get(p);
        long now = now(p);
        switch (deal.state()) {
            case OPEN, HUNTED -> {
                if (DealTerms.due(now, deal.dueAt())) {
                    Long login = LOGIN.get(p.getUUID());
                    if (login == null || now - login >= DealTerms.LOGIN_GRACE) startHunt(p, now);
                    return;
                }
                if (deal.state() == State.HUNTED) tickHunted(p, deal);
                if (DealTerms.lastDay(now, deal.dueAt())) omen(p, deal, now);
            }
            case COLLECTING -> tickHunt(p, deal, now);
            default -> {
            }
        }
    }

    public static void onLogin(ServerPlayer p) {
        long now = now(p);
        LOGIN.put(p.getUUID(), now);
        CrossroadsDeal deal = get(p);
        // Logging out is no way to outlast the hounds: the clock starts again once they are back.
        if (deal.state() == State.COLLECTING) set(p, deal.withHuntStartedAt(now + DealTerms.LOGIN_GRACE));
    }

    public static void onLogout(ServerPlayer p) {
        LOGIN.remove(p.getUUID());
        LAST_OMEN.remove(p.getUUID());
        MISSING.remove(p.getUUID());
    }

    /** The last day: distant howls and a warning, now and then. */
    private static void omen(ServerPlayer p, CrossroadsDeal deal, long now) {
        Long last = LAST_OMEN.get(p.getUUID());
        if (last != null && now - last < DealTerms.OMEN_INTERVAL) return;
        LAST_OMEN.put(p.getUUID(), now);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.omen", DealTerms.hoursLeft(now, deal.dueAt()))
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        p.playNotifySound(AllSounds.DEBT_HOWL.get(), SoundSource.HOSTILE, 0.45f, 0.75f + p.getRandom().nextFloat() * 0.2f);
    }

    // --- the hunt ---------------------------------------------------------------------------------

    /** The debt is due: the hounds come. A demon still walking (Break the Deal, too late) leaves. */
    public static void startHunt(ServerPlayer p, long now) {
        CrossroadsDeal deal = get(p);
        Optional<UUID> demon = deal.demon();
        List<UUID> pack = spawnPack(p, DealTerms.packSize(p.getRandom().nextInt()));
        set(p, deal.withState(DealTerms.next(deal.state(), DealTerms.Event.DUE, false))
                .withHounds(pack).withHuntStartedAt(now).withDemon(Optional.empty()));
        demon.ifPresent(id -> dismissDemon(p.server, id));
        MISSING.remove(p.getUUID());
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.hounds_come").withStyle(ChatFormatting.DARK_RED), true);
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.DEBT_HOWL.get(), SoundSource.HOSTILE, 3f, 0.8f);
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.HELLHOUND_BARK.get(), SoundSource.HOSTILE, 2f, 0.7f);
    }

    /** {@code n} unseen hounds around {@code p}, each sent for {@code p} alone. */
    public static List<UUID> spawnPack(ServerPlayer p, int n) {
        ServerLevel level = p.serverLevel();
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * (i + p.getRandom().nextDouble() * 0.6) / n;
            double r = 7 + p.getRandom().nextDouble() * 3;
            Vec3 at = floor(level, p.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
            HellhoundEntity hound = AllEntities.HELLHOUND.get().create(level);
            if (hound == null) continue;
            hound.moveTo(at.x, at.y, at.z, p.getRandom().nextFloat() * 360, 0);
            hound.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.MOB_SUMMONED, null);
            hound.setQuarry(p);
            hound.setTarget(p);
            level.addFreshEntity(hound);
            ids.add(hound.getUUID());
        }
        return ids;
    }

    private static void tickHunt(ServerPlayer p, CrossroadsDeal deal, long now) {
        if (deal.hounds().isEmpty() || (now > deal.huntStartedAt() && DealTerms.survived(now, deal.huntStartedAt()))) {
            free(p, ContractTerms.PAID);
            return;
        }
        Long login = LOGIN.get(p.getUUID());
        if (login != null && now - login < DealTerms.LOGIN_GRACE) return;
        // Hounds lost (the debtor changed dimension, logged out, outran the loaded world) regroup.
        ServerLevel level = p.serverLevel();
        List<UUID> present = new ArrayList<>();
        for (UUID id : deal.hounds()) {
            if (level.getEntity(id) instanceof HellhoundEntity h && h.isAlive()) present.add(id);
        }
        if (present.size() == deal.hounds().size()) {
            MISSING.remove(p.getUUID());
            return;
        }
        int checks = MISSING.merge(p.getUUID(), 1, Integer::sum);
        if (checks < REGROUP_CHECKS || level.getDifficulty() == Difficulty.PEACEFUL) return;
        MISSING.remove(p.getUUID());
        present.addAll(spawnPack(p, deal.hounds().size() - present.size()));
        set(p, deal.withHounds(present));
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.HELLHOUND_BARK.get(), SoundSource.HOSTILE, 2f, 0.7f);
    }

    /** A hound sent for a debt died: one fewer. The last one sets the debtor free. */
    public static void onHoundSlain(HellhoundEntity hound) {
        UUID quarry = hound.quarry();
        if (quarry == null) return;
        Player q = hound.quarryEntity();
        if (q == null && hound.getServer() != null) q = hound.getServer().getPlayerList().getPlayer(quarry);
        if (!(q instanceof ServerPlayer p)) return;
        CrossroadsDeal deal = get(p);
        if (deal.state() != State.COLLECTING || !deal.hounds().contains(hound.getUUID())) return;
        List<UUID> left = new ArrayList<>(deal.hounds());
        left.remove(hound.getUUID());
        set(p, deal.withHounds(left));
        if (left.isEmpty()) free(p, ContractTerms.PAID);
    }

    // --- breaking the deal --------------------------------------------------------------------------

    /** Why Break the Deal cannot be cast by {@code p} (a translation key), or null. */
    public static @Nullable String cannotBreak(ServerPlayer p) {
        CrossroadsDeal deal = get(p);
        if (deal.state() != State.OPEN || DealTerms.due(now(p), deal.dueAt())) return "message.supernaturalcraft.crossroads.no_deal";
        return null;
    }

    /** Break the Deal: the demon walks again, hostile, near {@code near}. */
    public static boolean breakDeal(ServerPlayer p, Vec3 near) {
        if (cannotBreak(p) != null) return false;
        CrossroadsDeal deal = get(p);
        ServerLevel level = p.serverLevel();
        double a = p.getRandom().nextDouble() * Math.PI * 2;
        CrossroadsDemonEntity demon = CrossroadsDemonEntity.hunt(level, floor(level, near.add(Math.cos(a) * 4, 0, Math.sin(a) * 4)), p);
        if (demon == null) return false;
        set(p, deal.withState(DealTerms.next(deal.state(), DealTerms.Event.BREAK, true)).withDemon(Optional.of(demon.getUUID())));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.demon_walks").withStyle(ChatFormatting.DARK_RED), true);
        return true;
    }

    /** A hunted demon that fled its vessel comes back on a later night. */
    private static void tickHunted(ServerPlayer p, CrossroadsDeal deal) {
        if (deal.demon().isPresent()) return;
        ServerLevel level = p.serverLevel();
        if (!DealTerms.demonReturns(level.isNight(), level.getDayTime(), deal.demonGoneAt())) return;
        double a = p.getRandom().nextDouble() * Math.PI * 2;
        CrossroadsDemonEntity demon = CrossroadsDemonEntity.hunt(level, floor(level, p.position().add(Math.cos(a) * 8, 0, Math.sin(a) * 8)), p);
        if (demon == null) return;
        set(p, deal.withDemon(Optional.of(demon.getUUID())));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.demon_returns").withStyle(ChatFormatting.DARK_RED), true);
    }

    /** The hunted demon fled its vessel (smoke): it will be back. */
    public static void onDemonFled(CrossroadsDemonEntity demon) {
        if (!(demon.debtor() instanceof ServerPlayer p)) return;
        CrossroadsDeal deal = get(p);
        if (deal.state() != State.HUNTED || !deal.demon().map(demon.getUUID()::equals).orElse(false)) return;
        set(p, deal.withDemon(Optional.empty()).withDemonGoneAt(p.serverLevel().getDayTime()));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.demon_fled").withStyle(ChatFormatting.GRAY), true);
    }

    /** The hunted demon is dead: in time, its debtor is free. */
    public static void onDemonSlain(CrossroadsDemonEntity demon) {
        if (!(demon.debtor() instanceof ServerPlayer p)) return;
        CrossroadsDeal deal = get(p);
        long now = now(p);
        if (DealTerms.next(deal.state(), DealTerms.Event.DEMON_SLAIN, !DealTerms.due(now, deal.dueAt())) == State.FREE) {
            free(p, ContractTerms.VOID);
        }
    }

    // --- settling ------------------------------------------------------------------------------------

    /** Off the hook: the pack and the demon go, the contract reads {@code status}. */
    public static void free(ServerPlayer p, String status) {
        CrossroadsDeal deal = get(p);
        set(p, deal.withState(State.FREE).withHounds(List.of()).withDemon(Optional.empty()));
        dismissHounds(p.server, deal.hounds());
        deal.demon().ifPresent(id -> dismissDemon(p.server, id));
        markContracts(p, status);
        ChorusRewards.award(p, "main/debt_paid");
        p.displayClientMessage(Component.translatable(ContractTerms.VOID.equals(status)
                ? "message.supernaturalcraft.crossroads.free_void" : "message.supernaturalcraft.crossroads.free_paid").withStyle(ChatFormatting.GOLD), true);
        p.serverLevel().playSound(null, p.blockPosition(), AllSounds.CONTRACT_BURN.get(), SoundSource.PLAYERS, 1f, 1f);
    }

    /**
     * Died with the hounds out: the soul is collected. The debt is settled, but an upgrade is taken
     * back, or (any other wish) the soul comes back hollow ({@link AllMobEffects#SOULLESS} on respawn).
     */
    public static void collect(ServerPlayer p) {
        CrossroadsDeal deal = get(p);
        if (DealTerms.next(deal.state(), DealTerms.Event.DIED, false) != State.COLLECTED) return;
        DealTerms.Wish wish = deal.wishKind();
        // A bound soul (v0.13) rises a demon instead of paying the usual price.
        boolean risen = deal.soulBound() && CrossroadsHooks.soul.collected(p);
        boolean loseUpgrade = !risen && wish != null && DealTerms.penalty(wish) == DealTerms.Penalty.LOSE_UPGRADE;
        set(p, deal.withState(State.COLLECTED).withHounds(List.of()).withDemon(Optional.empty()).withPenaltyPending(!risen && !loseUpgrade));
        if (loseUpgrade) Wishes.revokeUpgrade(p, deal.arg());
        dismissHounds(p.server, deal.hounds());
        markContracts(p, ContractTerms.COLLECTED);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.collected").withStyle(ChatFormatting.DARK_RED), false);
    }

    /** A collected soul comes back hollow. */
    public static void onRespawn(ServerPlayer p) {
        CrossroadsDeal deal = get(p);
        if (!deal.penaltyPending()) return;
        p.addEffect(new MobEffectInstance(AllMobEffects.SOULLESS, DealTerms.SOULLESS_TICKS, 0, false, false, true));
        set(p, deal.withPenaltyPending(false));
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.crossroads.soulless").withStyle(ChatFormatting.DARK_GRAY), true);
    }

    private static void markContracts(ServerPlayer p, String status) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            ContractTerms terms = s.get(AllDataComponents.CONTRACT.get());
            if (terms != null && terms.open() && terms.owner().equals(p.getUUID())) s.set(AllDataComponents.CONTRACT.get(), terms.withStatus(status));
        }
    }

    private static void dismissHounds(MinecraftServer server, List<UUID> hounds) {
        for (UUID id : hounds) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getEntity(id) instanceof HellhoundEntity h) h.vanish();
            }
        }
    }

    private static void dismissDemon(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e instanceof CrossroadsDemonEntity d) d.leave();
        }
    }

    // --- places ---------------------------------------------------------------------------------------

    /**
     * Somewhere to stand near {@code near}: the closest floor (solid below, two clear blocks) within
     * a few blocks of its height, else the surface there if not far off, else {@code near} itself.
     */
    public static Vec3 floor(ServerLevel level, Vec3 near) {
        BlockPos base = BlockPos.containing(near);
        for (int i = 0; i <= 12; i++) {
            int dy = (i % 2 == 0) ? i / 2 : -(i + 1) / 2;
            BlockPos pos = base.above(dy);
            if (standable(level, pos)) return new Vec3(near.x, pos.getY(), near.z);
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
        if (Math.abs(top.getY() - near.y) < 16 && standable(level, top)) return new Vec3(near.x, top.getY(), near.z);
        return near;
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getFluidState(pos).isEmpty();
    }
}
