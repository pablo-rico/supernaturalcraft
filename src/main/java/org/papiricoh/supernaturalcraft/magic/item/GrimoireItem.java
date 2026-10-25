package org.papiricoh.supernaturalcraft.magic.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.client.SNClientHooks;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.magic.spell.SigilComponent;
import org.papiricoh.supernaturalcraft.magic.spell.Spell;
import org.papiricoh.supernaturalcraft.magic.spell.SpellBook;
import org.papiricoh.supernaturalcraft.magic.spell.SpellCaster;
import org.papiricoh.supernaturalcraft.network.SNNetworking;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllSounds;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * The hunter's grimoire: six pages of composed spells. Use casts the open page; sneak-use opens
 * the Hunter's Book (dashboard, journal, scriptorium, roadmap); sneak + scroll wheel (or the cycle
 * key) turns the page.
 *
 * <p>Drawn as a GeckoLib journal (art in {@code tools/artgen/grimoire_art.py}; the GUI keeps the flat sprite). It lies closed
 * anywhere but a hand; in a hand (first or third person, so other players see it too) it opens and then breathes. Each
 * stack has its own GeckoLib id, so the opening replays whenever it comes back into a hand.
 */
public class GrimoireItem extends Item implements GeoItem {

    /** Sigils every new grimoire owner can already draw. */
    public static final List<ResourceLocation> STARTER_SIGILS = List.of(
            SupernaturalCraft.asResource("touch"), SupernaturalCraft.asResource("bolt"),
            SupernaturalCraft.asResource("smite"), SupernaturalCraft.asResource("mend"));

    private static final RawAnimation OPEN = RawAnimation.begin().thenPlay("animation.grimoire.open").thenLoop("animation.grimoire.idle");
    private static final RawAnimation CLOSED = RawAnimation.begin().thenLoop("animation.grimoire.closed");
    /** Ticks without being drawn in a hand after which the book counts as put away (and opens again when drawn). */
    private static final double PUT_AWAY_TICKS = 6;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GrimoireItem(Properties properties) {
        super(properties.stacksTo(1).component(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        // Its own animation instance: a book opening in one hand never opens another one in a frame or a chest.
        if (level instanceof ServerLevel server) GeoItem.getOrAssignId(stack, server);
    }

    static boolean inHand(@Nullable ItemDisplayContext context) {
        return context != null && (context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // One manager (and so one of these) per stack id: remembers when this book was last drawn in a hand.
        double[] lastHeld = {Double.NEGATIVE_INFINITY};
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            if (!inHand(state.getData(DataTickets.ITEM_RENDER_PERSPECTIVE))) {
                lastHeld[0] = Double.NEGATIVE_INFINITY;
                return state.setAndContinue(CLOSED);
            }
            Double tick = state.getData(DataTickets.TICK);
            double now = tick == null ? 0 : tick;
            if (now - lastHeld[0] > PUT_AWAY_TICKS) state.getController().forceAnimationReset();
            lastHeld[0] = now;
            return state.setAndContinue(OPEN);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static @Nullable InteractionHand heldHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof GrimoireItem) return InteractionHand.MAIN_HAND;
        if (player.getOffhandItem().getItem() instanceof GrimoireItem) return InteractionHand.OFF_HAND;
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) SNClientHooks.openBook();
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer sp) {
            teachStarterSigils(sp);
            SpellBook book = stack.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY);
            Spell spell = book.current();
            if (spell.isEmpty()) {
                sp.displayClientMessage(Component.translatable("message.supernaturalcraft.grimoire.empty_page")
                        .withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(stack);
            }
            SpellCaster.Result result = SpellCaster.cast(sp, spell, 1.0f, inkOf(stack, book.selected()));
            if (result.success()) player.swing(hand, true);
            return result.success() ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** The hexed ink page {@code page} was written in (v0.21), or {@code NONE}. */
    public static org.papiricoh.supernaturalcraft.magic.school.School inkOf(ItemStack grimoire, int page) {
        java.util.List<org.papiricoh.supernaturalcraft.magic.school.School> inks = grimoire.get(AllDataComponents.PAGE_INKS.get());
        return inks != null && page >= 0 && page < inks.size() ? inks.get(page) : org.papiricoh.supernaturalcraft.magic.school.School.NONE;
    }

    /** Records which ink page {@code page} was (re)written in (v0.21). */
    public static void setInk(ItemStack grimoire, int page, org.papiricoh.supernaturalcraft.magic.school.School ink) {
        java.util.List<org.papiricoh.supernaturalcraft.magic.school.School> inks = new java.util.ArrayList<>(
                grimoire.getOrDefault(AllDataComponents.PAGE_INKS.get(), java.util.List.of()));
        while (inks.size() < SpellBook.PAGES) inks.add(org.papiricoh.supernaturalcraft.magic.school.School.NONE);
        inks.set(page, ink == null ? org.papiricoh.supernaturalcraft.magic.school.School.NONE : ink);
        if (inks.stream().allMatch(org.papiricoh.supernaturalcraft.magic.school.School::isNone)) grimoire.remove(AllDataComponents.PAGE_INKS.get());
        else grimoire.set(AllDataComponents.PAGE_INKS.get(), java.util.List.copyOf(inks));
    }

    private static void teachStarterSigils(ServerPlayer player) {
        ArcanaData arcana = ManaManager.get(player);
        boolean taught = false;
        for (ResourceLocation id : STARTER_SIGILS) taught |= arcana.learn(id);
        if (taught) {
            player.displayClientMessage(Component.translatable("message.supernaturalcraft.grimoire.first_open")
                    .withStyle(ChatFormatting.GOLD), false);
            player.serverLevel().playSound(null, player.blockPosition(), AllSounds.SIGIL_LEARN.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
            SNNetworking.syncArcana(player);
        }
    }

    /** "Page 2: Holy Lance (Bolt · Smite · Empower)". */
    public static MutableComponent describe(Spell spell, int page) {
        MutableComponent text = Component.translatable("message.supernaturalcraft.grimoire.page", page + 1).withStyle(ChatFormatting.GOLD);
        if (spell.isEmpty()) {
            return text.append(Component.translatable("message.supernaturalcraft.grimoire.blank").withStyle(ChatFormatting.GRAY));
        }
        return text.append(spellName(spell).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                .append(sigilList(spell).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent spellName(Spell spell) {
        if (!spell.name().isBlank()) return Component.literal(spell.name());
        if (spell.form().isEmpty() || spell.effects().isEmpty()) return Component.translatable("spell.supernaturalcraft.unnamed");
        return Component.translatable("spell.supernaturalcraft.auto_name",
                org.papiricoh.supernaturalcraft.magic.spell.SigilLookup.name(org.papiricoh.supernaturalcraft.magic.spell.SigilLookup.local(), spell.form().get()),
                org.papiricoh.supernaturalcraft.magic.spell.SigilLookup.name(org.papiricoh.supernaturalcraft.magic.spell.SigilLookup.local(), spell.effects().getFirst()));
    }

    public static MutableComponent sigilList(Spell spell) {
        MutableComponent list = Component.empty();
        boolean first = true;
        for (ResourceLocation id : allSigils(spell)) {
            if (!first) list.append(" · ");
            list.append(org.papiricoh.supernaturalcraft.magic.spell.SigilLookup.name(org.papiricoh.supernaturalcraft.magic.spell.SigilLookup.local(), id));
            first = false;
        }
        return list;
    }

    public static List<ResourceLocation> allSigils(Spell spell) {
        java.util.ArrayList<ResourceLocation> all = new java.util.ArrayList<>();
        spell.form().ifPresent(all::add);
        all.addAll(spell.effects());
        all.addAll(spell.modifiers());
        return all;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        SpellBook book = stack.getOrDefault(AllDataComponents.SPELL_BOOK, SpellBook.EMPTY);
        tooltip.add(describe(book.current(), book.selected()));
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.grimoire.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
