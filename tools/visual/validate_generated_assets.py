#!/usr/bin/env python3
"""Dependency-free manifest/page validator for Blender render artifacts."""
from __future__ import annotations

import argparse
import json
import struct
from pathlib import Path

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    manifest = json.loads((root / "asset_manifest.json").read_text(encoding="utf-8"))
    page_limit = manifest["maxAtlasPageSize"]
    if manifest.get("pipelineVersion", 0) < 3:
        raise ValueError("premium-v2 output requires pipeline version 3 or newer")
    if manifest.get("renderSupersample") != 2:
        raise ValueError("premium-v2 output must use 2x working renders")
    if manifest.get("opaqueRenderSamples", 0) < 16:
        raise ValueError("premium-v2 opaque renders require at least 16 samples")
    if manifest.get("overlayRenderSamples", 0) < 8:
        raise ValueError("premium-v2 overlays require at least 8 samples")
    decoded_total = 0
    referenced: set[Path] = set()

    for asset in manifest["assets"]:
        key = asset["key"]
        pivot = asset["pivot"]
        if pivot["units"] != "normalized-bottom-left":
            raise ValueError(f"{key}: unsupported pivot units")
        if not (0.0 <= pivot["x"] <= 1.0 and 0.0 <= pivot["y"] <= 1.0):
            raise ValueError(f"{key}: pivot outside normalized frame")
        if asset["alphaMode"] != "STRAIGHT_RGBA":
            raise ValueError(f"{key}: invalid alpha contract")

        pages = []
        for sheet in asset["sheets"]:
            path = _inside(root, sheet["file"])
            width, height, bit_depth, color_type = _png_header(path)
            if (width, height) != (sheet["width"], sheet["height"]):
                raise ValueError(f"{key}: sheet metadata does not match {path.name}")
            if width > page_limit or height > page_limit:
                raise ValueError(f"{key}: {path.name} exceeds {page_limit}px")
            if bit_depth != 8 or color_type != 6:
                raise ValueError(f"{key}: {path.name} must be 8-bit RGBA PNG")
            expected_bytes = width * height * 4
            if sheet["decodedBytes"] != expected_bytes:
                raise ValueError(f"{key}: invalid decoded byte count")
            decoded_total += expected_bytes
            referenced.add(path)
            pages.append((width, height))

        frame_size = asset["frameSize"]
        for clip, frames in asset["clips"].items():
            for expected_index, frame in enumerate(frames):
                if frame["index"] != expected_index:
                    raise ValueError(f"{key}/{clip}: unstable frame order")
                if frame["width"] != frame_size or frame["height"] != frame_size:
                    raise ValueError(f"{key}/{clip}: changed frame contract")
                page = frame["page"]
                if page < 0 or page >= len(pages):
                    raise ValueError(f"{key}/{clip}: invalid page index")
                width, height = pages[page]
                if (frame["x"] < 0 or frame["y"] < 0
                    or frame["x"] + frame_size > width
                    or frame["y"] + frame_size > height):
                    raise ValueError(f"{key}/{clip}: frame outside page")

        if "icon" in asset:
            icon = _inside(root, asset["icon"])
            width, height, bit_depth, color_type = _png_header(icon)
            if (width, height, bit_depth, color_type) != (96, 96, 8, 6):
                raise ValueError(f"{key}: invalid equipment icon PNG")
            if icon not in referenced:
                decoded_total += width * height * 4
                referenced.add(icon)

    committed = {path.resolve() for path in root.rglob("*.png")}
    if committed != referenced:
        missing = sorted(str(path) for path in committed ^ referenced)
        raise ValueError(f"undeclared or missing PNG files: {missing}")
    budget = manifest["decodedCatalogBudgetBytes"]
    if decoded_total > budget:
        raise ValueError(f"decoded catalog {decoded_total} exceeds {budget}")
    print(
        f"Validated {len(manifest['assets'])} assets, {len(referenced)} RGBA PNGs, "
        f"{decoded_total} decoded bytes, max page {page_limit}px"
    )


def _inside(root: Path, relative: str) -> Path:
    path = (root / relative).resolve()
    if root not in path.parents or not path.is_file():
        raise ValueError(f"missing or unsafe generated path: {relative}")
    return path


def _png_header(path: Path) -> tuple[int, int, int, int]:
    with path.open("rb") as stream:
        if stream.read(8) != PNG_SIGNATURE:
            raise ValueError(f"not a PNG: {path}")
        length = struct.unpack(">I", stream.read(4))[0]
        if stream.read(4) != b"IHDR" or length != 13:
            raise ValueError(f"invalid PNG IHDR: {path}")
        width, height, bit_depth, color_type, compression, filtering, interlace = struct.unpack(
            ">IIBBBBB", stream.read(13)
        )
        if compression != 0 or filtering != 0 or interlace not in (0, 1):
            raise ValueError(f"unsupported PNG encoding: {path}")
        return width, height, bit_depth, color_type


if __name__ == "__main__":
    main()
