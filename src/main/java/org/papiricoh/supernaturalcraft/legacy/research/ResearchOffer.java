package org.papiricoh.supernaturalcraft.legacy.research;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One topic on a hunter's research board (v0.17), as the desk's screen shows it ({@code network.ResearchBoardPayload}).
 *
 * @param topic the topic id ({@code kind:subject}); sent back in {@code ResearchActionPayload} to start it
 * @param tier I–V (≤ the hunter's {@code LegacyRules.maxTier})
 * @param cost what starting it takes from the inventory (field notes of the topic, paper, ink, maybe a reagent)
 * @param ticks how long it takes (with the hunter's speed: config and the Men of Letters Ring)
 * @param titleKey a lang key for its title ({@code research.supernaturalcraft.topic.<kind>}); {@code args} fill its {@code %s}
 * @param args the title's arguments: plain text, or a lang key when it starts with {@code #} (e.g. {@code #entity.minecraft.zombie})
 * @param affordable whether the hunter carries the whole cost right now
 */
public record ResearchOffer(String topic, int tier, List<ItemStack> cost, long ticks, String titleKey, List<String> args,
                            boolean affordable) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchOffer> STREAM_CODEC = StreamCodec.of(
            (buf, o) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, o.topic);
                ByteBufCodecs.VAR_INT.encode(buf, o.tier);
                ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, o.cost);
                ByteBufCodecs.VAR_LONG.encode(buf, o.ticks);
                ByteBufCodecs.STRING_UTF8.encode(buf, o.titleKey);
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buf, o.args);
                ByteBufCodecs.BOOL.encode(buf, o.affordable);
            },
            buf -> new ResearchOffer(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)));

    public ResearchOffer {
        cost = List.copyOf(cost);
        args = List.copyOf(args);
    }

    public TopicKind kind() {
        return TopicKind.of(topic);
    }
}
