"""The Chain of Command's challenge-rite tokens (v0.27): the Gauntlet of Ash (thrown down before a demon or a fallen angel)
and the Seal of Summons (pressed before an angel of the Host). 16x16 item sprites."""
import palette as P
from common import art, save
from pixelkit import hexc


def gauntlet():
    pal = {"o": P.OUTLINE, "d": hexc("#2a2523"), "m": hexc("#4a413c"), "l": hexc("#6e625a"), "h": hexc("#958677"),
           "e": hexc("#c2410c"), "E": hexc("#f59e0b"), "a": hexc("#8a8580")}
    return art([
        "................",
        "....oo.oo.......",
        "...omlomlo.oo...",
        "...omlomlooml...",
        "...omlomlomlo...",
        "...omlomlomloo..",
        "...odmlmlmlmmlo.",
        "..odmmmmmmmmmlo.",
        "..odmmeEemmmlo..",
        "..odmmmemmmmlo..",
        "...odmmmmmmlo...",
        "...oddmmmmmo....",
        "...ohahahaho....",
        "...oaaaaaaao....",
        "....ooooooo.....",
        "................",
    ], pal)


def seal():
    pal = {"o": P.OUTLINE, "g": hexc("#a16207"), "G": hexc("#ca8a04"), "y": hexc("#facc15"), "w": hexc("#fef9c3"),
           "W": hexc("#ffffff"), "r": hexc("#e5ddc8")}
    return art([
        "................",
        ".....oooooo.....",
        "...ooGGyyGGoo...",
        "..oGGyywwyyGGo..",
        "..oGyywWWwyyGo..",
        ".oGyywwWWwwyyGo.",
        ".oGywWWWWWWwyGo.",
        ".oGyywwWWwwyyGo.",
        ".oGgyywWWwyygGo.",
        "..oGgyywwyygGo..",
        "..oGGgyyyygGGo..",
        "...ooGGggGGoo...",
        ".....oooooo.....",
        "......orro......",
        ".....orrrro.....",
        "......oooo......",
    ], pal)


def generate():
    save(gauntlet(), "item", "gauntlet_of_ash")
    save(seal(), "item", "seal_of_summons")
