"""Sprites for the v0.2 arsenal (tiers I-II) and the runes."""

import palette as P
from common import art, save
from pixelkit import hexc

O = P.OUTLINE


def silver_machete():
    """A broad, slightly hooked silver blade on a black-wrapped grip."""
    pal = {"o": O, "W": P.SILVER[5], "w": P.SILVER[4], "m": P.SILVER[3], "s": P.SILVER[2], "d": P.SILVER[1],
           "h": P.ASH[2], "H": P.ASH[4], "r": P.STEEL[3]}
    return art([
        "...........oooo.",
        "..........oWWwmo",
        ".........oWwwmso",
        "........oWwwmsso",
        ".......oWwwmsso.",
        "......oWwwmsso..",
        ".....oWwwmsso...",
        "....oWwwmsdo....",
        "...oWwmmsdo.....",
        "..ooWmsddo......",
        "..ororooo.......",
        "...ohHo.........",
        "..ohHho.........",
        ".ohHho..........",
        ".ohho...........",
        ".oo.............",
    ], pal)


def exorcists_mace():
    """A flanged head of blessed iron and gold, a cross set in its crown, on a wrapped haft."""
    pal = {"o": O, "I": P.STEEL[5], "i": P.STEEL[4], "s": P.STEEL[2], "d": P.STEEL[1], "g": P.GOLD[4], "G": P.GOLD[2],
           "w": P.WOOD[3], "W": P.WOOD[5], "c": P.LEATHER[3]}
    return art([
        "........ooo.....",
        "......ooIgIoo...",
        ".....oIigggiIo..",
        "....oIiiigiiido.",
        "....oiIggggGido.",
        "....oiiigGiiido.",
        "....osiigGiisdo.",
        ".....osdiisddo..",
        "......ooWsoo....",
        ".....ocWo.......",
        "....ocWco.......",
        "...owWco........",
        "..owWco.........",
        ".ocWco..........",
        ".oWco...........",
        ".oo.............",
    ], pal)


def ember_staff():
    """A gnarled staff whose head cradles a burning ember."""
    pal = {"o": O, "w": P.WOOD[2], "W": P.WOOD[4], "1": P.HELLFIRE[2], "2": P.HELLFIRE[3], "3": P.HELLFIRE[4], "4": P.HELLFIRE[5],
           "k": P.ASH[1]}
    return art([
        "...........o....",
        "..........o3o...",
        ".........o343o..",
        "........oWo32oo.",
        "........ok2o1kWo",
        ".......oWoo1oWo.",
        "......oWwoo.oo..",
        ".....oWwo.......",
        "....oWwo........",
        "...oWwo.........",
        "..oWwo..........",
        ".oWwo...........",
        ".owo............",
        "oWo.............",
        "oo..............",
        "................",
    ], pal)


def enochian_orb():
    """A violet glass orb holding a golden glyph, set in a brass claw."""
    pal = {"o": O, "v": hexc("#2a0f4a"), "V": hexc("#43177a"), "m": hexc("#6327b3"), "l": hexc("#8a4be0"), "L": hexc("#d5b8ff"),
           "g": P.GOLD[4], "G": P.GOLD[2], "b": P.GOLD[3]}
    return art([
        "................",
        ".....oooooo.....",
        "....oLlmmmVo....",
        "...oLlmmgmmVo...",
        "...olmmgggmVo...",
        "...ommmgmgmVo...",
        "...ommgmmmgvo...",
        "...oVmmmmmVvo...",
        "....oVVVVVvo....",
        "...oGboooobGo...",
        "...oG.oGGo.Go...",
        "......oGbo......",
        ".....obGGbo.....",
        ".....oGGGGo.....",
        "......oooo......",
        "................",
    ], pal)


def void_essence():
    """A tear of the Darkness: a black droplet holding one violet star."""
    pal = {"o": O, "k": hexc("#06040c"), "d": hexc("#140a24"), "v": hexc("#2d1a48"), "m": hexc("#5a3a9a"),
           "s": hexc("#e8deff"), "l": hexc("#b48cff")}
    return art([
        "................",
        ".......oo.......",
        "......omvo......",
        "......ovdo......",
        ".....omvddo.....",
        ".....ovddko.....",
        "....omvddkko....",
        "....ovdlskko....",
        "...omvdsldkko...",
        "...ovddlkkkko...",
        "...ovddkkkkko...",
        "...omvddkkkdo...",
        "....omvdddvo....",
        ".....oommoo.....",
        ".......oo.......",
        "................",
    ], pal)


def eclipse_sight():
    """A stoppered vial of black, an eclipsed eye floating in it."""
    pal = {"o": O, "g": hexc("#cfd8e6"), "G": hexc("#8a9aae"), "k": hexc("#06040c"), "d": hexc("#140a24"),
           "y": hexc("#ffe9b8"), "w": hexc("#ffffff"), "c": P.WOOD[3], "C": P.WOOD[1]}
    return art([
        "................",
        "......oooo......",
        "......oCco......",
        "......oCco......",
        ".....oggGGo.....",
        "......ogGo......",
        ".....ogkkGo.....",
        "....ogkdddGo....",
        "...ogkdyyydGo...",
        "...ogdywkwyGo...",
        "...ogdykkkyGo...",
        "...ogkdyyydGo...",
        "...oGkkdddkGo...",
        "....oGGkkGGo....",
        ".....oooooo.....",
        "................",
    ], pal)


RUNE_GLYPHS = {
    "edge": ["...#...", "..###..", ".#.#.#.", "...#...", "...#...", "..#.#..", ".#...#."],
    "ember": ["...#...", "..#.#..", ".#...#.", ".#.#.#.", ".#...#.", "..#.#..", "...#..."],
    "frost": ["...#...", "#..#..#", ".#.#.#.", "..###..", ".#.#.#.", "#..#..#", "...#..."],
    "leech": [".#...#.", ".#...#.", "..#.#..", "...#...", "..###..", ".#...#.", "..###.."],
    "sanctity": ["...#...", "...#...", "#######", "...#...", "...#...", "...#...", "..###.."],
    "swiftness": ["#...#..", ".#...#.", "..#...#", ".#...#.", "#...#..", ".......", "......."],
    "resonance": ["..###..", ".#...#.", "#..#..#", "#.###.#", "#..#..#", ".#...#.", "..###.."],
    "focus": ["#.....#", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#.....#"],
    "echo": ["#.#.#.#", ".......", "#.....#", "..#.#..", "#.....#", ".......", "#.#.#.#"],
    "void": ["..###..", ".#####.", "##...##", "##...##", "##...##", ".#####.", "..###.."],
    "hymn": ["..###..", ".#...#.", ".#.#.#.", ".#...#.", "#######", "...#...", "..#.#.."],
}
RUNE_COLORS = {"edge": "#C9CED6", "ember": "#FF6A1F", "frost": "#9FD8F0", "leech": "#D23A2A", "sanctity": "#F2E6B0",
               "swiftness": "#7FE07A", "resonance": "#B685FF", "focus": "#8FB8E8", "echo": "#FFFFFF", "void": "#6327B3",
               "hymn": "#FFD27A"}


def rune(name=None):
    """A slate tablet; carved runes glow in their colour."""
    from pixelkit import Tex, fbm
    t = Tex(16, 16, 31)
    for y in range(2, 15):
        for x in range(3, 13):
            edge = x in (3, 12) or y in (2, 14)
            n = fbm(x, y, 31, 16, 16, 2, 5.0)
            t.set(x, y, O if edge else P.DEEPSLATE[2 + int(round(n * 2))])
    for x in (3, 12):
        for y in (2, 14):
            t.set(x, y, (0, 0, 0, 0))
    if name:
        c = hexc(RUNE_COLORS[name])
        for gy, row in enumerate(RUNE_GLYPHS[name]):
            for gx, ch in enumerate(row):
                if ch == "#":
                    t.set(4 + gx + 0, 4 + gy, c)
    else:
        for x in range(5, 11):
            t.set(x, 8, P.DEEPSLATE[4])
    return t


SPRITES = {
    "silver_machete": silver_machete,
    "exorcists_mace": exorcists_mace,
    "ember_staff": ember_staff,
    "enochian_orb": enochian_orb,
}


def hellforge_faces():
    """Black iron and obsidian; the front shows a furnace mouth of live coals under an anvil plate."""
    from blocks import stone_base
    from pixelkit import fbm
    side = stone_base(P.DEEPSLATE, 41, 0.8)
    for x in range(16):
        side.set(x, 0, P.STEEL[2])
        side.set(x, 1, P.STEEL[3])
        side.set(x, 15, P.STEEL[1])
    for y in range(2, 15):
        side.set(0, y, P.STEEL[1])
        side.set(15, y, P.STEEL[1])
    front = stone_base(P.DEEPSLATE, 42, 0.8)
    for x in range(16):
        front.set(x, 0, P.STEEL[2])
        front.set(x, 1, P.STEEL[3])
    for y in range(6, 14):
        for x in range(3, 13):
            if y == 6 or x in (3, 12):
                front.set(x, y, P.STEEL[1])
            else:
                n = fbm(x, y, 43, 16, 16, 2, 4.0)
                front.set(x, y, P.HELLFIRE[1 + int(round(n * 4))])
    top = stone_base(P.STEEL, 44, 0.6)
    for x in range(2, 14):
        top.set(x, 7, P.STEEL[5])
        top.set(x, 8, P.STEEL[4])
    return side, front, top


def generate():
    save(void_essence(), "item", "void_essence")
    save(eclipse_sight(), "item", "eclipse_sight")
    for name, fn in SPRITES.items():
        save(fn(), "item", name)
    save(rune(None), "item", "rune_blank")
    for name in RUNE_GLYPHS:
        save(rune(name), "item", "rune_" + name)
    side, front, top = hellforge_faces()
    save(side, "block", "hellforge_side")
    save(front, "block", "hellforge_front")
    save(top, "block", "hellforge_top")
