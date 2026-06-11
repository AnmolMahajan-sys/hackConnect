#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
#  deploy.sh – zero-downtime redeploy for HackConnect
#  Usage:  ./scripts/deploy.sh [--scale N]
# ─────────────────────────────────────────────────────────────
set -euo pipefail

SCALE=${2:-2}   # default: 2 app replicas

echo "⬇  Pulling latest images..."
docker compose pull --quiet

echo "🔨  Building app image..."
docker compose build app

echo "🚀  Restarting services (${SCALE} app replicas)..."
docker compose up -d --scale app="${SCALE}" --remove-orphans

echo "🕐  Waiting for healthcheck..."
for i in $(seq 1 30); do
  STATUS=$(docker compose ps app | grep -c "healthy" || true)
  if [ "$STATUS" -ge 1 ]; then
    echo "✅  App is healthy."
    break
  fi
  echo "   attempt $i/30..."
  sleep 4
done

echo "📊  Container status:"
docker compose ps
