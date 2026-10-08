package org.papiricoh.supernaturalcraft.journal;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoadmapStateTest {

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("supernaturalcraft", path);
    }

    /** A node done by the advancement {@code main/<id>}. */
    private static RoadmapNode node(String id, boolean main, String... parents) {
        return new RoadmapNode(id, 0, 0, id("salt"), List.of(parents), false, main, Unlock.advancement(id("main/" + id)),
                Optional.empty());
    }

    /** A hunter who has done exactly these nodes. */
    private static Progress done(String... ids) {
        Set<String> set = Set.of(ids);
        return new Progress() {
            @Override
            public boolean done(ResourceLocation advancement) {
                return set.contains(advancement.getPath().substring("main/".length()));
            }

            @Override
            public boolean seen(ResourceLocation entity) {
                return false;
            }

            @Override
            public boolean has(ResourceLocation item) {
                return false;
            }
        };
    }

    // salt → trap → azazel; salt → bowl (side); trap + bowl → deal
    private static final List<RoadmapNode> ROAD = List.of(
            node("salt", true),
            node("trap", true, "salt"),
            node("bowl", false, "salt"),
            node("azazel", true, "trap"),
            node("deal", false, "trap", "bowl"));

    @Test
    void rootIsAvailableAtTheStart() {
        Map<String, RoadmapState.Status> s = RoadmapState.of(ROAD, done());
        assertEquals(RoadmapState.Status.AVAILABLE, s.get("salt"), "no parents: within reach from the start");
        assertEquals(RoadmapState.Status.LOCKED, s.get("trap"));
        assertEquals(RoadmapState.Status.LOCKED, s.get("deal"));
    }

    @Test
    void availableOnlyWhenEveryParentIsDone() {
        Map<String, RoadmapState.Status> s = RoadmapState.of(ROAD, done("salt", "trap"));
        assertEquals(RoadmapState.Status.DONE, s.get("salt"));
        assertEquals(RoadmapState.Status.AVAILABLE, s.get("azazel"));
        assertEquals(RoadmapState.Status.AVAILABLE, s.get("bowl"));
        assertEquals(RoadmapState.Status.LOCKED, s.get("deal"), "one parent of two is not enough");
        assertEquals(RoadmapState.Status.AVAILABLE, RoadmapState.of(ROAD, done("salt", "trap", "bowl")).get("deal"));
    }

    @Test
    void doneBeatsUnfinishedParents() {
        Map<String, RoadmapState.Status> s = RoadmapState.of(ROAD, done("azazel"));
        assertEquals(RoadmapState.Status.DONE, s.get("azazel"), "a boss killed on someone else's road still counts");
        assertEquals(RoadmapState.Status.LOCKED, s.get("trap"));
    }

    @Test
    void nextPrefersTheMainRoadInListOrder() {
        Map<String, RoadmapState.Status> s = RoadmapState.of(ROAD, done("salt"));
        assertEquals("trap", RoadmapState.next(ROAD, s).id(), "the bowl is available too, but off the main road");
        s = RoadmapState.of(ROAD, done("salt", "trap", "azazel"));
        assertEquals("bowl", RoadmapState.next(ROAD, s).id(), "with the main road walked, any step within reach");
        assertEquals("salt", RoadmapState.next(ROAD, RoadmapState.of(ROAD, done())).id());
    }

    @Test
    void nextIsNullWhenEverythingIsDone() {
        Map<String, RoadmapState.Status> s = RoadmapState.of(ROAD, done("salt", "trap", "bowl", "azazel", "deal"));
        assertNull(RoadmapState.next(ROAD, s));
    }

    @Test
    void anyUnlockCountsAStepWhoseItemWasSpent() {
        RoadmapNode seal = new RoadmapNode("seal", 0, 0, id("last_seal"), List.of(), false, true,
                Unlock.any(Unlock.item(id("last_seal")), Unlock.advancement(id("main/lucifer"))), Optional.empty());
        assertEquals(RoadmapState.Status.DONE, RoadmapState.of(List.of(seal), done("lucifer")).get("seal"));
        assertEquals(RoadmapState.Status.AVAILABLE, RoadmapState.of(List.of(seal), done()).get("seal"));
    }

    @Test
    void stepsOnAnotherSidesBranchAreForsaken() {
        RoadmapNode choice = node("choice", true);
        RoadmapNode angel = branch("angel_1", "angel", "choice");
        RoadmapNode demon = branch("demon_1", "demon", "choice");
        RoadmapNode hunter = branch("hunter_1", "hunter", "choice");
        List<RoadmapNode> road = List.of(choice, angel, demon, hunter);
        Progress sworn = sworn("demon", "choice", "demon_1");
        Map<String, RoadmapState.Status> s = RoadmapState.of(road, sworn);
        assertEquals(RoadmapState.Status.FORSAKEN, s.get("angel_1"));
        assertEquals(RoadmapState.Status.FORSAKEN, s.get("hunter_1"));
        assertEquals(RoadmapState.Status.DONE, s.get("demon_1"));
        assertNull(RoadmapState.next(road, s));
        // A human keeps every branch open.
        Map<String, RoadmapState.Status> free = RoadmapState.of(road, sworn("", "choice"));
        assertEquals(RoadmapState.Status.AVAILABLE, free.get("angel_1"));
        assertEquals(RoadmapState.Status.AVAILABLE, free.get("hunter_1"));
    }

    @Test
    void aStepAlreadyDoneStaysDoneAfterChangingSides() {
        RoadmapNode choice = node("choice", true);
        RoadmapNode angel = branch("angel_1", "angel", "choice");
        Map<String, RoadmapState.Status> s = RoadmapState.of(List.of(choice, angel), sworn("demon", "choice", "angel_1"));
        assertEquals(RoadmapState.Status.DONE, s.get("angel_1"));
    }

    private static RoadmapNode branch(String id, String side, String... parents) {
        return new RoadmapNode(id, 0, 0, id("salt"), List.of(parents), false, true, Unlock.advancement(id("main/" + id)),
                Optional.empty(), Optional.of(side));
    }

    private static Progress sworn(String side, String... ids) {
        Progress base = done(ids);
        return new Progress() {
            @Override
            public boolean done(ResourceLocation advancement) {
                return base.done(advancement);
            }

            @Override
            public boolean seen(ResourceLocation entity) {
                return false;
            }

            @Override
            public boolean has(ResourceLocation item) {
                return false;
            }

            @Override
            public String allegiance() {
                return side;
            }
        };
    }
}
