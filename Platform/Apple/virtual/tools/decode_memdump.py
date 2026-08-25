#!/usr/bin/env python3
"""Decode a DEBUG_MEM / OutOfMem memory dump into named resources.

Reads a dump from a file or stdin (paste after `---` if stdin is a TTY).

Looks up:
  - block type nibble  → RES_TYPE_* from mem.i (or built-in table)
  - resource id (hex)  → name from gen_modules.plh and/or pack_report.txt

Example:
  python3 python/scripts/decode_memdump.py dump.txt
  pbpaste | python3 python/scripts/decode_memdump.py
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

# Flags printed by printMem in mem.s: locked (*), active (+), inactive (-)
FLAG_NAMES = {"*": "locked", "+": "active", "-": "inactive"}

# RES_TYPE_* from src/include/mem.i
TYPE_NAMES = {
    0x0: "free",
    0x1: "code",
    0x2: "2d_map",
    0x3: "3d_map",
    0x4: "tileset",
    0x5: "texture",
    0x6: "screen",
    0x7: "font",
    0x8: "module",
    0x9: "bytecode",
    0xA: "fixup",
    0xB: "portrait",
    0xC: "song",
    0xD: "story",
    0xE: "sm_tileset",
}

# pack_report.txt section header → resource type number(s)
SECTION_TYPES: dict[str, tuple[int, ...]] = {
    "2D map": (0x2,),
    "3D map": (0x3,),
    "Tile set": (0x4,),
    "Texture image": (0x5,),
    "Full screen image": (0x6,),
    "Font": (0x7,),
    # CODE / MODULE / BYTECODE / FIXUP are collapsed in the report
    "Code": (0x1, 0x8, 0x9, 0xA),
    "Script": (0x8, 0x9, 0xA),
    "Portrait image": (0xB,),
    "Song": (0xC,),
    "Story": (0xD,),
    "Small tile set": (0xE,),
}

_SEG_RE = re.compile(
    r"\$([0-9A-Fa-f]{4}),L([0-9A-Fa-f]{4})([*+-])([0-9A-Fa-f])"
    r"(?::([0-9A-Fa-f]{2}))?"
)
_BANK_RE = re.compile(r"^(MainMem|AuxMem)\s*:", re.IGNORECASE)
_RES_LINE_RE = re.compile(
    r"^\s*(.+?)\s*:\s+[0-9.]+K memory,\s+[0-9.]+K disk\s+"
    r"\(ids?\s+(\d+)(?:-(\d+))?;\s*[^)]*\)\s*$",
    re.IGNORECASE,
)
_CONST_RE = re.compile(
    r"^\s*const\s+(CODE|MOD|GS)_([A-Z0-9_]+)\s*=\s*(\d+)\s*$"
)
_TYPE_CONST_RE = re.compile(
    r"^\s*RES_TYPE_([A-Z0-9_]+)\s*=\s*\$([0-9A-Fa-f]+)\s*$"
)


@dataclass(frozen=True)
class Segment:
    bank: str
    addr: int
    length: int
    flag: str
    type_num: int
    res_id: Optional[int]  # None when type is 0 / free


@dataclass(frozen=True)
class PackEntry:
    name: str
    section: str
    id_lo: int
    id_hi: int
    types: tuple[int, ...]


def _repo_root() -> Path:
    # .../virtual/tools/this.py → .../virtual
    return Path(__file__).resolve().parents[1]


def _const_to_name(kind: str, symbol: str) -> str:
    """CODE_RESOURCE_INDEX → resourceIndex; GS_NEW_GAME → gs_newGame."""
    parts = symbol.lower().split("_")
    if kind == "GS":
        return "gs_" + parts[0] + "".join(p.capitalize() for p in parts[1:])
    if not parts:
        return symbol.lower()
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def load_type_names(mem_i: Optional[Path]) -> dict[int, str]:
    names = dict(TYPE_NAMES)
    if mem_i is None or not mem_i.is_file():
        return names
    for line in mem_i.read_text(encoding="utf-8", errors="replace").splitlines():
        m = _TYPE_CONST_RE.match(line)
        if not m:
            continue
        num = int(m.group(2), 16)
        names[num] = m.group(1).lower()
    names.setdefault(0, "free")
    return names


def load_module_ids(path: Path) -> tuple[dict[int, str], dict[int, str]]:
    """Return (code_id→name, module_id→name) from gen_modules.plh."""
    code: dict[int, str] = {}
    module: dict[int, str] = {}
    if not path.is_file():
        return code, module
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        m = _CONST_RE.match(line)
        if not m:
            continue
        kind, symbol, num_s = m.group(1), m.group(2), m.group(3)
        num = int(num_s)
        name = _const_to_name(kind, symbol)
        if kind == "CODE":
            code[num] = name
        else:
            module[num] = name
    return code, module


def load_pack_entries(path: Path) -> list[PackEntry]:
    if not path.is_file():
        return []
    entries: list[PackEntry] = []
    section: Optional[str] = None
    section_types: tuple[int, ...] = ()
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        stripped = raw.strip()
        if stripped.endswith("resources:"):
            title = stripped[: -len("resources:")].strip()
            if title in SECTION_TYPES:
                section = title
                section_types = SECTION_TYPES[title]
            else:
                section = None
                section_types = ()
            continue
        if section is None:
            continue
        if stripped.startswith("Subtotal") or stripped.startswith("GRAND"):
            section = None
            continue
        m = _RES_LINE_RE.match(raw)
        if not m:
            continue
        lo = int(m.group(2))
        hi = int(m.group(3)) if m.group(3) else lo
        entries.append(
            PackEntry(
                name=m.group(1).strip(),
                section=section,
                id_lo=lo,
                id_hi=hi,
                types=section_types,
            )
        )
    return entries


def parse_dump(text: str) -> list[Segment]:
    segs: list[Segment] = []
    bank = "?"
    for raw in text.splitlines():
        line = raw.strip()
        bm = _BANK_RE.match(line)
        if bm:
            bank = "Main" if bm.group(1).lower().startswith("main") else "Aux"
            # Same line may also hold the first segment(s).
            line = line[bm.end() :]
        for m in _SEG_RE.finditer(line):
            typ = int(m.group(4), 16)
            rid = int(m.group(5), 16) if m.group(5) is not None else None
            if typ == 0:
                rid = None
            segs.append(
                Segment(
                    bank=bank,
                    addr=int(m.group(1), 16),
                    length=int(m.group(2), 16),
                    flag=m.group(3),
                    type_num=typ,
                    res_id=rid,
                )
            )
    return segs


class Resolver:
    def __init__(
        self,
        type_names: dict[int, str],
        code_ids: dict[int, str],
        module_ids: dict[int, str],
        pack: list[PackEntry],
    ) -> None:
        self.type_names = type_names
        self.code_ids = code_ids
        self.module_ids = module_ids
        self.pack = pack

    def type_label(self, typ: int) -> str:
        return self.type_names.get(typ, f"type_{typ:X}")

    def _pack_names(self, typ: int, rid: int) -> list[str]:
        hits = [
            e.name
            for e in self.pack
            if typ in e.types and e.id_lo <= rid <= e.id_hi
        ]
        # Prefer exact single-id matches over ranges when both exist.
        exact = [
            e.name
            for e in self.pack
            if typ in e.types and e.id_lo == rid and e.id_hi == rid
        ]
        return exact or hits

    def resolve_name(self, typ: int, rid: int) -> Optional[str]:
        if typ == 0x1:
            if rid in self.code_ids:
                return self.code_ids[rid]
            hits = self._pack_names(typ, rid)
            return hits[0] if hits else None

        if typ in (0x8, 0x9, 0xA):
            if rid in self.module_ids:
                return self.module_ids[rid]
            hits = self._pack_names(typ, rid)
            return hits[0] if hits else None

        hits = self._pack_names(typ, rid)
        if not hits:
            return None
        if len(hits) == 1:
            return hits[0]
        return " / ".join(hits)


def format_segment(seg: Segment, resolver: Resolver) -> str:
    flag = FLAG_NAMES.get(seg.flag, seg.flag)
    typ = resolver.type_label(seg.type_num)
    size = f"{seg.length} bytes"
    if seg.length >= 1024:
        size = f"{seg.length / 1024:.1f}K ({seg.length} bytes)"

    if seg.type_num == 0 or seg.res_id is None:
        what = "free/untyped" if seg.type_num == 0 else typ
        return (
            f"{seg.bank:4} ${seg.addr:04X}  L${seg.length:04X}  "
            f"{seg.flag} {what:12}  {flag:8}  {size}"
        )

    name = resolver.resolve_name(seg.type_num, seg.res_id)
    id_part = f"#{seg.res_id} (0x{seg.res_id:02X})"
    if name:
        what = f"{typ} {id_part}  {name}"
    else:
        what = f"{typ} {id_part}  ?"
    return (
        f"{seg.bank:4} ${seg.addr:04X}  L${seg.length:04X}  "
        f"{seg.flag} {what}  [{flag}]  {size}"
    )


def read_input(path: Optional[Path]) -> str:
    if path is not None:
        return path.read_text(encoding="utf-8", errors="replace")
    if sys.stdin.isatty():
        print(
            "Paste memory dump, then a line with only --- and Enter:",
            file=sys.stderr,
        )
        lines: list[str] = []
        for line in sys.stdin:
            if line.strip() == "---":
                break
            lines.append(line)
        return "".join(lines)
    return sys.stdin.read()


def main(argv: list[str] | None = None) -> int:
    root = _repo_root()
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument(
        "dump",
        nargs="?",
        type=Path,
        help="Memory dump file (default: stdin)",
    )
    ap.add_argument(
        "--pack-report",
        type=Path,
        default=root / "pack_report.txt",
        help="Path to pack_report.txt",
    )
    ap.add_argument(
        "--gen-modules",
        type=Path,
        default=root / "build/src/plasma/gen_modules.plh",
        help="Path to gen_modules.plh (CODE_/MOD_/GS_ ids)",
    )
    ap.add_argument(
        "--mem-i",
        type=Path,
        default=root.parent / "src" / "include" / "mem.i",
        help="Path to mem.i (optional; built-in types used if missing)",
    )
    ap.add_argument(
        "--used-only",
        action="store_true",
        help="Omit free/untyped (type 0) blocks",
    )
    args = ap.parse_args(argv)

    text = read_input(args.dump)
    segs = parse_dump(text)
    if not segs:
        print("No memory segments found in input.", file=sys.stderr)
        return 1

    code_ids, module_ids = load_module_ids(args.gen_modules)
    pack = load_pack_entries(args.pack_report)
    resolver = Resolver(
        load_type_names(args.mem_i), code_ids, module_ids, pack
    )

    missing = []
    if not args.pack_report.is_file():
        missing.append(f"pack_report: {args.pack_report}")
    if not args.gen_modules.is_file():
        missing.append(f"gen_modules: {args.gen_modules}")
    if missing:
        print(
            "Warning: lookup files missing:\n  " + "\n  ".join(missing),
            file=sys.stderr,
        )

    for seg in segs:
        if args.used_only and seg.type_num == 0:
            continue
        print(format_segment(seg, resolver))

    used = [s for s in segs if s.type_num != 0]
    free = [s for s in segs if s.type_num == 0]
    print()
    print(
        f"{len(segs)} blocks  |  "
        f"{sum(s.length for s in used)} bytes used in {len(used)}  |  "
        f"{sum(s.length for s in free)} bytes free/untyped in {len(free)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
