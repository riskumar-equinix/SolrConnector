#!/usr/bin/env bash
# Start Solr 9.10 binary (oracle-core ready for SolrConnector).
# Set SOLR9_HOME to your Solr 9.10 install dir, or run from SolrConnector with sibling solr-9.10.1 2.

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOLR_CONNECTOR_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
DEFAULT_SOLR9="${SOLR_CONNECTOR_ROOT}/../solr-9.10.1 2"
SOLR9_HOME="${SOLR9_HOME:-$DEFAULT_SOLR9}"
PORT="${SOLR_PORT:-8983}"

if [[ ! -d "$SOLR9_HOME" ]]; then
  echo "Solr 9.10 directory not found: $SOLR9_HOME"
  echo "Set SOLR9_HOME to your Solr 9.10 binary install, e.g.:"
  echo "  export SOLR9_HOME=/path/to/solr-9.10.1"
  echo "  $0"
  exit 1
fi

if [[ ! -x "$SOLR9_HOME/bin/solr" ]]; then
  echo "Not a Solr install (no bin/solr): $SOLR9_HOME"
  exit 1
fi

echo "Starting Solr 9.10 from $SOLR9_HOME on port $PORT ..."
"$SOLR9_HOME/bin/solr" start -p "$PORT"
echo "Solr 9.10 running at http://localhost:$PORT/solr (oracle-core: http://localhost:$PORT/solr/oracle-core)"
