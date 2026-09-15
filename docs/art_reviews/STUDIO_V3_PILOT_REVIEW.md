# Studio-v3 Pilot Review — Hero (33.7)

**Baseline:** `android/assets/generated` (premium-v2, 2×28/3×36, outline #142126, three-band ramp)
**Candidate:** `/tmp/hero-studio-pilot` (studio-v3, weighted 2.4/1.2 outline, LayerWeight 0.22/0.58 + Fresnel IOR 1.45 + Glossy 0.18/0.42, palette #1E8A4E/#8BF27A/#E8B84B/#F0C9A8)
**Sheets:** `pilot_characters.png` (191K) — Hero idle/attack/hit/death side-by-side against premium-v2 baseline, plus silhouette and grade row
**Validator:** `python3 tools/visual/validate_generated_assets.py /tmp/hero-studio-pilot` → `Validated 3 assets, 3 RGBA PNGs, 20185088 decoded bytes, max page 2048px / Edge safety ✓ Pivot stability ✓ Silhouette ✓ Grade alpha ✓`
**Contact-sheet audit:** Hero 6% larger head, defined brow/eye (0.038 vs 0.034), hair clump + strap end secondary details, rim-light additive above light band, highlight pop threshold 0.92 on metal/hair/eye only
**Decision:** **ACCEPTED** — line-weight ratio 2.0 (2.4/1.2) within 1.5–2.5, highlight coverage ~3% (narrow dot/streak) within bound, palette value steps 0.19→0.70 clearly separated, 3×36 top / 2×28 mid / 2×12 overlay firefly-free on contact sheet
**Provenance:** `visualQuality: studio-v3`, `engineVersion: 33.0-studio-v3-3x36-full`, `pipelineVersion: 3`, `renderSupersample` 2/3, `renderSamples` 28/36

Candidate approved to proceed to full re-render (33.8).
