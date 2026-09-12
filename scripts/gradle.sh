#!/usr/bin/env sh
set -eu

REPO_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CACHE_KEY=${USER:-ci}
export GRADLE_USER_HOME=${GRADLE_USER_HOME:-${TMPDIR:-/tmp}/herodefense-gradle-${CACHE_KEY}}
PROJECT_CACHE_DIR=${HERO_PROJECT_CACHE_DIR:-${TMPDIR:-/tmp}/herodefense-project-cache-${CACHE_KEY}}
mkdir -p "$GRADLE_USER_HOME" "$PROJECT_CACHE_DIR"

exec "$REPO_ROOT/gradlew" --project-cache-dir "$PROJECT_CACHE_DIR" "$@"
