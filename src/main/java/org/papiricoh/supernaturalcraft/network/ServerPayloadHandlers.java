package org.papiricoh.supernaturalcraft.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.papiricoh.supernaturalcraft.magic.item.GrimoireItem;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.SigilKind;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBook;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.registry.SNRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Server-side handling of client requests. Everything the client sends is re-validated here. */
public final class ServerPayloadHandlers {

    private ServerPayloadHandlers() {
    }

    public static void handleColtInput(ColtInputPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) org.papiricoh.supernaturalcraft.reward.ColtItem.input(player, payload.action());
    }

    public static void handleSelectSpell(SelectSpellPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        InteractionHand hand = GrimoireItem.heldHand(player);
        if (hand == null) return;
        ItemStack stack = player.getItemInHand(hand);
        SpellBook book = stack.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY).cycle(Integer.signum(payload.delta()));
        stack.set(AllDataComponents.SPELL_BOOK, book);
        player.displayClientMessage(GrimoireItem.describe(book.current(), book.selected()), true);
    }

    public static void handleCompose(ComposeSpellPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) compose(player, payload);
    }

    /** Writes a spell onto the grimoire in hand, or onto scrolls. */
    public static void compose(ServerPlayer player, ComposeSpellPayload payload) {
        InteractionHand hand = GrimoireItem.heldHand(player);
        if (hand == null || payload.page() < 0 || payload.page() >= SpellBook.PAGES) return;
        Spell spell = payload.spell();
        String problem = validate(player, spell);
        if (problem != null) {
            player.displayClientMessage(Component.translatable(problem).withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean creative = player.getAbilities().instabuild;
        if (payload.scroll()) {
            if (!spell.isComplete()) return;
            int count = payload.count();
            if (count < 1 || count > ComposeSpellPayload.MAX_SCROLLS) return;
            if (!creative && (player.getInventory().countItem(Items.PAPER) < count
                    || player.getInventory().countItem(AllItems.ENOCHIAN_INK.get()) < count)) {
                player.displayClientMessage(Component.translatable("message.supernaturalcraft.compose.need_paper_ink")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }
            if (!creative) {
                consume(player, Items.PAPER, count);
                consume(player, AllItems.ENOCHIAN_INK.get(), count);
            }
            ItemStack scroll = new ItemStack(AllItems.SPELL_SCROLL.get(), count);
            scroll.set(AllDataComponents.SCROLL_SPELL, spell);
            if (!player.getInventory().add(scroll)) player.drop(scroll, false);
        } else {
            ItemStack stack = player.getItemInHand(hand);
            SpellBook book = stack.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY);
            if (book.page(payload.page()).equals(spell)) return;
            // v0.21: hexed ink of the spell's own school is used first, and the page remembers it (a stronger spell).
            org.papiricoh.supernaturalcraft.magic.school.School ink = org.papiricoh.supernaturalcraft.magic.school.School.NONE;
            if (!spell.isEmpty()) {
                int hexed = hexedInkSlot(player, spellSchool(player, spell));
                if (hexed >= 0) {
                    ink = org.papiricoh.supernaturalcraft.magic.item.HexedInkItem.school(player.getInventory().getItem(hexed));
                    if (!creative) player.getInventory().getItem(hexed).shrink(1);
                } else if (!creative) {
                    if (player.getInventory().countItem(AllItems.ENOCHIAN_INK.get()) < 1) {
                        player.displayClientMessage(Component.translatable("message.supernaturalcraft.compose.need_ink")
                                .withStyle(ChatFormatting.RED), true);
                        return;
                    }
                    consumeOne(player, AllItems.ENOCHIAN_INK.get());
                }
            }
            stack.set(AllDataComponents.SPELL_BOOK, book.withPage(payload.page(), spell).select(payload.page()));
            GrimoireItem.setInk(stack, payload.page(), ink);
        }
        player.serverLevel().playSound(null, player.blockPosition(), AllSounds.SIGIL_LEARN.get(), SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    /** @return a translation key describing what is wrong, or null if the spell may be written. */
    static String validate(ServerPlayer player, Spell spell) {
        if (spell.isEmpty()) return null;
        Registry<SigilComponent> sigils = player.registryAccess().registryOrThrow(SNRegistries.SIGIL);
        ArcanaData arcana = ManaManager.get(player);
        if (spell.form().isEmpty() || spell.effects().isEmpty()) return "message.supernaturalcraft.compose.incomplete";
        if (!check(sigils, arcana, List.of(spell.form().get()), SigilKind.FORM, player)) return "message.supernaturalcraft.compose.unknown";
        if (!check(sigils, arcana, spell.effects(), SigilKind.EFFECT, player)) return "message.supernaturalcraft.compose.unknown";
        if (!check(sigils, arcana, spell.modifiers(), SigilKind.MODIFIER, player)) return "message.supernaturalcraft.compose.unknown";
        Set<ResourceLocation> distinct = new HashSet<>(spell.effects());
        if (distinct.size() != spell.effects().size()) return "message.supernaturalcraft.compose.duplicate";
        return null;
    }

    private static boolean check(Registry<SigilComponent> sigils, ArcanaData arcana, List<ResourceLocation> ids,
                                 SigilKind kind, ServerPlayer player) {
        for (ResourceLocation id : ids) {
            // Registered sigils and the hunter's own generated formulas (v0.17).
            SigilComponent s = org.papiricoh.supernaturalcraft.magic.spell.SigilLookup.get(player.registryAccess(),
                    org.papiricoh.supernaturalcraft.legacy.Legacies.archive(player), id);
            if (s == null || s.kind() != kind) return false;
            if (!arcana.knows(id) && !player.getAbilities().instabuild) return false;
        }
        return true;
    }

    /** The school of a spell's first effect (v0.21), or NONE. */
    static org.papiricoh.supernaturalcraft.magic.school.School spellSchool(ServerPlayer player, Spell spell) {
        if (spell.effects().isEmpty()) return org.papiricoh.supernaturalcraft.magic.school.School.NONE;
        SigilComponent s = org.papiricoh.supernaturalcraft.magic.spell.SigilLookup.get(player.registryAccess(),
                org.papiricoh.supernaturalcraft.legacy.Legacies.archive(player), spell.effects().getFirst());
        return s == null ? org.papiricoh.supernaturalcraft.magic.school.School.NONE : s.school();
    }

    /** The inventory slot of a hexed ink of {@code school}, or -1 (never for NONE). */
    static int hexedInkSlot(ServerPlayer player, org.papiricoh.supernaturalcraft.magic.school.School school) {
        if (school.isNone()) return -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(AllItems.HEXED_INK.get()) && org.papiricoh.supernaturalcraft.magic.item.HexedInkItem.school(s) == school) return i;
        }
        return -1;
    }

    private static void consumeOne(ServerPlayer player, net.minecraft.world.item.Item item) {
        consume(player, item, 1);
    }

    private static void consume(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
        for (int i = 0; i < player.getInventory().getContainerSize() && count > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) {
                int take = Math.min(count, s.getCount());
                s.shrink(take);
                count -= take;
            }
        }
    }

    /** Saves a design to the library (or clears its slot). Only sigils the hunter knows may be saved. */
    public static void handleLibraryEdit(LibraryEditPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) libraryEdit(player, payload);
    }

    public static void libraryEdit(ServerPlayer player, LibraryEditPayload payload) {
        Spell spell = payload.spell();
        String problem = spell.isEmpty() ? null : validateDesign(player, spell);
        if (problem != null) {
            player.displayClientMessage(Component.translatable(problem).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (org.papiricoh.supernaturalcraft.journal.HunterLogs.get(player).setDesign(payload.slot(), spell)) {
            org.papiricoh.supernaturalcraft.journal.HunterLogs.syncLibrary(player);
        }
    }

    /** Like {@link #validate}, but a design may still be unfinished (no form, or no effect yet). */
    static String validateDesign(ServerPlayer player, Spell spell) {
        Registry<SigilComponent> sigils = player.registryAccess().registryOrThrow(SNRegistries.SIGIL);
        ArcanaData arcana = ManaManager.get(player);
        if (spell.form().isPresent() && !check(sigils, arcana, List.of(spell.form().get()), SigilKind.FORM, player)) {
            return "message.supernaturalcraft.compose.unknown";
        }
        if (!check(sigils, arcana, spell.effects(), SigilKind.EFFECT, player)) return "message.supernaturalcraft.compose.unknown";
        if (!check(sigils, arcana, spell.modifiers(), SigilKind.MODIFIER, player)) return "message.supernaturalcraft.compose.unknown";
        if (new HashSet<>(spell.effects()).size() != spell.effects().size()) return "message.supernaturalcraft.compose.duplicate";
        return null;
    }

    /** An entry read, or a bookmark toggled. */
    public static void handleJournalAction(JournalActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && RateLimiter.allow(player, "journal", 10, 20)) journalAction(player, payload);
    }

    public static void journalAction(ServerPlayer player, JournalActionPayload payload) {
        // The client names the entry: only the mod's own ids are kept (v0.19).
        if (!payload.entry().getNamespace().equals(org.papiricoh.supernaturalcraft.SupernaturalCraft.MODID)
                || payload.entry().getPath().length() > 96) return;
        var log = org.papiricoh.supernaturalcraft.journal.HunterLogs.get(player);
        switch (payload.action()) {
            case JournalActionPayload.MARK_READ -> log.markRead(payload.entry());
            case JournalActionPayload.TOGGLE_BOOKMARK -> log.toggleBookmark(payload.entry());
            default -> {
            }
        }
    }
}
