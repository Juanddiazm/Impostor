#!/usr/bin/env python3
"""Une el APK generado por aapt2 con classes.dex y deja las entradas sin comprimir
alineadas a 4 bytes (equivalente a zipalign). Uso: package.py base.apk classes.dex out.apk"""
import struct
import sys
import zipfile

base, dex, out = sys.argv[1:4]
STORED_SUFFIXES = (".arsc", ".png", ".dex", ".jpg", ".webp", ".ogg", ".mp3")


def add(zout, name, data):
    zi = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
    zi.external_attr = 0
    stored = name.lower().endswith(STORED_SUFFIXES)
    zi.compress_type = zipfile.ZIP_STORED if stored else zipfile.ZIP_DEFLATED
    if stored:
        offset = zout.fp.tell() + 30 + len(name.encode("utf-8"))
        pad = (4 - offset % 4) % 4
        if pad:
            if pad < 4:
                pad += 4
            zi.extra = struct.pack("<HH", 0xD935, pad - 4) + b"\0" * (pad - 4)
    zout.writestr(zi, data)


with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out, "w") as zout:
    names = zin.namelist()
    ordered = [n for n in names if n == "AndroidManifest.xml"] + \
              [n for n in names if n == "resources.arsc"] + \
              [n for n in names if n not in ("AndroidManifest.xml", "resources.arsc")]
    for name in ordered:
        add(zout, name, zin.read(name))
    with open(dex, "rb") as f:
        add(zout, "classes.dex", f.read())

# Verificación de alineación
with zipfile.ZipFile(out) as z, open(out, "rb") as f:
    for zi in z.infolist():
        if zi.compress_type == zipfile.ZIP_STORED:
            f.seek(zi.header_offset)
            h = f.read(30)
            n, e = struct.unpack("<HH", h[26:30])
            data_off = zi.header_offset + 30 + n + e
            assert data_off % 4 == 0, f"{zi.filename} no alineado ({data_off})"
print(f"APK empaquetado: {out}")
