FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY src src
RUN mkdir -p bin && find src -name "*.java" > sources.txt && javac -d bin @sources.txt

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/bin bin
COPY resources resources

ENV PORT=3000
ENV RESOURCES_PATH=resources
ENV BACKEND_HOST=localhost:8080

EXPOSE 3000

ENTRYPOINT ["java", "-cp", "bin", "Server"]
