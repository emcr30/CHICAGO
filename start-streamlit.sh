#!/usr/bin/env bash
set -euo pipefail

# Start Streamlit using the PORT env var provided by the platform (Railway)
# Fallback to 8501 when PORT is not set (useful for local dev)
PORT="${PORT:-8501}"
echo "Starting Streamlit on port $PORT"

exec streamlit run main.py --server.port "$PORT" --server.address 0.0.0.0 --logger.level=error
