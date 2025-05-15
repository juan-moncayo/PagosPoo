FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copiar solo el pom.xml primero para aprovechar la caché de dependencias
COPY pom.xml .
# Descargar todas las dependencias de manera explícita
RUN mvn dependency:go-offline -B

# Ahora copiar el código fuente
COPY src ./src/

# Empaquetar la aplicación
RUN mvn package -DskipTests

# Segunda etapa: imagen de ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar el JAR compilado
COPY --from=build /app/target/*.jar app.jar

# Puerto expuesto
EXPOSE 8084

# Comando para ejecutar la aplicación
CMD ["java", "-jar", "app.jar"]
