"""The Men of Letters' smaller art (v0.17): the bunker's blocks, the order's items and the map marks.

Blocks (vanilla JSON models, facing north; the lamp and the map light themselves with NeoForge's per-element light):
  bunker_door           the armoured vault door: riveted steel, a round door with a spoked wheel; `bunker_door_open`
                        swung out on its hinge (left side) leaving the dark frame.
  research_desk         a walnut desk with a pedestal of drawers, a green leather writing surface, a banker's lamp with a
                        green glass shade, a little typewriter with a sheet in it, a stack of papers.
  map_table             a heavy table whose top is a lit map under glass in a brass-edged frame (tiles with its neighbours).
  archive_shelf         shelves of old books, file boxes and labelled binders.
  men_of_letters_emblem the order's mark: an eye in the Aquarian star, inlaid in brass in dark marble.

Items (16 px): bunker_key, field notes per topic (field_notes_<topic>), case_file, dead_mans_blood, cursed artifacts per
form (artifact_<form>, plus artifact_<form>_aura: a grey halo the item colour tints by rarity), men_of_letters_ring,
spellwrights_spectacles, henrys_case, aquarian_star. Map marks (8 px): bunker, case_site.
"""

import math

import blockmodel as BM
from common import art, save
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix, shade

O = hexc("#14100f")
CLEAR = (0, 0, 0, 0)
GOLD = Ramp("#4b3108", "#7d5410", "#b07d18", "#d9a92b", "#f2d257", "#fff2a8")
BRASS = Ramp("#3e2c0c", "#634816", "#8a6824", "#b08b36", "#d0ae55", "#ecd486")
STEEL = Ramp("#1c1f24", "#30353d", "#474e58", "#636b76", "#848c97", "#b3bac3")
SILVER = Ramp("#3a3f48", "#626a76", "#8c95a1", "#b5bdc7", "#d9dfe6", "#ffffff")
WALNUT = Ramp("#1a0f08", "#2a190d", "#3b2414", "#4e311c", "#634026", "#7a5133")
LEATHER_G = Ramp("#0d1a12", "#15281b", "#1e3826", "#284a32", "#345e40", "#43744f")
GREEN_GLASS = Ramp("#0b2a14", "#11421f", "#1a5e2c", "#27803c", "#40a556", "#7ad08a")
PAPER = Ramp("#7d7158", "#a4977a", "#c5b994", "#ddd2ae", "#ece4c6", "#f8f3df")
MANILA = Ramp("#6b5524", "#8f7533", "#b39447", "#cdb05e", "#dfc67a", "#eedb9c")
CASE = Ramp("#1c0e06", "#33190b", "#4c2711", "#663618", "#7f4721", "#9a5a2d")
BLOOD = Ramp("#0c0103", "#1e0306", "#33060b", "#4a0b11", "#62121a", "#7c1e24")
MARBLE = Ramp("#0d0e10", "#16181b", "#202327", "#2b2f34", "#383d43", "#4a5057")
INK = hexc("#2a2018")


def tex16(fn, w=16, h=16, seed=0):
    t = Tex(w, h, seed)
    for y in range(h):
        for x in range(w):
            c = fn(x, y)
            if c is not None:
                t.set(x, y, c)
    return t


def tone(ramp, v):
    return ramp[int(round(max(0.0, min(len(ramp) - 1.0, v))))]


def lit(ramp, x, y, w=16, h=16, base=3.0, amp=1.2):
    """A top-left lit shade across an icon."""
    return tone(ramp, base + amp * (1 - (x / w + y / h)))


# =====================================================================================================
# The Aquarian star: the order's mark (shared by the emblem, the medallion, the key and the map)
# =====================================================================================================

def star_points(cx, cy, r_out, r_in, n=5, rot=-90):
    pts = []
    for i in range(2 * n):
        r = r_out if i % 2 == 0 else r_in
        a = math.radians(rot + 180 * i / n)
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def seg_dist(px, py, a, b):
    ax, ay = a
    bx, by = b
    dx, dy = bx - ax, by - ay
    L = dx * dx + dy * dy
    t = 0 if L == 0 else max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / L))
    return math.hypot(px - (ax + t * dx), py - (ay + t * dy))


def emblem_field(size, ring=True):
    """{(x, y): kind} for the mark at `size` px: 'ring', 'star', 'eye_white', 'iris', 'pupil'. A circle, a five-pointed star
    drawn as one unbroken line (a pentagram) touching it, and an eye in its heart."""
    c = (size - 1) / 2
    R = size * 0.46
    pts = star_points(c, c, R * 0.93, R * 0.93, 5)[::2]
    order = [pts[0], pts[2], pts[4], pts[1], pts[3], pts[0]]
    lw = max(0.55, size / 26)
    out = {}
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            if ring and abs(d - R) < lw * 1.1:
                out[(x, y)] = "ring"
                continue
            if any(seg_dist(x, y, order[i], order[i + 1]) < lw for i in range(5)):
                out[(x, y)] = "star"
            # The eye: an almond shape in the centre pentagon.
            ex, ey = (x - c) / (R * 0.36), (y - c) / (R * 0.2)
            if ex * ex + ey * ey <= 1.0 and abs(ey) <= 1 - abs(ex) ** 2 * 0.9:
                ir = math.hypot((x - c) / (R * 0.15), (y - c) / (R * 0.15))
                out[(x, y)] = "pupil" if ir < 0.55 else ("iris" if ir < 1.05 else "eye_white")
    return out


# =====================================================================================================
# Blocks
# =====================================================================================================

def glowing(e):
    """A model element drawn full bright (NeoForge per-element lighting)."""
    e["neoforge_data"] = {"block_light": 15, "sky_light": 15}
    return e


def bunker_door_tex():
    """The vault door as a vanilla door (16 x 32, split into bottom and top): dark riveted steel plate in a heavy frame,
    the round door in the middle with its ring of locking bolts, and the brass wheel across it."""
    W, H = 16, 32
    cx, cy = 7.5, 15.5
    t = Tex(W, H, 18100)
    for y in range(H):
        for x in range(W):
            n = fbm(x, y, 18101, 16, 32, 3, 4.0)
            c = tone(STEEL, 2.4 + (n - 0.5) * 1.1 - (y - 16) / 40)
            if (x * 7 + y * 3) % 23 == 0 and n > 0.55:
                c = mix(c, hexc("#5a3b22"), 0.35)          # a little rust
            if x in (0, 15) or y in (0, 31):
                c = STEEL[0]
            elif x in (1, 14) or y in (1, 30):
                c = STEEL[4] if (y % 4 == 2 or x % 4 == 2) else STEEL[1]   # the frame and its rivets
            d = math.hypot(x - cx, (y - cy) * 0.95)
            if 6.4 < d <= 7.2:
                c = STEEL[0]                               # the seam round the round door
            elif 5.4 < d <= 6.4:
                a = math.degrees(math.atan2(y - cy, x - cx)) % 45
                c = STEEL[5] if a < 12 else STEEL[3]       # the bolt ring
            # The wheel: an octagonal brass rim, four spokes and a hub.
            rr = max(abs(x - cx), abs(y - cy), (abs(x - cx) + abs(y - cy)) / 1.41)
            if 3.6 < rr <= 4.6:
                c = tone(BRASS, 4.2 - (y - cy + 5) / 6)
            if (abs(x - cx) < 0.6 or abs(y - cy) < 0.6) and rr <= 4.6:
                c = BRASS[3]
            if rr <= 1.2:
                c = BRASS[5] if rr < 0.8 else BRASS[2]
            # Two hinge plates on the left edge, a handle bar on the right.
            if x in (2, 3) and (4 <= y <= 6 or 25 <= y <= 27):
                c = STEEL[4] if x == 2 else STEEL[2]
            if x == 13 and 12 <= y <= 19:
                c = STEEL[5] if y in (12, 19) else STEEL[3]
            t.set(x, y, c)
    top, bottom = Tex(16, 16, 0), Tex(16, 16, 0)
    for y in range(16):
        for x in range(16):
            top.set(x, y, t.get(x, y))
            bottom.set(x, y, t.get(x, y + 16))
    # The item icon: the door shrunk to 8 x 16 and centred, like vanilla's door items.
    icon = Tex(16, 16, 0)
    for y in range(16):
        for x in range(8):
            icon.set(x + 4, y, t.get(x * 2, y * 2))
    item_outline(icon, O)
    return top, bottom, icon


# --- the research desk -------------------------------------------------------------------------------------

def walnut_tex(seed, grain=True):
    def f(x, y):
        n = fbm(x * 0.4, y * 2.0, seed, 16, 16, 3, 4.0)
        v = 2.6 + (n - 0.5) * 1.6
        if grain and (y + int(3 * math.sin(x * 0.7 + seed))) % 5 == 0:
            v -= 0.6
        return tone(WALNUT, v)
    return tex16(f, seed=seed)


def desk_top():
    """The writing surface: green leather tooled with a gold line, a walnut border."""
    wood = walnut_tex(18201)

    def f(x, y):
        if x in (0, 15) or y in (0, 15):
            return wood.get(x, y)
        if x in (1, 14) or y in (1, 14):
            return GOLD[2] if (x + y) % 2 else GOLD[3]
        n = fbm(x, y, 18202, 16, 16, 2, 5.0)
        return tone(LEATHER_G, 2.6 + (n - 0.5) * 1.2)
    return tex16(f)


def drawers():
    wood = walnut_tex(18203)

    def f(x, y):
        if y in (0, 5, 10, 15) or x in (0, 15):
            return WALNUT[1]
        if y % 5 == 2 and 6 <= x <= 9:
            return BRASS[4] if x in (7, 8) else BRASS[2]   # pulls
        if y % 5 == 1:
            return shade(wood.get(x, y), 1.15)
        return wood.get(x, y)
    return tex16(f)


def lamp_shade():
    def f(x, y):
        v = 4.2 - y / 5 + 0.6 * math.sin(x * 0.8)
        if y in (0, 15):
            return BRASS[4]
        return tone(GREEN_GLASS, v)
    return tex16(f)


def keys_tex():
    """The little typewriter's keyboard from above: staggered rows of round chrome-rimmed keys on black."""
    def f(x, y):
        row = y // 3
        if y % 3 == 2:
            return hexc("#0c0c0e")
        if (x + row) % 2 == 0:
            return hexc("#d8d4c8") if y % 3 == 0 else hexc("#8a8a90")
        return hexc("#141418")
    return tex16(f)


def brass_tex():
    return tex16(lambda x, y: tone(BRASS, 3.4 - y / 8 + (0.6 if (x * 3 + y) % 7 == 0 else 0)))


def papers_tex():
    def f(x, y):
        c = tone(PAPER, 4.0 - (x + y) / 30)
        if y % 3 == 1 and 2 <= x <= 13 - (y * 7) % 5:
            return mix(c, INK, 0.55)
        return c
    return tex16(f)


def research_desk_model():
    W, T, D, G, B, P = "wood", "top", "drawers", "shade", "brass", "paper"
    TB, TF, TC, TK = "tw_body", "tw_front", "tw_chrome", "tw_key"
    els = [
        BM.cube("top", (0, 12, 0), (16, 13.5, 16), {"up": T, "*": W}),
        BM.cube("pedestal", (10, 0, 1), (15.5, 12, 15), {"north": D, "*": W}),
        BM.cube("leg_fw", (0.5, 0, 1), (2.5, 12, 3), {"*": W}),
        BM.cube("leg_bw", (0.5, 0, 13), (2.5, 12, 15), {"*": W}),
        BM.cube("apron", (2.5, 10.5, 1.5), (10, 12, 2.5), {"*": W}),
        BM.cube("modesty", (2.5, 3, 13.5), (10, 12, 14.5), {"*": W}),
        BM.cube("stretcher", (1, 1, 3), (2, 2, 13), {"*": W}),
        # The banker's lamp at the back left: a brass foot and stem, the green shade (lit), its pull chain.
        BM.cube("lamp_foot", (1.6, 13.5, 10.4), (5.4, 14.1, 13.2), {"*": B}),
        BM.cube("lamp_stem", (3.2, 14.1, 11.5), (3.8, 17.2, 12.1), {"*": B}),
        BM.cube("lamp_arm", (3.2, 16.8, 10.6), (3.8, 17.4, 12.1), {"*": B}),
        glowing(BM.cube("lamp_shade", (0.6, 16.6, 9.0), (6.4, 18.4, 11.4), {"*": G}, uv={"north": [0, 0, 16, 6], "south": [0, 0, 16, 6]})),
        glowing(BM.cube("lamp_shade_top", (1.0, 18.4, 9.3), (6.0, 18.9, 11.1), {"*": G})),
        BM.cube("lamp_chain", (5.0, 15.6, 9.4), (5.2, 16.6, 9.6), {"*": B}),
        # Papers fanned at the front left, one sheet askew.
        BM.cube("papers", (1.2, 13.5, 2.0), (6.2, 13.9, 7.4), {"*": P}),
        BM.cube("sheet", (2.0, 13.9, 3.0), (6.8, 14.0, 8.4), {"*": P}, rot=("y", 22.5, (4, 14, 5))),
        BM.cube("pen", (6.4, 14.0, 6.0), (6.8, 14.3, 9.6), {"*": B}, rot=("y", -22.5, (6.6, 14, 7.8))),
        # A little typewriter at the front right with a sheet in it.
        BM.cube("tw_chassis", (7.5, 13.5, 3.5), (14.5, 14.2, 9.5), {"*": TB}),
        BM.cube("tw_housing", (8.0, 14.2, 7.2), (14.0, 16.4, 9.5), {"*": TB, "north": TF}, uv={"north": [0, 0, 16, 7]}),
        BM.cube("tw_deck", (8.0, 14.2, 3.8), (14.0, 15.0, 7.2), {"*": TB, "up": TK}),
        BM.cube("tw_roller", (7.4, 16.4, 8.0), (14.6, 17.4, 9.2), {"*": TC}),
        BM.cube("tw_paper", (9.0, 16.4, 8.8), (13.0, 20.0, 8.8), {"north": P, "south": P}, rot=("x", 22.5, (11, 16.4, 8.8))),
    ]
    tex = {W: "research_desk_wood", T: "research_desk_top", D: "research_desk_drawers", G: "research_desk_shade", B: "research_desk_brass",
           P: "research_desk_paper", TB: "typewriter_body", TF: "typewriter_front", TC: "typewriter_chrome", TK: "research_desk_keys"}
    BM.write("research_desk", BM.model(tex, els, "research_desk_wood"))


# --- the map table -----------------------------------------------------------------------------------------------

def map_tex():
    """A lit map that tiles with its neighbours: pale land, blue water with a coastline, rivers, roads, a grid of
    longitude and latitude, a few red pins."""
    land = Ramp("#5c6b4a", "#7a8a5e", "#98a874", "#b3bf8a", "#ccd4a2", "#e3e6c0")
    sea = Ramp("#1d3a58", "#284d70", "#356289", "#4677a0", "#5b8db6", "#77a6cc")

    def f(x, y):
        h = fbm(x, y, 18301, 16, 16, 3, 2.0)
        if h < 0.42:
            c = tone(sea, 2.6 + (h - 0.3) * 6)
        elif h < 0.45:
            c = land[5]                                   # the shore
        else:
            c = tone(land, 2.4 + (h - 0.5) * 5)
        if x % 8 == 0 or y % 8 == 0:
            c = mix(c, hexc("#2e3a2a"), 0.35)             # the graticule
        r = fbm(x * 2 + 3, y * 2, 18302, 32, 32, 2, 4.0)
        if 0.49 < r < 0.52 and h >= 0.45:
            c = mix(c, sea[3], 0.7)                       # rivers
        if (x, y) in ((4, 11), (12, 5)):
            return hexc("#d02a2a")
        if (x, y) in ((4, 10), (12, 4)):
            return hexc("#ff7a6a")
        return c
    return tex16(f)


def map_rim():
    return tex16(lambda x, y: tone(BRASS, 3.0 + (0.8 if y in (0, 1) else 0) - (0.6 if y in (14, 15) else 0)))


def map_table_model():
    W, M, R = "wood", "map", "rim"
    els = [
        BM.cube("base", (1, 0, 1), (15, 10.5, 15), {"*": W}),
        BM.cube("plinth", (0.5, 0, 0.5), (15.5, 1.2, 15.5), {"*": W}),
        BM.cube("apron", (0, 10.5, 0), (16, 12.5, 16), {"*": W, "up": None}),
        glowing(BM.cube("map", (0.5, 12.5, 0.5), (15.5, 13.0, 15.5), {"up": M, "*": None})),
        BM.cube("rim_n", (0, 12.5, 0), (16, 13.4, 0.5), {"*": R}),
        BM.cube("rim_s", (0, 12.5, 15.5), (16, 13.4, 16), {"*": R}),
        BM.cube("rim_w", (0, 12.5, 0.5), (0.5, 13.4, 15.5), {"*": R}),
        BM.cube("rim_e", (15.5, 12.5, 0.5), (16, 13.4, 15.5), {"*": R}),
    ]
    BM.write("map_table", BM.model({W: "map_table_wood", M: "map_table_map", R: "map_table_rim"}, els, "map_table_wood"))


def map_wood():
    def f(x, y):
        c = walnut_tex(18303).get(x, y)
        if y in (0, 15) or x in (0, 15):
            return WALNUT[1]
        if 2 <= x <= 13 and 3 <= y <= 12 and (x in (2, 13) or y in (3, 12)):
            return WALNUT[4]                              # a raised panel
        return c
    return tex16(f)


# --- the archive shelf ---------------------------------------------------------------------------------------------

def archive_side():
    """Two shelves: on top, old cloth-bound books and a run of numbered file boxes; below, binders with white labels."""
    spines = [hexc(c) for c in ("#4a1c18", "#24324a", "#2f3d22", "#5a4220", "#3a2a3e", "#6b5a3a", "#1f2a2a", "#5c2a1e")]

    def f(x, y):
        if y in (0, 7, 8, 15):
            return WALNUT[2] if y in (0, 8) else WALNUT[1]
        if x == 0 or x == 15:
            return WALNUT[2]
        if y < 7:
            # Books (left) and file boxes (right, x >= 9).
            if x >= 9:
                col = (x - 9) // 3
                if (x - 9) % 3 == 2:
                    return MANILA[1]
                if y == 3:
                    return PAPER[5] if (x - 9) % 3 == 0 else PAPER[3]   # the box label
                return tone(MANILA, 3.2 - y / 6 + 0.3 * col)
            i = (x - 1)
            c = spines[(i * 5 + 3) % len(spines)]
            top = 1 + (i * 7) % 3
            if y < top:
                return WALNUT[0]
            if y == top + 1 or y == 5:
                return mix(c, GOLD[3], 0.6)               # gilt bands
            return shade(c, 1.1 if x % 2 else 0.9)
        # Binders: tall spines with a white label each.
        i = (x - 1) // 2
        c = spines[(i * 3 + 1) % len(spines)]
        if (x - 1) % 2 == 1:
            c = shade(c, 0.75)
        if y in (10, 11) and (x - 1) % 2 == 0:
            return PAPER[5]
        if y == 13 and (x - 1) % 2 == 0:
            return SILVER[3]                              # the finger hole ring
        return c
    return tex16(f)


def archive_model():
    BM.write("archive_shelf", {"parent": "minecraft:block/cube_column",
                               "textures": {"side": "supernaturalcraft:block/archive_shelf", "end": "supernaturalcraft:block/archive_shelf_top"}})


# --- the emblem -------------------------------------------------------------------------------------------------

def emblem_tex(size=32):
    fld = emblem_field(size)
    c0 = (size - 1) / 2

    def f(x, y):
        n = fbm(x, y, 18401, size, size, 3, 4.0)
        vein = abs(math.sin((x * 0.6 + y * 0.35) + n * 6)) < 0.08
        c = tone(MARBLE, 2.0 + (n - 0.5) * 1.6)
        if vein:
            c = mix(c, hexc("#8a8f96"), 0.4)
        k = fld.get((x, y))
        if k in ("ring", "star"):
            d = math.hypot(x - c0, y - c0) / size
            return tone(BRASS, 4.4 - 2.0 * d - (0.6 if (x + y) % 5 == 0 else 0))
        if k == "eye_white":
            return hexc("#e9e2cc")
        if k == "iris":
            return BRASS[2]
        if k == "pupil":
            return hexc("#0b0907")
        if x in (0, size - 1) or y in (0, size - 1):
            return MARBLE[0]
        return c
    return tex16(f, size, size)


def emblem_side():
    def f(x, y):
        n = fbm(x, y, 18402, 16, 16, 3, 4.0)
        c = tone(MARBLE, 2.0 + (n - 0.5) * 1.6)
        if abs(math.sin((x * 0.6 + y * 0.35) + n * 6)) < 0.08:
            c = mix(c, hexc("#8a8f96"), 0.4)
        if y in (1, 14) and 1 <= x <= 14:
            return BRASS[3]                               # brass bands top and bottom
        return c
    return tex16(f)


def emblem_model():
    BM.write("men_of_letters_emblem", BM.model({"e": "men_of_letters_emblem", "s": "men_of_letters_emblem_side"},
                                              [BM.cube("block", (0, 0, 0), (16, 16, 16), {"down": "s", "*": "e"})],
                                              "men_of_letters_emblem_side", cutout=False))


# =====================================================================================================
# Items
# =====================================================================================================

def bunker_key():
    pal = {"o": O, "d": BRASS[1], "g": BRASS[2], "G": BRASS[3], "Y": BRASS[5], "e": hexc("#2a1a0a"), "w": hexc("#e9e2cc"),
           "s": GOLD[4]}
    return art([
        "................",
        "..ooooo.........",
        ".oGYYYGo........",
        "oGYgggYGo.......",
        "oYgwswgYo.......",
        "oYgsesgYo.......",
        "oYgwswgYo.......",
        "oGYgggYGo.......",
        ".oGGYGGoo.......",
        "..ooodGGoo......",
        "......oGGGo.....",
        ".......oGGGo....",
        "........oGGGoo..",
        ".........oGGYGo.",
        "..........oGoGo.",
        "...........o.oo.",
    ], pal)


NOTE_COVERS = {
    "creature": Ramp("#2a1a10", "#3f2818", "#573822", "#6f4a2e", "#88603c", "#a2794c"),
    "arcane": Ramp("#0f1426", "#18203b", "#232e52", "#2f3d69", "#3e4f82", "#52669c"),
    "relic": Ramp("#260a0e", "#3b1017", "#521820", "#6b222b", "#842e37", "#9e3e46"),
    "place": Ramp("#0f1f15", "#182f20", "#22422c", "#2d5639", "#3a6b47", "#4b8358"),
}
GLYPHS = {
    "creature": (["..r..r..r.", "..r..r..r.", ".r..r..r..", ".r..r..r..", "r..r..r...", "r..r..r..."], hexc("#c9b38a")),
    "arcane": (["..yyyyy...", ".y..y..y..", "y..y.y..y.", "y.yyyyy.y.", ".y.y.y.y..", "..yyyyy..."], hexc("#a8c4ff")),
    "relic": (["....y.....", "...yyy....", "....y.....", "....y.....", "...yyy....", "..yyyyy..."], GOLD[4]),
    "place": (["...rrr....", "..rRRRr...", "..rRwRr...", "...rRr....", "....r.....", "..ggggg..."], hexc("#d23a32")),
}


def field_notes(topic):
    """A pocket notebook in a topic's colour: pages showing at the edge, an elastic band, the topic's mark stamped on it."""
    cover = NOTE_COVERS[topic]
    t = Tex(16, 16, 18500)
    for y in range(2, 15):
        for x in range(3, 13):
            t.set(x, y, tone(cover, 3.4 - (x - 3) / 8 - (y - 2) / 14))
    for y in range(3, 15):
        t.set(13, y, PAPER[4] if y % 2 else PAPER[3])     # page edges
    for x in range(4, 14):
        t.set(x, 15, PAPER[3])
    for x in range(3, 13):
        t.set(x, 2, tone(cover, 4.5))
    for y in range(2, 15):
        t.set(11, y, hexc("#141010"))                     # the band
    rows, col = GLYPHS[topic]
    for gy, row in enumerate(rows):
        for gx, ch in enumerate(row):
            if ch == ".":
                continue
            c = {"r": col, "y": col, "R": shade(col, 1.25), "w": hexc("#fff4e0"), "g": hexc("#7a8a5e")}[ch]
            t.set(2 + gx, 5 + gy, c)
    item_outline(t, O)
    return t


def case_file():
    pal = {"o": O, "m": MANILA[2], "M": MANILA[3], "L": MANILA[4], "d": MANILA[1], "p": PAPER[5], "P": PAPER[3], "r": hexc("#b0201c"),
           "R": hexc("#d8443a"), "s": SILVER[4], "S": SILVER[2], "k": INK}
    return art([
        "................",
        "..oooo..........",
        ".oLLLLo.........",
        ".oLMMMooooooo...",
        ".oLppppppppPo...",
        ".oLpkkkpkkpPoss.",
        ".oLMMMMMMMMMMoSs",
        ".oLMMMMMMMMMMo.s",
        ".oLMrRRRRRrMMo..",
        ".oLMrrrrrrrMMo..",
        ".oLMMMMMMMMMMo..",
        ".oLMMkkkkkMMMo..",
        ".oLMMMMMMMMMMo..",
        ".oLmmmmmmmmmmo..",
        ".oddddddddddddo.",
        "..oooooooooooo..",
    ], pal)


def dead_mans_blood():
    """A glass syringe of dead man's blood (near black-red), the plunger drawn back."""
    pal = {"o": O, "g": hexc("#9fb2bb"), "G": hexc("#d8e6ec"), "b": BLOOD[2], "B": BLOOD[3], "D": BLOOD[0], "s": SILVER[3],
           "S": SILVER[5], "n": SILVER[2], "w": hexc("#f2f2f2")}
    return art([
        "..ooo...........",
        ".osSso..........",
        ".oSsso..........",
        "..oosoo.........",
        "....osoo........",
        "....oGgooo......",
        ".....oGbBoo.....",
        "......oGbBDo....",
        ".......oGbBDo...",
        "........oGbBDo..",
        ".........oGBDo..",
        "..........oDoo..",
        "...........oSo..",
        "............on..",
        ".............n..",
        "................",
    ], pal)


ARTIFACT_ART = {
    "ring": ([
        "................",
        "................",
        "......oooo......",
        ".....oeEeEo.....",
        "......oEEo......",
        ".....ossSso.....",
        "....osooooso....",
        "...osoo..oooo...",
        "...oso....oso...",
        "...oso....oso...",
        "...oso....oso...",
        "...osoo..ooso...",
        "....osooooso....",
        ".....oSsssso....",
        "......oooo......",
        "................",
    ], {"o": O, "s": SILVER[2], "S": SILVER[4], "e": hexc("#2a0d32"), "E": hexc("#5a2066")}),
    "doll": ([
        "................",
        ".....oooooo.....",
        "....ohhffhho....",
        "....ohfffffo....",
        "....ofkffkfo....",
        "....offfffho....",
        "....offrrfho....",
        ".....oooooo.....",
        "....oddDDddo....",
        "...oddDDDDddo...",
        "..ofoddddddofo..",
        "..oo.oddddo.oo..",
        ".....oddDdo.....",
        ".....ollolo.....",
        ".....oo..oo.....",
        "................",
    ], {"o": O, "h": hexc("#5a3a1e"), "f": hexc("#e8dcc8"), "k": hexc("#101010"), "r": hexc("#8a1c1c"), "d": hexc("#6a5a7a"),
        "D": hexc("#4a3a5a"), "l": hexc("#2a2a30")}),
    "mirror": ([
        ".....oooooo.....",
        "....oGggggGo....",
        "...oGkkkkkkGo...",
        "...ogkKkkwkgo...",
        "...ogkkKkkkgo...",
        "...ogkkkKkkgo...",
        "...ogkwkkKkgo...",
        "...oGkkkkkkGo...",
        "....oGggggGo....",
        ".....oogGoo.....",
        "......ogGo......",
        "......ogGo......",
        "......ogGo......",
        ".....oGggGo.....",
        "......oooo......",
        "................",
    ], {"o": O, "g": SILVER[2], "G": SILVER[4], "k": hexc("#14161c"), "K": hexc("#3a4250"), "w": hexc("#b8c6d4")}),
    "watch": ([
        "......ooo.......",
        ".....oGoGo......",
        "......oGo.......",
        "....ooGGGoo.....",
        "...oGgggggGo....",
        "..oGgwwwwwgGo...",
        "..ogwwwkwwwgo...",
        "..ogwwwkwwwgo...",
        "..ogwwwkkkwgo...",
        "..ogwwwwwwwgo...",
        "..oGgwwwwwgGo...",
        "...oGgggggGo....",
        "....ooGGGoo.....",
        "......ooo.......",
        "................",
        "................",
    ], {"o": O, "g": GOLD[2], "G": GOLD[4], "w": hexc("#ece4cc"), "k": hexc("#1a1410")}),
    "coin": ([
        "................",
        "................",
        ".....oooooo.....",
        "....oGGGGGGo....",
        "...oGgggggggo...",
        "..oGgkgggkggdo..",
        "..oGgkkgkkggdo..",
        "..oGggkkkgggdo..",
        "..oGgggkggggdo..",
        "..oGggkkkgggdo..",
        "..oGgkgggkggdo..",
        "...oggggggddo...",
        "....oddddddo....",
        ".....oooooo.....",
        "................",
        "................",
    ], {"o": O, "g": hexc("#9a7a3a"), "G": hexc("#c8a65a"), "d": hexc("#6a5226"), "k": hexc("#3a2a12")}),
    "book": ([
        "................",
        "...ooooooooo....",
        "..obbbbbbbbbo...",
        "..obbbbrbbbbpo..",
        "..obbbrrrbbbpo..",
        "..obbrbrbrbbpo..",
        "..obbbbrbbbbpo..",
        "..obbbrbrbbbpo..",
        "..obbrbbbrbbpo..",
        "..obbbbbbbbbsso.",
        "..obbbbbbbbbpos.",
        "..obbbbbbbbbpo..",
        "..obbbbbbbbbpo..",
        "..oBBBBBBBBBpo..",
        "...ooooooooooo..",
        "................",
    ], {"o": O, "b": hexc("#1a1418"), "B": hexc("#0e0a0c"), "r": hexc("#8a1c24"), "p": PAPER[3], "s": SILVER[3]}),
}


def artifact(form):
    rows, pal = ARTIFACT_ART[form]
    return art(rows, pal)


def artifact_aura(base):
    """The rarity halo: a grey glow one pixel round the object and a few motes (tinted by the item colour in game)."""
    t = Tex(16, 16, 18600)
    src = base.copy_rows()
    for y in range(16):
        for x in range(16):
            if src[y][x][3]:
                continue
            near = 0
            for dx in (-2, -1, 0, 1, 2):
                for dy in (-2, -1, 0, 1, 2):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and src[ny][nx][3]:
                        near = max(near, 2 if abs(dx) <= 1 and abs(dy) <= 1 else 1)
            if near == 2:
                t.set(x, y, (235, 235, 235, 150))
            elif near == 1:
                t.set(x, y, (235, 235, 235, 60))
    for (x, y) in ((1, 2), (14, 4), (2, 13), (13, 14)):
        if not src[y][x][3]:
            t.set(x, y, (255, 255, 255, 200))
    return t


def spectacles_armor():
    """The spectacles worn (vanilla armour layer, 64 x 32; only the head): round wire rims at the eyes, a bridge, the
    temples running back over the ears; the lenses clear but for a glint."""
    t = Tex(64, 32, 18900)
    wire, hi, glint = SILVER[3], SILVER[5], hexc("#cfe6ff")
    # Front face of the head: x 8..15, y 8..15 (x = 8 is the wearer's right). Eyes on row 12.
    for (x0, x1) in ((9, 11), (12, 14)):
        for x in range(x0, x1 + 1):
            t.set(8 + x - 8, 11, wire)
            t.set(8 + x - 8, 13, wire)
        t.set(x0 - 1 + 0, 12, wire)
        t.set(x1 + 1, 12, wire)
        t.set(x0, 12, glint)
    t.set(11, 12, hi)
    t.set(12, 12, hi)
    # The temples along each side face (right side x 0..7, left side x 16..23), row 12, back to the ear.
    for x in range(2, 8):
        t.set(x, 12, wire)
    for x in range(16, 22):
        t.set(x, 12, wire)
    return t


def ring():
    """The Men of Letters' ring: a heavy gold signet, the order's star cut in its flat face."""
    pal = {"o": O, "g": GOLD[2], "G": GOLD[3], "Y": GOLD[5], "d": GOLD[1], "k": hexc("#3a2408"), "e": hexc("#e9e2cc")}
    return art([
        "................",
        "................",
        "....oooooooo....",
        "...oYGGGGGGGo...",
        "...oGgkgkgkgo...",
        "...oGkgegegko...",
        "...oGgkgkgkgo...",
        "....ogggggggo...",
        "....oooddoooo...",
        "...ogo....ogo...",
        "...oGo....odo...",
        "...oGo....odo...",
        "...ogoo..oodo...",
        "....oGgoogddo...",
        ".....ooddddo....",
        ".......oooo.....",
    ], pal)


def spectacles():
    pal = {"o": O, "w": SILVER[3], "W": SILVER[5], "l": hexc("#8fb8e8"), "L": hexc("#d6ecff"), "b": hexc("#5a86c0")}
    return art([
        "................",
        "................",
        "................",
        "................",
        "...oooo..oooo...",
        "..owwwwo.owwwwo.",
        ".owLllbwowLllbwo",
        "owlLlllwwwlLllwo",
        "owllllbw.wlllbwo",
        "owblllbw.wblllwo",
        ".owbbbwo.owbbwo.",
        "..owwwwo..owwwo.",
        "...oooo....ooo..",
        "................",
        "................",
        "................",
    ], pal)


def henrys_case():
    pal = {"o": O, "c": CASE[2], "C": CASE[3], "L": CASE[4], "d": CASE[1], "b": BRASS[3], "B": BRASS[5], "s": CASE[5]}
    return art([
        "................",
        "......oooo......",
        ".....odood......",
        ".....od..do.....",
        "..oooooooooooo..",
        ".oLLLLLLLLLLLLo.",
        ".oLCCBCCCCBCCco.",
        ".oLCCbCCCCbCCco.",
        ".oddddddddddddo.",
        ".oLCCCCCCCCCCco.",
        ".oLCsCsCsCsCCco.",
        ".oLCCCCCCCCCCco.",
        ".oLCCCCCCCCCCco.",
        ".obccccccccccbo.",
        "..oooooooooooo..",
        "................",
    ], pal)


def aquarian_star():
    """The Aquarian Star: the order's mark as a gold medallion on a loop."""
    t = Tex(16, 16, 18700)
    fld = emblem_field(13)
    for (x, y), k in fld.items():
        c = {"ring": GOLD[4], "star": GOLD[3], "eye_white": hexc("#efe6cc"), "iris": hexc("#2e6f8f"), "pupil": hexc("#0b0907")}[k]
        t.set(x + 1, y + 2, c)
    # The medallion's dark enamel inside the ring.
    for y in range(13):
        for x in range(13):
            if (x, y) not in fld and math.hypot(x - 6, y - 6) < 5.6:
                t.set(x + 1, y + 2, hexc("#14203a"))
    for (x, y) in ((7, 0), (6, 1), (8, 1), (7, 1)):
        t.set(x, y, GOLD[4] if (x, y) != (7, 1) else CLEAR)
    item_outline(t, O)
    return t


# --- map marks (8 px) -------------------------------------------------------------------------------------------

def bunker_mark():
    t = Tex(8, 8, 18800)
    fld = emblem_field(8, ring=True)
    for (x, y), k in fld.items():
        t.set(x, y, {"ring": GOLD[4], "star": GOLD[3], "eye_white": hexc("#f2ead2"), "iris": hexc("#2a1a0a"), "pupil": hexc("#000000")}[k])
    return t


def case_mark():
    pal = {"o": O, "r": hexc("#c4241e"), "R": hexc("#ff5a48")}
    return art([
        "oo....oo",
        "orR..Rro",
        ".orRRro.",
        "..oRRo..",
        "..oRRo..",
        ".orRRro.",
        "orR..Rro",
        "oo....oo",
    ], pal)


def generate():
    top, bottom, icon = bunker_door_tex()
    save(top, "block", "bunker_door_top")
    save(bottom, "block", "bunker_door_bottom")
    save(icon, "item", "bunker_door")
    save(walnut_tex(18200), "block", "research_desk_wood")
    save(desk_top(), "block", "research_desk_top")
    save(drawers(), "block", "research_desk_drawers")
    save(lamp_shade(), "block", "research_desk_shade")
    save(brass_tex(), "block", "research_desk_brass")
    save(keys_tex(), "block", "research_desk_keys")
    save(papers_tex(), "block", "research_desk_paper")
    research_desk_model()
    save(map_tex(), "block", "map_table_map")
    save(map_wood(), "block", "map_table_wood")
    save(map_rim(), "block", "map_table_rim")
    map_table_model()
    save(archive_side(), "block", "archive_shelf")
    save(walnut_tex(18310, grain=False), "block", "archive_shelf_top")
    archive_model()
    save(emblem_tex(32), "block", "men_of_letters_emblem")
    save(emblem_side(), "block", "men_of_letters_emblem_side")
    emblem_model()

    save(bunker_key(), "item", "bunker_key")
    for topic in NOTE_COVERS:
        save(field_notes(topic), "item", f"field_notes_{topic}")
    save(case_file(), "item", "case_file")
    save(dead_mans_blood(), "item", "dead_mans_blood")
    for form in ARTIFACT_ART:
        a = artifact(form)
        save(a, "item", f"artifact_{form}")
        save(artifact_aura(a), "item", f"artifact_{form}_aura")
    save(ring(), "item", "men_of_letters_ring")
    save(spectacles(), "item", "spellwrights_spectacles")
    save(spectacles_armor(), "models/armor", "spellwrights_spectacles_layer_1")
    save(henrys_case(), "item", "henrys_case")
    save(aquarian_star(), "item", "aquarian_star")
    save(bunker_mark(), "map/decorations", "bunker")
    save(case_mark(), "map/decorations", "case_site")


if __name__ == "__main__":
    generate()
