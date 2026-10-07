package org.papiricoh.supernaturalcraft.author;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.bowl.page.SpellPageItem;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;

/** The Author's world, once a second; Sam's amulet, every tick; the spell's page, when the last great enemy falls. */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class AuthorEvents {

    /** The spell the page teaches. */
    public static final ResourceLocation SPELL = SupernaturalCraft.asResource("find_the_author");

    private AuthorEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 20 != 7) return;
        AuthorWorld.tick(level, level.players());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) SamsAmulet.tick(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SamsAmulet.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Hunters who beat everything before the Author existed get their page too.
        if (event.getEntity() instanceof ServerPlayer p) offerPage(p);
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ResourceLocation id = event.getAdvancement().id();
        if (!id.getNamespace().equals(SupernaturalCraft.MODID)) return;
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            if (b != BossProgression.Boss.CHUCK && b.advancement.equals(id.getPath())) {
                offerPage(p);
                return;
            }
        }
    }

    /**
     * The page of "Find the Author", once, to a hunter who has beaten every great enemy before him.
     *
     * @return whether it was handed over now
     */
    public static boolean offerPage(ServerPlayer p) {
        AuthorSavedData data = AuthorSavedData.get(p.serverLevel());
        if (data.paged(p.getUUID()) || !AuthorWorld.readyForHim(p)) return false;
        data.page(p.getUUID());
        if (ManaManager.get(p).knowsRite(SPELL)) return false;
        ItemStack page = SpellPageItem.of(SPELL);
        if (!p.getInventory().add(page)) p.drop(page, false);
        p.displayClientMessage(Component.translatable("message.supernaturalcraft.author.page").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), false);
        return true;
    }
}
