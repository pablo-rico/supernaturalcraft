"""The Leviathans' items (v0.25): 16x16 sprites, the goo decal (wet and dried by borax) and two effect icons.

Light comes from the top-left, like the rest of the item set.
"""

import math

import palette as P
from common import art, save
from items import vial
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix

O = P.OUTLINE
GOO = Ramp("#020203", "#060608", "#0c0c10", "#15151b", "#24242d", "#4a4a58")
BONE = Ramp("#4a3c22", "#7a6538", "#a58c55", "#c4ad74", "#dccb95", "#efe3b8")
BLUE = Ramp("#0d2140", "#163566", "#22508f", "#3570b4", "#5a95d3", "#93c1ee")
CARD = Ramp("#4a3a24", "#6d5636", "#8f7449", "#ae905e", "#c8aa77", "#e0c592")
RED = Ramp("#3a0507", "#62080d", "#8d1016", "#b51c20", "#d43a33", "#ee6a5a")
BUN = Ramp("#4a2a0c", "#74431a", "#9c6127", "#bf8038", "#d9a055", "#ecc27e")
MEAT = Ramp("#2a1308", "#4a230f", "#6a3519", "#8a4c27", "#a8653a", "#c4835a")


def _blank(seed=0):
    return Tex(16, 16, seed)


# --- borax --------------------------------------------------------------------------------------------------------

def borax_blend():
    """A gritty grey-white pile, salt crystals glinting in it, a few grains spilled."""
    t = _blank(2501)
    for y in range(16):
        for x in range(16):
            dx, dy = (x + 0.5 - 8) / 7.0, (y + 0.5 - 13.2) / 7.4
            if dy > 0.12 or dx * dx + dy * dy > 1.0:
                continue
            n = fbm(x * 2, y * 2, 2502, 32, 32, 2, 8.0)
            v = 3.4 - (dx + dy) * 1.2 + (n - 0.5) * 2.4
            c = P.SALT[max(0, min(5, int(round(v))))]
            if (x * 7 + y * 5) % 9 == 0:
                c = mix(c, P.STONE[2], 0.6)        # grey grit
            t.set(x, y, c)
    for (x, y) in ((6, 9), (9, 8), (11, 11), (4, 12), (8, 11)):
        t.set(x, y, P.SALT[5])                      # salt crystals catching the light
    for (x, y) in ((1, 14), (14, 13), (13, 15), (2, 12)):
        t.set(x, y, P.SALT[3])
    item_outline(t, O)
    return t


def borax():
    """A paper box of borax powder, its top open on the white powder, a blue label band with a mule-team stripe."""
    pal = {"o": O, "c": CARD[4], "C": CARD[5], "d": CARD[2], "D": CARD[1], "w": P.SALT[5], "W": P.SALT[3], "s": P.SALT[2],
           "b": BLUE[3], "B": BLUE[4], "n": BLUE[1], "y": hexc("#f2d257"), "k": hexc("#ffffff")}
    return art([
        "................",
        "....oooooooo....",
        "...oCwwWwwWco...",
        "..oCwWwswWwdo...",
        "..ooooooooooDo..",
        "..occcccccccdo..",
        "..oBBBBBBBBBno..",
        "..obkkbkbkkbno..",
        "..obbbbbbbbbno..",
        "..obyybybyybno..",
        "..onnnnnnnnnno..",
        "..occcccccccdo..",
        "..ocWcccccccdo..",
        "..occcccccccdo..",
        "..oddddddddDDo..",
        "...ooooooooooo..",
    ], pal)


def borax_spray():
    """A trigger spray bottle of borax solution: blue trigger head, white bottle, blue label."""
    pal = {"o": O, "b": BLUE[3], "B": BLUE[4], "n": BLUE[1], "w": P.SALT[5], "W": P.SALT[3], "s": P.SALT[2], "S": P.SALT[1],
           "k": hexc("#ffffff"), "g": P.GLASS[3]}
    return art([
        "................",
        "...oooooo.......",
        "..oBBBBbbo......",
        ".obbbbbbnnoo....",
        "..oooobbno......",
        "....obBnno......",
        "....oonbo.......",
        "....owWWso......",
        "...owwWWsSo.....",
        "...owBBBbno.....",
        "...owbkkbno.....",
        "...owbbbbno.....",
        "...owwWWsSo.....",
        "...owwWWsSo.....",
        "...osssSSSo.....",
        "....ooooooo.....",
    ], pal)


def borax_sprayer():
    """A pump sprayer: a white tank with a blue band, a pump handle on top, a hose running to a long wand."""
    pal = {"o": O, "b": BLUE[3], "B": BLUE[4], "n": BLUE[1], "w": P.SALT[5], "W": P.SALT[3], "s": P.SALT[2], "S": P.SALT[1],
           "h": P.COAT[3], "H": P.COAT[5], "m": P.STEEL[4], "M": P.STEEL[2], "k": hexc("#ffffff")}
    return art([
        ".....oooo.....oo",
        ".....oHho....omo",
        "......om....omo.",
        "......oM...omo..",
        "...ooooMoooho...",
        "..owwwWWWWWso...",
        ".owwwWWWWWssSo..",
        ".owBBBBBBBbnSo..",
        ".owbkbkkbkbnSo..",
        ".owBBBBBBBbnSo.o",
        ".owwwWWWWWssSoom",
        ".owwWWWWWsssSohm",
        ".owwWWWWWsssSoho",
        ".oswWWWWsssSSohh",
        "..osssssSSSSo.hh",
        "...oooooooooo...",
    ], pal)


# --- food -----------------------------------------------------------------------------------------------------------

def turducken_slammer():
    """A tall stacked sandwich: sesame bun, lettuce, turkey, duck, chicken, cheese, a toothpick through the top."""
    pal = {"o": O, "B": BUN[5], "b": BUN[4], "d": BUN[2], "D": BUN[1], "e": hexc("#f4ead0"), "g": hexc("#7fbf4a"),
           "G": hexc("#4f8a2a"), "t": MEAT[5], "T": MEAT[4], "u": MEAT[2], "U": MEAT[1], "c": hexc("#e9cfa0"),
           "C": hexc("#c9aa78"), "y": hexc("#f2c23a"), "Y": hexc("#d39a1a"), "p": P.WOOD[5], "r": RED[4]}
    return art([
        ".......r........",
        ".......p........",
        "....ooopooo.....",
        "..ooBBbebBboo...",
        ".oBbebbbbbebdo..",
        ".obbbbbbbbbbdo..",
        "oGgGgGgGgGgGgGo.",
        ".ottttTttttTTo..",
        ".oTTTTTTTTTTTo..",
        ".ouuuUuuuuUUUo..",
        ".occcCccccCCCo..",
        "oyYyyYyyyyYyyYo.",
        ".obbbbbbbbbbdo..",
        ".odddddddddDDo..",
        "..ooooooooooo...",
        "................",
    ], pal)


def biggersons_burger():
    """A burger half-wrapped in Biggerson's red paper, the white logo stripe across the wrapper."""
    pal = {"o": O, "B": BUN[5], "b": BUN[4], "d": BUN[2], "e": hexc("#f4ead0"), "g": hexc("#7fbf4a"), "m": MEAT[2],
           "M": MEAT[1], "y": hexc("#f2c23a"), "r": RED[3], "R": RED[4], "n": RED[1], "N": RED[0], "w": hexc("#ffffff"),
           "W": hexc("#e7e2d6")}
    return art([
        "................",
        "................",
        "....oooooooo....",
        "..ooBBbebBbboo..",
        ".oBbebbbbbbebdo.",
        ".obbbbbbbbbbbdo.",
        "ogygygygygygygdo",
        "omMmmmMmmmMmmMmo",
        "oRRrRrrrrrrRrrno",
        "oRrrrrrrrrrrrrno",
        "orwwWwwWwwWwwWno",
        "orrrrrrrrrrrrrno",
        ".orrRrrrrrrrrno.",
        ".ornrrrrrrrrnno.",
        "..onnnnnnnnnNo..",
        "...oooooooooo...",
    ], pal)


# --- the righteous bone and the three bloods ------------------------------------------------------------------------

BONE_ROWS = [
    "................",
    "............oo..",
    "...........oHho.",
    "..........oHhhho",
    "..........ohhbo.",
    ".........ohhbo..",
    "........ohhbo...",
    ".......ohhbo....",
    "......ohhbo.....",
    ".....ohhbo......",
    "....ohhbo.......",
    ".oooohbo........",
    "ohhhhbbo........",
    "ohhbbbo.........",
    ".obbdo..........",
    "..oo............",
]


def righteous_bone():
    """An old bone, yellowed and pitted: the bone of a righteous man."""
    pal = {"o": O, "H": BONE[5], "h": BONE[4], "b": BONE[3], "d": BONE[1]}
    t = art(BONE_ROWS, pal)
    for (x, y) in ((9, 7), (6, 10), (12, 4)):
        t.set(x, y, BONE[2])                       # pits
    return t


def anointed_righteous_bone():
    """The bone dipped in the three bloods: streaks of gold-red, black-red and dark red, glinting."""
    t = righteous_bone()
    fallen, king, alpha = hexc("#d7902a"), hexc("#3a0a0e"), hexc("#7a0d14")
    streaks = [
        (fallen, [(12, 2), (12, 3), (11, 4), (11, 5), (10, 6)]),
        (king, [(13, 3), (12, 4), (11, 6), (10, 7), (9, 8), (8, 9)]),
        (alpha, [(9, 7), (8, 8), (7, 9), (6, 10), (5, 11), (4, 12), (3, 12)]),
        (fallen, [(6, 9), (5, 10)]),
        (king, [(2, 12), (2, 13), (3, 13)]),
    ]
    for c, pts in streaks:
        for (x, y) in pts:
            if t.get(x, y)[3] and t.get(x, y) != O:
                t.set(x, y, c)
    for (x, y) in ((12, 3), (8, 8), (4, 12)):
        t.set(x, y, hexc("#ffe9a0"))               # glints of gold
    return t


def _aura(t, color, alpha=110):
    """A faint halo of light just outside the silhouette."""
    src = t.copy_rows()
    for y in range(16):
        for x in range(16):
            if src[y][x][3]:
                continue
            if any(0 <= x + dx < 16 and 0 <= y + dy < 16 and src[y + dy][x + dx][3]
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                t.set(x, y, (color[0], color[1], color[2], alpha))
    return t


def fallen_blood():
    """Blood of a fallen angel: red shot with gold, a faint golden glow round the vial."""
    liquid = Ramp("#5a2208", "#8a3a10", "#b25a1a", "#d07e2a", "#e8a748", "#fbd68a")
    return _aura(vial(liquid, hexc("#fff2b0")), hexc("#f2d257"), 90)


def kings_blood():
    """Blood of the King of Hell: dark red clouded with black."""
    liquid = Ramp("#050203", "#160406", "#2a070b", "#4a0c12", "#6a121a", "#8e1d24")
    t = vial(liquid, hexc("#a0303a"))
    for (x, y) in ((6, 11), (8, 10), (9, 12), (7, 12)):
        t.set(x, y, hexc("#050203"))               # black swirls
    return t


def alpha_blood():
    """Blood of an Alpha: thick, dark crimson, clinging to the glass."""
    liquid = Ramp("#2a0306", "#4a050b", "#6e0a12", "#8f121b", "#ad1d25", "#cc3a3a")
    t = vial(liquid, hexc("#e05a5a"))
    for (x, y) in ((6, 8), (9, 8), (9, 7)):
        t.set(x, y, liquid[3])                      # it coats the glass above the level
    return t


# --- the tablet -----------------------------------------------------------------------------------------------------

GLYPHS = [("111", "100", "110", "100", "111"), ("010", "111", "010", "010", "011"), ("110", "101", "110", "100", "100"),
          ("101", "101", "111", "001", "001"), ("011", "100", "110", "100", "011"), ("100", "110", "101", "110", "100")]


def leviathan_tablet():
    """A broken fragment of the Word of God tablet: grey stone, a jagged break, lines of carved script."""
    t = _blank(2601)
    # Outline of the fragment: a slab with a jagged break along the right and the bottom.
    edge_r = [13, 13, 14, 13, 12, 13, 14, 14, 13, 12, 11, 12, 11, 10]
    edge_b = 14
    for y in range(1, edge_b):
        for x in range(2, edge_r[y - 1] + 1):
            n = fbm(x * 2, y * 2, 2602, 32, 32, 2, 6.0)
            v = 3.2 - (x + y) * 0.06 + (n - 0.5) * 1.6
            t.set(x, y, P.STONE[max(0, min(5, int(round(v))))])
    # Carved lines of script (2 px deep shadow under each stroke).
    for row, y0 in enumerate((3, 9)):
        for g in range(3):
            gl = GLYPHS[(row * 3 + g) % len(GLYPHS)]
            for cy, bits in enumerate(gl):
                for cx, b in enumerate(bits):
                    x, y = 3 + g * 4 + cx, y0 + cy
                    if b == "1" and t.get(x, y)[3]:
                        t.set(x, y, P.STONE[0])
                        if t.get(x + 1, y + 1)[3] and t.get(x + 1, y + 1) != P.STONE[0]:
                            t.set(x + 1, y + 1, P.STONE[5])
    item_outline(t, O)
    return t


# --- the goo on the floor -------------------------------------------------------------------------------------------

def goo():
    """Leviathan goo pooled on the floor: glossy black, a wet blue-grey sheen, ragged at the edges so blocks join up."""
    t = _blank(2701)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2702, 16, 16, 3, 4.0)
            e = min(x, y, 15 - x, 15 - y)
            if e == 0 and n < 0.45:
                continue
            v = 0.6 + 2.2 * n
            c = GOO[max(0, min(5, int(round(v))))]
            sheen = fbm(x * 1.3 + 7, y, 2703, 16, 16, 2, 3.0)
            if sheen > 0.64:
                c = mix(c, hexc("#6a7088"), min(0.6, (sheen - 0.64) * 2.6))
            if (x * 5 + y * 11) % 31 == 0:
                c = mix(c, hexc("#b8c0d8"), 0.55)   # a wet glint
            t.set(x, y, (c[0], c[1], c[2], 240))
    return t


def goo_dried():
    """The same goo dried by borax: a cracked white crust, grey seams of dead goo between the flakes."""
    t = _blank(2711)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 2702, 16, 16, 3, 4.0)
            e = min(x, y, 15 - x, 15 - y)
            if e == 0 and n < 0.45:
                continue
            # Voronoi-ish cracks: distance to the nearest of a few seeds, cracks where two are nearly equal.
            ds = sorted(math.hypot(x + 0.5 - sx, y + 0.5 - sy) for sx, sy in
                        ((3, 3), (11, 2), (7, 8), (14, 9), (2, 12), (9, 14), (13, 15), (5, 6)))
            crack = ds[1] - ds[0] < 1.0
            if crack:
                c = mix(GOO[3], P.SALT[1], 0.35)
            else:
                v = 3.4 + (n - 0.5) * 2.0 - (0.6 if ds[0] < 1.2 else 0)
                c = P.SALT[max(0, min(5, int(round(v))))]
            t.set(x, y, (c[0], c[1], c[2], 245))
    return t


# --- effect icons ---------------------------------------------------------------------------------------------------

K = hexc("#1E1410")

ICONS = {
    # Sedated: a heavy-lidded eye sinking shut, a drift of z's.
    "sedated": ([
        "..................",
        "...........zzzzz..",
        "..............z...",
        ".............z....",
        "............zzzzz.",
        ".......zzz........",
        "........z.........",
        ".......zzz........",
        "..................",
        "....llllllllll....",
        "..llllllllllllll..",
        ".lllllllllllllllk.",
        ".kLLLLLLLLLLLLLLk.",
        ".kwkwkiippikwkwwk.",
        "..kwwwiippiiwwwk..",
        "....kkkkkkkkkk....",
        "..................",
        "..................",
    ], {"k": K, "z": hexc("#bcd2f0"), "l": hexc("#9a8a7c"), "L": hexc("#5e5048"), "w": hexc("#e8e2d6"),
        "i": hexc("#5a6f88"), "p": hexc("#141418")}),
    # Borax burn: a blob of black goo fizzing white where the borax eats it.
    "borax_burn": ([
        "........w.........",
        "...w.......w......",
        ".....kkkkkk....w..",
        "....kgggwggk......",
        "...kgGgggggwk.....",
        "..kggwgGgggggk....",
        "..kgggggggwGgk..w.",
        ".kgGgggwggggggk...",
        ".kgggggggggGggk...",
        ".kggwgGgggggwgk...",
        ".kgggggggggggggk..",
        "..kggGggwgggGgk...",
        "..kgggggggggggk...",
        "...kggwgggGggk....",
        "....kkgggggkk.....",
        "......kkkkk.......",
        "..................",
        "..................",
    ], {"k": K, "g": hexc("#16161c"), "G": hexc("#4a4a58"), "w": hexc("#f4f4f8")}),
}


def icon(rows, pal):
    t = art(rows, pal)
    item_outline(t, K)
    return t


SPRITES = {
    "borax_blend": borax_blend, "borax": borax, "borax_spray": borax_spray, "borax_sprayer": borax_sprayer,
    "turducken_slammer": turducken_slammer, "biggersons_burger": biggersons_burger, "righteous_bone": righteous_bone,
    "anointed_righteous_bone": anointed_righteous_bone, "fallen_blood": fallen_blood, "kings_blood": kings_blood,
    "alpha_blood": alpha_blood, "leviathan_tablet": leviathan_tablet,
}


def generate():
    for name, fn in SPRITES.items():
        save(fn(), "item", name)
    save(goo(), "block", "leviathan_goo")
    save(goo_dried(), "block", "leviathan_goo_dried")
    for name, (rows, pal) in ICONS.items():
        save(icon(rows, pal), "mob_effect", name)


if __name__ == "__main__":
    generate()
