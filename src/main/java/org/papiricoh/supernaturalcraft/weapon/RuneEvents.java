package org.papiricoh.supernaturalcraft.weapon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;

/** What graved runes do in a fight. Stat runes ride on attribute modifiers; the rest trigger on hit. */
public final class RuneEvents {

    /** EDGE: per rune, a tenth of the weapon's (ascended) attack damage, never less than {@link #EDGE_DAMAGE}. */
    public static final float EDGE_DAMAGE = 1.5f, EDGE_SHARE = 0.10f, SWIFTNESS_SPEED = 0.08f, LEECH_FRACTION = 0.08f;
    public static final int EMBER_SECONDS = 3, FROST_TICKS = 40;
    /** HYMN: every this many hits (one fewer with two runes) the blow rings out as holy light. */
    public static final int HYMN_EVERY = 4;
    public static final float HYMN_FRACTION = 0.5f, HYMN_RADIUS = 2.5f;
    /** Hits counted toward the next resonance, per attacker (forgotten on restart). */
    private static final java.util.Map<java.util.UUID, Integer> HYMN_COUNT = new java.util.HashMap<>();
    private static boolean resonating;
    private static final ResourceLocation EDGE_ID = SupernaturalCraft.asResource("rune_edge");
    private static final ResourceLocation SWIFT_ID = SupernaturalCraft.asResource("rune_swiftness");

    private RuneEvents() {
    }

    public static void onAttributes(ItemAttributeModifierEvent event) {
        RuneSet runes = event.getItemStack().get(AllDataComponents.RUNES);
        if (runes == null || runes.runes().isEmpty()) return;
        int edge = runes.count(Rune.EDGE), swift = runes.count(Rune.SWIFTNESS);
        if (edge > 0) {
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(EDGE_ID, edgeDamage(event.getItemStack(),
                    org.papiricoh.supernaturalcraft.weapon.ascension.AscensionEvents.baseAttack(event.getDefaultModifiers())) * edge,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        if (swift > 0) {
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(SWIFT_ID, SWIFTNESS_SPEED * swift,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND);
        }
    }

    /** What one EDGE rune adds to a weapon whose own attack damage is {@code base} (before Ascension). */
    public static float edgeDamage(net.minecraft.world.item.ItemStack stack, float base) {
        return Math.max(EDGE_DAMAGE, EDGE_SHARE * base * org.papiricoh.supernaturalcraft.weapon.ascension.Ascension.multiplier(stack));
    }

    public static void onDamage(LivingDamageEvent.Post event) {
        if (resonating) return;
        hymnFromSpells(event);
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker) || event.getSource().getEntity() != attacker) return;
        RuneSet runes = attacker.getMainHandItem().get(AllDataComponents.RUNES);
        if (runes == null || runes.runes().isEmpty()) return;
        LivingEntity target = event.getEntity();
        if (runes.has(Rune.EMBER)) target.igniteForSeconds(EMBER_SECONDS * runes.count(Rune.EMBER));
        if (runes.has(Rune.FROST)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FROST_TICKS, runes.count(Rune.FROST)), attacker);
        }
        if (runes.has(Rune.LEECH)) attacker.heal(event.getNewDamage() * LEECH_FRACTION * runes.count(Rune.LEECH));
        if (runes.has(Rune.HYMN)) hymn(attacker, target, event.getNewDamage(), runes.count(Rune.HYMN));
    }

    /** Spells cast through a catalyst graved with HYMN count too. */
    private static void hymnFromSpells(LivingDamageEvent.Post event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof net.minecraft.world.entity.player.Player p) || source.getDirectEntity() == p) return;
        if (!source.is(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.SPELL) && !source.is(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.SMITE)
                && !source.is(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.HELLFIRE)) return;
        RuneSet runes = org.papiricoh.supernaturalcraft.weapon.catalyst.Catalysts.held(p).runes();
        if (runes.has(Rune.HYMN)) hymn(p, event.getEntity(), event.getNewDamage(), runes.count(Rune.HYMN));
    }

    /**
     * Counts a hit; on the resonant one, half its damage rings out again as holy light on
     * everything near the target (the target included, the attacker spared).
     *
     * @return whether this hit resonated
     */
    public static boolean hymn(LivingEntity attacker, LivingEntity target, float damage, int runeCount) {
        int every = Math.max(2, HYMN_EVERY - (runeCount - 1));
        int n = HYMN_COUNT.merge(attacker.getUUID(), 1, Integer::sum);
        if (n < every) return false;
        HYMN_COUNT.put(attacker.getUUID(), 0);
        if (!(attacker.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        float amount = damage * HYMN_FRACTION;
        resonating = true;
        try {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(HYMN_RADIUS))) {
                if (e == attacker || !e.isAlive() || e.distanceTo(target) > HYMN_RADIUS + 0.5) continue;
                e.invulnerableTime = 0;
                e.hurt(org.papiricoh.supernaturalcraft.registry.AllDamageTypes.source(level,
                        org.papiricoh.supernaturalcraft.registry.AllDamageTypes.GRACE, attacker), amount);
            }
        } finally {
            resonating = false;
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, target.getX(), target.getY(0.5), target.getZ(), 16, 0.6, 0.6, 0.6, 0.08);
        level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.BELL_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS,
                1.2f, 1.4f);
        return true;
    }

    /** Test hook: forget every count. */
    public static void resetHymnCounts() {
        HYMN_COUNT.clear();
    }
}
