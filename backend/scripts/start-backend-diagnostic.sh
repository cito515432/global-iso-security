#!/bin/sh
set -eu

JAR_PATH="${JAR_PATH:-/app/app.jar}"
PORT="${PORT:-10000}"
TIMEOUT_SECONDS="${STARTUP_DIAG_TIMEOUT_SECONDS:-150}"
SECOND_DUMP_DELAY_SECONDS="${STARTUP_DIAG_SECOND_DUMP_DELAY_SECONDS:-20}"
SMOKE_ONLY="${STARTUP_SMOKE_ONLY:-false}"
READY_FILE="${STARTUP_DIAGNOSTIC_READY_FILE:-/tmp/backend-startup-ready}"
REQUIRE_HEALTH="${STARTUP_DIAG_REQUIRE_HEALTH:-false}"
JAVA_BIN="${JAVA_BIN:-java}"

started_at=$(date +%s)
started_at_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)
rm -f "$READY_FILE"

probe_health() {
    if command -v curl >/dev/null 2>&1; then
        curl --fail --silent --show-error --max-time 3 "http://127.0.0.1:${PORT}/health" >/dev/null 2>&1
        return $?
    fi
    if command -v wget >/dev/null 2>&1; then
        wget --quiet --timeout=3 --tries=1 -O /dev/null "http://127.0.0.1:${PORT}/health" >/dev/null 2>&1
        return $?
    fi
    return 2
}

if [ -n "${JAVA_CLASSPATH:-}" ] && [ -n "${JAVA_MAIN_CLASS:-}" ]; then
    "$JAVA_BIN" -cp "$JAVA_CLASSPATH" "$JAVA_MAIN_CLASS" &
else
    "$JAVA_BIN" -jar "$JAR_PATH" &
fi
JAVA_PID=$!
echo "[STARTUP-DIAG] process-start pid=${JAVA_PID} port=${PORT} timeoutSeconds=${TIMEOUT_SECONDS} startedAt=${started_at_utc}"

dump_sent=0
dump_at=0
while kill -0 "$JAVA_PID" 2>/dev/null; do
    now=$(date +%s)
    elapsed=$((now - started_at))

    if probe_health; then
        probe_status=0
    else
        probe_status=$?
    fi

    if [ -f "$READY_FILE" ]; then
        echo "[STARTUP-DIAG] application-ready-marker elapsedSeconds=${elapsed}"
        if [ "$probe_status" -eq 0 ]; then
            echo "[STARTUP-DIAG] health-ready elapsedSeconds=${elapsed}"
            if [ "$SMOKE_ONLY" = "true" ]; then
                kill "$JAVA_PID" 2>/dev/null || true
                wait "$JAVA_PID" 2>/dev/null || true
                exit 0
            fi
            wait "$JAVA_PID"
            exit $?
        fi
        if [ "$probe_status" -eq 2 ]; then
            echo "[STARTUP-DIAG] health-probe-unavailable marker-confirmed elapsedSeconds=${elapsed}"
            if [ "$REQUIRE_HEALTH" != "true" ]; then
                if [ "$SMOKE_ONLY" = "true" ]; then
                    kill "$JAVA_PID" 2>/dev/null || true
                    wait "$JAVA_PID" 2>/dev/null || true
                    exit 0
                fi
                wait "$JAVA_PID"
                exit $?
            fi
        else
            echo "[STARTUP-DIAG] health-not-ready marker-present elapsedSeconds=${elapsed}"
        fi
    fi

    if [ "$dump_sent" -eq 0 ] && [ "$elapsed" -ge "$TIMEOUT_SECONDS" ]; then
        if [ "$probe_status" -eq 2 ]; then
            echo "[STARTUP-DIAG] health-probe-unavailable elapsedSeconds=${elapsed}"
        else
            echo "[STARTUP-DIAG] health-not-ready elapsedSeconds=${elapsed}"
        fi
        echo "[STARTUP-DIAG] thread-dump-requested signal=SIGQUIT pid=${JAVA_PID} elapsedSeconds=${elapsed}"
        kill -3 "$JAVA_PID" 2>/dev/null || true
        dump_sent=1
        dump_at=$elapsed
    elif [ "$dump_sent" -eq 1 ] && [ "$elapsed" -ge $((dump_at + SECOND_DUMP_DELAY_SECONDS)) ]; then
        echo "[STARTUP-DIAG] second-thread-dump-requested signal=SIGQUIT pid=${JAVA_PID} elapsedSeconds=${elapsed}"
        kill -3 "$JAVA_PID" 2>/dev/null || true
        dump_sent=2
    fi
    sleep 2
done

wait "$JAVA_PID"
