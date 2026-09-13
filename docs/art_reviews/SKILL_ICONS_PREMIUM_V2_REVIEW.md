# Skill Icons Premium-v2 Review (Phase 17)

**Decision:** ACCEPTED

- Batch: `skill-icons` rendered by the `Generate visual asset batch` workflow on branch `phase17-gameplay` (second render; the first bolt read as disconnected fragments and was rejected).
- Candidate manifest SHA-256: `10a0f4cda33377d184ba33657c447ffd60cfbbb2e1049f8a35c449ff0a81ae37`
- Evidence: [`skill_icons_premium_v2/skill_icons_contact.png`](skill_icons_premium_v2/skill_icons_contact.png) — native 96 px row and the 64 px size used in the Shop rows, on the shop panel colour.
- Audit record: [`skill_icons_premium_v2/skill_icons_audit.json`](skill_icons_premium_v2/skill_icons_audit.json) (written by `tools/visual/promote_skill_icons.py`).

## Checklist

| Icon | Glyph | Reads at 64 px | Family match |
| --- | --- | --- | --- |
| `ui_skill_chain_lightning` | connected cyan zig-zag bolt between two gold nodes | yes | heartwood medallion, guard ring, paired gold anchors |
| `ui_skill_multi_shot` | three fanned parchment arrows, gold tips, one wooden nock | yes | same |
| `ui_skill_stun_chance` | cyan burst ringed by four gold star points | yes | same |
| `ui_skill_critical_mastery` | blade crossing a crimson target ring with a gold spark | yes | same |
| `ui_skill_long_range` | drawn wooden bow beside a far-flying leaf arrow | yes | same |

All five keep transparent outer margins, 96×96 straight-alpha RGBA, 2× supersample, 16 samples, and the `ui-control-icon-premium-v2` model revision, so they sit in the Skills tab next to the Stats-tab medallions without a visible style seam.
