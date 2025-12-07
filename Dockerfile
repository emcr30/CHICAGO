FROM python:3.11-slim

# Install system dependencies
RUN apt-get update && \
    apt-get install -y build-essential gcc libpq-dev && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy requirements and install Python dependencies
COPY requirements.txt ./
RUN pip install --upgrade pip && pip install -r requirements.txt

# Copy application code
COPY . /app

# Expose port
EXPOSE 5000

# Copy startup script
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

# Run using Procfile
CMD ["bash", "-c", "gunicorn -b 0.0.0.0:${PORT:-5000} api:app"]
