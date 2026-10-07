#!/usr/bin/env python3
"""Generates the Author's sounds (v0.10): pure-stdlib synthesis to WAV, then OGG Vorbis.

Every sound is deterministic (one random.Random per file, seeded from its name) and goes through
synth.finish (DC block, trim, fades, normalise) before encoding. OGGs land in
src/main/resources/assets/supernaturalcraft/sounds/chuck/; ChuckAssetData points the sound events at them.

Mono through GStreamer's libvorbis when available (positional in game); otherwise stereo via ffmpeg (see oggenc).
Usage:  python3 tools/soundgen/generate.py [name ...]     (no names = everything)
"""

import os
import random
import sys
import time
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import music  # noqa: E402
import oggenc  # noqa: E402
import sfx  # noqa: E402
from synth import finish, stats, write_wav  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "supernaturalcraft", "sounds", "chuck")
WAV = os.environ.get("SN_SOUND_TMP",
                     "/private/tmp/claude-501/-Users-pabloricoal-IdeaProjects/48bfac8a-fc08-4858-bb40-e6e315583706/scratchpad/art2/wav")

SR = 32000          # effects
MUSIC_SR = 22050    # the long piece


def _keys(sr, seed):
    rng = random.Random(seed)
    return [sfx.type_key(sr, rng, i % 4) for i in range(6)]


# name -> (builder(sr, rng), peak dBFS, sample rate)
SOUNDS = {}


def sound(name, peak=-1.5, sr=SR):
    def deco(fn):
        SOUNDS[name] = (fn, peak, sr)
        return fn
    return deco


for _i in range(4):
    sound(f"type_{_i + 1}", -1.5)(lambda sr, rng, i=_i: sfx.type_key(sr, rng, i))
for _i in range(2):
    sound(f"carriage_{_i + 1}", -1.5)(lambda sr, rng, i=_i: sfx.carriage(sr, rng, i))
    sound(f"snap_{_i + 1}", -1.0)(lambda sr, rng, i=_i: sfx.snap(sr, rng, i))
    sound(f"key_impact_{_i + 1}", -1.0)(lambda sr, rng, i=_i: sfx.key_impact(sr, rng, i))
    sound(f"page_tear_{_i + 1}", -2.0)(lambda sr, rng, i=_i: sfx.page_tear(sr, rng, i))
    sound(f"write_{_i + 1}", -3.0)(lambda sr, rng, i=_i: sfx.write(sr, rng, i))
    sound(f"hurt_{_i + 1}", -2.0)(lambda sr, rng, i=_i: sfx.hurt(sr, rng, i))
    sound(f"npc_hum_{_i + 1}", -3.0)(lambda sr, rng, i=_i: sfx.hum(sr, rng, i))
sound("bell", -2.0)(sfx.margin_bell)
sound("backspace", -1.5)(sfx.backspace)
sound("rewrite", -1.5)(lambda sr, rng: sfx.rewrite(sr, rng, _keys(sr, 7)))
sound("laugh", -3.0)(sfx.laugh)
sound("reveal", -1.5)(sfx.reveal)
sound("crack", -1.0)(sfx.crack)
sound("erase", -2.0)(sfx.erase)
sound("echo", -1.5)(sfx.echo)
sound("approve", -2.0)(sfx.approve)
sound("ally_arrive", -1.5)(sfx.ally_arrive)
sound("pen_write", -3.0)(sfx.pen_write)
sound("music", -4.0, MUSIC_SR)(music.compose)


def build(name):
    fn, peak, sr = SOUNDS[name]
    rng = random.Random(zlib.crc32(name.encode()))
    x = fn(sr, rng)
    long_piece = name == "music"
    x = finish(x, sr, peak, fade_in=0.5 if long_piece else 0.0008, fade_out=4.0 if long_piece else 0.04,
               trim=not long_piece)
    os.makedirs(WAV, exist_ok=True)
    wav = os.path.join(WAV, name + ".wav")
    write_wav(wav, x, sr)
    ogg = os.path.join(OUT, name + ".ogg")
    used = oggenc.encode(wav, ogg, name, quality=0.3 if not long_piece else 0.25)
    dur, pk, rms, dc = stats(x, sr)
    return used, dur, pk, rms, dc, os.path.getsize(ogg)


def main():
    names = sys.argv[1:] or list(SOUNDS)
    print(f"encoder: {oggenc.encoder()} ({'mono' if oggenc.encoder() == 'gstreamer' else 'stereo'})")
    for n in names:
        t0 = time.time()
        used, dur, pk, rms, dc, size = build(n)
        print(f"  {n:14s} {dur:6.2f}s peak {pk:.3f} rms {rms:.3f} dc {dc:+.4f} {size / 1024:7.1f} KB  ({time.time() - t0:.1f}s)")
    print(f"wrote {len(names)} sounds")


if __name__ == "__main__":
    main()
