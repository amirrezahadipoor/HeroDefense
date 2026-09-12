# Temporary Blender Tooling

Blender is a generation-time dependency, never a game dependency and never a tracked repository artifact.

Run:

```sh
./scripts/install-blender-temp.sh
```

The pinned, checksum-verified Blender 4.2 LTS binary is extracted below `${TMPDIR:-/tmp}/hero-defense-tools`. Keep Blender, display helpers, render intermediates, and dependency caches in `/tmp` or an ephemeral CI runner; never extract them into the repository. On a constrained runner, clear stale `/tmp` data and render category batches sequentially rather than retaining a second tool installation.

Render the required low-poly UI icon batch with:

```sh
blender --background --factory-startup --python tools/blender/generate_assets.py -- \
  --batch ui --output android/assets/generated --isolate-frames
```

## Premium-v2 pilot

Render the guarded pilot into a disposable candidate directory, not directly over reviewed runtime assets:

```sh
blender --background --factory-startup --python tools/blender/generate_assets.py -- \
  --batch premium-pilot --output /tmp/hero-defense-premium-pilot --isolate-frames
python3 tools/visual/validate_generated_assets.py /tmp/hero-defense-premium-pilot
python3 tools/visual/create_premium_pilot_review.py \
  /tmp/hero-defense-premium-baseline \
  /tmp/hero-defense-premium-pilot \
  /tmp/hero-defense-premium-review
```

Only after opening and accepting all three contact sheets should the exact-key guarded promotion run:

```sh
python3 tools/visual/promote_premium_pilot.py \
  /tmp/hero-defense-premium-pilot android/assets/generated
```

For a complete character-category review, generate the reusable native-size motion and readability sheets, inspect both, write the review document, and only then record acceptance in the catalog:

```sh
python3 tools/visual/create_character_animation_review.py \
  /tmp/hero-defense-baseline android/assets/generated \
  hero "Elf Hero" /tmp/hero-defense-hero-review
python3 tools/visual/record_category_review.py \
  android/assets/generated docs/art_reviews/HERO_PREMIUM_V2_REVIEW.md hero hero
```

For the complete equipment category, render all 40 entries into a disposable candidate,
audit every overlay against all 28 finalized Hero frames, and inspect the four icon plus
five equipped-motion sheets. The promotion command refuses any unreviewed, clipped,
detached, extra, missing, or hash-mismatched payload:

```sh
python3 tools/visual/create_equipment_batch_review.py \
  /tmp/hero-defense-equipment-baseline \
  /tmp/hero-defense-equipment-candidate \
  android/assets/generated \
  tools/blender/equipment_visuals.json \
  docs/art_reviews/equipment_premium_v2
python3 tools/visual/promote_equipment_batch.py \
  /tmp/hero-defense-equipment-candidate android/assets/generated
```

Premium-v2 renders at 2× the unchanged runtime dimensions, uses 16 EEVEE samples for opaque assets and 8 for transparent equipment overlays, downsamples in linear premultiplied-alpha space, and applies the deterministic outline afterward.

Every animated batch is packed by the deterministic premium-v2 planner. Runtime pages are capped at 2048×2048; oversized or 2× working batches spill into additional libGDX atlas pages without changing clip keys, frame order, dimensions, or pivots.

Validate any generated output before review:

```sh
python3 -m unittest discover -s tools/blender/tests -v
python3 tools/visual/validate_generated_assets.py android/assets/generated
```

The validator has no third-party dependency. The deterministic contact-sheet and legacy repack utilities use Pillow; normal asset generation remains Blender/bpy-only. `tools/visual/repack_committed_assets.py` exists only for the reviewed one-time migration of legacy pages and is not a substitute for rerendering premium-v2 assets.
