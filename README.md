# Tutor IA de Estudio - Concurso DIAN 2676 (CNSC)

Bot automatizado para preparación del concurso de méritos de la DIAN mediante pruebas de Juicio Situacional con distractores técnicos y metodología CNSC.

## 🚀 Variables de Entorno Requeridas

Para ejecutar o desplegar este bot (en Docker, Railway, Render, VPS o local), define las siguientes variables de entorno:

| Variable | Descripción |
| :--- | :--- |
| `TELEGRAM_BOT_TOKEN` | Token de tu bot obtenido desde @BotFather |
| `TELEGRAM_CHAT_ID` | Tu ID numérico de chat o grupo en Telegram |
| `GEMINI_API_KEY` | Llave API de Google AI Studio |

## 🛠️ Ejecución Local con Docker

```bash
docker build -t dian-study-bot .
docker run -d \
  -e TELEGRAM_BOT_TOKEN="tu_token" \
  -e TELEGRAM_CHAT_ID="tu_chat_id" \
  -e GEMINI_API_KEY="tu_api_key" \
  --name dian-bot dian-study-bot
```
