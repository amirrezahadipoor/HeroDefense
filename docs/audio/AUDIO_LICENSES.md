# Committed Audio License Ledger

Every source and license below was checked on 2026-09-12 **before** the corresponding file was added to the repository. All six files are unmodified Ogg Vorbis source bytes renamed for their game role. No full source archive is committed.

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

## Per-file ledger

| Committed file | Original file | Source | Use | SHA-256 |
|---|---|---|---|---|
| `audio/music/world_tree_vigil.ogg` | `Heavenly Loop.ogg` | OpenGameArt / isaiah658 | Seamless background loop | `a842e9e054019132cacc8fd352e7b31c000ebb51e0b227a2511e1bccb4eb166e` |
| `audio/sfx/hit.ogg` | `Audio/chop.ogg` | Kenney RPG Audio | Hit | `d00c2b3c9fff07e376145c8c8c45c90e5084ec192f6ce0387db233f7b86f1486` |
| `audio/sfx/death.ogg` | `Audio/dropLeather.ogg` | Kenney RPG Audio | Death | `097e1d3b74949b0145fda0519d40b7e0773ab82ec4858727f95be830927e1a45` |
| `audio/sfx/item_drop.ogg` | `Audio/drop_002.ogg` | Kenney Interface Sounds | Item drop/pickup | `4ac4d1cef7e936965cbf795852ca2020300b9e2ba7daa59f2bf4f1f7bf416218` |
| `audio/sfx/level_up.ogg` | `Audio/confirmation_004.ogg` | Kenney Interface Sounds | Level up | `568967a3d9f8a8f6af54ea01729c4882284308f2a27d78c07ffd7ee0d6951661` |
| `audio/sfx/boss_entrance.ogg` | `Audio/doorOpen_2.ogg` | Kenney RPG Audio | Boss entrance | `68962fb0458c9bac59dec3adefa9849703c7c89d34712de03dd0075d095e79e9` |

## Technical validation

All files decoded successfully during import. The music is stereo 44.1 kHz and 33.652 seconds long. Effects are mono/stereo 44.1–48 kHz and 0.191–1.413 seconds long. Every file has non-empty, non-silent decoded samples and is directly supported by libGDX's Android audio backend.
