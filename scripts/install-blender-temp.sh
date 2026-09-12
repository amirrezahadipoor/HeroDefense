#!/usr/bin/env sh
set -eu

BLENDER_VERSION=4.2.23
BLENDER_ARCHIVE="blender-${BLENDER_VERSION}-linux-x64.tar.xz"
BLENDER_URL="https://download.blender.org/release/Blender4.2/${BLENDER_ARCHIVE}"
BLENDER_SHA256="bea0eb3146be13eae6225409a117b215184f41b7f79e799f97cb3abb8f6dc404"
TOOLS_ROOT="${HERO_TOOLS_ROOT:-${TMPDIR:-/tmp}/hero-defense-tools}"
ARCHIVE_PATH="${TOOLS_ROOT}/${BLENDER_ARCHIVE}"
INSTALL_PATH="${TOOLS_ROOT}/blender-${BLENDER_VERSION}-linux-x64"

mkdir -p "$TOOLS_ROOT"
if [ ! -x "$INSTALL_PATH/blender" ]; then
    curl --fail --location --retry 3 "$BLENDER_URL" --output "$ARCHIVE_PATH"
    printf '%s  %s\n' "$BLENDER_SHA256" "$ARCHIVE_PATH" | sha256sum --check --status
    rm -rf "$INSTALL_PATH"
    tar -xJf "$ARCHIVE_PATH" -C "$TOOLS_ROOT"
    rm -f "$ARCHIVE_PATH"
fi

BLENDER_VERSION_OUTPUT=$("$INSTALL_PATH/blender" --version)
printf '%s\n' "$BLENDER_VERSION_OUTPUT" | sed -n '1p'
printf '%s\n' "$INSTALL_PATH/blender"
