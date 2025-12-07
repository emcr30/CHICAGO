# 🚀 GUÍA COMPLETA DE DESPLIEGUE EN RAILWAY

## ✅ LO QUE YA ESTÁ LISTO

Tu proyecto **CHICAGO** ha sido preparado completamente para Railway y el código está actualizado en GitHub:
- **Repositorio:** https://github.com/emcr30/CHICAGO
- **Rama:** main
- **Último commit:** Configuración final para Railway

### Archivos preparados:
- ✅ `Dockerfile.railway` - Dockerfile optimizado para Railway
- ✅ `railway.json` - Configuración de Railway
- ✅ `.railwayrc.json` - Configuración local
- ✅ `deploy-to-railway.sh` - Script de despliegue
- ✅ `.env.example` - Variables de entorno de ejemplo
- ✅ `requirements.txt` - Dependencias Python

---

## 📋 PASOS PARA DESPLEGAR (MANUAL)

### PASO 1: Ir al Dashboard de Railway
1. Abre: https://railway.app/dashboard
2. Inicia sesión con tu cuenta

### PASO 2: Crear Nuevo Proyecto
1. Haz clic en **"Create New Project"** (o el botón "+")
2. Selecciona **"Deploy from GitHub"**
3. Autoriza Railway para acceder a tu GitHub
4. Selecciona tu repositorio: **emcr30/CHICAGO**
5. Selecciona la rama **main**
6. Haz clic en **Deploy**

### PASO 3: Esperar el Build Inicial
- Railway detectará el `Dockerfile.railway` automáticamente
- El build debería tomar 2-5 minutos
- Verifica los logs para asegurarte que no hay errores

### PASO 4: Agregar PostgreSQL
1. Una vez que el primer deploy termine, ve a **"Services"** en tu proyecto
2. Haz clic en **"+ Add Service"**
3. Selecciona **"Add from Marketplace"** → **"PostgreSQL"**
4. Acepta los valores por defecto (Railway los configurará automáticamente)

### PASO 5: Configurar Variables de Entorno
Railway debería auto-inyectar estas variables de PostgreSQL:
- `PGHOST` → `PG_HOST`
- `PGPORT` → `PG_PORT`
- `PGUSER` → `PG_USER`
- `PGPASSWORD` → `PG_PASSWORD`
- `PGDATABASE` → `PG_DBNAME`

Agrega esta variable manualmente en los Settings:
```
DB_MODE=postgres
```

### PASO 6: Re-desplegar
1. Ve a tu servicio de API/Web
2. Haz clic en **"Redeploy"** en los logs
3. Espera a que se complete el deploy

### PASO 7: Obtener la URL de tu aplicación
Una vez que el deploy esté completo:
1. Ve a la pestaña **"Deployments"**
2. Copia la URL pública de Railway
3. ¡Tu aplicación estará corriendo en la nube!

---

## 🔧 ALTERNATIVA: Despliegue Automático desde Terminal

Si prefieres hacerlo desde terminal después de crear el proyecto en Railroad:

```bash
cd /Users/evelyn/Documents/CHICAGO/CHICAGO

# Necesitarás hacer login en Railway CLI
railway login

# Vincular tu proyecto (reemplaza <PROJECT_ID> con el ID de tu proyecto)
railway link <PROJECT_ID>

# Desplegar
railway up
```

---

## 📊 ESTRUCTURA DEL PROYECTO

```
CHICAGO/
├── api.py              # API Flask (Port 8000)
├── main.py             # Streamlit app (Port 8501)
├── db_postgres.py      # Conexión a PostgreSQL
├── requirements.txt    # Dependencias Python
├── Dockerfile.railway  # Dockerfile optimizado
├── railway.json        # Configuración Railway
└── ...otros archivos
```

---

## 🚨 SOLUCIÓN DE PROBLEMAS

### Si el build falla:
1. Ve a la pestaña **"Logs"**
2. Busca el error específico
3. Verifica que `requirements.txt` tiene todas las dependencias

### Si PostgreSQL no se conecta:
1. Ve a **"Services"** → **PostgreSQL**
2. Copia las variables de entorno mostradas
3. Agrega al servicio de API/Web

### Si el puerto no funciona:
- Railway asigna automáticamente un puerto via la variable `PORT`
- El `Dockerfile.railway` ya está configurado para usarlo: `${PORT:-8000}`

---

## ✨ PRÓXIMOS PASOS DESPUÉS DEL DEPLOY

1. Probar tu API:
   ```bash
   curl https://tu-url-railway.railway.app/health
   ```

2. Verificar base de datos:
   - Los datos se crearán automáticamente al iniciar la app
   - La tabla `crimes` se creará en PostgreSQL

3. Configurar dominio personalizado (opcional):
   - En Railway Settings → "Domain"

---

## 📞 NECESITAS AYUDA?

Si algo no funciona:
1. Revisa los **Logs** en Railway dashboard
2. Verifica que el **API Token** tiene permisos correctos
3. Asegúrate que PostgreSQL está en **"Connected"**

¡Tu proyecto está 100% listo para Railway! 🎉
