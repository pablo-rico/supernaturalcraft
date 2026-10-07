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

    /** Hands {@code stack} back to the caster once the spell has worked. */
    public void giveBack(ItemStack stack) {
        if (!stack.isEmpty()) returned.add(stack.copy());
    }

    public List<ItemStack> returned() {
        return List.copyOf(returned);
    }
}
