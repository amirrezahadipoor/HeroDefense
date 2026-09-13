# Committed Audio License Ledger

Every source and license below was checked on 2026-09-12 **before** the corresponding file was added to the repository. All files are unmodified Ogg Vorbis source bytes renamed for their game role. No full source archive is committed.

## OpenGameArt — Heavenly Loop

- Author: `isaiah658`
- Source page: https://opengameart.org/content/heavenly-loop
- Direct file: https://opengameart.org/sites/default/files/Heavenly%20Loop_0.ogg
- License displayed on source page: Creative Commons Zero 1.0 (`CC0-1.0`)
- License URL: https://creativecommons.org/publicdomain/zero/1.0/
- Source notes: advertised as a seamless ambient loop; attribution is appreciated but not required.
- Download SHA-256: `a842e9e054019132cacc8fd352e7b31c000ebb51e0b227a2511e1bccb4eb166e`
- Preserved provenance: `licenses/heavenly-loop-provenance.txt`

## Kenney — RPG Audio 1.0

- Creator/distributor: Kenney Vleugels (`Kenney.nl`)
- Source page: https://kenney.nl/assets/rpg-audio
- Download archive: https://kenney.nl/media/pages/assets/rpg-audio/8e99002d76-1677590336/kenney_rpg-audio.zip
- License: Creative Commons Zero 1.0 (`CC0-1.0`), including commercial use; attribution optional.
- Archive SHA-256 at review: `6dbeaf8544da958d8f2adcb4a4a4b76c1ade34a05f8ab9edccd327da7375f38b`
- Exact bundled license: `licenses/kenney-rpg-audio-license.txt`

## Kenney — Interface Sounds 1.0

- Creator/distributor: Kenney (`Kenney.nl`)
- Source page: https://kenney.nl/assets/interface-sounds
- Download archive: https://kenney.nl/media/pages/assets/interface-sounds/fa43c1dd4d-1677589452/kenney_interface-sounds.zip
- License: Creative Commons Zero 1.0 (`CC0-1.0`), including personal, educational, and commercial use; attribution optional.
- Archive SHA-256 at review: `f2193d072726d6758a5f7871b2dcc54dcce0d5c35c6f0a62f92549b327c81232`
- Exact bundled license: `licenses/kenney-interface-sounds-license.txt`

## Kenney — Impact Sounds 1.0 (added 2026-09-14, Phase 18)

- Creator/distributor: Kenney (`Kenney.nl`)
- Source page: https://kenney.nl/assets/impact-sounds
- Download archive: https://kenney.nl/media/pages/assets/impact-sounds/87b4ddecda-1677589768/kenney_impact-sounds.zip
- License: Creative Commons Zero 1.0 (`CC0-1.0`); bundled `License.txt` states "Creative Commons Zero, CC0" and permits commercial use without attribution.
- Archive SHA-256 at review: `029d734af1582474edf3a694d1b0cebc97c1c152f2f39fa34d4c2bafc5de77f8`
- Exact bundled license: `licenses/kenney-impact-sounds-license.txt`
- The RPG Audio and Interface Sounds archives were re-downloaded on 2026-09-14 and matched the SHA-256 values recorded above before the two new files were taken from them.

## Per-file ledger

| Committed file | Original file | Source | Use | SHA-256 |
|---|---|---|---|---|
| `audio/music/world_tree_vigil.ogg` | `Heavenly Loop.ogg` | OpenGameArt / isaiah658 | Seamless background loop | `a842e9e054019132cacc8fd352e7b31c000ebb51e0b227a2511e1bccb4eb166e` |
| `audio/sfx/hit.ogg` | `Audio/chop.ogg` | Kenney RPG Audio | Hit | `d00c2b3c9fff07e376145c8c8c45c90e5084ec192f6ce0387db233f7b86f1486` |
| `audio/sfx/death.ogg` | `Audio/dropLeather.ogg` | Kenney RPG Audio | Death | `097e1d3b74949b0145fda0519d40b7e0773ab82ec4858727f95be830927e1a45` |
| `audio/sfx/item_drop.ogg` | `Audio/drop_002.ogg` | Kenney Interface Sounds | Item drop/pickup | `4ac4d1cef7e936965cbf795852ca2020300b9e2ba7daa59f2bf4f1f7bf416218` |
| `audio/sfx/level_up.ogg` | `Audio/confirmation_004.ogg` | Kenney Interface Sounds | Level up | `568967a3d9f8a8f6af54ea01729c4882284308f2a27d78c07ffd7ee0d6951661` |
| `audio/sfx/boss_entrance.ogg` | `Audio/doorOpen_2.ogg` | Kenney RPG Audio | Boss entrance | `68962fb0458c9bac59dec3adefa9849703c7c89d34712de03dd0075d095e79e9` |
| `audio/sfx/critical.ogg` | `Audio/impactMetal_heavy_003.ogg` | Kenney Impact Sounds | Critical arrow | `b0f2ba4dabde9a87eb9c188a19d31e0c2300fd321adeba08d3b9b8aa011d7037` |
| `audio/sfx/kill.ogg` | `Audio/impactPunch_heavy_002.ogg` | Kenney Impact Sounds | Enemy kill thump (layered under Death) | `7993dd4c156b9979ad69f17be5ebe31850b16039a3857f8477141be54dfee1b3` |
| `audio/sfx/chain_lightning.ogg` | `Audio/impactGlass_light_002.ogg` | Kenney Impact Sounds | Chain-lightning arc | `710db7451bb7857b9140dfe6afe4621ea2188ad65eda3919c569d1bb285b933d` |
| `audio/sfx/stun.ogg` | `Audio/impactBell_heavy_004.ogg` | Kenney Impact Sounds | Stun | `49fe4fafa2001bd0d312976796824571ca8429851f285997e1327d17bb34fd00` |
| `audio/sfx/multi_shot.ogg` | `Audio/pluck_002.ogg` | Kenney Interface Sounds | Multi Shot volley bowstring | `c977fe249ff42d1c93a552b33abc13a8399df3879fa510475426e5c4bbac1da9` |
| `audio/sfx/purchase.ogg` | `Audio/handleCoins.ogg` | Kenney RPG Audio | Shop purchase | `8a91f969e932df709df80ee124d86a51389eed9b67f22e5e716bc2bbf60d8dab` |

## Technical validation

All files decoded successfully during import. The seven Phase 18 effects were decoded with `soundfile` on 2026-09-14: 0.16–0.85 s, peaks 0.88–0.95, non-silent; per-cue playback is rate-limited in `AudioThrottle` so volleys cannot stack identical samples. The music is stereo 44.1 kHz and 33.652 seconds long. Effects are mono/stereo 44.1–48 kHz and 0.191–1.413 seconds long. Every file has non-empty, non-silent decoded samples and is directly supported by libGDX's Android audio backend.
