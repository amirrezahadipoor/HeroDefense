"""Pure deterministic atlas-page planning shared by Blender and unit tests."""
from __future__ import annotations

from collections.abc import Mapping
from math import ceil

MAX_ATLAS_SIZE = 4096  # Phase 54: 2048->4096 for 384px frames, ASTC compression keeps APK <100MB


def plan_grid(
    frame_counts: Mapping[str, int],
    frame_size: int,
    max_page_size: int = MAX_ATLAS_SIZE,
) -> tuple[list[dict[str, int]], dict[str, list[dict[str, int]]]]:
    """Return bounded page dimensions and top-left frame regions.

    The legacy clip-row layout is retained whenever it fits. Oversized batches use a
    deterministic row-major layout and spill to additional pages as needed.
    """
    if frame_size <= 0 or frame_size > max_page_size:
        raise ValueError("frame size must fit inside the atlas-page limit")
    if not frame_counts or any(count <= 0 for count in frame_counts.values()):
        raise ValueError("every atlas clip must contain at least one frame")

    clips = list(frame_counts)
    legacy_columns = max(frame_counts.values())
    legacy_width = legacy_columns * frame_size
    legacy_height = len(clips) * frame_size
    regions: dict[str, list[dict[str, int]]] = {clip: [] for clip in clips}

    if legacy_width <= max_page_size and legacy_height <= max_page_size:
        for row, clip in enumerate(clips):
            for index in range(frame_counts[clip]):
                regions[clip].append(_region(0, index * frame_size, row * frame_size, frame_size, index))
        return [{"index": 0, "width": legacy_width, "height": legacy_height}], regions

    columns = max_page_size // frame_size
    rows = max_page_size // frame_size
    capacity = columns * rows
    flattened = [(clip, index) for clip in clips for index in range(frame_counts[clip])]
    pages: list[dict[str, int]] = []

    for page_index, start in enumerate(range(0, len(flattened), capacity)):
        page_frames = flattened[start:start + capacity]
        page_columns = min(columns, len(page_frames))
        page_rows = ceil(len(page_frames) / page_columns)
        pages.append({
            "index": page_index,
            "width": page_columns * frame_size,
            "height": page_rows * frame_size,
        })
        for local_index, (clip, frame_index) in enumerate(page_frames):
            regions[clip].append(_region(
                page_index,
                (local_index % page_columns) * frame_size,
                (local_index // page_columns) * frame_size,
                frame_size,
                frame_index,
            ))

    return pages, regions


def _region(page: int, x: int, y: int, size: int, index: int) -> dict[str, int]:
    return {
        "page": page,
        "x": x,
        "y": y,
        "width": size,
        "height": size,
        "index": index,
    }
