package org.papiricoh.supernaturalcraft.legacy.research;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.papiricoh.supernaturalcraft.registry.AllDataComponents;
import org.papiricoh.supernaturalcraft.registry.AllItems;

import java.util.List;

/**
 * Field notes on a topic ({@code NOTE_TOPIC}): what research is paid in (v0.17). Topics: {@code creature:<entity id>} (from
 * kills: a creature's file, its boss or a case about it), {@code arcane} (formulas and rites), {@code relic} (artifacts) and
 * {@code place} (the archive's lore). Notes only stack with notes on the same topic.
 */
public class FieldNotesItem extends Item {

    public static final String CREATURE = "creature";
    public static final String ARCANE = "arcane";
    public static final String RELIC = "relic";
    public static final String PLACE = "place";

    public FieldNotesItem(Properties properties) {
        super(properties);
    }

    /** {@code count} field notes on {@code topic} ({@code creature:minecraft:zombie}, {@code arcane}, {@code relic}, {@code place}). */
    public static ItemStack stack(String topic, int count) {
        ItemStack s = new ItemStack(AllItems.FIELD_NOTES.get(), Math.max(1, count));
        s.set(AllDataComponents.NOTE_TOPIC.get(), topic);
        return s;
    }

    /** Notes on a creature. */
    public static String creature(ResourceLocation entity) {
        return CREATURE + ":" + entity;
    }

    public static String topic(ItemStack stack) {
        return stack.getOrDefault(AllDataComponents.NOTE_TOPIC.get(), "");
    }

    /** The family of a note topic ({@code creature}, {@code arcane}, {@code relic}, {@code place}): picks the icon. */
    public static String family(String topic) {
        int c = topic.indexOf(':');
        return c < 0 ? topic : topic.substring(0, c);
    }

    @Override
    public Component getName(ItemStack stack) {
        String topic = topic(stack);
        if (topic.isEmpty()) return super.getName(stack);
        String family = family(topic);
        if (CREATURE.equals(family)) {
            ResourceLocation id = ResourceLocation.tryParse(topic.substring(CREATURE.length() + 1));
            Component who = id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                    ? BuiltInRegistries.ENTITY_TYPE.get(id).getDescription() : Component.literal(String.valueOf(id));
            return Component.translatable("item.supernaturalcraft.field_notes.creature", who);
        }
        return Component.translatable("item.supernaturalcraft.field_notes." + family);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String family = family(topic(stack));
        String key = family.isEmpty() ? "blank" : family;
        tooltip.add(Component.translatable("tooltip.supernaturalcraft.field_notes." + key).withStyle(ChatFormatting.GRAY));
    }
}
