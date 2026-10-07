package org.papiricoh.supernaturalcraft.magic.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The pages of a grimoire and which one is open. Immutable, as data components must be. */
public record SpellBook(List<Spell> pages, int selected) {

    public static final int PAGES = 6;
    public static final SpellBook EMPTY = new SpellBook(Collections.nCopies(PAGES, Spell.EMPTY), 0);

    public static final Codec<SpellBook> CODEC = RecordCodecBuilder.create(i -> i.group(
            Spell.CODEC.listOf(0, PAGES).fieldOf("pages").forGetter(SpellBook::pages),
            Codec.intRange(0, PAGES - 1).optionalFieldOf("selected", 0).forGetter(SpellBook::selected)
    ).apply(i, SpellBook::new));

    public static final StreamCodec<ByteBuf, SpellBook> STREAM_CODEC = StreamCodec.composite(
            Spell.STREAM_CODEC.apply(ByteBufCodecs.list(PAGES)), SpellBook::pages,
            ByteBufCodecs.VAR_INT, SpellBook::selected,
            SpellBook::new);

    public Spell page(int index) {
        return index >= 0 && index < pages.size() ? pages.get(index) : Spell.EMPTY;
    }

    public Spell current() {
        return page(selected);
    }

    public SpellBook withPage(int index, Spell spell) {
        List<Spell> copy = new ArrayList<>(pages);
        while (copy.size() < PAGES) copy.add(Spell.EMPTY);
        copy.set(index, spell);
        return new SpellBook(List.copyOf(copy), selected);
    }

    public SpellBook cycle(int delta) {
        return new SpellBook(pages, Math.floorMod(selected + delta, PAGES));
    }

    public SpellBook select(int index) {
        return new SpellBook(pages, Math.floorMod(index, PAGES));
    }
}
