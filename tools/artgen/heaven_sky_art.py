"""v0.18 Heaven's sky texture (HeavenAssets.SKY_TEXTURE = textures/environment/heaven_sky.png). 2D art work.

A 256 x 256 sea of clouds, seamless in both directions (tile it), meant for the cloud layer Heaven draws BELOW its islands
(a big horizontal plane, scrolled slowly; or a few planes at different heights and scales for parallax). RGBA:
  colour  white-gold billow tops lit from the north-west, warm peach in their folds, a cool blue-grey in the deepest shade
  alpha   cloud density (clear gaps between the banks show the sky colour the code paints beneath)
The sky's colour gradient and the sun are left to the code (DimensionSpecialEffects / the sky renderer).
"""

import math

from common import save
from pixelkit import Tex, fbm, hexc, mix

N = 256
TOP = hexc("#fffdf6")
WARM = hexc("#fbf0dc")
FOLD = hexc("#efd0b0")
SHADE = hexc("#b4bfd2")


def density(x, y):
    base = fbm(x % N, y % N, 22001, N, N, 4, 4.0)
    detail = fbm(x % N, y % N, 22002, N, N, 2, 8.0)
    return base * 0.75 + detail * 0.25


def smooth(e0, e1, v):
    t = max(0.0, min(1.0, (v - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)


def sky():
    t = Tex(N, N, 22000)
    d = [[density(x, y) for x in range(N)] for y in range(N)]
    for y in range(N):
        for x in range(N):
            v = d[y][x]
            a = smooth(0.34, 0.52, v)
            if a <= 0:
                continue
            # Light from the north-west: the slope towards the light brightens, away from it shades.
            slope = (v - d[(y - 5) % N][(x - 5) % N]) * 5.0
            thick = smooth(0.45, 0.8, v)
            lit = max(0.0, min(1.0, 0.62 + slope + 0.2 * thick))
            if lit > 0.6:
                c = mix(WARM, TOP, (lit - 0.6) / 0.4)
            elif lit > 0.3:
                c = mix(FOLD, WARM, (lit - 0.3) / 0.3)
            else:
                c = mix(SHADE, FOLD, lit / 0.3)
            t.set(x, y, (c[0], c[1], c[2], int(255 * a)))
    return t


def generate():
    save(sky(), "environment", "heaven_sky")


if __name__ == "__main__":
    generate()
