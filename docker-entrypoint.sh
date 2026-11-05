#!/bin/sh
# Hosts such as Render give the port in PORT and the database as separate parts; Spring wants SERVER_PORT and a
# JDBC URL. Explicit SERVER_PORT and DB_URL values win.
set -e
if [ -z "$SERVER_PORT" ] && [ -n "$PORT" ]; then
  export SERVER_PORT="$PORT"
fi
if [ -z "$DB_URL" ] && [ -n "$DB_HOST" ]; then
  export DB_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT:-5432}/${DB_NAME}"
fi
exec java -jar /app/app.jar
