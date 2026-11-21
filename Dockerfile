FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/sajha-backend-0.1.0-SNAPSHOT.jar /app/app.jar
USER 10001
ENV PORT=10000
EXPOSE 10000
ENTRYPOINT ["java","-jar","/app/app.jar"]
