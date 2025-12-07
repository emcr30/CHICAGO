# Configuración de Dos Servicios en Railway

Este proyecto tiene dos servicios que deben ejecutarse juntos en Railway:

## 1. **Servicio API (web)** - Flask
- **Puerto**: Dinámico (asignado por Railway)
- **Comando**: `gunicorn -w 4 -b 0.0.0.0:${PORT} --timeout 120 api:app`
- **Dockerfile**: `Dockerfile.railway`
- **Endpoints**:
  - `GET /health` - Verificar que la API está viva
  - `GET /records` - Obtener registros de crímenes
  - `GET /records/<id>` - Obtener registro específico
  - `POST /records` - Crear registro
  - `PUT /records/<id>` - Actualizar registro
  - `DELETE /records/<id>` - Eliminar registro

## 2. **Servicio Dashboard (dashboard)** - Streamlit
- **Puerto**: Dinámico (asignado por Railway)
- **Comando**: `streamlit run main.py --server.port=${PORT} --server.address=0.0.0.0`
- **Dockerfile**: `Dockerfile.streamlit`
- **Conecta a**: API Flask para obtener datos
- **Función**: Interfaz web de visualización de datos

## Configuración en Railway

### Opción A: Usando el Dashboard de Railway

1. Ve a https://railway.app/dashboard
2. Crea un nuevo proyecto: "Create New Project" → "Deploy from GitHub"
3. Selecciona: `emcr30/CHICAGO`
4. Una vez que se cree el primer servicio (web):
   - Haz clic en "Add Service"
   - Selecciona "Deploy from GitHub" nuevamente
   - Selecciona el mismo repositorio
   - Cambia el Dockerfile a `Dockerfile.streamlit`
   - Configura el comando de inicio: `streamlit run main.py --server.port=${PORT} --server.address=0.0.0.0`

5. Para cada servicio, agrega las variables de entorno:
   - Base de datos PostgreSQL (Railway auto-inyectará)
   - `DB_MODE=postgres`
   - `API_URL=<URL del servicio web>` (para Streamlit)

### Opción B: Usando el archivo railway.json

El archivo `railway.json` ya contiene la configuración de ambos servicios. Railway debería detectarlo automáticamente.

## Variables de Entorno Necesarias

### Ambos servicios:
```
DB_MODE=postgres
```

### Variables de PostgreSQL (auto-inyectadas por Railway):
```
PGHOST
PGPORT
PGUSER
PGPASSWORD
PGDATABASE
```

### Solo para Streamlit (dashboard):
```
API_URL=https://web-production-xxxxx.railway.app
```

Reemplaza `xxxxx` con el ID real de tu servicio web.

## Pruebas

Una vez desplegado:

### 1. Verificar que la API funciona:
```bash
curl https://tu-web.railway.app/health
```

### 2. Acceder a Streamlit:
Abre el URL del servicio dashboard en tu navegador

### 3. Verificar logs:
En Railway Dashboard → Logs → Buscar errores

## Solución de Problemas

### Si Streamlit muestra error de conexión:
1. Verifica que `API_URL` esté configurado correctamente
2. Revisa los logs de Streamlit
3. Asegúrate que el servicio web (API) esté corriendo

### Si el puerto no se asigna correctamente:
1. Revisa que los comandos usan `${PORT}` no `${PORT:-xxxx}`
2. Reinicia el servicio desde Railway

### Si PostgreSQL no conecta:
1. Verifica que PostgreSQL esté en "Connected" en Railway
2. Asegúrate que `DB_MODE=postgres` esté configurado
3. Revisa los logs de error en Railway

## Estructura de Archivos

```
CHICAGO/
├── api.py                 # API Flask (servicio web)
├── main.py               # Streamlit dashboard
├── Dockerfile.railway    # Para ambos servicios (API)
├── Dockerfile.streamlit  # Específico para Streamlit
├── Procfile              # Configuración de procesos
├── railway.json          # Configuración de Railway
├── .streamlit/           # Configuración de Streamlit
│   └── config.toml
└── requirements.txt      # Dependencias
```

## Despliegue Automático

Con cada push a `main` en GitHub, Railway automáticamente redeploy ambos servicios.

Para desplegar cambios:
```bash
git add .
git commit -m "Cambios importantes"
git push origin main
```

Railway detectará los cambios y redesplegará automáticamente.
