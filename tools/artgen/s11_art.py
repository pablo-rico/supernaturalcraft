"""Season 11 (v0.42) art outside the Hands slice: Casifer and the fogged villagers' overlay. Contract: S11Assets.

Casifer: Lucifer wearing Castiel. He uses Castiel's own rig (geo/entity/castiel.geo.json, made by godstiel_art), so his
texture is built from that rig: godstiel_art's NPC look, painted again with a few materials swapped per cube tag. The UVs are
the same by construction (generate() checks the packing against castiel.png's size and the geo file). Same beige trench coat
and navy suit, but worn the way the Devil would: the coat closed and buttoned below the lapels, the belt buckled, the tie's
loose tail tucked away and the blade pressed flat, the skin a little paler, heavy lids over a cold stare and a smug half
smile. casifer_glowmask.png lights only his eyes, Lucifer's red.

The fogged villager overlay (fogged_villager_veins.png) is a 64 x 64 cutout drawn over the vanilla villager model: black
veins climbing the neck and the face, eyes gone black, a dark stain around the mouth and the nose, faint veins on the hands.
"""

import json

import godstiel_art as G
from chuck_art import h01
from common import ASSETS, save
from pixelkit import Ramp, Tex, hexc, mix, shade

# --- Casifer ------------------------------------------------------------------------------------------------------------

SKIN_PALE = Ramp(*[mix(a, b, 0.32) for a, b in zip(G.SKIN.c, G.SKIN_GOD.c)])
RED = Ramp("#2a0403", "#5c0806", "#96100a", "#d42014", "#ff4a2e", "#ffb49a")
SMUG = Ramp("#4a2a24", "#663a32", "#7c4a40", "#8f5a4e", "#a26a5c", "#b47c6c")   # paler lips than Castiel's
FACE_SEED = 39001   # build_head's seed: the same skin mottling as Castiel's


def _is_iris_face(x, y):
    return y in (7, 8) and ((2 <= x <= 5 and x - 2 in (1, 2)) or (10 <= x <= 13 and x - 10 in (1, 2)))


def face_override(look):
    """The north face: Castiel's, with the Devil's lids, eyes and smirk painted over it."""
    sk = look.skin(FACE_SEED)

    def over(x, y, base):
        s = sk("north", x, y, 16, 16)
        # Heavy, half-lowered lids: the top row of each eye goes under the lid (a cold, bored stare).
        if y == 7 and (2 <= x <= 5 or 10 <= x <= 13):
            ex = x - 2 if x <= 5 else x - 10
            if ex in (1, 2):
                return RED[1]
            return shade(s, 0.72)
        if y == 8 and (2 <= x <= 5 or 10 <= x <= 13):
            ex = x - 2 if x <= 5 else x - 10
            if ex in (1, 2):
                return RED[3] if ex == 1 else RED[2]
            return mix(G.EYE_WHITE, s, 0.25)
        if y == 6 and x in (1, 14):
            return shade(s, 0.82)               # the lids' outer creases, longer
        # A cool sneer: the bridge of the nose a touch sharper.
        if x == 6 and y in (9, 10):
            return shade(s, 0.9)
        # The mouth: a thin line pulled up at his left corner (the viewer's right), a crease in that cheek.
        if y == 13 and 5 <= x <= 9:
            return SMUG[2] if x == 5 else SMUG[0] if x >= 8 else SMUG[1]
        if y == 12 and x in (10, 11):
            return SMUG[0] if x == 10 else SMUG[2]
        if y == 11 and x == 12:
            return shade(s, 0.8)                # the smirk's dimple
        if y == 12 and x == 12:
            return shade(s, 0.9)
        if y == 13 and x in (10, 11):
            return shade(s, 1.04)               # the cheek pushed up and lit
        if y == 13 and x == 4:
            return shade(s, 0.86)               # his right corner pressed flat
        if y == 14 and 6 <= x <= 9:
            return mix(SMUG[3], s, 0.45 if x < 9 else 0.7)
        return base

    return over


def eye_shell(face, x, y, w, h):
    """The eyes' shell (12 x 2): the same mask as Castiel's, lidded, the irises the Devil's red."""
    col = x % 8 - 2 if x < 8 else x - 10
    if not 0 <= col <= 3 or (5 < x < 8):
        return None
    if col in (1, 2):
        if y == 0:
            return RED[1]
        return RED[4] if col == 2 else RED[3]
    return mix(SKIN_PALE[2], SKIN_PALE[3], 0.5) if y == 0 else mix(G.EYE_WHITE, SKIN_PALE[3], 0.2)


def eye_glow(face, x, y, w, h):
    col = x % 8 - 2 if x < 8 else x - 10
    if not 0 <= col <= 3 or (5 < x < 8):
        return None
    if col in (1, 2):
        if y == 0:
            return hexc("#a0140c")
        return hexc("#ff6a48") if col == 2 else hexc("#ff3020")
    return hexc("#3a0402") if y == 1 else None      # the faintest red at the iris' rim


def coat_shell(orig):
    """The coat buttoned from the lapels' tips down: the right front crossed over, two rows of buttons."""
    coat = G.trench_mat(G.Look("npc"), 39300)

    def m(f, x, y, w, h):
        if f != "north" or not (4 <= x <= w - 5) or y < 10:
            c = orig(f, x, y, w, h)
            if f == "north" and c is not None and x in (1, w - 2) and y % 5 == 3 and y > 6:
                return G.TRENCH[3]          # the open fronts' buttons are now on the closure
            return c
        if y == 10:
            return G.TRENCH[4] if x in (7, 8) else coat(f, x, y, w, h)
        if x == 8:
            return G.TRENCH[5]              # the crossing front's edge
        if x == 9:
            return G.TRENCH[1]              # its shadow
        if x in (5, 11) and y in (12, 17):
            return G.BUTTON[4]
        if x in (5, 11) and y in (13, 18):
            return G.BUTTON[1]
        return coat(f, x, y, w, h)
    return m


def coat_belt(orig):
    """The belt buckled at the front."""
    def m(f, x, y, w, h):
        if f == "north" and 4 <= x < w - 4:
            if x in (7, 8):
                return G.BUTTON[5] if y == 0 else G.BUTTON[3]
            if x == 9:
                return G.BUTTON[2]
            return G.TRENCH[2] if y == 1 else G.TRENCH[3]
        return orig(f, x, y, w, h)
    return m


TIE_TUCK = 9   # the blade's rows below this go under the buttoned coat (its closure starts at the shell's row 10)


def tie_blade(f, x, y, w, h):
    """Pressed flat: an even blue, a crisp edge, no rumples; tucked into the buttoned coat."""
    if f == "down" or (f != "up" and y >= TIE_TUCK):
        return None
    if f != "up" and y == TIE_TUCK - 1 and x in (0, w - 1):
        return None
    if f == "north":
        c = G.TIE[3] if 1 <= x <= w - 2 else G.TIE[2]
        if x == 2 and y < h - 3:
            c = G.TIE[4]
        if y >= h - 2:
            c = G.TIE[1]
        return c
    return G.TIE[2]


def casifer_rig():
    """godstiel_art's rig with the NPC look, the skin swapped while it builds (the Look reads SKIN when it is made)."""
    old = G.SKIN
    G.SKIN = SKIN_PALE
    try:
        m = G.rig("npc")
    finally:
        G.SKIN = old
    look = G.Look("npc")
    look.skin_ramp = SKIN_PALE
    face = face_override(look)
    jobs = []
    for c, mat, glow, shading in m.jobs:
        if c.tag == "skull":
            mat = (lambda orig: lambda f, x, y, w, h: face(x, y, orig(f, x, y, w, h)) if f == "north" else orig(f, x, y, w, h))(mat)
        elif c.tag == "eyes":
            mat = eye_shell
        elif c.tag == "coat_shell":
            mat = coat_shell(mat)
        elif c.tag == "coat_belt":
            mat = coat_belt(mat)
        elif c.tag == "tie_blade":
            mat = tie_blade
        elif c.tag == "tie_tail":
            mat = lambda f, x, y, w, h: None
        elif c.tag == "tie_knot":
            mat = lambda f, x, y, w, h: G.TIE[4] if f == "up" or (f == "north" and y == 0) else G.TIE[3]
        jobs.append((c, mat, glow, shading))
    m.jobs = jobs
    return m


def casifer_textures():
    m = casifer_rig()
    t, _ = m.build(gutter=1, seed=39000)
    g = Tex(t.w, t.h, 1)
    for c, *_ in m.jobs:
        if c.tag == "eyes":
            u, v, w, h = c.rects["north"]
            for y in range(h):
                for x in range(w):
                    col = eye_glow("north", x, y, w, h)
                    if col is not None:
                        g.set(u + x, v + y, col)
        elif c.tag == "skull":
            u, v, w, h = c.rects["north"]
            for y in range(h):
                for x in range(w):
                    if _is_iris_face(x, y):
                        g.set(u + x, v + y, hexc("#ff3020") if y == 8 else hexc("#a0140c"))
    return m, t, g


def _check_uvs(m):
    """The packing must be Castiel's: compare every cube's UVs with geo/entity/castiel.geo.json."""
    with open(ASSETS + "/geo/entity/castiel.geo.json", encoding="utf-8") as fh:
        geo = json.load(fh)
    desc = geo["minecraft:geometry"][0]["description"]
    assert (desc["texture_width"], desc["texture_height"]) == (m.rig.tex_w, m.rig.tex_h), "texture size differs from Castiel's"
    theirs = []
    for b in geo["minecraft:geometry"][0]["bones"]:
        for c in b.get("cubes", []):
            theirs.append(json.dumps(c.get("uv"), sort_keys=True))
    mine = []
    for b in m.rig.bones:
        for c in b.cubes:
            mine.append(sorted((f, tuple(r)) for f, r in c.rects.items()))
    assert len(theirs) == len(mine), f"cube count differs: {len(theirs)} vs {len(mine)}"
    for tj, mc in zip(theirs, mine):
        uv = json.loads(tj)
        for f, (u, v, w, h) in mc:
            assert uv[f]["uv"][0] == u and uv[f]["uv"][1] == v, f"UV differs on a {f} face"


# --- The fogged villager's overlay (vanilla villager UVs, 64 x 64) --------------------------------------------------------

VEIN = (8, 6, 10, 255)
VEIN_SOFT = (14, 10, 16, 200)
VEIN_FAINT = (18, 14, 20, 130)
STAIN = (10, 6, 10, 120)
STAIN_DEEP = (6, 4, 6, 185)
EYE_BLACK = (4, 3, 5, 255)
EYE_SHEEN = (34, 30, 40, 255)


# Veins as hand-drawn paths: (x, y) texels, strongest first (they thin as they climb). V = full, s = soft, f = faint.
def _path(t, pts, strengths):
    for (x, y), k in zip(pts, strengths):
        c = {"V": VEIN, "s": VEIN_SOFT, "f": VEIN_FAINT}[k]
        if t.get(x, y)[3] < c[3]:
            t.set(x, y, c)


def fogged_overlay():
    """Vanilla villager.png: head 0,0 (8 x 10 x 8; the face at 8..15 x 8..17: the brow at row 13, the eyes at row 14, white
    9/14 and iris 10/13, the mouth at row 16, the nose box's front over 11..12), the head's right side 0..7 and left side
    16..23, its back 24..31; nose 24,0 (front 26..27 x 2..5); the crossed arms 44,22 (4 x 8 x 4 boxes) and their middle bar
    40,38 (8 x 4 x 4, front 44..51 x 42..45), both bare skin in vanilla. The body and the robe are covered by clothes."""
    t = Tex(64, 64, 41101)
    # --- the face: the eyes gone black, bruised sockets, veins from the jaw into the eyes and up past the brow.
    for x in (9, 10, 13, 14):
        t.set(x, 14, EYE_BLACK)
    t.set(10, 14, EYE_SHEEN)                 # a dull wet glint, never a pupil
    t.set(13, 14, EYE_SHEEN)
    for x in (9, 14):
        t.set(x, 15, STAIN)
    _path(t, [(8, 17), (8, 16), (9, 15), (9, 14)], "VVVV")
    _path(t, [(15, 17), (15, 16), (14, 15), (14, 14)], "VVVV")
    _path(t, [(10, 17), (10, 16)], "Vs")
    _path(t, [(13, 17), (13, 16)], "Vs")
    _path(t, [(8, 13), (8, 12), (9, 11), (9, 10)], "Vsff")     # past the brow at the temples
    _path(t, [(15, 13), (15, 12), (14, 11)], "Vsf")
    _path(t, [(12, 12), (12, 11), (11, 10)], "sff")
    # --- the mouth: a black, wet stain spreading out from under the nose.
    for x in range(10, 14):
        t.set(x, 16, STAIN_DEEP)
    for x in (11, 12):
        t.set(x, 17, STAIN_DEEP)
    t.set(9, 16, STAIN)
    t.set(14, 16, STAIN)
    # --- the nose: blackened at the tip and round the nostrils.
    for x in (26, 27):
        t.set(x, 5, STAIN_DEEP)
        t.set(x, 4, STAIN)
    t.set(25, 5, STAIN)                       # the nose's sides
    t.set(28, 5, STAIN)
    _path(t, [(27, 3), (27, 2)], "sf")
    # --- the head's sides: veins up from the neck along the jaw toward the temples, thick at the bottom.
    _path(t, [(7, 17), (6, 16), (6, 15), (5, 14), (5, 13), (4, 12)], "VVVssf")
    _path(t, [(3, 17), (3, 16), (2, 15), (2, 14)], "Vssf")
    _path(t, [(5, 16), (4, 16)], "sf")
    _path(t, [(16, 17), (17, 16), (17, 15), (18, 14), (18, 13), (19, 12)], "VVVssf")
    _path(t, [(20, 17), (20, 16), (21, 15), (21, 14)], "Vssf")
    _path(t, [(18, 16), (19, 16)], "sf")
    # --- the back of the head: two, faint, from the nape.
    _path(t, [(26, 17), (26, 16), (27, 15)], "ssf")
    _path(t, [(29, 17), (30, 16), (30, 15)], "sff")
    # --- the hands: faint veins over the backs of the hands (the middle bar) and up the forearms.
    _path(t, [(45, 45), (45, 44), (46, 43)], "sff")
    _path(t, [(49, 45), (50, 44), (50, 43), (49, 42)], "sfff")
    _path(t, [(46, 41), (47, 40)], "ff")
    _path(t, [(49, 33), (49, 32), (50, 31), (50, 30)], "sfff")
    _path(t, [(45, 33), (45, 32), (46, 31)], "sff")
    _path(t, [(53, 33), (54, 32), (54, 31)], "sff")
    return t


def generate():
    m, t, g = casifer_textures()
    _check_uvs(m)
    save(t, "entity", "casifer")
    save(g, "entity", "casifer_glowmask")
    save(fogged_overlay(), "entity", "fogged_villager_veins")


if __name__ == "__main__":
    generate()
