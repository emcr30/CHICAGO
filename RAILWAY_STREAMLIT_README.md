Streamlit deployment (Railway)
================================

This file explains a simple, reliable way to deploy only the Streamlit dashboard as a standalone service on Railway (or any container host).

What we changed
- Added `start-streamlit.sh` to expand `PORT` at runtime and run Streamlit.
- Updated `Dockerfile.streamlit` to use the script as the container entrypoint.

Quick local test (from repo root)

```bash
# Build image for testing
docker build -f Dockerfile.streamlit -t chicago-streamlit:local .

# Run locally mapping to port 8501
docker run -p 8501:8501 -e PORT=8501 -e API_URL=http://host.docker.internal:5000 chicago-streamlit:local
```

Deploy to Railway (recommended simple flow)

1. Create a new Railway project for the Streamlit dashboard (keep API in a different Railway project or elsewhere).
2. Connect the GitHub repo or push a Docker image. If using the repo/GitHub approach, in Railway set the root Dockerfile to `Dockerfile.streamlit` (Railway UI -> Settings -> Service -> Build settings) or put the service in a subdirectory with its own Dockerfile.
3. Add environment variables in Railway:
   - `PORT` is provided by Railway automatically at runtime — no need to set it.
   - `API_URL` set to the URL of your deployed API (important so the app mobile can use the endpoints).
4. Deploy and check logs. Ensure the service listens on the provided `PORT` (our entrypoint script prints the port at startup).

Notes / troubleshooting
- Do not put `${PORT}` inside `.streamlit/config.toml` — Streamlit will attempt to parse the file and fail. Use the CLI `--server.port` option as we do in the start script.
- If Railway still builds the wrong Dockerfile, explicitly configure the service build to use `Dockerfile.streamlit` (Railway web UI) or create a repository subfolder for streamlit with its own Dockerfile.
