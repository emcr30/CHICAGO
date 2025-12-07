#!/bin/bash

# Railway Auto-Deploy Script
# Este script intenta desplegar automáticamente en Railway

set -e

CHICAGO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$CHICAGO_DIR"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           Railway Auto-Deploy para CHICAGO                    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Verificar que Railway CLI está instalado
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI no está instalado"
    echo "Instálalo con: brew install railway"
    exit 1
fi

echo "✅ Railway CLI $(railway --version) detectado"
echo ""

# Verificar código está en git
if [ ! -d ".git" ]; then
    echo "❌ Este no es un repositorio git"
    exit 1
fi

echo "📌 Estado del repositorio:"
git log --oneline -3
echo ""

# Intentar hacer login
echo "🔐 Intentando autenticación en Railway..."
echo ""
echo "⚠️  Abre tu navegador cuando se indique para autenticarse"
echo "   (Un navegador debería abrirse automáticamente)"
echo ""

if railway login; then
    echo "✅ ¡Autenticación exitosa!"
else
    echo "❌ La autenticación falló"
    echo ""
    echo "Alternativa: Haz login manualmente:"
    echo "  $ railway login"
    echo "Luego ejecuta este script de nuevo"
    exit 1
fi

echo ""
echo "🚀 Creando proyecto CHICAGO en Railway..."

# Crear proyecto
if railway init -n "CHICAGO" 2>/dev/null || railway link 2>/dev/null; then
    echo "✅ Proyecto inicializado"
else
    # Si falla, probablemente ya existe
    echo "⚠️  El proyecto podría ya existir, continuando..."
fi

echo ""
echo "📦 Estado actual:"
railway projects

echo ""
echo "✨ Proyecto listo. Próximos pasos:"
echo ""
echo "  1. Agrega PostgreSQL:"
echo "     $ railway add postgres"
echo ""
echo "  2. Despliega:"
echo "     $ railway up"
echo ""
echo "  3. Abre en el navegador:"
echo "     $ railway open"
echo ""
