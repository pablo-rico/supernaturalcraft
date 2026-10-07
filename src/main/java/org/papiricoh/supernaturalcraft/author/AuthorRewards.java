package org.papiricoh.supernaturalcraft.author;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression;
import org.papiricoh.supernaturalcraft.crossroads.CrossroadsDeal;
import org.papiricoh.supernaturalcraft.crossroads.Debts;
import org.papiricoh.supernaturalcraft.journal.HunterLog;
import org.papiricoh.supernaturalcraft.journal.HunterLogs;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What passing the Author's test gives (the facade the fight calls at its very end): to each hunter who fought it to
 * the end, the first time only, "The End" (their chronicle, {@link Chronicle}), the Author's Pen and Sam's amulet; to
 * all of them the final advancement. He goes home afterwards, and will offer "another draft" (no rewards).
 */
public final class AuthorRewards {

    /** Things the chronicle may mention having been held, in this order. */
    private static final List<Supplier<? extends Item>> TREASURES = List.of(AllItems.THE_COLT, AllItems.ARCHANGEL_BLADE,
            AllItems.LUCIFERS_GRACE, AllItems.SERAPH_WINGS, AllItems.ECLIPSE_SIGHT, AllItems.ANGEL_TABLET, AllItems.KEY_TO_THE_CAGE,
            AllItems.RING_OF_DEATH, AllItems.HOUND_WHISTLE, AllItems.FALLEN_STAR);

    private AuthorRewards() {
    }

    /** The test is passed: {@code hunters} fought to the end; {@code at} is where the cabin's door stands again. */
    public static void victory(ServerLevel level, List<ServerPlayer> hunters, Vec3 at, boolean rematch) {
        AuthorSavedData data = AuthorSavedData.get(level);
        for (ServerPlayer p : hunters) {
            ChorusRewards.award(p, "main/the_end");
            // The gifts are each hunter's first victory, whoever asked for the draft.
            if (!data.reward(p.getUUID())) {
                p.displayClientMessage(Component.translatable("message.supernaturalcraft.author.another_ending").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), false);
                continue;
            }
            give(p, manuscript(p));
            give(p, new ItemStack(AllItems.AUTHORS_PEN.get()));
            give(p, new ItemStack(AllItems.SAMS_AMULET.get()));
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.author.the_end").withStyle(ChatFormatting.GOLD), false);
        }
        level.playSound(null, at.x, at.y, at.z, AllSounds.CHUCK_BELL.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
    }

    /** The test is failed (everyone fell): "Let's try another draft". The cabin comes back with him in it. */
    public static void defeat(ServerLevel level, Vec3 at) {
        Component line = Component.translatable("message.supernaturalcraft.author.another_draft").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceTo(at) < 160) p.displayClientMessage(line, false);
        }
        level.playSound(null, at.x, at.y, at.z, AllSounds.CHUCK_CARRIAGE.get(), SoundSource.HOSTILE, 1.5f, 0.8f);
    }

    /** "The End", written for {@code p} from their log. */
    public static ItemStack manuscript(ServerPlayer p) {
        ItemStack book = new ItemStack(AllItems.THE_END_MANUSCRIPT.get());
        book.set(AllDataComponents.MANUSCRIPT.get(), new Manuscript(p.getGameProfile().getName(), Chronicle.write(facts(p))));
        return book;
    }

    /** What the Author read in {@code p}'s story. */
    public static Chronicle.Facts facts(ServerPlayer p) {
        HunterLog log = HunterLogs.get(p);
        List<String> bosses = new ArrayList<>();
        for (BossProgression.Boss b : BossProgression.Boss.values()) {
            if (b != BossProgression.Boss.CHUCK && AuthorWorld.done(p, b.advancement)) {
                bosses.add(Component.translatable("entity.supernaturalcraft." + b.entity).getString());
            }
        }
        int kills = 0, best = 0;
        String favourite = null;
        for (Map.Entry<ResourceLocation, Integer> e : log.kills().entrySet()) {
            kills += e.getValue();
            if (e.getValue() > best) {
                best = e.getValue();
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(e.getKey());
                favourite = type.getDescription().getString();
            }
        }
        List<String> treasures = new ArrayList<>();
        for (Supplier<? extends Item> t : TREASURES) {
            Item item = t.get();
            if (log.has(BuiltInRegistries.ITEM.getKey(item))) treasures.add(new ItemStack(item).getHoverName().getString());
        }
        int spells = ManaManager.get(p).rites().size();
        boolean dealt = Debts.get(p).state() != CrossroadsDeal.State.NONE;
        return new Chronicle.Facts(p.getGameProfile().getName(), bosses, kills, favourite, best, log.seen().size(), spells, dealt, treasures);
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }
}
