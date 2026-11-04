package org.papiricoh.supernaturalcraft.prophecy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Kin;
import org.papiricoh.supernaturalcraft.hell.HellDimension;
import org.papiricoh.supernaturalcraft.interrogation.Interrogation;
import org.papiricoh.supernaturalcraft.prophecy.trials.GateSeals;
import org.papiricoh.supernaturalcraft.prophecy.trials.TrialRules;
import org.papiricoh.supernaturalcraft.prophecy.trials.Trials;
import org.papiricoh.supernaturalcraft.registry.AllEntities;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;
import org.papiricoh.supernaturalcraft.util.WorldClock;

/**
 * The Word's hooks in the world (v0.36): interrogation and the cure's injections on a right click, the Trials' drops and
 * crossings, their toll on the hunter, the sealed ground, and a prophet's visions on waking.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class WordEvents {

    public static final int TOLL_TICKS = 100;
    /** A prophet's vision: how many letters of a tablet it shows. */
    public static final int VISION_LETTERS = 2, VISION_LINES = 6;
    static final String VISION_DAY = "SNVisionDay";

    private WordEvents() {
    }

    /** At mod construction. */
    public static void bootstrap() {
        Interrogation.registerDialogue();
        Trials.registerDialogue();
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer sp) || !(event.getTarget() instanceof Mob mob)) return;
        ItemStack held = sp.getMainHandItem();
        boolean done = held.is(AllItems.PURIFIED_BLOOD.get()) ? Trials.inject(sp, mob, held)
                : Interrogation.subject(mob) && Interrogation.press(sp, mob, InteractionHand.MAIN_HAND);
        if (done) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp) || !(event.getEntity().level() instanceof ServerLevel level)) return;
        int trial = Prophecies.get(sp).trial();
        var dead = event.getEntity();
        ItemStack drop = ItemStack.EMPTY;
        if (trial == TrialRules.HOUND && dead.getType() == AllEntities.HELLHOUND.get() && !sp.getInventory().contains(new ItemStack(AllItems.HELLHOUND_BLOOD.get()))) {
            drop = new ItemStack(AllItems.HELLHOUND_BLOOD.get());
        } else if (trial == TrialRules.SOUL && level.dimension() == HellDimension.LEVEL && Kin.isDemon(dead)
                && !dead.getType().is(AllTags.Entities.BOSSES) && !sp.getInventory().contains(new ItemStack(AllItems.INNOCENT_SOUL.get()))) {
            // The soul it was tormenting, freed.
            drop = new ItemStack(AllItems.INNOCENT_SOUL.get());
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.trials.soul_found").withStyle(ChatFormatting.GOLD), false);
        }
        if (!drop.isEmpty()) event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY() + 0.5, dead.getZ(), drop));
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || event.getFrom() != HellDimension.LEVEL || event.getTo() == HellDimension.LEVEL) return;
        if (Prophecies.get(sp).trial() != TrialRules.SOUL) return;
        for (ItemStack s : sp.getInventory().items) {
            if (s.is(AllItems.INNOCENT_SOUL.get())) {
                s.shrink(1);
                Trials.advance(sp, TrialRules.SOUL);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || (sp.tickCount + sp.getId()) % TOLL_TICKS != 0) return;
        if (Prophecies.get(sp).trial() != TrialRules.NONE) Trials.applyToll(sp);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && Prophecies.get(sp).trial() != TrialRules.NONE) Trials.applyToll(sp);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && Prophecies.get(sp).trial() != TrialRules.NONE) Trials.applyToll(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Interrogation.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onSpawn(FinalizeSpawnEvent event) {
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION && type != MobSpawnType.PATROL && type != MobSpawnType.EVENT) return;
        if (!(event.getLevel().getLevel() instanceof ServerLevel level) || !Kin.isDemon(event.getEntity())
                || event.getEntity().getType().is(AllTags.Entities.BOSSES)) return;
        if (GateSeals.get(level).sealed(event.getX(), event.getZ())) event.setSpawnCancelled(true);
    }

    /** A prophet who sleeps the night through sees something: once a day, a few letters of a tablet or a glimpse of what waits. */
    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || event.wakeImmediately() || !Prophecies.prophet(sp)) return;
        long day = WorldClock.day(sp.server);
        if (sp.getPersistentData().getLong(VISION_DAY) == day + 1) return;
        sp.getPersistentData().putLong(VISION_DAY, day + 1);
        Tablet t = Tablet.byOrdinal(sp.getRandom().nextInt(Tablet.values().length));
        if (sp.getRandom().nextBoolean()) {
            int letters = WordCipher.lettersFrom(day * 31 + sp.getUUID().hashCode() + t.ordinal(), VISION_LETTERS);
            Prophecies.update(sp, p -> p.withKnown(t, p.known(t) | letters));
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.vision.letters",
                    Component.translatable("item.supernaturalcraft." + t.id() + "_tablet"), WordCipher.letters(letters)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        } else {
            sp.displayClientMessage(Component.translatable("message.supernaturalcraft.vision." + sp.getRandom().nextInt(VISION_LINES))
                    .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), false);
        }
    }
}
