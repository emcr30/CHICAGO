FROM python:3.11-slim

# Instala dependencias del sistema
RUN apt-get update && \
    apt-get install -y build-essential gcc libpq-dev nginx supervisor && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copia requirements y dependencias Python
COPY requirements.txt ./
RUN pip install --upgrade pip && pip install -r requirements.txt

# Copia el código de la app
COPY . /app

# Copia configs de nginx y supervisord
COPY nginx.conf /etc/nginx/nginx.conf
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

# Ajusta permisos para streamlit
RUN chmod -R 777 /tmp

# Expone solo el puerto 80 (nginx)
EXPOSE 80

# Comando de arranque: supervisord (levanta gunicorn y streamlit), nginx en foreground
CMD service nginx start && supervisord -c /etc/supervisor/conf.d/supervisord.conf
