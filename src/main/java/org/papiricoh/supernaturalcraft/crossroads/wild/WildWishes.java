package org.papiricoh.supernaturalcraft.crossroads.wild;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms.Affliction;
import org.papiricoh.supernaturalcraft.crossroads.DealTerms.Wish;
import org.papiricoh.supernaturalcraft.crossroads.Deals;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseLevels;
import org.papiricoh.supernaturalcraft.weapon.curse.CurseState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The wishes only a demon called at a natural crossroads grants (v0.18): ASCEND, TROPHY, REVIVE and UNCURSE. What is on offer
 * depends on the hunter (what they hold, have beaten, have lost, carry with them); everything is checked again when sealing.
 */
public final class WildWishes {

    private WildWishes() {
    }

    /** The wild-only wishes on offer to {@code p} right now. */
    public static List<Deals.Option> offer(ServerPlayer p) {
        List<Deals.Option> out = new ArrayList<>();
        if (canAscend(p)) out.add(new Deals.Option(Wish.ASCEND, 0));
        for (int arg : DealTerms.trophyOffer(b -> BossTrophies.has(b) && beaten(p, b))) out.add(new Deals.Option(Wish.TROPHY, arg));
        if (WildPets.mostRecentDead(p.server, p.getUUID()).isPresent()) out.add(new Deals.Option(Wish.REVIVE, 0));
        for (Affliction a : Affliction.values()) if (afflicted(p, a)) out.add(new Deals.Option(Wish.UNCURSE, a.ordinal()));
        return out;
    }

    /** @return false if there was nothing to grant after all */
    public static boolean grant(ServerPlayer p, Wish wish, int arg, Vec3 at) {
        return switch (wish) {
            case ASCEND -> ascend(p);
            case TROPHY -> trophy(p, DealTerms.trophyBoss(arg));
            case REVIVE -> WildPets.revive(p, at) != null;
            case UNCURSE -> uncurse(p, Affliction.of(arg));
            default -> false;
        };
    }

    /** What the memory log is told the wish was for ({@code MemoryHooks.dealSealed}). */
    public static String memoryArg(Wish wish, int arg) {
        return switch (wish) {
            case TROPHY -> {
                Boss b = DealTerms.trophyBoss(arg);
                yield b == null ? "" : b.id();
            }
            case UNCURSE -> {
                Affliction a = Affliction.of(arg);
                yield a == null ? "" : a.id();
            }
            default -> String.valueOf(arg);
        };
    }

    // --- ASCEND -------------------------------------------------------------------------------------------------------------

    public static boolean canAscend(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        return Ascension.ascendable(held) && DealTerms.canAscend(Ascension.level(held), Ascension.playerTier(p), Ascension.maxLevel(held));
    }

    private static boolean ascend(ServerPlayer p) {
        if (!canAscend(p)) return false;
        Ascension.raise(p.getMainHandItem());
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.6f);
        return true;
    }

    // --- TROPHY -------------------------------------------------------------------------------------------------------------

    /** Whether {@code p} has beaten {@code boss}: its advancement, or the Vitality the victory left (as for the hunter's tier). */
    public static boolean beaten(ServerPlayer p, Boss boss) {
        return AuthorWorld.done(p, boss.advancement) || p.getData(AllAttachments.VITALITY).has(boss.id());
    }

    private static boolean trophy(ServerPlayer p, @Nullable Boss boss) {
        if (boss == null || !BossTrophies.has(boss) || !beaten(p, boss)) return false;
        give(p, BossTrophies.trophy(boss));
        give(p, BossTrophies.shard(boss));
        return true;
    }

    // --- UNCURSE ------------------------------------------------------------------------------------------------------------

    public static boolean afflicted(ServerPlayer p, Affliction a) {
        return switch (a) {
            case HUNGER -> {
                boolean[] hungry = {false};
                cursedStacks(p, s -> hungry[0] |= !CurseLevels.sated(s.getOrDefault(AllDataComponents.CURSE, CurseState.FRESH).satiation()));
                yield hungry[0];
            }
            case HEAVENS_MARK -> p.hasEffect(AllMobEffects.HEAVENS_MARK);
            case SOULLESS -> p.hasEffect(AllMobEffects.SOULLESS);
            case JINXED -> p.hasEffect(AllMobEffects.JINXED);
        };
    }

    private static boolean uncurse(ServerPlayer p, @Nullable Affliction a) {
        if (a == null || !afflicted(p, a)) return false;
        switch (a) {
            case HUNGER -> cursedStacks(p, s -> {
                CurseState c = s.getOrDefault(AllDataComponents.CURSE, CurseState.FRESH);
                s.set(AllDataComponents.CURSE, new CurseState(c.souls(), CurseLevels.MAX_SATIATION, c.owner(), c.lastMarkTick()));
            });
            case HEAVENS_MARK -> remove(p, AllMobEffects.HEAVENS_MARK);
            case SOULLESS -> remove(p, AllMobEffects.SOULLESS);
            case JINXED -> remove(p, AllMobEffects.JINXED);
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 0.6f);
        return true;
    }

    private static void remove(ServerPlayer p, Holder<MobEffect> effect) {
        p.removeEffect(effect);
    }

    private static void cursedStacks(ServerPlayer p, Consumer<ItemStack> each) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.has(AllDataComponents.CURSE.get())) each.accept(s);
        }
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!p.getInventory().add(stack) && !stack.isEmpty()) p.drop(stack, false);
    }
}
