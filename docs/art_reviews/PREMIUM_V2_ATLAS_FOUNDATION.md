# Premium-v2 Atlas Foundation Review

**Scope:** deterministic atlas packing and metadata migration only. No rendered pose, color, silhouette, outline, animation, or visible frame pixel was changed.

## Review result

**Accepted.** The four existing Boss sheets were the only pages above the 2048-pixel hard limit. They were rearranged from 2560×1024 to 2048×1024. Every source frame was compared byte-for-byte as straight RGBA after repacking.

| Asset | Old page | New page | Canonical ordered-frame SHA-256 | Pixel match |
|---|---:|---:|---|---|
| Ancient Golem | 2560×1024 | 2048×1024 | `a0dfa7d59ee3d5871303c1a02c56a9824403d26be62273d12f20f1bfd10d8aeb` | Exact |
| Ember Wyrm | 2560×1024 | 2048×1024 | `212af640904275e4fecf944783bfaa5b23e2e4260c25769013392735cbc9d107` | Exact |
| Thorn Matriarch | 2560×1024 | 2048×1024 | `2a00b3c3c58fd9606bbbf3d10c8cfa0134728fe77cbc77776dc79bd8f5568fba` | Exact |
| Void Knight | 2560×1024 | 2048×1024 | `965534d964b1a97d151b100f6b534b3a14e4b58bcd9dec46a553001ac484e967` | Exact |

The atlas planner retains the prior clip-row coordinates when a batch fits. Oversized batches switch to deterministic row-major packing and spill across as many bounded pages as required. Automated tests cover both current Boss dimensions and a 2× supersampled two-page case.

## Contract checks

- All 79 manifest assets declare a normalized, frame-class-stable pivot.
- All 119 generated PNGs are declared and are 8-bit straight RGBA.
- Every animation frame remains within its page, preserves its index and locked dimensions, contains visible pixels, and has a transparent outer edge.
- Every atlas page is now at most 2048×2048.
- Full decoded catalog estimate: **305,291,264 bytes (291.15 MiB)**, below the 320 MiB catalog gate.
- Conservative peak combat residency estimate: **79,585,280 bytes (75.90 MiB)**, below the 128 MiB gate. This includes the Hero, every regular enemy retained by the current lazy cache, both World Tree states, arena props, the largest Boss, six equipped overlays, and every item/UI icon.
- The repack removed unused transparent Boss columns and reduced the four Boss PNGs from 4,285,340 compressed bytes to 1,379,192 bytes without changing frame pixels.

## Automated coverage

- `tools/blender/tests/test_atlas_layout.py` validates stable legacy layout, bounded Boss packing, deterministic multi-page spill, and oversized-frame rejection.
- `tools/visual/validate_generated_assets.py` validates generated-batch page, RGBA, frame, pivot, declaration, and decoded-catalog metadata without third-party dependencies.
- `PremiumAssetContractTest` performs decoded PNG alpha-edge, visible-frame, page-bound, pivot, frame-count, reference completeness, and GPU-memory-budget checks on every committed asset.
