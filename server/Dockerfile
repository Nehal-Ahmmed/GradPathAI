# ==============================================================================
# GradPath AI - Stateless Cloud REST API Gateway Container
# ==============================================================================
FROM gradle:8.12-jdk21 AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN ./gradlew :server:installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /home/gradle/project/server/build/install/server /app

ENV PORT=8080
ENV WORKSPACE_DIR=/app/data

RUN mkdir -p /app/data

EXPOSE 8080

CMD ["bin/server"]
