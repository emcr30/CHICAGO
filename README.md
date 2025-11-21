# CrimeGO

Este proyecto crea una app Streamlit que consume la API pública de la ciudad de Chicago y permite: 

- Traer datos desde la API (filtrando desde 2024 por defecto).
- Inyectar registros sintéticos con la estructura descrita (para geolocalizar, p. ej. cerca de tu casa).
- Persistir (opcional) los datos en una base SQLite local `chicago.db`.
- Visualizaciones simples: conteo por tipo, serie temporal por mes y mapa de puntos.

Requisitos

- Python 3.10+ (recomendado)
- Instalar dependencias:

```bash
python -m pip install -r requirements.txt
```

Ejecutar localmente

```bash
streamlit run main.py
```

Notas sobre volumen de datos y despliegue en la nube

- La API contiene muchos registros. Por defecto la app trae hasta 5k registros en memoria. Si quieres persistir todo 2024+ la app paginará y guardará en `chicago.db`.
- Para producción y datos grandes recomiendo usar un RDS/managed DB (Azure SQL, AWS RDS/Postgres) o un data lake. Si quieres puedo añadir la integración a Azure SQL o a AWS RDS.

Siguientes pasos sugeridos

- Añadir autenticación (si el servicio se publica en la nube).
- Mover persistencia a Postgres en la nube y añadir ETL programado para mantener datos recientes.
- Mejorar visualizaciones con Plotly o Kepler for large geospatial views.

Se puede visualizar la web ingresando al siguiente enlace:
[CRIMEN GO](https://crimengo.azurewebsites.net/)

## API para app móvil

Se añadió un servicio Flask que expone endpoints CRUD para que una app móvil inyecte y consulte registros.

- `GET /health` : estado del servicio
- `GET /records?limit=N` : obtener N registros más recientes
- `GET /records/<id>` : obtener un registro por id
- `POST /records` : insertar uno o varios registros (JSON object o lista)
- `PUT /records/<id>` : actualizar/insertar un registro con id
- `DELETE /records/<id>` : eliminar un registro

Ejemplo rápido (local):

```bash
# Construir y levantar ambos servicios (API y Streamlit)
docker-compose up --build

# Probar health
curl http://localhost:5000/health
```

Si al ejecutar `docker-compose up --build` obtienes un error similar a:

```
Error response from daemon: ports are not available: exposing port TCP 0.0.0.0:5000 -> 127.0.0.1:0: listen tcp 0.0.0.0:5000: bind: address already in use
```
significa que el puerto `5000` ya está ocupado en tu máquina.

Opciones para resolverlo:

- Liberar el puerto (buscar y detener el proceso que lo usa):

```bash
# Ver el proceso que está escuchando en 5000
sudo lsof -iTCP:5000 -sTCP:LISTEN -Pn

# Matar el proceso (reemplaza <PID> por el PID mostrado)
kill -9 <PID>
```

- Cambiar el puerto host que usa el servicio API editando el archivo `.env` y añadiendo (por ejemplo):

```dotenv
API_HOST_PORT=5001
STREAMLIT_HOST_PORT=8501
```

El `docker-compose.yml` usa por defecto `API_HOST_PORT=5001` si no defines `.env`, así evita choques con servicios que usen `5000`.

- Otra opción temporal es arrancar solo el servicio que necesitas:

```bash
# Sólo el API
docker-compose up --build api
# Sólo Streamlit
docker-compose up --build streamlit
```

Se incluye una colección Postman `postman_collection.json` con ejemplos.

Notas de seguridad: las credenciales a la base de datos se leen desde `.env`. El archivo `.env` está en `.dockerignore` para evitar que se copie en la imagen.

Modo local (SQLite)
--------------------
Por conveniencia la aplicación ahora usa SQLite por defecto (modo local) para que siga funcionando aunque la base PostgreSQL de Azure esté inaccesible.

- El backend seleccionado se controla por la variable de entorno `DB_MODE`:
	- `DB_MODE=sqlite` (valor por defecto) — usa un archivo local `chicago_local.db` dentro del contenedor.
	- `DB_MODE=postgres` — usa las variables `PG_HOST`, `PG_USER`, etc. como antes.

Si quieres forzar Postgres, añade a tu `.env`:

```dotenv
DB_MODE=postgres
PG_HOST=...
PG_USER=...
PG_PASSWORD=...
```

Si prefieres seguir en modo local (por ahora no cambies nada), `docker-compose up --build` usará SQLite y no intentará conectarse a Azure.
