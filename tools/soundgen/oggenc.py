"""WAV -> OGG Vorbis, deterministically, on macOS or Windows.

Minecraft only positions MONO sounds. The encoders, in order of preference:
  1. GStreamer's libvorbis (`vorbisenc`): true mono;
  2. ffmpeg built with libvorbis (`ffmpeg -encoders` lists it): mono too (-ac 1 -c:a libvorbis);
  3. ffmpeg's built-in `vorbis` encoder: it refuses mono ("only supports 2 channels"), so stereo (the sound then plays
     unpositioned, fine for music and narration).
If none is found, encoder() returns None and encode() raises EncoderMissing: generate.py still writes and measures the WAVs.

The binaries are looked up on PATH first, then in the usual install folders (Homebrew and the GStreamer framework on macOS;
C:\\ffmpeg, Program Files, Chocolatey, WinGet and Scoop, and the GStreamer MSVC installs on Windows). SN_FFMPEG / SN_GST
override the search.

oggmux picks a random bitstream serial number, so every page's serial is rewritten to a value derived from the file
name and each page CRC recomputed: the same WAV always yields a byte-identical OGG.
"""

import glob
import os
import shutil
import struct
import subprocess
import zlib


class EncoderMissing(RuntimeError):
    pass


def _home(*parts):
    return os.path.join(os.path.expanduser("~"), *parts)


def _local_appdata(*parts):
    base = os.environ.get("LOCALAPPDATA") or _home("AppData", "Local")
    return os.path.join(base, *parts)


def _gst_candidates():
    pf = os.environ.get("ProgramFiles", r"C:\Program Files")
    return [
        r"C:\gstreamer\1.0\msvc_x86_64\bin\gst-launch-1.0.exe",
        os.path.join(pf, "gstreamer", "1.0", "msvc_x86_64", "bin", "gst-launch-1.0.exe"),
        r"C:\Program Files\gstreamer\1.0\msvc_x86_64\bin\gst-launch-1.0.exe",
        r"C:\gstreamer\1.0\mingw_x86_64\bin\gst-launch-1.0.exe",
        "/Library/Frameworks/GStreamer.framework/Versions/1.0/bin/gst-launch-1.0",
        "/opt/homebrew/bin/gst-launch-1.0",
        "/usr/local/bin/gst-launch-1.0",
    ]


def _ffmpeg_candidates():
    pf = os.environ.get("ProgramFiles", r"C:\Program Files")
    out = [
        r"C:\ffmpeg\bin\ffmpeg.exe",
        os.path.join(pf, "ffmpeg", "bin", "ffmpeg.exe"),
        r"C:\Program Files\ffmpeg\bin\ffmpeg.exe",
        r"C:\ProgramData\chocolatey\bin\ffmpeg.exe",
        _local_appdata("Microsoft", "WinGet", "Links", "ffmpeg.exe"),
    ]
    out += sorted(glob.glob(_local_appdata("Microsoft", "WinGet", "Packages", "Gyan.FFmpeg*", "ffmpeg-*", "bin", "ffmpeg.exe")),
                  reverse=True)
    out += [_home("scoop", "shims", "ffmpeg.exe"), "/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg", "/usr/bin/ffmpeg"]
    return out


def _find(env, names, candidates):
    forced = os.environ.get(env)
    if forced and os.path.isfile(forced):
        return forced
    for n in names:
        p = shutil.which(n)
        if p:
            return p
    for c in candidates:
        if c and os.path.isfile(c):
            return c
    return None


_CACHE = {}


def _probe():
    if "kind" in _CACHE:
        return _CACHE["kind"], _CACHE["path"]
    kind, path = None, None
    gst = _find("SN_GST", ("gst-launch-1.0",), _gst_candidates())
    if gst and _gst_has_vorbis(gst):
        kind, path = "gstreamer", gst
    else:
        ff = _find("SN_FFMPEG", ("ffmpeg",), _ffmpeg_candidates())
        if ff:
            kind, path = ("ffmpeg-libvorbis" if _ffmpeg_has_libvorbis(ff) else "ffmpeg"), ff
    _CACHE["kind"], _CACHE["path"] = kind, path
    return kind, path


def _gst_has_vorbis(gst):
    inspect = os.path.join(os.path.dirname(gst), "gst-inspect-1.0" + (".exe" if gst.lower().endswith(".exe") else ""))
    if not os.path.isfile(inspect):
        return True     # cannot check: trust the install (the framework build always ships vorbisenc)
    try:
        r = subprocess.run([inspect, "vorbisenc"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30)
        return r.returncode == 0
    except (OSError, subprocess.SubprocessError):
        return False


def _ffmpeg_has_libvorbis(ff):
    try:
        r = subprocess.run([ff, "-hide_banner", "-encoders"], capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.SubprocessError):
        return False
    return "libvorbis" in (r.stdout or "")


def encoder():
    """'gstreamer' (mono), 'ffmpeg-libvorbis' (mono), 'ffmpeg' (stereo) or None (nothing installed)."""
    return _probe()[0]


def is_mono():
    return encoder() in ("gstreamer", "ffmpeg-libvorbis")


def describe():
    kind, path = _probe()
    if kind is None:
        return "none found (install ffmpeg or GStreamer; WAVs are still written)"
    return f"{kind} ({'mono' if is_mono() else 'stereo'}) at {path}"


def _crc_table():
    table = []
    for i in range(256):
        r = i << 24
        for _ in range(8):
            r = ((r << 1) ^ 0x04C11DB7) if r & 0x80000000 else (r << 1)
        table.append(r & 0xFFFFFFFF)
    return table


_TABLE = _crc_table()


def ogg_crc(data):
    """Ogg's CRC-32: polynomial 0x04C11DB7, init 0, unreflected, no final xor."""
    crc = 0
    t = _TABLE
    for b in data:
        crc = ((crc << 8) & 0xFFFFFFFF) ^ t[((crc >> 24) ^ b) & 0xFF]
    return crc


def restamp(path, serial):
    """Rewrites the stream serial of every page and fixes each page's CRC."""
    with open(path, "rb") as f:
        data = bytearray(f.read())
    pos = 0
    pages = 0
    while pos < len(data):
        if data[pos:pos + 4] != b"OggS":
            raise ValueError(f"{path}: lost page sync at {pos}")
        nseg = data[pos + 26]
        body = sum(data[pos + 27:pos + 27 + nseg])
        size = 27 + nseg + body
        struct.pack_into("<I", data, pos + 14, serial)
        struct.pack_into("<I", data, pos + 22, 0)
        struct.pack_into("<I", data, pos + 22, ogg_crc(data[pos:pos + size]))
        pos += size
        pages += 1
    with open(path, "wb") as f:
        f.write(data)
    return pages


def encode(wav, ogg, name, quality=0.3):
    """Encodes and restamps; returns the encoder used. Raises EncoderMissing when nothing is installed."""
    kind, path = _probe()
    if kind is None:
        raise EncoderMissing("no OGG Vorbis encoder (ffmpeg or GStreamer) found")
    os.makedirs(os.path.dirname(ogg), exist_ok=True)
    if kind == "gstreamer":
        cmd = [path, "-q", "filesrc", "location=" + wav, "!", "wavparse", "!", "audioconvert", "!",
               "vorbisenc", f"quality={quality}", "!", "oggmux", "!", "filesink", "location=" + ogg]
    elif kind == "ffmpeg-libvorbis":
        # -map_metadata -1 and -fflags +bitexact keep ffmpeg's version string out of the file (deterministic output).
        cmd = [path, "-y", "-loglevel", "error", "-i", wav, "-map_metadata", "-1", "-fflags", "+bitexact",
               "-flags:a", "+bitexact", "-ac", "1", "-c:a", "libvorbis", "-q:a", str(round(quality * 10)), ogg]
    else:
        cmd = [path, "-y", "-loglevel", "error", "-i", wav, "-map_metadata", "-1", "-fflags", "+bitexact",
               "-flags:a", "+bitexact", "-c:a", "vorbis", "-strict", "-2", "-ac", "2", "-q:a", str(round(quality * 10)), ogg]
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    restamp(ogg, zlib.crc32(name.encode()) & 0x7FFFFFFF)
    return kind
