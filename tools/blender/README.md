# Temporary Blender Tooling

Blender is a generation-time dependency, never a game dependency and never a tracked repository artifact.

Run:

```sh
./scripts/install-blender-temp.sh
```

The pinned, checksum-verified Blender 4.2 LTS binary is extracted below `${TMPDIR:-/tmp}/hero-defense-tools`. On a size-constrained tmpfs, set `HERO_TOOLS_ROOT` to another disposable, non-repository cache (for example `$HOME/.cache/hero-defense-tools`). Only the small `bpy` source scripts and reviewed rendered output are committed. CI follows the same policy on an ephemeral runner.
