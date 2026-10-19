FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
 
# Primero solo el pom, para que las dependencias queden en caché
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
 
COPY src ./src
# Los tests de integración usan Testcontainers y se ejecutan con mvn verify, no en la imagen
RUN mvn -B -q package -DskipTests
 
# Etapa 2: imagen de ejecución
FROM eclipse-temurin:21-jre
WORKDIR /app
 
# Usuario sin privilegios
RUN groupadd --system app && useradd --system --gid app app
 
COPY --from=build /app/target/*.jar app.jar
 
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]