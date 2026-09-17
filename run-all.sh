#!/usr/bin/env bash
# POS Tunisie — run backend (Spring Boot) + frontend (JavaFX) together.
# Usage:
#   ./run-all.sh                  # backend + frontend
#   ./run-all.sh --backend-only   # API only (http://localhost:8080)
#   ./run-all.sh --frontend-only  # JavaFX only (needs backend already up)
#   ./run-all.sh --help
#
# Env: backend loads backend-pos/.env itself via springboot4-dotenv.
# The script also parses it (tolerant parser, values may contain & ! spaces
# unquoted) to learn SERVER_PORT for the health check and POS_API_BASE.
# Frontend points at POS_API_BASE (default http://localhost:${SERVER_PORT:-8080}/api/v1).

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT/backend-pos"
FRONTEND_DIR="$ROOT/desktop-pos"
BACKEND_LOG="/tmp/pos-backend.log"
FRONTEND_LOG="/tmp/pos-frontend.log"

MODE="all"
for arg in "$@"; do
  case "$arg" in
    --backend-only) MODE="backend" ;;
    --frontend-only) MODE="frontend" ;;
    -h|--help)
      grep '^#' "$0" | grep -v '^#!' | grep -v '^# ---' | sed 's/^# \{0,1\}//'
      echo ""
      echo "Logs: $BACKEND_LOG, $FRONTEND_LOG"
      exit 0
      ;;
    *) echo "Unknown arg: $arg (try --help)" >&2; exit 1 ;;
  esac
done

# --- Load backend .env (tolerant parser: values may contain & ! spaces unquoted) ---
if [[ -f "$BACKEND_DIR/.env" ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
    [[ "$line" != *"="* ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    key="$(echo "$key" | tr -d '[:space:]')"
    value="${value%\"}"; value="${value#\"}"
    value="${value%\'}"; value="${value#\'}"
    [[ -z "$key" ]] && continue
    export "$key=$value"
  done < "$BACKEND_DIR/.env"
  echo "Loaded $BACKEND_DIR/.env"
else
  echo "No backend-pos/.env — using defaults (copy backend-pos/.env.example to .env to customize)."
fi

PORT="${SERVER_PORT:-8080}"
API_BASE="${POS_API_BASE:-http://localhost:${PORT}/api/v1}"

command -v java >/dev/null || { echo "ERROR: java not found (need JDK 26 for backend)." >&2; exit 1; }
[[ -x "$BACKEND_DIR/mvnw" ]] || chmod +x "$BACKEND_DIR/mvnw" 2>/dev/null || true
[[ -x "$FRONTEND_DIR/mvnw" ]] || chmod +x "$FRONTEND_DIR/mvnw" 2>/dev/null || true

BACKEND_PID=""
cleanup() {
  echo ""
  echo "Stopping..."
  [[ -n "$BACKEND_PID" ]] && kill "$BACKEND_PID" 2>/dev/null || true
  # javafx:run spawns java children under the shell — kill the group on exit
  jobs -p | xargs -r kill 2>/dev/null || true
  exit 0
}
trap cleanup INT TERM

wait_for_backend() {
  echo "Waiting for backend on :$PORT ..."
  for i in $(seq 1 90); do
    if curl -sf -o /dev/null "http://localhost:${PORT}/api/v1/categories" 2>/dev/null \
    || curl -sf -o /dev/null "http://localhost:${PORT}/api-docs" 2>/dev/null; then
      echo "Backend UP → http://localhost:${PORT} (swagger: /swagger-ui.html)"
      return 0
    fi
    sleep 2
  done
  echo "ERROR: backend did not start in ~3 min. See $BACKEND_LOG" >&2
  return 1
}

start_backend() {
  echo "Starting backend (log: $BACKEND_LOG)..."
  pushd "$BACKEND_DIR" >/dev/null
  ./mvnw -q spring-boot:run >"$BACKEND_LOG" 2>&1 &
  BACKEND_PID=$!
  popd >/dev/null
  wait_for_backend
}

start_frontend() {
  echo "Starting frontend → $API_BASE (log: $FRONTEND_LOG)..."
  echo "Close the POS window or press Ctrl-C to stop."
  pushd "$FRONTEND_DIR" >/dev/null
  ./mvnw -q javafx:run -Dpos.apiBase="$API_BASE" 2>&1 | tee "$FRONTEND_LOG"
  popd >/dev/null
}

case "$MODE" in
  backend)
    start_backend
    echo "Backend running (PID $BACKEND_PID). Tail logs: tail -f $BACKEND_LOG"
    wait "$BACKEND_PID"
    ;;
  frontend)
    start_frontend
    ;;
  all)
    start_backend
    start_frontend
    ;;
esac
