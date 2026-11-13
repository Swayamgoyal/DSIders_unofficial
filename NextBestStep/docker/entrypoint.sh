#!/usr/bin/env bash
# Builds Next Best Step inside the container and copies the APKs to /dist.
#
# Usage: entrypoint.sh [debug|release|test|all]
#   debug    unit tests + debug APK   (default)
#   release  unit tests + release APK (minified, signed with the debug key)
#   test     unit tests only
#   all      unit tests + debug and release APKs
#
# Keys come from the environment (OPENAI_API_KEY / LLM_API_KEY, OPENAI_MODEL,
# OPENAI_BASE_URL, ADMIN_PASSWORD); app/build.gradle.kts reads them directly.
set -euo pipefail

MODE="${1:-debug}"
DIST=/dist
cd /app

if [[ -z "${OPENAI_API_KEY:-}${LLM_API_KEY:-}" ]]; then
  echo "Note: no OPENAI_API_KEY / LLM_API_KEY set. The app will use its offline fallback (template questions, keyword matching)."
fi

case "$MODE" in
  debug)   TASKS=(:app:testDebugUnitTest :app:assembleDebug) ;;
  release) TASKS=(:app:testDebugUnitTest :app:assembleRelease) ;;
  test)    TASKS=(:app:testDebugUnitTest) ;;
  all)     TASKS=(:app:testDebugUnitTest :app:assembleDebug :app:assembleRelease) ;;
  bash|sh) exec bash ;;
  *) echo "Unknown mode '$MODE'. Use: debug | release | test | all | bash" >&2; exit 2 ;;
esac

# `gradle` is installed in the image at the wrapper's version (see Dockerfile).
echo "Running: gradle ${TASKS[*]}"
status=0
gradle "${TASKS[@]}" || status=$?

mkdir -p "$DIST"
# Test report is copied even when tests fail, so the failure can be inspected.
if [[ -d app/build/reports/tests ]]; then
  rm -rf "$DIST/test-report"
  cp -r app/build/reports/tests/testDebugUnitTest "$DIST/test-report" 2>/dev/null || true
fi
[[ $status -ne 0 ]] && { echo "Build failed (exit $status)." >&2; exit $status; }

find app/build/outputs/apk -name '*.apk' -exec cp {} "$DIST/" \; 2>/dev/null || true

# On Linux hosts the files would otherwise be owned by root.
if [[ -n "${HOST_UID:-}" ]]; then
  chown -R "${HOST_UID}:${HOST_GID:-$HOST_UID}" "$DIST"
fi

echo
echo "Done. Output in ./dist:"
ls -1 "$DIST"
