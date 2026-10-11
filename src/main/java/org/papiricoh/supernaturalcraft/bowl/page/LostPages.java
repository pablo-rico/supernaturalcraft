package org.papiricoh.supernaturalcraft.bowl.page;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.author.AuthorSavedData;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllItems;

/**
 * Spell pages handed out only once (v0.19): Summon the Messenger (heeding him) and Find the Author (beating every great enemy).
 * A hunter who earned one, has not learnt its rite and no longer has the page (inventory or ender chest) is given another on
 * login or respawn, at most once a day, so a page dropped in lava never ends the road.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class LostPages {

    public static final ResourceLocation MESSENGER = SupernaturalCraft.asResource("summon_messenger");
    public static final ResourceLocation AUTHOR = SupernaturalCraft.asResource("find_the_author");
    private static final String KEY = "supernaturalcraft_page_reissued";
    /** Game ticks between two reissues of the same page. */
    public static final long COOLDOWN = 24000;

    private LostPages() {
    }

    /** @return how many pages were given back */
    public static int check(ServerPlayer p) {
        int n = 0;
        if (has(p, "main/heeded_the_call") && reissue(p, MESSENGER)) n++;
        if (AuthorSavedData.get(p.serverLevel()).paged(p.getUUID()) && AuthorWorld.readyForHim(p) && reissue(p, AUTHOR)) n++;
        if (authorMap(p)) n++;
        return n;
    }

    /**
     * Once the Author is expected ({@link AuthorSavedData#spellCast()}), a hunter on their way to him who has no map to his cabin
     * gets one: one cast of the spell (and its Fallen Star) shows the way for good.
     */
    private static boolean authorMap(ServerPlayer p) {
        if (p.getServer() == null) return false;
        net.minecraft.server.level.ServerLevel overworld = p.getServer().overworld();
        if (!AuthorSavedData.get(overworld).spellCast() || !AuthorWorld.readyForHim(p) || has(p, "main/the_end")) return false;
        if (holdsMap(p.getInventory()) || holdsMap(p.getEnderChestInventory()) || !due(p, "author_map")) return false;
        ItemStack map = AuthorWorld.map(overworld, org.papiricoh.supernaturalcraft.author.AuthorSite.of(overworld));
        if (!p.getInventory().add(map)) p.drop(map, false);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.page.reissued", map.getHoverName())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        return true;
    }

    private static boolean holdsMap(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            var name = c.getItem(i).get(net.minecraft.core.component.DataComponents.ITEM_NAME);
            if (name != null && name.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().equals(AuthorWorld.MAP_NAME)) return true;
        }
        return false;
    }

    /** Whether {@code what} may be handed out again (not within a day of the last time), marking it handed out if so. */
    private static boolean due(ServerPlayer p, String what) {
        CompoundTag data = p.getPersistentData().getCompound(KEY);
        long now = p.serverLevel().getGameTime();
        if (data.contains(what) && now - data.getLong(what) < COOLDOWN) return false;
        data.putLong(what, now);
        p.getPersistentData().put(KEY, data);
        return true;
    }

    private static boolean reissue(ServerPlayer p, ResourceLocation spell) {
        if (ManaManager.get(p).knowsRite(spell) || carries(p, spell) || !due(p, spell.toString())) return false;
        ItemStack page = SpellPageItem.of(spell);
        if (!p.getInventory().add(page)) p.drop(page, false);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.page.reissued", page.getHoverName())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        return true;
    }

    /** Whether the page is in the inventory or the ender chest. */
    public static boolean carries(ServerPlayer p, ResourceLocation spell) {
        return holds(p.getInventory(), spell) || holds(p.getEnderChestInventory(), spell);
    }

    private static boolean holds(Container c, ResourceLocation spell) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.is(AllItems.SPELL_PAGE.get()) && spell.equals(s.get(org.papiricoh.supernaturalcraft.registry.AllDataComponents.BOWL_SPELL.get()))) return true;
        }
        return false;
    }

    private static boolean has(ServerPlayer p, String advancement) {
        if (p.getServer() == null) return false;
        AdvancementHolder adv = p.getServer().getAdvancements().get(SupernaturalCraft.asResource(advancement));
        return adv != null && p.getAdvancements().getOrStartProgress(adv).isDone();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) check(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !event.isEndConquered()) check(p);
    }
}
