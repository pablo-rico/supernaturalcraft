package org.papiricoh.supernaturalcraft.interrogation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.AllegianceDialogue;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.interrogation.InterrogationRules.Tool;
import org.papiricoh.supernaturalcraft.network.InterrogationPayload;
import org.papiricoh.supernaturalcraft.network.Send;
import org.papiricoh.supernaturalcraft.prophecy.Prophecies;
import org.papiricoh.supernaturalcraft.prophecy.Tablet;
import org.papiricoh.supernaturalcraft.prophecy.WordCipher;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.reward.ChorusRewards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interrogating a demon or angel held by a trap (v0.36; the arithmetic in {@link InterrogationRules}). Each right click on
 * it is a press with what is in hand ({@link #tool}); its answers come in the action bar and its resistance as a bar on the
 * HUD. Broken, it offers what it knows ({@link #DIALOGUE}): the way to the nearest Enochian crypt (a map), what it has heard
 * of a prince of Hell or Heaven, and — an angel — a few letters of the Word. Then it is gone. Pushed too far, it dies first.
 */
public final class Interrogation {

    public static final String DIALOGUE = "interrogate";
    public static final int TIMEOUT = 600, CONFESSION_LINES = 8, ANGEL_LETTERS = 3;
    public static final TagKey<Structure> CRYPTS = TagKey.create(Registries.STRUCTURE, SupernaturalCraft.asResource("enochian_crypts"));

    /** One interrogation under way. */
    private static final class Session {
        final int entity;
        int resistance = InterrogationRules.RESISTANCE;
        Tool last;
        int repeats;

        Session(int entity) {
            this.entity = entity;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    private Interrogation() {
    }

    public static void registerDialogue() {
        AllegianceDialogue.register(DIALOGUE, Interrogation::answer);
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
    }

    /** Whether {@code e} can be interrogated now. */
    public static boolean subject(Entity e) {
        return e instanceof Mob m && m.isAlive() && (Kin.isDemon(m) || Kin.isAngel(m)) && !m.getType().is(AllTags.Entities.BOSSES)
                && m.hasEffect(AllMobEffects.TRAPPED);
    }

    /** What the item in hand presses with, for this creature; null if it is no tool here. */
    @Nullable
    public static Tool tool(ItemStack stack, boolean angel) {
        if (stack.isEmpty()) return Tool.WORDS;
        if (stack.is(Items.BOOK) || stack.is(Items.WRITTEN_BOOK) || stack.is(Items.WRITABLE_BOOK)) return Tool.FAITH;
        if (angel) {
            if (stack.is(AllItems.HOLY_OIL.get())) return Tool.PAIN;
            if (stack.is(AllTags.Items.ANGELIC_BLADES)) return Tool.STEEL;
        } else {
            if (stack.is(AllItems.HOLY_WATER.get())) return Tool.PAIN;
            if (stack.is(ItemTags.SWORDS)) return Tool.STEEL;
        }
        return null;
    }

    /** A press. @return whether it was taken as one (the click is then spent) */
    public static boolean press(ServerPlayer sp, Mob subject, InteractionHand hand) {
        boolean angel = Kin.isAngel(subject);
        ItemStack stack = sp.getItemInHand(hand);
        Tool tool = tool(stack, angel);
        if (tool == null || !subject(subject)) return false;
        if (!org.papiricoh.supernaturalcraft.network.RateLimiter.allow(sp, "interrogate", 1.5, 2)) return true;
        Session s = SESSIONS.compute(sp.getUUID(), (k, old) -> old != null && old.entity == subject.getId() ? old : new Session(subject.getId()));
        s.repeats = tool == s.last ? s.repeats + 1 : 0;
        s.last = tool;
        Tool weak = InterrogationRules.weakness(subject.getUUID().getLeastSignificantBits());
        s.resistance = Math.max(0, s.resistance - InterrogationRules.wear(tool, weak, s.repeats));
        ServerLevel level = sp.serverLevel();
        float strain = InterrogationRules.strain(tool);
        if (strain > 0) {
            subject.setHealth(Math.max(0.5f, subject.getHealth() - subject.getMaxHealth() * strain));
            subject.hurtMarked = true;
            level.sendParticles(angel ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.LARGE_SMOKE, subject.getX(), subject.getEyeY(), subject.getZ(), 12, 0.2, 0.3, 0.2, 0.03);
            level.playSound(null, subject.blockPosition(), angel ? SoundEvents.BLAZE_HURT : SoundEvents.ZOMBIE_VILLAGER_HURT, SoundSource.HOSTILE, 1f, angel ? 1.5f : 0.7f);
        }
        if (tool == Tool.PAIN && !sp.getAbilities().instabuild) stack.shrink(1);
        String kind = angel ? "angel" : "demon";
        if (InterrogationRules.overdone(subject.getHealth() / subject.getMaxHealth())) {
            SESSIONS.remove(sp.getUUID());
            sp.displayClientMessage(Component.translatable("interrogation.supernaturalcraft." + kind + ".lost").withStyle(ChatFormatting.DARK_GRAY), true);
            Send.toPlayer(sp, new InterrogationPayload(subject.getId(), s.resistance, InterrogationPayload.LOST));
            gone(level, subject);
            return true;
        }
        if (s.resistance <= 0) {
            SESSIONS.remove(sp.getUUID());
            Send.toPlayer(sp, new InterrogationPayload(subject.getId(), 0, InterrogationPayload.BROKEN));
            AllegianceDialogue.ask(sp, subject, DIALOGUE, kind, options(angel), TIMEOUT);
            ChorusRewards.award(sp, "main/first_confession");
            return true;
        }
        int band = InterrogationRules.band(s.resistance);
        // Its weak point shows: it flinches in a way the other tools never get from it.
        String line = tool == weak ? "weak" : tool.name().toLowerCase(java.util.Locale.ROOT);
        sp.displayClientMessage(Component.translatable("interrogation.supernaturalcraft." + kind + "." + line + "." + band)
                .withStyle(angel ? ChatFormatting.AQUA : ChatFormatting.RED), true);
        Send.toPlayer(sp, new InterrogationPayload(subject.getId(), s.resistance, InterrogationPayload.ONGOING));
        return true;
    }

    static List<String> options(boolean angel) {
        List<String> out = new ArrayList<>(List.of("crypt", "boss"));
        if (angel) out.add(1, "word");
        return out;
    }

    private static void answer(ServerPlayer sp, @Nullable Entity speaker, String node, String option) {
        switch (option) {
            case "crypt" -> giveCryptMap(sp, speaker != null ? speaker.blockPosition() : sp.blockPosition());
            case "word" -> {
                int letters = WordCipher.lettersFrom(sp.level().getGameTime() ^ sp.getUUID().getMostSignificantBits(), ANGEL_LETTERS);
                Prophecies.update(sp, p -> p.withKnown(Tablet.ANGEL, p.known(Tablet.ANGEL) | letters));
                sp.displayClientMessage(Component.translatable("interrogation.supernaturalcraft.confess.word", WordCipher.letters(letters))
                        .withStyle(ChatFormatting.AQUA), false);
            }
            default -> {
                int line = Math.floorMod(sp.getRandom().nextInt(), CONFESSION_LINES);
                sp.displayClientMessage(Component.translatable("interrogation.supernaturalcraft.confess.boss." + line).withStyle(ChatFormatting.GOLD), false);
            }
        }
        if (speaker instanceof Mob m && m.level() instanceof ServerLevel level) gone(level, m);
    }

    /** A map with the nearest Enochian crypt marked, or word that it knows of none near. */
    static void giveCryptMap(ServerPlayer sp, BlockPos from) {
        ServerLevel level = sp.serverLevel();
        BlockPos crypt = level.findNearestMapStructure(CRYPTS, from, 100, false);
        if (crypt == null) {
            sp.displayClientMessage(Component.translatable("interrogation.supernaturalcraft.confess.no_crypt").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        ItemStack map = MapItem.create(level, crypt.getX(), crypt.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, crypt, "+", MapDecorationTypes.RED_X);
        map.set(net.minecraft.core.component.DataComponents.ITEM_NAME, Component.translatable("item.supernaturalcraft.crypt_map"));
        if (!sp.getInventory().add(map)) sp.drop(map, false);
        sp.displayClientMessage(Component.translatable("interrogation.supernaturalcraft.confess.crypt").withStyle(ChatFormatting.GOLD), false);
    }

    /** A broken (or broken too far) creature leaves: smoke for a demon, light for an angel. */
    static void gone(ServerLevel level, LivingEntity e) {
        boolean angel = Kin.isAngel(e);
        level.sendParticles(angel ? ParticleTypes.END_ROD : ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 1, e.getZ(), 40, 0.3, 0.8, 0.3, 0.06);
        level.playSound(null, e.blockPosition(), angel ? SoundEvents.BEACON_DEACTIVATE : SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1f, 0.8f);
        e.discard();
    }
}
