package org.papiricoh.supernaturalcraft.bowl.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.bowl.BowlContents;
import org.papiricoh.supernaturalcraft.bowl.BowlLiquid;
import org.papiricoh.supernaturalcraft.bowl.BowlSpellRecipe;
import org.papiricoh.supernaturalcraft.bowl.Dose;
import org.papiricoh.supernaturalcraft.bowl.PersonalComponent;
import org.papiricoh.supernaturalcraft.bowl.spell.Taglock;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * One casting of a bowl spell: where, by whom, with what in the bowl. The caster is held by
 * reference (never looked up by UUID), so fake players in GameTests cast like anyone else.
 * Effects may {@link #giveBack} ingredients that should survive the casting (a collar, say);
 * everything else in the bowl is consumed when the spell succeeds.
 */
public final class BowlCast {

    private final ServerLevel level;
    private final BlockPos bowl;
    private final ServerPlayer caster;
    private final BowlContents contents;
    private final BowlSpellRecipe recipe;
    private final List<ItemStack> returned = new ArrayList<>();
    private @Nullable Vec3 destination;
    private @Nullable Entity destinationEntity;

    public BowlCast(ServerLevel level, BlockPos bowl, ServerPlayer caster, BowlContents contents, BowlSpellRecipe recipe) {
        this.level = level;
        this.bowl = bowl;
        this.caster = caster;
        this.contents = contents;
        this.recipe = recipe;
    }

    public ServerLevel level() {
        return level;
    }

    public BlockPos bowl() {
        return bowl;
    }

    /** Just above the liquid's surface: where smoke rises from. */
    public Vec3 surface() {
        return Vec3.atBottomCenterOf(bowl).add(0, 0.45, 0);
    }

    public ServerPlayer caster() {
        return caster;
    }

    public BowlContents contents() {
        return contents;
    }

    public BowlSpellRecipe recipe() {
        return recipe;
    }

    /** The first ingredient that is {@code item}, or empty. */
    public ItemStack find(Item item) {
        return contents.stacks().stream().filter(s -> s.is(item)).findFirst().orElse(ItemStack.EMPTY);
    }

    /** Whose blood is in the bowl, if any. */
    public Optional<Dose> blood() {
        return contents.liquids().stream().filter(d -> d.kind() == BowlLiquid.BLOOD && d.owner().isPresent()).findFirst();
    }

    public Optional<UUID> bloodOwner() {
        return blood().flatMap(Dose::owner);
    }

    /**
     * Whom the spell is for (v0.21, {@link PersonalComponent}): whose blood was poured in, else the first personal ingredient (a
     * taglock, a vial of bound blood).
     */
    public Optional<Taglock> target() {
        for (Dose d : contents.liquids()) {
            Optional<Taglock> t = PersonalComponent.resolve(d);
            if (t.isPresent()) return t;
        }
        for (ItemStack s : contents.stacks()) {
            Optional<Taglock> t = PersonalComponent.resolve(s);
            if (t.isPresent()) return t;
        }
        return Optional.empty();
    }

    /**
     * The spell goes off toward {@code destination} (v0.21): once it has worked, the bowl shows its seal flying there
     * ({@code MagicFx.bowlSend}). Effects that reach a distant target call it.
     */
    public void sendTo(Vec3 destination, @Nullable Entity target) {
        this.destination = destination;
        this.destinationEntity = target;
    }

    public @Nullable Vec3 destination() {
        return destination;
    }

    public @Nullable Entity destinationEntity() {
        return destinationEntity;
    }

    /** Hands {@code stack} back to the caster once the spell has worked. */
    public void giveBack(ItemStack stack) {
        if (!stack.isEmpty()) returned.add(stack.copy());
    }

    public List<ItemStack> returned() {
        return List.copyOf(returned);
    }
}
