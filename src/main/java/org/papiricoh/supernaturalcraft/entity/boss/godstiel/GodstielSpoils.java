package org.papiricoh.supernaturalcraft.entity.boss.godstiel;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.arena.Stakes;
import org.papiricoh.supernaturalcraft.magic.item.SigilPageItem;
import org.papiricoh.supernaturalcraft.pact.PactDialogue;
import org.papiricoh.supernaturalcraft.pact.PactRules;
import org.papiricoh.supernaturalcraft.pact.Pacts;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.util.Players;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Godstiel's end (v0.39). Beaten, he kneels, and the hunter who opened the door (else whoever struck last) is asked: <b>forgive</b>
 * him (the goo pours out of him, he rises and goes; their reward is a Shard of his Grace) or <b>condemn</b> him (the Leviathans
 * drag him into the black water; their reward is the trench coat he leaves floating). Silence is condemnation. Every hunter
 * who fought him moves to the same end of the pact and gets the same reward, the Blade of the New God and the page of
 * {@link #SIGIL}. A rematch by rite asks nothing and leaves the blade and the page.
 */
public final class GodstielSpoils {

    public static final String ADVANCEMENT = "main/the_man_who_knew_too_much";
    /** The sigil taught by his page: the New God's wrath. */
    public static final ResourceLocation SIGIL = SupernaturalCraft.asResource("wrath_of_the_new_god");

    private GodstielSpoils() {
    }

    /** Hooks the talk into the shared dialogue (once, at mod construction). */
    public static void registerDialogue() {
        AllegianceDialogue.register(PactDialogue.FATE, GodstielSpoils::answer);
    }

    /** Who decides: the one who opened the door if they are near, else whoever struck last, else the nearest hunter. */
    public static @Nullable ServerPlayer decider(ServerLevel level, GodstielEntity boss) {
        UUID opener = boss.opener();
        if (opener != null && Players.byUuid(level, opener) instanceof ServerPlayer p && p.isAlive() && p.distanceToSqr(boss) < 64 * 64) return p;
        if (boss.getLastHurtByMob() instanceof ServerPlayer p && p.isAlive()) return p;
        ServerPlayer best = null;
        for (ServerPlayer p : boss.challengers()) if (best == null || p.distanceToSqr(boss) < best.distanceToSqr(boss)) best = p;
        return best;
    }

    /** As he kneels: the question. */
    public static void offer(ServerLevel level, GodstielEntity boss) {
        if (boss.askedOf() != null) return;
        ServerPlayer who = decider(level, boss);
        if (who == null) {
            boss.verdict(false, null);
            return;
        }
        offerTo(boss, who);
    }

    /** Puts the question to {@code who} (public for tests: fake players are not in the player index). */
    public static void offerTo(GodstielEntity boss, ServerPlayer who) {
        boss.askedOf(who.getUUID());
        AllegianceDialogue.ask(who, boss, PactDialogue.FATE, PactDialogue.FATE_LINE.node(), PactDialogue.FATE_LINE.options(), GodstielBalance.TALK_TIMEOUT);
        Component name = who.getDisplayName();
        for (ServerPlayer p : boss.challengers()) {
            if (p == who) continue;
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.godstiel.deciding", name).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
        }
    }

    /** An answer from the shared dialogue. Public for tests (fake players answer by hand). */
    public static void answer(ServerPlayer player, @Nullable Entity speaker, String node, String option) {
        if (!(speaker instanceof GodstielEntity boss) || !player.getUUID().equals(boss.askedOf())) return;
        decide(player, boss, PactDialogue.fate(option) == PactDialogue.Act.FORGIVE);
    }

    /** The verdict lands: everyone near hears it, and his end plays out accordingly. */
    public static void decide(@Nullable ServerPlayer by, GodstielEntity boss, boolean forgive) {
        if (!(boss.level() instanceof ServerLevel level) || boss.verdict() != null) return;
        Component name = by != null ? by.getDisplayName() : Component.translatable("message.supernaturalcraft.godstiel.nobody");
        String key = "message.supernaturalcraft.godstiel." + (forgive ? "forgiven" : "condemned");
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < 64 * 64) p.displayClientMessage(Component.translatable(key, name).withStyle(forgive ? ChatFormatting.AQUA : ChatFormatting.DARK_PURPLE), false);
        }
        boss.verdict(forgive, by);
    }

    /** At the very end of his fall: every hunter's share, and the pact's end. */
    public static void defeated(ServerLevel level, GodstielEntity boss, List<ServerPlayer> hunters) {
        Vec3 at = boss.position().add(0, 1, 0);
        List<ItemStack> spoils = new ArrayList<>();
        spoils.add(new ItemStack(AllItems.NEW_GOD_BLADE.get()));
        spoils.add(SigilPageItem.of(AllItems.SIGIL_PAGE.get(), SIGIL));
        Boolean forgive = boss.verdict();
        if (forgive != null) spoils.add(new ItemStack(forgive ? AllItems.GRACE_SHARD.get() : AllItems.CASTIELS_TRENCHCOAT.get()));
        List<ServerPlayer> share = new ArrayList<>(hunters);
        if (share.isEmpty()) {
            for (ItemStack s : spoils) spawn(level, at, s);
            return;
        }
        for (ServerPlayer p : share) {
            List<ItemStack> mine = new ArrayList<>();
            for (ItemStack s : spoils) mine.add(s.copy());
            Stakes.give(level.getServer(), p.getUUID(), mine, false);
            if (forgive != null && !boss.isRematch()) {
                PactRules.Step step = Pacts.step(p);
                if (!step.decided()) Pacts.advance(p, forgive ? PactRules.Step.FORGIVEN : PactRules.Step.CONDEMNED);
            }
        }
    }

    private static void spawn(ServerLevel level, Vec3 at, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }
}
