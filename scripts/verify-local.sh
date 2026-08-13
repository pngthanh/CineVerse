#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "[1/3] Backend: test + checkstyle"
(cd "$ROOT_DIR/backend" && mvn verify)

echo "[2/3] Frontend: lint + typecheck + build"
(cd "$ROOT_DIR/frontend" && npm install --no-audit --no-fund && npm run check)

echo "[3/3] Docker Compose config"
(cd "$ROOT_DIR" && docker compose config >/dev/null)

echo "CineVerse V1 local verification passed."
