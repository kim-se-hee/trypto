# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon || true
COPY src/ src/
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon -x test \
 && cp build/libs/*.jar app.jar \
 && java -Djarmode=layertools -jar app.jar extract --destination extracted

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./
EXPOSE 8082
VOLUME /wal
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
