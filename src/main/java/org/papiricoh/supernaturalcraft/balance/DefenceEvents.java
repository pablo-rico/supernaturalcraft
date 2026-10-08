package org.papiricoh.supernaturalcraft.balance;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.allegiance.Allegiance;
import org.papiricoh.supernaturalcraft.allegiance.Allegiances;
import org.papiricoh.supernaturalcraft.author.AuthorWorld;
import org.papiricoh.supernaturalcraft.crossroads.BossProgression.Boss;
import org.papiricoh.supernaturalcraft.entity.boss.BossDamage;
import org.papiricoh.supernaturalcraft.registry.AllAttachments;
import org.papiricoh.supernaturalcraft.registry.AllDamageTypes;
import org.papiricoh.supernaturalcraft.weapon.ascension.Ascension;

/**
 * A hunter's defence against the great enemies (v0.15): Aegis (ascended armour and faction rank) turns part of
 * their blows aside, Divine Wrath included, and the first defeat of each enemy gives hearts (Vitality).
 * <ul>
 *   <li><b>Aegis</b>, on the final damage ({@link LivingDamageEvent.Pre}) of a blow from a great enemy (the attacker or the
 *   projectile, or one of their parts) or of Divine Wrath: each ascended armour piece gives a quarter of
 *   {@link ProgressionScale#armorAegis} of its level; an angel {@link #ANGEL_AEGIS} per rank; a hunter {@link #HUNTER_AEGIS}
 *   per rank against Divine Wrath only. Capped at {@link ProgressionScale#MAX_AEGIS}. Normal mobs and the enemies' minions
 *   are unaffected.</li>
 *   <li><b>Faction</b> (permanent modifiers, refreshed every second): an angel +{@link #ANGEL_ARMOR} armour per rank, a demon
 *   +{@link #DEMON_HEALTH} max health per rank, a hunter +{@link #HUNTER_HEALTH} per rank.</li>
 *   <li><b>Vitality</b>: {@link Balance#vitalityHearts} hearts of max health, once per enemy, on its advancement (or
 *   {@link #grant}); kept through death (attachment {@code VITALITY}) and caught up on login for older worlds.</li>
 * </ul>
 */
@EventBusSubscriber(modid = SupernaturalCraft.MODID)
public final class DefenceEvents {

    public static final float ANGEL_AEGIS = 0.05f, HUNTER_AEGIS = 0.05f;
    public static final double ANGEL_ARMOR = 2, DEMON_HEALTH = 4, HUNTER_HEALTH = 2;
    public static final ResourceLocation VITALITY_ID = SupernaturalCraft.asResource("vitality");
    public static final ResourceLocation FACTION_ARMOR_ID = SupernaturalCraft.asResource("faction_armor");
    public static final ResourceLocation FACTION_HEALTH_ID = SupernaturalCraft.asResource("faction_health");
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private DefenceEvents() {
    }

    // --- Aegis -----------------------------------------------------------------------------------------------------------

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Aegis acts on the final damage (onDamagePre), after armour and every bonus of this event.
    }

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getNewDamage() <= 0) return;
        DamageSource src = event.getSource();
        boolean divine = src.is(AllDamageTypes.DIVINE_WRATH);
        if (!divine && !fromGreatEnemy(src)) return;
        float aegis = aegis(p, divine);
        if (aegis > 0) event.setNewDamage(ProgressionScale.applyAegis(event.getNewDamage(), aegis));
    }

    /** Whether a great enemy (or one of its parts, or its own projectile) dealt this blow. */
    public static boolean fromGreatEnemy(DamageSource src) {
        return (src.getEntity() != null && BossDamage.isBoss(src.getEntity()))
                || (src.getDirectEntity() != null && BossDamage.isBoss(src.getDirectEntity()));
    }

    /** Aegis from the armour {@code p} wears: a quarter of {@link ProgressionScale#armorAegis} per ascended piece. */
    public static float armorAegis(Player p) {
        float sum = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = p.getItemBySlot(slot);
            if (Ascension.isArmor(stack)) sum += ProgressionScale.armorAegis(Ascension.level(stack)) / ARMOR_SLOTS.length;
        }
        return sum;
    }

    /** Aegis from {@code p}'s side and rank against a blow ({@code divine}: Divine Wrath). */
    public static float factionAegis(Player p, boolean divine) {
        Allegiance a = Allegiances.get(p);
        if (a.isAngel()) return ANGEL_AEGIS * a.rank();
        if (a.isHuman() && divine) return HUNTER_AEGIS * a.hunterRank();
        return 0;
    }

    /** The share of a great enemy's blow {@code p} turns aside. */
    public static float aegis(Player p, boolean divine) {
        return ProgressionScale.totalAegis(armorAegis(p), factionAegis(p, divine));
    }

    // --- Vitality ----------------------------------------------------------------------------------------------------------

    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ResourceLocation id = event.getAdvancement().id();
        if (!id.getNamespace().equals(SupernaturalCraft.MODID)) return;
        for (Boss b : Boss.values()) {
            if (b.advancement.equals(id.getPath())) grant(p, b, true);
        }
    }

    /** Gives {@code p} the hearts of {@code boss}'s first defeat, once. Public for tests (fake players get no advancements). */
    public static boolean grant(ServerPlayer p, Boss boss) {
        return grant(p, boss, true);
    }

    private static boolean grant(ServerPlayer p, Boss boss, boolean announce) {
        Vitality v = p.getData(AllAttachments.VITALITY);
        if (v.has(boss.id())) return false;
        p.setData(AllAttachments.VITALITY, v.with(boss.id()));
        float before = p.getMaxHealth();
        refresh(p);
        float gained = p.getMaxHealth() - before;
        if (gained > 0) p.heal(gained);
        int hearts = Balance.vitalityHearts(boss);
        if (announce && hearts > 0) {
            p.displayClientMessage(Component.translatable("message.supernaturalcraft.vitality",
                    Component.translatable("entity.supernaturalcraft." + boss.entity), hearts).withStyle(ChatFormatting.GOLD), false);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.6f);
        }
        return true;
    }

    /** Hearts of max health {@code p}'s victories have given. */
    public static int vitalityHearts(Player p) {
        Vitality v = p.getData(AllAttachments.VITALITY);
        int hearts = 0;
        for (Boss b : Boss.values()) if (v.has(b.id())) hearts += Balance.vitalityHearts(b);
        return hearts;
    }

    // --- Keeping the modifiers in step -------------------------------------------------------------------------------------

    /** Puts Vitality and the faction's stats on {@code p}'s attributes (idempotent). */
    public static void refresh(ServerPlayer p) {
        set(p, Attributes.MAX_HEALTH, VITALITY_ID, vitalityHearts(p) * 2.0);
        Allegiance a = Allegiances.get(p);
        set(p, Attributes.ARMOR, FACTION_ARMOR_ID, a.isAngel() ? ANGEL_ARMOR * a.rank() : 0);
        set(p, Attributes.MAX_HEALTH, FACTION_HEALTH_ID, a.isDemon() ? DEMON_HEALTH * a.rank() : HUNTER_HEALTH * a.hunterRank());
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void set(Player p, Holder<Attribute> attribute, ResourceLocation id, double value) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier current = inst.getModifier(id);
        if (value == 0) {
            if (current != null) inst.removeModifier(id);
            return;
        }
        if (current != null && current.amount() == value) return;
        // Permanent (saved with the player) so a relog does not clamp the extra health away.
        inst.addOrReplacePermanentModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 7) refresh(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        // Enemies beaten before v0.15 still owe their hearts.
        for (Boss b : Boss.values()) if (AuthorWorld.done(p, b.advancement)) grant(p, b, false);
        refresh(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        refresh(p);
        if (!event.isEndConquered()) p.setHealth(p.getMaxHealth());
    }
}
