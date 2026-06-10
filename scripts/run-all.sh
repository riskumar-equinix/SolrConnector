#!/usr/bin/env bash
# Start Solr 9.10 (if not already running), then run the SolrConnector app.
# Uses SOLR9_HOME / SOLR_PORT like start-solr9.sh. Run from SolrConnector root or scripts/.

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOLR_CONNECTOR_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_SOLR9="${SOLR_CONNECTOR_ROOT}/../solr-9.10.1 2"
# If Solr lives next to GitHub repo (sibling of Solr-POC), try that too
if [[ ! -d "$DEFAULT_SOLR9" ]]; then
  DEFAULT_SOLR9="${SOLR_CONNECTOR_ROOT}/../../solr-9.10.1 2"
fi
SOLR9_HOME="${SOLR9_HOME:-$DEFAULT_SOLR9}"
PORT="${SOLR_PORT:-8983}"
SOLR_URL="http://localhost:${PORT}/solr"
MAX_WAIT=60

cd "$SOLR_CONNECTOR_ROOT"

# Check if Solr is already up
if curl -s -o /dev/null -w "%{http_code}" "${SOLR_URL}/admin/ping" 2>/dev/null | grep -q 200; then
  echo "Solr already running at ${SOLR_URL}"
else
  if [[ ! -d "$SOLR9_HOME" ]] || [[ ! -x "$SOLR9_HOME/bin/solr" ]]; then
    echo "Solr 9.10 not found at: $SOLR9_HOME"
    echo "Set SOLR9_HOME to your Solr 9.10 binary install and run again."
    exit 1
  fi
  echo "Starting Solr 9.10 on port $PORT ..."
  "$SOLR9_HOME/bin/solr" start -p "$PORT"
  echo "Waiting for Solr to be ready (up to ${MAX_WAIT}s) ..."
  for i in $(seq 1 "$MAX_WAIT"); do
    if curl -s -o /dev/null -w "%{http_code}" "${SOLR_URL}/admin/ping" 2>/dev/null | grep -q 200; then
      echo "Solr is ready."
      break
    fi
    if [[ $i -eq $MAX_WAIT ]]; then
      echo "Solr did not become ready in time."
      exit 1
    fi
    sleep 1
  done
fi

echo "Running SolrConnector (Oracle → Solr indexer) ..."
exec mvn -q exec:java
