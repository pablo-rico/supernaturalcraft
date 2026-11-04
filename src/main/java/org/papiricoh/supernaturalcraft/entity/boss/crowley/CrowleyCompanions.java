package org.papiricoh.supernaturalcraft.entity.boss.crowley;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.chain.ChainEvents;
import org.papiricoh.supernaturalcraft.chain.ChainFaction;
import org.papiricoh.supernaturalcraft.chain.ChainNames;
import org.papiricoh.supernaturalcraft.chain.ChainRules;
import org.papiricoh.supernaturalcraft.chain.ChainSavedData;
import org.papiricoh.supernaturalcraft.chain.Feud;
import org.papiricoh.supernaturalcraft.chain.Officer;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.crossroads.ContractTerms;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.entity.hellhound.HellhoundEntity;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The recruited Crowley's world side (v0.35): he comes back to his hunter whenever he is not about (after a retreat, a portal,
 * a login), or to his seat once its chunk is loaded; and his menu — information, debt-free deals (one a day), intrigue in the
 * Pit (demons only), stay here / follow me.
 */
public final class CrowleyCompanions {

    public static final String DIALOGUE = "crowley_companion";
    public static final String MENU = "menu", DEALS = "deals";
    public static final String INFO = "info", DEAL = "deal", INTRIGUE = "intrigue", STAY = "stay", FOLLOW = "follow", BYE = "bye";
    public static final String LUCK = "luck", REVEAL = "reveal", TRADE = "trade", FAVOUR = "favour", BACK = "back";
    /** Every node and every option (the lang's contract). */
    public static final List<String> NODES = List.of(MENU, DEALS);
    public static final List<String> MENU_OPTIONS = List.of(INFO, DEAL, INTRIGUE, STAY, FOLLOW, BYE);
    public static final List<String> DEAL_OPTIONS = List.of(LUCK, REVEAL, TRADE, FAVOUR, BACK);

    /** A boss fell near his hunter: his next remark is about it. */
    private static final Map<UUID, Long> BOSS_FELL = new ConcurrentHashMap<>();

    private CrowleyCompanions() {
    }

    public static void registerDialogue() {
        AllegianceDialogue.register(DIALOGUE, CrowleyCompanions::answer);
    }

    // --- coming and going ---------------------------------------------------------------------------------------------

    /** Whether he will be where {@code owner} is now (never Heaven; Hell by config). */
    public static boolean welcome(ServerPlayer owner) {
        return CrowleyCompanionEntity.follows(owner.level());
    }

    public static Vec3 besideOf(ServerPlayer owner) {
        Vec3 side = owner.getLookAngle().multiply(1, 0, 1);
        side = side.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 at = owner.position().add(side.z * 1.6, 0, -side.x * 1.6);
        BlockPos p = BlockPos.containing(at);
        ServerLevel level = owner.serverLevel();
        if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) return owner.position();
        return at;
    }

    /** Recruited at {@code at}: he comes back as his hunter's companion, there and then. */
    public static void arrive(ServerLevel level, Vec3 at, CrowleySavedData data) {
        CrowleyCompanionEntity c = AllEntities.CROWLEY_COMPANION.get().create(level);
        if (c == null) return;
        c.moveTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(c);
        data.setCompanion(c.getUUID());
        c.poof(level, true);
        ServerPlayer owner = c.ownerPlayer();
        if (owner != null) c.say(owner, CrowleyVoice.RECRUITED);
    }

    /** Once a second: brings him back when he should be about and is not. */
    public static void tick(MinecraftServer server) {
        CrowleySavedData data = CrowleySavedData.get(server);
        if (data.state() != CrowleySavedData.State.RECRUITED || data.owner() == null) return;
        if (server.overworld().getGameTime() < data.absentUntil()) return;
        if (CrowleySummoning.fighting(server)) return;
        if (present(server, data) != null) return;
        if (data.seat() != null) {
            ServerLevel level = data.seatLevel() != null ? server.getLevel(data.seatLevel()) : null;
            if (level == null || !level.isLoaded(data.seat())) return;
            boolean back = data.absentUntil() > 0;
            spawn(level, CrowleyCompanionEntity.sittingPoint(level, data.seat()), data, back);
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(data.owner());
        if (owner == null || !owner.isAlive() || owner.isSpectator() || !welcome(owner)) return;
        spawn(owner.serverLevel(), besideOf(owner), data, data.absentUntil() > 0);
    }

    private static void spawn(ServerLevel level, Vec3 at, CrowleySavedData data, boolean back) {
        CrowleyCompanionEntity c = AllEntities.CROWLEY_COMPANION.get().create(level);
        if (c == null) return;
        c.moveTo(at.x, at.y, at.z, 0, 0);
        data.setCompanion(c.getUUID());
        level.addFreshEntity(c);
        c.poof(level, true);
        if (back) {
            data.setAbsentUntil(0);
            ServerPlayer owner = c.ownerPlayer();
            if (owner != null) c.say(owner, CrowleyVoice.BACK);
        }
    }

    /** The companion the world names, if it is loaded anywhere. */
    public static @Nullable CrowleyCompanionEntity present(MinecraftServer server, CrowleySavedData data) {
        UUID id = data.companion();
        if (id == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e instanceof CrowleyCompanionEntity c && c.isAlive()) return c;
        }
        return null;
    }

    /** Sends him off (a rematch, a command): he is gone until {@link #tick} brings him back. */
    public static void dismiss(MinecraftServer server) {
        CrowleySavedData data = CrowleySavedData.get(server);
        CrowleyCompanionEntity c = present(server, data);
        if (c != null) {
            c.poof((ServerLevel) c.level(), false);
            c.discard();
        }
    }

    public static void bossFell(ServerPlayer near) {
        BOSS_FELL.put(near.getUUID(), near.serverLevel().getGameTime());
    }

    // --- his remarks --------------------------------------------------------------------------------------------------

    /** What he would remark on now, or null (nothing worth his breath). */
    public static @Nullable String situation(ServerLevel level, CrowleyCompanionEntity c, ServerPlayer owner, @Nullable LivingEntity foe) {
        Long fell = BOSS_FELL.remove(owner.getUUID());
        if (fell != null && level.getGameTime() - fell < 600) return CrowleyVoice.BOSS;
        if (owner.getHealth() < owner.getMaxHealth() * 0.3f) return CrowleyVoice.HURT;
        if (foe != null) return c.getRandom().nextInt(4) == 0 ? CrowleyVoice.FIGHT : null;
        if (HellDimension.isHell(level)) return c.getRandom().nextInt(6) == 0 ? CrowleyVoice.HELL : null;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, c.getBoundingBox().inflate(16), CrowleyRetinue::rivals)) {
            if (ChainEvents.factionOf(e) == ChainFaction.DEMON) return CrowleyVoice.PIT;
        }
        if (c.seated()) return c.getRandom().nextInt(20) == 0 ? CrowleyVoice.SEATED : null;
        if (level.isThundering() || level.isRaining()) return c.getRandom().nextInt(30) == 0 ? CrowleyVoice.RAIN : null;
        if (level.isNight()) return c.getRandom().nextInt(30) == 0 ? CrowleyVoice.NIGHT : null;
        return c.getRandom().nextInt(40) == 0 ? CrowleyVoice.IDLE : null;
    }

    // --- his menu -----------------------------------------------------------------------------------------------------

    public static void openMenu(ServerPlayer p, CrowleyCompanionEntity c) {
        List<String> options = new ArrayList<>();
        options.add(INFO);
        options.add(DEAL);
        if (Kin.isDemon(p)) options.add(INTRIGUE);
        options.add(CrowleySavedData.get(p.server).seat() != null ? FOLLOW : STAY);
        options.add(BYE);
        AllegianceDialogue.ask(p, c, DIALOGUE, MENU, options, 0);
    }

    public static void answer(ServerPlayer p, @Nullable Entity speaker, String node, String option) {
        if (!(speaker instanceof CrowleyCompanionEntity c) || !p.getUUID().equals(c.owner())) return;
        CrowleySavedData data = CrowleySavedData.get(p.server);
        long day = ChainEvents.day(p.server);
        switch (option) {
            case INFO -> info(p);
            case DEAL -> AllegianceDialogue.ask(p, c, DIALOGUE, DEALS, DEAL_OPTIONS, 0);
            case INTRIGUE -> intrigue(p, data, day);
            case STAY -> {
                data.seat(c.blockPosition(), c.level().dimension());
                c.say(p, CrowleyVoice.SEATED);
            }
            case FOLLOW -> {
                data.seat(null, null);
                tell(p, "follow");
            }
            case BACK -> openMenu(p, c);
            case LUCK, REVEAL, TRADE, FAVOUR -> deal(p, c, data, day, option);
            default -> {
            }
        }
    }

    private static void tell(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable("message.supernaturalcraft.crowley." + key, args).withStyle(ChatFormatting.GRAY));
    }

    private static void info(ServerPlayer p) {
        BossProgression.Boss next = BossProgression.next(path -> AuthorWorld.done(p, path));
        if (next != null) tell(p, "info.boss", Component.translatable("entity.supernaturalcraft." + next.entity));
        else tell(p, "info.no_boss");
        Officer best = null;
        double bestD = Double.MAX_VALUE;
        String dim = p.level().dimension().location().toString();
        var h = ChainSavedData.get(p.server).hierarchy(ChainFaction.DEMON);
        if (h != null) {
            for (Officer o : h.alive()) {
                if (o.seenDay < 0 || o.player != null || !dim.equals(o.seenDimension)) continue;
                double d = Math.hypot(o.seenX - p.getX(), o.seenZ - p.getZ());
                if (d < bestD) {
                    bestD = d;
                    best = o;
                }
            }
        }
        if (best != null) tell(p, "info.officer", ChainNames.name(best), best.seenX, best.seenZ);
        else tell(p, "info.no_officer");
    }

    private static void intrigue(ServerPlayer p, CrowleySavedData data, long day) {
        if (!Kin.isDemon(p)) return;
        if (!data.mayIntrigue(day)) {
            tell(p, "intrigue.wait");
            return;
        }
        ChainSavedData chain = ChainSavedData.get(p.server);
        var h = chain.hierarchy(ChainFaction.DEMON);
        var court = chain.court(ChainFaction.DEMON);
        if (h == null || court == null || !chain.awake(ChainFaction.DEMON) || court.feuding()) {
            tell(p, "intrigue.none");
            return;
        }
        var pair = Feud.pick(h, court.vacant(), ChainRules.rng(p.server.overworld().getSeed(), -61, p.server.overworld().getGameTime()));
        if (pair == null) {
            tell(p, "intrigue.none");
            return;
        }
        court.feudChallenger = pair.challenger().id;
        court.feudDefender = pair.defender().id;
        chain.changed();
        data.intrigued(day);
        tell(p, "intrigue.done", ChainNames.name(pair.challenger()), ChainNames.name(pair.defender()));
    }

    private static void deal(ServerPlayer p, CrowleyCompanionEntity c, CrowleySavedData data, long day, String which) {
        if (!data.mayDeal(p.getUUID(), day)) {
            tell(p, "deal.wait");
            return;
        }
        switch (which) {
            case LUCK -> p.addEffect(new MobEffectInstance(MobEffects.LUCK, CrowleyBalance.LUCK_TICKS, 1));
            case REVEAL -> {
                for (HellhoundEntity h : p.serverLevel().getEntitiesOfClass(HellhoundEntity.class, p.getBoundingBox().inflate(64))) {
                    h.reveal(CrowleyBalance.REVEAL_TICKS);
                }
            }
            case TRADE -> {
                if (!take(p, Items.EMERALD, CrowleyBalance.TRADE_EMERALDS)) {
                    tell(p, "deal.trade.short", CrowleyBalance.TRADE_EMERALDS);
                    return;
                }
                give(p, new ItemStack(AllItems.SALT.get(), 16));
                give(p, new ItemStack(AllItems.SULFUR.get(), 8));
                give(p, new ItemStack(AllItems.HOLY_WATER.get(), 2));
            }
            case FAVOUR -> {
                if (!Debts.get(p).active()) {
                    tell(p, "deal.favour.none");
                    return;
                }
                if (!data.mayFavour(p.getUUID(), day)) {
                    tell(p, "deal.favour.wait", CrowleyBalance.FAVOUR_DAYS);
                    return;
                }
                Debts.free(p, ContractTerms.VOID);
                data.favoured(p.getUUID(), day);
            }
            default -> {
                return;
            }
        }
        data.dealt(p.getUUID(), day);
        c.triggerAnim("action", "snap");
        tell(p, "deal." + which);
    }

    private static boolean take(ServerPlayer p, net.minecraft.world.item.Item item, int n) {
        if (p.getInventory().countItem(item) < n) return false;
        int left = n;
        for (int i = 0; i < p.getInventory().getContainerSize() && left > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.is(item)) continue;
            int t = Math.min(left, s.getCount());
            s.shrink(t);
            left -= t;
        }
        return true;
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    /** Whether {@code e} is a boss (his remark when one falls near his hunter). */
    public static boolean boss(Entity e) {
        return e.getType().is(AllTags.Entities.BOSSES);
    }
}
