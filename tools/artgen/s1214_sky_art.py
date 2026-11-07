"""The v0.41 skies (textures only; the dimension owners write the DimensionSpecialEffects):

  environment/apocalypse_world_sky.png   128x128, tileable (drawn like the End sky): a smoke-brown overcast, folds of darker
                                         smoke, thin ember streaks drifting through it
  environment/apocalypse_world_sun.png   32x32 (vanilla sun layout): a dim red disc behind the smoke, a soft dark rim
  environment/the_empty_sky.png          128x128, tileable: pure black, the faintest violet nebula and a few dead points
"""

import math

from common import save
from pixelkit import Tex, fbm, hexc, mix

SMOKE = [hexc(c) for c in ("#120c08", "#1e1510", "#2c2018", "#3a2b20", "#4a3828", "#5c4630")]
EMBER = [hexc(c) for c in ("#5a1406", "#8a2408", "#c0400c", "#ec6a1c", "#ffa040")]
VIOLET = [hexc(c) for c in ("#000000", "#06030a", "#0d0716", "#160c24", "#221436", "#341e4e")]


def r(ramp, v):
    """A smooth sample of a ramp (skies are seen big: no banding)."""
    v = max(0.0, min(len(ramp) - 1.0, v))
    i = min(len(ramp) - 2, int(v))
    return mix(ramp[i], ramp[i + 1], v - i)


def apocalypse_sky():
    N = 128
    t = Tex(N, N, 57001)
    for y in range(N):
        for x in range(N):
            n = fbm(x, y, 57001, N, N, 4, 4.0)
            m = fbm(x, y, 57002, N, N, 3, 6.0)
            c = r(SMOKE, 1.2 + n * 3.6 - (0.8 if m > 0.62 else 0))
            # Ember streaks: thin bands along a slow drift, tileable (periodic in x and y).
            s = math.sin((x / N) * 2 * math.pi * 3 + (y / N) * 2 * math.pi * 1 + 4 * (n - 0.5))
            e = fbm(x, y, 57003, N, N, 2, 4.0)
            if s > 0.985 and e > 0.5:
                c = mix(c, r(EMBER, (e - 0.5) * 9), 0.7)
            t.set(x, y, c)
    return t


def apocalypse_sun():
    t = Tex(32, 32, 57010)
    for y in range(32):
        for x in range(32):
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            if d < 9:
                k = d / 9
                c = mix(hexc("#b02a14"), hexc("#5a0e06"), k * k)
                t.set(x, y, c)
            elif d < 13:
                a = int(120 * (1 - (d - 9) / 4) ** 2)
                if a > 0:
                    t.set(x, y, (90, 20, 10, a))
    return t


def empty_sky():
    N = 128
    t = Tex(N, N, 57020)
    for y in range(N):
        for x in range(N):
            n = fbm(x, y, 57020, N, N, 4, 4.0)
            m = fbm(x, y, 57021, N, N, 3, 8.0)
            v = max(0.0, n - 0.45) * 11 * (0.5 + 0.7 * m)
            c = r(VIOLET, v)
            t.set(x, y, c)
    for i in range(14):
        x = int(fbm(i * 9, 3, 57022, 128, 128, 1, 2.0) * 997) % N
        y = int(fbm(5, i * 7, 57023, 128, 128, 1, 2.0) * 991) % N
        t.set(x, y, hexc("#3a2c4a"))
    return t


def generate():
    save(apocalypse_sky(), "environment", "apocalypse_world_sky")
    save(apocalypse_sun(), "environment", "apocalypse_world_sun")
    save(empty_sky(), "environment", "the_empty_sky")


if __name__ == "__main__":
    generate()
