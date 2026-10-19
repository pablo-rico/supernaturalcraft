package org.papiricoh.supernaturalcraft.coven.rowena;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * A hunter's dealings with Rowena (v0.21, pure; the {@code ROWENA} attachment). {@code trust} grows with every page bought
 * and every favour (capped at {@link #MAX_TRUST}); {@code met} once they have spoken; {@code clueGiven} once she has told them
 * where Cain keeps his bees ({@code main/rowenas_clue}); {@code book} what they did with the Book of the Damned (P6 reads it);
 * {@code bought} the pages they have bought from her (no longer on offer, read or not).
 */
public record RowenaRelation(int trust, boolean met, boolean clueGiven, Book book, java.util.List<String> bought) {

    public static final int MAX_TRUST = 20;
    public static final RowenaRelation NONE = new RowenaRelation(0, false, false, Book.NONE, java.util.List.of());

    /** What became of the Book of the Damned (P6). Only append. */
    public enum Book implements StringRepresentable {
        NONE, GIVEN, KEPT;

        public static final Codec<Book> CODEC = StringRepresentable.fromEnum(Book::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<RowenaRelation> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, MAX_TRUST).optionalFieldOf("trust", 0).forGetter(RowenaRelation::trust),
            Codec.BOOL.optionalFieldOf("met", false).forGetter(RowenaRelation::met),
            Codec.BOOL.optionalFieldOf("clue_given", false).forGetter(RowenaRelation::clueGiven),
            Book.CODEC.optionalFieldOf("book", Book.NONE).forGetter(RowenaRelation::book),
            Codec.STRING.listOf().optionalFieldOf("bought", java.util.List.of()).forGetter(RowenaRelation::bought)
    ).apply(i, RowenaRelation::new));

    public RowenaRelation {
        trust = Math.max(0, Math.min(MAX_TRUST, trust));
        if (book == null) book = Book.NONE;
        bought = bought == null ? java.util.List.of() : java.util.List.copyOf(bought);
    }

    public RowenaRelation(int trust, boolean met, boolean clueGiven, Book book) {
        this(trust, met, clueGiven, book, java.util.List.of());
    }

    public RowenaRelation withTrust(int t) {
        return new RowenaRelation(t, met, clueGiven, book, bought);
    }

    /** {@code page} bought from her: off her offer for good. */
    public RowenaRelation buy(String page) {
        if (bought.contains(page)) return this;
        java.util.List<String> b = new java.util.ArrayList<>(bought);
        b.add(page);
        return new RowenaRelation(trust, met, clueGiven, book, b);
    }

    public RowenaRelation trusted(int more) {
        return withTrust(trust + more);
    }

    public RowenaRelation meet() {
        return new RowenaRelation(trust, true, clueGiven, book, bought);
    }

    public RowenaRelation giveClue() {
        return new RowenaRelation(trust, met, true, book, bought);
    }

    public RowenaRelation withBook(Book b) {
        return new RowenaRelation(trust, met, clueGiven, b, bought);
    }
}
