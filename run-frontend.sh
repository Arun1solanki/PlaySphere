#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$ROOT/frontend"
[ -d node_modules ] || npm install
exec npm run dev
