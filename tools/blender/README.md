# Temporary Blender Tooling

Blender is a generation-time dependency, never a game dependency and never a tracked repository artifact.

Run:

```sh
./scripts/install-blender-temp.sh
```

The pinned, checksum-verified Blender 4.2 LTS binary is extracted below `${TMPDIR:-/tmp}/hero-defense-tools`. On a size-constrained tmpfs, set `HERO_TOOLS_ROOT` to another disposable, non-repository cache (for example `$HOME/.cache/hero-defense-tools`). Only the small `bpy` source scripts and reviewed rendered output are committed. CI follows the same policy on an ephemeral runner.

Render the required low-poly UI icon batch with:

```sh
blender --background --factory-startup --python tools/blender/generate_assets.py -- \
  --batch ui --output android/assets/generated --isolate-frames
```

Every animated batch is packed by the deterministic premium-v2 planner. Runtime pages are capped at 2048×2048; oversized or 2× working batches spill into additional libGDX atlas pages without changing clip keys, frame order, dimensions, or pivots.

Validate any generated output before review:

```sh
python3 -m unittest discover -s tools/blender/tests -v
python3 tools/visual/validate_generated_assets.py android/assets/generated
```

The validator has no third-party dependency. `tools/visual/repack_committed_assets.py` is only the reviewable Pillow-based migration utility used to rearrange already-rendered legacy sheets; normal asset generation remains Blender/bpy-only.
