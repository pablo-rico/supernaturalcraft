package org.papiricoh.supernaturalcraft.author;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.compat.curios.CuriosCompat;
import org.papiricoh.supernaturalcraft.registry.AllItems;
import org.papiricoh.supernaturalcraft.registry.AllTags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What Sam's amulet does while worn: four more hearts (a max-health modifier, removed when it comes off), and once a
 * second the bosses and the mod's creatures within {@link #RANGE} blocks are outlined, for its wearer only (their
 * glowing flag is sent to that one player, never set on the creature), and the amulet itself shines (an enchantment
 * glint) while one of them is near.
 */
public final class SamsAmulet {

    public static final double RANGE = 32;
    public static final ResourceLocation HEARTS = SupernaturalCraft.asResource("sams_amulet");
    /** Four hearts. */
    public static final double BONUS = 8;
    private static final boolean CURIOS = ModList.get() != null && ModList.get().isLoaded("curios");
    /** Who each wearer currently sees outlined, by entity id. */
    private static final Map<UUID, Set<Integer>> OUTLINED = new HashMap<>();

    private SamsAmulet() {
    }

    /** The amulet {@code player} wears: in a Curios necklace slot, else the off hand or the hotbar. Empty if none. */
    public static ItemStack worn(Player player) {
        if (CURIOS) {
            ItemStack s = CuriosCompat.findEquipped(player, AllItems.SAMS_AMULET.get());
            if (!s.isEmpty()) return s;
        }
        if (player.getOffhandItem().is(AllItems.SAMS_AMULET.get())) return player.getOffhandItem();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(AllItems.SAMS_AMULET.get())) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Every tick, on the server. */
    public static void tick(ServerPlayer player) {
        ItemStack amulet = worn(player);
        boolean wearing = !amulet.isEmpty();
        hearts(player, wearing);
        if (player.tickCount % 20 != 0) return;
        Set<Integer> before = OUTLINED.getOrDefault(player.getUUID(), Set.of());
        Set<Integer> now = new HashSet<>();
        if (wearing) {
            for (LivingEntity e : powerful(player)) {
                now.add(e.getId());
                send(player, e, true);
            }
            boolean shine = !now.isEmpty();
            if (shine != Boolean.TRUE.equals(amulet.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))) {
                if (shine) amulet.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                else amulet.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            }
        }
        for (int id : before) {
            if (now.contains(id)) continue;
            Entity e = player.level().getEntity(id);
            if (e != null) send(player, e, false);
        }
        if (now.isEmpty()) OUTLINED.remove(player.getUUID());
        else OUTLINED.put(player.getUUID(), now);
    }

    /** Forget a hunter who left. */
    public static void forget(Player player) {
        OUTLINED.remove(player.getUUID());
    }

    /** Bosses and the mod's own creatures near {@code player}. */
    public static List<LivingEntity> powerful(Player player) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE), e -> e.isAlive()
                && !(e instanceof Player) && !(e instanceof AuthorNpcEntity) && isPowerful(e.getType()));
    }

    public static boolean isPowerful(EntityType<?> type) {
        return type.is(AllTags.Entities.BOSSES) || EntityType.getKey(type).getNamespace().equals(SupernaturalCraft.MODID);
    }

    /** Four hearts while worn. */
    public static void hearts(Player player, boolean wearing) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        boolean has = health.hasModifier(HEARTS);
        if (wearing && !has) {
            health.addPermanentModifier(new AttributeModifier(HEARTS, BONUS, AttributeModifier.Operation.ADD_VALUE));
        } else if (!wearing && has) {
            health.removeModifier(HEARTS);
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
    }

    /** Tells {@code player} alone that {@code e} glows (or no longer does). */
    private static void send(ServerPlayer player, Entity e, boolean glow) {
        byte flags = e.getEntityData().get(Flags.SHARED);
        byte value = glow ? (byte) (flags | 1 << 6) : flags;
        player.connection.send(new ClientboundSetEntityDataPacket(e.getId(), List.of(SynchedEntityData.DataValue.create(Flags.SHARED, value))));
    }

    /** Reaches the entity's shared flags (protected in {@link Entity}); never instantiated. */
    private abstract static class Flags extends Entity {
        static final EntityDataAccessor<Byte> SHARED = DATA_SHARED_FLAGS_ID;

        private Flags(EntityType<?> type, Level level) {
            super(type, level);
        }
    }
}
