#!/bin/bash
set -e

mkdir -p ./documentos

echo "=========================================================="
echo "📥 Descargando los 170 documentos desde Google Drive..."
echo "=========================================================="

# Descargar la carpeta de Google Drive completa
gdown --folder "https://drive.google.com/drive/folders/1zbsregPcgKxV6mxZw59uI9kQck_6uqeh" -O ./documentos --remaining-ok || true

echo "✅ Descarga finalizada. Iniciando Tutor DIAN 2676..."
java -jar app.jar
