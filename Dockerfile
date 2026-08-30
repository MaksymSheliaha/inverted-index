FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY pom.xml pom.xml
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/inverted-index-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
EXPOSE 9010

ENTRYPOINT ["java", "-jar", "app.jar"]
