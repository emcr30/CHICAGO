#!/bin/bash

# Script para desplegar en Railway después del login manual
# Uso: ./deploy-to-railway.sh <RAILWAY_PROJECT_ID>

PROJECT_ID="$1"

if [ -z "$PROJECT_ID" ]; then
    echo "❌ Error: Proporciona el PROJECT_ID de Railway"
    echo "Uso: ./deploy-to-railway.sh <PROJECT_ID>"
    exit 1
fi

cd "$(dirname "$0")" || exit

echo "🔗 Vinculando proyecto de Railway..."
railway link "$PROJECT_ID" || exit 1

echo "📦 Agregando servicio de PostgreSQL..."
railway add postgres || {
    echo "⚠️  PostgreSQL ya podría estar agregado"
}

echo "🚀 Iniciando despliegue..."
railway up

echo "✅ ¡Despliegue completado!"
echo ""
echo "Puedes monitorear el progreso en:"
echo "  https://railway.app/project/$PROJECT_ID"
