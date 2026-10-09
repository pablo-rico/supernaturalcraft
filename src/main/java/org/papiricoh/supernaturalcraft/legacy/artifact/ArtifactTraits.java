package org.papiricoh.supernaturalcraft.legacy.artifact;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.compat.curios.CuriosCompat;
import org.papiricoh.supernaturalcraft.magic.mana.ArcanaData;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllMobEffects;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.ArrayList;
import java.util.List;

/**
 * What a cursed artifact does to whoever holds it in the off hand or wears it as a Curios charm (v0.17), checked every second.
 * Boons: swiftness (Speed I), mending (1 HP every 10 s), night_eyes (night vision), sixth_sense (Second Sight: the hidden show
 * themselves), warding (−15 % damage from creatures other than bosses), fortune (Luck), haste (Haste I),
 * feather (half fall damage). Curses: misfortune (Bad Luck), hunger (Hunger), whispers (sanity drains, so the hallucinations
 * of a frayed mind come), frailty (Weakness). Traits work whether or not the artifact has been identified.
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class ArtifactTraits {

    private static final boolean CURIOS = ModList.get() != null && ModList.get().isLoaded("curios");

    public static final float WARDING = 0.85f;
    public static final float FEATHER = 0.5f;
    /** Sanity the whispers take every 10 s. */
    public static final float WHISPERS = 4f;

    private ArtifactTraits() {
    }

    /** Every artifact {@code player} carries where it works: the off hand and a Curios charm slot. */
    public static List<ArtifactData> active(Player player) {
        List<ArtifactData> out = new ArrayList<>();
        ArtifactData off = Artifacts.data(player.getOffhandItem());
        if (off != null) out.add(off);
        if (CURIOS) {
            ArtifactData worn = Artifacts.data(curio(player));
            if (worn != null && (off == null || worn.seed() != off.seed())) out.add(worn);
        }
        return out;
    }

    private static ItemStack curio(Player player) {
        return CuriosCompat.findEquipped(player, AllItems.CURSED_ARTIFACT.get());
    }

    public static boolean has(Player player, String trait) {
        for (ArtifactData d : active(player)) if (d.boons().contains(trait) || d.curse().equals(trait)) return true;
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        List<ArtifactData> active = active(player);
        if (active.isEmpty()) return;
        for (ArtifactData d : active) {
            for (String boon : d.boons()) apply(player, boon);
            if (!d.curse().isEmpty()) apply(player, d.curse());
        }
    }

    /** One second of {@code trait} on {@code player}. */
    public static void apply(ServerPlayer player, String trait) {
        switch (trait) {
            case "swiftness" -> effect(player, MobEffects.MOVEMENT_SPEED, 60);
            case "night_eyes" -> effect(player, MobEffects.NIGHT_VISION, 300);
            case "sixth_sense" -> effect(player, AllMobEffects.SECOND_SIGHT, 60);
            case "fortune" -> effect(player, MobEffects.LUCK, 60);
            case "haste" -> effect(player, MobEffects.DIG_SPEED, 60);
            case "mending" -> {
                if (player.tickCount % 200 == 0 && player.getHealth() < player.getMaxHealth()) player.heal(1f);
            }
            case "misfortune" -> effect(player, MobEffects.UNLUCK, 60);
            case "hunger" -> effect(player, MobEffects.HUNGER, 60);
            case "frailty" -> effect(player, MobEffects.WEAKNESS, 60);
            case "whispers" -> {
                if (player.tickCount % 200 == 0) {
                    ArcanaData a = ManaManager.get(player);
                    a.setSanity(Math.max(0, a.sanity() - WHISPERS));
                }
            }
            default -> {
            }
        }
    }

    private static void effect(ServerPlayer player, Holder<MobEffect> effect, int ticks) {
        @Nullable MobEffectInstance now = player.getEffect(effect);
        if (now != null && (now.getAmplifier() > 0 || now.getDuration() > ticks - 20)) return;
        player.addEffect(new MobEffectInstance(effect, ticks, 0, true, false, true));
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
        if (event.getSource().is(DamageTypeTags.IS_FALL) && has(player, "feather")) {
            event.setAmount(event.getAmount() * FEATHER);
        } else if (event.getSource().getEntity() instanceof net.minecraft.world.entity.Mob mob && !mob.getType().is(AllTags.Entities.BOSSES)
                && has(player, "warding")) {
            event.setAmount(event.getAmount() * WARDING);
        }
    }
}
