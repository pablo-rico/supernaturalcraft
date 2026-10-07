"""16x16 item sprites, hand-placed as string art. Light comes from the top-left."""

import palette as P
from common import art, save
from pixelkit import Tex, hexc, item_outline

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
}


def generate():
    for name, fn in ITEMS.items():
        save(fn(), "item", name)
