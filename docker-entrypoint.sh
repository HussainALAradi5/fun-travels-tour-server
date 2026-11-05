#!/bin/sh
# Hosts give settings in their own shapes; Spring wants SERVER_PORT, a JDBC DB_URL, DB_USERNAME and DB_PASSWORD.
#  - PORT (Render)                                   -> SERVER_PORT
#  - DB_URL=postgresql://user:pass@host/db?sslmode=… -> jdbc:postgresql://host/db?sslmode=… plus user and password
#    (the connection string Neon, Render and most hosts show)
#  - DB_HOST, DB_PORT, DB_NAME                       -> DB_URL
# Values already given explicitly win.
set -e
if [ -z "$SERVER_PORT" ] && [ -n "$PORT" ]; then
  export SERVER_PORT="$PORT"
fi
case "$DB_URL" in
  postgres://*|postgresql://*)
    rest="${DB_URL#*://}"
    case "$rest" in
      *@*)
        userinfo="${rest%%@*}"
        rest="${rest#*@}"
        if [ -z "$DB_USERNAME" ]; then export DB_USERNAME="${userinfo%%:*}"; fi
        case "$userinfo" in
          *:*) if [ -z "$DB_PASSWORD" ]; then export DB_PASSWORD="${userinfo#*:}"; fi ;;
        esac
        ;;
    esac
    export DB_URL="jdbc:postgresql://${rest}"
    ;;
esac
if [ -z "$DB_URL" ] && [ -n "$DB_HOST" ]; then
  export DB_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT:-5432}/${DB_NAME}"
fi
exec java -jar /app/app.jar
