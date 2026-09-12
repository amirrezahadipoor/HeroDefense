#!/usr/bin/env sh
set -eu

REPO_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

exec "$REPO_ROOT/scripts/gradle.sh" \
  :core:test \
  --tests 'com.amirrezahadipoor.herodefense.balance.BalanceSimulatorTest' \
  --rerun-tasks \
  "$@"
