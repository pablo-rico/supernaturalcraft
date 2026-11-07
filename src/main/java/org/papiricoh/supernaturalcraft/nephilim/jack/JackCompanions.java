package org.papiricoh.supernaturalcraft.nephilim.jack;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRegistry;
import org.papiricoh.supernaturalcraft.nephilim.NephilimRules;
import org.papiricoh.supernaturalcraft.nephilim.Nephilims;
import org.papiricoh.supernaturalcraft.util.WorldClock;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.41: Jack at a hunter's side (one per hunter, from his birth until he trades his soul away). The birth scene hands him
 * over with {@link #arrive}; he follows, stays when told ({@link JackBond#seat}), goes away to heal when he falls, and this
 * brings him back every few seconds (also into another dimension the hunter went to). Talking to him opens the {@code jack}
 * dialogue: a word, his blood (once a day, for the rift's rite), stay or come.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class JackCompanions {

    public static final String DIALOGUE = "jack";
    public static final String MENU = "menu";
    public static final String TALK = "talk", BLOOD = "blood", STAY = "stay", FOLLOW = "follow", BYE = "bye";
    private static final int PERIOD = 100;
    /** A staying Jack is brought back to his seat only while the hunter is this close to it. */
    private static final double SEAT_RANGE = 64;

    private static final Map<UUID, UUID> WITH = new ConcurrentHashMap<>();

    private JackCompanions() {
    }

    /** At mod construction. */
    public static void registerDialogue() {
        AllegianceDialogue.register(DIALOGUE, JackCompanions::answer);
    }

    public static JackBond bond(ServerPlayer p) {
        return p.getData(NephilimRegistry.JACK_BOND);
    }

    private static void setBond(ServerPlayer p, JackBond b) {
        p.setData(NephilimRegistry.JACK_BOND, b);
    }

    static void remember(UUID hunter, UUID jack) {
        WITH.put(hunter, jack);
    }

    static void forget(UUID hunter, UUID jack) {
        WITH.remove(hunter, jack);
    }

    /** The Jack walking with {@code hunter} now, if any (in their level). */
    public static @Nullable JackEntity with(ServerPlayer hunter) {
        UUID id = WITH.get(hunter.getUUID());
        if (id == null) return null;
        if (hunter.serverLevel().getEntity(id) instanceof JackEntity j && j.isAlive()) return j;
        return null;
    }

    /** Jack, newly born (or come back), joins {@code hunter} at {@code at} (or beside them if null). */
    public static void arrive(ServerPlayer hunter, @Nullable BlockPos at) {
        if (!Nephilims.step(hunter).jackWalks() || with(hunter) != null) return;
        if (bond(hunter).retreatUntil() > hunter.serverLevel().getGameTime()) return;
        ServerLevel level = hunter.serverLevel();
        JackEntity j = NephilimRegistry.JACK.get().create(level);
        if (j == null) return;
        Vec3 pos = at != null ? Vec3.atBottomCenterOf(at)
                : hunter.position().subtract(Vec3.directionFromRotation(0, hunter.getYRot()).scale(2));
        j.moveTo(pos.x, at != null ? at.getY() : hunter.getY(), pos.z, hunter.getYRot() + 180, 0);
        j.bind(hunter.getUUID());
        level.addFreshEntity(j);
        remember(hunter.getUUID(), j.getUUID());
        j.flash(level);
        j.triggerAnim("action", "appear");
    }

    /** He fell defending {@code hunter}: away to heal. */
    static void fell(ServerPlayer hunter) {
        setBond(hunter, bond(hunter).withRetreat(hunter.serverLevel().getGameTime() + JackRules.RETREAT_TICKS));
        hunter.sendSystemMessage(Component.translatable("message.supernaturalcraft.jack.fell"));
    }

    /** Sends away the Jack walking with {@code hunter} (the road moved past him: he left with his soul traded away). */
    public static void dismiss(ServerPlayer hunter) {
        JackEntity j = with(hunter);
        if (j != null) {
            j.flash(hunter.serverLevel());
            j.discard();
        }
        setBond(hunter, JackBond.EMPTY);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.overworld().getGameTime() % PERIOD != 53) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || p.isSpectator() || !Nephilims.step(p).jackWalks() || with(p) != null) continue;
            JackBond b = bond(p);
            if (b.retreatUntil() > p.serverLevel().getGameTime()) continue;
            Optional<GlobalPos> seat = b.seat();
            if (seat.isEmpty()) {
                arrive(p, null);
            } else if (seat.get().dimension() == p.level().dimension() && p.blockPosition().closerThan(seat.get().pos(), SEAT_RANGE)
                    && p.serverLevel().isLoaded(seat.get().pos())) {
                arrive(p, seat.get().pos());
            }
        }
    }

    // --- Talking with him ---------------------------------------------------------------------------------------------

    static void menu(ServerPlayer p, JackEntity j) {
        NephilimRules.Step step = Nephilims.step(p);
        List<String> options = new java.util.ArrayList<>();
        options.add(TALK);
        if (JackRules.graced(step)) options.add(BLOOD);
        options.add(bond(p).staying() ? FOLLOW : STAY);
        options.add(BYE);
        j.getNavigation().stop();
        j.getLookControl().setLookAt(p);
        AllegianceDialogue.ask(p, j, DIALOGUE, MENU + "." + JackRules.mood(step), options, 0);
    }

    private static void answer(ServerPlayer p, @Nullable Entity speaker, String node, String option) {
        JackEntity j = speaker instanceof JackEntity e ? e : with(p);
        NephilimRules.Step step = Nephilims.step(p);
        switch (option) {
            case TALK -> {
                int n = p.getRandom().nextInt(JackRules.LINES);
                p.sendSystemMessage(Component.translatable("message.supernaturalcraft.jack.talk." + JackRules.mood(step) + "." + n));
                if (j != null) j.triggerAnim("action", "talk");
            }
            case BLOOD -> {
                long today = WorldClock.day(p.getServer()) / 24000L;
                JackBond b = bond(p);
                if (!JackRules.graced(step)) {
                    p.sendSystemMessage(Component.translatable("message.supernaturalcraft.jack.no_grace"));
                } else if (!JackRules.bloodDue(b.lastBlood(), today)) {
                    p.sendSystemMessage(Component.translatable("message.supernaturalcraft.jack.blood_tomorrow"));
                } else {
                    setBond(p, b.withBlood(today));
                    ItemStack vial = new ItemStack(NephilimRegistry.NEPHILIM_BLOOD.get());
                    if (!p.getInventory().add(vial)) p.drop(vial, false);
                    p.sendSystemMessage(Component.translatable("message.supernaturalcraft.jack.blood"));
                    if (j != null) j.triggerAnim("action", "heal");
                }
            }
            case STAY -> {
                if (j != null) {
                    setBond(p, bond(p).withSeat(Optional.of(JackBond.at(j.level().dimension(), j.blockPosition()))));
                    p.sendSystemMessage(Component.translatable("message.supernaturalcraft.jack.stay"));
                }
            }
            case FOLLOW -> {
                setBond(p, bond(p).withSeat(Optional.empty()));
                p.sendSystemMessage(Component.translatable("message.supernaturalcraft.jack.follow"));
            }
            default -> {
            }
        }
    }
}
