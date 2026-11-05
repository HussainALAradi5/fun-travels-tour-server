# Build: compile and package with the Maven wrapper (tests run in CI).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# Every setting in the example file reads an environment variable, so it serves as the runtime config.
RUN cp src/main/resources/application.properties.example src/main/resources/application.properties \
    && ./mvnw -B -q -DskipTests package

# Run: a small JRE image as a non-root user.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app && mkdir -p /app/uploads /app/media && chown -R app /app
COPY --from=build /app/target/server-0.0.1-SNAPSHOT.jar app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh
USER app
ENV FILE_UPLOAD_DIR=/app/uploads MEDIA_DIR=/app/media \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["/app/docker-entrypoint.sh"]
