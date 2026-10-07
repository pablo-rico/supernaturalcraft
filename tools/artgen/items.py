"""16x16 item sprites, hand-placed as string art. Light comes from the top-left."""

import math

import palette as P
from common import art, save
from pixelkit import Ramp, Tex, fbm, hexc, item_outline, mix

O = P.OUTLINE


def salt():
    pal = {"o": O, "W": P.SALT[5], "w": P.SALT[3], "m": P.SALT[2], "s": P.SALT[1], "d": P.SALT[0]}
    return art([
        "................",
        "................",
        "................",
        "................",
        "........o.......",
        ".......oWo......",
        "......oWwmo.o...",
        ".....oWwwmooWo..",
        "..o.oWwWwwmWwmo.",
        ".oWooWwwwmwwwmo.",
        "oWwmWwwmwwwwmsmo",
        "oWwwwwwwwmwwmmso",
        "omwwmwwwmwwmmsso",
        ".osmmssmmmsmsso.",
        "..oooooooooooo..",
        "................",
    ], pal)


def sulfur():
    pal = {"o": O, "H": P.SULFUR[5], "y": P.SULFUR[4], "Y": P.SULFUR[3], "m": P.SULFUR[2], "d": P.SULFUR[1]}
    return art([
        "................",
        "................",
        "......o.........",
        ".....oHo...o....",
        "....oHyYo.oHo...",
        "....oHyYmoHyYo..",
        "...oHyyYmoyyYmo.",
        "..oHyyyYmmyYYmo.",
        "..oyyyYYmmyYmdo.",
        ".oHyyYYmdYYmmdo.",
        ".oyyYYmmdYmmddo.",
        ".omyYmmddmmddo..",
        "..ommddddddoo...",
        "...ooooooooo....",
        "................",
        "................",
    ], pal)


def vial(liquid, glint):
    """A corked glass vial. `liquid` is a Ramp."""
    pal = {"o": O, "c": P.WOOD[3], "C": P.WOOD[5], "g": P.GLASS[1], "G": P.GLASS[5], "a": P.GLASS[3],
           "R": liquid[5], "r": liquid[3], "m": liquid[2], "D": liquid[1], "x": glint}
    return art([
        "................",
        "......oooo......",
        "......oCco......",
        "......occo......",
        ".....ogGago.....",
        "......oGao......",
        "......oGao......",
        ".....oGaaao.....",
        "....oGaaaaao....",
        "....oGRrrrmo....",
        "....oxrrrrmo....",
        "....orrrmmDo....",
        "....ormmmDDo....",
        ".....oDDDDo.....",
        "......oooo......",
        "................",
    ], pal)


def demon_blood():
    return vial(P.BLOOD, P.BLOOD[5])


def holy_water():
    t = flask(P.WATER)
    return t


def flask(liquid):
    pal = {"o": O, "c": P.WOOD[3], "C": P.WOOD[5], "g": P.GLASS[1], "G": P.GLASS[5], "a": P.GLASS[3],
           "R": liquid[5], "r": liquid[3], "m": liquid[2], "D": liquid[1], "+": P.GOLD[4], "k": P.GOLD[2]}
    return art([
        "................",
        "......oooo......",
        "......oCco......",
        "......occo......",
        ".....oGaago.....",
        "......oGao......",
        "....ooGaaaoo....",
        "...oGaaaaaaao...",
        "..oGaaaaaaaaao..",
        "..oGRrrr+rrrmo..",
        "..oRrrr+++rrmo..",
        "..orrrrr+rrmmo..",
        "..ormmrrkrmmDo..",
        "...ommmmmmDDo...",
        "....oooooooo....",
        "................",
    ], pal)


def hellfire_ember():
    pal = {"o": P.HELLFIRE[0], "1": P.HELLFIRE[1], "2": P.HELLFIRE[2], "3": P.HELLFIRE[3],
           "4": P.HELLFIRE[4], "5": P.HELLFIRE[5], "a": P.ASH[2], "A": P.ASH[4], "k": O}
    return art([
        "................",
        "................",
        "................",
        "......k.........",
        ".....k4k..k.....",
        "....kk34kk3k....",
        "...kaa2345kk....",
        "..kaA12345a1k...",
        "..kA1234553aAk..",
        "..ka12345532ak..",
        "..kaA234432aAk..",
        "...ka122221aak..",
        "...kkaa111aakk..",
        ".....kkkkkkk....",
        "................",
        "................",
    ], pal)


def stick(r):
    pal = {"o": O, "W": r[5], "w": r[4], "m": r[3], "s": r[2], "d": r[1]}
    return art([
        "................",
        "................",
        "...........oo...",
        "..........oWWo..",
        ".........oWwwmo.",
        "........oWwwmso.",
        ".......oWwwmso..",
        "......oWwwmso...",
        ".....oWwwmso....",
        "....oWwwmso.....",
        "...oWwwmso......",
        "..owwwmso.......",
        "..omwmso........",
        "..osmso.........",
        "...ooo..........",
        "................",
    ], pal)


def chalk():
    return stick(P.CHALK)


def blood_chalk():
    t = stick(P.CHALK)
    # Dip the business end in blood.
    for y in range(8, 15):
        for x in range(0, 9):
            c = t.get(x, y)
            if c[3] and c != O:
                lum = (c[0] + c[1] + c[2]) / (3 * 255)
                t.set(x, y, P.BLOOD.t(min(1.0, lum * 1.1)))
    return t


def devils_trap_item():
    """A folded parchment with the trap sigil painted on it."""
    pal = {"o": O, "p": P.PARCHMENT[4], "P": P.PARCHMENT[5], "q": P.PARCHMENT[3], "Q": P.PARCHMENT[2],
           "r": P.BLOOD[4], "R": P.BLOOD[3]}
    return art([
        "................",
        "................",
        "..oooooooooooo..",
        "..oPPpppppppqo..",
        "..oPppRrrrRpqo..",
        "..oppRpprppRqo..",
        "..opRppRrpppRo..",
        "..oprprRRrrprQo.",
        "..opRprrrRprRQo.",
        "..oprpRpprRprqo.",
        "..oppRppRppRpqo.",
        "..opppRrrrRppqo.",
        "..oqqqqqqqqqqQo.",
        "..oQQQQQQQQQQQo.",
        "...ooooooooooo..",
        "................",
    ], pal)


def rubys_knife():
    """Ruby's demon-killing knife: a curved blade etched with runes, dark wooden grip."""
    pal = {"o": O, "W": P.STEEL[5], "w": P.STEEL[4], "m": P.STEEL[3], "s": P.STEEL[2], "e": P.STEEL[1],
           "h": P.WOOD[2], "H": P.WOOD[4], "g": P.GOLD[3]}
    return art([
        "................",
        "............oo..",
        "...........oWwo.",
        "..........oWwmo.",
        ".........oWwems.",
        "........oWwemso.",
        ".......oWwmeso..",
        "......oWwemso...",
        ".....oWwmeso....",
        "..o.oWmesso.....",
        "..ogoomsso......",
        "...ogHoso.......",
        "...ohHgo........",
        "..ohHo.o........",
        ".ohho...........",
        ".oo.............",
    ], pal)


def angel_blade():
    """An angel blade: a long triple-edged silver spike with a cloth-wrapped grip."""
    pal = {"o": O, "W": P.SILVER[5], "w": P.SILVER[4], "m": P.SILVER[3], "s": P.SILVER[2], "d": P.SILVER[1],
           "c": P.LEATHER[3], "C": P.LEATHER[5], "x": P.GRACE[5]}
    return art([
        "..............o.",
        ".............oxo",
        "............oWwo",
        "...........oWwmo",
        "..........oWwmo.",
        ".........oWwmdo.",
        "........oWwmdo..",
        ".......oWwmdo...",
        "......oWwmdo....",
        ".....oWwmdo.....",
        "..o.oWmsdo......",
        "..omomsdo.......",
        "...omCdo........",
        "..ocCco.........",
        ".ocCco..........",
        ".ooo............",
    ], pal)


def grimoire():
    pal = {"o": O, "l": P.LEATHER[2], "L": P.LEATHER[3], "h": P.LEATHER[4], "d": P.LEATHER[1],
           "p": P.PARCHMENT[4], "P": P.PARCHMENT[3], "g": P.GOLD[4], "G": P.GOLD[2], "r": P.BLOOD[4]}
    return art([
        "................",
        "...oooooooooo...",
        "..ohhhhhhhhhLo..",
        "..ohLLLLLLLLlo..",
        "..ohLLLgggLLlpo.",
        "..ohLLgLrLgLlPo.",
        "..ohLLgrrrgLlpo.",
        "..ohLLgLrLgLlPo.",
        "..ohLLLgggLLlpo.",
        "..ohLLLLLLLLlPo.",
        "..ohLLGLLGLLlpo.",
        "..ohLLLLLLLLlPo.",
        "..ohLLLLLLLLlpo.",
        "..oddddddddddoo.",
        "...oooooooooo...",
        "................",
    ], pal)


def spell_scroll():
    pal = {"o": O, "p": P.PARCHMENT[4], "P": P.PARCHMENT[5], "q": P.PARCHMENT[3], "Q": P.PARCHMENT[2],
           "r": P.BLOOD[4], "R": P.BLOOD[2], "i": P.ASH[2]}
    return art([
        "................",
        "...........oo...",
        "..........oPpo..",
        ".........oPpqo..",
        "........oPpiqo..",
        ".......oPpiqo...",
        "......oPpipqo...",
        ".....oPpiqqo....",
        "....oPrrRqo.....",
        "...oPprRrQo.....",
        "..oPpipqQo......",
        ".oPpiqqQo.......",
        ".opqqQQo........",
        ".oQQQoo.........",
        "..ooo...........",
        "................",
    ], pal)


def sigil_page():
    pal = {"o": O, "p": P.PARCHMENT[4], "P": P.PARCHMENT[5], "q": P.PARCHMENT[3], "Q": P.PARCHMENT[2],
           "g": P.GOLD[3], "k": P.ASH[1]}
    return art([
        "................",
        "...oooooooo.....",
        "...oPPpppppoo...",
        "...oPpppppppqo..",
        "...oPppgggppqo..",
        "...oPpgpkpgpqo..",
        "...oPpgkkkgpqo..",
        "...oPpgpkpgpqo..",
        "...oPppgggppqo..",
        "...oPppppppppqo.",
        "...oPpkkpkkpqo..",
        "...oPppppppppqo.",
        "...oqqqqQqqqqo..",
        "...oQQo.oQQQo...",
        "...oo....ooo....",
        "................",
    ], pal)


def enochian_ink():
    pal = {"o": O, "g": P.GLASS[1], "G": P.GLASS[5], "a": P.GLASS[3], "c": P.WOOD[3], "C": P.WOOD[5],
           "k": hexc("#120a1e"), "v": hexc("#4a2380"), "V": hexc("#8a4be0"), "f": P.SILVER[4]}
    return art([
        "................",
        "................",
        "........f.......",
        ".......f........",
        "......ooo.......",
        "......oCo.......",
        ".....oGaao......",
        "....oGaaaao.....",
        "...oGkkkkkko....",
        "...oVkvkkkko....",
        "...okkkvkkko....",
        "...okkkkkvko....",
        "...okkvkkkko....",
        "....okkkkko.....",
        ".....ooooo......",
        "................",
    ], pal)


def hunters_amulet():
    """A small horned bronze face on a leather cord."""
    pal = {"o": O, "c": P.LEATHER[3], "C": P.LEATHER[5], "g": P.GOLD[3], "G": P.GOLD[5], "d": P.GOLD[1], "m": P.GOLD[2]}
    return art([
        "...c........c...",
        "...c........c...",
        "....c......c....",
        "....c......c....",
        ".....c....c.....",
        ".....c....c.....",
        "......c..c......",
        "....o..cc..o....",
        "...oGo.oo.oGo...",
        "...ogGoggoGgo...",
        "....ogGGGGgo....",
        "....oGdGGdGo....",
        "....ogGmmGgo....",
        ".....ogGGgo.....",
        "......oddo......",
        ".......oo.......",
    ], pal)


def key(cracked=False):
    """An ornate iron key with a ruby set in the bow, its bit shaped like a cage door."""
    pal = {"o": O, "i": P.STEEL[3], "I": P.STEEL[5], "d": P.STEEL[1], "r": P.BLOOD[5], "R": P.BLOOD[3],
           "g": P.GOLD[4], "G": P.GOLD[2], "c": P.HELLFIRE[4]}
    t = art([
        "................",
        "....oooo........",
        "...oIiigo.......",
        "..oIgrrRgo......",
        "..oigrRRgo......",
        "..oiggRggo......",
        "...odiigoo......",
        "....oiIo........",
        ".....oiIo.......",
        "......oiIo......",
        ".......oiIo.....",
        "........oiIoo...",
        ".........oiIIo..",
        "..........oiio..",
        "...........oIo..",
        "............o...",
    ], pal)
    if cracked:
        for (x, y) in ((5, 3), (6, 4), (6, 5), (7, 6), (9, 10), (10, 11)):
            t.set(x, y, P.HELLFIRE[4] if (x + y) % 2 else O)
    return t


def archangel_blade():
    """Lucifer's blade: the angel blade's triple edge in white gold, with a burning core."""
    pal = {"o": O, "W": hexc("#ffffff"), "w": P.GRACE[4], "m": P.GRACE[3], "s": P.GRACE[2], "d": P.GRACE[1],
           "c": P.HELLFIRE[4], "g": P.GOLD[4], "G": P.GOLD[2], "h": P.LEATHER[2]}
    return art([
        "..............o.",
        ".............oWo",
        "............oWwo",
        "...........oWwmo",
        "..........oWwcmo",
        ".........oWwcmo.",
        "........oWwcmo..",
        ".......oWwcmso..",
        "......oWwcmso...",
        ".....oWwcmso....",
        "..ooowWcmso.....",
        "..ogGgwmso......",
        "...ohGGoo.......",
        "..ohhGo.........",
        ".ohho...........",
        ".oo.............",
    ], pal)


def colt_bullet():
    pal = {"o": O, "b": P.GOLD[3], "B": P.GOLD[5], "d": P.GOLD[1], "l": P.SILVER[4], "L": P.SILVER[5]}
    return art([
        "................",
        "................",
        "................",
        "....o.....o.....",
        "...oLo...oLo....",
        "...olo...olo....",
        "..oBbbo.oBbbo...",
        "..oBbdo.oBbdo...",
        "..oBbdo.oBbdo...",
        "..oBbdo.oBbdo...",
        "..oooooooooooo..",
        "......o.........",
        ".....oLo........",
        "....oBbbo.......",
        "....oBbdo.......",
        "....ooooo.......",
    ], pal)


def lucifers_grace():
    """A vial of swirling blue-white light."""
    t = vial(P.CAGE, hexc("#ffffff"))
    for (x, y) in ((6, 10), (7, 9), (8, 10), (9, 11), (7, 11), (6, 12), (8, 12)):
        t.set(x, y, P.GRACE[5] if (x + y) % 2 else hexc("#cfe9ff"))
    return t


# --- v0.8: the spell bowl, ghosts and the crossroads ---------------------------------------------

def blood_vial():
    """A slim corked tube of someone's blood, a paper tag tied to its neck (not the round demon_blood flask)."""
    pal = {"o": O, "C": P.WOOD[5], "c": P.WOOD[3], "d": P.WOOD[1], "G": P.GLASS[5], "a": P.GLASS[3],
           "w": hexc("#ffffff"), "R": P.BLOOD[4], "r": P.BLOOD[3], "m": P.BLOOD[2], "D": P.BLOOD[1],
           "x": P.BLOOD[5], "s": hexc("#cbbd9a"), "t": P.PARCHMENT[4], "T": P.BLOOD[2]}
    return art([
        "................",
        "......ooo.......",
        ".....oCCco......",
        ".....occdo......",
        "....ooGaaoo.....",
        ".....oGwaos.....",
        ".....oGaao.s....",
        ".....oGaao..s...",
        ".....oGaao.oooo.",
        ".....oRrxo.otto.",
        ".....oRrmo.oTto.",
        ".....orrmo.oooo.",
        ".....ormDo......",
        ".....oDmDo......",
        "......ooo.......",
        "................",
    ], pal)


COLLAR = Ramp("#24090a", "#431311", "#661d17", "#86291f", "#a63b2b", "#c4563d")


def pet_collar():
    """A red leather collar with silver studs and a gold name tag, seen in perspective."""
    pal = {"o": O, "d": COLLAR[1], "l": COLLAR[2], "L": COLLAR[3], "c": COLLAR[5], "s": P.SILVER[5],
           "S": P.SILVER[3], "g": P.GOLD[3], "G": P.GOLD[5], "k": P.GOLD[1]}
    return art([
        "................",
        "................",
        "....oooooooo....",
        "..oodddddddcoo..",
        ".odcoooooooocLo.",
        "odco........oLlo",
        "oSo..........oLo",
        "oso..........oLo",
        "oLlo........oLlo",
        ".oLcoo....oocLo.",
        "..ooLLsLLLsLoo..",
        "....oooogoooo...",
        ".......ogo......",
        "......oGGgo.....",
        "......oGgko.....",
        ".......ooo......",
    ], pal)


def spell_page():
    """An aged page of the bowl's rites: a flame over a bowl in red-brown ink above lines of Latin."""
    pal = {"o": O, "P": P.PARCHMENT[5], "p": mix(P.PARCHMENT[4], P.PARCHMENT[3], 0.35), "q": P.PARCHMENT[2],
           "Q": P.PARCHMENT[1], "k": hexc("#3b2a1c"), "K": hexc("#5e4630"), "r": P.HELLFIRE[3], "R": P.HELLFIRE[5],
           "b": hexc("#7f5427"), "B": hexc("#cb9a52"), "s": mix(P.PARCHMENT[3], P.PARCHMENT[2], 0.5)}
    return art([
        "................",
        "..oooooooooo....",
        "..oPPPPPPPPpo...",
        "..oPpprppsppqo..",
        "..oPprRrpKkpqo..",
        "..oPpprppppsqo..",
        "..oPbBBbpKkkqo..",
        "..oPpbbpppppqo..",
        "..oPppppsppppqo.",
        "..oPkKkpkkKpqo..",
        "..oPppppppppqo..",
        "..oPkkpKkkpkqo..",
        "..oPpspppppppqo.",
        "..oPkKkkpkKpqo..",
        "..oqqQqqqQqqo...",
        "..ooooooooooo...",
    ], pal)


def crossroads_contract():
    """A contract rolled at the head: lines of terms, a signature in blood and a red wax seal with ribbons."""
    t = Tex(16, 16, 1201)
    for y in range(4, 14):
        for x in range(2, 14):
            n = fbm(x, y, 1202, 16, 16, 2, 4.0)
            t.set(x, y, P.PARCHMENT[3 if n < 0.45 else 4])
        t.set(2, y, P.PARCHMENT[5])
        t.set(13, y, P.PARCHMENT[2])
    for x in range(2, 14):
        t.set(x, 13, P.PARCHMENT[1])
    # The roll at the head.
    for x in range(1, 15):
        t.set(x, 1, P.PARCHMENT[5])
        t.set(x, 2, P.PARCHMENT[3])
        t.set(x, 3, P.PARCHMENT[1])
    for y in (1, 2, 3):
        t.set(1, y, P.PARCHMENT[2])
        t.set(14, y, P.PARCHMENT[2])
    t.set(14, 2, P.PARCHMENT[0])
    ink = hexc("#2e2018")
    for (y, runs) in ((5, ((4, 11),)), (7, ((4, 6), (8, 12))), (9, ((4, 8), (10, 11))), (11, ((10, 12),))):
        for (a, b) in runs:
            for x in range(a, b + 1):
                t.set(x, y, ink if (x + y) % 5 else hexc("#4a3424"))
    # Signature in blood.
    for (x, y) in ((8, 11), (9, 10), (10, 11), (11, 10), (12, 11), (9, 11)):
        t.set(x, y, P.BLOOD[4])
    # Wax seal over the bottom-left corner, two ribbon tails below it.
    for (x, y) in ((3, 15), (4, 14), (6, 14), (7, 15)):
        t.set(x, y, P.BLOOD[2])
    t.disc(5.5, 12.5, 2.6, P.BLOOD[3])
    t.set(4, 11, P.BLOOD[5])
    t.set(5, 11, P.BLOOD[4])
    t.set(4, 12, P.BLOOD[4])
    t.set(5, 12, P.BLOOD[1])
    t.set(6, 13, P.BLOOD[1])
    t.set(6, 12, P.BLOOD[2])
    t.set(5, 13, P.BLOOD[2])
    item_outline(t, O)
    return t


ECTO = Ramp("#2f6a70", "#5a9fa4", "#8fcdcd", "#c3ebe8", "#e6fbf8", "#ffffff")


def _lumps(t, lumps, ramp, seed, alpha=255, speckle=0.0):
    """Overlapping round lumps, each shaded on its own (lit top-left), nearer lumps drawn last."""
    for (cx, cy, r) in lumps:
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                d = math.hypot(dx, dy) / r
                if d > 1:
                    continue
                n = fbm(x, y, seed, 16, 16, 2, 8.0)
                v = 3.0 - (dx + dy) / r * 1.3 - d * d * 0.8 + (n - 0.5) * speckle
                c = ramp[max(0, min(5, int(round(v))))]
                t.set(x, y, (c[0], c[1], c[2], alpha))


def ectoplasm():
    """A glob of pale, translucent spirit goo: a clear rim round a cloudy core, gloss, long drips."""
    t = Tex(16, 16, 1211)
    lumps = [(8.0, 8.6, 4.9), (5.2, 11.6, 2.6), (11.4, 11.2, 2.8)]
    inside = {}
    for y in range(16):
        for x in range(16):
            d = min(math.hypot(x + 0.5 - cx, y + 0.5 - cy) / r for (cx, cy, r) in lumps)
            if d <= 1:
                inside[(x, y)] = d
    for (x, y) in ((4, 14), (4, 15), (11, 14), (11, 15), (12, 14)):
        inside[(x, y)] = 0.95
    for (x, y), d in inside.items():
        n = fbm(x, y, 1212, 16, 16, 2, 8.0)
        edge = any((x + dx, y + dy) not in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        v = 2.2 + (n - 0.5) * 1.2 + (0.9 if edge and (x < 8 or y < 8) else 0) - (0.6 if edge and x >= 8 and y >= 8 else 0)
        c = ECTO[max(0, min(5, int(round(v))))]
        a = 235 if edge else int(150 + (1 - d) * 60)  # clearer toward the rim, cloudy at heart
        t.set(x, y, (c[0], c[1], c[2], a))
    for (x, y) in ((5, 6), (5, 7), (6, 5), (7, 4), (4, 9)):  # a curved gloss
        t.set(x, y, ECTO[5])
    for (x, y) in ((10, 10), (9, 11), (7, 9), (11, 8)):  # bubbles caught inside
        t.set(x, y, (ECTO[4][0], ECTO[4][1], ECTO[4][2], 240))
    item_outline(t, hexc("#183034"))
    return t


SOIL = Ramp("#140d08", "#24180f", "#352417", "#4a3321", "#62462e", "#7d5d3f")


def grave_dirt():
    """A handful of dark grave earth: crumbling clods, a pebble and a pale root."""
    t = Tex(16, 16, 1221)
    _lumps(t, [(9.6, 7.6, 3.0), (5.8, 9.4, 3.4), (11.4, 11.0, 3.2), (7.4, 12.0, 3.3)], SOIL, 1222, speckle=2.4)
    for (x, y) in ((2, 14), (14, 14), (3, 6), (13, 5), (12, 15)):  # crumbs
        t.set(x, y, SOIL[3])
    for (x, y, c) in ((10, 11, P.STONE[4]), (11, 11, P.STONE[2]), (10, 12, P.STONE[1]), (5, 8, P.STONE[3])):
        t.set(x, y, c)
    for (x, y) in ((6, 12), (7, 11), (8, 11), (9, 10), (10, 9)):  # a pale root
        t.set(x, y, hexc("#a8946c"))
    t.set(11, 9, hexc("#7c6a4b"))
    item_outline(t, O)
    return t


LINEN = Ramp("#5f5444", "#8d7f66", "#b5a585", "#d3c4a2", "#e9ddc0", "#f8f1df")


def pouch(t, cloth, seed, cinch, knot):
    """A small drawstring pouch: frilled neck, round body lit from the top-left."""
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            body = ((px - 8) / 5.3) ** 2 + ((py - 10.6) / 4.6) ** 2 <= 1
            neck = 5 <= y <= 6 and 6 <= x <= 9
            frill = 2 <= y <= 4 and 5 <= x <= 10 and not (y == 2 and x in (6, 9)) and not (y == 2 and x in (5, 10))
            if not (body or neck or frill):
                continue
            n = fbm(x, y, seed, 16, 16, 2, 8.0)
            v = 3.1 + (n - 0.5) * 1.2
            if body:
                v += -((px - 6) * 0.16 + (py - 8.5) * 0.22)
            if frill:
                v += 0.6 if (x + y) % 2 else -0.2
            if (x * 3 + y) % 5 == 0 and body:
                v -= 0.35  # weave
            t.set(x, y, cloth[max(1, min(5, int(round(v))))])
    for x in range(5, 11):
        t.set(x, 5, cinch[3] if x % 2 else cinch[2])
    t.set(5, 6, cinch[3])
    t.set(4, 7, cinch[2])
    t.set(4, 8, knot)


def protection_bag():
    """A clean linen pouch tied with red thread, a blue sun-cross stitched on it and a silver charm."""
    t = Tex(16, 16, 1231)
    pouch(t, LINEN, 1232, P.BLOOD, P.BLOOD[4])
    t.stamp([".bbb.", "b.b.b", "bBBBb", "b.b.b", ".bbb."], {"b": hexc("#2b4f8a"), "B": hexc("#5f8ad0")}, 6, 9)
    # A little silver medal hanging from the knot.
    for (x, y, c) in ((11, 6, P.SILVER[3]), (12, 7, P.SILVER[3]), (12, 8, P.SILVER[5]), (13, 8, P.SILVER[3]),
                      (12, 9, P.SILVER[2]), (13, 9, P.SILVER[4])):
        t.set(x, y, c)
    item_outline(t, O)
    return t


ITEMS = {
    "salt": salt,
    "sulfur": sulfur,
    "demon_blood": demon_blood,
    "holy_water": holy_water,
    "hellfire_ember": hellfire_ember,
    "chalk": chalk,
    "blood_chalk": blood_chalk,
    "devils_trap": devils_trap_item,
    "rubys_knife": rubys_knife,
    "angel_blade": angel_blade,
    "grimoire": grimoire,
    "spell_scroll": spell_scroll,
    "sigil_page": sigil_page,
    "enochian_ink": enochian_ink,
    "hunters_amulet": hunters_amulet,
    "key_to_the_cage": key,
    "cracked_key": lambda: key(True),
    "archangel_blade": archangel_blade,
    "colt_bullet": colt_bullet,
    "lucifers_grace": lucifers_grace,
    "blood_vial": blood_vial,
    "pet_collar": pet_collar,
    "spell_page": spell_page,
    "crossroads_contract": crossroads_contract,
    "ectoplasm": ectoplasm,
    "grave_dirt": grave_dirt,
    "protection_bag": protection_bag,
}


def generate():
    for name, fn in ITEMS.items():
        save(fn(), "item", name)
