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
En la primera versión

Notas sobre volumen de datos y despliegue en la nube

- La API contiene muchos registros. Por defecto la app trae hasta 5k registros en memoria. Si quieres persistir todo 2024+ la app paginará y guardará en `chicago.db`.
- Para producción y datos grandes recomiendo usar un RDS/managed DB (Azure SQL, AWS RDS/Postgres) o un data lake. Si quieres puedo añadir la integración a Azure SQL o a AWS RDS.

Siguientes pasos sugeridos

- Añadir autenticación (si el servicio se publica en la nube).
- Mover persistencia a Postgres en la nube y añadir ETL programado para mantener datos recientes.
- Mejorar visualizaciones con Plotly o Kepler for large geospatial views.

Se puede visualizar la web ingresando al siguiente enlace:
[CRIMEN GO](https://crimengo.azurewebsites.net/)

## Uso de Docker

**1. Construir y ejecutar la API Flask:**

```bash
docker build -f Dockerfile.api -t api-test .
docker run -p 5002:5000 api-test
```

La API estará disponible en `http://localhost:5002/health`

**2. Construir y ejecutar Streamlit:**

```bash
docker build -f Dockerfile.streamlit -t streamlit-test .
docker run -p 8501:8501 streamlit-test
```

La UI estará disponible en `http://localhost:8501`

Estructura del Repositorio
---------------------------

```
CHICAGO (raíz del proyecto)
│
├── Core - Backend API
│   ├── api.py              - API REST Flask con endpoints CRUD
│   ├── db_postgres.py      - Abstracción de BD (SQLite/Postgres)
│   ├── auth.py             - Autenticación y manejo de usuarios
│   └── users.json          - Base de datos de usuarios (dev)
│
├── Frontend - Interfaz Streamlit
│   ├── main.py             - Aplicación principal Streamlit (UI/Admin)
│   ├── data.py             - Lógica de datos y consumo de API Chicago
│   ├── viz.py              - Funciones de visualización (mapas, gráficos)
│   └── notify_config.json  - Configuración de notificaciones
│
├── Docker & Deployment
│   ├── Dockerfile          - Imagen principal (nginx + gunicorn + streamlit)
│   ├── Dockerfile.api      - Imagen separada para API Flask
│   ├── Dockerfile.streamlit - Imagen separada para UI Streamlit
│   ├── docker-compose.yml  - Orquestación multi-contenedor
│   ├── start.sh            - Script de arranque para contenedor
│   ├── nginx.conf          - Configuración proxy nginx
│   └── supervisord.conf    - Configuración supervisord (deprecated, usar start.sh)
│
├── Configuración
│   ├── requirements.txt    - Dependencias Python (pip)
│   ├── .env                - Variables de entorno (no subir a git)
│   ├── .dockerignore       - Archivos a excluir en build Docker
│   └── .gitignore          - Archivos a excluir en git
│
├── Documentación & Testing
│   ├── README.md           - Documentación completa (este archivo)
│   ├── postman_collection.json - Ejemplos de API endpoints
│   └── __pycache__/        - Caché Python (ignorado en git)
│
└── Datos
    ├── chicago.db          - Base de datos SQLite local (opcional)
    └── data/               - Carpeta para almacenamiento persistente


## API para app móvil

Se añadió un servicio Flask que expone endpoints CRUD para que una app móvil inyecte y consulte registros.

- `GET /health` : estado del servicio
- `GET /records?limit=N` : obtener N registros más recientes
- `GET /records/<id>` : obtener un registro por id
- `POST /records` : insertar uno o varios registros (JSON object o lista)
- `PUT /records/<id>` : actualizar/insertar un registro con id
- `DELETE /records/<id>` : eliminar un registro


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

-------------------------------------------------------------------------------

**README completo — Uso, despliegue y referencia (Docker / Docker Hub / Azure)**

Resumen
-------

Este repositorio contiene una aplicación Streamlit llamada CRIMENGO y una API Flask para integrar con una app móvil. La aplicación por defecto usa SQLite localmente para persistir registros si no configuras PostgreSQL.

Contenido del repositorio (archivos clave añadidos/actualizados)
-----------------------------------------------------------

- `main.py` — Interfaz Streamlit (UI) para administrar y visualizar registros.
- `data.py` — Lógica para consumir la API pública de Chicago y generar registros sintéticos de Arequipa.
- `viz.py` — Funciones de visualización usadas por Streamlit.
- `auth.py` / `users.json` — Manejo básico de usuarios/admin para la UI.
- `db_postgres.py` — Abstracción de almacenamiento: soporta `sqlite` (por defecto) y `postgres` (activar con `DB_MODE=postgres`). Contiene:
	- Creación de tabla en SQLite y en Postgres (si se configura).
	- `insert_crimes`, `fetch_latest_crimes`, `fetch_crime_by_id`, `delete_crime_by_id`.
	- Normalización de tipos (timestamps, booleans, dicts) para evitar errores de bind.
	- Forzado de timestamps recientes: todas las inserciones actualizan `date` a hoy (UTC) - al menos 1 hora (aleatorio entre 1h y 1h59m59s) y `updated_on` = ahora.
- `api.py` — API Flask con endpoints CRUD para la app móvil:
	- `GET /health`
	- `GET /records?limit=N`
	- `GET /records/<id>`
	- `POST /records` (acepta objeto JSON o lista)
	- `PUT /records/<id>`
	- `DELETE /records/<id>`
- `Dockerfile` — imagen base que instala dependencias y expone Streamlit.
- `docker-compose.yml` — orquesta 2 servicios: `api` (gunicorn) y `streamlit`.
- `.dockerignore` — evita copiar `.env` y archivos no deseados a la imagen.
- `postman_collection.json` — colección mínima para probar la API localmente.
- `.env` — variables de entorno (no subir a repositorio público). Ejemplo de contenido en secciones abajo.

Uso con Docker Compose (recomendado para desarrollo local)
------------------------------------------------------

1. Coloca tu archivo `.env` en la raíz del proyecto (ver ejemplo abajo).
2. Desde la carpeta del proyecto ejecuta:

```bash
docker-compose up --build
```

Por defecto los servicios mapean a estos puertos en el host:
- API (gunicorn): `http://localhost:${API_HOST_PORT:-5001}` -> contenedor `:5000`
- Streamlit: `http://localhost:${STREAMLIT_HOST_PORT:-8501}` -> contenedor `:8501`

Si necesitas arrancar sólo uno de los servicios:

```bash
docker-compose up --build api
docker-compose up --build streamlit
```

Usar la imagen desde Docker Hub
-------------------------------

Si prefieres no construir localmente, puedes usar la imagen pública que mencionaste:

```bash
# Descargar la imagen
docker pull emcr30/chicagofullv3:latest

# Ejecutar la API (ejemplo: exponer puerto host 5001 -> contenedor 5000)
docker run -d --env-file .env -p 5001:5000 --name crimengo_api emcr30/chicagofullv3:latest gunicorn -w 4 -b 0.0.0.0:5000 api:app

# Ejecutar Streamlit (si la imagen incluye Streamlit)
docker run -d --env-file .env -p 8501:8501 --name crimengo_ui emcr30/chicagofullv3:latest streamlit run main.py --server.port=8501 --server.address=0.0.0.0
```

Variables de entorno (lista y ejemplo)
-------------------------------------

Variables relevantes (poner en `.env`):

- `DB_MODE` — `sqlite` (por defecto) o `postgres`.
- `SQLITE_PATH` — ruta al archivo sqlite dentro del contenedor (default `chicago_local.db`).
- `PG_HOST`, `PG_DBNAME`, `PG_USER`, `PG_PASSWORD`, `PG_PORT`, `PG_SSLMODE` — para Postgres.
- `API_HOST_PORT`, `STREAMLIT_HOST_PORT` — puertos host si usas `docker-compose`.

Ejemplo mínimo `.env` para SQLite (local, seguro para dev):

```dotenv
DB_MODE=sqlite
SQLITE_PATH=chicago_local.db
API_HOST_PORT=5001
STREAMLIT_HOST_PORT=8501
```

Ejemplo `.env` para Postgres (Azure) — NO dejes credenciales en repositorio público:

```dotenv
DB_MODE=postgres
PG_HOST=crimengo.postgres.database.azure.com
PG_DBNAME=postgres
PG_USER=TaylorSwift
PG_PASSWORD="CamilaCabello1997#"
PG_PORT=5432
PG_SSLMODE=require
API_HOST_PORT=5001
STREAMLIT_HOST_PORT=8501
```

Nota: si tu contraseña contiene `#` o espacios, ponla entre comillas.

Endpoints (resumen rápido)
--------------------------

- `GET /health` — devuelve `{'status':'ok'}`
- `GET /records?limit=N` — devuelve hasta N registros más recientes (N por defecto 1000)
- `GET /records/<id>` — devuelve un registro por id
- `POST /records` — inserta uno o varios registros (JSON object o list)
- `PUT /records/<id>` — inserta/actualiza un registro con id
- `DELETE /records/<id>` — elimina registro por id

Ejemplo con `curl`:

```bash
curl http://localhost:5001/health

curl -X POST http://localhost:5001/records \
	-H 'Content-Type: application/json' \
	-d '{"id":"LOCAL-TEST-1","case_number":"AQP202500000","date":"2025-11-20T12:00:00Z","primary_type":"ROBO","description":"Test"}'

curl http://localhost:5001/records/LOCAL-TEST-1
```

Comportamiento de fechas
-------------------------

Por diseño todas las inserciones son normalizadas para usar una fecha reciente y coherente con el sistema de alarmas:

- Antes de insertarse, cada registro recibe `date = UTC now - (1 hour + random 0-59 minutes + random 0-59 seconds)`.
- `updated_on` se establece a `UTC now`.
- `year` se actualiza en consecuencia.

Esto garantiza que los eventos inyectados parezcan ocurridos en el día actual y muy recientes (al menos 1 hora antes), como requeriste.

Persistencia SQLite fuera del contenedor (recomendado para dev)
----------------------------------------------------------------

Si usas `DB_MODE=sqlite` y quieres que el archivo `chicago_local.db` sobreviva reinicios del contenedor, añade un volumen host en `docker-compose.yml`:

```yaml
services:
	streamlit:
		volumes:
			- ./data:/app/data
		environment:
			- SQLITE_PATH=/app/data/chicago_local.db

	api:
		volumes:
			- ./data:/app/data
		environment:
			- SQLITE_PATH=/app/data/chicago_local.db
```

Luego en tu `.env` ajusta `SQLITE_PATH=/app/data/chicago_local.db`.

Colección Postman
-----------------

Importa `postman_collection.json` (en la raíz del repo) en Postman para tener ejemplos listos.

Problemas comunes y solución rápida
----------------------------------

- Error de puerto en `docker-compose up`: otro proceso usa el puerto (5000/8501). Cambia `API_HOST_PORT` o para el proceso que usa el puerto.
- Error binding parameter 'Timestamp' o problemas con `pandas.Timestamp`: la app normaliza timestamps antes de insertarlos; asegúrate de reconstruir la imagen después de cambios: `docker-compose up --build`.
- La app no conecta a Postgres: verifica `DB_MODE=postgres` y que las variables `PG_*` sean correctas. Revisa `docker-compose logs -f` para el traceback.

Seguridad y buenas prácticas
----------------------------

- No comitas `.env` con credenciales. Usa secretos de la plataforma (Azure App Service, ACR, GitHub Actions secrets) para despliegue en producción.
- Añade autenticación a la API (API key o JWT) antes de exponerla públicamente si será consumida por apps móviles/terceros.

Despliegue en Azure (breve)
---------------------------

Puedes desplegar usando:
- Azure App Service (multi-container) con `docker-compose.yml`.
- Azure Container Instances (ACI) o Azure Kubernetes Service (AKS) si necesitas escalado.

Pasos cortos para App Service (Linux, multi-container):

1. Sube `docker-compose.yml` al App Service (o referencia imagen desde Docker Hub `emcr30/chicagofullv3`).
2. Configura las `Application settings` con las variables de entorno (no subir `.env`).
3. Arranca y revisa logs en Azure Portal.

Soporte
-------

Si algo no funciona, incluye:
- Comando que ejecutaste.
- Salida de `docker-compose logs -f` o logs del contenedor.
- Payload JSON que intentaste insertar (si aplica).

---

Si deseas, puedo:

- Añadir autenticación básica por API key a `api.py`.
- Crear un `Dockerfile` optimizado sólo para API y publicarlo en Docker Hub.
- Añadir persistencia por defecto en `docker-compose.yml` para SQLite (volumen host) y confirmar que los datos sobreviven reinicios.
# Chicago Crime Alert - Alerta Arequipa

Sistema de alertas de seguridad en tiempo real para Android.

## 📱 Descargar APK

[**Descargar última versión**](https://github.com/emcr30/CHICAGO/releases/latest)

## 🚀 Características

- ✅ Monitoreo en tiempo real de zonas peligrosas
- ✅ Alertas sonoras basadas en proximidad
- ✅ Visualización en mapa interactivo
- ✅ Detección automática de ubicación
- ✅ Niveles de peligro (Bajo, Medio, Alto)

## 📥 Instalación

1. Descarga el APK desde [Releases](https://github.com/emcr30/CHICAGO/releases)
2. Permite la instalación de fuentes desconocidas en tu dispositivo Android
3. Instala el APK
4. Otorga los permisos solicitados (ubicación, notificaciones)

## 🔧 Requisitos

- Android 8.0 (API 26) o superior
- GPS activado
- Conexión a internet

## 🛠️ Desarrollo

### Estructura del proyecto
```
android-app/       - Aplicación Android (Kotlin)
releases/         - APKs compilados
```
## 👥 Autores

- Marycielo Guadalupe Bedoya Pinto
- Kenny Luis Flores Chacón
- Angela Milagros Quispe Huanca
- Evelyn Milagros Chipana Ramos
Fin del README.
