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
EXPOSE 8000

# Run API with gunicorn on fixed port
CMD exec gunicorn -w 4 -b 0.0.0.0:8000 --timeout 120 api:app
