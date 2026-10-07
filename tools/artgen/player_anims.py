"""Whole-body player animations for the Colt, played by PlayerAnimationLib when it is installed
(compat/pal). GeckoLib/Bedrock format; the animation's name is its id (supernaturalcraft:<name>).

Only PlayerAnimationLib's bones are used: head, body, torso, right_arm, left_arm, right_leg,
left_leg. Same rotation convention as this repo's GeckoLib clips (-X swings a limb forward), but
PlayerAnimationLib names the arms mirrored from the player's own view (checked in game): its
"left_arm" is a right-handed shooter's gun arm, "right_arm" the other. Left-handed shooters are
mirrored by the library. Firing itself is not here: the gun arm then follows the gaze through
ColtArmPoses, kick included, which needs no expressions.
"""

from animkit import AnimFile
from common import ASSETS

PATH = ASSETS + "/player_animations/colt.json"
CAPACITY = 5
GUN, OFF = "left_arm", "right_arm"
INTRO, PER, SEAT, OUTRO = 10, 8, 6, 8    # ticks, as reward/colt/ColtReload


def animations():
    f = AnimFile()

    a = f.new("colt_dry", 0.5)
    a.rot(GUN, (0, [-80, 0, 8]), (0.18, [-84, 0, 8]), (0.22, [-78, 0, 8]), (0.5, [-80, 0, 8]))

    for n in range(1, CAPACITY + 1):
        length = (INTRO + PER * n + OUTRO) / 20
        end = (INTRO + PER * n) / 20
        a = f.new(f"colt_reload_{n}", length)
        held = [-58, 0, 18]          # the gun up before the chest, cylinder to the off hand
        a.rot(GUN, (0, [-20, 0, 0]), (0.3, held, "easeOutCubic"), (end, held), (length, [-20, 0, 0], "easeInOutCubic"))
        a.rot("head", (0, [0, 0, 0]), (0.3, [24, 0, 0], "easeOutCubic"), (end, [24, 0, 0]), (length, [0, 0, 0], "easeInOutSine"))
        belt, gun = [-12, 0, 6], [-62, 0, -26]
        keys = [(0, [0, 0, 0]), (0.25, belt, "easeOutCubic")]
        for k in range(n):
            t0 = (INTRO + PER * k) / 20
            keys += [(t0 + 0.05, belt), (t0 + 0.22, gun, "easeOutCubic"), (t0 + 0.36, [-58, 0, -22])]
        keys += [(end + 0.15, [-10, 0, 0], "easeInCubic"), (length, [0, 0, 0])]
        a.rot(OFF, *_monotone(keys))

    a = f.new("colt_inspect", 2.4)
    look = [-78, 0, 28]
    a.rot(GUN, (0, [-20, 0, 0]), (0.4, look, "easeOutCubic"), (1.15, [-82, 0, 24], "easeInOutSine"),
          (1.25, [-66, 0, 16]), (2.05, [-66, 0, 16]), (2.4, [-20, 0, 0], "easeInOutCubic"))
    a.rot("head", (0, [0, 0, 0]), (0.4, [20, 0, 0], "easeOutCubic"), (1.15, [18, 0, 0]), (1.3, [8, 0, 0]),
          (2.4, [0, 0, 0], "easeInOutSine"))
    return f


def _monotone(keys):
    out, last = [], -1
    for k in keys:
        if k[0] > last + 1e-4:
            out.append(k)
            last = k[0]
    return out


def generate():
    animations().write(PATH)


if __name__ == "__main__":
    generate()
