package org.papiricoh.supernaturalcraft.legacy.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.legacy.Archive;
import org.papiricoh.supernaturalcraft.legacy.Legacies;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Field notes filed straight into a member's {@link Archive} (v0.19) instead of carried: kills, bosses, solved cases and the
 * chests a member opens file theirs here, and loose notes (older worlds, chests opened by others) are filed when a member picks
 * them up, uses them, opens a research desk or closes Henry's case on them. Outsiders keep them as items.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class NoteLedger {

    private NoteLedger() {
    }

    /** Files {@code n} notes on {@code topic} for a member, with a word on the action bar. */
    public static boolean file(ServerPlayer player, String topic, int n) {
        return file(player, Map.of(topic, n));
    }

    /**
     * Files notes (topic → how many) for a member, with one word on the action bar for them all.
     *
     * @return false (and nothing filed) for an outsider or nothing to file
     */
    public static boolean file(ServerPlayer player, Map<String, Integer> notes) {
        if (!Legacies.member(player)) return false;
        Map<String, Integer> clean = new LinkedHashMap<>();
        notes.forEach((t, n) -> {
            if (t != null && !t.isEmpty() && n != null && n > 0) clean.merge(t, n, Integer::sum);
        });
        if (clean.isEmpty()) return false;
        Legacies.updateArchive(player, a -> {
            for (var e : clean.entrySet()) a = a.withNotes(e.getKey(), e.getValue());
            return a;
        });
        Archive now = Legacies.archive(player);
        MutableComponent list = Component.empty();
        boolean first = true;
        for (var e : clean.entrySet()) {
            if (!first) list.append(Component.literal(", "));
            first = false;
            list.append(Component.translatable("message.supernaturalcraft.research.notes_entry", name(e.getKey()), e.getValue(),
                    now.notes(e.getKey())));
        }
        player.displayClientMessage(Component.translatable("message.supernaturalcraft.research.notes_filed", list)
                .withStyle(ChatFormatting.DARK_AQUA), true);
        player.level().playSound(null, player.blockPosition(), AllSounds.LEGACY_PAPER.get(), SoundSource.PLAYERS, 0.5f, 1.2f);
        return true;
    }

    /** The display name of notes on {@code topic} ("Field Notes: Zombie"). */
    public static Component name(String topic) {
        return FieldNotesItem.stack(topic, 1).getHoverName();
    }

    /** Whether a stack is field notes on some topic. */
    public static boolean isNotes(ItemStack stack) {
        return stack.is(AllItems.FIELD_NOTES.get()) && !FieldNotesItem.topic(stack).isEmpty();
    }

    /** Files a stack of loose notes for a member and empties it. */
    public static boolean absorb(ServerPlayer player, ItemStack stack) {
        if (!isNotes(stack) || !file(player, FieldNotesItem.topic(stack), stack.getCount())) return false;
        stack.setCount(0);
        return true;
    }

    /** Files every loose note a member carries, in one go. */
    public static int absorbInventory(ServerPlayer player) {
        if (!Legacies.member(player)) return 0;
        Map<String, Integer> found = new LinkedHashMap<>();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (isNotes(s)) found.merge(FieldNotesItem.topic(s), s.getCount(), Integer::sum);
        }
        if (found.isEmpty() || !file(player, found)) return 0;
        for (int i = 0; i < inv.getContainerSize(); i++) if (isNotes(inv.getItem(i))) inv.setItem(i, ItemStack.EMPTY);
        inv.setChanged();
        return found.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Takes the field notes out of {@code loot} and files them for a member; an outsider's loot is left as it is. */
    public static void absorbLoot(ServerPlayer player, List<ItemStack> loot) {
        if (!Legacies.member(player)) return;
        Map<String, Integer> found = new LinkedHashMap<>();
        for (ItemStack s : loot) if (isNotes(s)) found.merge(FieldNotesItem.topic(s), s.getCount(), Integer::sum);
        if (found.isEmpty() || !file(player, found)) return;
        loot.removeIf(NoteLedger::isNotes);
    }

    /** A member walking over loose notes files them instead of pocketing them. */
    public static boolean pickUp(ServerPlayer player, ItemEntity item) {
        ItemStack s = item.getItem();
        if (!isNotes(s) || item.hasPickUpDelay() || (item.getTarget() != null && !item.getTarget().equals(player.getUUID()))) return false;
        int n = s.getCount();
        if (!file(player, FieldNotesItem.topic(s), n)) return false;
        player.take(item, n);
        item.discard();
        return true;
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (event.getPlayer() instanceof ServerPlayer sp && !event.getItemEntity().isRemoved() && pickUp(sp, event.getItemEntity())) {
            event.setCanPickup(TriState.FALSE);
        }
    }
}
