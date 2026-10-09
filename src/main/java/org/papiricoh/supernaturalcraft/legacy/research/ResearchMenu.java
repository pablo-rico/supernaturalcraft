package org.papiricoh.supernaturalcraft.legacy.research;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.papiricoh.supernaturalcraft.registry.AllBlocks;
import org.papiricoh.supernaturalcraft.registry.AllMenus;

/**
 * A research desk's menu (v0.17), in the pattern of {@code HellforgeMenu}: the server validates every action
 * ({@code ResearchActionPayload}); the client draws it in {@code client.legacy.ResearchScreen}. The board is sent by {@code ResearchService.sendBoard} when it opens and after every action;
 * research belongs to the hunter, so the menu holds no items.
 */
public class ResearchMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;

    public ResearchMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public ResearchMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(AllMenus.RESEARCH.get(), id);
        this.access = access;
    }

    public ContainerLevelAccess access() {
        return access;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return org.papiricoh.supernaturalcraft.legacy.Legacies.member(player) && stillValid(access, player, AllBlocks.RESEARCH_DESK.get());
    }
}
