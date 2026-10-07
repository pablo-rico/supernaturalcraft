package org.papiricoh.supernaturalcraft.weapon.forge;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMenus;
import org.papiricoh.supernaturalcraft.weapon.Rune;
import org.papiricoh.supernaturalcraft.weapon.RuneItem;
import org.papiricoh.supernaturalcraft.weapon.RuneSet;
import org.papiricoh.supernaturalcraft.weapon.WeaponProfile;
import org.papiricoh.supernaturalcraft.weapon.WeaponProfiles;

import java.util.ArrayList;
import java.util.List;

/**
 * Weapon in slot 0, up to four runes beside it (as many as the weapon has free rune slots).
 * Button 0 graves the runes for {@code tier × 2} levels each; button 1 purges the weapon for one
 * level, returning every rune but one, which is lost.
 */
public class HellforgeMenu extends AbstractContainerMenu {

    public static final int WEAPON = 0, RUNE_SLOTS = 4, INSCRIBE = 0, PURGE = 1, PURGE_COST = 1;

    private final Container forge = new SimpleContainer(1 + RUNE_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ContainerLevelAccess access;
    private final Player player;

    public HellforgeMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public HellforgeMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(AllMenus.HELLFORGE.get(), id);
        this.access = access;
        this.player = inv.player;
        addSlot(new Slot(forge, WEAPON, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WeaponProfiles.of(stack) != null;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int i = 0; i < RUNE_SLOTS; i++) {
            int index = i;
            addSlot(new Slot(forge, 1 + i, 62 + i * 20, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof RuneItem r && r.rune() != null;
                }

                @Override
                public boolean isActive() {
                    return index < freeSlots();
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 142));
    }

    public ItemStack weapon() {
        return forge.getItem(WEAPON);
    }

    public @Nullable WeaponProfile profile() {
        return WeaponProfiles.of(weapon());
    }

    public RuneSet graved() {
        return weapon().getOrDefault(AllDataComponents.RUNES, RuneSet.EMPTY);
    }

    /** Rune slots the weapon still has room for. */
    public int freeSlots() {
        WeaponProfile p = profile();
        return p == null ? 0 : Math.max(0, Math.min(RUNE_SLOTS, p.runeSlots() - graved().runes().size()));
    }

    public List<Rune> pending() {
        List<Rune> out = new ArrayList<>();
        for (int i = 0; i < RUNE_SLOTS; i++) {
            if (forge.getItem(1 + i).getItem() instanceof RuneItem r && r.rune() != null) out.add(r.rune());
        }
        return out;
    }

    public int inscribeCost() {
        WeaponProfile p = profile();
        return p == null ? 0 : p.tier() * 2 * pending().size();
    }

    /** Why the pending runes can't be graved, or null if they can. */
    public @Nullable String inscribeProblem() {
        WeaponProfile p = profile();
        if (p == null) return "no_weapon";
        if (pending().isEmpty()) return "no_runes";
        RuneSet set = graved();
        for (Rune r : pending()) {
            String why = set.rejection(r, p);
            if (why != null) return why;
            set = set.with(r);
        }
        if (!player.getAbilities().instabuild && player.experienceLevel < inscribeCost()) return "no_xp";
        return null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == INSCRIBE) return inscribe();
        if (id == PURGE) return purge();
        return false;
    }

    private boolean inscribe() {
        if (inscribeProblem() != null) return false;
        RuneSet set = graved();
        for (Rune r : pending()) set = set.with(r);
        if (!player.getAbilities().instabuild) player.giveExperienceLevels(-inscribeCost());
        weapon().set(AllDataComponents.RUNES, set);
        for (int i = 0; i < RUNE_SLOTS; i++) forge.setItem(1 + i, ItemStack.EMPTY);
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1f, 0.7f));
        broadcastChanges();
        return true;
    }

    private boolean purge() {
        RuneSet set = graved();
        if (set.runes().isEmpty() || (!player.getAbilities().instabuild && player.experienceLevel < PURGE_COST)) return false;
        if (!player.getAbilities().instabuild) player.giveExperienceLevels(-PURGE_COST);
        for (Rune r : set.survivorsOfPurge(player.getRandom())) {
            ItemStack back = new ItemStack(AllItems.RUNES.get(r).get());
            if (!player.getInventory().add(back)) player.drop(back, false);
        }
        weapon().remove(AllDataComponents.RUNES);
        access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1f, 0.6f));
        broadcastChanges();
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, forge));
        if (access == ContainerLevelAccess.NULL) clearContainer(player, forge);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, AllBlocks.HELLFORGE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int forgeEnd = 1 + RUNE_SLOTS, invEnd = slots.size();
        if (index < forgeEnd) {
            if (!moveItemStackTo(stack, forgeEnd, invEnd, true)) return ItemStack.EMPTY;
        } else if (WeaponProfiles.of(stack) != null && !slots.get(WEAPON).hasItem()) {
            if (!moveItemStackTo(stack, WEAPON, WEAPON + 1, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof RuneItem) {
            if (!moveItemStackTo(stack, 1, forgeEnd, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** Test hook: the forge's internal container. */
    public Container container() {
        return forge;
    }
}
