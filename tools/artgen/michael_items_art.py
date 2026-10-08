"""Michael's smaller art (v0.12): the Grace of Michael (a vial of blue-white light, the pair of Lucifer's Grace), the
two projectiles (a steel feather and a spear of the halo, tiny GeckoLib models pointing along local +Z, drawn full
bright), the trophy (a block model on the trophies' pedestal: a bust in the great helm with folded wings), the
effect icons (vessel, grace_favor, heavens_mark) and the particle sprites (steel_feather, halo_ray; 4 frames each).
The Lance and its borrowed texture are michael_lance_art's; the armour icons general_armor_art's."""

import math

import blockmodel as BM
from chuck_art import Model
from common import ASSETS, art, save
from items import vial
from michael_art import GOLD, LIGHT, STEEL, WHITE_METAL, MetalFeather, glyph_bit, tone
from michael_archangel_art import build_spear
from pixelkit import Ramp, Tex, hexc, item_outline, mix, shade

GEO = ASSETS + "/geo/entity/"
K = hexc("#1E1410")
LIGHTRAMP = Ramp("#2a5aa8", "#4f86d8", "#86b4f0", "#bcd8fb", "#e6f2ff", "#ffffff")


# --- the Grace -------------------------------------------------------------------------------------------

def michaels_grace():
    """A vial of swirling blue-white light, a spark of gold at its heart."""
    t = vial(LIGHTRAMP, hexc("#ffffff"))
    for (x, y) in ((6, 10), (7, 9), (8, 10), (9, 11), (7, 11), (6, 12), (8, 12), (9, 10)):
        t.set(x, y, LIGHTRAMP[5] if (x + y) % 2 else hexc("#dcecff"))
    t.set(7, 10, GOLD[5])
    t.set(8, 11, GOLD[4])
    return t


# --- projectiles -----------------------------------------------------------------------------------------

def steel_feather():
    """A steel feather some 9 px long, tip along +Z, a gold edge and a raised shaft."""
    m = Model("steel_feather", 64, 64)
    m.bone("root", (0, 0, 0))
    b = m.bone("feather", (0, 0, 0), "root", rotation=(90, 0, 0))
    mf = MetalFeather("primary", -1, 9601)
    m.cube(b, (-1.25, -4.5, -0.25), (2.5, 9, 0.5), mf.mat, None, density=4, tag="vane")
    m.cube(b, (-0.25, -4.0, 0.25), (0.5, 8.0, 0.25), lambda f, x, y, w, h: tone(STEEL, 5 - 2 * y / max(1, h)), None, density=4,
           tag="shaft")
    t, _ = m.build(gutter=1, seed=9600)
    return m, t


def light_spear():
    """One spear of the halo, 14 px, centred, its head along +Z."""
    m = Model("light_spear", 64, 64)
    m.bone("root", (0, 0, 0))
    b = m.bone("spear", (0, 0, 0), "root", rotation=(-90, 0, 0))
    build_spear(m, b, (0, -7, 0), 14.0)
    t, _ = m.build(gutter=1, seed=9610)
    return m, t


# --- the trophy ------------------------------------------------------------------------------------------

def _tex(fn, seed=0):
    t = Tex(16, 16, seed)
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


def trophy():
    """The bust in the great helm on the trophies' pedestal: tapered helm with a prow, a glowing visor slit, cheek guards
    and a crest of feathers swept back; a gorget; scalloped pauldrons; the winged sunburst on the chest; and two folded
    metal wings rising behind the shoulders to frame the head (flat cut-out planes, like the other winged trophies)."""
    def plate_v(y, h=16):
        return tone(WHITE_METAL, 4.7 - 2.4 * y / max(1, h - 1) + (0.7 if abs(y / (h - 1) - 0.25) < 0.08 else 0))

    plate = _tex(lambda x, y: GOLD[4] if y == 0 else (GOLD[2] if y == 15 else plate_v(y)))
    gold = _tex(lambda x, y: GOLD[5] if y == 0 else (GOLD[1] if y == 15 else tone(GOLD, 4.3 - 2.0 * y / 15)))

    def scallop(x, y):
        # A pauldron: white plate, gold rim at the top, a scalloped gold lower edge (cut out between the lobes).
        lobe = abs((x % 4) - 1.5) / 2.0
        edge = 13 + int(round(2.5 * lobe))
        if y > 15 - int(round(2.5 * lobe)) + 0:
            return None
        if y >= edge - 1:
            return GOLD[4] if y == edge - 1 else GOLD[2]
        if y == 0:
            return GOLD[5]
        return plate_v(y)
    pauldron = _tex(scallop)

    def chest(x, y):
        # The winged sunburst across the breastplate (the face is mapped from rows 2..14).
        cx, cy = 7.5, 7.0
        r = math.hypot(x - cx, (y - cy) * 1.3)
        if r < 1.6:
            return hexc("#ffffff")
        if r < 2.6:
            return GOLD[5]
        if r < 4.2 and (math.degrees(math.atan2(y - cy, x - cx)) % 45) < 16 and y <= cy + 1:
            return GOLD[4]
        ax = abs(x - cx)
        if 3 <= ax <= 7.5 and cy - 1 - (ax - 3) * 0.9 <= y <= cy + 1.5 - (ax - 3) * 0.2:
            return GOLD[4] if int(ax) % 2 else GOLD[3]
        if y in (12, 13) and 3 <= x <= 12 and (x + y) % 3:
            return LIGHT[3]  # a line of script, glowing
        if y == 2:
            return GOLD[4]
        return plate_v(y)
    chest_t = _tex(chest)

    def helm_face(x, y):
        # The front of the prow (both halves): a gold ridge down the middle, a brow, the visor slit of blue light, breaths.
        if y in (6, 7, 8):
            return LIGHT[5] if y == 7 else LIGHT[3]
        if y in (4, 5):
            return GOLD[4] if y == 4 else GOLD[2]
        if x in (7, 8):
            return GOLD[5] if x == 7 else GOLD[3]
        if y >= 10 and x in (4, 11) and y % 2 == 0:
            return hexc("#1a2232")
        return plate_v(y)
    helm_t = _tex(helm_face)

    def wing(x, y):
        # A folded wing seen from behind/in front: root at the right edge (x=15), the wrist arching above at x~5, the
        # outer edge falling to a long tip at the bottom left; steel-blue feathers in columns with rounded tips, gold
        # along the leading edge. Cut out everywhere else.
        top = (x - 5) * 0.55 if x >= 5 else (5 - x) * 0.9
        if x >= 13:
            top = (13 - 5) * 0.55 + (x - 13) * 1.6
        col, fx = divmod(x, 2)
        bottom = 15 - (x / 15) * 6 - (1 if fx == 1 else 0)
        if y < top - 0.5 or y > bottom:
            return None
        if y < top + 0.9:
            return GOLD[5] if x > 4 else GOLD[4]
        if y < top + 2.2:
            return GOLD[3] if (x + y) % 2 else STEEL[4]   # covert scales, gold-rimmed
        v = 4.6 - 2.6 * (y - top) / max(1.0, bottom - top)
        if fx == 0:
            v -= 0.9  # the dark line between feathers
        return tone(STEEL, v)
    wing_t = _tex(wing)

    def crest(x, y):
        # A crest feather on its side: white, the tip (top) gold, pointed.
        half = 3.5 * min(1.0, (15 - y) / 6.0) if y < 9 else 3.5
        if abs(x - 7.5) > half:
            return None
        if y < 4:
            return GOLD[5] if abs(x - 7.5) < 1 else GOLD[4]
        return hexc("#ffffff") if abs(x - 7.5) < 0.8 else tone(WHITE_METAL, 4.6 - y / 10)
    crest_t = _tex(crest)
    light = _tex(lambda x, y: mix(LIGHT[5], LIGHT[2], math.hypot(x - 7.5, y - 7.5) / 11))
    for name, t in (("plate", plate), ("gold", gold), ("pauldron", pauldron), ("chest", chest_t), ("helm", helm_t),
                    ("wing", wing_t), ("crest", crest_t), ("light", light)):
        save(t, "block", f"michael_trophy_{name}")

    P, G, PA, CH, H, W, C, L = "plate", "gold", "pauldron", "chest", "helm", "wing", "crest", "light"
    full = [0, 0, 16, 16]
    e = [
        BM.cube("pedestal", (4, 0, 4), (12, 3, 12), {"*": "base"}),
        BM.cube("band", (4.5, 2.6, 4.5), (11.5, 3.4, 11.5), {"*": G}),
        BM.cube("gem", (7.25, 0.8, 3.7), (8.75, 2.2, 4.1), {"*": L}),
        # The wings first (behind): flat cut-out planes rising from behind the shoulders, flared back.
        BM.cube("wing_r", (0.2, 3.5, 10.6), (7.2, 17.5, 10.6), {"north": W, "south": W}, uv={"north": [16, 0, 0, 16], "south": full},
                rot=("y", 22.5, (7.2, 8, 10.6))),
        BM.cube("wing_l", (8.8, 3.5, 10.6), (15.8, 17.5, 10.6), {"north": W, "south": W}, uv={"north": full, "south": [16, 0, 0, 16]},
                rot=("y", -22.5, (8.8, 8, 10.6))),
        # The breastplate, deep, with the winged sunburst; a narrower waist under it.
        BM.cube("waist", (5.6, 3, 6.4), (10.4, 4.6, 9.8), {"*": P}),
        BM.cube("chest", (4.8, 4.4, 5.6), (11.2, 7.6, 10.4), {"north": CH, "*": P}, uv={"north": [0, 2, 16, 14]}),
        BM.cube("pauldron_r", (2.6, 5.4, 5.3), (5.6, 7.9, 10.7), {"*": PA, "up": P}, uv={"north": full, "south": full, "east": full, "west": full},
                rot=("z", 22.5, (5.2, 7.6, 8))),
        BM.cube("pauldron_l", (10.4, 5.4, 5.3), (13.4, 7.9, 10.7), {"*": PA, "up": P}, uv={"north": full, "south": full, "east": full, "west": full},
                rot=("z", -22.5, (10.8, 7.6, 8))),
        BM.cube("lame_r", (2.4, 4.2, 5.5), (5.2, 5.6, 10.5), {"*": PA}, uv={"north": full, "south": full, "east": full, "west": full},
                rot=("z", 45, (5.0, 5.4, 8))),
        BM.cube("lame_l", (10.8, 4.2, 5.5), (13.6, 5.6, 10.5), {"*": PA}, uv={"north": full, "south": full, "east": full, "west": full},
                rot=("z", -45, (11.0, 5.4, 8))),
        BM.cube("gorget", (6.0, 7.6, 6.0), (10.0, 8.6, 10.0), {"*": G}),
        # The great helm, tapering: two shells and a cap, a comb along the top.
        BM.cube("helm", (5.3, 8.4, 5.4), (10.7, 11.8, 10.8), {"*": P}),
        BM.cube("helm_top", (5.8, 11.8, 5.9), (10.2, 13.2, 10.4), {"*": P}),
        BM.cube("helm_cap", (6.5, 13.2, 6.6), (9.5, 14.0, 9.8), {"*": P}),
        BM.cube("comb", (7.6, 13.8, 6.2), (8.4, 14.6, 10.4), {"*": G}),
        # The prow: two face plates meeting in a ridge, carrying the brow and the visor slit of light.
        BM.cube("prow_r", (5.4, 8.6, 4.7), (8.0, 12.2, 5.4), {"north": H, "*": P}, uv={"north": [0, 0, 8, 16]},
                rot=("y", 22.5, (8, 10, 5.0))),
        BM.cube("prow_l", (8.0, 8.6, 4.7), (10.6, 12.2, 5.4), {"north": H, "*": P}, uv={"north": [8, 0, 16, 16]},
                rot=("y", -22.5, (8, 10, 5.0))),
        BM.cube("cheek_r", (4.9, 8.2, 5.8), (5.5, 11.2, 8.8), {"*": P}),
        BM.cube("cheek_l", (10.5, 8.2, 5.8), (11.1, 11.2, 8.8), {"*": P}),
        # The crest: three feathers on the comb, swept back, the outer two fanned out so they read from the front.
        BM.cube("crest_c", (8.0, 14.2, 6.6), (8.0, 19.4, 11.6), {"east": C, "west": C}, uv={"east": full, "west": full},
                rot=("x", -22.5, (8, 14.2, 8))),
        BM.cube("crest_r", (5.8, 13.8, 8.6), (8.0, 18.2, 8.6), {"north": C, "south": C}, uv={"north": full, "south": full},
                rot=("z", 22.5, (8.0, 14, 8.6))),
        BM.cube("crest_l", (8.0, 13.8, 8.8), (10.2, 18.2, 8.8), {"north": C, "south": C}, uv={"north": full, "south": full},
                rot=("z", -22.5, (8.0, 14, 8.8))),
    ]
    m = BM.model({"base": "trophy_base", P: "michael_trophy_plate", G: "michael_trophy_gold", PA: "michael_trophy_pauldron",
                  CH: "michael_trophy_chest", H: "michael_trophy_helm", W: "michael_trophy_wing", C: "michael_trophy_crest",
                  L: "michael_trophy_light"}, e, "trophy_base")
    BM.write("michael_trophy", m)


# --- effect icons ----------------------------------------------------------------------------------------

def effect_icons():
    vessel = art([
        "..................",
        ".......kkkkk......",
        "......kdddddk.....",
        ".....kdddddddk....",
        ".....kddddddddk...",
        "....kdlLddlLddk...",
        "....kdLWddLWddk...",
        "....kdlLddlLddk...",
        "....kdddddddddk...",
        ".....kddddddddk...",
        ".....kdddmmdddk...",
        "......kdddddk.....",
        "...l...kdddk...l..",
        "..lLl.kddddk..lLl.",
        "...l.kddddddk..l..",
        "....kddddddddk....",
        "....kkkkkkkkkk....",
        "..................",
    ], {"k": K, "d": hexc("#3a3f4c"), "l": LIGHT[3], "L": LIGHT[4], "W": hexc("#ffffff"), "m": hexc("#5a5060")})
    favor = art([
        "..................",
        "........yy........",
        "....y...yy...y....",
        ".....y..yy..y.....",
        "......kkkkkk......",
        "..ww.kGGGGGGk.ww..",
        ".wWWwkGYYYYGkwWWw.",
        "wWWWWkGYwwYGkWWWWw",
        "wWWwwkGYwwYGkwwWWw",
        ".wwW.kGYYYYGk.Www.",
        "..w..kGGGGGGk..w..",
        "......kkkkkk......",
        ".....y..yy..y.....",
        "....y...yy...y....",
        "........yy........",
        "..................",
        "..................",
        "..................",
    ], {"k": K, "G": GOLD[3], "Y": GOLD[5], "y": GOLD[4], "w": WHITE_METAL[3], "W": hexc("#ffffff")})
    mark = art([
        "..................",
        "......kkkkkk......",
        "....kkbbbbbbkk....",
        "...kbb..BB..bbk...",
        "..kb....BB....bk..",
        "..kb..BBBBBB..bk..",
        ".kb...B.BB.B...bk.",
        ".kb.....BB.....bk.",
        ".kbBBBBBBBBBBBBbk.",
        ".kb.....BB.....bk.",
        ".kb...B.BB.B...bk.",
        "..kb..BBBBBB..bk..",
        "..kb....BB....bk..",
        "...kbb..BB..bbk...",
        "....kkbbbbbbkk....",
        "......kkkkkk......",
        "..................",
        "..................",
    ], {"k": K, "b": GOLD[3], "B": LIGHT[4]})
    save(vessel, "mob_effect", "vessel")
    save(favor, "mob_effect", "grace_favor")
    save(mark, "mob_effect", "heavens_mark")


# --- particles -------------------------------------------------------------------------------------------

def particles():
    for i in range(4):
        # steel_feather: a little metal feather turning (its angle changes frame to frame), gold at the edge.
        t = Tex(8, 8, 9700 + i)
        a = math.radians(-45 + i * 30)
        ca, sa = math.cos(a), math.sin(a)
        for y in range(8):
            for x in range(8):
                u = (x + 0.5 - 4) * ca + (y + 0.5 - 4) * sa      # along the feather
                v = -(x + 0.5 - 4) * sa + (y + 0.5 - 4) * ca     # across
                half = 1.1 * max(0.0, 1 - abs(u) / 3.6) ** 0.6
                if abs(u) <= 3.6 and abs(v) <= half + 0.2:
                    c = tone(STEEL, 4.4 - (u + 3.6) / 3) if v > 0 else tone(STEEL, 3.2 - (u + 3.6) / 3.5)
                    if abs(v) < 0.35:
                        c = WHITE_METAL[5]
                    elif v > half - 0.5:
                        c = GOLD[4]
                    t.set(x, y, c)
        save(t, "particle", f"steel_feather_{i}")
        # halo_ray: a streak of white-blue light, fading as the frames go.
        t = Tex(8, 8, 9710 + i)
        fade = 1 - i * 0.22
        for y in range(8):
            for x in range(8):
                d = abs(x - 3.5)
                along = 1 - abs(y - 3.5) / 4.0
                if d <= 1.6 * along:
                    k = (1 - d / max(0.4, 1.6 * along)) * along
                    c = mix(LIGHT[3], hexc("#ffffff"), k)
                    t.set(x, y, c[:3] + (int(255 * min(1.0, 0.3 + k) * fade),))
        save(t, "particle", f"halo_ray_{i}")


def generate():
    save(michaels_grace(), "item", "michaels_grace")
    for fn, name in ((steel_feather, "steel_feather"), (light_spear, "light_spear")):
        m, t = fn()
        m.rig.write(GEO + name + ".geo.json")
        save(t, "entity", name)
    trophy()
    effect_icons()
    particles()


if __name__ == "__main__":
    generate()
