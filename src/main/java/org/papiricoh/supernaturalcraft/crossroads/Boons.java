package org.papiricoh.supernaturalcraft.crossroads;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.papiricoh.supernaturalcraft.SupernaturalCraft;
import org.papiricoh.supernaturalcraft.magic.mana.ManaManager;

/**
 * What a deal leaves in the body: the extra hearts in {@code ArcanaData.bonusHearts}, worn as a
 * max-health modifier. A respawned or cloned player starts with base values only, so this is
 * re-applied on login, respawn, clone and dimension change. Idempotent. (Extra mana needs nothing:
 * {@code maxMana()} already counts it.)
 */
public final class Boons {

    public static final ResourceLocation HEARTS = SupernaturalCraft.asResource("crossroads_hearts");

    private Boons() {
    }

    public static void apply(Player player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        int hearts = ManaManager.get(player).bonusHearts();
        if (hearts <= 0) {
            health.removeModifier(HEARTS);
        } else {
            // Permanent (saved with the player) so a relog does not clamp the extra health away.
            health.addOrReplacePermanentModifier(new AttributeModifier(HEARTS, hearts * 2.0, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
}
