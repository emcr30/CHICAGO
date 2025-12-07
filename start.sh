#!/bin/bash
exec gunicorn -w 4 -b 0.0.0.0:${PORT:-8000} --timeout 120 api:app
