"""Read blocks out of Anvil region files (1.18+ section/block_states layout).

Independent implementation for captures-recreate2 (verification + evidence):
reads a region file, decodes every chunk's paletted block states back into
world coordinates, and can compare the result against a generator's Grid.
"""

from __future__ import annotations

import struct
from pathlib import Path

import nbt


def _unpack(longs, bits, count, span=True):
    out = []
    if bits == 0:
        return out
    mask = (1 << bits) - 1
    if span:
        per_long = 64 // bits
        for value in longs:
            value &= 0xFFFFFFFFFFFFFFFF
            for i in range(per_long):
                out.append((value >> (i * bits)) & mask)
                if len(out) == count:
                    return out
    else:
        per_long = 64 // bits
        pos = 0
        while len(out) < count:
            value = longs[pos] & 0xFFFFFFFFFFFFFFFF
            for i in range(per_long):
                out.append((value >> (i * bits)) & mask)
                if len(out) == count:
                    break
            pos += 1
        return out
    return out


def _palette_entries(container):
    """palette: list of compound Tags -> list of 'name[props]' strings."""
    item, items = container.value["palette"].value
    out = []
    for entry in items:
        plain = nbt.unwrap(entry)
        name = plain["Name"]
        props = plain.get("Properties")
        if props:
            ps = ",".join(f"{k}={v}" for k, v in sorted(props.items()))
            out.append(f"{name}[{ps}]")
        else:
            out.append(name)
    return out


def read_chunk(blob: bytes):
    root = nbt.unwrap(nbt.parse(blob))
    out = {}
    for sec in root.get("sections", []):
        y = sec["Y"]
        bs = sec.get("block_states")
        if not bs:
            continue
        palette = _palette_entries(nbt.Tag(nbt.TAG_COMPOUND, sec["block_states"])) if False else None
        # palette entries as plain strings
        ent = []
        for entry in bs["palette"]:
            name = entry["Name"]
            props = entry.get("Properties")
            ent.append(name + ("[" + ",".join(f"{k}={v}" for k, v in sorted(props.items())) + "]" if props else ""))
        data = bs.get("data")
        n = 4096
        if data is None or len(ent) == 1:
            idx = [0] * n
        else:
            bits = max(4, (len(ent) - 1).bit_length())
            idx = _unpack(data, bits, n)
        for i, pi in enumerate(idx):
            block = ent[pi]
            if block.endswith("minecraft:air]") or block == "minecraft:air" or block.startswith("minecraft:cave_air") or block.startswith("minecraft:void_air"):
                continue
            x = i & 15
            z = (i >> 4) & 15
            yy = (i >> 8) & 15
            out[(x, y * 16 + yy, z)] = block
    return out


def read_region(path: Path, cx=None, cz=None):
    """Return {(x,y,z): block} for the whole region (or one chunk)."""
    path = Path(path)
    _, rz = (int(v) for v in path.stem.split(".")[1:3])
    rx = int(path.stem.split(".")[1])
    data = path.read_bytes()
    locations = struct.unpack_from(">1024i", data, 0)
    out = {}
    for i, loc in enumerate(locations):
        if loc == 0:
            continue
        sector = loc >> 8
        if sector == 0:
            continue
        lx, lz = i % 32, i // 32
        if cx is not None and (rx * 32 + lx != cx or rz * 32 + lz != cz):
            continue
        off = sector * 4096
        length = struct.unpack_from(">i", data, off)[0]
        comp = data[off + 4]
        blob = data[off + 5:off + 4 + length]
        if comp == 1:
            import gzip
            blob = gzip.decompress(blob)
        elif comp == 2:
            import zlib
            blob = zlib.decompress(blob)
        elif comp == 3:
            blob = blob  # uncompressed
        else:
            raise ValueError(f"unknown compression {comp}")
        blocks = read_chunk(blob)
        bx, bz = (rx * 32 + lx) * 16, (rz * 32 + lz) * 16
        for (x, y, z), b in blocks.items():
            out[(bx + x, y, bz + z)] = b
    return out


def world_blocks(world: Path):
    """Union of every region file under <world>/region."""
    out = {}
    for path in sorted((Path(world) / "region").glob("*.mca")):
        out.update(read_region(path))
    return out


def compare(blocks: dict, grid_blocks: dict, ignore_air=True):
    """Diff a saved world against the generator's expectation."""
    missing = []
    wrong = []
    for pos, expected in grid_blocks.items():
        exp = expected if ":" in expected else "minecraft:" + expected
        got = blocks.get(pos)
        if got is None:
            if ignore_air and exp.endswith("minecraft:air"):
                continue
            missing.append((pos, exp, got))
        elif got != exp:
            wrong.append((pos, exp, got))
    extra = [p for p in blocks if p not in grid_blocks]
    return {"missing": missing[:40], "wrong": wrong[:40], "extra_count": len(extra),
            "missing_count": len(missing), "wrong_count": len(wrong)}
